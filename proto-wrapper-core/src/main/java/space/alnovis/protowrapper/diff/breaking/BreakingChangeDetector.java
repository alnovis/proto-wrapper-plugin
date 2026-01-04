package space.alnovis.protowrapper.diff.breaking;

import space.alnovis.protowrapper.diff.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects breaking changes in schema differences.
 * A breaking change is a modification that may cause wire format incompatibility
 * or require code changes in consumers.
 */
public class BreakingChangeDetector {

    /**
     * Detects all breaking changes in the provided message and enum diffs.
     *
     * @param messageDiffs List of message differences
     * @param enumDiffs    List of enum differences
     * @return List of detected breaking changes
     */
    public List<BreakingChange> detectAll(List<MessageDiff> messageDiffs, List<EnumDiff> enumDiffs) {
        List<BreakingChange> breaking = new ArrayList<>();

        // Detect message-level breaking changes
        for (MessageDiff md : messageDiffs) {
            detectMessageBreaking(md, "", breaking);
        }

        // Detect enum-level breaking changes
        for (EnumDiff ed : enumDiffs) {
            detectEnumBreaking(ed, "", breaking);
        }

        return breaking;
    }

    /**
     * Detects breaking changes in a message diff, including nested types.
     *
     * @param md       The message diff to analyze
     * @param prefix   Parent path prefix (for nested messages)
     * @param breaking List to add detected breaking changes to
     */
    private void detectMessageBreaking(MessageDiff md, String prefix, List<BreakingChange> breaking) {
        String path = prefix.isEmpty() ? md.messageName() : prefix + "." + md.messageName();

        // Message removed is breaking
        if (md.changeType() == ChangeType.REMOVED) {
            breaking.add(new BreakingChange(
                BreakingChange.Type.MESSAGE_REMOVED,
                BreakingChange.Severity.ERROR,
                path,
                "Message removed",
                md.messageName(),
                null
            ));
            return; // No need to check fields if message is removed
        }

        // Check field-level breaking changes
        for (FieldChange fc : md.fieldChanges()) {
            detectFieldBreaking(fc, path, breaking);
        }

        // Recursively check nested messages
        for (MessageDiff nested : md.nestedMessageChanges()) {
            detectMessageBreaking(nested, path, breaking);
        }

        // Check nested enums
        for (EnumDiff nested : md.nestedEnumChanges()) {
            detectEnumBreaking(nested, path, breaking);
        }
    }

