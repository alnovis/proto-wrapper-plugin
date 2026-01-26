package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import io.alnovis.protowrapper.model.FieldConstraints;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates Bean Validation (JSR-380) annotation specs from field constraints.
 *
 * <p>This class converts {@link FieldConstraints} into JavaPoet
 * {@link AnnotationSpec} instances that can be added to generated methods.</p>
 *
 * <h2>Supported Annotations</h2>
 * <ul>
 *   <li>{@code @NotNull} - from constraints package</li>
 *   <li>{@code @Valid} - from base validation package</li>
 *   <li>{@code @Min}, {@code @Max} - numeric range constraints</li>
 *   <li>{@code @Size} - collection/string size constraints</li>
 *   <li>{@code @Pattern} - regex constraints for strings</li>
 * </ul>
 *
 * <h2>Namespace Support</h2>
 * <ul>
 *   <li>"jakarta" (default): {@code jakarta.validation.constraints.*}</li>
 *   <li>"javax": {@code javax.validation.constraints.*}</li>
 * </ul>
 *
 * @since 2.3.0
 */
public class ValidationAnnotationGenerator {

    // Annotation class names (resolved based on style)
    private final ClassName notNullClass;
    private final ClassName validClass;
    private final ClassName minClass;
    private final ClassName maxClass;
    private final ClassName sizeClass;
    private final ClassName patternClass;

    /**
     * Create a new annotation generator with the given configuration.
     *
     * @param config Generator configuration
     */
    public ValidationAnnotationGenerator(GeneratorConfig config) {
        String constraintsPackage = config.getValidationPackage();
        String basePackage = config.getValidationBasePackage();

        // Constraint annotations are in the constraints sub-package
        this.notNullClass = ClassName.get(constraintsPackage, "NotNull");
        this.minClass = ClassName.get(constraintsPackage, "Min");
        this.maxClass = ClassName.get(constraintsPackage, "Max");
        this.sizeClass = ClassName.get(constraintsPackage, "Size");
        this.patternClass = ClassName.get(constraintsPackage, "Pattern");

        // @Valid is in the base validation package
        this.validClass = ClassName.get(basePackage, "Valid");
    }

    /**
     * Generate annotation specs for the given constraints.
     *
     * @param constraints The field constraints
     * @return List of annotation specs (may be empty)
     */
    public List<AnnotationSpec> generate(FieldConstraints constraints) {
        if (constraints == null || !constraints.hasAnyConstraint()) {
            return List.of();
        }

        List<AnnotationSpec> annotations = new ArrayList<>();

        // @NotNull
        if (constraints.notNull()) {
            annotations.add(AnnotationSpec.builder(notNullClass).build());
        }

        // @Valid
        if (constraints.valid()) {
            annotations.add(AnnotationSpec.builder(validClass).build());
        }

        // @Min
        if (constraints.min() != null) {
            annotations.add(AnnotationSpec.builder(minClass)
                    .addMember("value", "$L", constraints.min())
                    .build());
        }

        // @Max
        if (constraints.max() != null) {
            annotations.add(AnnotationSpec.builder(maxClass)
                    .addMember("value", "$L", constraints.max())
                    .build());
        }

        // @Size (min and/or max)
        if (constraints.hasSizeConstraint()) {
            AnnotationSpec.Builder sizeBuilder = AnnotationSpec.builder(sizeClass);
            if (constraints.sizeMin() != null) {
                sizeBuilder.addMember("min", "$L", constraints.sizeMin());
            }
            if (constraints.sizeMax() != null) {
                sizeBuilder.addMember("max", "$L", constraints.sizeMax());
            }
            annotations.add(sizeBuilder.build());
        }

        // @Pattern
        if (constraints.pattern() != null) {
            annotations.add(AnnotationSpec.builder(patternClass)
                    .addMember("regexp", "$S", constraints.pattern())
                    .build());
        }

        return annotations;
    }

    /**
     * Get the @NotNull annotation class name.
     *
     * @return ClassName for @NotNull
     */
    public ClassName getNotNullClass() {
        return notNullClass;
    }

    /**
     * Get the @Valid annotation class name.
     *
     * @return ClassName for @Valid
     */
    public ClassName getValidClass() {
        return validClass;
    }

    /**
     * Get the @Min annotation class name.
     *
     * @return ClassName for @Min
     */
    public ClassName getMinClass() {
        return minClass;
    }

    /**
     * Get the @Max annotation class name.
     *
     * @return ClassName for @Max
     */
    public ClassName getMaxClass() {
        return maxClass;
    }

    /**
     * Get the @Size annotation class name.
     *
     * @return ClassName for @Size
     */
    public ClassName getSizeClass() {
        return sizeClass;
    }

    /**
     * Get the @Pattern annotation class name.
     *
     * @return ClassName for @Pattern
     */
    public ClassName getPatternClass() {
        return patternClass;
    }
}
