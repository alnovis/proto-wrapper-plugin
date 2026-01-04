package space.alnovis.protowrapper.diff.breaking;

import space.alnovis.protowrapper.diff.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects schema changes and classifies them based on plugin handling capabilities.
 *
 * <p>Severity levels are based on what the proto-wrapper plugin can handle:</p>
 * <ul>
 *   <li><b>INFO</b> - Plugin handles automatically (removed entities, type conversions)</li>
 *   <li><b>WARNING</b> - May need attention but plugin can handle (narrowing, semantic changes)</li>
 *   <li><b>ERROR</b> - Plugin cannot handle (primitive ↔ message incompatibility)</li>
 * </ul>
 */
public class BreakingChangeDetector {

    /**
     * Detects all schema changes in the provided message and enum diffs.
     *
     * @param messageDiffs List of message differences
     * @param enumDiffs    List of enum differences
     * @return List of detected changes with appropriate severity levels
     */
    public List<BreakingChange> detectAll(List<MessageDiff> messageDiffs, List<EnumDiff> enumDiffs) {
        List<BreakingChange> changes = new ArrayList<>();

        for (MessageDiff md : messageDiffs) {
            detectMessageChanges(md, "", changes);
        }

        for (EnumDiff ed : enumDiffs) {
            detectEnumChanges(ed, "", changes);
        }

        return changes;
    }

    /**
     * Detects changes in a message diff, including nested types.
     */
    private void detectMessageChanges(MessageDiff md, String prefix, List<BreakingChange> changes) {
        String path = prefix.isEmpty() ? md.messageName() : prefix + "." + md.messageName();

        // Message removed - plugin handles by generating code only for versions where it exists
        if (md.changeType() == ChangeType.REMOVED) {
            changes.add(new BreakingChange(
                BreakingChange.Type.MESSAGE_REMOVED,
                BreakingChange.Severity.INFO,
                path,
                "Plugin generates version-specific code; message available only in source version",
                md.messageName(),
                null
            ));
            return;
        }

        // Check field-level changes
        for (FieldChange fc : md.fieldChanges()) {
            detectFieldChanges(fc, path, changes);
        }

        // Recursively check nested messages
        for (MessageDiff nested : md.nestedMessageChanges()) {
            detectMessageChanges(nested, path, changes);
        }

        // Check nested enums
        for (EnumDiff nested : md.nestedEnumChanges()) {
            detectEnumChanges(nested, path, changes);
        }
    }

    /**
     * Detects changes in a field change.
     */
    private void detectFieldChanges(FieldChange fc, String messagePath, List<BreakingChange> changes) {
        String fieldPath = messagePath + "." + fc.fieldName();

        switch (fc.changeType()) {
            case REMOVED -> {
                // Field removed - plugin handles with hasXxx/supportsXxx methods
                changes.add(new BreakingChange(
                    BreakingChange.Type.FIELD_REMOVED,
                    BreakingChange.Severity.INFO,
                    fieldPath,
                    "Plugin generates hasXxx()/supportsXxx() for version checking",
                    formatFieldInfo(fc.v1Field()),
                    null
                ));
            }

            case TYPE_CHANGED -> {
                TypeConflictType conflictType = fc.getTypeConflictType();
                BreakingChange.Severity severity = conflictType.getSeverity();
                BreakingChange.Type changeType = conflictType.isPluginHandled()
                    ? BreakingChange.Type.FIELD_TYPE_CONVERTED
                    : BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE;

                String description = conflictType.isPluginHandled()
                    ? conflictType.name() + ": " + conflictType.getPluginNote()
                    : "Incompatible type change: " + conflictType.getPluginNote();

                changes.add(new BreakingChange(
                    changeType,
                    severity,
                    fieldPath,
                    description,
                    FieldChange.formatType(fc.v1Field()),
                    FieldChange.formatType(fc.v2Field())
                ));
            }

            case LABEL_CHANGED -> {
                // repeated <-> singular - plugin handles with REPEATED_SINGLE conflict type
                if (fc.v1Field() != null && fc.v2Field() != null &&
                    fc.v1Field().isRepeated() != fc.v2Field().isRepeated()) {
                    changes.add(new BreakingChange(
                        BreakingChange.Type.CARDINALITY_CHANGED,
                        BreakingChange.Severity.INFO,
                        fieldPath,
                        "Plugin uses List<T> for unified access across versions",
                        fc.v1Field().isRepeated() ? "repeated" : "singular",
                        fc.v2Field().isRepeated() ? "repeated" : "singular"
                    ));
                }
            }

            case MODIFIED -> {
                // Check for oneof membership changes
                if (fc.v1Field() != null && fc.v2Field() != null) {
                    if (fc.v1Field().isInOneof() && !fc.v2Field().isInOneof()) {
                        changes.add(new BreakingChange(
                            BreakingChange.Type.FIELD_MOVED_OUT_OF_ONEOF,
                            BreakingChange.Severity.WARNING,
                            fieldPath,
                            "Semantic change - field moved out of oneof",
                            "oneof " + fc.v1Field().getOneofName(),
                            "standalone field"
                        ));
                    } else if (!fc.v1Field().isInOneof() && fc.v2Field().isInOneof()) {
                        changes.add(new BreakingChange(
                            BreakingChange.Type.FIELD_MOVED_INTO_ONEOF,
                            BreakingChange.Severity.WARNING,
                            fieldPath,
                            "Semantic change - field moved into oneof",
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
     * Detects changes in an enum diff.
     */
    private void detectEnumChanges(EnumDiff ed, String prefix, List<BreakingChange> changes) {
        String path = prefix.isEmpty() ? ed.enumName() : prefix + "." + ed.enumName();

        // Enum removed - plugin handles similarly to message removal
        if (ed.changeType() == ChangeType.REMOVED) {
            changes.add(new BreakingChange(
                BreakingChange.Type.ENUM_REMOVED,
                BreakingChange.Severity.INFO,
                path,
                "Plugin generates version-specific code; enum available only in source version",
                ed.enumName(),
                null
            ));
            return;
        }

        // Check enum value changes
        for (EnumValueChange vc : ed.valueChanges()) {
            detectEnumValueChanges(vc, path, changes);
        }
    }

    /**
     * Detects changes in enum value changes.
     */
    private void detectEnumValueChanges(EnumValueChange vc, String enumPath, List<BreakingChange> changes) {
        String valuePath = enumPath + "." + vc.valueName();

        switch (vc.changeType()) {
            case VALUE_REMOVED -> {
                // Enum value removed - plugin uses int values or unified enum
                changes.add(new BreakingChange(
                    BreakingChange.Type.ENUM_VALUE_REMOVED,
                    BreakingChange.Severity.INFO,
                    valuePath,
                    "Plugin uses int accessor or unified enum for version compatibility",
                    vc.valueName() + " = " + vc.v1Number(),
                    null
                ));
            }

            case VALUE_NUMBER_CHANGED -> {
                // Enum value number changed - plugin uses int values
                changes.add(new BreakingChange(
                    BreakingChange.Type.ENUM_VALUE_NUMBER_CHANGED,
                    BreakingChange.Severity.WARNING,
                    valuePath,
                    "Wire format change - plugin uses int for safe cross-version access",
                    String.valueOf(vc.v1Number()),
                    String.valueOf(vc.v2Number())
                ));
            }

            case VALUE_ADDED -> {
                // Adding a value is informational only
            }

            default -> {
                // No action for other types
            }
        }
    }

    /**
     * Formats field info for display in change messages.
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
