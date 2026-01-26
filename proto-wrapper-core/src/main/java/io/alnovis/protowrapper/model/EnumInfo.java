package io.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents information about a protobuf enum type.
 */
public class EnumInfo {

    private final String name;
    private final String fullName;
    private final List<EnumValue> values;
    private final String sourceFileName;

    /**
     * Creates EnumInfo from protobuf descriptor.
     *
     * @param proto the enum descriptor proto
     */
    public EnumInfo(EnumDescriptorProto proto) {
        this(proto, null, null);
    }

    /**
     * Creates EnumInfo from protobuf descriptor with source file name.
     *
     * @param proto the enum descriptor proto
     * @param sourceFileName the source proto file name
     */
    public EnumInfo(EnumDescriptorProto proto, String sourceFileName) {
        this(proto, null, sourceFileName);
    }

    /**
     * Creates EnumInfo from protobuf descriptor with package and source file name.
     *
     * @param proto the enum descriptor proto
     * @param packageName the protobuf package name
     * @param sourceFileName the source proto file name
     */
    public EnumInfo(EnumDescriptorProto proto, String packageName, String sourceFileName) {
        this.name = proto.getName();
        this.fullName = packageName != null && !packageName.isEmpty()
                ? packageName + "." + proto.getName()
                : proto.getName();
        this.values = proto.getValueList().stream()
                .map(EnumValue::fromProto)
                .toList();
        this.sourceFileName = sourceFileName;
    }

    /**
     * Creates EnumInfo for merged enums.
     *
     * @param name the enum name
     * @param values the list of enum values
     */
    public EnumInfo(String name, List<EnumValue> values) {
        this(name, name, values);
    }

    /**
     * Creates EnumInfo for merged enums with full name.
     *
     * @param name the enum name
     * @param fullName the fully qualified enum name
     * @param values the list of enum values
     */
    public EnumInfo(String name, String fullName, List<EnumValue> values) {
        this.name = name;
        this.fullName = fullName != null ? fullName : name;
        this.values = new ArrayList<>(values);
        this.sourceFileName = null;
    }

    /** @return the source proto file name */
    public String getSourceFileName() {
        return sourceFileName;
    }

    /** @return the enum name */
    public String getName() {
        return name;
    }

    /** @return the fully qualified enum name (package.EnumName) */
    public String getFullName() {
        return fullName;
    }

    /** @return unmodifiable list of enum values */
    public List<EnumValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Get all unique value names across versions.
     *
     * @return list of enum value names
     */
    public List<String> getValueNames() {
        return values.stream()
                .map(EnumValue::getName)
                .toList();
    }

    /**
     * Check if this enum is equivalent to another enum.
     * Enums are equivalent if they have the same name and same values (by number).
     *
     * @param other Another enum to compare
     * @return true if enums are equivalent
     */
    public boolean isEquivalentTo(EnumInfo other) {
        if (other == null) return false;
        if (!this.name.equals(other.name)) return false;
        if (this.values.size() != other.values.size()) return false;

        // Compare values by number
        for (EnumValue thisValue : this.values) {
            boolean found = false;
            for (EnumValue otherValue : other.values) {
                if (thisValue.getNumber() == otherValue.getNumber()) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("EnumInfo[%s: %s]", name,
            values.stream().map(EnumValue::getName).collect(Collectors.joining(", ")));
    }

    /**
     * Represents a single enum value.
     *
     * @param name the proto enum value name
     * @param number the enum value number
     */
    public record EnumValue(String name, int number) {

        /**
         * Creates EnumValue from protobuf descriptor.
         *
         * @param proto the enum value descriptor proto
         * @return new EnumValue instance
         */
        public static EnumValue fromProto(EnumValueDescriptorProto proto) {
            return new EnumValue(proto.getName(), proto.getNumber());
        }

        /**
         * Convert proto enum name to Java enum name.
         * E.g., "OPERATION_BUY" -> "BUY"
         *
         * @return the Java-style enum name
         */
        public String getJavaName() {
            // Remove common prefixes like "OPERATION_", "PAYMENT_", etc.
            String result = name;
            int underscoreIdx = name.indexOf('_');
            if (underscoreIdx > 0 && underscoreIdx < name.length() - 1) {
                result = name.substring(underscoreIdx + 1);
            }
            return result;
        }

        /** @return the enum value name */
        public String getName() { return name; }
        /** @return the enum value number */
        public int getNumber() { return number; }

        @Override
        public String toString() {
            return String.format("%s=%d", name, number);
        }
    }
}
