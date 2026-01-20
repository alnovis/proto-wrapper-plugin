package io.alnovis.protowrapper.diff;

import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.*;

import java.util.*;

/**
 * Adapter that converts MergedSchema to diff model structures (MessageDiff, FieldChange, etc.).
 *
 * <p>This adapter bridges the gap between the main plugin's merge infrastructure
 * (MergedSchema, MergedField, MergedEnum) and the diff tool's reporting structures.</p>
 *
 * <p>The adapter extracts differences between two specific versions from a MergedSchema,
 * using the conflict type information already computed by VersionMerger.</p>
 */
public class MergedSchemaDiffAdapter {

    private final String v1Name;
    private final String v2Name;

    /**
     * Creates a new adapter for the specified version names.
     *
     * @param v1Name Name of the source version
     * @param v2Name Name of the target version
     */
    public MergedSchemaDiffAdapter(String v1Name, String v2Name) {
        this.v1Name = v1Name;
        this.v2Name = v2Name;
    }

    /**
     * Converts a MergedSchema to a SchemaDiff.
     *
     * @param merged The merged schema containing information about both versions
     * @return SchemaDiff with all differences between v1 and v2
     */
    public SchemaDiff adapt(MergedSchema merged) {
        List<MessageDiff> messageDiffs = adaptMessages(merged);
        List<EnumDiff> enumDiffs = adaptEnums(merged);
        List<BreakingChange> breakingChanges = collectBreakingChanges(messageDiffs, enumDiffs);

        return new SchemaDiff(v1Name, v2Name, messageDiffs, enumDiffs, breakingChanges);
    }

    /**
     * Static convenience method.
     *
     * @param merged the merged schema
     * @param v1Name the source version name
     * @param v2Name the target version name
     * @return the schema diff
     */
    public static SchemaDiff toSchemaDiff(MergedSchema merged, String v1Name, String v2Name) {
        return new MergedSchemaDiffAdapter(v1Name, v2Name).adapt(merged);
    }

    // ========== Message Adaptation ==========

    private List<MessageDiff> adaptMessages(MergedSchema merged) {
        return merged.getMessages().stream()
            .map(this::adaptMessage)
            .filter(md -> md.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(MessageDiff::messageName))
            .toList();
    }

    private MessageDiff adaptMessage(MergedMessage message) {
        Set<String> versions = message.getPresentInVersions();
        boolean inV1 = versions.contains(v1Name);
        boolean inV2 = versions.contains(v2Name);

        if (!inV1 && inV2) {
            // Added in v2
            return createAddedMessageDiff(message);
        }
        if (inV1 && !inV2) {
            // Removed in v2
            return createRemovedMessageDiff(message);
        }
        if (inV1 && inV2) {
            // Present in both - check for modifications
            return createModifiedMessageDiff(message);
        }

        // Not in either version (shouldn't happen)
        return new MessageDiff(message.getName(), ChangeType.UNCHANGED,
            null, null, List.of(), List.of(), List.of());
    }

    private MessageDiff createAddedMessageDiff(MergedMessage message) {
        // Create a synthetic MessageInfo for the added message
        MessageInfo messageInfo = createMessageInfoFromMerged(message, v2Name);
        return MessageDiff.added(messageInfo);
    }

    private MessageDiff createRemovedMessageDiff(MergedMessage message) {
        MessageInfo messageInfo = createMessageInfoFromMerged(message, v1Name);
        return MessageDiff.removed(messageInfo);
    }

    private MessageDiff createModifiedMessageDiff(MergedMessage message) {
        List<FieldChange> fieldChanges = adaptFields(message);
        List<MessageDiff> nestedMessageChanges = adaptNestedMessages(message);
        List<EnumDiff> nestedEnumChanges = adaptNestedEnums(message);

        MessageInfo v1Info = createMessageInfoFromMerged(message, v1Name);
        MessageInfo v2Info = createMessageInfoFromMerged(message, v2Name);

        return MessageDiff.compared(v1Info, v2Info, fieldChanges, nestedMessageChanges, nestedEnumChanges);
    }

