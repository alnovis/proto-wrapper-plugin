package io.alnovis.protowrapper.model;

import java.util.Objects;

/**
 * Validation constraints for a merged field.
 *
 * <p>This record holds validation metadata that will be used to generate
 * Bean Validation (JSR-380) annotations on interface getters.</p>
 *
 * <h2>Constraint Types</h2>
 * <ul>
 *   <li>{@code notNull} - generates {@code @NotNull}</li>
 *   <li>{@code valid} - generates {@code @Valid} for cascading validation</li>
 *   <li>{@code min/max} - generates {@code @Min}/{@code @Max} for numeric fields</li>
 *   <li>{@code pattern} - generates {@code @Pattern} for string fields</li>
 *   <li>{@code sizeMin/sizeMax} - generates {@code @Size} for collections/strings</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldConstraints constraints = FieldConstraints.builder()
 *     .notNull(true)
 *     .min(0L)
 *     .max(100L)
 *     .build();
 * }</pre>
 *
 * @param notNull   whether the field cannot be null
 * @param valid     whether to cascade validation to nested objects
 * @param min       minimum value for numeric fields (inclusive), or null if not constrained
 * @param max       maximum value for numeric fields (inclusive), or null if not constrained
 * @param pattern   regex pattern for string fields, or null if not constrained
 * @param sizeMin   minimum size for collections/strings, or null if not constrained
 * @param sizeMax   maximum size for collections/strings, or null if not constrained
 * @since 2.3.0
 */
public record FieldConstraints(
        boolean notNull,
        boolean valid,
        Long min,
        Long max,
        String pattern,
        Integer sizeMin,
        Integer sizeMax
) {

    /**
     * Returns an empty constraints instance with no constraints set.
     *
     * @return constraints with all values set to defaults (false/null)
     */
    public static FieldConstraints none() {
        return new FieldConstraints(false, false, null, null, null, null, null);
    }

    /**
     * Check if any constraint is set.
     *
     * @return true if at least one constraint is defined
     */
    public boolean hasAnyConstraint() {
        return notNull || valid || min != null || max != null
                || pattern != null || sizeMin != null || sizeMax != null;
    }

    /**
     * Check if this has @Size constraint (sizeMin or sizeMax).
     *
     * @return true if size constraint is defined
     */
    public boolean hasSizeConstraint() {
        return sizeMin != null || sizeMax != null;
    }

    /**
     * Check if this has @Min or @Max constraint.
     *
     * @return true if numeric range constraint is defined
     */
    public boolean hasRangeConstraint() {
        return min != null || max != null;
    }

    /**
     * Merge this constraints with another, taking the most restrictive values.
     *
     * <p>Merge rules:</p>
     * <ul>
     *   <li>{@code notNull/valid}: OR (either true → result true)</li>
     *   <li>{@code min}: take the larger value</li>
     *   <li>{@code max}: take the smaller value</li>
     *   <li>{@code sizeMin}: take the larger value</li>
     *   <li>{@code sizeMax}: take the smaller value</li>
     *   <li>{@code pattern}: take non-null, prefer {@code this} if both set</li>
     * </ul>
     *
     * @param other the other constraints to merge with
     * @return new merged constraints
     */
    public FieldConstraints merge(FieldConstraints other) {
        if (other == null) {
            return this;
        }
        return new FieldConstraints(
                this.notNull || other.notNull,
                this.valid || other.valid,
                mergeMin(this.min, other.min),
                mergeMax(this.max, other.max),
                this.pattern != null ? this.pattern : other.pattern,
                mergeMin(toLong(this.sizeMin), toLong(other.sizeMin)) != null
                        ? mergeMin(toLong(this.sizeMin), toLong(other.sizeMin)).intValue() : null,
                mergeMax(toLong(this.sizeMax), toLong(other.sizeMax)) != null
                        ? mergeMax(toLong(this.sizeMax), toLong(other.sizeMax)).intValue() : null
        );
    }

    private static Long mergeMin(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private static Long mergeMax(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private static Long toLong(Integer i) {
        return i != null ? i.longValue() : null;
    }

    /**
     * Create a new builder for FieldConstraints.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for FieldConstraints.
     */
    public static class Builder {
        private boolean notNull = false;
        private boolean valid = false;
        private Long min = null;
        private Long max = null;
        private String pattern = null;
        private Integer sizeMin = null;
        private Integer sizeMax = null;

        /**
         * Set the notNull constraint.
         *
         * @param notNull whether the field cannot be null
         * @return this builder
         */
        public Builder notNull(boolean notNull) {
            this.notNull = notNull;
            return this;
        }

        /**
         * Set the valid constraint for cascading validation.
         *
         * @param valid whether to cascade validation
         * @return this builder
         */
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        /**
         * Set the minimum value constraint.
         *
         * @param min minimum value (inclusive), or null to remove constraint
         * @return this builder
         */
        public Builder min(Long min) {
            this.min = min;
            return this;
        }

        /**
         * Set the maximum value constraint.
         *
         * @param max maximum value (inclusive), or null to remove constraint
         * @return this builder
         */
        public Builder max(Long max) {
            this.max = max;
            return this;
        }

        /**
         * Set the regex pattern constraint.
         *
         * @param pattern regex pattern, or null to remove constraint
         * @return this builder
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Set the minimum size constraint.
         *
         * @param sizeMin minimum size, or null to remove constraint
         * @return this builder
         */
        public Builder sizeMin(Integer sizeMin) {
            this.sizeMin = sizeMin;
            return this;
        }

        /**
         * Set the maximum size constraint.
         *
         * @param sizeMax maximum size, or null to remove constraint
         * @return this builder
         */
        public Builder sizeMax(Integer sizeMax) {
            this.sizeMax = sizeMax;
            return this;
        }

        /**
         * Build the FieldConstraints.
         *
         * @return the built constraints
         */
        public FieldConstraints build() {
            return new FieldConstraints(notNull, valid, min, max, pattern, sizeMin, sizeMax);
        }
    }
}
