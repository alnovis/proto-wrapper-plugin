package space.alnovis.protowrapper.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a merged message from multiple protocol versions.
 *
 * <p>Contains unified field definitions that work across all versions
 * where this message is present.</p>
 */
public class MergedMessage {

    private final String name;
    private final List<MergedField> fields;
    private final List<MergedMessage> nestedMessages;
    private final List<MergedEnum> nestedEnums;
    private final List<MergedOneof> oneofGroups;
    private final Set<String> presentInVersions;
    private final Map<String, String> versionSourceFiles; // version -> source file name
    private MergedMessage parent; // Parent message for nested types

    /**
     * Create a new MergedMessage.
     *
     * @param name the message name
     */
    public MergedMessage(String name) {
        this.name = name;
        this.fields = new ArrayList<>();
        this.nestedMessages = new ArrayList<>();
        this.nestedEnums = new ArrayList<>();
        this.oneofGroups = new ArrayList<>();
        this.presentInVersions = new LinkedHashSet<>();
        this.versionSourceFiles = new LinkedHashMap<>();
        this.parent = null;
    }

    /**
     * Add a field to this message.
     *
     * @param field the field to add
     */
    public void addField(MergedField field) {
        fields.add(field);
    }

    /**
     * Add a nested message.
     *
     * @param nested the nested message to add
     */
    public void addNestedMessage(MergedMessage nested) {
        nested.setParent(this);
        nestedMessages.add(nested);
    }

    /**
     * Add a nested enum.
     *
     * @param nestedEnum the nested enum to add
     */
    public void addNestedEnum(MergedEnum nestedEnum) {
        nestedEnums.add(nestedEnum);
    }

    /**
     * Add a oneof group.
     *
     * @param oneof the oneof group to add
     */
    public void addOneofGroup(MergedOneof oneof) {
        oneofGroups.add(oneof);
    }

    /**
     * Remove a nested enum (used when merging equivalent enums).
     *
     * @param nestedEnum the nested enum to remove
     */
    public void removeNestedEnum(MergedEnum nestedEnum) {
        nestedEnums.remove(nestedEnum);
    }

    /**
     * Set the parent message.
     *
     * @param parent the parent message
     */
    public void setParent(MergedMessage parent) {
        this.parent = parent;
    }

    /** @return the parent message, or null if top-level */
    public MergedMessage getParent() {
        return parent;
    }

    /** @return true if this is a nested message */
    public boolean isNested() {
        return parent != null;
    }

    /**
     * Add a version where this message is present.
     *
     * @param version the version identifier
     */
    public void addVersion(String version) {
        presentInVersions.add(version);
    }

    /**
     * Add source file for a version.
     *
     * @param version the version identifier
     * @param sourceFileName the source proto file name
     */
    public void addSourceFile(String version, String sourceFileName) {
        versionSourceFiles.put(version, sourceFileName);
    }

    /**
     * Get the source file for a version.
     *
     * @param version the version identifier
     * @return the source file name
     */
    public String getSourceFile(String version) {
        return versionSourceFiles.get(version);
    }

    /**
     * Get the outer class name for a specific version.
     * E.g., for source file "common.proto" returns "Common".
     * If the derived name conflicts with a message name, appends "OuterClass" suffix
     * (matching protobuf's behavior).
     *
     * @param version the version identifier
     * @return the outer class name
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
        String outerClassName = toPascalCase(sourceFileName);

        // If outer class name conflicts with this message name, protobuf adds "OuterClass" suffix
        if (outerClassName.equals(name)) {
            return outerClassName + "OuterClass";
        }
        return outerClassName;
    }

    private String toPascalCase(String snakeCase) {
        return Arrays.stream(snakeCase.split("_"))
                .filter(s -> !s.isEmpty())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }

    /** @return the message name */
    public String getName() {
        return name;
    }

    /** @return unmodifiable list of fields */
    public List<MergedField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /** @return unmodifiable list of nested messages */
    public List<MergedMessage> getNestedMessages() {
        return Collections.unmodifiableList(nestedMessages);
    }

    /** @return unmodifiable list of nested enums */
    public List<MergedEnum> getNestedEnums() {
        return Collections.unmodifiableList(nestedEnums);
    }

    /** @return unmodifiable list of oneof groups */
    public List<MergedOneof> getOneofGroups() {
        return Collections.unmodifiableList(oneofGroups);
    }

    /**
     * Check if this message has any oneof groups.
     *
     * @return true if there are oneof groups
     */
    public boolean hasOneofGroups() {
        return !oneofGroups.isEmpty();
    }

    /**
     * Find oneof group by name.
     *
     * @param name the oneof name
     * @return the oneof group, or empty
     */
    public Optional<MergedOneof> findOneofByName(String name) {
        return oneofGroups.stream()
                .filter(o -> o.getProtoName().equals(name) || o.getJavaName().equals(name))
                .findFirst();
    }

    /**
     * Find oneof group containing a specific field.
     *
     * @param field the field to find
     * @return the oneof group, or empty
     */
    public Optional<MergedOneof> findOneofForField(MergedField field) {
        return oneofGroups.stream()
                .filter(o -> o.containsField(field.getNumber()))
                .findFirst();
    }

