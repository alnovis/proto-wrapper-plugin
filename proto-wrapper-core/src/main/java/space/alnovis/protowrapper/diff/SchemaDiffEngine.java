package space.alnovis.protowrapper.diff;

import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.diff.breaking.BreakingChangeDetector;
import space.alnovis.protowrapper.diff.model.*;
import space.alnovis.protowrapper.model.EnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MessageInfo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Engine for comparing two protobuf schema versions.
 * Detects added, removed, and modified messages, fields, and enums.
 */
public class SchemaDiffEngine {

    private final BreakingChangeDetector breakingDetector;

    /**
     * Creates a new SchemaDiffEngine with default settings.
     */
    public SchemaDiffEngine() {
        this.breakingDetector = new BreakingChangeDetector();
    }

    /**
     * Compares two version schemas and returns the differences.
     *
     * @param v1 Source version schema
     * @param v2 Target version schema
     * @return SchemaDiff containing all differences
     */
    public SchemaDiff compare(VersionSchema v1, VersionSchema v2) {
        List<MessageDiff> messageDiffs = compareMessages(v1, v2);
        List<EnumDiff> enumDiffs = compareEnums(v1, v2);
        List<BreakingChange> breakingChanges = breakingDetector.detectAll(messageDiffs, enumDiffs);

        return new SchemaDiff(
            v1.getVersion(),
            v2.getVersion(),
            messageDiffs,
            enumDiffs,
            breakingChanges
        );
    }

    // ========== Message Comparison ==========

    /**
     * Compares all messages between two schemas.
     */
    private List<MessageDiff> compareMessages(VersionSchema v1, VersionSchema v2) {
        Set<String> allNames = new HashSet<>();
        allNames.addAll(v1.getMessageNames());
        allNames.addAll(v2.getMessageNames());

        return allNames.stream()
            .map(name -> compareMessage(name, v1, v2))
            .filter(diff -> diff.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(MessageDiff::messageName))
            .toList();
    }

    /**
     * Compares a single message between two schemas.
     */
    private MessageDiff compareMessage(String name, VersionSchema v1, VersionSchema v2) {
        Optional<MessageInfo> m1 = v1.getMessage(name);
        Optional<MessageInfo> m2 = v2.getMessage(name);

        if (m1.isEmpty() && m2.isPresent()) {
            return MessageDiff.added(m2.get());
        }
        if (m1.isPresent() && m2.isEmpty()) {
            return MessageDiff.removed(m1.get());
        }
        if (m1.isPresent() && m2.isPresent()) {
            return compareMessageContents(m1.get(), m2.get());
        }

        // Neither exists (shouldn't happen if allNames is from both schemas)
        return new MessageDiff(name, ChangeType.UNCHANGED, null, null,
            List.of(), List.of(), List.of());
    }

    /**
     * Compares the contents of two messages (fields, nested types).
     */
    private MessageDiff compareMessageContents(MessageInfo m1, MessageInfo m2) {
        List<FieldChange> fieldChanges = compareFields(m1, m2);
        List<MessageDiff> nestedChanges = compareNestedMessages(m1, m2);
        List<EnumDiff> nestedEnumChanges = compareNestedEnums(m1, m2);

        return MessageDiff.compared(m1, m2, fieldChanges, nestedChanges, nestedEnumChanges);
    }

    // ========== Field Comparison ==========

    /**
     * Compares all fields between two messages.
     */
    private List<FieldChange> compareFields(MessageInfo m1, MessageInfo m2) {
        // Group fields by number for comparison
        Map<Integer, FieldInfo> v1Fields = m1.getFields().stream()
            .collect(Collectors.toMap(FieldInfo::getNumber, Function.identity()));
        Map<Integer, FieldInfo> v2Fields = m2.getFields().stream()
            .collect(Collectors.toMap(FieldInfo::getNumber, Function.identity()));

        Set<Integer> allNumbers = new HashSet<>();
        allNumbers.addAll(v1Fields.keySet());
        allNumbers.addAll(v2Fields.keySet());

        return allNumbers.stream()
            .map(num -> compareField(num, v1Fields.get(num), v2Fields.get(num)))
            .filter(fc -> fc.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(FieldChange::fieldNumber))
            .toList();
    }

