package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents the merged schema from multiple protobuf versions.
 * Contains unified message definitions that work across all versions.
 */
public class MergedSchema {

    private final List<String> versions;
    private final Map<String, MergedMessage> messages;
    private final Map<String, MergedEnum> enums;
    // Maps nested enum path to equivalent top-level enum name
    // E.g., "Product.CategoryEnum" -> "CategoryEnum"
    private final Map<String, String> equivalentEnumMappings;

    public MergedSchema(List<String> versions) {
        this.versions = new ArrayList<>(versions);
        this.messages = new LinkedHashMap<>();
        this.enums = new LinkedHashMap<>();
        this.equivalentEnumMappings = new LinkedHashMap<>();
    }

    /**
     * Register an equivalent enum mapping (nested -> top-level).
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @param topLevelName Simple name like "CategoryEnum"
     */
    public void addEquivalentEnumMapping(String nestedPath, String topLevelName) {
        equivalentEnumMappings.put(nestedPath, topLevelName);
    }

    /**
     * Check if a nested enum has an equivalent top-level enum.
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @return true if there's an equivalent top-level enum
     */
    public boolean hasEquivalentTopLevelEnum(String nestedPath) {
        return equivalentEnumMappings.containsKey(nestedPath);
    }

    /**
     * Get the top-level enum name for a nested enum path.
     * @param nestedPath Full path like "Product.CategoryEnum"
     * @return Top-level enum name or null if no mapping exists
     */
    public String getEquivalentTopLevelEnum(String nestedPath) {
        return equivalentEnumMappings.get(nestedPath);
    }

    /**
     * Get all equivalent enum mappings.
     */
    public Map<String, String> getEquivalentEnumMappings() {
        return Collections.unmodifiableMap(equivalentEnumMappings);
    }

    public void addMessage(MergedMessage message) {
        messages.put(message.getName(), message);
    }

    public void addEnum(MergedEnum enumInfo) {
        enums.put(enumInfo.getName(), enumInfo);
    }

    public Optional<MergedMessage> getMessage(String name) {
        return Optional.ofNullable(messages.get(name));
    }

    public Optional<MergedEnum> getEnum(String name) {
        return Optional.ofNullable(enums.get(name));
    }

    public List<String> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public Collection<MergedMessage> getMessages() {
        return Collections.unmodifiableCollection(messages.values());
    }

    public Collection<MergedEnum> getEnums() {
        return Collections.unmodifiableCollection(enums.values());
    }

