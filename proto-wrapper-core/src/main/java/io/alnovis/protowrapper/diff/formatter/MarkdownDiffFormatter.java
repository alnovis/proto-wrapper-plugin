package io.alnovis.protowrapper.diff.formatter;

import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

/**
 * Formats schema diff results as Markdown.
 */
public class MarkdownDiffFormatter implements DiffFormatter {

    @Override
    public String format(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("# Schema Comparison: ").append(diff.getV1Name())
          .append(" -> ").append(diff.getV2Name()).append("\n\n");

        // Summary table
        sb.append("## Summary\n\n");
        formatSummaryTable(diff.getSummary(), sb);
        sb.append("\n");

        if (diff.hasBreakingChanges()) {
            sb.append("**Breaking Changes:** ")
              .append(diff.getSummary().errorCount()).append(" errors, ")
              .append(diff.getSummary().warningCount()).append(" warnings\n\n");
        }

        sb.append("---\n\n");

        // Messages
        sb.append("## Messages\n\n");
        formatMessages(diff, sb);

        sb.append("---\n\n");

        // Enums
        sb.append("## Enums\n\n");
        formatEnums(diff, sb);

        // Breaking changes section
        if (diff.hasBreakingChanges()) {
            sb.append("---\n\n");
            sb.append("## Breaking Changes\n\n");
            formatBreakingChangesTable(diff.getBreakingChanges(), sb);
        }

        // Suspected renumbered fields
        if (diff.hasSuspectedRenumbers()) {
            sb.append("---\n\n");
            sb.append("## Suspected Renumbered Fields\n\n");
            sb.append("The following fields appear to have been renumbered between versions. ");
            sb.append("Consider adding field mappings to the plugin configuration.\n\n");
            formatSuspectedRenumbersTable(diff, sb);
        }

        return sb.toString();
    }

    @Override
    public String formatBreakingOnly(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Breaking Changes: ").append(diff.getV1Name())
          .append(" -> ").append(diff.getV2Name()).append("\n\n");

        if (!diff.hasBreakingChanges()) {
            sb.append("No breaking changes detected.\n");
            return sb.toString();
        }

        sb.append("**Total:** ").append(diff.getSummary().errorCount())
          .append(" errors, ").append(diff.getSummary().warningCount())
          .append(" warnings\n\n");

        formatBreakingChangesTable(diff.getBreakingChanges(), sb);

        return sb.toString();
    }

    private void formatSummaryTable(SchemaDiff.DiffSummary summary, StringBuilder sb) {
        sb.append("| Category | Added | Modified | Removed |\n");
        sb.append("|----------|-------|----------|--------|\n");
        sb.append("| Messages | ").append(summary.addedMessages())
          .append(" | ").append(summary.modifiedMessages())
          .append(" | ").append(summary.removedMessages()).append(" |\n");
        sb.append("| Enums | ").append(summary.addedEnums())
          .append(" | ").append(summary.modifiedEnums())
          .append(" | ").append(summary.removedEnums()).append(" |\n");

        if (summary.hasRenumbers()) {
            sb.append("\n**Renumbered fields:** ")
              .append(summary.mappedRenumbers()).append(" mapped, ")
              .append(summary.suspectedRenumbers()).append(" suspected\n");
        }
    }

    private void formatMessages(SchemaDiff diff, StringBuilder sb) {
        List<MessageInfo> added = diff.getAddedMessages();
        List<MessageDiff> modified = diff.getModifiedMessages();
        List<MessageInfo> removed = diff.getRemovedMessages();

        // Added messages
        if (!added.isEmpty()) {
            sb.append("### Added Messages\n\n");
            for (MessageInfo msg : added) {
                sb.append("#### ").append(msg.getName()).append("\n");
                if (msg.getSourceFileName() != null) {
                    sb.append("*Source: ").append(msg.getSourceFileName()).append("*\n\n");
                }
                formatMessageFieldsTable(msg, sb);
                sb.append("\n");
            }
        }

        // Modified messages
        if (!modified.isEmpty()) {
            sb.append("### Modified Messages\n\n");
            for (MessageDiff md : modified) {
                sb.append("#### ").append(md.messageName()).append("\n\n");
                formatMessageDiffTable(md, sb);
                sb.append("\n");
            }
        }

        // Removed messages
        if (!removed.isEmpty()) {
            sb.append("### Removed Messages\n\n");
            for (MessageInfo msg : removed) {
                sb.append("- **").append(msg.getName()).append("** - **BREAKING**\n");
            }
            sb.append("\n");
        }

        if (added.isEmpty() && modified.isEmpty() && removed.isEmpty()) {
            sb.append("No message changes.\n\n");
        }
    }

