package io.alnovis.protowrapper.ir.decl;

/**
 * Enumeration of modifiers for classes, methods, fields, and other declarations.
 *
 * <p>Modifiers control visibility, mutability, inheritance, and other aspects
 * of declared elements.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Access modifiers
 * public class MyClass { }
 * protected void method() { }
 * private int field;
 *
 * // Other modifiers
 * public static final int CONSTANT = 42;
 * public abstract void abstractMethod();
 * public synchronized void syncMethod() { }
 *
 * // Interface default methods
 * default void defaultMethod() { }
 * }</pre>
 *
 * <p>Valid modifier combinations vary by element type:
 * <ul>
 *   <li><b>Classes:</b> PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL, STATIC (inner classes)</li>
 *   <li><b>Interfaces:</b> PUBLIC, PROTECTED, PRIVATE, ABSTRACT (implicit), STATIC (inner)</li>
 *   <li><b>Methods:</b> PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL, STATIC, SYNCHRONIZED, DEFAULT</li>
 *   <li><b>Fields:</b> PUBLIC, PROTECTED, PRIVATE, FINAL, STATIC, TRANSIENT, VOLATILE</li>
 *   <li><b>Constructors:</b> PUBLIC, PROTECTED, PRIVATE</li>
 * </ul>
 *
 * @since 2.4.0
 */
public enum Modifier {
    // ==================== Access Modifiers ====================

    /**
     * Public access modifier.
     * Element is accessible from any class.
     */
    PUBLIC("public"),

    /**
     * Protected access modifier.
     * Element is accessible from the same package and subclasses.
     */
    PROTECTED("protected"),

    /**
     * Private access modifier.
     * Element is accessible only from the declaring class.
     */
    PRIVATE("private"),

    // ==================== Class/Method Modifiers ====================

    /**
     * Static modifier.
     * For methods and fields: belongs to the class rather than instances.
     * For nested classes: can be instantiated without an outer instance.
     */
    STATIC("static"),

    /**
     * Final modifier.
     * For classes: cannot be extended.
     * For methods: cannot be overridden.
     * For fields/variables: cannot be reassigned.
     */
    FINAL("final"),

    /**
     * Abstract modifier.
     * For classes: cannot be instantiated, may have abstract methods.
     * For methods: no implementation, must be overridden.
     */
    ABSTRACT("abstract"),

    /**
     * Default modifier for interface methods.
     * Provides a default implementation in an interface.
     */
    DEFAULT("default"),

    // ==================== Method Modifiers ====================

    /**
     * Synchronized modifier.
     * Method acquires a lock before execution.
     */
    SYNCHRONIZED("synchronized"),

    /**
     * Native modifier.
     * Method is implemented in native code (e.g., C/C++).
     */
    NATIVE("native"),

    /**
     * Strictfp modifier.
     * Floating-point calculations use strict IEEE 754 semantics.
     */
    STRICTFP("strictfp"),

    // ==================== Field Modifiers ====================

    /**
     * Volatile modifier.
     * Field may be modified by multiple threads; reads/writes are not cached.
     */
    VOLATILE("volatile"),

    /**
     * Transient modifier.
     * Field is not serialized.
     */
    TRANSIENT("transient"),

    // ==================== Record/Sealed Modifiers ====================

    /**
     * Sealed modifier (Java 17+).
     * Class/interface can only be extended by permitted types.
     */
    SEALED("sealed"),

    /**
     * Non-sealed modifier (Java 17+).
     * Subclass of a sealed class that allows further unrestricted extension.
     */
    NON_SEALED("non-sealed");

    private final String keyword;

    Modifier(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the Java keyword for this modifier.
     *
     * @return the modifier keyword (e.g., "public", "static", "final")
     */
    public String keyword() {
        return keyword;
    }

    /**
     * Returns {@code true} if this is an access modifier.
     *
     * @return {@code true} for PUBLIC, PROTECTED, or PRIVATE
     */
    public boolean isAccessModifier() {
        return this == PUBLIC || this == PROTECTED || this == PRIVATE;
    }
}