    /**
     * Compares a single field between two versions.
     */
    private FieldChange compareField(int number, FieldInfo f1, FieldInfo f2) {
        if (f1 == null && f2 != null) {
            return new FieldChange(number, f2.getJavaName(), ChangeType.ADDED,
                null, f2, List.of("Added field"));
        }
        if (f1 != null && f2 == null) {
            return new FieldChange(number, f1.getJavaName(), ChangeType.REMOVED,
                f1, null, List.of("Removed field"));
        }
        if (f1 == null) {
            // Both null - shouldn't happen
            return new FieldChange(number, "unknown", ChangeType.UNCHANGED,
                null, null, List.of());
        }

        // Both exist - compare details
        List<String> changes = new ArrayList<>();
        ChangeType primaryChangeType = ChangeType.UNCHANGED;

        // Check type change
        if (!isSameType(f1, f2)) {
            changes.add(String.format("Type: %s -> %s",
                FieldChange.formatType(f1), FieldChange.formatType(f2)));
            primaryChangeType = ChangeType.TYPE_CHANGED;
        }

        // Check label change (repeated/singular)
        if (f1.isRepeated() != f2.isRepeated()) {
            changes.add(String.format("Cardinality: %s -> %s",
                f1.isRepeated() ? "repeated" : "singular",
                f2.isRepeated() ? "repeated" : "singular"));
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.LABEL_CHANGED;
            }
        }

