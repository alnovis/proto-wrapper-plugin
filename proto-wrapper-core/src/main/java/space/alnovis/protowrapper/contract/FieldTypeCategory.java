package space.alnovis.protowrapper.contract;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * Categorizes protobuf field types for contract matrix purposes.
 *
 * <p>This simplifies the 18 protobuf types into 5 categories that have
 * distinct behavior patterns in the wrapper API.</p>
 */
public enum FieldTypeCategory {

    /**
     * Numeric scalar types that have primitive Java equivalents.
     * Includes: int32, int64, uint32, uint64, sint32, sint64,
     * fixed32, fixed64, sfixed32, sfixed64, float, double, bool.
     *
     * <p>These types:</p>
     * <ul>
     *   <li>Have boxed wrappers (Integer, Long, Float, Double, Boolean)</li>
     *   <li>Have default values (0, 0L, 0.0f, 0.0, false)</li>
     *   <li>Are nullable only when has*() is available</li>
     * </ul>
     */
    SCALAR_NUMERIC,

    /**
     * String type.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Default value is empty string ""</li>
     *   <li>Nullable only when has*() is available</li>
     *   <li>May conflict with bytes type across versions</li>
     * </ul>
     */
    SCALAR_STRING,

    /**
     * Bytes type (binary data).
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Mapped to byte[] in Java</li>
     *   <li>Default value is empty byte array</li>
     *   <li>Nullable only when has*() is available</li>
     *   <li>May conflict with string type across versions</li>
     * </ul>
     */
    SCALAR_BYTES,

    /**
     * Message type (nested message).
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Always has has*() method in both proto2 and proto3</li>
     *   <li>Returns wrapper object or null</li>
     *   <li>Default instance exists but wrapper returns null when unset</li>
     * </ul>
     */
    MESSAGE,

    /**
     * Enum type.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Mapped to generated enum class</li>
     *   <li>Default value is first enum value (typically 0)</li>
     *   <li>In proto3 without optional: not nullable</li>
     *   <li>May conflict with int type across versions (INT_ENUM conflict)</li>
     * </ul>
     */
    ENUM;

    /**
     * Determines the category from a protobuf Type.
     *
     * @param type the protobuf field type
     * @return the corresponding category
     */
    public static FieldTypeCategory fromProtoType(Type type) {
        return switch (type) {
            case TYPE_INT32, TYPE_INT64, TYPE_UINT32, TYPE_UINT64,
                 TYPE_SINT32, TYPE_SINT64, TYPE_FIXED32, TYPE_FIXED64,
                 TYPE_SFIXED32, TYPE_SFIXED64, TYPE_FLOAT, TYPE_DOUBLE,
                 TYPE_BOOL -> SCALAR_NUMERIC;
            case TYPE_STRING -> SCALAR_STRING;
            case TYPE_BYTES -> SCALAR_BYTES;
            case TYPE_MESSAGE, TYPE_GROUP -> MESSAGE;
            case TYPE_ENUM -> ENUM;
        };
    }

    /**
     * @return true if this is a scalar type (not message or enum)
     */
    public boolean isScalar() {
        return this == SCALAR_NUMERIC || this == SCALAR_STRING || this == SCALAR_BYTES;
    }

    /**
     * @return true if this type has a primitive Java equivalent
     */
    public boolean hasPrimitiveEquivalent() {
        return this == SCALAR_NUMERIC;
    }
}
