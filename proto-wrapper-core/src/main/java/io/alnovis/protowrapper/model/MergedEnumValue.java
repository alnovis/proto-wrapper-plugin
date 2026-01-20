package io.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged enum value from multiple protocol versions.
 *
 * <p>During schema merging, enum values from different versions are unified
 * into a single representation. This class tracks which versions contain
 * each enum value, enabling version-aware code generation.</p>
 *
 * <h2>Merging Behavior</h2>
 * <p>Enum values are matched by their numeric value (not name). When the same
 * numeric value appears in multiple versions:</p>
 * <ul>
 *   <li>The name from the first version is used</li>
 *   <li>All versions containing this value are tracked in {@link #getPresentInVersions()}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * // v1: enum Status { ACTIVE = 1; INACTIVE = 2; }
 * // v2: enum Status { ACTIVE = 1; INACTIVE = 2; PENDING = 3; }
 *
 * // After merging:
 * // MergedEnumValue[ACTIVE=1, versions={v1, v2}]
 * // MergedEnumValue[INACTIVE=2, versions={v1, v2}]
 * // MergedEnumValue[PENDING=3, versions={v2}]
 * </pre>
 *
 * <h2>Version Availability</h2>
 * <p>Generated wrapper enums include all values from all versions. When
 * converting to a specific proto version, values not present in that
 * version will throw {@code UnsupportedOperationException}.</p>
 *
 * @see MergedEnum
 * @see EnumInfo.EnumValue
 * @see io.alnovis.protowrapper.merger.VersionMerger
 */
public class MergedEnumValue {

    private final String name;
    private final String javaName;
    private final int number;
    private final Set<String> presentInVersions;

    /**
     * Creates a new merged enum value from a source enum value.
     *
     * @param value The source enum value from a specific version
     * @param version The version identifier (e.g., "v1", "v2")
     */
    public MergedEnumValue(EnumInfo.EnumValue value, String version) {
        this.name = value.getName();
        this.javaName = value.getJavaName();
        this.number = value.getNumber();
        this.presentInVersions = new LinkedHashSet<>();
        this.presentInVersions.add(version);
    }

    /**
     * Adds a version to the set of versions containing this enum value.
     *
     * <p>Called during merging when the same numeric value is found
     * in another version.</p>
     *
     * @param version The version identifier to add
     */
    public void addVersion(String version) {
        presentInVersions.add(version);
    }

    /**
     * Returns the proto name of this enum value.
     *
     * @return The enum constant name as defined in .proto file (e.g., "ACTIVE")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java-compatible name for this enum value.
     *
     * <p>Usually the same as {@link #getName()}, but may differ if the proto
     * name is a Java reserved word or requires transformation.</p>
     *
     * @return The Java enum constant name
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * Returns the numeric value of this enum constant.
     *
     * <p>This is the unique identifier used for matching enum values
     * across versions during merging.</p>
     *
     * @return The enum value number as defined in .proto file
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the set of versions containing this enum value.
     *
     * @return Unmodifiable set of version identifiers (e.g., {"v1", "v2"})
     */
    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    @Override
    public String toString() {
        return String.format("MergedEnumValue[%s=%d, versions=%s]", name, number, presentInVersions);
    }
}
