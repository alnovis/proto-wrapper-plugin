package space.alnovis.protowrapper.generator;

import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods shared across code generators.
 *
 * <p>This class consolidates common utility methods that were previously
 * duplicated across InterfaceGenerator, AbstractClassGenerator, and other generators.</p>
 *
 * <p>Main responsibilities:</p>
 * <ul>
 *   <li>Build version check expressions for generated code</li>
 *   <li>Type classification helpers (primitive, primitive-like)</li>
 *   <li>Message hierarchy navigation and naming</li>
 *   <li>Builder method name generation</li>
 * </ul>
 *
 * @since 1.2.0
 */
public final class GeneratorUtils {

    private GeneratorUtils() {
        // Utility class - no instantiation
    }

    // ==================== Version Check Methods ====================

    /**
     * Build a version check expression for the given set of versions.
     *
     * <p>Generates code like:</p>
     * <ul>
     *   <li>Single version: {@code getWrapperVersion() == 1}</li>
     *   <li>Multiple versions: {@code getWrapperVersion() == 1 || getWrapperVersion() == 2}</li>
     * </ul>
     *
     * @param versions Set of version strings (e.g., "v1", "v2", "v3")
     * @return Version check expression for code generation
     */
    public static String buildVersionCheck(Set<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return "true";
        }

        if (versions.size() == 1) {
            String version = versions.iterator().next();
            String numericPart = extractVersionNumber(version);
            if (!numericPart.isEmpty()) {
                return "getWrapperVersion() == " + numericPart;
            }
            return "true";
        }

        // Multiple versions - build OR condition
        String result = versions.stream()
                .map(GeneratorUtils::extractVersionNumber)
                .filter(numericPart -> !numericPart.isEmpty())
                .map(numericPart -> "getWrapperVersion() == " + numericPart)
                .collect(Collectors.joining(" || "));

        return result.isEmpty() ? "true" : result;
    }

    /**
     * Extract numeric part from version string.
     *
     * @param version Version string (e.g., "v1", "v2")
     * @return Numeric part (e.g., "1", "2") or empty string
     */
    public static String extractVersionNumber(String version) {
        if (version == null) {
            return "";
        }
        return version.replaceAll("[^0-9]", "");
    }

    /**
     * Extract numeric version as integer.
     *
     * @param version Version string (e.g., "v1", "v2")
     * @return Numeric version or 0 if parsing fails
     */
    public static int extractVersionInt(String version) {
        String numStr = extractVersionNumber(version);
        if (numStr.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== Type Classification Methods ====================

    /**
     * Check if field is primitive or "primitive-like" (String, bytes).
     *
     * <p>String and bytes are treated as primitive-like because they can be accessed
     * directly without needing message wrappers, unlike nested message types.</p>
     *
     * @param field Field info to check
     * @return true if field is primitive or primitive-like
     */
    public static boolean isPrimitiveOrPrimitiveLike(FieldInfo field) {
        if (field == null) {
            return false;
        }
        if (field.isPrimitive()) {
            return true;
        }
        // String and bytes are "primitive-like" - they're not nested messages
        String javaType = field.getJavaType();
        return "String".equals(javaType) ||
               "byte[]".equals(javaType) ||
               "ByteString".equals(javaType);
    }

    /**
     * Check if a Java type name is primitive or primitive-like.
     *
     * @param javaType Java type name
     * @return true if primitive or primitive-like
     */
    public static boolean isPrimitiveOrPrimitiveLikeType(String javaType) {
        if (javaType == null) {
            return false;
        }
        return JavaTypeMapping.isPrimitive(javaType) ||
               "String".equals(javaType) ||
               "byte[]".equals(javaType) ||
               "ByteString".equals(javaType);
    }

    // ==================== Message Hierarchy Methods ====================

    /**
     * Collect message names from root to current (e.g., ["Order", "Item"] for Order.Item).
     *
     * <p>Traverses the parent chain to build a complete path from top-level message
     * to the given message.</p>
     *
     * @param message Message to get hierarchy for
     * @return List of message names from root to leaf
     */
    public static List<String> collectMessageHierarchyNames(MergedMessage message) {
        LinkedList<String> names = new LinkedList<>();
        for (MergedMessage current = message; current != null; current = current.getParent()) {
            names.addFirst(current.getName());
        }
        return names;
    }

    /**
     * Build the method name for creating a nested builder via VersionContext.
     *
     * <p>For example, for nested message Order.Item, returns "newOrderItemBuilder".</p>
     *
     * @param message Nested message
     * @return Builder method name
     */
    public static String buildNestedBuilderMethodName(MergedMessage message) {
        return "new" + String.join("", collectMessageHierarchyNames(message)) + "Builder";
    }

    /**
     * Build the qualified name for a nested message.
     *
     * <p>For example, for nested message Order.Item, returns "Order.Item".</p>
     *
     * @param message Nested message
     * @return Dot-separated qualified name
     */
    public static String buildNestedQualifiedName(MergedMessage message) {
        return String.join(".", collectMessageHierarchyNames(message));
    }

    /**
     * Build the interface name for a nested message.
     *
     * <p>Returns the concatenated hierarchy names (e.g., "OrderItem" for Order.Item).</p>
     *
     * @param message Nested message
     * @return Concatenated interface name
     */
    public static String buildNestedInterfaceName(MergedMessage message) {
        return String.join("", collectMessageHierarchyNames(message));
    }

    // ==================== String Utilities ====================

    // ==================== String Check Methods ====================

    /**
     * Check if string is null or empty.
     *
     * <p>Equivalent to Guava's {@code Strings.isNullOrEmpty()}.</p>
     *
     * @param s String to check
     * @return true if null or empty
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Check if string is null, empty, or blank (whitespace only).
     *
     * @param s String to check
     * @return true if null, empty, or blank
     */
    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Return empty string if null, otherwise the original string.
     *
     * @param s String to check
     * @return Original string or empty string if null
     */
    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Return null if empty, otherwise the original string.
     *
     * @param s String to check
     * @return Original string or null if empty
     */
    public static String emptyToNull(String s) {
        return isNullOrEmpty(s) ? null : s;
    }

    /**
     * Capitalize the first character of a string.
     *
     * @param s String to capitalize
     * @return Capitalized string, or original if null/empty
     */
    public static String capitalize(String s) {
        if (isNullOrEmpty(s)) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Uncapitalize the first character of a string.
     *
     * @param s String to uncapitalize
     * @return Uncapitalized string, or original if null/empty
     */
    public static String uncapitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Convert snake_case to camelCase.
     *
     * @param snakeCase Snake case string (e.g., "field_name")
     * @return Camel case string (e.g., "fieldName")
     */
    public static String snakeToCamel(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Convert snake_case to PascalCase.
     *
     * @param snakeCase Snake case string (e.g., "field_name")
     * @return Pascal case string (e.g., "FieldName")
     */
    public static String snakeToPascal(String snakeCase) {
        return capitalize(snakeToCamel(snakeCase));
    }
}
