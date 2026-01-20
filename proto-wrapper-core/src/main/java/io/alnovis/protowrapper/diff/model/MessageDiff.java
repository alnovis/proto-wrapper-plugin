package io.alnovis.protowrapper.diff.model;

import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

/**
 * Represents changes to a message between two schema versions.
 *
 * @param messageName          The name of the message
 * @param changeType           The type of change (ADDED, REMOVED, MODIFIED, UNCHANGED)
 * @param v1Message            The message in source version (null if added)
 * @param v2Message            The message in target version (null if removed)
 * @param fieldChanges         List of changes to fields
 * @param nestedMessageChanges List of changes to nested messages
 * @param nestedEnumChanges    List of changes to nested enums
 */
public record MessageDiff(
    String messageName,
    ChangeType changeType,
    MessageInfo v1Message,
    MessageInfo v2Message,
    List<FieldChange> fieldChanges,
    List<MessageDiff> nestedMessageChanges,
    List<EnumDiff> nestedEnumChanges
) {

    /**
     * Creates a diff for an added message.
     *
     * @param message the added message
     * @return diff representing an addition
     */
    public static MessageDiff added(MessageInfo message) {
        return new MessageDiff(
            message.getName(),
            ChangeType.ADDED,
            null,
            message,
            List.of(),
            List.of(),
            List.of()
        );
    }

    /**
     * Creates a diff for a removed message.
     *
     * @param message the removed message
     * @return diff representing a removal
     */
    public static MessageDiff removed(MessageInfo message) {
        return new MessageDiff(
            message.getName(),
            ChangeType.REMOVED,
            message,
            null,
            List.of(),
            List.of(),
            List.of()
        );
    }

    /**
     * Creates a diff for a modified or unchanged message.
     *
     * @param v1 the message in source version
     * @param v2 the message in target version
     * @param fieldChanges the list of field changes
     * @param nestedMessages the list of nested message changes
     * @param nestedEnums the list of nested enum changes
     * @return diff representing a comparison
     */
    public static MessageDiff compared(
            MessageInfo v1,
            MessageInfo v2,
            List<FieldChange> fieldChanges,
            List<MessageDiff> nestedMessages,
            List<EnumDiff> nestedEnums) {

        ChangeType changeType = (fieldChanges.isEmpty() &&
                                 nestedMessages.isEmpty() &&
                                 nestedEnums.isEmpty())
            ? ChangeType.UNCHANGED
            : ChangeType.MODIFIED;

        return new MessageDiff(
            v1.getName(),
            changeType,
            v1,
            v2,
            fieldChanges,
            nestedMessages,
            nestedEnums
        );
    }

    /**
     * Returns the list of added fields.
     *
     * @return list of added field changes
     */
    public List<FieldChange> getAddedFields() {
        return fieldChanges.stream()
            .filter(fc -> fc.changeType() == ChangeType.ADDED)
            .toList();
    }

    /**
     * Returns the list of removed fields.
     *
     * @return list of removed field changes
     */
    public List<FieldChange> getRemovedFields() {
        return fieldChanges.stream()
            .filter(fc -> fc.changeType() == ChangeType.REMOVED)
            .toList();
    }

    /**
     * Returns the list of modified fields (type/label/name changed).
     *
     * @return list of modified field changes
     */
    public List<FieldChange> getModifiedFields() {
        return fieldChanges.stream()
            .filter(fc -> fc.changeType() == ChangeType.MODIFIED ||
                          fc.changeType() == ChangeType.TYPE_CHANGED ||
                          fc.changeType() == ChangeType.LABEL_CHANGED ||
                          fc.changeType() == ChangeType.NAME_CHANGED)
            .toList();
    }

    /**
     * Returns true if this message has any breaking changes.
     *
     * @return true if there are breaking changes
     */
    public boolean hasBreakingChanges() {
        // Message itself removed is breaking
        if (changeType == ChangeType.REMOVED) {
            return true;
        }

        // Check field-level breaking changes
        if (fieldChanges.stream().anyMatch(FieldChange::isBreaking)) {
            return true;
        }

        // Check nested messages
        if (nestedMessageChanges.stream().anyMatch(MessageDiff::hasBreakingChanges)) {
            return true;
        }

        // Check nested enums
        return nestedEnumChanges.stream().anyMatch(EnumDiff::hasBreakingChanges);
    }

    /**
     * Counts total breaking changes in this message and nested types.
     *
     * @return count of breaking changes
     */
    public int countBreakingChanges() {
        int count = 0;

        if (changeType == ChangeType.REMOVED) {
            count++;
        }

        count += (int) fieldChanges.stream().filter(FieldChange::isBreaking).count();
        count += nestedMessageChanges.stream().mapToInt(MessageDiff::countBreakingChanges).sum();
        count += nestedEnumChanges.stream()
            .filter(EnumDiff::hasBreakingChanges)
            .mapToInt(ed -> 1 + (int) ed.valueChanges().stream().filter(EnumValueChange::isBreaking).count())
            .sum();

        return count;
    }

    /**
     * Returns a summary of the changes.
     *
     * @return summary string
     */
    public String getSummary() {
        return switch (changeType) {
            case ADDED -> "Added message: " + messageName;
            case REMOVED -> "Removed message: " + messageName;
            case MODIFIED -> {
                int added = getAddedFields().size();
                int removed = getRemovedFields().size();
                int modified = getModifiedFields().size();
                yield String.format("Modified message %s: +%d fields, -%d fields, ~%d changed",
                    messageName, added, removed, modified);
            }
            default -> messageName + ": " + changeType;
        };
    }

    /**
     * Returns the source file name if available.
     *
     * @return source file name, or null if not available
     */
    public String getSourceFileName() {
        if (v2Message != null && v2Message.getSourceFileName() != null) {
            return v2Message.getSourceFileName();
        }
        if (v1Message != null && v1Message.getSourceFileName() != null) {
            return v1Message.getSourceFileName();
        }
        return null;
    }
}
