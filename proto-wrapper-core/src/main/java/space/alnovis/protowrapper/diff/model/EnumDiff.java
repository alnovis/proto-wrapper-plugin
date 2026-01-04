package space.alnovis.protowrapper.diff.model;

import space.alnovis.protowrapper.model.EnumInfo;

import java.util.List;

/**
 * Represents changes to an enum between two schema versions.
 *
 * @param enumName     The name of the enum
 * @param changeType   The type of change (ADDED, REMOVED, MODIFIED, UNCHANGED)
 * @param v1Enum       The enum in source version (null if added)
 * @param v2Enum       The enum in target version (null if removed)
 * @param valueChanges List of changes to enum values
 */
public record EnumDiff(
    String enumName,
    ChangeType changeType,
    EnumInfo v1Enum,
    EnumInfo v2Enum,
    List<EnumValueChange> valueChanges
) {

    /**
     * Creates a diff for an added enum.
     */
    public static EnumDiff added(EnumInfo enumInfo) {
        return new EnumDiff(
            enumInfo.getName(),
            ChangeType.ADDED,
            null,
            enumInfo,
            List.of()
        );
    }

    /**
     * Creates a diff for a removed enum.
     */
    public static EnumDiff removed(EnumInfo enumInfo) {
        return new EnumDiff(
            enumInfo.getName(),
            ChangeType.REMOVED,
            enumInfo,
            null,
            List.of()
        );
    }

    /**
     * Creates a diff for a modified enum.
     */
    public static EnumDiff modified(EnumInfo v1, EnumInfo v2, List<EnumValueChange> changes) {
        return new EnumDiff(
            v1.getName(),
            changes.isEmpty() ? ChangeType.UNCHANGED : ChangeType.MODIFIED,
            v1,
            v2,
            changes
        );
    }

    /**
     * Returns the list of added enum values.
     */
    public List<EnumValueChange> getAddedValues() {
        return valueChanges.stream()
            .filter(vc -> vc.changeType() == ChangeType.VALUE_ADDED)
            .toList();
    }

    /**
     * Returns the list of removed enum values.
     */
    public List<EnumValueChange> getRemovedValues() {
        return valueChanges.stream()
            .filter(vc -> vc.changeType() == ChangeType.VALUE_REMOVED)
            .toList();
    }

    /**
     * Returns the list of values with changed numbers.
     */
    public List<EnumValueChange> getChangedValues() {
        return valueChanges.stream()
            .filter(vc -> vc.changeType() == ChangeType.VALUE_NUMBER_CHANGED)
            .toList();
    }

    /**
     * Returns true if this enum has breaking changes.
     */
    public boolean hasBreakingChanges() {
        if (changeType == ChangeType.REMOVED) {
            return true;
        }
        return valueChanges.stream().anyMatch(EnumValueChange::isBreaking);
    }

    /**
     * Returns a summary of the changes.
     */
    public String getSummary() {
        return switch (changeType) {
            case ADDED -> "Added enum: " + enumName;
            case REMOVED -> "Removed enum: " + enumName;
            case MODIFIED -> {
                int added = getAddedValues().size();
                int removed = getRemovedValues().size();
                int changed = getChangedValues().size();
                yield String.format("Modified enum %s: +%d values, -%d values, ~%d changed",
                    enumName, added, removed, changed);
            }
            default -> enumName + ": " + changeType;
        };
    }
}
