package space.alnovis.protowrapper.diff.model;

/**
 * Classification of type conflicts between schema versions.
 * Indicates how the proto-wrapper plugin handles each type of conflict.
 */
public enum TypeConflictType {

    /** No type conflict - same type in all versions */
    NONE(Handling.NATIVE, "Types are identical"),

    /** int ↔ enum conflict (plugin uses int with enum helper methods) */
    INT_ENUM(Handling.CONVERTED, "Plugin uses int type with enum helper methods"),

    /** enum ↔ enum conflict (different enum types, plugin uses int) */
    ENUM_ENUM(Handling.CONVERTED, "Plugin uses int type for unified access"),

    /** Integer type widening: int32 → int64 (plugin uses long) */
    WIDENING(Handling.CONVERTED, "Plugin uses wider type (long)"),

    /** Float type widening: float → double (plugin uses double) */
    FLOAT_DOUBLE(Handling.CONVERTED, "Plugin uses double type"),

    /** Signed/unsigned conflict: int32 ↔ uint32 (plugin uses long for safety) */
    SIGNED_UNSIGNED(Handling.CONVERTED, "Plugin uses long type for unsigned safety"),

    /** Repeated ↔ singular conflict (plugin uses List for both) */
    REPEATED_SINGLE(Handling.CONVERTED, "Plugin uses List<T> for unified access"),

    /** Optional ↔ required conflict (plugin handles via hasX() methods) */
    OPTIONAL_REQUIRED(Handling.NATIVE, "Plugin provides hasX() method for checking"),

    /** string ↔ bytes conflict (requires manual conversion) */
    STRING_BYTES(Handling.MANUAL, "Requires getBytes()/new String() conversion"),

    /** Type narrowing: int64 → int32 (potential data loss) */
    NARROWING(Handling.WARNING, "Potential data loss on narrowing conversion"),

    /** Primitive to message conflict (plugin generates dual accessors) */
    PRIMITIVE_MESSAGE(Handling.CONVERTED, "Plugin generates getXxx() and getXxxMessage() accessors"),

    /** Other incompatible types (not convertible) */
    INCOMPATIBLE(Handling.INCOMPATIBLE, "Incompatible type change");

    /**
     * How the plugin handles this type of conflict.
     */
    public enum Handling {
        /** No special handling needed - types are compatible */
        NATIVE,
        /** Plugin automatically converts between types */
        CONVERTED,
        /** Conversion possible but requires manual code */
        MANUAL,
        /** Works but may have issues (data loss, etc.) */
        WARNING,
        /** Types are fundamentally incompatible */
        INCOMPATIBLE
    }

    private final Handling handling;
    private final String pluginNote;

    TypeConflictType(Handling handling, String pluginNote) {
        this.handling = handling;
        this.pluginNote = pluginNote;
    }

    /**
     * Returns how the plugin handles this conflict type.
     *
     * @return the handling type
     */
    public Handling getHandling() {
        return handling;
    }

    /**
     * Returns a note about how the plugin handles this conflict.
     *
     * @return the plugin note
     */
    public String getPluginNote() {
        return pluginNote;
    }

    /**
     * Returns true if the plugin can automatically handle this conflict.
     *
     * @return true if plugin-handled
     */
    public boolean isPluginHandled() {
        return handling == Handling.NATIVE || handling == Handling.CONVERTED;
    }

    /**
     * Returns true if this conflict is a breaking change.
     * Plugin-handled conflicts are not considered breaking.
     *
     * @return true if breaking
     */
    public boolean isBreaking() {
        return handling == Handling.INCOMPATIBLE;
    }

    /**
     * Returns true if this conflict should be shown as a warning.
     *
     * @return true if warning
     */
    public boolean isWarning() {
        return handling == Handling.WARNING || handling == Handling.MANUAL;
    }

    /**
     * Returns the severity for breaking change detection.
     *
     * @return the severity level
     */
    public BreakingChange.Severity getSeverity() {
        return switch (handling) {
            case NATIVE, CONVERTED -> BreakingChange.Severity.INFO;
            case MANUAL, WARNING -> BreakingChange.Severity.WARNING;
            case INCOMPATIBLE -> BreakingChange.Severity.ERROR;
        };
    }
}
