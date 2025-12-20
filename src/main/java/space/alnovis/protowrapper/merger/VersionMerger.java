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

        return merged;
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

        FieldWithVersion first = fields.get(0);
        MergedField merged = new MergedField(first.field(), first.version());

        // Check for type conflicts and add other versions
        fields.stream()
                .skip(1)
                .peek(fv -> {
                    if (!first.field().getJavaType().equals(fv.field().getJavaType())) {
                        String resolvedType = config.resolveTypeConflict(
                                first.field().getProtoName(),
                                first.field().getJavaType(),
                                fv.field().getJavaType()
                        );
                        if (resolvedType == null) {
                            logger.warn(String.format("Type conflict for field '%s': %s vs %s",
                                    first.field().getProtoName(),
                                    first.field().getJavaType(),
                                    fv.field().getJavaType()));
                        }
                    }
                })
                .forEach(fv -> merged.addVersion(fv.version(), fv.field()));

        return merged;
    }

    private MergedEnum mergeEnum(String enumName, List<VersionSchema> schemas) {
        MergedEnum merged = new MergedEnum(enumName);

        // Collect values and add versions using streams
        Map<Integer, List<EnumValueWithVersion>> valuesByNumber = schemas.stream()
                .flatMap(schema -> schema.getEnum(enumName).stream()
                        .peek(e -> merged.addVersion(schema.getVersion()))
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