    // ========== Field Adaptation ==========

    private List<FieldChange> adaptFields(MergedMessage message) {
        return message.getFields().stream()
            .map(this::adaptField)
            .filter(fc -> fc.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(FieldChange::fieldNumber))
            .toList();
    }

    private FieldChange adaptField(MergedField field) {
        Set<String> versions = field.getPresentInVersions();
        boolean inV1 = versions.contains(v1Name);
        boolean inV2 = versions.contains(v2Name);

        if (!inV1 && inV2) {
            // Added in v2
            FieldInfo v2Field = field.getVersionFields().get(v2Name);
            return new FieldChange(field.getNumber(), field.getJavaName(), ChangeType.ADDED,
                null, v2Field, List.of("Added field"));
        }
        if (inV1 && !inV2) {
            // Removed in v2
            FieldInfo v1Field = field.getVersionFields().get(v1Name);
            return new FieldChange(field.getNumber(), field.getJavaName(), ChangeType.REMOVED,
                v1Field, null, List.of("Removed field"));
        }
        if (inV1 && inV2) {
            // Present in both - check for modifications
            return createModifiedFieldChange(field);
        }

        // Not in either version (shouldn't happen)
        return new FieldChange(field.getNumber(), field.getJavaName(), ChangeType.UNCHANGED,
            null, null, List.of());
    }

    private FieldChange createModifiedFieldChange(MergedField field) {
        FieldInfo v1Field = field.getVersionFields().get(v1Name);
        FieldInfo v2Field = field.getVersionFields().get(v2Name);

        List<String> changes = new ArrayList<>();
        ChangeType primaryChangeType = ChangeType.UNCHANGED;

        // Check for type conflict using the already-computed conflict type
        MergedField.ConflictType conflictType = field.getConflictType();
        if (conflictType != MergedField.ConflictType.NONE) {
            String v1Type = v1Field != null ? formatFieldType(v1Field) : "unknown";
            String v2Type = v2Field != null ? formatFieldType(v2Field) : "unknown";
            changes.add(String.format("Type: %s -> %s (%s)", v1Type, v2Type, conflictType));
            primaryChangeType = ChangeType.TYPE_CHANGED;
        }

        // Check for label change (repeated/singular)
        if (v1Field != null && v2Field != null && v1Field.isRepeated() != v2Field.isRepeated()) {
            changes.add(String.format("Cardinality: %s -> %s",
                v1Field.isRepeated() ? "repeated" : "singular",
                v2Field.isRepeated() ? "repeated" : "singular"));
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.LABEL_CHANGED;
            }
        }

