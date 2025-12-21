package space.alnovis.protowrapper.merger;

import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
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

    public VersionMerger() {
        this(new MergerConfig(), PluginLogger.console());
    }

    public VersionMerger(MergerConfig config) {
        this(config, PluginLogger.console());
    }

    public VersionMerger(PluginLogger logger) {
        this(new MergerConfig(), logger);
    }

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
        for (VersionSchema schema : schemas) {
            if (!schema.getVersion().equals(version)) continue;

            Optional<MessageInfo> msgOpt = schema.getMessage(messageName);
            if (msgOpt.isPresent()) {
                MessageInfo msg = msgOpt.get();
                for (EnumInfo nestedEnum : msg.getNestedEnums()) {
                    if (nestedEnum.getName().equals(enumTypeName)) {
                        return nestedEnum;
                    }
                }
            }
        }

        // Try to find as a top-level enum
        for (VersionSchema schema : schemas) {
            if (!schema.getVersion().equals(version)) continue;

            Optional<EnumInfo> enumOpt = schema.getEnum(enumTypeName);
            if (enumOpt.isPresent()) {
                return enumOpt.get();
            }
        }

        return null;
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

        return merged.getPresentInVersions().isEmpty() ? null : merged;
    }

    private MergedField mergeFields(List<FieldWithVersion> fields) {
        if (fields.isEmpty()) {
            return null;
        }

        // Build merged field using builder for proper conflict handling
        MergedField.Builder builder = MergedField.builder();

        // Add all version fields
        fields.forEach(fv -> builder.addVersionField(fv.version(), fv.field()));

        // Collect unique types
        Set<String> uniqueTypes = fields.stream()
                .map(fv -> fv.field().getJavaType())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Detect conflict type if multiple types exist
        if (uniqueTypes.size() > 1) {
            FieldWithVersion first = fields.get(0);
            MergedField.ConflictType conflictType = detectConflictType(fields);
            builder.conflictType(conflictType);

            // For WIDENING conflicts, set the resolved type to the wider type
            if (conflictType == MergedField.ConflictType.WIDENING) {
                String widerType = determineWiderType(uniqueTypes);
                if (widerType != null) {
                    builder.resolvedJavaType(widerType);
                    builder.resolvedGetterType(widerType);
                }
            }

            // Log warning for conflicts
            String typesStr = fields.stream()
                    .map(fv -> fv.version() + ":" + fv.field().getJavaType())
                    .collect(Collectors.joining(", "));
            logger.warn(String.format("Type conflict for field '%s' [%s]: %s",
                    first.field().getProtoName(), conflictType, typesStr));
        }

        return builder.build();
    }

    /**
     * Detect the type of conflict between field types across versions.
     */
    private MergedField.ConflictType detectConflictType(List<FieldWithVersion> fields) {
        Set<String> types = fields.stream()
                .map(fv -> fv.field().getJavaType())
                .collect(Collectors.toSet());

        if (types.size() == 1) {
            return MergedField.ConflictType.NONE;
        }

        // Collect field characteristics
        boolean hasInt = types.stream().anyMatch(this::isIntType);
        boolean hasLong = types.stream().anyMatch(this::isLongType);
        boolean hasDouble = types.stream().anyMatch(this::isDoubleType);
        boolean hasEnum = fields.stream().anyMatch(fv -> fv.field().isEnum());
        boolean hasMessage = fields.stream().anyMatch(fv -> fv.field().isMessage());
        boolean hasPrimitive = fields.stream().anyMatch(fv -> fv.field().isPrimitive());
        boolean hasString = types.contains("String");
        boolean hasBytes = types.contains("byte[]") || types.contains("ByteString");

        // Check for int ↔ enum conflict
        if (hasInt && hasEnum && types.size() == 2) {
            return MergedField.ConflictType.INT_ENUM;
        }

        // Check for widening: int → long, int → double
        if ((hasInt && hasLong && !hasDouble && !hasMessage && !hasEnum) ||
            (hasInt && hasDouble && !hasLong && !hasMessage && !hasEnum)) {
            return MergedField.ConflictType.WIDENING;
        }

        // Check for narrowing: long → int (if long comes first and int later, or vice versa)
        // This is lossy, so we mark it as NARROWING
        if (hasLong && hasInt && !hasDouble && !hasMessage && !hasEnum) {
            // Already handled as WIDENING above if int→long
            // If long→int pattern exists, it would still be WIDENING (use long)
            return MergedField.ConflictType.WIDENING;
        }

        // Check for string ↔ bytes
        if (hasString && hasBytes && types.size() == 2) {
            return MergedField.ConflictType.STRING_BYTES;
        }

        // Check for primitive ↔ message conflict
        if (hasPrimitive && hasMessage) {
            return MergedField.ConflictType.PRIMITIVE_MESSAGE;
        }

        // Default: incompatible types
        return MergedField.ConflictType.INCOMPATIBLE;
    }

    private boolean isIntType(String type) {
        return "int".equals(type) || "Integer".equals(type) || "int32".equals(type) || "uint32".equals(type);
    }

    private boolean isLongType(String type) {
        return "long".equals(type) || "Long".equals(type) || "int64".equals(type) || "uint64".equals(type);
    }

    private boolean isDoubleType(String type) {
        return "double".equals(type) || "Double".equals(type) || "float".equals(type) || "Float".equals(type);
    }

    /**
     * Determine the wider type for WIDENING conflicts.
     * Priority: double > long > int
     */
    private String determineWiderType(Set<String> types) {
        // Check for double/float (widest)
        for (String type : types) {
            if (isDoubleType(type)) {
                return "double";
            }
        }
        // Check for long (wider than int)
        for (String type : types) {
            if (isLongType(type)) {
                return "long";
            }
        }
        // Default to int (narrowest)
        for (String type : types) {
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
         */
        public MergerConfig addTypeConflictResolution(String fieldName, String resolvedType) {
            typeConflictResolutions.put(fieldName, resolvedType);
            return this;
        }

        /**
         * Exclude a message from generation.
         *
         * @param messageName Message name or pattern
         */
        public MergerConfig excludeMessage(String messageName) {
            excludedMessages.add(messageName);
            return this;
        }

        /**
         * Exclude a field from generation.
         *
         * @param fieldName Field name (format: "MessageName.fieldName")
         */
        public MergerConfig excludeField(String fieldName) {
            excludedFields.add(fieldName);
            return this;
        }

        public String getJavaName(String protoName) {
            return fieldNameMappings.getOrDefault(protoName, null);
        }

        public String resolveTypeConflict(String fieldName, String type1, String type2) {
            return typeConflictResolutions.get(fieldName);
        }

        public boolean isMessageExcluded(String messageName) {
            return excludedMessages.contains(messageName);
        }

        public boolean isFieldExcluded(String messageName, String fieldName) {
            return excludedFields.contains(messageName + "." + fieldName);
        }
    }
}