    private void formatMessageFieldsTable(MessageInfo msg, StringBuilder sb) {
        List<FieldInfo> fields = msg.getFieldsSorted();
        if (fields.isEmpty()) {
            sb.append("*No fields*\n");
            return;
        }

        sb.append("| Field | Type | Number |\n");
        sb.append("|-------|------|--------|\n");
        for (FieldInfo field : fields) {
            sb.append("| ").append(field.getProtoName())
              .append(" | ").append(formatTypeForMarkdown(field))
              .append(" | ").append(field.getNumber()).append(" |\n");
        }
    }

    private void formatMessageDiffTable(MessageDiff md, StringBuilder sb) {
        List<FieldChange> allChanges = md.fieldChanges();
        if (allChanges.isEmpty() && md.nestedMessageChanges().isEmpty() && md.nestedEnumChanges().isEmpty()) {
            sb.append("*No field changes*\n");
            return;
        }

        if (!allChanges.isEmpty()) {
            sb.append("| Change | Field | Details |\n");
            sb.append("|--------|-------|--------|\n");

            // Added fields
            for (FieldChange fc : md.getAddedFields()) {
                sb.append("| + Added | ").append(fc.fieldName())
                  .append(" (#").append(fc.fieldNumber()).append(") | `")
                  .append(FieldChange.formatType(fc.v2Field())).append("` |\n");
            }

            // Modified fields (including renumbered)
            for (FieldChange fc : md.getModifiedFields()) {
                if (fc.changeType() == ChangeType.NUMBER_CHANGED && fc.isRenumberedByMapping()) {
                    sb.append("| ~ Renumbered | ").append(fc.fieldName())
                      .append(" | ").append(fc.getRenumberDescription())
                      .append(" `[MAPPED]` |\n");
                } else {
                    String details = String.join("; ", fc.changes());
                    if (fc.isBreaking()) {
                        details += " **BREAKING**";
                    } else if (fc.getCompatibilityNote() != null) {
                        details += " (" + fc.getCompatibilityNote() + ")";
                    }
                    sb.append("| ~ Changed | ").append(fc.fieldName())
                      .append(" (#").append(fc.fieldNumber()).append(") | ")
                      .append(details).append(" |\n");
                }
            }

            // Removed fields
            for (FieldChange fc : md.getRemovedFields()) {
                sb.append("| - Removed | ").append(fc.fieldName())
                  .append(" (#").append(fc.fieldNumber()).append(") | **BREAKING** |\n");
            }
        }

        // Nested changes
        if (!md.nestedMessageChanges().isEmpty()) {
            sb.append("\n**Nested message changes:**\n");
            for (MessageDiff nested : md.nestedMessageChanges()) {
                sb.append("- ").append(nested.changeType().name().toLowerCase())
                  .append(": ").append(nested.messageName());
                if (nested.hasBreakingChanges()) {
                    sb.append(" **BREAKING**");
                }
                sb.append("\n");
            }
        }

        if (!md.nestedEnumChanges().isEmpty()) {
            sb.append("\n**Nested enum changes:**\n");
            for (EnumDiff nested : md.nestedEnumChanges()) {
                sb.append("- ").append(nested.changeType().name().toLowerCase())
                  .append(": ").append(nested.enumName());
                if (nested.hasBreakingChanges()) {
                    sb.append(" **BREAKING**");
                }
                sb.append("\n");
            }
        }
    }

