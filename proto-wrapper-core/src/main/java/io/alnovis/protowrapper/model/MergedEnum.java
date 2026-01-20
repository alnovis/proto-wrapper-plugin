package io.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged enum from multiple protocol versions.
 *
 * <p>Contains the unified enum definition with values that work
 * across all versions where this enum is present.</p>
 */
public class MergedEnum {

    private final String name;
    private final List<MergedEnumValue> values;
    private final Set<String> presentInVersions;
    private final Map<String, String> versionSourceFiles; // version -> source file name

    /**
     * Create a new MergedEnum.
     *
     * @param name the enum name
     */
    public MergedEnum(String name) {
        this.name = name;
        this.values = new ArrayList<>();
        this.presentInVersions = new LinkedHashSet<>();
        this.versionSourceFiles = new LinkedHashMap<>();
    }

    /**
     * Add a value to this enum.
     *
     * @param value the value to add
     */
    public void addValue(MergedEnumValue value) {
        values.add(value);
    }

    /**
     * Add a version where this enum is present.
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

    /** @return the enum name */
    public String getName() {
        return name;
    }

    /** @return unmodifiable list of enum values */
    public List<MergedEnumValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    /** @return the set of versions where this enum is present */
    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    @Override
    public String toString() {
        return String.format("MergedEnum[%s, %d values]", name, values.size());
    }
}
