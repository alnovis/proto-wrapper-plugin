package io.alnovis.protowrapper.diff.formatter;

import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

/**
 * Formats schema diff results as plain text.
 */
public class TextDiffFormatter implements DiffFormatter {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String THIN_SEPARATOR = "-".repeat(40);

    @Override
    public String format(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Schema Comparison: ").append(diff.getV1Name())
          .append(" -> ").append(diff.getV2Name()).append("\n\n");

        // Messages section
        sb.append(SEPARATOR).append("\n");
        sb.append("MESSAGES\n");
        sb.append(SEPARATOR).append("\n\n");

        formatMessages(diff, sb);

        // Enums section
        sb.append(SEPARATOR).append("\n");
        sb.append("ENUMS\n");
        sb.append(SEPARATOR).append("\n\n");

        formatEnums(diff, sb);

        // Breaking changes section - show if there are any changes (errors, warnings, or info)
        if (!diff.getBreakingChanges().isEmpty()) {
            sb.append(SEPARATOR).append("\n");
            sb.append("BREAKING CHANGES\n");
            sb.append(SEPARATOR).append("\n\n");

            formatBreakingChanges(diff.getBreakingChanges(), sb);
        }

        // Summary
        sb.append(SEPARATOR).append("\n");
        sb.append("SUMMARY\n");
        sb.append(SEPARATOR).append("\n\n");

        formatSummary(diff.getSummary(), sb);

        return sb.toString();
    }

    @Override
    public String formatBreakingOnly(SchemaDiff diff) {
        if (!diff.hasBreakingChanges()) {
            return "No breaking changes detected.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Breaking Changes: ").append(diff.getV1Name())
          .append(" -> ").append(diff.getV2Name()).append("\n\n");

        formatBreakingChanges(diff.getBreakingChanges(), sb);

        sb.append("\nTotal: ").append(diff.getSummary().errorCount()).append(" errors, ")
          .append(diff.getSummary().warningCount()).append(" warnings\n");

        return sb.toString();
    }

    private void formatMessages(SchemaDiff diff, StringBuilder sb) {
        // Added messages
        List<MessageInfo> added = diff.getAddedMessages();
        if (!added.isEmpty()) {
            for (MessageInfo msg : added) {
                sb.append("+ ADDED: ").append(msg.getName());
                if (msg.getSourceFileName() != null) {
                    sb.append(" (").append(msg.getSourceFileName()).append(")");
                }
                sb.append("\n");
                formatMessageFields(msg, sb, "    ");
                sb.append("\n");
            }
        }

        // Modified messages
        List<MessageDiff> modified = diff.getModifiedMessages();
        if (!modified.isEmpty()) {
            for (MessageDiff md : modified) {
                sb.append("~ MODIFIED: ").append(md.messageName()).append("\n");
                formatMessageDiff(md, sb, "    ");
                sb.append("\n");
            }
        }

        // Removed messages
        List<MessageInfo> removed = diff.getRemovedMessages();
        if (!removed.isEmpty()) {
            for (MessageInfo msg : removed) {
                sb.append("- REMOVED: ").append(msg.getName());
                sb.append(" [BREAKING]\n");
            }
            sb.append("\n");
        }

        if (added.isEmpty() && modified.isEmpty() && removed.isEmpty()) {
            sb.append("No message changes.\n\n");
        }
    }

    private void formatMessageFields(MessageInfo msg, StringBuilder sb, String indent) {
        sb.append(indent).append("Fields:\n");
        for (FieldInfo field : msg.getFieldsSorted()) {
            sb.append(indent).append("  - ").append(field.getProtoName())
              .append(": ").append(FieldChange.formatType(field))
              .append(" (#").append(field.getNumber()).append(")");
            if (field.isRepeated()) {
                sb.append(" [repeated]");
            }
            sb.append("\n");
        }
    }

    private void formatMessageDiff(MessageDiff md, StringBuilder sb, String indent) {
        // Added fields
        for (FieldChange fc : md.getAddedFields()) {
            sb.append(indent).append("+ Added field: ").append(fc.fieldName())
              .append(" (").append(FieldChange.formatType(fc.v2Field()))
              .append(", #").append(fc.fieldNumber()).append(")\n");
        }

        // Modified fields
        for (FieldChange fc : md.getModifiedFields()) {
            sb.append(indent).append("~ Changed field: ").append(fc.fieldName()).append("\n");
            for (String change : fc.changes()) {
                sb.append(indent).append("    ").append(change);
                if (fc.isBreaking()) {
                    sb.append(" [BREAKING]");
                }
                sb.append("\n");
            }
        }

        // Removed fields
        for (FieldChange fc : md.getRemovedFields()) {
            sb.append(indent).append("- Removed field: ").append(fc.fieldName())
              .append(" (#").append(fc.fieldNumber()).append(") [BREAKING]\n");
        }

        // Nested message changes
        for (MessageDiff nested : md.nestedMessageChanges()) {
            sb.append(indent).append("Nested ").append(nested.changeType().name().toLowerCase())
              .append(": ").append(nested.messageName()).append("\n");
            if (nested.changeType() == ChangeType.MODIFIED) {
                formatMessageDiff(nested, sb, indent + "  ");
            }
        }

        // Nested enum changes
        for (EnumDiff nested : md.nestedEnumChanges()) {
            sb.append(indent).append("Nested enum ").append(nested.changeType().name().toLowerCase())
              .append(": ").append(nested.enumName()).append("\n");
        }
    }

