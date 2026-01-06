package space.alnovis.protowrapper.merger;

import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import space.alnovis.protowrapper.generator.TypeNormalizer;
import space.alnovis.protowrapper.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Merges multiple version schemas into a unified schema.
 *
 * <p>The merger identifies common messages and fields across versions,
 * tracks which fields exist in which versions, and handles type conflicts.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * VersionMerger merger = new VersionMerger();
 * MergedSchema schema = merger.merge(Arrays.asList(v1Schema, v2Schema));
 * </pre>
 */
public class VersionMerger {

    private final MergerConfig config;
    private final PluginLogger logger;

    /**
     * Create a new VersionMerger with default configuration.
     */
    public VersionMerger() {
        this(new MergerConfig(), PluginLogger.console());
    }

    /**
     * Create a new VersionMerger with the specified configuration.
     *
     * @param config the merger configuration
     */
    public VersionMerger(MergerConfig config) {
        this(config, PluginLogger.console());
    }

    /**
     * Create a new VersionMerger with the specified logger.
     *
     * @param logger the logger to use
     */
    public VersionMerger(PluginLogger logger) {
        this(new MergerConfig(), logger);
    }

    /**
     * Create a new VersionMerger with the specified configuration and logger.
     *
     * @param config the merger configuration
     * @param logger the logger to use
     */
    public VersionMerger(MergerConfig config, PluginLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Merge multiple version schemas into one unified schema.
     *
     * @param schemas List of version schemas to merge
     * @return Merged schema
     */
    public MergedSchema merge(List<VersionSchema> schemas) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("At least one schema required");
        }

        // Collect versions using stream
        List<String> versions = schemas.stream()
                .map(VersionSchema::getVersion)
                .toList();

        MergedSchema merged = new MergedSchema(versions);

