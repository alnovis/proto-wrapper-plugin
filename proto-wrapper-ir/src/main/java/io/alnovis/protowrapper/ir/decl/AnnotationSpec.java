package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an annotation in the IR.
 *
 * <p>An annotation provides metadata about program elements. It consists of:
 * <ul>
 *   <li>An annotation type (the annotation class)</li>
 *   <li>Optional members (annotation values)</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Marker annotation (no values)
 * @Override
 * @Deprecated
 *
 * // Single-value annotation
 * @SuppressWarnings("unchecked")
 * @Retention(RetentionPolicy.RUNTIME)
 *
 * // Multi-value annotation
 * @RequestMapping(path = "/api", method = RequestMethod.GET)
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Marker annotation: @Override
 * AnnotationSpec override = AnnotationSpec.of(Types.type("java.lang.Override"));
 *
 * // Single-value annotation: @SuppressWarnings("unchecked")
 * AnnotationSpec suppress = AnnotationSpec.of(
 *     Types.type("java.lang.SuppressWarnings"),
 *     Map.of("value", "unchecked")
 * );
 *
 * // Multi-value annotation
 * AnnotationSpec mapping = AnnotationSpec.of(
 *     Types.type("org.springframework.web.bind.annotation.RequestMapping"),
 *     Map.of("path", "/api", "method", "GET")
 * );
 * }</pre>
 *
 * <p><b>Member value types:</b>
 * The values in the members map should be:
 * <ul>
 *   <li>Primitives: Integer, Long, Float, Double, Boolean, Character</li>
 *   <li>String</li>
 *   <li>ClassType (for class literals)</li>
 *   <li>Enum constants (as String for simplicity, emitter resolves)</li>
 *   <li>AnnotationSpec (for nested annotations)</li>
 *   <li>List of any of the above (for array values)</li>
 * </ul>
 *
 * @param type    the annotation type; must not be null
 * @param members the annotation member values; empty map for marker annotations;
 *                the map is copied and made immutable
 * @since 2.4.0
 */
public record AnnotationSpec(
        TypeRef type,
        Map<String, Object> members
) {

    /**
     * Creates a new AnnotationSpec with validation.
     *
     * @param type    the annotation type
     * @param members the member values (may be null, treated as empty map)
     * @throws NullPointerException if type is null
     */
    public AnnotationSpec {
        Objects.requireNonNull(type, "type must not be null");
        members = members == null ? Map.of() : Map.copyOf(members);
    }

    /**
     * Creates a marker annotation (no members).
     *
     * <p>Example:
     * <pre>{@code
     * // @Override
     * AnnotationSpec override = AnnotationSpec.of(Types.type("java.lang.Override"));
     * }</pre>
     *
     * @param type the annotation type
     * @return a marker annotation
     */
    public static AnnotationSpec of(TypeRef type) {
        return new AnnotationSpec(type, Map.of());
    }

    /**
     * Creates an annotation with the specified members.
     *
     * <p>Example:
     * <pre>{@code
     * // @SuppressWarnings("unchecked")
     * AnnotationSpec suppress = AnnotationSpec.of(
     *     Types.type("java.lang.SuppressWarnings"),
     *     Map.of("value", "unchecked")
     * );
     * }</pre>
     *
     * @param type    the annotation type
     * @param members the member values
     * @return an annotation with members
     */
    public static AnnotationSpec of(TypeRef type, Map<String, Object> members) {
        return new AnnotationSpec(type, members);
    }

    /**
     * Creates an annotation with a single "value" member.
     *
     * <p>This is a convenience for annotations that have only a value() member.
     *
     * <p>Example:
     * <pre>{@code
     * // @SuppressWarnings("unchecked")
     * AnnotationSpec suppress = AnnotationSpec.withValue(
     *     Types.type("java.lang.SuppressWarnings"),
     *     "unchecked"
     * );
     * }</pre>
     *
     * @param type  the annotation type
     * @param value the value for the "value" member
     * @return an annotation with a single value member
     */
    public static AnnotationSpec withValue(TypeRef type, Object value) {
        return new AnnotationSpec(type, Map.of("value", value));
    }

    /**
     * Returns {@code true} if this is a marker annotation (no members).
     *
     * @return {@code true} if members map is empty
     */
    public boolean isMarker() {
        return members.isEmpty();
    }

    /**
     * Returns {@code true} if this annotation has a single "value" member.
     *
     * @return {@code true} if members contains only "value"
     */
    public boolean isSingleValue() {
        return members.size() == 1 && members.containsKey("value");
    }

    /**
     * Returns the value of a specific member, or null if not present.
     *
     * @param memberName the name of the member
     * @return the member value, or null if not present
     */
    public Object getMember(String memberName) {
        return members.get(memberName);
    }
}