    private void formatEnums(SchemaDiff diff, StringBuilder sb) {
        // Added enums
        List<EnumInfo> added = diff.getAddedEnums();
        if (!added.isEmpty()) {
            for (EnumInfo e : added) {
                sb.append("+ ADDED: ").append(e.getName()).append("\n");
                sb.append("    Values: ");
                sb.append(formatEnumValues(e));
                sb.append("\n\n");
            }
        }

        // Modified enums
        List<EnumDiff> modified = diff.getModifiedEnums();
        if (!modified.isEmpty()) {
            for (EnumDiff ed : modified) {
                sb.append("~ MODIFIED: ").append(ed.enumName()).append("\n");
                for (EnumValueChange vc : ed.getAddedValues()) {
                    sb.append("    + Added value: ").append(vc.valueName())
                      .append("(").append(vc.v2Number()).append(")\n");
                }
                for (EnumValueChange vc : ed.getRemovedValues()) {
                    sb.append("    - Removed value: ").append(vc.valueName())
                      .append("(").append(vc.v1Number()).append(") [BREAKING]\n");
                }
                for (EnumValueChange vc : ed.getChangedValues()) {
                    sb.append("    ~ Number changed: ").append(vc.valueName())
                      .append(" (").append(vc.v1Number()).append(" -> ")
                      .append(vc.v2Number()).append(") [BREAKING]\n");
                }
                sb.append("\n");
            }
        }

        // Removed enums
        List<EnumInfo> removed = diff.getRemovedEnums();
        if (!removed.isEmpty()) {
            for (EnumInfo e : removed) {
                sb.append("- REMOVED: ").append(e.getName()).append(" [BREAKING]\n");
            }
            sb.append("\n");
        }

        if (added.isEmpty() && modified.isEmpty() && removed.isEmpty()) {
            sb.append("No enum changes.\n\n");
        }
    }

    private String formatEnumValues(EnumInfo e) {
        StringBuilder sb = new StringBuilder();
        List<EnumInfo.EnumValue> values = e.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            EnumInfo.EnumValue v = values.get(i);
            sb.append(v.name()).append("(").append(v.number()).append(")");
        }
        return sb.toString();
    }

    private void formatBreakingChanges(List<BreakingChange> changes, StringBuilder sb) {
        // Group by severity
        List<BreakingChange> errors = changes.stream()
            .filter(BreakingChange::isError).toList();
        List<BreakingChange> warnings = changes.stream()
            .filter(BreakingChange::isWarning).toList();
        List<BreakingChange> infos = changes.stream()
            .filter(bc -> bc.severity() == BreakingChange.Severity.INFO).toList();

        if (!errors.isEmpty()) {
            sb.append("ERRORS (").append(errors.size()).append(") - Incompatible changes:\n");
            for (BreakingChange bc : errors) {
                sb.append("  [ERROR] ").append(bc.type()).append(": ")
                  .append(bc.entityPath());
                if (bc.v1Value() != null && bc.v2Value() != null) {
                    sb.append(" (").append(bc.v1Value()).append(" -> ").append(bc.v2Value()).append(")");
                } else if (bc.v1Value() != null) {
                    sb.append(" (was: ").append(bc.v1Value()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!warnings.isEmpty()) {
            sb.append("WARNINGS (").append(warnings.size()).append(") - May require attention:\n");
            for (BreakingChange bc : warnings) {
                sb.append("  [WARN] ").append(bc.type()).append(": ")
                  .append(bc.entityPath());
                if (bc.description() != null) {
                    sb.append(" - ").append(bc.description());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!infos.isEmpty()) {
            sb.append("PLUGIN-HANDLED (").append(infos.size()).append(") - Automatically converted by proto-wrapper:\n");
            for (BreakingChange bc : infos) {
                sb.append("  [INFO] ").append(bc.type()).append(": ")
                  .append(bc.entityPath());
                if (bc.v1Value() != null && bc.v2Value() != null) {
                    sb.append(" (").append(bc.v1Value()).append(" -> ").append(bc.v2Value()).append(")");
                }
                if (bc.description() != null) {
                    sb.append("\n         ").append(bc.description());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
    }

    private void formatSummary(SchemaDiff.DiffSummary summary, StringBuilder sb) {
        sb.append("Messages:  +").append(summary.addedMessages())
          .append(" added, ~").append(summary.modifiedMessages())
          .append(" modified, -").append(summary.removedMessages())
          .append(" removed\n");

        sb.append("Enums:     +").append(summary.addedEnums())
          .append(" added, ~").append(summary.modifiedEnums())
          .append(" modified, -").append(summary.removedEnums())
          .append(" removed\n");

        sb.append("Changes:   ").append(summary.errorCount())
          .append(" errors, ").append(summary.warningCount())
          .append(" warnings, ").append(summary.infoCount())
          .append(" plugin-handled\n");

        if (summary.infoCount() > 0) {
            sb.append("\nNote: Plugin-handled changes are type conversions that proto-wrapper\n");
            sb.append("      automatically handles via unified accessor methods.\n");
        }
    }
}
