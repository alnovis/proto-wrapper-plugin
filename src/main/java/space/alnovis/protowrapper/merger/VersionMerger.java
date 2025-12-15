package space.alnovis.protowrapper.merger;

import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.model.*;
import space.alnovis.protowrapper.model.MergedSchema.*;

import java.util.*;

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

    public VersionMerger() {
        this(new MergerConfig());
    }

    public VersionMerger(MergerConfig config) {
        this.config = config;
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

        List<String> versions = new ArrayList<>();
        for (VersionSchema schema : schemas) {
            versions.add(schema.getVersion());
        }

        MergedSchema merged = new MergedSchema(versions);

        // Collect all message names across versions
        Set<String> allMessageNames = new LinkedHashSet<>();
        for (VersionSchema schema : schemas) {
            allMessageNames.addAll(schema.getMessageNames());
        }

        // Merge each message
        for (String messageName : allMessageNames) {
            MergedMessage mergedMessage = mergeMessage(messageName, schemas);
            if (mergedMessage != null) {
                merged.addMessage(mergedMessage);
            }
        }

        // Collect and merge enums
        Set<String> allEnumNames = new LinkedHashSet<>();
        for (VersionSchema schema : schemas) {
            allEnumNames.addAll(schema.getEnumNames());
        }

        for (String enumName : allEnumNames) {
            MergedEnum mergedEnum = mergeEnum(enumName, schemas);
            if (mergedEnum != null) {
                merged.addEnum(mergedEnum);
            }
        }

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
        // Collect all top-level enum infos for comparison
        Map<String, EnumInfo> topLevelEnumInfos = new LinkedHashMap<>();
        for (VersionSchema schema : schemas) {
            for (String enumName : schema.getEnumNames()) {
                Optional<EnumInfo> enumOpt = schema.getEnum(enumName);
                enumOpt.ifPresent(e -> topLevelEnumInfos.putIfAbsent(enumName, e));
            }
        }

        // Check each message's nested enums for equivalence with top-level enums
        for (MergedMessage message : merged.getMessages()) {
            detectEquivalentEnumsInMessage(message, topLevelEnumInfos, merged, schemas);
        }
    }

    /**
     * Recursively check message and its nested messages for equivalent enums.
     */
    private void detectEquivalentEnumsInMessage(MergedMessage message,
                                                 Map<String, EnumInfo> topLevelEnumInfos,
                                                 MergedSchema merged,
                                                 List<VersionSchema> schemas) {
        // Get nested enums to check
        List<MergedEnum> nestedEnumsToRemove = new ArrayList<>();

        for (MergedEnum nestedEnum : message.getNestedEnums()) {
            String nestedEnumName = nestedEnum.getName();

            // Check if there's a top-level enum with the same name
            if (topLevelEnumInfos.containsKey(nestedEnumName)) {
                EnumInfo topLevelInfo = topLevelEnumInfos.get(nestedEnumName);

                // Find the nested enum's EnumInfo to compare
                EnumInfo nestedInfo = findNestedEnumInfo(message.getName(), nestedEnumName, schemas);

                if (nestedInfo != null && nestedInfo.isEquivalentTo(topLevelInfo)) {
                    // They are equivalent - register mapping and mark for removal
                    String nestedPath = message.getQualifiedInterfaceName() + "." + nestedEnumName;
                    merged.addEquivalentEnumMapping(nestedPath, nestedEnumName);
                    nestedEnumsToRemove.add(nestedEnum);

                    System.out.println("Detected equivalent enums: " + nestedPath + " -> " + nestedEnumName);
                }
            }
        }

        // Remove equivalent nested enums from the message
        for (MergedEnum toRemove : nestedEnumsToRemove) {
            message.removeNestedEnum(toRemove);
        }

        // Recursively check nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            detectEquivalentEnumsInMessage(nested, topLevelEnumInfos, merged, schemas);
        }
    }

    /**
     * Find the EnumInfo for a nested enum by searching through version schemas.
     */
    private EnumInfo findNestedEnumInfo(String messageName, String enumName, List<VersionSchema> schemas) {
        for (VersionSchema schema : schemas) {
            Optional<MessageInfo> messageOpt = schema.getMessage(messageName);
            if (messageOpt.isPresent()) {
                for (EnumInfo nestedEnum : messageOpt.get().getNestedEnums()) {
                    if (nestedEnum.getName().equals(enumName)) {
                        return nestedEnum;
                    }
                }
            }
        }
        return null;
    }

    private MergedMessage mergeMessage(String messageName, List<VersionSchema> schemas) {
        MergedMessage merged = new MergedMessage(messageName);

        // Collect fields from all versions
        Map<Integer, List<FieldWithVersion>> fieldsByNumber = new LinkedHashMap<>();

        // Collect nested message and enum names across all versions to avoid duplicates
        Set<String> processedNestedMessages = new LinkedHashSet<>();
        Set<String> processedNestedEnums = new LinkedHashSet<>();

        for (VersionSchema schema : schemas) {
            Optional<MessageInfo> messageOpt = schema.getMessage(messageName);
            if (messageOpt.isPresent()) {
                MessageInfo message = messageOpt.get();
                merged.addVersion(schema.getVersion());
                // Store source file name for outer class detection
                if (message.getSourceFileName() != null) {
                    merged.addSourceFile(schema.getVersion(), message.getSourceFileName());
                }

                for (FieldInfo field : message.getFields()) {
                    fieldsByNumber
                            .computeIfAbsent(field.getNumber(), k -> new ArrayList<>())
                            .add(new FieldWithVersion(field, schema.getVersion()));
                }

                // Collect nested message names (don't process yet)
                for (MessageInfo nested : message.getNestedMessages()) {
                    processedNestedMessages.add(nested.getName());
                }

                // Collect nested enum names
                for (EnumInfo nestedEnum : message.getNestedEnums()) {
                    processedNestedEnums.add(nestedEnum.getName());
                }
            }
        }

        // Now merge each unique nested message across all versions
        for (String nestedName : processedNestedMessages) {
            List<VersionSchema> nestedSchemas = new ArrayList<>();
            for (VersionSchema s : schemas) {
                Optional<MessageInfo> parentOpt = s.getMessage(messageName);
                if (parentOpt.isPresent()) {
                    for (MessageInfo n : parentOpt.get().getNestedMessages()) {
                        if (n.getName().equals(nestedName)) {
                            VersionSchema nestedSchema = new VersionSchema(s.getVersion());
                            nestedSchema.addMessage(n);
                            nestedSchemas.add(nestedSchema);
                            break;
                        }
                    }
                }
            }
            if (!nestedSchemas.isEmpty()) {
                MergedMessage nestedMerged = mergeMessage(nestedName, nestedSchemas);
                if (nestedMerged != null) {
                    merged.addNestedMessage(nestedMerged);
                }
            }
        }

        // Merge nested enums
        for (String enumName : processedNestedEnums) {
            List<VersionSchema> enumSchemas = new ArrayList<>();
            for (VersionSchema s : schemas) {
                Optional<MessageInfo> parentOpt = s.getMessage(messageName);
                if (parentOpt.isPresent()) {
                    for (EnumInfo e : parentOpt.get().getNestedEnums()) {
                        if (e.getName().equals(enumName)) {
                            // Create a temporary schema with just this enum
                            VersionSchema enumSchema = new VersionSchema(s.getVersion());
                            enumSchema.addEnum(e);
                            enumSchemas.add(enumSchema);
                            break;
                        }
                    }
                }
            }
            if (!enumSchemas.isEmpty()) {
                MergedEnum mergedEnum = mergeEnum(enumName, enumSchemas);
                if (mergedEnum != null) {
                    merged.addNestedEnum(mergedEnum);
                }
            }
        }

        // Merge fields with same number
        for (Map.Entry<Integer, List<FieldWithVersion>> entry : fieldsByNumber.entrySet()) {
            MergedField mergedField = mergeFields(entry.getValue());
            if (mergedField != null) {
                merged.addField(mergedField);
            }
        }

        return merged.getPresentInVersions().isEmpty() ? null : merged;
    }

    private MergedField mergeFields(List<FieldWithVersion> fields) {
        if (fields.isEmpty()) {
            return null;
        }

        // Use first field as base
        FieldWithVersion first = fields.get(0);
        MergedField merged = new MergedField(first.field, first.version);

        // Add other versions
        for (int i = 1; i < fields.size(); i++) {
            FieldWithVersion fv = fields.get(i);

            // Check for type conflicts
            if (!first.field.getJavaType().equals(fv.field.getJavaType())) {
                // Type conflict - check if it's in allowed mappings
                String resolvedType = config.resolveTypeConflict(
                        first.field.getProtoName(),
                        first.field.getJavaType(),
                        fv.field.getJavaType()
                );

                if (resolvedType == null) {
                    // Log warning but continue - use Object type
                    System.err.printf("Warning: Type conflict for field '%s': %s vs %s%n",
                            first.field.getProtoName(),
                            first.field.getJavaType(),
                            fv.field.getJavaType());
                }
            }

            merged.addVersion(fv.version, fv.field);
        }

        return merged;
    }

    private MergedEnum mergeEnum(String enumName, List<VersionSchema> schemas) {
        MergedEnum merged = new MergedEnum(enumName);

        Map<Integer, List<EnumValueWithVersion>> valuesByNumber = new LinkedHashMap<>();

        for (VersionSchema schema : schemas) {
            Optional<EnumInfo> enumOpt = schema.getEnum(enumName);
            if (enumOpt.isPresent()) {
                EnumInfo enumInfo = enumOpt.get();
                merged.addVersion(schema.getVersion());

                for (EnumInfo.EnumValue value : enumInfo.getValues()) {
                    valuesByNumber
                            .computeIfAbsent(value.getNumber(), k -> new ArrayList<>())
                            .add(new EnumValueWithVersion(value, schema.getVersion()));
                }
            }
        }

        // Merge enum values
        for (List<EnumValueWithVersion> values : valuesByNumber.values()) {
            if (!values.isEmpty()) {
                EnumValueWithVersion first = values.get(0);
                MergedEnumValue mergedValue = new MergedEnumValue(first.value, first.version);

                for (int i = 1; i < values.size(); i++) {
                    mergedValue.addVersion(values.get(i).version);
                }

                merged.addValue(mergedValue);
            }
        }

        return merged.getPresentInVersions().isEmpty() ? null : merged;
    }

    // Helper classes
    private static class FieldWithVersion {
        final FieldInfo field;
        final String version;

        FieldWithVersion(FieldInfo field, String version) {
            this.field = field;
            this.version = version;
        }
    }

    private static class EnumValueWithVersion {
        final EnumInfo.EnumValue value;
        final String version;

        EnumValueWithVersion(EnumInfo.EnumValue value, String version) {
            this.value = value;
            this.version = version;
        }
    }

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
