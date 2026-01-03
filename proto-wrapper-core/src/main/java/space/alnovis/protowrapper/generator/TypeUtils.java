package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import space.alnovis.protowrapper.model.MapInfo;

import java.util.Map;

/**
 * Utility class for type-related operations in code generation.
 *
 * <p>This class consolidates type parsing and manipulation methods that were
 * previously duplicated across InterfaceGenerator, AbstractClassGenerator,
 * ImplClassGenerator, and various conflict handlers.</p>
 *
 * <p>All methods are static and thread-safe.</p>
 */
public final class TypeUtils {

    private TypeUtils() {
        // Utility class - no instantiation
    }

    // ==================== Simple Type Parsing ====================

    /**
     * Parse a simple Java type string into a JavaPoet TypeName.
     *
     * <p>Handles primitives, common types, and fully qualified class names.</p>
     *
     * @param javaType Java type string (e.g., "int", "String", "com.example.MyClass")
     * @return TypeName for code generation
     */
    public static TypeName parseSimpleType(String javaType) {
        return switch (javaType) {
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "double" -> TypeName.DOUBLE;
            case "float" -> TypeName.FLOAT;
            case "boolean" -> TypeName.BOOLEAN;
            case "String" -> ClassName.get(String.class);
            case "com.google.protobuf.ByteString" -> ClassName.get("com.google.protobuf", "ByteString");
            default -> parseQualifiedType(javaType);
        };
    }

    /**
     * Parse a potentially qualified type name.
     */
    private static TypeName parseQualifiedType(String javaType) {
        if (javaType.contains(".")) {
            int lastDot = javaType.lastIndexOf('.');
            return ClassName.get(javaType.substring(0, lastDot), javaType.substring(lastDot + 1));
        }
        return ClassName.get("", javaType);
    }

    // ==================== Map Type Operations ====================

    /**
     * Parse the key type from MapInfo.
     *
     * @param mapInfo Map field information
     * @return TypeName for the key type
     */
    public static TypeName parseMapKeyType(MapInfo mapInfo) {
        return parseSimpleType(mapInfo.getKeyJavaType());
    }

    /**
     * Parse the value type from MapInfo.
     *
     * @param mapInfo Map field information
     * @return TypeName for the value type
     */
    public static TypeName parseMapValueType(MapInfo mapInfo) {
        return parseSimpleType(mapInfo.getValueJavaType());
    }

    /**
     * Create a parameterized Map type with boxed key and value types.
     *
     * @param keyType   Key type (will be boxed if primitive)
     * @param valueType Value type (will be boxed if primitive)
     * @return ParameterizedTypeName for Map&lt;K, V&gt;
     */
    public static TypeName createMapType(TypeName keyType, TypeName valueType) {
        TypeName boxedKeyType = keyType.isPrimitive() ? keyType.box() : keyType;
        TypeName boxedValueType = valueType.isPrimitive() ? valueType.box() : valueType;
        return ParameterizedTypeName.get(ClassName.get(Map.class), boxedKeyType, boxedValueType);
    }

    /**
     * Create a parameterized Map type from MapInfo.
     *
     * @param mapInfo Map field information
     * @return ParameterizedTypeName for Map&lt;K, V&gt;
     */
    public static TypeName createMapType(MapInfo mapInfo) {
        TypeName keyType = parseMapKeyType(mapInfo);
        TypeName valueType = parseMapValueType(mapInfo);
        return createMapType(keyType, valueType);
    }

    /**
     * Create a parameterized Map type with a resolved value type (for map value conflicts).
     *
     * @param mapInfo Map field information
     * @param resolvedValueType Resolved unified value type (e.g., "long" for WIDENING, "int" for INT_ENUM)
     * @return ParameterizedTypeName for Map&lt;K, V&gt; with the resolved value type
     */
    public static TypeName createMapTypeWithResolvedValue(MapInfo mapInfo, String resolvedValueType) {
        TypeName keyType = parseMapKeyType(mapInfo);
        TypeName valueType = parseSimpleType(resolvedValueType);
        return createMapType(keyType, valueType);
    }

    // ==================== List Type Operations ====================

    /**
     * Extract the element type from a List type.
     *
     * @param listType Parameterized List type
     * @return Element type, or Object if not parameterized
     */
    public static TypeName extractListElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName parameterized
                && !parameterized.typeArguments.isEmpty()) {
            return parameterized.typeArguments.get(0);
        }
        return ClassName.get(Object.class);
    }

    // ==================== Widening Type Operations ====================

    /**
     * Get the wider primitive type for widening conversions.
     *
     * <p>Used for handling type conflicts where a field has different
     * primitive types across versions (e.g., int in v1, long in v2).</p>
     *
     * @param javaType Java type string
     * @return The wider TypeName (e.g., LONG for int/long conflict)
     */
    public static TypeName getWiderPrimitiveType(String javaType) {
        return switch (javaType) {
            case "long", "Long" -> TypeName.LONG;
            case "double", "Double" -> TypeName.DOUBLE;
            case "int", "Integer" -> TypeName.INT;
            default -> TypeName.LONG; // Default to long for numeric widening
        };
    }

    // ==================== Type Boxing ====================

    /**
     * Box a primitive type if necessary.
     *
     * @param typeName Type to box
     * @return Boxed type if primitive, original type otherwise
     */
    public static TypeName boxIfPrimitive(TypeName typeName) {
        return typeName.isPrimitive() ? typeName.box() : typeName;
    }

    /**
     * Check if a type name represents a primitive type.
     *
     * @param javaType Java type string
     * @return true if primitive
     */
    public static boolean isPrimitive(String javaType) {
        return switch (javaType) {
            case "int", "long", "double", "float", "boolean", "byte", "short", "char" -> true;
            default -> false;
        };
    }

    // ==================== Repeated Conflict Type Resolution ====================

    /**
     * Get the unified element type for a repeated field with type conflict.
     *
     * @param conflictType The type of conflict
     * @return TypeName for the unified element type (always boxed)
     */
    public static TypeName getRepeatedConflictElementType(space.alnovis.protowrapper.model.MergedField.ConflictType conflictType) {
        return switch (conflictType) {
            case WIDENING -> TypeName.LONG.box();
            case FLOAT_DOUBLE -> TypeName.DOUBLE.box();
            case SIGNED_UNSIGNED -> TypeName.LONG.box();
            case INT_ENUM -> TypeName.INT.box();
            case STRING_BYTES -> ClassName.get(String.class);
            default -> ClassName.get(Object.class);
        };
    }

    /**
     * Get primitive unbox cast for boxed types.
     * E.g., for "Long" returns "(long)" to unbox before further casting.
     *
     * @param boxedType Boxed type name (e.g., "Long", "Double")
     * @return Cast string or empty if not applicable
     */
    public static String getPrimitiveUnboxCast(String boxedType) {
        return switch (boxedType) {
            case "Long" -> "(long)";
            case "Integer" -> "(int)";
            case "Double" -> "(double)";
            case "Float" -> "(float)";
            default -> "";
        };
    }

    /**
     * Check if a type is int/Integer.
     */
    public static boolean isIntType(String elementType) {
        return "Integer".equals(elementType) || "int".equals(elementType);
    }

    /**
     * Check if a type is float/Float.
     */
    public static boolean isFloatType(String elementType) {
        return "Float".equals(elementType) || "float".equals(elementType);
    }

    /**
     * Check if a type is long/Long.
     */
    public static boolean isLongType(String elementType) {
        return "Long".equals(elementType) || "long".equals(elementType);
    }
}