        // Check for name change
        if (v1Field != null && v2Field != null &&
                !v1Field.getProtoName().equals(v2Field.getProtoName())) {
            changes.add(String.format("Name: %s -> %s",
                v1Field.getProtoName(), v2Field.getProtoName()));
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.NAME_CHANGED;
            }
        }

        // Check for oneof membership change
        if (v1Field != null && v2Field != null && v1Field.isInOneof() != v2Field.isInOneof()) {
            if (v1Field.isInOneof()) {
                changes.add("Moved out of oneof: " + v1Field.getOneofName());
            } else {
                changes.add("Moved into oneof: " + v2Field.getOneofName());
            }
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.MODIFIED;
            }
        }

        return new FieldChange(field.getNumber(), field.getJavaName(), primaryChangeType,
            v1Field, v2Field, changes);
    }

    // ========== Nested Message Adaptation ==========

    private List<MessageDiff> adaptNestedMessages(MergedMessage parent) {
        return parent.getNestedMessages().stream()
            .map(this::adaptMessage)
            .filter(md -> md.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(MessageDiff::messageName))
            .toList();
    }

    // ========== Enum Adaptation ==========

    private List<EnumDiff> adaptEnums(MergedSchema merged) {
        return merged.getEnums().stream()
            .map(this::adaptEnum)
            .filter(ed -> ed.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(EnumDiff::enumName))
            .toList();
    }

    private List<EnumDiff> adaptNestedEnums(MergedMessage parent) {
        return parent.getNestedEnums().stream()
            .map(this::adaptEnum)
            .filter(ed -> ed.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(EnumDiff::enumName))
            .toList();
    }

    private EnumDiff adaptEnum(MergedEnum mergedEnum) {
        Set<String> versions = mergedEnum.getPresentInVersions();
        boolean inV1 = versions.contains(v1Name);
        boolean inV2 = versions.contains(v2Name);

        if (!inV1 && inV2) {
            EnumInfo enumInfo = createEnumInfoFromMerged(mergedEnum, v2Name);
            return EnumDiff.added(enumInfo);
        }
        if (inV1 && !inV2) {
            EnumInfo enumInfo = createEnumInfoFromMerged(mergedEnum, v1Name);
            return EnumDiff.removed(enumInfo);
        }
        if (inV1 && inV2) {
            return createModifiedEnumDiff(mergedEnum);
        }

        return new EnumDiff(mergedEnum.getName(), ChangeType.UNCHANGED, null, null, List.of());
    }

    private EnumDiff createModifiedEnumDiff(MergedEnum mergedEnum) {
        List<EnumValueChange> valueChanges = adaptEnumValues(mergedEnum);

        EnumInfo v1Info = createEnumInfoFromMerged(mergedEnum, v1Name);
        EnumInfo v2Info = createEnumInfoFromMerged(mergedEnum, v2Name);

        return EnumDiff.modified(v1Info, v2Info, valueChanges);
    }

    private List<EnumValueChange> adaptEnumValues(MergedEnum mergedEnum) {
        List<EnumValueChange> changes = new ArrayList<>();

        for (MergedEnumValue value : mergedEnum.getValues()) {
            Set<String> versions = value.getPresentInVersions();
            boolean inV1 = versions.contains(v1Name);
            boolean inV2 = versions.contains(v2Name);

            if (!inV1 && inV2) {
                changes.add(EnumValueChange.added(value.getName(), value.getNumber()));
            } else if (inV1 && !inV2) {
                changes.add(EnumValueChange.removed(value.getName(), value.getNumber()));
            }
            // Note: Number changes within the same value name would require
            // tracking value numbers per version, which MergedEnumValue doesn't currently do
        }

        return changes.stream()
            .sorted(Comparator.comparing(EnumValueChange::valueName))
            .toList();
    }

    // ========== Breaking Change Collection ==========

    private List<BreakingChange> collectBreakingChanges(List<MessageDiff> messageDiffs,
                                                         List<EnumDiff> enumDiffs) {
        List<BreakingChange> changes = new ArrayList<>();

        // Collect from messages
        for (MessageDiff msgDiff : messageDiffs) {
            collectMessageBreakingChanges(msgDiff, "", changes);
        }

        // Collect from top-level enums
        for (EnumDiff enumDiff : enumDiffs) {
            collectEnumBreakingChanges(enumDiff, "", changes);
        }

        return changes;
    }

    private void collectMessageBreakingChanges(MessageDiff msgDiff, String parentPath,
                                                List<BreakingChange> changes) {
        String msgPath = parentPath.isEmpty() ? msgDiff.messageName() : parentPath + "." + msgDiff.messageName();

        // Message removed
        if (msgDiff.changeType() == ChangeType.REMOVED) {
            changes.add(new BreakingChange(
                BreakingChange.Type.MESSAGE_REMOVED,
                BreakingChange.Severity.INFO, // Plugin handles missing messages
                msgPath,
                "Message removed",
                msgDiff.messageName(),
                null
            ));
        }

        // Field changes
        for (FieldChange fieldChange : msgDiff.fieldChanges()) {
            collectFieldBreakingChanges(fieldChange, msgPath, changes);
        }

        // Nested messages
        for (MessageDiff nestedMsg : msgDiff.nestedMessageChanges()) {
            collectMessageBreakingChanges(nestedMsg, msgPath, changes);
        }

        // Nested enums
        for (EnumDiff nestedEnum : msgDiff.nestedEnumChanges()) {
            collectEnumBreakingChanges(nestedEnum, msgPath, changes);
        }
    }

    private void collectFieldBreakingChanges(FieldChange fieldChange, String msgPath,
                                              List<BreakingChange> changes) {
        String fieldPath = msgPath + "." + fieldChange.fieldName();

        switch (fieldChange.changeType()) {
            case REMOVED -> changes.add(new BreakingChange(
                BreakingChange.Type.FIELD_REMOVED,
                BreakingChange.Severity.INFO, // Plugin handles missing fields via supportsXxx()
                fieldPath,
                "Field removed",
                formatFieldType(fieldChange.v1Field()),
                null
            ));

            case TYPE_CHANGED -> {
                // Use the conflict type from FieldChange (TypeConflictType) and map to severity
                // This uses the existing TypeConflictType until we complete the refactoring
                var typeConflict = fieldChange.getTypeConflictType();
                MergedField.ConflictType conflictType = mapTypeConflict(typeConflict);
                MergedField.ConflictType.Severity severity = conflictType.getSeverity();

                BreakingChange.Type bcType = conflictType.isPluginHandled()
                    ? BreakingChange.Type.FIELD_TYPE_CONVERTED
                    : BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE;

                BreakingChange.Severity bcSeverity = switch (severity) {
                    case INFO -> BreakingChange.Severity.INFO;
                    case WARNING -> BreakingChange.Severity.WARNING;
                    case ERROR -> BreakingChange.Severity.ERROR;
                };

                String description = conflictType.isPluginHandled()
                    ? conflictType.name() + ": " + conflictType.getPluginNote()
                    : "Type changed incompatibly";

                changes.add(new BreakingChange(
                    bcType,
                    bcSeverity,
                    fieldPath,
                    description,
                    formatFieldType(fieldChange.v1Field()),
                    formatFieldType(fieldChange.v2Field())
                ));
            }

            case LABEL_CHANGED -> {
                if (fieldChange.v1Field() != null && fieldChange.v2Field() != null) {
                    boolean repeatedToSingular = fieldChange.v1Field().isRepeated() && !fieldChange.v2Field().isRepeated();
                    boolean singularToRepeated = !fieldChange.v1Field().isRepeated() && fieldChange.v2Field().isRepeated();

                    if (repeatedToSingular || singularToRepeated) {
                        changes.add(new BreakingChange(
                            BreakingChange.Type.CARDINALITY_CHANGED,
                            BreakingChange.Severity.INFO, // Plugin handles via REPEATED_SINGLE
                            fieldPath,
                            "REPEATED_SINGLE: " + MergedField.ConflictType.REPEATED_SINGLE.getPluginNote(),
                            fieldChange.v1Field().isRepeated() ? "repeated" : "singular",
                            fieldChange.v2Field().isRepeated() ? "repeated" : "singular"
                        ));
                    }
                }
            }

            default -> {
                // Other changes (NAME_CHANGED, MODIFIED) are not breaking
            }
        }
    }

    private void collectEnumBreakingChanges(EnumDiff enumDiff, String parentPath,
                                             List<BreakingChange> changes) {
        String enumPath = parentPath.isEmpty() ? enumDiff.enumName() : parentPath + "." + enumDiff.enumName();

        if (enumDiff.changeType() == ChangeType.REMOVED) {
            changes.add(new BreakingChange(
                BreakingChange.Type.ENUM_REMOVED,
                BreakingChange.Severity.INFO, // Plugin handles missing enums
                enumPath,
                "Enum removed",
                enumDiff.enumName(),
                null
            ));
        }

        for (EnumValueChange valueChange : enumDiff.valueChanges()) {
            String valuePath = enumPath + "." + valueChange.valueName();

            if (valueChange.changeType() == ChangeType.VALUE_REMOVED) {
                changes.add(new BreakingChange(
                    BreakingChange.Type.ENUM_VALUE_REMOVED,
                    BreakingChange.Severity.INFO, // Plugin handles via int accessor
                    valuePath,
                    "Enum value removed",
                    String.valueOf(valueChange.v1Number()),
                    null
                ));
            } else if (valueChange.changeType() == ChangeType.VALUE_NUMBER_CHANGED) {
                changes.add(new BreakingChange(
                    BreakingChange.Type.ENUM_VALUE_NUMBER_CHANGED,
                    BreakingChange.Severity.WARNING,
                    valuePath,
                    "Enum value number changed",
                    String.valueOf(valueChange.v1Number()),
                    String.valueOf(valueChange.v2Number())
                ));
            }
        }
    }

    // ========== Helper Methods ==========

    /**
     * Maps TypeConflictType (from diff module) to MergedField.ConflictType (from model module).
     * This is a temporary bridge until TypeConflictType is fully replaced.
     */
    private MergedField.ConflictType mapTypeConflict(TypeConflictType typeConflict) {
        if (typeConflict == null) {
            return MergedField.ConflictType.NONE;
        }
        return switch (typeConflict) {
            case NONE -> MergedField.ConflictType.NONE;
            case INT_ENUM -> MergedField.ConflictType.INT_ENUM;
            case ENUM_ENUM -> MergedField.ConflictType.ENUM_ENUM;
            case WIDENING -> MergedField.ConflictType.WIDENING;
            case FLOAT_DOUBLE -> MergedField.ConflictType.FLOAT_DOUBLE;
            case SIGNED_UNSIGNED -> MergedField.ConflictType.SIGNED_UNSIGNED;
            case REPEATED_SINGLE -> MergedField.ConflictType.REPEATED_SINGLE;
            case NARROWING -> MergedField.ConflictType.NARROWING;
            case STRING_BYTES -> MergedField.ConflictType.STRING_BYTES;
            case PRIMITIVE_MESSAGE -> MergedField.ConflictType.PRIMITIVE_MESSAGE;
            case OPTIONAL_REQUIRED -> MergedField.ConflictType.OPTIONAL_REQUIRED;
            case INCOMPATIBLE -> MergedField.ConflictType.INCOMPATIBLE;
        };
    }

    private MessageInfo createMessageInfoFromMerged(MergedMessage merged, String version) {
        // Collect fields that exist in this version
        List<FieldInfo> fields = merged.getFields().stream()
            .filter(f -> f.getPresentInVersions().contains(version))
            .map(f -> f.getVersionFields().get(version))
            .filter(Objects::nonNull)
            .toList();

        // Use the constructor for merged messages
        // Parameters: name, fullName, packageName, fields, nestedMessages, nestedEnums
        return new MessageInfo(
            merged.getName(),
            merged.getName(), // fullName - use name as minimal
            null, // packageName - not needed for diff display
            fields,
            List.of(), // nested messages - handled separately
            List.of()  // nested enums - handled separately
        );
    }

    private EnumInfo createEnumInfoFromMerged(MergedEnum merged, String version) {
        // Collect values that exist in this version
        List<EnumInfo.EnumValue> values = merged.getValues().stream()
            .filter(v -> v.getPresentInVersions().contains(version))
            .map(v -> new EnumInfo.EnumValue(v.getName(), v.getNumber()))
            .toList();

        // Use the constructor for merged enums
        return new EnumInfo(merged.getName(), values);
    }

    private String formatFieldType(FieldInfo field) {
        if (field == null) {
            return "null";
        }
        return FieldChange.formatType(field);
    }
}
