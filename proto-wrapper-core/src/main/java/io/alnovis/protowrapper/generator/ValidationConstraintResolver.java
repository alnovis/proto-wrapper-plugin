package io.alnovis.protowrapper.generator;

import io.alnovis.protowrapper.model.FieldConstraints;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;

import java.util.Set;

/**
 * Resolves validation constraints for merged fields.
 *
 * <p>This class analyzes merged fields and determines which Bean Validation
 * (JSR-380) annotations should be generated based on:</p>
 * <ul>
 *   <li>Proto field metadata (required, optional, repeated, message type)</li>
 *   <li>Field presence across versions (universal vs version-specific)</li>
 *   <li>Type conflicts (skip @NotNull for complex semantics)</li>
 *   <li>Custom proto options (from validate.proto extension)</li>
 * </ul>
 *
 * <h2>Auto-detection Rules</h2>
 * <table border="1">
 *   <tr><th>Condition</th><th>Annotation</th></tr>
 *   <tr><td>Required in all versions (proto2)</td><td>@NotNull</td></tr>
 *   <tr><td>Universal message field (non-optional)</td><td>@NotNull + @Valid</td></tr>
 *   <tr><td>Optional message field</td><td>@Valid (no @NotNull)</td></tr>
 *   <tr><td>Repeated/Map field</td><td>@NotNull (never null, empty list)</td></tr>
 *   <tr><td>Repeated message</td><td>@NotNull + element @Valid</td></tr>
 * </table>
 *
 * <h2>Skip @NotNull for</h2>
 * <ul>
 *   <li>Primitive return types (int, long, boolean - cannot be null)</li>
 *   <li>Fields with type conflicts (complex semantics)</li>
 *   <li>Version-specific fields (can return null if not in version)</li>
 *   <li>Oneof fields (only one field active at a time)</li>
 * </ul>
 *
 * @since 2.3.0
 */
public class ValidationConstraintResolver {

    private final GeneratorConfig config;

    /**
     * Create a new resolver with the given configuration.
     *
     * @param config Generator configuration
     */
    public ValidationConstraintResolver(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Resolve validation constraints for a merged field.
     *
     * @param field      The merged field to analyze
     * @param message    The parent message (for context)
     * @param allVersions All available versions in the schema
     * @return FieldConstraints with resolved validation rules
     */
    public FieldConstraints resolve(MergedField field, MergedMessage message, Set<String> allVersions) {
        if (!config.isGenerateValidationAnnotations()) {
            return FieldConstraints.none();
        }

        FieldConstraints.Builder builder = FieldConstraints.builder();

        // Determine @NotNull
        boolean shouldBeNotNull = resolveNotNull(field, allVersions);
        builder.notNull(shouldBeNotNull);

        // Determine @Valid (cascading validation)
        boolean shouldBeValid = resolveValid(field);
        builder.valid(shouldBeValid);

        // TODO: Custom proto options parsing (Phase 2)
        // FieldConstraints customConstraints = parseCustomConstraints(field);
        // return builder.build().merge(customConstraints);

        return builder.build();
    }

    /**
     * Resolve @NotNull constraint for a field.
     *
     * <p>Rules for @NotNull:</p>
     * <ul>
     *   <li>Skip for primitive return types (int, long, boolean, etc.)</li>
     *   <li>Skip for fields with type conflicts</li>
     *   <li>Skip for version-specific fields (not universal)</li>
     *   <li>Skip for oneof fields</li>
     *   <li>Apply for repeated/map fields (never null, empty list instead)</li>
     *   <li>Apply for required fields in all versions</li>
     *   <li>Apply for universal non-optional message fields</li>
     * </ul>
     *
     * @param field The merged field
     * @param allVersions All available versions
     * @return true if @NotNull should be generated
     */
    private boolean resolveNotNull(MergedField field, Set<String> allVersions) {
        // Skip for primitive return types (cannot be null)
        if (isPrimitiveReturnType(field)) {
            return false;
        }

        // Skip for fields with type conflicts (complex semantics)
        if (field.hasTypeConflict()) {
            return false;
        }

        // Skip for oneof fields (only one field active at a time, others return null)
        if (field.isInOneof()) {
            return false;
        }

        // Skip for version-specific fields (can return null if not in version)
        if (!field.isUniversal(allVersions)) {
            return false;
        }

        // Repeated and map fields are never null (empty list/map instead)
        if (field.isRepeated() || field.isMap()) {
            return true;
        }

        // Check if field is required in all versions
        if (isRequiredInAllVersions(field)) {
            return true;
        }

        // Universal non-optional message field
        return field.isMessage() && !field.isEffectivelyOptional();
    }

    /**
     * Resolve @Valid constraint for a field.
     *
     * <p>@Valid triggers cascading validation for nested objects.
     * It should be applied to message-type fields so that the
     * validation framework validates nested objects.</p>
     *
     * @param field The merged field
     * @return true if @Valid should be generated
     */
    private boolean resolveValid(MergedField field) {
        // @Valid for message-type fields (including well-known types that unwrap to objects)
        if (field.isMessage()) {
            // Skip if WKT unwraps to primitive (e.g., Timestamp -> Instant is okay, but consider)
            // For now, apply @Valid to all message types
            return true;
        }

        // @Valid for repeated message elements (List<@Valid T>)
        // This is handled at annotation generation level, not here
        // The resolver just marks the field as needing @Valid
        if (field.isRepeated() && isMessageElementType(field)) {
            return true;
        }

        // @Valid for map with message values
        return field.isMap() && field.getMapInfo() != null && field.getMapInfo().hasMessageValue();
    }

    /**
     * Check if field is required in all versions.
     *
     * <p>In proto2, fields can be marked as required. This checks if the field
     * is required (not optional) in ALL versions where it's present.</p>
     *
     * @param field The merged field
     * @return true if required in all versions
     */
    private boolean isRequiredInAllVersions(MergedField field) {
        // Check optionality per version - none must be optional (all required)
        return field.getOptionalityPerVersion().values().stream()
                .noneMatch(Boolean::booleanValue);
    }

    /**
     * Check if the field has a primitive return type.
     *
     * <p>Primitive types cannot be null in Java, so @NotNull is meaningless.</p>
     *
     * @param field The merged field
     * @return true if getter returns primitive type
     */
    private boolean isPrimitiveReturnType(MergedField field) {
        String type = field.getGetterType();
        return switch (type) {
            case "int", "long", "float", "double", "boolean", "byte", "short", "char" -> true;
            default -> false;
        };
    }

    /**
     * Check if repeated field has message element type.
     *
     * @param field The merged field (must be repeated)
     * @return true if element type is a message
     */
    private boolean isMessageElementType(MergedField field) {
        // For repeated fields, check if the base type is a message
        // The java type for repeated is like List<SomeMessage>
        // We need to check if SomeMessage is a message type
        // This is approximated by checking if field.isMessage() was originally true
        // before it was wrapped in List
        return field.isMessage();
    }
}