        // Collect all message names across versions using flatMap
        Set<String> allMessageNames = schemas.stream()
                .flatMap(schema -> schema.getMessageNames().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Merge each message using stream with filter
        allMessageNames.stream()
                .map(name -> mergeMessage(name, schemas))
                .filter(Objects::nonNull)
                .forEach(merged::addMessage);

        // Collect and merge enums using stream
        Set<String> allEnumNames = schemas.stream()
                .flatMap(schema -> schema.getEnumNames().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        allEnumNames.stream()
                .map(name -> mergeEnum(name, schemas))
                .filter(Objects::nonNull)
                .forEach(merged::addEnum);

        // Detect equivalent enums (nested vs top-level)
        detectEquivalentEnums(merged, schemas);

        // Collect conflict enums for INT_ENUM type conflicts
        collectConflictEnums(merged, schemas);

        return merged;
    }

    /**
     * Collect conflict enum information for all fields with INT_ENUM type conflicts.
     */
    private void collectConflictEnums(MergedSchema merged, List<VersionSchema> schemas) {
        merged.getMessages().forEach(message ->
                collectConflictEnumsForMessage(message, schemas, merged));
    }

    /**
     * Collect conflict enums for a single message and its nested messages.
     */
    private void collectConflictEnumsForMessage(MergedMessage message,
                                                  List<VersionSchema> schemas,
                                                  MergedSchema merged) {
        // Process fields with INT_ENUM conflict
        message.getFields().stream()
                .filter(field -> field.getConflictType() == MergedField.ConflictType.INT_ENUM)
                .forEach(field -> {
                    ConflictEnumInfo enumInfo = createConflictEnumInfo(
                            message.getName(), field, schemas);
                    if (enumInfo != null) {
                        merged.addConflictEnum(enumInfo);
                        logger.info(String.format("Created conflict enum '%s' for %s.%s with %d values",
                                enumInfo.getEnumName(), message.getName(), field.getName(),
                                enumInfo.getValues().size()));
                    }
                });

        // Recursively process nested messages
        message.getNestedMessages().forEach(nested ->
                collectConflictEnumsForMessage(nested, schemas, merged));
    }

    /**
     * Create ConflictEnumInfo for a field with INT_ENUM conflict.
     */
    private ConflictEnumInfo createConflictEnumInfo(String messageName,
                                                      MergedField field,
                                                      List<VersionSchema> schemas) {
        ConflictEnumInfo.Builder builder = ConflictEnumInfo.builder()
                .messageName(messageName)
                .fieldName(field.getName());

        // Collect enum values from all versions that have enum type
        for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
            String version = entry.getKey();
            FieldInfo fieldInfo = entry.getValue();

            if (fieldInfo.isEnum()) {
                // Find the enum info for this field
                EnumInfo enumInfo = findEnumForField(messageName, fieldInfo, version, schemas);
                if (enumInfo != null) {
                    builder.addValuesFrom(enumInfo);
                    // Store the proto enum FQN for this version
                    String protoEnumFqn = getProtoEnumFqn(fieldInfo, version, schemas);
                    if (protoEnumFqn != null) {
                        builder.addVersionEnumType(version, protoEnumFqn);
                    }
                }
            }
        }

        try {
            return builder.build();
        } catch (IllegalStateException e) {
            logger.warn("Failed to create ConflictEnumInfo for " + messageName + "." + field.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Find the EnumInfo for a field that is of enum type.
     */
    private EnumInfo findEnumForField(String messageName, FieldInfo field,
                                       String version, List<VersionSchema> schemas) {
        // The field's javaType contains the enum name (simple name or qualified)
        String enumTypeName = extractEnumName(field.getJavaType());

        // First, try to find as a nested enum in the message
        return schemas.stream()
                .filter(schema -> schema.getVersion().equals(version))
                .flatMap(schema -> schema.getMessage(messageName).stream())
                .flatMap(msg -> msg.getNestedEnums().stream())
                .filter(nestedEnum -> nestedEnum.getName().equals(enumTypeName))
                .findFirst()
                // If not found as nested enum, try to find as a top-level enum
                .or(() -> schemas.stream()
                        .filter(schema -> schema.getVersion().equals(version))
                        .flatMap(schema -> schema.getEnum(enumTypeName).stream())
                        .findFirst())
                .orElse(null);
    }

    /**
     * Get the fully qualified proto enum name for a field.
     */
    private String getProtoEnumFqn(FieldInfo field, String version, List<VersionSchema> schemas) {
        // The field's typeName contains the proto path
        String typeName = field.getTypeName();
        if (typeName != null && !typeName.isEmpty()) {
            // Remove leading dot if present
            return typeName.startsWith(".") ? typeName.substring(1) : typeName;
        }
        // Fallback to java type
        return field.getJavaType();
    }

    /**
     * Extract simple enum name from a potentially qualified type name.
     */
    private String extractEnumName(String javaType) {
        if (javaType == null) return null;
        // Handle qualified names like "com.example.MyEnum" or "OuterClass.InnerEnum"
        int lastDot = javaType.lastIndexOf('.');
        return lastDot >= 0 ? javaType.substring(lastDot + 1) : javaType;
    }

    /**
     * Detect equivalent enums where a nested enum in one version
     * corresponds to a top-level enum in another version.
     * Register mappings and remove duplicate nested enums.
     */
    private void detectEquivalentEnums(MergedSchema merged, List<VersionSchema> schemas) {
        // Collect all top-level enum infos for comparison using streams
        Map<String, EnumInfo> topLevelEnumInfos = schemas.stream()
                .flatMap(schema -> schema.getEnumNames().stream()
                        .flatMap(name -> schema.getEnum(name).stream()
                                .map(e -> Map.entry(name, e))))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // Check each message's nested enums for equivalence with top-level enums
        merged.getMessages().forEach(message ->
                detectEquivalentEnumsInMessage(message, topLevelEnumInfos, merged, schemas));
    }

    /**
     * Recursively check message and its nested messages for equivalent enums.
     */
    private void detectEquivalentEnumsInMessage(MergedMessage message,
                                                 Map<String, EnumInfo> topLevelEnumInfos,
                                                 MergedSchema merged,
                                                 List<VersionSchema> schemas) {
        // Find nested enums that are equivalent to top-level enums
        List<MergedEnum> nestedEnumsToRemove = message.getNestedEnums().stream()
                .filter(nestedEnum -> {
                    String nestedEnumName = nestedEnum.getName();
                    EnumInfo topLevelInfo = topLevelEnumInfos.get(nestedEnumName);
                    if (topLevelInfo == null) {
                        return false;
                    }
                    EnumInfo nestedInfo = findNestedEnumInfo(message.getName(), nestedEnumName, schemas);
                    if (nestedInfo != null && nestedInfo.isEquivalentTo(topLevelInfo)) {
                        String nestedPath = message.getQualifiedInterfaceName() + "." + nestedEnumName;
                        merged.addEquivalentEnumMapping(nestedPath, nestedEnumName);
                        logger.info("Detected equivalent enums: " + nestedPath + " -> " + nestedEnumName);
                        return true;
                    }
                    return false;
                })
                .toList();

        // Remove equivalent nested enums from the message
        nestedEnumsToRemove.forEach(message::removeNestedEnum);

        // Recursively check nested messages
        message.getNestedMessages().forEach(nested ->
                detectEquivalentEnumsInMessage(nested, topLevelEnumInfos, merged, schemas));
    }

    /**
     * Find the EnumInfo for a nested enum by searching through version schemas.
     */
    private EnumInfo findNestedEnumInfo(String messageName, String enumName, List<VersionSchema> schemas) {
        return schemas.stream()
                .map(schema -> schema.getMessage(messageName))
                .flatMap(Optional::stream)
                .flatMap(msg -> msg.getNestedEnums().stream())
                .filter(e -> e.getName().equals(enumName))
                .findFirst()
                .orElse(null);
    }

    private MergedMessage mergeMessage(String messageName, List<VersionSchema> schemas) {
        MergedMessage merged = new MergedMessage(messageName);

        // Process schemas and collect data using streams
        Map<Integer, List<FieldWithVersion>> fieldsByNumber = schemas.stream()
                .flatMap(schema -> schema.getMessage(messageName).stream()
                        .peek(msg -> {
                            merged.addVersion(schema.getVersion());
                            if (msg.getSourceFileName() != null) {
                                merged.addSourceFile(schema.getVersion(), msg.getSourceFileName());
                            }
                        })
                        .flatMap(msg -> msg.getFields().stream()
                                .map(field -> new FieldWithVersion(field, schema.getVersion()))))
                .collect(Collectors.groupingBy(
                        fv -> fv.field().getNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Collect nested message and enum names across all versions
        Set<String> processedNestedMessages = schemas.stream()
                .flatMap(schema -> schema.getMessage(messageName).stream())
                .flatMap(msg -> msg.getNestedMessages().stream())
                .map(MessageInfo::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> processedNestedEnums = schemas.stream()
                .flatMap(schema -> schema.getMessage(messageName).stream())
                .flatMap(msg -> msg.getNestedEnums().stream())
                .map(EnumInfo::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Merge each unique nested message across all versions
        processedNestedMessages.forEach(nestedName -> {
            List<VersionSchema> nestedSchemas = schemas.stream()
                    .flatMap(s -> s.getMessage(messageName).stream()
                            .flatMap(parent -> parent.getNestedMessages().stream()
                                    .filter(n -> n.getName().equals(nestedName))
                                    .map(n -> {
                                        VersionSchema nestedSchema = new VersionSchema(s.getVersion());
                                        nestedSchema.addMessage(n);
                                        return nestedSchema;
                                    })))
                    .toList();

            if (!nestedSchemas.isEmpty()) {
                Optional.ofNullable(mergeMessage(nestedName, nestedSchemas))
                        .ifPresent(merged::addNestedMessage);
            }
        });

        // Merge nested enums
        processedNestedEnums.forEach(enumName -> {
            List<VersionSchema> enumSchemas = schemas.stream()
                    .flatMap(s -> s.getMessage(messageName).stream()
                            .flatMap(parent -> parent.getNestedEnums().stream()
                                    .filter(e -> e.getName().equals(enumName))
                                    .map(e -> {
                                        VersionSchema enumSchema = new VersionSchema(s.getVersion());
                                        enumSchema.addEnum(e);
                                        return enumSchema;
                                    })))
                    .toList();

            if (!enumSchemas.isEmpty()) {
                Optional.ofNullable(mergeEnum(enumName, enumSchemas))
                        .ifPresent(merged::addNestedEnum);
            }
        });

        // Merge fields with same number
        fieldsByNumber.values().stream()
                .map(this::mergeFields)
                .filter(Objects::nonNull)
                .forEach(merged::addField);

        // Merge oneof groups
        mergeOneofGroups(merged, messageName, schemas);

        return merged.getPresentInVersions().isEmpty() ? null : merged;
    }

    /**
     * Merge oneof groups from all versions into the merged message.
     */
    private void mergeOneofGroups(MergedMessage merged, String messageName, List<VersionSchema> schemas) {
        OneofConflictDetector conflictDetector = new OneofConflictDetector();
        Set<String> allVersions = schemas.stream()
                .map(VersionSchema::getVersion)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Collect oneofs by version for renamed detection
        Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
        for (VersionSchema schema : schemas) {
            schema.getMessage(messageName).ifPresent(msg -> {
                oneofsByVersion.put(schema.getVersion(), msg.getOneofGroups());
            });
        }

        // Detect renamed oneofs (same field numbers, different names)
        List<OneofConflictDetector.RenamedOneofGroup> renamedGroups =
                conflictDetector.detectRenamedOneofs(oneofsByVersion, messageName);

        // Build set of field numbers that are part of renamed groups (to avoid duplicate processing)
        Set<Set<Integer>> renamedFieldSets = renamedGroups.stream()
                .map(OneofConflictDetector.RenamedOneofGroup::fieldNumbers)
                .collect(Collectors.toSet());

        // Collect all oneof names across versions
        Set<String> allOneofNames = schemas.stream()
                .flatMap(schema -> schema.getMessage(messageName).stream())
                .flatMap(msg -> msg.getOneofGroups().stream())
                .map(OneofInfo::getProtoName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (allOneofNames.isEmpty()) {
            return;
        }

        // Process renamed oneofs first
        Set<String> processedOneofNames = new HashSet<>();
        for (OneofConflictDetector.RenamedOneofGroup renamedGroup : renamedGroups) {
            String primaryName = renamedGroup.getMostCommonName();
            MergedOneof.Builder oneofBuilder = MergedOneof.builder(primaryName);

            // Add all version names
            for (String name : renamedGroup.getAllNames()) {
                oneofBuilder.addMergedFromName(name);
                processedOneofNames.add(name);
            }

            // Collect oneofs from each version (using version-specific names)
            for (VersionSchema schema : schemas) {
                String version = schema.getVersion();
                String versionName = renamedGroup.versionToName().get(version);
                if (versionName != null) {
                    schema.getMessage(messageName).ifPresent(msg -> {
                        msg.findOneofByName(versionName).ifPresent(oneof -> {
                            oneofBuilder.addVersionOneof(version, oneof);
                        });
                    });
                }
            }

            // Add renamed conflict
            oneofBuilder.addConflict(renamedGroup.toConflictInfo());
            logger.warn(String.format("Oneof RENAMED: '%s.%s' has different names: %s",
                    messageName, primaryName, renamedGroup.versionToName()));

            finishOneofBuild(oneofBuilder, merged, messageName, conflictDetector, allVersions);
        }

        // Process remaining oneofs that weren't part of renamed groups
        for (String oneofName : allOneofNames) {
            if (processedOneofNames.contains(oneofName)) {
                continue;
            }

            // Check if this oneof's fields are part of a renamed group (already processed)
            Set<Integer> thisOneofFields = schemas.stream()
                    .flatMap(schema -> schema.getMessage(messageName).stream())
                    .flatMap(msg -> msg.findOneofByName(oneofName).stream())
                    .flatMap(oneof -> oneof.getFieldNumbers().stream())
                    .collect(Collectors.toSet());

            boolean isPartOfRenamed = renamedFieldSets.stream()
                    .anyMatch(fs -> !Collections.disjoint(fs, thisOneofFields));
            if (isPartOfRenamed) {
                continue;
            }

            MergedOneof.Builder oneofBuilder = MergedOneof.builder(oneofName);

            // Collect oneofs from each version
            for (VersionSchema schema : schemas) {
                schema.getMessage(messageName).ifPresent(msg -> {
                    msg.findOneofByName(oneofName).ifPresent(oneof -> {
                        oneofBuilder.addVersionOneof(schema.getVersion(), oneof);
                    });
                });
            }

            finishOneofBuild(oneofBuilder, merged, messageName, conflictDetector, allVersions);
        }

        // Detect field membership changes (field in/out of oneof)
        detectAndLogFieldMembershipChanges(conflictDetector, merged, messageName, schemas, allVersions);
    }

    /**
     * Complete oneof building with fields and conflict detection.
     */
    private void finishOneofBuild(MergedOneof.Builder oneofBuilder, MergedMessage merged,
                                   String messageName, OneofConflictDetector conflictDetector,
                                   Set<String> allVersions) {
        // Collect all field numbers from all version oneofs
        Set<Integer> oneofFieldNumbers = new LinkedHashSet<>();
        for (OneofInfo oneof : oneofBuilder.getVersionOneofs().values()) {
            oneofFieldNumbers.addAll(oneof.getFieldNumbers());
        }

        // Find merged fields that belong to this oneof
        List<MergedField> oneofFields = merged.getFields().stream()
                .filter(f -> oneofFieldNumbers.contains(f.getNumber()))
                .toList();

        oneofBuilder.addFields(oneofFields);

        // Detect conflicts
        List<OneofConflictInfo> conflicts = conflictDetector.detectConflicts(
                oneofBuilder, messageName, allVersions);
        oneofBuilder.addConflicts(conflicts);

        // Detect field number changes
        List<OneofConflictInfo> numberChanges = conflictDetector.detectFieldNumberChanges(
                oneofBuilder, messageName);
        oneofBuilder.addConflicts(numberChanges);

        MergedOneof mergedOneof = oneofBuilder.build();
        merged.addOneofGroup(mergedOneof);

        // Log conflicts
        logOneofConflicts(mergedOneof, messageName);
    }

    /**
     * Log detected oneof conflicts.
     */
    private void logOneofConflicts(MergedOneof oneof, String messageName) {
        for (OneofConflictInfo conflict : oneof.getConflicts()) {
            switch (conflict.getType()) {
                case PARTIAL_EXISTENCE -> logger.warn(String.format(
                        "Oneof PARTIAL: '%s.%s' not in all versions - %s",
                        messageName, oneof.getProtoName(), conflict.getDescription()));
                case FIELD_SET_DIFFERENCE -> logger.warn(String.format(
                        "Oneof FIELD_DIFF: '%s.%s' - %s",
                        messageName, oneof.getProtoName(), conflict.getDescription()));
                case FIELD_TYPE_CONFLICT -> logger.warn(String.format(
                        "Oneof TYPE_CONFLICT: '%s.%s' - %s",
                        messageName, oneof.getProtoName(), conflict.getDescription()));
                case FIELD_REMOVED -> logger.warn(String.format(
                        "Oneof FIELD_REMOVED: '%s.%s' - %s",
                        messageName, oneof.getProtoName(), conflict.getDescription()));
                case FIELD_NUMBER_CHANGE -> logger.warn(String.format(
                        "Oneof NUMBER_CHANGE: '%s.%s' - %s",
                        messageName, oneof.getProtoName(), conflict.getDescription()));
                case RENAMED -> {
                    // Already logged during processing
                }
                default -> logger.warn(String.format(
                        "Oneof CONFLICT: '%s.%s' [%s] - %s",
                        messageName, oneof.getProtoName(), conflict.getType(), conflict.getDescription()));
            }
        }
    }

    /**
     * Detect and log field membership changes (field moving in/out of oneof).
     */
    private void detectAndLogFieldMembershipChanges(
            OneofConflictDetector conflictDetector,
            MergedMessage merged,
            String messageName,
            List<VersionSchema> schemas,
            Set<String> allVersions) {

        // Build field map: fieldNumber -> version -> FieldInfo
        Map<Integer, Map<String, FieldInfo>> allMessageFields = new LinkedHashMap<>();
        for (VersionSchema schema : schemas) {
            schema.getMessage(messageName).ifPresent(msg -> {
                for (FieldInfo field : msg.getFields()) {
                    allMessageFields
                            .computeIfAbsent(field.getNumber(), k -> new LinkedHashMap<>())
                            .put(schema.getVersion(), field);
                }
            });
        }

        // Build oneof map
        Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
        for (VersionSchema schema : schemas) {
            schema.getMessage(messageName).ifPresent(msg -> {
                oneofsByVersion.put(schema.getVersion(), msg.getOneofGroups());
            });
        }

        List<OneofConflictInfo> membershipConflicts = conflictDetector.detectFieldMembershipChanges(
                allMessageFields, oneofsByVersion, messageName, allVersions);

        for (OneofConflictInfo conflict : membershipConflicts) {
            logger.warn(String.format("Oneof MEMBERSHIP: '%s' - %s",
                    messageName, conflict.getDescription()));
        }
    }

    private MergedField mergeFields(List<FieldWithVersion> fields) {
        if (fields.isEmpty()) {
            return null;
        }

        // Build merged field using builder for proper conflict handling
        MergedField.Builder builder = MergedField.builder();

        // Add all version fields
        fields.forEach(fv -> builder.addVersionField(fv.version(), fv.field()));

        // Collect unique Java types
        Set<String> uniqueTypes = fields.stream()
                .map(fv -> fv.field().getJavaType())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Check for REPEATED_SINGLE conflict - some versions have repeated, others singular
        if (hasRepeatedSingleConflict(fields)) {
            FieldWithVersion first = fields.get(0);
            builder.conflictType(MergedField.ConflictType.REPEATED_SINGLE);

            // For REPEATED_SINGLE, always use List<T> as unified type
            String elementType = determineElementType(fields);
            String listType = "java.util.List<" + boxType(elementType) + ">";
            builder.resolvedJavaType(listType);
            builder.resolvedGetterType(listType);

            // Log warning for conflict
            String labelStr = fields.stream()
                    .map(fv -> fv.version() + ":" + (fv.field().isRepeated() ? "repeated" : "singular"))
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Repeated/singular conflict for field '%s': %s (using List<%s>)",
                    first.field().getProtoName(), labelStr, boxType(elementType)));
        }
        // Check for SIGNED_UNSIGNED conflict - this conflict is not visible
        // by looking at Java types alone (int32 and uint32 both map to "int")
        else if (hasSignedUnsignedConflict(fields)) {
            FieldWithVersion first = fields.get(0);
            builder.conflictType(MergedField.ConflictType.SIGNED_UNSIGNED);

            // For SIGNED_UNSIGNED, always use long (safe for uint32 values > Integer.MAX_VALUE)
            String widerType = "long";
            boolean isRepeated = first.field().isRepeated();
            if (isRepeated) {
                String listType = "java.util.List<" + boxType(widerType) + ">";
                builder.resolvedJavaType(listType);
                builder.resolvedGetterType(listType);
            } else {
                builder.resolvedJavaType(widerType);
                builder.resolvedGetterType(widerType);
            }

            // Log warning for conflict
            String typesStr = fields.stream()
                    .map(fv -> fv.version() + ":" + fv.field().getType())
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Signed/unsigned conflict for field '%s': %s (using long)",
                    first.field().getProtoName(), typesStr));
        } else if (uniqueTypes.size() > 1) {
            // Other type conflicts (different Java types)
            FieldWithVersion first = fields.get(0);

            // For map fields, value type conflicts are handled separately by detectMapValueConflict()
            // Don't set field-level conflictType for maps - they use mapValueConflictType instead
            boolean allMaps = fields.stream().allMatch(fv -> fv.field().isMap());
            if (allMaps) {
                // Map fields with value type conflicts are handled by detectMapValueConflict()
                // Don't mark as INCOMPATIBLE - leave conflictType as NONE
            } else {
                MergedField.ConflictType conflictType = detectConflictType(fields);
                builder.conflictType(conflictType);

                // For WIDENING and FLOAT_DOUBLE conflicts, set the resolved type
                if (conflictType == MergedField.ConflictType.WIDENING ||
                    conflictType == MergedField.ConflictType.FLOAT_DOUBLE) {
                    String widerType = determineWiderType(uniqueTypes);
                    if (widerType != null) {
                        boolean isRepeated = first.field().isRepeated();
                        if (isRepeated) {
                            String listType = "java.util.List<" + boxType(widerType) + ">";
                            builder.resolvedJavaType(listType);
                            builder.resolvedGetterType(listType);
                        } else {
                            builder.resolvedJavaType(widerType);
                            builder.resolvedGetterType(widerType);
                        }
                    }
                }

                // For ENUM_ENUM conflicts, use int as unified type
                if (conflictType == MergedField.ConflictType.ENUM_ENUM) {
                    builder.resolvedJavaType("int");
                    builder.resolvedGetterType("int");
                }

                // Log warning for conflicts
                String typesStr = fields.stream()
                        .map(fv -> fv.version() + ":" + fv.field().getJavaType())
                        .collect(Collectors.joining(", "));
                logger.warn(String.format("Type conflict for field '%s' [%s]: %s",
                        first.field().getProtoName(), conflictType, typesStr));
            }
        }

        // Check for OPTIONAL_REQUIRED conflict after type conflicts
        // This can exist independently or alongside type conflicts
        if (hasOptionalRequiredConflict(fields)) {
            FieldWithVersion first = fields.get(0);
            // Only set conflict type if no other conflict was detected
            if (builder.getConflictType() == MergedField.ConflictType.NONE) {
                builder.conflictType(MergedField.ConflictType.OPTIONAL_REQUIRED);
            }
            // Log warning for conflict
            String optionalityStr = fields.stream()
                    .map(fv -> fv.version() + ":" + (fv.field().isOptional() ? "optional" : "required"))
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Optional/required conflict for field '%s': %s",
                    first.field().getProtoName(), optionalityStr));
        }

        // Detect map value type conflicts
        detectMapValueConflict(fields, builder);

        return builder.build();
    }

    /**
     * Detect type conflicts in map value types across versions.
     */
    private void detectMapValueConflict(List<FieldWithVersion> fields, MergedField.Builder builder) {
        // Only process if all fields are maps
        boolean allMaps = fields.stream().allMatch(fv -> fv.field().isMap());
        if (!allMaps) {
            return;
        }

        // Collect map value types from all versions
        Set<String> valueTypes = new LinkedHashSet<>();
        Set<com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type> protoValueTypes = new LinkedHashSet<>();
        boolean hasEnumValue = false;
        boolean hasMessageValue = false;

        for (FieldWithVersion fv : fields) {
            MapInfo mapInfo = fv.field().getMapInfo();
            if (mapInfo != null) {
                valueTypes.add(mapInfo.getValueJavaType());
                protoValueTypes.add(mapInfo.getValueType());
                if (mapInfo.hasEnumValue()) hasEnumValue = true;
                if (mapInfo.hasMessageValue()) hasMessageValue = true;
            }
        }

        // No conflict if all value types are the same
        if (valueTypes.size() <= 1) {
            return;
        }

        FieldWithVersion first = fields.get(0);
        String fieldName = first.field().getProtoName();

        // Determine conflict type based on value types
        boolean hasIntValue = valueTypes.stream().anyMatch(this::isIntType);
        boolean hasLongValue = valueTypes.stream().anyMatch(this::isLongType);

        // INT_ENUM conflict: some versions have int, others have enum
        if (hasIntValue && hasEnumValue) {
            builder.mapValueConflictType(MergedField.ConflictType.INT_ENUM);
            builder.resolvedMapValueType("int");
            String typesStr = fields.stream()
                    .map(fv -> fv.version() + ":" + fv.field().getMapInfo().getValueJavaType())
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Map value type conflict [INT_ENUM] for field '%s': %s (using int)",
                    fieldName, typesStr));
            return;
        }

        // WIDENING conflict: int32 -> int64
        if (hasIntValue && hasLongValue) {
            builder.mapValueConflictType(MergedField.ConflictType.WIDENING);
            builder.resolvedMapValueType("long");
            String typesStr = fields.stream()
                    .map(fv -> fv.version() + ":" + fv.field().getMapInfo().getValueJavaType())
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Map value type conflict [WIDENING] for field '%s': %s (using long)",
                    fieldName, typesStr));
            return;
        }

        // Other conflicts - log warning but don't set special handling
        String typesStr = fields.stream()
                .map(fv -> fv.version() + ":" + fv.field().getMapInfo().getValueJavaType())
                .collect(Collectors.joining(", "));
        logger.warn(String.format("Map value type conflict for field '%s': %s",
                fieldName, typesStr));
    }

    /**
     * Detect the type of conflict between field types across versions.
     */
    private MergedField.ConflictType detectConflictType(List<FieldWithVersion> fields) {
        Set<String> rawTypes = fields.stream()
                .map(fv -> fv.field().getJavaType())
                .collect(Collectors.toSet());

        // Note: SIGNED_UNSIGNED is now detected in mergeFields() BEFORE this method is called
        // because int32/uint32 both map to Java "int", so rawTypes.size() == 1 for them

        if (rawTypes.size() == 1) {
            return MergedField.ConflictType.NONE;
        }

        // Check if all fields are repeated - if so, analyze element types
        boolean allRepeated = fields.stream().allMatch(fv -> fv.field().isRepeated());

        // Extract element types for repeated fields, otherwise use raw types
        Set<String> types = rawTypes.stream()
                .map(this::extractElementType)
                .collect(Collectors.toSet());

        // Collect field characteristics based on element types
        boolean hasInt = types.stream().anyMatch(this::isIntType);
        boolean hasLong = types.stream().anyMatch(this::isLongType);
        boolean hasFloat = types.stream().anyMatch(this::isFloatType);
        boolean hasDouble = types.stream().anyMatch(this::isDoubleOnlyType);
        boolean hasEnum = fields.stream().anyMatch(fv -> fv.field().isEnum());
        boolean hasMessage = fields.stream().anyMatch(fv -> fv.field().isMessage());
        boolean hasPrimitive = fields.stream().anyMatch(fv -> fv.field().isPrimitive());
        boolean hasString = types.contains("String");
        boolean hasBytes = types.contains("byte[]") || types.contains("ByteString");

        // Check for int ↔ enum conflict
        if (hasInt && hasEnum && types.size() == 2) {
            return MergedField.ConflictType.INT_ENUM;
        }

        // Check for enum ↔ enum conflict (different enum types)
        // All fields are enums but with different Java types
        boolean allEnums = fields.stream().allMatch(fv -> fv.field().isEnum());
        if (allEnums && types.size() > 1) {
            return MergedField.ConflictType.ENUM_ENUM;
        }

        // Check for float ↔ double conflict (separate from integer widening)
        if (hasFloat && hasDouble && !hasInt && !hasLong && !hasMessage && !hasEnum) {
            return MergedField.ConflictType.FLOAT_DOUBLE;
        }

        // Check for integer widening: int → long
        if ((hasInt && hasLong && !hasDouble && !hasFloat && !hasMessage && !hasEnum) ||
            (hasInt && (hasDouble || hasFloat) && !hasLong && !hasMessage && !hasEnum)) {
            return MergedField.ConflictType.WIDENING;
        }

        // Check for narrowing: long → int (if long comes first and int later, or vice versa)
        // This is lossy, so we mark it as NARROWING but handle as WIDENING (use wider type)
        if (hasLong && hasInt && !hasDouble && !hasFloat && !hasMessage && !hasEnum) {
            // Already handled as WIDENING above if int→long
            // If long→int pattern exists, it would still be WIDENING (use long)
            return MergedField.ConflictType.WIDENING;
        }

        // Check for string ↔ bytes
        if (hasString && hasBytes && types.size() == 2) {
            return MergedField.ConflictType.STRING_BYTES;
        }

        // Check for primitive ↔ message conflict
        // Also treat String and bytes as "primitive-like" for this purpose
        // because they can be accessed directly without needing message wrapper
        if ((hasPrimitive || hasString || hasBytes) && hasMessage) {
            return MergedField.ConflictType.PRIMITIVE_MESSAGE;
        }

        // Default: incompatible types
        return MergedField.ConflictType.INCOMPATIBLE;
    }

    /**
     * Extract element type from List<X> or return type as-is.
     */
    private String extractElementType(String type) {
        return TypeNormalizer.extractListElementType(type);
    }

    private boolean isIntType(String type) {
        return TypeNormalizer.isIntType(type);
    }

    private boolean isLongType(String type) {
        return TypeNormalizer.isLongType(type);
    }

    private boolean isFloatType(String type) {
        return TypeNormalizer.isFloatType(type);
    }

    private boolean isDoubleOnlyType(String type) {
        return TypeNormalizer.isDoubleType(type);
    }

    /**
     * Check if type is any floating point (float or double).
     */
    private boolean isFloatingPointType(String type) {
        return TypeNormalizer.isFloatingPointType(type);
    }

    /**
     * Check if there is a signed/unsigned conflict between fields.
     * This detects cases where Java types are the same (int or long)
     * but protobuf types differ in signedness.
     *
     * <p>Examples of conflicts:</p>
     * <ul>
     *   <li>int32 (signed) vs uint32 (unsigned)</li>
     *   <li>int32 vs sint32 (different wire encoding)</li>
     *   <li>int64 vs uint64</li>
     *   <li>fixed32 vs sfixed32</li>
     * </ul>
     *
     * @param fields List of fields from different versions
     * @return true if signed/unsigned conflict exists
     */
    private boolean hasSignedUnsignedConflict(List<FieldWithVersion> fields) {
        Set<Type> protoTypes = fields.stream()
                .map(fv -> fv.field().getType())
                .collect(Collectors.toSet());

        // If all fields have the same protobuf type, no conflict
        if (protoTypes.size() <= 1) {
            return false;
        }

        // Only check if all are integer types (not enum, not message)
        if (fields.stream().anyMatch(fv -> fv.field().isEnum() || fv.field().isMessage())) {
            return false;
        }

        // 32-bit integer type variants
        boolean hasInt32 = protoTypes.contains(Type.TYPE_INT32);
        boolean hasUint32 = protoTypes.contains(Type.TYPE_UINT32);
        boolean hasSint32 = protoTypes.contains(Type.TYPE_SINT32);
        boolean hasFixed32 = protoTypes.contains(Type.TYPE_FIXED32);
        boolean hasSfixed32 = protoTypes.contains(Type.TYPE_SFIXED32);

        // 64-bit integer type variants
        boolean hasInt64 = protoTypes.contains(Type.TYPE_INT64);
        boolean hasUint64 = protoTypes.contains(Type.TYPE_UINT64);
        boolean hasSint64 = protoTypes.contains(Type.TYPE_SINT64);
        boolean hasFixed64 = protoTypes.contains(Type.TYPE_FIXED64);
        boolean hasSfixed64 = protoTypes.contains(Type.TYPE_SFIXED64);

        // Check for 32-bit signed/unsigned conflicts
        // Signed types: int32, sint32, sfixed32
        // Unsigned types: uint32, fixed32
        boolean has32BitSigned = hasInt32 || hasSint32 || hasSfixed32;
        boolean has32BitUnsigned = hasUint32 || hasFixed32;
        if (has32BitSigned && has32BitUnsigned) {
            return true;
        }

        // Check for 64-bit signed/unsigned conflicts
        // Signed types: int64, sint64, sfixed64
        // Unsigned types: uint64, fixed64
        boolean has64BitSigned = hasInt64 || hasSint64 || hasSfixed64;
        boolean has64BitUnsigned = hasUint64 || hasFixed64;
        if (has64BitSigned && has64BitUnsigned) {
            return true;
        }

        // Check for encoding conflicts within same signedness
        // sint32 uses ZigZag encoding, int32 uses varint - different wire formats
        // This can cause issues when parsing cross-version data
        Set<Type> signedTypes32 = new HashSet<>();
        if (hasInt32) signedTypes32.add(Type.TYPE_INT32);
        if (hasSint32) signedTypes32.add(Type.TYPE_SINT32);
        if (hasSfixed32) signedTypes32.add(Type.TYPE_SFIXED32);
        if (signedTypes32.size() > 1) {
            return true;
        }

        Set<Type> signedTypes64 = new HashSet<>();
        if (hasInt64) signedTypes64.add(Type.TYPE_INT64);
        if (hasSint64) signedTypes64.add(Type.TYPE_SINT64);
        if (hasSfixed64) signedTypes64.add(Type.TYPE_SFIXED64);
        if (signedTypes64.size() > 1) {
            return true;
        }

        return false;
    }

    /**
     * Check if fields have a repeated/singular conflict.
     * This occurs when some versions have repeated label and others have singular.
     */
    private boolean hasRepeatedSingleConflict(List<FieldWithVersion> fields) {
        if (fields.size() < 2) {
            return false;
        }

        boolean hasRepeated = fields.stream().anyMatch(fv -> fv.field().isRepeated());
        boolean hasSingular = fields.stream().anyMatch(fv -> !fv.field().isRepeated());

        // Conflict exists if some versions are repeated and some are singular
        // Also must not be a map in any version (maps have their own handling)
        boolean hasMap = fields.stream().anyMatch(fv -> fv.field().isMap());
        return hasRepeated && hasSingular && !hasMap;
    }

    /**
     * Check if fields have an optional/required conflict.
     * This occurs when some versions mark the field as optional and others as required.
     */
    private boolean hasOptionalRequiredConflict(List<FieldWithVersion> fields) {
        if (fields.size() < 2) {
            return false;
        }

        boolean hasOptional = fields.stream().anyMatch(fv -> fv.field().isOptional());
        boolean hasRequired = fields.stream().anyMatch(fv -> !fv.field().isOptional());

        return hasOptional && hasRequired;
    }

    /**
     * Determine the element type for REPEATED_SINGLE conflicts.
     * Returns the primitive/base type that should be used as List element type.
     */
    private String determineElementType(List<FieldWithVersion> fields) {
        // Prefer the type from the singular field (it's the base type)
        for (FieldWithVersion fv : fields) {
            if (!fv.field().isRepeated()) {
                return fv.field().getJavaType();
            }
        }
        // Fall back to extracting element type from repeated field
        for (FieldWithVersion fv : fields) {
            if (fv.field().isRepeated()) {
                return extractElementType(fv.field().getJavaType());
            }
        }
        // Should never reach here
        return "Object";
    }

    /**
     * Box a primitive type name to its wrapper type name.
     */
    private String boxType(String type) {
        return TypeNormalizer.toWrapper(type);
    }

    /**
     * Determine the wider type for WIDENING conflicts.
     * Priority: double > float > long > int
     */
    private String determineWiderType(Set<String> types) {
        // Extract element types in case of repeated fields
        Set<String> elementTypes = types.stream()
                .map(this::extractElementType)
                .collect(Collectors.toSet());

        // Check for double (widest floating point)
        for (String type : elementTypes) {
            if (isDoubleOnlyType(type)) {
                return "double";
            }
        }
        // Check for float
        for (String type : elementTypes) {
            if (isFloatType(type)) {
                // If there's also double, use double; otherwise float
                return elementTypes.stream().anyMatch(this::isDoubleOnlyType) ? "double" : "float";
            }
        }
        // Check for long (wider than int)
        for (String type : elementTypes) {
            if (isLongType(type)) {
                return "long";
            }
        }
        // Default to int (narrowest)
        for (String type : elementTypes) {
            if (isIntType(type)) {
                return "int";
            }
        }
        return null;
    }

    private MergedEnum mergeEnum(String enumName, List<VersionSchema> schemas) {
        MergedEnum merged = new MergedEnum(enumName);

        // Collect values and add versions using streams
        Map<Integer, List<EnumValueWithVersion>> valuesByNumber = schemas.stream()
                .flatMap(schema -> schema.getEnum(enumName).stream()
                        .peek(e -> {
                            merged.addVersion(schema.getVersion());
                            if (e.getSourceFileName() != null) {
                                merged.addSourceFile(schema.getVersion(), e.getSourceFileName());
                            }
                        })
                        .flatMap(enumInfo -> enumInfo.getValues().stream()
                                .map(value -> new EnumValueWithVersion(value, schema.getVersion()))))
                .collect(Collectors.groupingBy(
                        evv -> evv.value().getNumber(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Merge enum values
        valuesByNumber.values().stream()
                .filter(values -> !values.isEmpty())
                .forEach(values -> {
                    EnumValueWithVersion first = values.get(0);
                    MergedEnumValue mergedValue = new MergedEnumValue(first.value(), first.version());
                    values.stream()
                            .skip(1)
                            .map(EnumValueWithVersion::version)
                            .forEach(mergedValue::addVersion);
                    merged.addValue(mergedValue);
                });

        return merged.getPresentInVersions().isEmpty() ? null : merged;
    }

    // Helper records (Java 17+)
    private record FieldWithVersion(FieldInfo field, String version) {}
    private record EnumValueWithVersion(EnumInfo.EnumValue value, String version) {}

    /**
     * Configuration for the merger.
     */
    public static class MergerConfig {
        private final Map<String, String> fieldNameMappings = new HashMap<>();
        private final Map<String, String> typeConflictResolutions = new HashMap<>();
        private final Set<String> excludedMessages = new HashSet<>();
        private final Set<String> excludedFields = new HashSet<>();

        /**
         * Add a field name mapping (for typos or renames).
         *
         * @param protoName Original proto name
         * @param javaName Desired Java name
         * @return this configuration for method chaining
         */
        public MergerConfig addFieldNameMapping(String protoName, String javaName) {
            fieldNameMappings.put(protoName, javaName);
            return this;
        }

        /**
         * Add a type conflict resolution.
         *
         * @param fieldName Field name
         * @param resolvedType Type to use when conflict occurs
         * @return this configuration for method chaining
         */
        public MergerConfig addTypeConflictResolution(String fieldName, String resolvedType) {
            typeConflictResolutions.put(fieldName, resolvedType);
            return this;
        }

        /**
         * Exclude a message from generation.
         *
         * @param messageName Message name or pattern
         * @return this configuration for method chaining
         */
        public MergerConfig excludeMessage(String messageName) {
            excludedMessages.add(messageName);
            return this;
        }

        /**
         * Exclude a field from generation.
         *
         * @param fieldName Field name (format: "MessageName.fieldName")
         * @return this configuration for method chaining
         */
        public MergerConfig excludeField(String fieldName) {
            excludedFields.add(fieldName);
            return this;
        }

        /**
         * Get the Java name mapping for a proto field name.
         *
         * @param protoName the proto field name
         * @return the mapped Java name, or null if no mapping exists
         */
        public String getJavaName(String protoName) {
            return fieldNameMappings.getOrDefault(protoName, null);
        }

        /**
         * Resolve a type conflict for a field.
         *
         * @param fieldName the field name
         * @param type1 the first type
         * @param type2 the second type
         * @return the resolved type, or null if no resolution configured
         */
        public String resolveTypeConflict(String fieldName, String type1, String type2) {
            return typeConflictResolutions.get(fieldName);
        }

        /**
         * Check if a message is excluded from generation.
         *
         * @param messageName the message name to check
         * @return true if the message is excluded
         */
        public boolean isMessageExcluded(String messageName) {
            return excludedMessages.contains(messageName);
        }

        /**
         * Check if a field is excluded from generation.
         *
         * @param messageName the message name
         * @param fieldName the field name
         * @return true if the field is excluded
         */
        public boolean isFieldExcluded(String messageName, String fieldName) {
            return excludedFields.contains(messageName + "." + fieldName);
        }
    }
}
