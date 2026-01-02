package space.alnovis.protowrapper.model;

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
    private final List<EnumValue> values;
    private final String sourceFileName;

    public EnumInfo(EnumDescriptorProto proto) {
        this(proto, null);
    }

    public EnumInfo(EnumDescriptorProto proto, String sourceFileName) {
        this.name = proto.getName();
        this.values = proto.getValueList().stream()
                .map(EnumValue::fromProto)
                .toList();
        this.sourceFileName = sourceFileName;
    }

    // Constructor for merged enums
    public EnumInfo(String name, List<EnumValue> values) {
        this.name = name;
        this.values = new ArrayList<>(values);
        this.sourceFileName = null;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getName() {
        return name;
    }

    public List<EnumValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Get all unique value names across versions.
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
         */
        public static EnumValue fromProto(EnumValueDescriptorProto proto) {
            return new EnumValue(proto.getName(), proto.getNumber());
        }

        /**
         * Convert proto enum name to Java enum name.
         * E.g., "OPERATION_BUY" -> "BUY"
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

        // Accessor methods for compatibility
        public String getName() { return name; }
        public int getNumber() { return number; }

        @Override
        public String toString() {
            return String.format("%s=%d", name, number);
        }
    }
}
