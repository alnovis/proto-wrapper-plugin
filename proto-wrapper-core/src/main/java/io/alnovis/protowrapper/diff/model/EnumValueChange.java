package io.alnovis.protowrapper.diff.model;

/**
 * Represents a change to an enum value between two schema versions.
 *
 * @param valueName   The name of the enum value
 * @param changeType  The type of change (VALUE_ADDED, VALUE_REMOVED, VALUE_NUMBER_CHANGED)
 * @param v1Number    The value number in source version (null if added)
 * @param v2Number    The value number in target version (null if removed)
 */
public record EnumValueChange(
    String valueName,
    ChangeType changeType,
    Integer v1Number,
    Integer v2Number
) {

    /**
     * Creates a change record for an added enum value.
     *
     * @param name the enum value name
     * @param number the enum value number
     * @return new EnumValueChange for the addition
     */
    public static EnumValueChange added(String name, int number) {
        return new EnumValueChange(name, ChangeType.VALUE_ADDED, null, number);
    }

    /**
     * Creates a change record for a removed enum value.
     *
     * @param name the enum value name
     * @param number the enum value number
     * @return new EnumValueChange for the removal
     */
    public static EnumValueChange removed(String name, int number) {
        return new EnumValueChange(name, ChangeType.VALUE_REMOVED, number, null);
    }

    /**
     * Creates a change record for a changed enum value number.
     *
     * @param name the enum value name
     * @param oldNumber the old enum value number
     * @param newNumber the new enum value number
     * @return new EnumValueChange for the number change
     */
    public static EnumValueChange numberChanged(String name, int oldNumber, int newNumber) {
        return new EnumValueChange(name, ChangeType.VALUE_NUMBER_CHANGED, oldNumber, newNumber);
    }

    /**
     * Returns true if this enum value change is a breaking change.
     * Removing a value or changing its number is breaking.
     *
     * @return true if this is a breaking change
     */
    public boolean isBreaking() {
        return changeType == ChangeType.VALUE_REMOVED ||
               changeType == ChangeType.VALUE_NUMBER_CHANGED;
    }

    /**
     * Returns a summary of the change for display.
     *
     * @return human-readable summary of the change
     */
    public String getSummary() {
        return switch (changeType) {
            case VALUE_ADDED -> "Added value: " + valueName + " (" + v2Number + ")";
            case VALUE_REMOVED -> "Removed value: " + valueName + " (" + v1Number + ")";
            case VALUE_NUMBER_CHANGED -> "Number changed: " + valueName + " (" + v1Number + " -> " + v2Number + ")";
            default -> valueName + ": " + changeType;
        };
    }
}