    /**
     * Get fields that are NOT part of any oneof group.
     *
     * @return list of non-oneof fields
     */
    public List<MergedField> getNonOneofFields() {
        Set<Integer> oneofFieldNumbers = oneofGroups.stream()
                .flatMap(o -> o.getAllFieldNumbers().stream())
                .collect(Collectors.toSet());
        return fields.stream()
                .filter(f -> !oneofFieldNumbers.contains(f.getNumber()))
                .toList();
    }

    /** @return the set of versions where this message is present */
    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    /** @return the interface name */
    public String getInterfaceName() {
        return name;
    }

    /** @return the abstract class name */
    public String getAbstractClassName() {
        return "Abstract" + name;
    }

    /**
     * Get the version-specific class name.
     *
     * @param version the version identifier
     * @return the version class name
     */
    public String getVersionClassName(String version) {
        return name + version.substring(0, 1).toUpperCase() + version.substring(1);
    }

    /**
     * Get the flattened class name for nested types.
     * E.g., for Report.Section returns "ReportSection".
     *
     * @return the flattened name
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
     *
     * @return the flattened abstract class name
     */
    public String getFlattenedAbstractClassName() {
        return "Abstract" + getFlattenedName();
    }

    /**
     * Get flattened version-specific class name.
     * E.g., for Report.Section in v2 returns "ReportSectionV2".
     *
     * @param version the version identifier
     * @return the flattened version class name
     */
    public String getFlattenedVersionClassName(String version) {
        return getFlattenedName() + version.substring(0, 1).toUpperCase() + version.substring(1);
    }

    /**
     * Get the full qualified interface name including parent hierarchy.
     * E.g., for Tax nested in Order returns "Order.Tax"
     *
     * @return the qualified interface name
     */
    public String getQualifiedInterfaceName() {
        if (parent == null) {
            return name;
        }
        return parent.getQualifiedInterfaceName() + "." + name;
    }

    /**
     * Get the top-level parent message (root of the nesting hierarchy).
     *
     * @return the top-level message
     */
    public MergedMessage getTopLevelParent() {
        if (parent == null) {
            return this;
        }
        return parent.getTopLevelParent();
    }

    /**
     * Find a nested message by name (direct children only).
     *
     * @param nestedName the nested message name
     * @return the nested message, or empty
     */
    public Optional<MergedMessage> findNestedMessage(String nestedName) {
        return nestedMessages.stream()
                .filter(m -> m.getName().equals(nestedName))
                .findFirst();
    }

    /**
     * Find a nested message by name recursively (checks all descendants).
     *
     * @param nestedName the nested message name
     * @return the nested message, or empty
     */
    public Optional<MergedMessage> findNestedMessageRecursive(String nestedName) {
        return nestedMessages.stream()
                .flatMap(nested -> Stream.concat(
                        Stream.of(nested).filter(m -> m.getName().equals(nestedName)),
                        nested.findNestedMessageRecursive(nestedName).stream()))
                .findFirst();
    }

    /**
     * Find a nested enum by name (direct children only).
     *
     * @param enumName the enum name
     * @return the nested enum, or empty
     */
    public Optional<MergedEnum> findNestedEnum(String enumName) {
        return nestedEnums.stream()
                .filter(e -> e.getName().equals(enumName))
                .findFirst();
    }

    /**
     * Find a nested enum by name recursively (checks all descendants).
     *
     * @param enumName the enum name
     * @return the nested enum, or empty
     */
    public Optional<MergedEnum> findNestedEnumRecursive(String enumName) {
        // Check direct nested enums first, then recurse into nested messages
        return Stream.concat(
                nestedEnums.stream().filter(e -> e.getName().equals(enumName)),
                nestedMessages.stream().flatMap(nested -> nested.findNestedEnumRecursive(enumName).stream())
        ).findFirst();
    }

    /**
     * Check if this message has any nested types (messages or enums).
     *
     * @return true if there are nested types
     */
    public boolean hasNestedTypes() {
        return !nestedMessages.isEmpty() || !nestedEnums.isEmpty();
    }

    /**
     * Get fields that exist in all versions.
     *
     * @return list of common fields
     */
    public List<MergedField> getCommonFields() {
        return fields.stream()
                .filter(field -> field.getPresentInVersions().containsAll(presentInVersions))
                .toList();
    }

    /**
     * Get fields sorted by field number.
     *
     * @return list of fields sorted by number
     */
    public List<MergedField> getFieldsSorted() {
        return fields.stream()
                .sorted(Comparator.comparingInt(MergedField::getNumber))
                .toList();
    }

    /**
     * Get map fields.
     *
     * @return list of map fields
     */
    public List<MergedField> getMapFields() {
        return fields.stream()
                .filter(MergedField::isMap)
                .toList();
    }

    /**
     * Get repeated fields (excluding map fields).
     *
     * @return list of repeated fields
     */
    public List<MergedField> getRepeatedFields() {
        return fields.stream()
                .filter(f -> f.isRepeated() && !f.isMap())
                .toList();
    }

    /**
     * Check if this message has any map fields.
     *
     * @return true if there are map fields
     */
    public boolean hasMapFields() {
        return fields.stream().anyMatch(MergedField::isMap);
    }

    @Override
    public String toString() {
        return String.format("MergedMessage[%s, %d fields, versions=%s]",
            name, fields.size(), presentInVersions);
    }
}
