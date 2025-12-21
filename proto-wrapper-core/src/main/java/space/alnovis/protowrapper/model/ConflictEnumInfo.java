package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents information about a unified enum generated for INT_ENUM type conflicts.
 *
 * <p>When a field has type int in one version and enum in another,
 * this class holds the merged enum information to generate a unified enum type
 * that can be used in the Builder interface.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // V1: int unitType
 * // V2: UnitTypeEnum unitType
 *
 * // Generated unified enum:
 * public enum UnitType {
 *     CELSIUS(0),
 *     FAHRENHEIT(1),
 *     KELVIN(2);
 *     ...
 * }
 * </pre>
 */
public class ConflictEnumInfo {

    private final String fieldName;        // Original field name, e.g., "unitType"
    private final String enumName;         // Generated enum name, e.g., "UnitType"
    private final String messageName;      // Parent message name, e.g., "SensorReading"
    private final Set<EnumValue> values;   // Merged values from all versions
    private final Map<String, String> versionEnumTypes;  // version -> proto enum FQN

    /**
     * Represents a single enum value with name and number.
     */
    public record EnumValue(String name, int number) implements Comparable<EnumValue> {
        @Override
        public int compareTo(EnumValue other) {
            return Integer.compare(this.number, other.number);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnumValue enumValue = (EnumValue) o;
            return number == enumValue.number && Objects.equals(name, enumValue.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, number);
        }
    }

    private ConflictEnumInfo(Builder builder) {
        this.fieldName = builder.fieldName;
        this.enumName = builder.enumName != null ? builder.enumName : deriveEnumName(builder.fieldName);
        this.messageName = builder.messageName;
        this.values = Collections.unmodifiableSet(new TreeSet<>(builder.values));
        this.versionEnumTypes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.versionEnumTypes));
    }

    /**
     * Create a new builder for ConflictEnumInfo.
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Derive enum name from field name.
     * E.g., "unitType" -> "UnitType", "sync_status" -> "SyncStatus"
     */
    private static String deriveEnumName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "UnknownEnum";
        }
        // Handle snake_case
        String[] parts = fieldName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        // Ensure first char is uppercase for camelCase input
        String result = sb.toString();
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }
        return result;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getEnumName() {
        return enumName;
    }

    public String getMessageName() {
        return messageName;
    }

    /**
     * Get all merged enum values, sorted by number.
     * @return Immutable set of enum values
     */
    public Set<EnumValue> getValues() {
        return values;
    }

    /**
     * Get the proto enum FQN for a specific version.
     * @param version Version identifier
     * @return Proto enum fully qualified name, or null if not an enum in this version
     */
    public String getProtoEnumType(String version) {
        return versionEnumTypes.get(version);
    }

    /**
     * Get all version to proto enum type mappings.
     * @return Immutable map of version to proto enum FQN
     */
    public Map<String, String> getVersionEnumTypes() {
        return versionEnumTypes;
    }

    /**
     * Check if a version uses enum type (vs int).
     * @param version Version identifier
     * @return true if this version uses enum type
     */
    public boolean isEnumInVersion(String version) {
        return versionEnumTypes.containsKey(version);
    }

    /**
     * Get all versions that use enum type.
     * @return Set of version identifiers
     */
    public Set<String> getEnumVersions() {
        return versionEnumTypes.keySet();
    }

    /**
     * Get the full path for this conflict enum (messageName.fieldName).
     * @return Full path like "SensorReading.unitType"
     */
    public String getFullPath() {
        return messageName + "." + fieldName;
    }

    @Override
    public String toString() {
        return String.format("ConflictEnumInfo[%s.%s -> %s with %d values]",
                messageName, fieldName, enumName, values.size());
    }

    /**
     * Builder for creating ConflictEnumInfo instances.
     */
    public static class Builder {
        private String fieldName;
        private String enumName;
        private String messageName;
        private final Set<EnumValue> values = new LinkedHashSet<>();
        private final Map<String, String> versionEnumTypes = new LinkedHashMap<>();

        /**
         * Set the original field name.
         * @param fieldName Field name like "unitType"
         * @return This builder
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Set the generated enum name. If not set, will be derived from field name.
         * @param enumName Enum name like "UnitType"
         * @return This builder
         */
        public Builder enumName(String enumName) {
            this.enumName = enumName;
            return this;
        }

        /**
         * Set the parent message name.
         * @param messageName Message name like "SensorReading"
         * @return This builder
         */
        public Builder messageName(String messageName) {
            this.messageName = messageName;
            return this;
        }

        /**
         * Add an enum value.
         * @param name Value name like "CELSIUS"
         * @param number Value number like 0
         * @return This builder
         */
        public Builder addValue(String name, int number) {
            this.values.add(new EnumValue(name, number));
            return this;
        }

        /**
         * Add all enum values from an EnumInfo.
         * @param enumInfo Enum info with values
         * @return This builder
         */
        public Builder addValuesFrom(EnumInfo enumInfo) {
            for (EnumInfo.EnumValue value : enumInfo.getValues()) {
                this.values.add(new EnumValue(value.getName(), value.getNumber()));
            }
            return this;
        }

        /**
         * Register a version that uses enum type.
         * @param version Version identifier
         * @param protoEnumFqn Proto enum fully qualified name
         * @return This builder
         */
        public Builder addVersionEnumType(String version, String protoEnumFqn) {
            this.versionEnumTypes.put(version, protoEnumFqn);
            return this;
        }

        /**
         * Build the ConflictEnumInfo instance.
         * @return New ConflictEnumInfo
         * @throws IllegalStateException if fieldName or messageName is not set
         */
        public ConflictEnumInfo build() {
            if (fieldName == null || fieldName.isEmpty()) {
                throw new IllegalStateException("fieldName must be set");
            }
            if (messageName == null || messageName.isEmpty()) {
                throw new IllegalStateException("messageName must be set");
            }
            if (values.isEmpty()) {
                throw new IllegalStateException("at least one enum value must be added");
            }
            return new ConflictEnumInfo(this);
        }
    }
}
