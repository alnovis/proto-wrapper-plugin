package io.alnovis.protowrapper.contract;

import java.util.Objects;

/**
 * Holds all method names for a field.
 *
 * <p>This record centralizes method name generation, ensuring consistency
 * across all generators. Method names are derived from the field's Java name
 * following protobuf conventions.</p>
 *
 * @param javaName the Java field name (e.g., "userId")
 * @param capitalizedName the capitalized name for method suffixes (e.g., "UserId")
 */
public record FieldMethodNames(
        String javaName,
        String capitalizedName
) {

    public FieldMethodNames {
        Objects.requireNonNull(javaName, "javaName must not be null");
        Objects.requireNonNull(capitalizedName, "capitalizedName must not be null");
    }

    /**
     * Creates FieldMethodNames from a Java field name.
     *
     * @param javaName the Java field name (e.g., "userId")
     * @return the method names record
     */
    public static FieldMethodNames from(String javaName) {
        Objects.requireNonNull(javaName, "javaName must not be null");
        return new FieldMethodNames(javaName, capitalize(javaName));
    }

    // ==================== Getter Method Names ====================

    /**
     * @return getter method name (e.g., "getUserId")
     */
    public String getterName() {
        return "get" + capitalizedName;
    }

    /**
     * @return boolean getter method name (e.g., "isActive" for bool fields)
     */
    public String booleanGetterName() {
        return "is" + capitalizedName;
    }

    // ==================== Has Method Names ====================

    /**
     * @return has method name (e.g., "hasUserId")
     */
    public String hasMethodName() {
        return "has" + capitalizedName;
    }

    // ==================== Extract Method Names (Abstract) ====================

    /**
     * @return extract method name (e.g., "extractUserId")
     */
    public String extractMethodName() {
        return "extract" + capitalizedName;
    }

    /**
     * @return extract has method name (e.g., "extractHasUserId")
     */
    public String extractHasMethodName() {
        return "extractHas" + capitalizedName;
    }

    // ==================== Builder Method Names ====================

    /**
     * @return setter method name (e.g., "setUserId")
     */
    public String setterName() {
        return "set" + capitalizedName;
    }

    /**
     * @return clear method name (e.g., "clearUserId")
     */
    public String clearMethodName() {
        return "clear" + capitalizedName;
    }

    /**
     * @return add method name for repeated fields (e.g., "addItem")
     */
    public String addMethodName() {
        return "add" + capitalizedName;
    }

    /**
     * @return addAll method name for repeated fields (e.g., "addAllItems")
     */
    public String addAllMethodName() {
        return "addAll" + capitalizedName;
    }

    // ==================== Do-Method Names (Abstract Builder) ====================

    /**
     * @return doSet method name (e.g., "doSetUserId")
     */
    public String doSetMethodName() {
        return "doSet" + capitalizedName;
    }

    /**
     * @return doClear method name (e.g., "doClearUserId")
     */
    public String doClearMethodName() {
        return "doClear" + capitalizedName;
    }

    /**
     * @return doAdd method name for repeated fields (e.g., "doAddItem")
     */
    public String doAddMethodName() {
        return "doAdd" + capitalizedName;
    }

    /**
     * @return doAddAll method name for repeated fields (e.g., "doAddAllItems")
     */
    public String doAddAllMethodName() {
        return "doAddAll" + capitalizedName;
    }

    // ==================== Utility ====================

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