        // Check name change (same number, different name)
        if (!f1.getProtoName().equals(f2.getProtoName())) {
            changes.add(String.format("Name: %s -> %s",
                f1.getProtoName(), f2.getProtoName()));
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.NAME_CHANGED;
            }
        }

        // Check oneof membership change
        if (f1.isInOneof() != f2.isInOneof()) {
            if (f1.isInOneof()) {
                changes.add("Moved out of oneof: " + f1.getOneofName());
            } else {
                changes.add("Moved into oneof: " + f2.getOneofName());
            }
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.MODIFIED;
            }
        } else if (f1.isInOneof() && !Objects.equals(f1.getOneofName(), f2.getOneofName())) {
            changes.add(String.format("Oneof changed: %s -> %s",
                f1.getOneofName(), f2.getOneofName()));
            if (primaryChangeType == ChangeType.UNCHANGED) {
                primaryChangeType = ChangeType.MODIFIED;
            }
        }

        if (changes.isEmpty()) {
            primaryChangeType = ChangeType.UNCHANGED;
        }

        return new FieldChange(number, f1.getJavaName(), primaryChangeType, f1, f2, changes);
    }

    /**
     * Checks if two fields have the same type.
     * Uses the same logic as VersionMerger: compare Java types (simple names for message/enum).
     */
    private boolean isSameType(FieldInfo f1, FieldInfo f2) {
        // Compare using Java types which extract simple names for MESSAGE/ENUM types
        // This matches the behavior of VersionMerger in the main plugin
        return Objects.equals(f1.getJavaType(), f2.getJavaType());
    }

    // ========== Nested Message Comparison ==========

    /**
     * Compares nested messages between two parent messages.
     */
    private List<MessageDiff> compareNestedMessages(MessageInfo m1, MessageInfo m2) {
        Map<String, MessageInfo> v1Nested = m1.getNestedMessages().stream()
            .collect(Collectors.toMap(MessageInfo::getName, Function.identity()));
        Map<String, MessageInfo> v2Nested = m2.getNestedMessages().stream()
            .collect(Collectors.toMap(MessageInfo::getName, Function.identity()));

        Set<String> allNames = new HashSet<>();
        allNames.addAll(v1Nested.keySet());
        allNames.addAll(v2Nested.keySet());

        return allNames.stream()
            .map(name -> compareNestedMessage(name, v1Nested.get(name), v2Nested.get(name)))
            .filter(diff -> diff.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(MessageDiff::messageName))
            .toList();
    }

    /**
     * Compares a single nested message.
     */
    private MessageDiff compareNestedMessage(String name, MessageInfo m1, MessageInfo m2) {
        if (m1 == null && m2 != null) {
            return MessageDiff.added(m2);
        }
        if (m1 != null && m2 == null) {
            return MessageDiff.removed(m1);
        }
        if (m1 != null) {
            return compareMessageContents(m1, m2);
        }
        return new MessageDiff(name, ChangeType.UNCHANGED, null, null,
            List.of(), List.of(), List.of());
    }

    // ========== Nested Enum Comparison ==========

    /**
     * Compares nested enums between two parent messages.
     */
    private List<EnumDiff> compareNestedEnums(MessageInfo m1, MessageInfo m2) {
        Map<String, EnumInfo> v1Enums = m1.getNestedEnums().stream()
            .collect(Collectors.toMap(EnumInfo::getName, Function.identity()));
        Map<String, EnumInfo> v2Enums = m2.getNestedEnums().stream()
            .collect(Collectors.toMap(EnumInfo::getName, Function.identity()));

        Set<String> allNames = new HashSet<>();
        allNames.addAll(v1Enums.keySet());
        allNames.addAll(v2Enums.keySet());

        return allNames.stream()
            .map(name -> compareEnumPair(name, v1Enums.get(name), v2Enums.get(name)))
            .filter(diff -> diff.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(EnumDiff::enumName))
            .toList();
    }

    // ========== Top-Level Enum Comparison ==========

    /**
     * Compares all top-level enums between two schemas.
     */
    private List<EnumDiff> compareEnums(VersionSchema v1, VersionSchema v2) {
        Set<String> allNames = new HashSet<>();
        allNames.addAll(v1.getEnumNames());
        allNames.addAll(v2.getEnumNames());

        return allNames.stream()
            .map(name -> {
                Optional<EnumInfo> e1 = v1.getEnum(name);
                Optional<EnumInfo> e2 = v2.getEnum(name);
                return compareEnumPair(name, e1.orElse(null), e2.orElse(null));
            })
            .filter(diff -> diff.changeType() != ChangeType.UNCHANGED)
            .sorted(Comparator.comparing(EnumDiff::enumName))
            .toList();
    }

    /**
     * Compares a pair of enums (one from each version).
     */
    private EnumDiff compareEnumPair(String name, EnumInfo e1, EnumInfo e2) {
        if (e1 == null && e2 != null) {
            return EnumDiff.added(e2);
        }
        if (e1 != null && e2 == null) {
            return EnumDiff.removed(e1);
        }
        if (e1 != null) {
            return compareEnumContents(e1, e2);
        }
        return new EnumDiff(name, ChangeType.UNCHANGED, null, null, List.of());
    }

    /**
     * Compares the values of two enums.
     */
    private EnumDiff compareEnumContents(EnumInfo e1, EnumInfo e2) {
        Map<String, Integer> v1Values = e1.getValues().stream()
            .collect(Collectors.toMap(EnumInfo.EnumValue::name, EnumInfo.EnumValue::number));
        Map<String, Integer> v2Values = e2.getValues().stream()
            .collect(Collectors.toMap(EnumInfo.EnumValue::name, EnumInfo.EnumValue::number));

        Set<String> allValueNames = new HashSet<>();
        allValueNames.addAll(v1Values.keySet());
        allValueNames.addAll(v2Values.keySet());

        List<EnumValueChange> changes = new ArrayList<>();

        for (String valueName : allValueNames) {
            Integer v1Num = v1Values.get(valueName);
            Integer v2Num = v2Values.get(valueName);

            if (v1Num == null && v2Num != null) {
                changes.add(EnumValueChange.added(valueName, v2Num));
            } else if (v1Num != null && v2Num == null) {
                changes.add(EnumValueChange.removed(valueName, v1Num));
            } else if (v1Num != null && !v1Num.equals(v2Num)) {
                changes.add(EnumValueChange.numberChanged(valueName, v1Num, v2Num));
            }
        }

        // Sort by value name
        changes.sort(Comparator.comparing(EnumValueChange::valueName));

        return EnumDiff.modified(e1, e2, changes);
    }
}
