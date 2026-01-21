package io.alnovis.protowrapper.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.generator.conflict.CodeGenerationHelper;
import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;

import java.util.Map;
import java.util.Objects;

import static io.alnovis.protowrapper.model.ProtoSyntax.PROTO2;

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
    private final WellKnownTypeInfo wellKnownType; // null if not a well-known type
    private final boolean supportsHasMethod; // true if proto has*() method exists for this field
    private final ProtoSyntax detectedSyntax; // Syntax of the proto file this field came from

    /**
     * Creates a FieldInfo from a field descriptor with default settings.
     *
     * @param proto the field descriptor
     */
    public FieldInfo(FieldDescriptorProto proto) {
        this(proto, -1, null, null, PROTO2);
    }

    /**
     * Creates a FieldInfo with oneof information.
     *
     * @param proto the field descriptor
     * @param oneofIndex the index of the oneof this field belongs to, or -1 if not in oneof
     * @param oneofName the name of the oneof this field belongs to, or null if not in oneof
     */
    public FieldInfo(FieldDescriptorProto proto, int oneofIndex, String oneofName) {
        this(proto, oneofIndex, oneofName, null, PROTO2);
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
        this(proto, oneofIndex, oneofName, mapEntries, PROTO2);
    }

    /**
     * Creates a FieldInfo with oneof information, map entry access, and syntax info.
     *
     * @param proto the field descriptor
     * @param oneofIndex the index of the oneof this field belongs to, or -1 if not in oneof
     * @param oneofName the name of the oneof this field belongs to, or null if not in oneof
     * @param mapEntries map of entry type name to entry descriptor (can be null)
     * @param syntax the proto syntax version (PROTO2 or PROTO3)
     */
    public FieldInfo(FieldDescriptorProto proto, int oneofIndex, String oneofName,
                     Map<String, DescriptorProto> mapEntries, ProtoSyntax syntax) {
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

        // Detect well-known type
        this.wellKnownType = (typeName != null && type == Type.TYPE_MESSAGE)
                ? WellKnownTypeInfo.fromProtoType(typeName).orElse(null)
                : null;

        // Determine if has*() method is available for this field
        // In proto3, only message types and fields in oneofs (including synthetic for optional) have has*()
        // In proto2, all optional fields have has*()
        this.supportsHasMethod = determineHasMethodSupport(proto, syntax);
        this.detectedSyntax = syntax;
    }

    /**
     * Determines if this field supports has*() method in protobuf.
     * <p>
     * General rules:
     * <ul>
     *   <li>Repeated fields NEVER have has*() method (use getXxxCount() instead)</li>
     *   <li>Fields in oneof always have has*() method</li>
     * </ul>
     * In proto3:
     * <ul>
     *   <li>Singular message fields have has*() method</li>
     *   <li>Scalar fields with 'optional' keyword (represented as synthetic oneof) have has*() method</li>
     *   <li>Scalar fields without 'optional' modifier do NOT have has*() method</li>
     * </ul>
     * In proto2:
     * <ul>
     *   <li>All optional fields have has*() method</li>
     *   <li>Required fields have has*() method (always returns true)</li>
     * </ul>
     */
    private static boolean determineHasMethodSupport(FieldDescriptorProto proto, ProtoSyntax syntax) {
        // Repeated fields NEVER have has*() method (in any syntax)
        if (proto.getLabel() == Label.LABEL_REPEATED) {
            return false;
        }

        // Singular message types always have has*() method
        if (proto.getType() == Type.TYPE_MESSAGE) {
            return true;
        }

        // Fields in oneof always have has*() method (including synthetic oneofs for proto3 optional)
        if (proto.hasOneofIndex()) {
            return true;
        }

        // In proto2, all non-repeated fields (optional and required) have has*() method
        // In proto3, scalar fields without optional modifier do NOT have has*() method
        return !syntax.isProto3();
    }

    /**
     * Constructor for merged fields.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName) {
        this(protoName, javaName, number, type, label, typeName, null, -1, null, null);
    }

    /**
     * Constructor for merged fields with oneof info.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     * @param oneofIndex the oneof index, or -1 if not in oneof
     * @param oneofName the oneof name, or null if not in oneof
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, int oneofIndex, String oneofName) {
        this(protoName, javaName, number, type, label, typeName, null, oneofIndex, oneofName, null);
    }

    /**
     * Constructor for merged fields with map info and oneof info.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     * @param mapInfo the map info, or null if not a map
     * @param oneofIndex the oneof index, or -1 if not in oneof
     * @param oneofName the oneof name, or null if not in oneof
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, MapInfo mapInfo, int oneofIndex, String oneofName) {
        this(protoName, javaName, number, type, label, typeName, mapInfo, oneofIndex, oneofName, null);
    }

    /**
     * Full constructor for merged fields with map info, oneof info, and well-known type.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     * @param mapInfo the map info, or null if not a map
     * @param oneofIndex the oneof index, or -1 if not in oneof
     * @param oneofName the oneof name, or null if not in oneof
     * @param wellKnownType the well-known type info, or null
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, MapInfo mapInfo, int oneofIndex, String oneofName,
                     WellKnownTypeInfo wellKnownType) {
        this(protoName, javaName, number, type, label, typeName, mapInfo, oneofIndex, oneofName, wellKnownType, true);
    }

    /**
     * Full constructor for merged fields with all info including supportsHasMethod.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     * @param mapInfo the map info, or null if not a map
     * @param oneofIndex the oneof index, or -1 if not in oneof
     * @param oneofName the oneof name, or null if not in oneof
     * @param wellKnownType the well-known type info, or null
     * @param supportsHasMethod whether the field supports has method
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, MapInfo mapInfo, int oneofIndex, String oneofName,
                     WellKnownTypeInfo wellKnownType, boolean supportsHasMethod) {
        this(protoName, javaName, number, type, label, typeName, mapInfo, oneofIndex, oneofName,
                wellKnownType, supportsHasMethod, PROTO2);
    }

    /**
     * Full constructor for merged fields with all info including supportsHasMethod and syntax.
     *
     * @param protoName the proto field name
     * @param javaName the Java field name
     * @param number the field number
     * @param type the field type
     * @param label the field label
     * @param typeName the type name for message/enum types
     * @param mapInfo the map info, or null if not a map
     * @param oneofIndex the oneof index, or -1 if not in oneof
     * @param oneofName the oneof name, or null if not in oneof
     * @param wellKnownType the well-known type info, or null
     * @param supportsHasMethod whether the field supports has method
     * @param syntax the proto syntax version
     */
    public FieldInfo(String protoName, String javaName, int number, Type type,
                     Label label, String typeName, MapInfo mapInfo, int oneofIndex, String oneofName,
                     WellKnownTypeInfo wellKnownType, boolean supportsHasMethod, ProtoSyntax syntax) {
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
        // Detect well-known type if not explicitly provided
        this.wellKnownType = wellKnownType != null ? wellKnownType
                : (typeName != null && type == Type.TYPE_MESSAGE
                        ? WellKnownTypeInfo.fromProtoType(typeName).orElse(null)
                        : null);
        this.supportsHasMethod = supportsHasMethod;
        this.detectedSyntax = syntax;
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
     *
     * @return the Java type string
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
     *
     * @return the getter return type string
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
        return CodeGenerationHelper.extractSimpleTypeName(fullTypeName);
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

    /**
     * Check if this field is a primitive type.
     *
     * @return true if primitive
     */
    public boolean isPrimitive() {
        return switch (type) {
            case TYPE_DOUBLE, TYPE_FLOAT,
                 TYPE_INT64, TYPE_UINT64, TYPE_SINT64, TYPE_FIXED64, TYPE_SFIXED64,
                 TYPE_INT32, TYPE_UINT32, TYPE_SINT32, TYPE_FIXED32, TYPE_SFIXED32,
                 TYPE_BOOL -> true;
            default -> false;
        };
    }

    /**
     * Check if this field is a message type.
     *
     * @return true if message
     */
    public boolean isMessage() {
        return type == Type.TYPE_MESSAGE;
    }

    /**
     * Check if this field is an enum type.
     *
     * @return true if enum
     */
    public boolean isEnum() {
        return type == Type.TYPE_ENUM;
    }

    /**
     * Get the getter method name for this field.
     *
     * @return the getter method name
     */
    public String getGetterName() {
        String prefix = type == Type.TYPE_BOOL ? "is" : "get";
        return prefix + capitalize(javaName);
    }

    // Map field method names

    /**
     * Getter name for map field (e.g., "getItemCountsMap").
     *
     * @return the map getter method name
     */
    public String getMapGetterName() {
        return "get" + capitalize(javaName) + "Map";
    }

    /**
     * Count getter name for map field (e.g., "getItemCountsCount").
     *
     * @return the map count method name
     */
    public String getMapCountMethodName() {
        return "get" + capitalize(javaName) + "Count";
    }

    /**
     * Contains method name for map field (e.g., "containsItemCounts").
     *
     * @return the map contains method name
     */
    public String getMapContainsMethodName() {
        return "contains" + capitalize(javaName);
    }

    /**
     * GetOrDefault method name for map field (e.g., "getItemCountsOrDefault").
     *
     * @return the map getOrDefault method name
     */
    public String getMapGetOrDefaultMethodName() {
        return "get" + capitalize(javaName) + "OrDefault";
    }

    /**
     * GetOrThrow method name for map field (e.g., "getItemCountsOrThrow").
     *
     * @return the map getOrThrow method name
     */
    public String getMapGetOrThrowMethodName() {
        return "get" + capitalize(javaName) + "OrThrow";
    }

    /**
     * Extract method name for map field (e.g., "extractItemCountsMap").
     *
     * @return the map extract method name
     */
    public String getMapExtractMethodName() {
        return "extract" + capitalize(javaName) + "Map";
    }

    // Builder method names for maps

    /**
     * Put method name for map builder (e.g., "putItemCounts").
     *
     * @return the map put method name
     */
    public String getMapPutMethodName() {
        return "put" + capitalize(javaName);
    }

    /**
     * PutAll method name for map builder (e.g., "putAllItemCounts").
     *
     * @return the map putAll method name
     */
    public String getMapPutAllMethodName() {
        return "putAll" + capitalize(javaName);
    }

    /**
     * Remove method name for map builder (e.g., "removeItemCounts").
     *
     * @return the map remove method name
     */
    public String getMapRemoveMethodName() {
        return "remove" + capitalize(javaName);
    }

    /**
     * Clear method name for map builder (e.g., "clearItemCounts").
     *
     * @return the map clear method name
     */
    public String getMapClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * Get the has method name for this field.
     *
     * @return the has method name
     */
    public String getHasMethodName() {
        return "has" + capitalize(javaName);
    }

    /**
     * Get the extract method name for this field.
     *
     * @return the extract method name
     */
    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }

    /**
     * Get the extractHas method name for this field.
     *
     * @return the extractHas method name
     */
    public String getExtractHasMethodName() {
        return "extractHas" + capitalize(javaName);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** @return the proto field name */
    public String getProtoName() { return protoName; }
    /** @return the Java field name */
    public String getJavaName() { return javaName; }
    /** @return the field number */
    public int getNumber() { return number; }
    /** @return the field type */
    public Type getType() { return type; }
    /** @return the field label */
    public Label getLabel() { return label; }
    /** @return the type name for message/enum types */
    public String getTypeName() { return typeName; }
    /** @return true if field is optional */
    public boolean isOptional() { return isOptional; }
    /** @return true if field is repeated */
    public boolean isRepeated() { return isRepeated; }
    /** @return true if field is a map */
    public boolean isMap() { return isMap; }
    /** @return the map info, or null if not a map */
    public MapInfo getMapInfo() { return mapInfo; }
    /** @return the oneof index, or -1 if not in oneof */
    public int getOneofIndex() { return oneofIndex; }
    /** @return the oneof name, or null if not in oneof */
    public String getOneofName() { return oneofName; }
    /** @return true if field is in a oneof */
    public boolean isInOneof() { return oneofIndex >= 0; }
    /** @return the well-known type info, or null */
    public WellKnownTypeInfo getWellKnownType() { return wellKnownType; }
    /** @return true if field is a well-known type */
    public boolean isWellKnownType() { return wellKnownType != null; }
    /**
     * Returns true if the proto has*() method is available for this field.
     * In proto3, scalar fields without 'optional' modifier do not have has*() methods.
     *
     * @return true if has method is supported
     */
    public boolean supportsHasMethod() { return supportsHasMethod; }

    /**
     * Returns the detected proto syntax for the file this field came from.
     *
     * @return the proto syntax (PROTO2 or PROTO3)
     */
    public ProtoSyntax getDetectedSyntax() { return detectedSyntax; }

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