    /**
     * Detects breaking changes in a field change.
     */
    private void detectFieldBreaking(FieldChange fc, String messagePath, List<BreakingChange> breaking) {
        String fieldPath = messagePath + "." + fc.fieldName();

        switch (fc.changeType()) {
            case REMOVED -> breaking.add(new BreakingChange(
                BreakingChange.Type.FIELD_REMOVED,
                BreakingChange.Severity.ERROR,
                fieldPath,
                "Field removed",
                formatFieldInfo(fc.v1Field()),
                null
            ));

            case TYPE_CHANGED -> {
                if (!fc.isCompatibleTypeChange()) {
                    breaking.add(new BreakingChange(
                        BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
                        BreakingChange.Severity.ERROR,
                        fieldPath,
                        "Incompatible type change",
                        FieldChange.formatType(fc.v1Field()),
                        FieldChange.formatType(fc.v2Field())
                    ));
                } else {
                    // Compatible type change is just a warning
                    breaking.add(new BreakingChange(
                        BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
                        BreakingChange.Severity.WARNING,
                        fieldPath,
                        "Type changed (compatible: " + fc.getCompatibilityNote() + ")",
                        FieldChange.formatType(fc.v1Field()),
                        FieldChange.formatType(fc.v2Field())
                    ));
                }
            }

            case LABEL_CHANGED -> {
                // repeated <-> singular is breaking
                if (fc.v1Field() != null && fc.v2Field() != null &&
                    fc.v1Field().isRepeated() != fc.v2Field().isRepeated()) {
                    breaking.add(new BreakingChange(
                        BreakingChange.Type.CARDINALITY_CHANGED,
                        BreakingChange.Severity.ERROR,
                        fieldPath,
                        "Cardinality changed",
                        fc.v1Field().isRepeated() ? "repeated" : "singular",
                        fc.v2Field().isRepeated() ? "repeated" : "singular"
                    ));
                }
            }

            case MODIFIED -> {
                // Check for oneof membership changes
                if (fc.v1Field() != null && fc.v2Field() != null) {
                    if (fc.v1Field().isInOneof() && !fc.v2Field().isInOneof()) {
                        breaking.add(new BreakingChange(
                            BreakingChange.Type.FIELD_MOVED_OUT_OF_ONEOF,
                            BreakingChange.Severity.WARNING,
                            fieldPath,
                            "Field moved out of oneof",
                            "oneof " + fc.v1Field().getOneofName(),
                            "standalone field"
                        ));
                    } else if (!fc.v1Field().isInOneof() && fc.v2Field().isInOneof()) {
                        breaking.add(new BreakingChange(
                            BreakingChange.Type.FIELD_MOVED_INTO_ONEOF,
                            BreakingChange.Severity.WARNING,
                            fieldPath,
                            "Field moved into oneof",
                            "standalone field",
                            "oneof " + fc.v2Field().getOneofName()
                        ));
                    }
                }
            }

            default -> {
                // NAME_CHANGED and others are not breaking
            }
        }
    }

    /**
     * Detects breaking changes in an enum diff.
     */
    private void detectEnumBreaking(EnumDiff ed, String prefix, List<BreakingChange> breaking) {
        String path = prefix.isEmpty() ? ed.enumName() : prefix + "." + ed.enumName();

        // Enum removed is breaking
        if (ed.changeType() == ChangeType.REMOVED) {
            breaking.add(new BreakingChange(
                BreakingChange.Type.ENUM_REMOVED,
                BreakingChange.Severity.ERROR,
                path,
                "Enum removed",
                ed.enumName(),
                null
            ));
            return;
        }

        // Check enum value changes
        for (EnumValueChange vc : ed.valueChanges()) {
            detectEnumValueBreaking(vc, path, breaking);
        }
    }

    /**
     * Detects breaking changes in enum value changes.
     */
    private void detectEnumValueBreaking(EnumValueChange vc, String enumPath, List<BreakingChange> breaking) {
        String valuePath = enumPath + "." + vc.valueName();

        switch (vc.changeType()) {
            case VALUE_REMOVED -> breaking.add(new BreakingChange(
                BreakingChange.Type.ENUM_VALUE_REMOVED,
                BreakingChange.Severity.ERROR,
                valuePath,
                "Enum value removed",
                vc.valueName() + " = " + vc.v1Number(),
                null
            ));

            case VALUE_NUMBER_CHANGED -> breaking.add(new BreakingChange(
                BreakingChange.Type.ENUM_VALUE_NUMBER_CHANGED,
                BreakingChange.Severity.ERROR,
                valuePath,
                "Enum value number changed",
                String.valueOf(vc.v1Number()),
                String.valueOf(vc.v2Number())
            ));

            case VALUE_ADDED -> {
                // Adding a value is not breaking by itself
                // But we could add an INFO-level note if desired
            }

            default -> {
                // No action for other types
            }
        }
    }

    /**
     * Formats field info for display in breaking change messages.
     */
    private String formatFieldInfo(space.alnovis.protowrapper.model.FieldInfo field) {
        if (field == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        if (field.isRepeated()) {
            sb.append("repeated ");
        }
        sb.append(FieldChange.formatType(field));
        sb.append(" ").append(field.getProtoName());
        sb.append(" = ").append(field.getNumber());
        return sb.toString();
    }
}
