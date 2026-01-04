package space.alnovis.protowrapper.diff.model;

/**
 * Enumeration of change types detected during schema comparison.
 */
public enum ChangeType {

    // Entity-level changes
    /**
     * Entity is new in the target version.
     */
    ADDED,

    /**
     * Entity was removed in the target version.
     */
    REMOVED,

    /**
     * Entity exists in both versions but has changes.
     */
    MODIFIED,

    /**
     * Entity is unchanged between versions.
     */
    UNCHANGED,

    // Field-specific changes
    /**
     * Field type has changed.
     */
    TYPE_CHANGED,

    /**
     * Field label changed (optional/required/repeated).
     */
    LABEL_CHANGED,

    /**
     * Field number changed (breaking change).
     */
    NUMBER_CHANGED,

    /**
     * Field name changed but number is the same.
     */
    NAME_CHANGED,

    /**
     * Default value changed.
     */
    DEFAULT_CHANGED,

    // Enum-specific changes
    /**
     * New enum value added.
     */
    VALUE_ADDED,

    /**
     * Enum value was removed.
     */
    VALUE_REMOVED,

    /**
     * Enum value number changed.
     */
    VALUE_NUMBER_CHANGED;

    /**
     * Returns true if this change type represents an addition.
     */
    public boolean isAddition() {
        return this == ADDED || this == VALUE_ADDED;
    }

    /**
     * Returns true if this change type represents a removal.
     */
    public boolean isRemoval() {
        return this == REMOVED || this == VALUE_REMOVED;
    }

    /**
     * Returns true if this change type represents a modification.
     */
    public boolean isModification() {
        return this == MODIFIED || this == TYPE_CHANGED || this == LABEL_CHANGED ||
               this == NUMBER_CHANGED || this == NAME_CHANGED || this == DEFAULT_CHANGED ||
               this == VALUE_NUMBER_CHANGED;
    }

    /**
     * Returns true if this is a potentially breaking change type.
     */
    public boolean isPotentiallyBreaking() {
        return this == REMOVED || this == TYPE_CHANGED || this == LABEL_CHANGED ||
               this == NUMBER_CHANGED || this == VALUE_REMOVED || this == VALUE_NUMBER_CHANGED;
    }
}