    /**
     * Find a nested message by path like "Order.ShippingInfo".
     * @param nestedPath Path like "ParentMessage.NestedMessage"
     * @return The nested message if found
     */
    public Optional<MergedMessage> findMessageByPath(String nestedPath) {
        if (nestedPath == null || nestedPath.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = nestedPath.split("\\.");
        if (parts.length == 0) {
            return Optional.empty();
        }

        // Find the top-level message
        MergedMessage current = messages.get(parts[0]);
        if (current == null) {
            return Optional.empty();
        }

        // Navigate through nested messages
        for (int i = 1; i < parts.length; i++) {
            Optional<MergedMessage> nested = current.findNestedMessage(parts[i]);
            if (!nested.isPresent()) {
                return Optional.empty();
            }
            current = nested.get();
        }

        return Optional.of(current);
    }

    /**
     * Find a nested enum by path like "Order.Item.ItemTypeEnum".
     * @param nestedPath Path like "ParentMessage.NestedEnum"
     * @return The nested enum if found
     */
    public Optional<MergedEnum> findEnumByPath(String nestedPath) {
        if (nestedPath == null || nestedPath.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = nestedPath.split("\\.");
        if (parts.length < 2) {
            // No path, check top-level enums
            return getEnum(nestedPath);
        }

        // Navigate to parent message
        MergedMessage parent = messages.get(parts[0]);
        if (parent == null) {
            return Optional.empty();
        }

        // Navigate through nested messages until we reach the enum's parent
        for (int i = 1; i < parts.length - 1; i++) {
            Optional<MergedMessage> nested = parent.findNestedMessage(parts[i]);
            if (!nested.isPresent()) {
                return Optional.empty();
            }
            parent = nested.get();
        }

        // Find the enum in the last parent
        return parent.findNestedEnum(parts[parts.length - 1]);
    }

    /**
     * Represents a merged message from multiple versions.
     */
    public static class MergedMessage {
        private final String name;
        private final List<MergedField> fields;
        private final List<MergedMessage> nestedMessages;
        private final List<MergedEnum> nestedEnums;
        private final Set<String> presentInVersions;
        private final Map<String, String> versionSourceFiles; // version -> source file name
        private MergedMessage parent; // Parent message for nested types

        public MergedMessage(String name) {
            this.name = name;
            this.fields = new ArrayList<>();
            this.nestedMessages = new ArrayList<>();
            this.nestedEnums = new ArrayList<>();
            this.presentInVersions = new LinkedHashSet<>();
            this.versionSourceFiles = new LinkedHashMap<>();
            this.parent = null;
        }

        public void addField(MergedField field) {
            fields.add(field);
        }

        public void addNestedMessage(MergedMessage nested) {
            nested.setParent(this);
            nestedMessages.add(nested);
        }

        public void addNestedEnum(MergedEnum nestedEnum) {
            nestedEnums.add(nestedEnum);
        }

        /**
         * Remove a nested enum (used when merging equivalent enums).
         */
        public void removeNestedEnum(MergedEnum nestedEnum) {
            nestedEnums.remove(nestedEnum);
        }

        public void setParent(MergedMessage parent) {
            this.parent = parent;
        }

        public MergedMessage getParent() {
            return parent;
        }

        public boolean isNested() {
            return parent != null;
        }

        public void addVersion(String version) {
            presentInVersions.add(version);
        }

        public void addSourceFile(String version, String sourceFileName) {
            versionSourceFiles.put(version, sourceFileName);
        }

        public String getSourceFile(String version) {
            return versionSourceFiles.get(version);
        }

        /**
         * Get the outer class name for a specific version.
         * E.g., for source file "common.proto" returns "Common".
         */
        public String getOuterClassName(String version) {
            String sourceFileName = versionSourceFiles.get(version);
            if (sourceFileName == null) {
                return null;
            }
            // Remove path prefix if present (e.g., "v1/common.proto" -> "common.proto")
            int lastSlash = sourceFileName.lastIndexOf('/');
            if (lastSlash >= 0) {
                sourceFileName = sourceFileName.substring(lastSlash + 1);
            }
            // Remove .proto extension
            if (sourceFileName.endsWith(".proto")) {
                sourceFileName = sourceFileName.substring(0, sourceFileName.length() - 6);
            }
            // Convert snake_case to PascalCase
            return toPascalCase(sourceFileName);
        }

        private String toPascalCase(String snakeCase) {
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;
            for (char c : snakeCase.toCharArray()) {
                if (c == '_') {
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }

        public String getName() { return name; }
        public List<MergedField> getFields() { return Collections.unmodifiableList(fields); }
        public List<MergedMessage> getNestedMessages() { return Collections.unmodifiableList(nestedMessages); }
        public List<MergedEnum> getNestedEnums() { return Collections.unmodifiableList(nestedEnums); }
        public Set<String> getPresentInVersions() { return Collections.unmodifiableSet(presentInVersions); }

        public String getInterfaceName() { return name; }
        public String getAbstractClassName() { return "Abstract" + name; }
        public String getVersionClassName(String version) {
            return name + version.substring(0, 1).toUpperCase() + version.substring(1);
        }

        /**
         * Get the flattened class name for nested types.
         * E.g., for Report.Section returns "ReportSection".
         */
        public String getFlattenedName() {
            if (parent == null) {
                return name;
            }
            return parent.getFlattenedName() + name;
        }

        /**
         * Get flattened abstract class name.
         * E.g., for Report.Section returns "AbstractReportSection".
         */
        public String getFlattenedAbstractClassName() {
            return "Abstract" + getFlattenedName();
        }

        /**
         * Get flattened version-specific class name.
         * E.g., for Report.Section in v2 returns "ReportSectionV2".
         */
        public String getFlattenedVersionClassName(String version) {
            return getFlattenedName() + version.substring(0, 1).toUpperCase() + version.substring(1);
        }

        /**
         * Get the full qualified interface name including parent hierarchy.
         * E.g., for Tax nested in Order returns "Order.Tax"
         */
        public String getQualifiedInterfaceName() {
            if (parent == null) {
                return name;
            }
            return parent.getQualifiedInterfaceName() + "." + name;
        }

        /**
         * Get the top-level parent message (root of the nesting hierarchy).
         */
        public MergedMessage getTopLevelParent() {
            if (parent == null) {
                return this;
            }
            return parent.getTopLevelParent();
        }

        /**
         * Find a nested message by name (direct children only).
         */
        public Optional<MergedMessage> findNestedMessage(String nestedName) {
            return nestedMessages.stream()
                    .filter(m -> m.getName().equals(nestedName))
                    .findFirst();
        }

        /**
         * Find a nested message by name recursively (checks all descendants).
         */
        public Optional<MergedMessage> findNestedMessageRecursive(String nestedName) {
            for (MergedMessage nested : nestedMessages) {
                if (nested.getName().equals(nestedName)) {
                    return Optional.of(nested);
                }
                Optional<MergedMessage> found = nested.findNestedMessageRecursive(nestedName);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        /**
         * Find a nested enum by name (direct children only).
         */
        public Optional<MergedEnum> findNestedEnum(String enumName) {
            return nestedEnums.stream()
                    .filter(e -> e.getName().equals(enumName))
                    .findFirst();
        }

        /**
         * Find a nested enum by name recursively (checks all descendants).
         */
        public Optional<MergedEnum> findNestedEnumRecursive(String enumName) {
            // Check direct nested enums
            for (MergedEnum nestedEnum : nestedEnums) {
                if (nestedEnum.getName().equals(enumName)) {
                    return Optional.of(nestedEnum);
                }
            }
            // Check enums in nested messages
            for (MergedMessage nested : nestedMessages) {
                Optional<MergedEnum> found = nested.findNestedEnumRecursive(enumName);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        /**
         * Check if this message has any nested types (messages or enums).
         */
        public boolean hasNestedTypes() {
            return !nestedMessages.isEmpty() || !nestedEnums.isEmpty();
        }

        /**
         * Get fields that exist in all versions.
         */
        public List<MergedField> getCommonFields() {
            List<MergedField> result = new ArrayList<>();
            for (MergedField field : fields) {
                if (field.getPresentInVersions().containsAll(presentInVersions)) {
                    result.add(field);
                }
            }
            return result;
        }

        /**
         * Get fields sorted by field number.
         */
        public List<MergedField> getFieldsSorted() {
            List<MergedField> sorted = new ArrayList<>(fields);
            sorted.sort(Comparator.comparingInt(MergedField::getNumber));
            return sorted;
        }

        @Override
        public String toString() {
            return String.format("MergedMessage[%s, %d fields, versions=%s]",
                name, fields.size(), presentInVersions);
        }
    }

    /**
     * Represents a merged field from multiple versions.
     */
    public static class MergedField {
        private final String name;
        private final String javaName;
        private final int number;
        private final String javaType;
        private final String getterType;
        private final boolean optional;
        private final boolean repeated;
        private final boolean primitive;
        private final boolean message;
        private final boolean isEnum;
        private final Set<String> presentInVersions;
        private final Map<String, FieldInfo> versionFields; // Version -> original field

        public MergedField(FieldInfo field, String version) {
            this.name = field.getProtoName();
            this.javaName = field.getJavaName();
            this.number = field.getNumber();
            this.javaType = field.getJavaType();
            this.getterType = field.getGetterType();
            this.optional = field.isOptional();
            this.repeated = field.isRepeated();
            this.primitive = field.isPrimitive();
            this.message = field.isMessage();
            this.isEnum = field.isEnum();
            this.presentInVersions = new LinkedHashSet<>();
            this.presentInVersions.add(version);
            this.versionFields = new LinkedHashMap<>();
            this.versionFields.put(version, field);
        }

        public void addVersion(String version, FieldInfo field) {
            presentInVersions.add(version);
            versionFields.put(version, field);
        }

        public String getName() { return name; }
        public String getJavaName() { return javaName; }
        public int getNumber() { return number; }
        public String getJavaType() { return javaType; }
        public String getGetterType() { return getterType; }
        public boolean isOptional() { return optional; }
        public boolean isRepeated() { return repeated; }
        public boolean isPrimitive() { return primitive; }
        public boolean isMessage() { return message; }
        public boolean isEnum() { return isEnum; }
        public Set<String> getPresentInVersions() { return Collections.unmodifiableSet(presentInVersions); }
        public Map<String, FieldInfo> getVersionFields() { return Collections.unmodifiableMap(versionFields); }

        /**
         * Get the full nested type path from the proto type name.
         * E.g., for type ".example.proto.v1.Order.ShippingInfo" with package "example.proto.v1"
         * returns "Order.ShippingInfo".
         *
         * @param protoPackage The proto package to strip (e.g., "example.proto.v1")
         * @return Full nested type path or simple name if not nested
         */
        public String getNestedTypePath(String protoPackage) {
            // Get the original field info to access the full type name
            if (versionFields.isEmpty()) {
                return javaType;
            }
            FieldInfo firstField = versionFields.values().iterator().next();
            return firstField.extractNestedTypePath(protoPackage);
        }

        /**
         * Get the raw proto type name from the first version's field info.
         * @return Raw proto type name (e.g., ".example.proto.v1.Order.ShippingInfo")
         */
        public String getRawTypeName() {
            if (versionFields.isEmpty()) {
                return null;
            }
            FieldInfo firstField = versionFields.values().iterator().next();
            return firstField.getTypeName();
        }

        public String getGetterName() {
            String prefix = javaType.equals("boolean") || javaType.equals("Boolean") ? "is" : "get";
            return prefix + capitalize(javaName);
        }

        public String getExtractMethodName() {
            return "extract" + capitalize(javaName);
        }

        public String getExtractHasMethodName() {
            return "extractHas" + capitalize(javaName);
        }

        /**
         * Whether this field needs has-check pattern (optional primitive).
         */
        public boolean needsHasCheck() {
            return optional && primitive;
        }

        /**
         * Whether this field exists in all versions.
         */
        public boolean isUniversal(Set<String> allVersions) {
            return presentInVersions.containsAll(allVersions);
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        @Override
        public String toString() {
            return String.format("MergedField[%s:%s #%d, versions=%s]",
                name, javaType, number, presentInVersions);
        }
    }

    /**
     * Represents a merged enum from multiple versions.
     */
    public static class MergedEnum {
        private final String name;
        private final List<MergedEnumValue> values;
        private final Set<String> presentInVersions;

        public MergedEnum(String name) {
            this.name = name;
            this.values = new ArrayList<>();
            this.presentInVersions = new LinkedHashSet<>();
        }

        public void addValue(MergedEnumValue value) {
            values.add(value);
        }

        public void addVersion(String version) {
            presentInVersions.add(version);
        }

        public String getName() { return name; }
        public List<MergedEnumValue> getValues() { return Collections.unmodifiableList(values); }
        public Set<String> getPresentInVersions() { return Collections.unmodifiableSet(presentInVersions); }

        @Override
        public String toString() {
            return String.format("MergedEnum[%s, %d values]", name, values.size());
        }
    }

    /**
     * Represents a merged enum value.
     */
    public static class MergedEnumValue {
        private final String name;
        private final String javaName;
        private final int number;
        private final Set<String> presentInVersions;

        public MergedEnumValue(EnumInfo.EnumValue value, String version) {
            this.name = value.getName();
            this.javaName = value.getJavaName();
            this.number = value.getNumber();
            this.presentInVersions = new LinkedHashSet<>();
            this.presentInVersions.add(version);
        }

        public void addVersion(String version) {
            presentInVersions.add(version);
        }

        public String getName() { return name; }
        public String getJavaName() { return javaName; }
        public int getNumber() { return number; }
        public Set<String> getPresentInVersions() { return Collections.unmodifiableSet(presentInVersions); }
    }
}
