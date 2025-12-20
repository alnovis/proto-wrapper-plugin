package space.alnovis.protowrapper.model;

import java.util.*;

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

    public String getName() {
        return name;
    }

    public List<MergedField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public List<MergedMessage> getNestedMessages() {
        return Collections.unmodifiableList(nestedMessages);
    }

    public List<MergedEnum> getNestedEnums() {
        return Collections.unmodifiableList(nestedEnums);
    }

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    public String getInterfaceName() {
        return name;
    }

    public String getAbstractClassName() {
        return "Abstract" + name;
    }

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
