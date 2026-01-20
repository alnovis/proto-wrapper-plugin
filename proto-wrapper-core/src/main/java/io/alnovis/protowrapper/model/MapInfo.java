package io.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * Information about a protobuf map field.
 *
 * <p>Maps in protobuf are represented as:
 * <pre>
 * map&lt;KeyType, ValueType&gt; field_name = 1;
 *
 * // Which is equivalent to:
 * message FieldNameEntry {
 *     option map_entry = true;
 *     KeyType key = 1;
 *     ValueType value = 2;
 * }
 * repeated FieldNameEntry field_name = 1;
 * </pre>
 */
public record MapInfo(
        Type keyType,
        Type valueType,
        String keyTypeName,   // For message/enum key types (rare)
        String valueTypeName  // For message/enum value types
) {

    /**
     * Creates MapInfo from the map entry descriptor.
     *
     * @param entryDescriptor the nested message with map_entry option
     * @return MapInfo instance
     * @throws IllegalArgumentException if entry descriptor is invalid
     */
    public static MapInfo fromDescriptor(DescriptorProto entryDescriptor) {
        FieldDescriptorProto keyField = null;
        FieldDescriptorProto valueField = null;

        for (FieldDescriptorProto field : entryDescriptor.getFieldList()) {
            if (field.getNumber() == 1 && "key".equals(field.getName())) {
                keyField = field;
            } else if (field.getNumber() == 2 && "value".equals(field.getName())) {
                valueField = field;
            }
        }

        if (keyField == null || valueField == null) {
            throw new IllegalArgumentException(
                    "Invalid map entry descriptor: missing key or value field in " + entryDescriptor.getName());
        }

        return new MapInfo(
                keyField.getType(),
                valueField.getType(),
                keyField.hasTypeName() ? keyField.getTypeName() : null,
                valueField.hasTypeName() ? valueField.getTypeName() : null
        );
    }

    /** @return the proto key type */
    public Type getKeyType() {
        return keyType;
    }

    /** @return the proto value type */
    public Type getValueType() {
        return valueType;
    }

    /** @return the key type name for message/enum keys */
    public String getKeyTypeName() {
        return keyTypeName;
    }

    /** @return the value type name for message/enum values */
    public String getValueTypeName() {
        return valueTypeName;
    }

    /**
     * Returns the Java type for the map key.
     * @return the key Java type
     */
    public String getKeyJavaType() {
        return toJavaType(keyType, keyTypeName);
    }

    /**
     * Returns the Java type for the map value.
     * @return the value Java type
     */
    public String getValueJavaType() {
        return toJavaType(valueType, valueTypeName);
    }

    /**
     * Returns the boxed Java type for the map key (for generic type parameters).
     * @return the boxed key type
     */
    public String getKeyBoxedJavaType() {
        return boxType(getKeyJavaType());
    }

    /**
     * Returns the boxed Java type for the map value (for generic type parameters).
     * @return the boxed value type
     */
    public String getValueBoxedJavaType() {
        return boxType(getValueJavaType());
    }

    /**
     * Returns the full Map type declaration, e.g., "Map&lt;String, Integer&gt;".
     * @return the full Map type declaration
     */
    public String getMapJavaType() {
        return "java.util.Map<" + getKeyBoxedJavaType() + ", " + getValueBoxedJavaType() + ">";
    }

    /**
     * Checks if the value type is a message type.
     * @return true if value is a message type
     */
    public boolean hasMessageValue() {
        return valueType == Type.TYPE_MESSAGE;
    }

    /**
     * Checks if the value type is an enum type.
     * @return true if value is an enum type
     */
    public boolean hasEnumValue() {
        return valueType == Type.TYPE_ENUM;
    }

    /**
     * Checks if the key type is a string (most common case).
     * @return true if key is string type
     */
    public boolean hasStringKey() {
        return keyType == Type.TYPE_STRING;
    }

    /**
     * Extracts simple type name from full protobuf type name.
     * @return the simple value type name
     */
    public String getSimpleValueTypeName() {
        return extractSimpleTypeName(valueTypeName);
    }

    /**
     * Returns the full proto type name for the value (e.g., ".google.protobuf.Timestamp").
     * Returns null for primitive types.
     *
     * @return the value proto type name
     * @since 1.3.0
     */
    public String getValueProtoTypeName() {
        return valueTypeName;
    }

    /**
     * Extracts simple type name from full protobuf type name.
     * @return the simple key type name
     */
    public String getSimpleKeyTypeName() {
        return extractSimpleTypeName(keyTypeName);
    }

    private String toJavaType(Type type, String typeName) {
        return switch (type) {
            case TYPE_DOUBLE -> "double";
            case TYPE_FLOAT -> "float";
            case TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64 -> "long";
            case TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32 -> "int";
            case TYPE_BOOL -> "boolean";
            case TYPE_STRING -> "String";
            case TYPE_BYTES -> "com.google.protobuf.ByteString";
            case TYPE_MESSAGE, TYPE_ENUM -> extractSimpleTypeName(typeName);
            default -> "Object";
        };
    }

    private String boxType(String primitiveType) {
        return switch (primitiveType) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "boolean" -> "Boolean";
            default -> primitiveType;
        };
    }

    private String extractSimpleTypeName(String fullTypeName) {
        if (fullTypeName == null) return "Object";
        int lastDot = fullTypeName.lastIndexOf('.');
        return lastDot >= 0 ? fullTypeName.substring(lastDot + 1) : fullTypeName;
    }

    @Override
    public String toString() {
        return String.format("MapInfo[%s -> %s]", getKeyJavaType(), getValueJavaType());
    }
}
