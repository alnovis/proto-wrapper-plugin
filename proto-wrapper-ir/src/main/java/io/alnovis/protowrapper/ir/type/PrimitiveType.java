package io.alnovis.protowrapper.ir.type;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a primitive type in the IR (int, long, double, float, boolean, byte, short, char).
 *
 * <p>Primitive types are value types that are not objects. In Java, these correspond
 * to the eight primitive types. When emitting to other languages, the emitter is
 * responsible for mapping these to the appropriate native types.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Using the Types DSL (recommended)
 * TypeRef intType = Types.INT;
 * TypeRef boolType = Types.BOOLEAN;
 *
 * // Direct construction
 * TypeRef longType = new PrimitiveType(PrimitiveKind.LONG);
 * }</pre>
 *
 * <p>Mapping to Java:
 * <table border="1">
 *   <caption>Primitive type mappings</caption>
 *   <tr><th>PrimitiveKind</th><th>Java Type</th><th>Size</th></tr>
 *   <tr><td>INT</td><td>int</td><td>32-bit</td></tr>
 *   <tr><td>LONG</td><td>long</td><td>64-bit</td></tr>
 *   <tr><td>DOUBLE</td><td>double</td><td>64-bit IEEE 754</td></tr>
 *   <tr><td>FLOAT</td><td>float</td><td>32-bit IEEE 754</td></tr>
 *   <tr><td>BOOLEAN</td><td>boolean</td><td>1-bit (JVM uses int)</td></tr>
 *   <tr><td>BYTE</td><td>byte</td><td>8-bit signed</td></tr>
 *   <tr><td>SHORT</td><td>short</td><td>16-bit signed</td></tr>
 *   <tr><td>CHAR</td><td>char</td><td>16-bit unsigned</td></tr>
 * </table>
 *
 * @param kind the kind of primitive type, must not be null
 * @see PrimitiveKind
 * @see io.alnovis.protowrapper.dsl.Types
 * @since 2.4.0
 */
public record PrimitiveType(PrimitiveKind kind) implements TypeRef {

    // Cache of PrimitiveType instances for each kind
    private static final Map<PrimitiveKind, PrimitiveType> CACHE;

    static {
        CACHE = new EnumMap<>(PrimitiveKind.class);
        for (PrimitiveKind k : PrimitiveKind.values()) {
            CACHE.put(k, new PrimitiveType(k));
        }
    }

    /**
     * Creates a new PrimitiveType with the specified kind.
     *
     * @param kind the kind of primitive type
     * @throws NullPointerException if kind is null
     */
    public PrimitiveType {
        Objects.requireNonNull(kind, "kind must not be null");
    }

    /**
     * Returns a cached PrimitiveType for the given kind.
     *
     * <p>This method returns cached instances for better performance
     * and identity comparison.
     *
     * @param kind the primitive type kind
     * @return the cached PrimitiveType instance
     * @throws NullPointerException if kind is null
     */
    public static PrimitiveType of(PrimitiveKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return CACHE.get(kind);
    }

    /**
     * Enumeration of primitive type kinds.
     *
     * <p>Each kind corresponds to a Java primitive type and provides
     * the Java keyword representation.
     */
    public enum PrimitiveKind {
        /**
         * 32-bit signed integer type.
         * Java keyword: {@code int}
         */
        INT("int"),

        /**
         * 64-bit signed integer type.
         * Java keyword: {@code long}
         */
        LONG("long"),

        /**
         * 64-bit IEEE 754 floating-point type.
         * Java keyword: {@code double}
         */
        DOUBLE("double"),

        /**
         * 32-bit IEEE 754 floating-point type.
         * Java keyword: {@code float}
         */
        FLOAT("float"),

        /**
         * Boolean type with values {@code true} and {@code false}.
         * Java keyword: {@code boolean}
         */
        BOOLEAN("boolean"),

        /**
         * 8-bit signed integer type.
         * Java keyword: {@code byte}
         */
        BYTE("byte"),

        /**
         * 16-bit signed integer type.
         * Java keyword: {@code short}
         */
        SHORT("short"),

        /**
         * 16-bit unsigned Unicode character type.
         * Java keyword: {@code char}
         */
        CHAR("char");

        private final String keyword;

        PrimitiveKind(String keyword) {
            this.keyword = keyword;
        }

        /**
         * Returns the Java keyword for this primitive type.
         *
         * @return the Java keyword (e.g., "int", "boolean")
         */
        public String keyword() {
            return keyword;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the Java keyword for this primitive type (e.g., "int", "boolean")
     */
    @Override
    public String toDebugString() {
        return kind.keyword();
    }
}