    private void formatEnums(SchemaDiff diff, StringBuilder sb) {
        List<EnumInfo> added = diff.getAddedEnums();
        List<EnumDiff> modified = diff.getModifiedEnums();
        List<EnumInfo> removed = diff.getRemovedEnums();

        // Added enums
        if (!added.isEmpty()) {
            sb.append("### Added Enums\n\n");
            for (EnumInfo e : added) {
                sb.append("#### ").append(e.getName()).append("\n\n");
                formatEnumValuesTable(e, sb);
                sb.append("\n");
            }
        }

        // Modified enums
        if (!modified.isEmpty()) {
            sb.append("### Modified Enums\n\n");
            for (EnumDiff ed : modified) {
                sb.append("#### ").append(ed.enumName()).append("\n\n");

                for (EnumValueChange vc : ed.getAddedValues()) {
                    sb.append("- + Added: `").append(vc.valueName())
                      .append("` (").append(vc.v2Number()).append(")\n");
                }
                for (EnumValueChange vc : ed.getRemovedValues()) {
                    sb.append("- - Removed: `").append(vc.valueName())
                      .append("` (").append(vc.v1Number()).append(") **BREAKING**\n");
                }
                for (EnumValueChange vc : ed.getChangedValues()) {
                    sb.append("- ~ Changed: `").append(vc.valueName())
                      .append("` (").append(vc.v1Number()).append(" -> ")
                      .append(vc.v2Number()).append(") **BREAKING**\n");
                }
                sb.append("\n");
            }
        }

        // Removed enums
        if (!removed.isEmpty()) {
            sb.append("### Removed Enums\n\n");
            for (EnumInfo e : removed) {
                sb.append("- **").append(e.getName()).append("** - **BREAKING**\n");
            }
            sb.append("\n");
        }

        if (added.isEmpty() && modified.isEmpty() && removed.isEmpty()) {
            sb.append("No enum changes.\n\n");
        }
    }

    private void formatEnumValuesTable(EnumInfo e, StringBuilder sb) {
        sb.append("| Name | Number |\n");
        sb.append("|------|--------|\n");
        for (EnumInfo.EnumValue v : e.getValues()) {
            sb.append("| ").append(v.name())
              .append(" | ").append(v.number()).append(" |\n");
        }
    }

    private void formatBreakingChangesTable(List<BreakingChange> changes, StringBuilder sb) {
        sb.append("| Severity | Type | Entity | Description |\n");
        sb.append("|----------|------|--------|-------------|\n");

        for (BreakingChange bc : changes) {
            sb.append("| ").append(bc.severity().name())
              .append(" | ").append(formatBreakingType(bc.type()))
              .append(" | ").append(bc.entityPath())
              .append(" | ").append(bc.description());

            if (bc.v1Value() != null && bc.v2Value() != null) {
                sb.append(" (`").append(bc.v1Value()).append("` -> `")
                  .append(bc.v2Value()).append("`)");
            } else if (bc.v1Value() != null) {
                sb.append(" (was: `").append(bc.v1Value()).append("`)");
            }
            sb.append(" |\n");
        }
    }

    private void formatSuspectedRenumbersTable(SchemaDiff diff, StringBuilder sb) {
        sb.append("| Confidence | Message.Field | ").append(diff.getV1Name())
          .append(" | ").append(diff.getV2Name()).append(" | Type | Suggested Mapping |\n");
        sb.append("|------------|---------------|------|------|------|-------------------|\n");

        for (SuspectedRenumber sr : diff.getSuspectedRenumbers()) {
            sb.append("| ").append(sr.confidence().name())
              .append(" | ").append(sr.messageName()).append(".").append(sr.fieldName())
              .append(" | #").append(sr.v1Number())
              .append(" | #").append(sr.v2Number())
              .append(" | ");
            if (sr.v1Field() != null) {
                sb.append("`").append(FieldChange.formatType(sr.v1Field())).append("`");
            }
            sb.append(" | `<fieldMapping><message>").append(sr.messageName())
              .append("</message><fieldName>").append(sr.fieldName())
              .append("</fieldName></fieldMapping>` |\n");
        }
        sb.append("\n");
    }

    private String formatTypeForMarkdown(FieldInfo field) {
        String type = FieldChange.formatType(field);
        if (field.isRepeated()) {
            return "`repeated " + type + "`";
        }
        return "`" + type + "`";
    }

    private String formatBreakingType(BreakingChange.Type type) {
        // Convert ENUM_VALUE_REMOVED to "Enum Value Removed"
        String name = type.name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toLowerCase().toCharArray()) {
            if (c == ' ') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
