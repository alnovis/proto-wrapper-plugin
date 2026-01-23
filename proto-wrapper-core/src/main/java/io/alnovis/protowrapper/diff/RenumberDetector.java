package io.alnovis.protowrapper.diff;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.FieldInfo;

import java.util.*;

/**
 * Heuristic detector for field renumbering between schema versions.
 *
 * <p>Scans field changes within modified messages to find fields
 * that likely moved to a different number rather than being truly added/removed.</p>
 *
 * <p>Detection strategies:</p>
 * <ul>
 *   <li><b>REMOVED+ADDED pairs:</b> Same proto name in both REMOVED and ADDED fields</li>
 *   <li><b>Displaced fields:</b> REMOVED field's proto name matches a NAME_CHANGED field's
 *       v2 proto name, indicating the field was renumbered to the position of another
 *       field that was removed or replaced</li>
 * </ul>
 *
 * <p>Type compatibility is required: same type (HIGH confidence) or compatible
 * widening conversion (MEDIUM confidence).</p>
 */
public class RenumberDetector {

    private static final Set<Type> INTEGER_TYPES = Set.of(
        Type.TYPE_INT32, Type.TYPE_INT64,
        Type.TYPE_UINT32, Type.TYPE_UINT64,
        Type.TYPE_SINT32, Type.TYPE_SINT64,
        Type.TYPE_FIXED32, Type.TYPE_FIXED64,
        Type.TYPE_SFIXED32, Type.TYPE_SFIXED64
    );

    /**
     * Detects suspected renumbered fields across all message diffs.
     *
     * @param messageDiffs the list of message differences to scan
     * @return list of suspected renumbered fields
     */
    public List<SuspectedRenumber> detect(List<MessageDiff> messageDiffs) {
        List<SuspectedRenumber> result = new ArrayList<>();

        for (MessageDiff msgDiff : messageDiffs) {
            if (msgDiff.changeType() != ChangeType.MODIFIED) {
                continue;
            }
            detectInMessage(msgDiff, result);
        }

        return Collections.unmodifiableList(result);
    }

