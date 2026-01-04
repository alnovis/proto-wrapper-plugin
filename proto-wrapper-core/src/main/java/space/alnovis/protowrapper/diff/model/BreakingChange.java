package space.alnovis.protowrapper.diff.model;

/**
 * Represents a breaking change detected during schema comparison.
 * Breaking changes are modifications that may cause wire incompatibility
 * or require code changes in consumers.
 *
 * @param type        The type of breaking change
 * @param severity    The severity level (ERROR, WARNING, INFO)
 * @param entityPath  Path to the affected entity (e.g., "Order.status" or "PaymentType")
 * @param description Human-readable description of the change
 * @param v1Value     Original value/type in the source version
 * @param v2Value     New value/type in the target version (null if removed)
 */
public record BreakingChange(
    Type type,
    Severity severity,
    String entityPath,
    String description,
    String v1Value,
    String v2Value
) {

    /**
     * Types of breaking changes.
     */
    public enum Type {
        /**
         * A message was removed from the schema.
         */
        MESSAGE_REMOVED,

        /**
         * A field was removed from a message.
         */
        FIELD_REMOVED,

        /**
         * A field number was changed (always breaking for wire format).
         */
        FIELD_NUMBER_CHANGED,

        /**
         * A field type was changed to an incompatible type.
         */
        FIELD_TYPE_INCOMPATIBLE,

        /**
         * An enum was removed from the schema.
         */
        ENUM_REMOVED,

        /**
         * An enum value was removed.
         */
        ENUM_VALUE_REMOVED,

        /**
         * An enum value number was changed.
         */
        ENUM_VALUE_NUMBER_CHANGED,

        /**
         * A required field was added (breaking for existing messages).
         */
        REQUIRED_FIELD_ADDED,

        /**
         * A field label was changed to required.
         */
        LABEL_CHANGED_TO_REQUIRED,

        /**
         * Field changed from repeated to singular or vice versa.
         */
        CARDINALITY_CHANGED,

        /**
         * A oneof group was removed.
         */
        ONEOF_REMOVED,

        /**
         * A field was moved out of a oneof.
         */
        FIELD_MOVED_OUT_OF_ONEOF,

        /**
         * A field was moved into a oneof.
         */
        FIELD_MOVED_INTO_ONEOF
    }

    /**
     * Severity levels for breaking changes.
     */
    public enum Severity {
        /**
         * Definitely breaking - wire format incompatibility.
         */
        ERROR,

        /**
         * Potentially breaking - semantic change that may affect behavior.
         */
        WARNING,

        /**
         * Non-breaking but notable change.
         */
        INFO
    }

    /**
     * Returns true if this is an error-level breaking change.
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /**
     * Returns true if this is a warning-level breaking change.
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    /**
     * Returns a formatted string representation for display.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(type).append(": ");
        sb.append(entityPath);
        if (description != null && !description.isEmpty()) {
            sb.append(" - ").append(description);
        }
        if (v1Value != null && v2Value != null) {
            sb.append(" (").append(v1Value).append(" -> ").append(v2Value).append(")");
        } else if (v1Value != null) {
            sb.append(" (was: ").append(v1Value).append(")");
        } else if (v2Value != null) {
            sb.append(" (now: ").append(v2Value).append(")");
        }
        return sb.toString();
    }
}
