package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Objects;

/**
 * Represents information about a protobuf field.
 */
public class FieldInfo {

    private final String protoName;
    private final String javaName;
    private final int number;
    private final Type type;
    private final Label label;
    private final String typeName; // For message/enum types
    private final boolean isOptional;
    private final boolean isRepeated;
    private final boolean isMap;

    public FieldInfo(FieldDescriptorProto proto) {
        this.protoName = proto.getName();
        this.javaName = toJavaName(proto.getName());
        this.number = proto.getNumber();
        this.type = proto.getType();
        this.label = proto.getLabel();
        this.typeName = proto.hasTypeName() ? proto.getTypeName() : null;
        this.isOptional = proto.getLabel() == Label.LABEL_OPTIONAL;
        this.isRepeated = proto.getLabel() == Label.LABEL_REPEATED;
        this.isMap = isRepeated && isMapEntry(proto);
    }

    // Constructor for merged fields
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName) {
        this.protoName = protoName;
        this.javaName = javaName;
        this.number = number;
        this.type = type;
        this.label = label;
        this.typeName = typeName;
        this.isOptional = label == Label.LABEL_OPTIONAL;
        this.isRepeated = label == Label.LABEL_REPEATED;
        this.isMap = false; // Simplified
    }

    private static String toJavaName(String protoName) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : protoName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    private boolean isMapEntry(FieldDescriptorProto proto) {
        // Maps in proto3 are represented as repeated message with MapEntry option
        return proto.getTypeName().endsWith("Entry");
    }

    /**
     * Get Java type for this field.
     */
    public String getJavaType() {
        if (isRepeated && !isMap) {
            return "java.util.List<" + getSingleJavaType() + ">";
        }
        return getSingleJavaType();
    }

    /**
     * Get Java type for getter return type (may be boxed for optional primitives).
     */
    public String getGetterType() {
        if (isRepeated) {
            return "java.util.List<" + getBoxedType(getSingleJavaType()) + ">";
        }
        if (isOptional && isPrimitive()) {
            return getBoxedType(getSingleJavaType());
        }
        return getSingleJavaType();
    }

    private String getSingleJavaType() {
        switch (type) {
            case TYPE_DOUBLE: return "double";
            case TYPE_FLOAT: return "float";
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_SINT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64: return "long";
            case TYPE_INT32:
            case TYPE_UINT32:
            case TYPE_SINT32:
            case TYPE_FIXED32:
            case TYPE_SFIXED32: return "int";
            case TYPE_BOOL: return "boolean";
            case TYPE_STRING: return "String";
            case TYPE_BYTES: return "byte[]";
            case TYPE_MESSAGE:
            case TYPE_ENUM:
                return extractSimpleTypeName(typeName);
            default:
                return "Object";
        }
    }

    private String getBoxedType(String primitiveType) {
        switch (primitiveType) {
            case "int": return "Integer";
            case "long": return "Long";
            case "double": return "Double";
            case "float": return "Float";
            case "boolean": return "Boolean";
            default: return primitiveType;
        }
    }

    private String extractSimpleTypeName(String fullTypeName) {
        if (fullTypeName == null) return "Object";
        int lastDot = fullTypeName.lastIndexOf('.');
        return lastDot >= 0 ? fullTypeName.substring(lastDot + 1) : fullTypeName;
    }

    /**
     * Extract the nested type path from a full proto type name.
     * E.g., ".example.proto.v1.Order.ShippingInfo" with package "example.proto.v1"
     * returns "Order.ShippingInfo".
     *
     * <p>This method is version-independent: it will correctly strip packages
     * with any version (v1, v2, etc.) as long as the base package matches.</p>
     *
     * @param protoPackage the proto package to strip (e.g., "example.proto.v1")
     * @return the message hierarchy path, or simple name if package doesn't match
     */
    public String extractNestedTypePath(String protoPackage) {
        if (typeName == null) return "Object";

        String cleanTypeName = typeName;
        // Remove leading dot if present
        if (cleanTypeName.startsWith(".")) {
            cleanTypeName = cleanTypeName.substring(1);
        }

        // Try to strip the package prefix (version-independent)
        if (protoPackage != null && !protoPackage.isEmpty()) {
            // Get base package without version (e.g., "example.proto.v1" -> "example.proto")
            String basePackage = protoPackage.replaceAll("\\.v\\d+$", "");

            // Try to match package with any version: basePackage.vXXX.
            // This handles cases where typeName version differs from protoPackage version
            String regex = "^" + basePackage.replace(".", "\\.") + "\\.v\\d+\\.";
            String result = cleanTypeName.replaceFirst(regex, "");
            if (!result.equals(cleanTypeName)) {
                return result;
            }

            // Fallback: try exact match (for non-versioned packages)
            String packagePrefix = protoPackage + ".";
            if (cleanTypeName.startsWith(packagePrefix)) {
                return cleanTypeName.substring(packagePrefix.length());
            }
        }

        // Fallback to simple name
        return extractSimpleTypeName(typeName);
    }

    public boolean isPrimitive() {
        switch (type) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_SINT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_INT32:
            case TYPE_UINT32:
            case TYPE_SINT32:
            case TYPE_FIXED32:
            case TYPE_SFIXED32:
            case TYPE_BOOL:
                return true;
            default:
                return false;
        }
    }

    public boolean isMessage() {
        return type == Type.TYPE_MESSAGE;
    }

    public boolean isEnum() {
        return type == Type.TYPE_ENUM;
    }

    public String getGetterName() {
        String prefix = type == Type.TYPE_BOOL ? "is" : "get";
        return prefix + capitalize(javaName);
    }

    public String getHasMethodName() {
        return "has" + capitalize(javaName);
    }

    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }

    public String getExtractHasMethodName() {
        return "extractHas" + capitalize(javaName);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Getters
    public String getProtoName() { return protoName; }
    public String getJavaName() { return javaName; }
    public int getNumber() { return number; }
    public Type getType() { return type; }
    public Label getLabel() { return label; }
    public String getTypeName() { return typeName; }
    public boolean isOptional() { return isOptional; }
    public boolean isRepeated() { return isRepeated; }
    public boolean isMap() { return isMap; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return number == fieldInfo.number &&
               Objects.equals(protoName, fieldInfo.protoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protoName, number);
    }

    @Override
    public String toString() {
        return String.format("FieldInfo[%s:%s #%d %s%s]",
            protoName, getJavaType(), number,
            isOptional ? "optional" : "required",
            isRepeated ? " repeated" : "");
    }
}
