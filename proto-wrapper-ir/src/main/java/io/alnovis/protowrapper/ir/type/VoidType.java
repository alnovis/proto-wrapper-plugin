package io.alnovis.protowrapper.ir.type;

/**
 * Represents the void type in the IR.
 *
 * <p>The void type is used exclusively for method return types to indicate
 * that a method does not return a value. It cannot be used as:
 * <ul>
 *   <li>A variable type</li>
 *   <li>A parameter type</li>
 *   <li>A type argument</li>
 *   <li>An array component type</li>
 * </ul>
 *
 * <p>This class uses the singleton pattern since all void types are identical.
 * Use {@link #INSTANCE} or {@link io.alnovis.protowrapper.dsl.Types#VOID} to
 * access the singleton instance.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Using the singleton
 * TypeRef voidType = VoidType.INSTANCE;
 *
 * // Using the Types DSL (recommended)
 * TypeRef voidType = Types.VOID;
 *
 * // In method declaration
 * MethodDecl setter = Method.method("setValue")
 *     .param("value", Types.INT)
 *     .returns(Types.VOID)
 *     .body(...)
 *     .build();
 * }</pre>
 *
 * <p><b>Note:</b> For methods that return nothing in Kotlin, the emitter should
 * translate this to the {@code Unit} type.
 *
 * @see io.alnovis.protowrapper.dsl.Types#VOID
 * @since 2.4.0
 */
public record VoidType() implements TypeRef {

    /**
     * The singleton instance of VoidType.
     *
     * <p>Since all void types are identical, this instance should be used
     * instead of creating new instances.
     */
    public static final VoidType INSTANCE = new VoidType();

    /**
     * {@inheritDoc}
     *
     * @return the string "void"
     */
    @Override
    public String toDebugString() {
        return "void";
    }
}
