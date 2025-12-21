package space.alnovis.protowrapper.generator;

import java.util.Map;
import java.util.Set;

/**
 * Utility class for type normalization and conversion operations.
 *
 * <p>Consolidates type-related logic previously duplicated across
 * ImplClassGenerator, VersionMerger, and other classes.</p>
 *
 * <p>Provides operations for:</p>
 * <ul>
 *   <li>Converting between primitive and wrapper types</li>
 *   <li>Type category checking (int, long, float, double)</li>
 *   <li>Widening conversion detection</li>
 *   <li>Determining wider types for conflict resolution</li>
 * </ul>
 */
public final class TypeNormalizer {

    private static final Map<String, String> WRAPPER_TO_PRIMITIVE = Map.of(
            "Integer", "int",
            "Long", "long",
            "Double", "double",
            "Float", "float",
            "Boolean", "boolean",
            "Short", "short",
            "Byte", "byte",
            "Character", "char"
    );

    private static final Map<String, String> PRIMITIVE_TO_WRAPPER = Map.of(
            "int", "Integer",
            "long", "Long",
            "double", "Double",
            "float", "Float",
            "boolean", "Boolean",
            "short", "Short",
            "byte", "Byte",
            "char", "Character"
    );

    private TypeNormalizer() {
        // Utility class
    }

    // ==================== Normalization ====================

    /**
     * Normalize a type to its primitive form for comparison.
     *
     * @param type Type name (e.g., "Integer", "int", "Long")
     * @return Primitive form (e.g., "int", "long")
     */
    public static String toPrimitive(String type) {
        return WRAPPER_TO_PRIMITIVE.getOrDefault(type, type);
    }

    /**
     * Convert a primitive type to its wrapper form.
     *
     * @param type Type name (e.g., "int", "long")
     * @return Wrapper form (e.g., "Integer", "Long")
     */
    public static String toWrapper(String type) {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(type, type);
    }

    /**
     * Check if two types are equivalent (primitive vs wrapper of same type).
     *
     * @param type1 First type
     * @param type2 Second type
     * @return true if types represent the same underlying type
     */
    public static boolean areEquivalent(String type1, String type2) {
        return toPrimitive(type1).equals(toPrimitive(type2));
    }

    // ==================== Type Category Checks ====================

    /**
     * Check if type is an integer type (int/Integer/int32/uint32).
     */
    public static boolean isIntType(String type) {
        return "int".equals(type) || "Integer".equals(type)
                || "int32".equals(type) || "uint32".equals(type);
    }

    /**
     * Check if type is a long type (long/Long/int64/uint64).
     */
    public static boolean isLongType(String type) {
        return "long".equals(type) || "Long".equals(type)
                || "int64".equals(type) || "uint64".equals(type);
    }

    /**
     * Check if type is a float type (float/Float).
     */
    public static boolean isFloatType(String type) {
        return "float".equals(type) || "Float".equals(type);
    }

    /**
     * Check if type is a double type (double/Double).
     */
    public static boolean isDoubleType(String type) {
        return "double".equals(type) || "Double".equals(type);
    }

    /**
     * Check if type is any floating point type (float or double).
     */
    public static boolean isFloatingPointType(String type) {
        return isFloatType(type) || isDoubleType(type);
    }

    /**
     * Check if type is any numeric type.
     */
    public static boolean isNumericType(String type) {
        return isIntType(type) || isLongType(type) || isFloatingPointType(type);
    }

    // ==================== Widening Conversions ====================

    /**
     * Check if conversion from sourceType to targetType is a safe widening conversion.
     *
     * <p>Safe widening conversions:</p>
     * <ul>
     *   <li>int → long</li>
     *   <li>int → double</li>
     *   <li>float → double</li>
     *   <li>long → double (precision loss possible, but compiles)</li>
     * </ul>
     *
     * @param sourceType The source type (being read)
     * @param targetType The target type (being returned)
     * @return true if widening is safe
     */
    public static boolean isWideningConversion(String sourceType, String targetType) {
        String normalizedSource = toPrimitive(sourceType);
        String normalizedTarget = toPrimitive(targetType);

        // int can be safely widened to long or double
        if ("int".equals(normalizedSource) &&
                ("long".equals(normalizedTarget) || "double".equals(normalizedTarget))) {
            return true;
        }
        // float can be safely widened to double
        if ("float".equals(normalizedSource) && "double".equals(normalizedTarget)) {
            return true;
        }
        // long can be safely widened to double
        if ("long".equals(normalizedSource) && "double".equals(normalizedTarget)) {
            return true;
        }
        return false;
    }

    /**
     * Determine the wider type from a set of numeric types.
     *
     * <p>Priority: double > float > long > int</p>
     *
     * @param types Set of type names
     * @return The widest type, or null if no numeric types found
     */
    public static String determineWiderType(Set<String> types) {
        // Check for double (widest floating point)
        if (types.stream().anyMatch(TypeNormalizer::isDoubleType)) {
            return "double";
        }
        // Check for float (if also has int/long, might need double)
        if (types.stream().anyMatch(TypeNormalizer::isFloatType)) {
            // If there's also long or int, could widen to double for consistency
            // But typically float+int stays float, float+double -> double
            return "double"; // Conservative: widen to double
        }
        // Check for long
        if (types.stream().anyMatch(TypeNormalizer::isLongType)) {
            return "long";
        }
        // Check for int
        if (types.stream().anyMatch(TypeNormalizer::isIntType)) {
            return "int";
        }
        return null;
    }

    /**
     * Get the Number method name for widening conversion.
     *
     * <p>Used when converting boxed types in streams, e.g., {@code e.longValue()}</p>
     *
     * @param targetType The target type to widen to
     * @return Method name like "longValue", "doubleValue"
     */
    public static String getWideningMethodName(String targetType) {
        String normalized = toPrimitive(targetType);
        return switch (normalized) {
            case "long" -> "longValue";
            case "double" -> "doubleValue";
            case "float" -> "floatValue";
            case "int" -> "intValue";
            case "short" -> "shortValue";
            case "byte" -> "byteValue";
            default -> "longValue"; // fallback
        };
    }

    // ==================== List Type Handling ====================

    /**
     * Extract element type from a List type string.
     *
     * @param type Type string like "java.util.List&lt;Integer&gt;"
     * @return Element type like "Integer", or original type if not a List
     */
    public static String extractListElementType(String type) {
        if (type != null && type.startsWith("java.util.List<") && type.endsWith(">")) {
            return type.substring("java.util.List<".length(), type.length() - 1);
        }
        return type;
    }

    /**
     * Extract simple type name from a potentially qualified type.
     *
     * @param typeName Fully qualified or simple type name
     * @return Simple type name
     */
    public static String extractSimpleName(String typeName) {
        if (typeName == null) return "Object";
        String type = extractListElementType(typeName);
        int lastDot = type.lastIndexOf('.');
        return lastDot >= 0 ? type.substring(lastDot + 1) : type;
    }
}
