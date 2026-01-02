package space.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Map;
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
    private final MapInfo mapInfo;    // null if not a map
    private final int oneofIndex;     // -1 if not in oneof
    private final String oneofName;   // null if not in oneof

    public FieldInfo(FieldDescriptorProto proto) {
        this(proto, -1, null, null);
    }

    /**
     * Creates a FieldInfo with oneof information.
     *
     * @param proto the field descriptor
     * @param oneofIndex the index of the oneof this field belongs to, or -1 if not in oneof
     * @param oneofName the name of the oneof this field belongs to, or null if not in oneof
     */
    public FieldInfo(FieldDescriptorProto proto, int oneofIndex, String oneofName) {
        this(proto, oneofIndex, oneofName, null);
    }

    /**
     * Creates a FieldInfo with oneof information and access to map entry descriptors.
     *
     * @param proto the field descriptor
     * @param oneofIndex the index of the oneof this field belongs to, or -1 if not in oneof
     * @param oneofName the name of the oneof this field belongs to, or null if not in oneof
     * @param mapEntries map of entry type name to entry descriptor (can be null)
     */
    public FieldInfo(FieldDescriptorProto proto, int oneofIndex, String oneofName,
                     Map<String, DescriptorProto> mapEntries) {
        this.protoName = proto.getName();
        this.javaName = toJavaName(proto.getName());
        this.number = proto.getNumber();
        this.type = proto.getType();
        this.label = proto.getLabel();
        this.typeName = proto.hasTypeName() ? proto.getTypeName() : null;
        this.isOptional = proto.getLabel() == Label.LABEL_OPTIONAL;
        this.isRepeated = proto.getLabel() == Label.LABEL_REPEATED;
        this.oneofIndex = oneofIndex;
        this.oneofName = oneofName;

        // Detect map field
        MapInfo detectedMapInfo = null;
        if (isRepeated && proto.getType() == Type.TYPE_MESSAGE && mapEntries != null) {
            // Look for map entry by type name
            String entryTypeName = extractSimpleTypeName(typeName);
            DescriptorProto entryDescriptor = mapEntries.get(entryTypeName);
            if (entryDescriptor != null && entryDescriptor.getOptions().getMapEntry()) {
                detectedMapInfo = MapInfo.fromDescriptor(entryDescriptor);
            }
        }

        // Fallback to heuristic if no map entries provided
        if (detectedMapInfo == null && isRepeated && isMapEntryHeuristic(proto)) {
            // Cannot create MapInfo without entry descriptor, just mark as potential map
            this.isMap = true;
            this.mapInfo = null;
        } else {
            this.isMap = detectedMapInfo != null;
            this.mapInfo = detectedMapInfo;
        }
    }

    // Constructor for merged fields
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName) {
        this(protoName, javaName, number, type, label, typeName, null, -1, null);
    }

    // Constructor for merged fields with oneof info
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, int oneofIndex, String oneofName) {
        this(protoName, javaName, number, type, label, typeName, null, oneofIndex, oneofName);
    }

    // Constructor for merged fields with map info and oneof info
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, MapInfo mapInfo, int oneofIndex, String oneofName) {
        this.protoName = protoName;
        this.javaName = javaName;
        this.number = number;
        this.type = type;
        this.label = label;
        this.typeName = typeName;
        this.isOptional = label == Label.LABEL_OPTIONAL;
        this.isRepeated = label == Label.LABEL_REPEATED;
        this.isMap = mapInfo != null;
        this.mapInfo = mapInfo;
        this.oneofIndex = oneofIndex;
        this.oneofName = oneofName;
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

    private boolean isMapEntryHeuristic(FieldDescriptorProto proto) {
        // Fallback heuristic: maps in proto3 are represented as repeated message with MapEntry option
        // This is not 100% accurate (a message could legitimately end with "Entry")
        return proto.hasTypeName() && proto.getTypeName().endsWith("Entry");
    }

    /**
     * Get Java type for this field.
     */
    public String getJavaType() {
        if (isMap && mapInfo != null) {
            return mapInfo.getMapJavaType();
        }
        if (isRepeated && !isMap) {
            return "java.util.List<" + getSingleJavaType() + ">";
        }
        return getSingleJavaType();
    }

    /**
     * Get Java type for getter return type (may be boxed for optional primitives).
     */
    public String getGetterType() {
        if (isMap && mapInfo != null) {
            return mapInfo.getMapJavaType();
        }
        if (isRepeated) {
            return "java.util.List<" + getBoxedType(getSingleJavaType()) + ">";
        }
        if (isOptional && isPrimitive()) {
            return getBoxedType(getSingleJavaType());
        }
        return getSingleJavaType();
    }

    private String getSingleJavaType() {
        return switch (type) {
            case TYPE_DOUBLE -> "double";
            case TYPE_FLOAT -> "float";
            case TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64 -> "long";
            case TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32 -> "int";
            case TYPE_BOOL -> "boolean";
            case TYPE_STRING -> "String";
            case TYPE_BYTES -> "byte[]";
            case TYPE_MESSAGE, TYPE_ENUM -> extractSimpleTypeName(typeName);
            default -> "Object";
        };
    }

    private String getBoxedType(String primitiveType) {
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

        // Fallback: try to find version pattern in typeName directly
        // This handles cases where proto package differs from java package
        // e.g., typeName = "billing.v1.InvoiceDocument.Fee" when java_package = "com.example.proto.v1"
        String versionRegex = "^[a-zA-Z0-9_.]*\\.v\\d+\\.";
        String result = cleanTypeName.replaceFirst(versionRegex, "");
        if (!result.equals(cleanTypeName)) {
            return result;
        }

        // Final fallback to simple name
        return extractSimpleTypeName(typeName);
    }

    public boolean isPrimitive() {
        return switch (type) {
            case TYPE_DOUBLE, TYPE_FLOAT,
                 TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64,
                 TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32,
                 TYPE_BOOL -> true;
            default -> false;
        };
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

    // Map field method names

    /**
     * Getter name for map field (e.g., "getItemCountsMap").
     */
    public String getMapGetterName() {
        return "get" + capitalize(javaName) + "Map";
    }

    /**
     * Count getter name for map field (e.g., "getItemCountsCount").
     */
    public String getMapCountMethodName() {
        return "get" + capitalize(javaName) + "Count";
    }

    /**
     * Contains method name for map field (e.g., "containsItemCounts").
     */
    public String getMapContainsMethodName() {
        return "contains" + capitalize(javaName);
    }

    /**
     * GetOrDefault method name for map field (e.g., "getItemCountsOrDefault").
     */
    public String getMapGetOrDefaultMethodName() {
        return "get" + capitalize(javaName) + "OrDefault";
    }

    /**
     * GetOrThrow method name for map field (e.g., "getItemCountsOrThrow").
     */
    public String getMapGetOrThrowMethodName() {
        return "get" + capitalize(javaName) + "OrThrow";
    }

    /**
     * Extract method name for map field (e.g., "extractItemCountsMap").
     */
    public String getMapExtractMethodName() {
        return "extract" + capitalize(javaName) + "Map";
    }

    // Builder method names for maps

    /**
     * Put method name for map builder (e.g., "putItemCounts").
     */
    public String getMapPutMethodName() {
        return "put" + capitalize(javaName);
    }

    /**
     * PutAll method name for map builder (e.g., "putAllItemCounts").
     */
    public String getMapPutAllMethodName() {
        return "putAll" + capitalize(javaName);
    }

    /**
     * Remove method name for map builder (e.g., "removeItemCounts").
     */
    public String getMapRemoveMethodName() {
        return "remove" + capitalize(javaName);
    }

    /**
     * Clear method name for map builder (e.g., "clearItemCounts").
     */
    public String getMapClearMethodName() {
        return "clear" + capitalize(javaName);
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
    public MapInfo getMapInfo() { return mapInfo; }
    public int getOneofIndex() { return oneofIndex; }
    public String getOneofName() { return oneofName; }
    public boolean isInOneof() { return oneofIndex >= 0; }

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
