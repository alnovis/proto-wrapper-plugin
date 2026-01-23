package io.alnovis.protowrapper.diff.formatter;

import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

/**
 * Formats schema diff results as JSON.
 * Uses manual JSON construction to avoid external dependencies.
 */
public class JsonDiffFormatter implements DiffFormatter {

    private static final String INDENT = "  ";

    @Override
    public String format(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Version info
        appendString(sb, 1, "v1", diff.getV1Name());
        sb.append(",\n");
        appendString(sb, 1, "v2", diff.getV2Name());
        sb.append(",\n");

        // Summary
        appendKey(sb, 1, "summary");
        sb.append("{\n");
        formatSummary(diff.getSummary(), sb, 2);
        sb.append(indent(1)).append("},\n");

        // Messages
        appendKey(sb, 1, "messages");
        sb.append("{\n");
        formatMessages(diff, sb, 2);
        sb.append(indent(1)).append("},\n");

        // Enums
        appendKey(sb, 1, "enums");
        sb.append("{\n");
        formatEnums(diff, sb, 2);
        sb.append(indent(1)).append("},\n");

        // Breaking changes
        appendKey(sb, 1, "breakingChanges");
        sb.append("[\n");
        formatBreakingChanges(diff.getBreakingChanges(), sb, 2);
        sb.append(indent(1)).append("]");

        // Suspected renumbers
        if (diff.hasSuspectedRenumbers()) {
            sb.append(",\n");
            appendKey(sb, 1, "suspectedRenumbers");
            sb.append("[\n");
            formatSuspectedRenumbers(diff, sb, 2);
            sb.append(indent(1)).append("]");
        }
        sb.append("\n");

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String formatBreakingOnly(SchemaDiff diff) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        appendString(sb, 1, "v1", diff.getV1Name());
        sb.append(",\n");
        appendString(sb, 1, "v2", diff.getV2Name());
        sb.append(",\n");

        appendKey(sb, 1, "breakingChanges");
        sb.append("[\n");
        formatBreakingChanges(diff.getBreakingChanges(), sb, 2);
        sb.append(indent(1)).append("],\n");

        appendNumber(sb, 1, "errorCount", diff.getSummary().errorCount());
        sb.append(",\n");
        appendNumber(sb, 1, "warningCount", diff.getSummary().warningCount());
        sb.append("\n");

        sb.append("}\n");
        return sb.toString();
    }

    private void formatSummary(SchemaDiff.DiffSummary summary, StringBuilder sb, int depth) {
        appendNumber(sb, depth, "addedMessages", summary.addedMessages());
        sb.append(",\n");
        appendNumber(sb, depth, "removedMessages", summary.removedMessages());
        sb.append(",\n");
        appendNumber(sb, depth, "modifiedMessages", summary.modifiedMessages());
        sb.append(",\n");
        appendNumber(sb, depth, "addedEnums", summary.addedEnums());
        sb.append(",\n");
        appendNumber(sb, depth, "removedEnums", summary.removedEnums());
        sb.append(",\n");
        appendNumber(sb, depth, "modifiedEnums", summary.modifiedEnums());
        sb.append(",\n");
        appendNumber(sb, depth, "errorCount", summary.errorCount());
        sb.append(",\n");
        appendNumber(sb, depth, "warningCount", summary.warningCount());
        sb.append(",\n");
        appendNumber(sb, depth, "mappedRenumbers", summary.mappedRenumbers());
        sb.append(",\n");
        appendNumber(sb, depth, "suspectedRenumbers", summary.suspectedRenumbers());
        sb.append("\n");
    }