    private void detectInMessage(MessageDiff msgDiff, List<SuspectedRenumber> result) {
        List<FieldChange> removed = msgDiff.fieldChanges().stream()
            .filter(fc -> fc.changeType() == ChangeType.REMOVED)
            .toList();

        List<FieldChange> added = msgDiff.fieldChanges().stream()
            .filter(fc -> fc.changeType() == ChangeType.ADDED)
            .toList();

        Set<String> matchedRemovedNames = new HashSet<>();

        // Strategy 1: Match removed+added pairs by proto name
        if (!removed.isEmpty() && !added.isEmpty()) {
            Set<String> matchedAddedNames = new HashSet<>();

            for (FieldChange removedField : removed) {
                if (removedField.v1Field() == null) {
                    continue;
                }
                String protoName = removedField.v1Field().getProtoName();

                for (FieldChange addedField : added) {
                    if (addedField.v2Field() == null) {
                        continue;
                    }
                    if (matchedAddedNames.contains(addedField.v2Field().getProtoName())) {
                        continue; // Already matched
                    }

                    if (protoName.equals(addedField.v2Field().getProtoName())) {
                        // Same name — check type compatibility
                        SuspectedRenumber.Confidence confidence = assessConfidence(
                            removedField.v1Field(), addedField.v2Field());

                        if (confidence != null) {
                            result.add(new SuspectedRenumber(
                                msgDiff.messageName(),
                                protoName,
                                removedField.fieldNumber(),
                                addedField.fieldNumber(),
                                removedField.v1Field(),
                                addedField.v2Field(),
                                confidence
                            ));
                            matchedAddedNames.add(protoName);
                            matchedRemovedNames.add(protoName);
                            break; // One match per removed field
                        }
                    }
                }
            }
        }

        // Strategy 2: Displaced fields — match REMOVED fields against modified fields
        // where the modified field's v2 proto name matches the REMOVED field's v1 proto name.
        // This handles the case where a field was renumbered to the position of another field
        // that was removed (e.g., parent_ticket moved from #17 to #15, displacing shift_document_number).
        // Note: The primary changeType may be TYPE_CHANGED, NAME_CHANGED, or MODIFIED depending
        // on what other differences exist, so we match by v1/v2 name difference instead of changeType.
        if (!removed.isEmpty()) {
            List<FieldChange> renamedFields = msgDiff.fieldChanges().stream()
                .filter(fc -> fc.v1Field() != null && fc.v2Field() != null)
                .filter(fc -> !fc.v1Field().getProtoName().equals(fc.v2Field().getProtoName()))
                .toList();

            if (!renamedFields.isEmpty()) {
                for (FieldChange removedField : removed) {
                    if (removedField.v1Field() == null) {
                        continue;
                    }
                    String protoName = removedField.v1Field().getProtoName();
                    if (matchedRemovedNames.contains(protoName)) {
                        continue; // Already matched by strategy 1
                    }

                    for (FieldChange renamedField : renamedFields) {
                        if (protoName.equals(renamedField.v2Field().getProtoName())) {
                            // REMOVED field's name matches the v2 side of a NAME_CHANGED field
                            // This means the field was renumbered to this position
                            SuspectedRenumber.Confidence confidence = assessConfidence(
                                removedField.v1Field(), renamedField.v2Field());

                            if (confidence != null) {
                                result.add(new SuspectedRenumber(
                                    msgDiff.messageName(),
                                    protoName,
                                    removedField.fieldNumber(),
                                    renamedField.fieldNumber(),
                                    removedField.v1Field(),
                                    renamedField.v2Field(),
                                    confidence
                                ));
                                matchedRemovedNames.add(protoName);
                                break; // One match per removed field
                            }
                        }
                    }
                }
            }
        }

        // Recurse into nested messages (always, regardless of current level matches)
        for (MessageDiff nested : msgDiff.nestedMessageChanges()) {
            if (nested.changeType() == ChangeType.MODIFIED) {
                detectInMessage(nested, result);
            }
        }
    }

    /**
     * Assesses the confidence level for a suspected renumber based on type compatibility.
     *
     * @param v1Field the field from source version
     * @param v2Field the field from target version
     * @return confidence level, or null if types are incompatible
     */
    private SuspectedRenumber.Confidence assessConfidence(FieldInfo v1Field, FieldInfo v2Field) {
        // Check cardinality mismatch (repeated vs singular) — not a renumber
        if (v1Field.isRepeated() != v2Field.isRepeated()) {
            return null;
        }

        // Same Java type — highest confidence
        if (Objects.equals(v1Field.getJavaType(), v2Field.getJavaType())) {
            return SuspectedRenumber.Confidence.HIGH;
        }

        // Check for compatible type conversions
        Type t1 = v1Field.getType();
        Type t2 = v2Field.getType();

        if (isCompatibleConversion(t1, t2)) {
            return SuspectedRenumber.Confidence.MEDIUM;
        }

        return null; // Incompatible types — not a suspected renumber
    }

    /**
     * Checks if the type conversion is compatible (widening, int-enum, float-double).
     */
    private boolean isCompatibleConversion(Type from, Type to) {
        // Integer widening (int32 -> int64, etc.)
        if (INTEGER_TYPES.contains(from) && INTEGER_TYPES.contains(to)) {
            return true;
        }

        // Float -> double
        if ((from == Type.TYPE_FLOAT && to == Type.TYPE_DOUBLE) ||
            (from == Type.TYPE_DOUBLE && to == Type.TYPE_FLOAT)) {
            return true;
        }

        // Int <-> enum
        if ((INTEGER_TYPES.contains(from) && to == Type.TYPE_ENUM) ||
            (from == Type.TYPE_ENUM && INTEGER_TYPES.contains(to))) {
            return true;
        }

        // String <-> bytes (common in proto evolution)
        return (from == Type.TYPE_STRING && to == Type.TYPE_BYTES) ||
               (from == Type.TYPE_BYTES && to == Type.TYPE_STRING);
    }
}