    private void formatMessages(SchemaDiff diff, StringBuilder sb, int depth) {
        // Added
        appendKey(sb, depth, "added");
        sb.append("[\n");
        List<MessageInfo> added = diff.getAddedMessages();
        for (int i = 0; i < added.size(); i++) {
            formatAddedMessage(added.get(i), sb, depth + 1);
            if (i < added.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("],\n");

        // Removed
        appendKey(sb, depth, "removed");
        sb.append("[\n");
        List<MessageInfo> removed = diff.getRemovedMessages();
        for (int i = 0; i < removed.size(); i++) {
            formatRemovedMessage(removed.get(i), sb, depth + 1);
            if (i < removed.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("],\n");

        // Modified
        appendKey(sb, depth, "modified");
        sb.append("[\n");
        List<MessageDiff> modified = diff.getModifiedMessages();
        for (int i = 0; i < modified.size(); i++) {
            formatModifiedMessage(modified.get(i), sb, depth + 1);
            if (i < modified.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("]\n");
    }

    private void formatAddedMessage(MessageInfo msg, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", msg.getName());
        sb.append(",\n");
        if (msg.getSourceFileName() != null) {
            appendString(sb, depth + 1, "sourceFile", msg.getSourceFileName());
            sb.append(",\n");
        }
        appendKey(sb, depth + 1, "fields");
        sb.append("[\n");
        List<FieldInfo> fields = msg.getFieldsSorted();
        for (int i = 0; i < fields.size(); i++) {
            formatField(fields.get(i), sb, depth + 2);
            if (i < fields.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth + 1)).append("]\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatRemovedMessage(MessageInfo msg, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", msg.getName());
        if (msg.getSourceFileName() != null) {
            sb.append(",\n");
            appendString(sb, depth + 1, "sourceFile", msg.getSourceFileName());
        }
        sb.append("\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatModifiedMessage(MessageDiff md, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", md.messageName());
        sb.append(",\n");
        appendKey(sb, depth + 1, "fieldChanges");
        sb.append("[\n");
        List<FieldChange> changes = md.fieldChanges();
        for (int i = 0; i < changes.size(); i++) {
            formatFieldChange(changes.get(i), sb, depth + 2);
            if (i < changes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth + 1)).append("]\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatField(FieldInfo field, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", field.getProtoName());
        sb.append(",\n");
        appendString(sb, depth + 1, "type", FieldChange.formatType(field));
        sb.append(",\n");
        appendNumber(sb, depth + 1, "number", field.getNumber());
        if (field.isRepeated()) {
            sb.append(",\n");
            appendBoolean(sb, depth + 1, "repeated", true);
        }
        sb.append("\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatFieldChange(FieldChange fc, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendNumber(sb, depth + 1, "fieldNumber", fc.fieldNumber());
        sb.append(",\n");
        appendString(sb, depth + 1, "fieldName", fc.fieldName());
        sb.append(",\n");
        appendString(sb, depth + 1, "changeType", fc.changeType().name());
        if (fc.v1Field() != null) {
            sb.append(",\n");
            appendString(sb, depth + 1, "v1Type", FieldChange.formatType(fc.v1Field()));
            if (fc.changeType() == ChangeType.NUMBER_CHANGED) {
                sb.append(",\n");
                appendNumber(sb, depth + 1, "v1Number", fc.v1Field().getNumber());
            }
        }
        if (fc.v2Field() != null) {
            sb.append(",\n");
            appendString(sb, depth + 1, "v2Type", FieldChange.formatType(fc.v2Field()));
            if (fc.changeType() == ChangeType.NUMBER_CHANGED) {
                sb.append(",\n");
                appendNumber(sb, depth + 1, "v2Number", fc.v2Field().getNumber());
            }
        }
        sb.append(",\n");
        appendBoolean(sb, depth + 1, "breaking", fc.isBreaking());
        if (fc.isRenumberedByMapping()) {
            sb.append(",\n");
            appendBoolean(sb, depth + 1, "mapped", true);
        }
        if (fc.getCompatibilityNote() != null) {
            sb.append(",\n");
            appendString(sb, depth + 1, "compatibilityNote", fc.getCompatibilityNote());
        }
        sb.append("\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatEnums(SchemaDiff diff, StringBuilder sb, int depth) {
        // Added
        appendKey(sb, depth, "added");
        sb.append("[\n");
        List<EnumInfo> added = diff.getAddedEnums();
        for (int i = 0; i < added.size(); i++) {
            formatEnum(added.get(i), sb, depth + 1);
            if (i < added.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("],\n");

        // Removed
        appendKey(sb, depth, "removed");
        sb.append("[\n");
        List<EnumInfo> removed = diff.getRemovedEnums();
        for (int i = 0; i < removed.size(); i++) {
            sb.append(indent(depth + 1)).append("{\"name\": \"")
              .append(escapeJson(removed.get(i).getName())).append("\"}");
            if (i < removed.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("],\n");

        // Modified
        appendKey(sb, depth, "modified");
        sb.append("[\n");
        List<EnumDiff> modified = diff.getModifiedEnums();
        for (int i = 0; i < modified.size(); i++) {
            formatEnumDiff(modified.get(i), sb, depth + 1);
            if (i < modified.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth)).append("]\n");
    }

    private void formatEnum(EnumInfo e, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", e.getName());
        sb.append(",\n");
        appendKey(sb, depth + 1, "values");
        sb.append("[\n");
        List<EnumInfo.EnumValue> values = e.getValues();
        for (int i = 0; i < values.size(); i++) {
            EnumInfo.EnumValue v = values.get(i);
            sb.append(indent(depth + 2)).append("{\"name\": \"")
              .append(escapeJson(v.name())).append("\", \"number\": ")
              .append(v.number()).append("}");
            if (i < values.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent(depth + 1)).append("]\n");
        sb.append(indent(depth)).append("}");
    }

    private void formatEnumDiff(EnumDiff ed, StringBuilder sb, int depth) {
        sb.append(indent(depth)).append("{\n");
        appendString(sb, depth + 1, "name", ed.enumName());
        sb.append(",\n");

        // Added values
        appendKey(sb, depth + 1, "addedValues");
        sb.append("[");
        List<EnumValueChange> addedValues = ed.getAddedValues();
        for (int i = 0; i < addedValues.size(); i++) {
            EnumValueChange vc = addedValues.get(i);
            sb.append("{\"name\": \"").append(escapeJson(vc.valueName()))
              .append("\", \"number\": ").append(vc.v2Number()).append("}");
            if (i < addedValues.size() - 1) sb.append(", ");
        }
        sb.append("],\n");

        // Removed values
        appendKey(sb, depth + 1, "removedValues");
        sb.append("[");
        List<EnumValueChange> removedValues = ed.getRemovedValues();
        for (int i = 0; i < removedValues.size(); i++) {
            EnumValueChange vc = removedValues.get(i);
            sb.append("{\"name\": \"").append(escapeJson(vc.valueName()))
              .append("\", \"number\": ").append(vc.v1Number()).append("}");
            if (i < removedValues.size() - 1) sb.append(", ");
        }
        sb.append("]\n");

        sb.append(indent(depth)).append("}");
    }

    private void formatBreakingChanges(List<BreakingChange> changes, StringBuilder sb, int depth) {
        for (int i = 0; i < changes.size(); i++) {
            BreakingChange bc = changes.get(i);
            sb.append(indent(depth)).append("{\n");
            appendString(sb, depth + 1, "type", bc.type().name());
            sb.append(",\n");
            appendString(sb, depth + 1, "severity", bc.severity().name());
            sb.append(",\n");
            appendString(sb, depth + 1, "entityPath", bc.entityPath());
            sb.append(",\n");
            appendString(sb, depth + 1, "description", bc.description());
            if (bc.v1Value() != null) {
                sb.append(",\n");
                appendString(sb, depth + 1, "v1Value", bc.v1Value());
            }
            if (bc.v2Value() != null) {
                sb.append(",\n");
                appendString(sb, depth + 1, "v2Value", bc.v2Value());
            }
            sb.append("\n");
            sb.append(indent(depth)).append("}");
            if (i < changes.size() - 1) sb.append(",");
            sb.append("\n");
        }
    }

    private void formatSuspectedRenumbers(SchemaDiff diff, StringBuilder sb, int depth) {
        List<SuspectedRenumber> suspected = diff.getSuspectedRenumbers();
        for (int i = 0; i < suspected.size(); i++) {
            SuspectedRenumber sr = suspected.get(i);
            sb.append(indent(depth)).append("{\n");
            appendString(sb, depth + 1, "messageName", sr.messageName());
            sb.append(",\n");
            appendString(sb, depth + 1, "fieldName", sr.fieldName());
            sb.append(",\n");
            appendNumber(sb, depth + 1, "v1Number", sr.v1Number());
            sb.append(",\n");
            appendNumber(sb, depth + 1, "v2Number", sr.v2Number());
            sb.append(",\n");
            appendString(sb, depth + 1, "confidence", sr.confidence().name());
            if (sr.v1Field() != null) {
                sb.append(",\n");
                appendString(sb, depth + 1, "type", FieldChange.formatType(sr.v1Field()));
            }
            sb.append(",\n");
            appendString(sb, depth + 1, "suggestedMapping",
                String.format("<fieldMapping><message>%s</message><fieldName>%s</fieldName></fieldMapping>",
                    sr.messageName(), sr.fieldName()));
            sb.append("\n");
            sb.append(indent(depth)).append("}");
            if (i < suspected.size() - 1) sb.append(",");
            sb.append("\n");
        }
    }

    // Helper methods

    private String indent(int depth) {
        return INDENT.repeat(depth);
    }

    private void appendKey(StringBuilder sb, int depth, String key) {
        sb.append(indent(depth)).append("\"").append(key).append("\": ");
    }

    private void appendString(StringBuilder sb, int depth, String key, String value) {
        sb.append(indent(depth)).append("\"").append(key).append("\": \"")
          .append(escapeJson(value)).append("\"");
    }

    private void appendNumber(StringBuilder sb, int depth, String key, int value) {
        sb.append(indent(depth)).append("\"").append(key).append("\": ").append(value);
    }

    private void appendBoolean(StringBuilder sb, int depth, String key, boolean value) {
        sb.append(indent(depth)).append("\"").append(key).append("\": ").append(value);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
