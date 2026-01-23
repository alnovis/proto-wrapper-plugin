package io.alnovis.protowrapper.diff.model;

import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.FieldMapping;

import java.util.Map;

/**
 * Represents a field that was heuristically detected as having been renumbered
 * between schema versions (without an explicit field mapping configured).
 *
 * <p>The detector finds pairs of REMOVED + ADDED fields with the same proto name
 * and compatible types, suggesting the field was simply assigned a new number.</p>
 *
 * @param messageName the message containing the field
 * @param fieldName   the proto field name (same in both versions)
 * @param v1Number    the field number in the source version
 * @param v2Number    the field number in the target version
 * @param v1Field     the field info from the source version
 * @param v2Field     the field info from the target version
 * @param confidence  the confidence level of the detection
 */
public record SuspectedRenumber(
    String messageName,
    String fieldName,
    int v1Number,
    int v2Number,
    FieldInfo v1Field,
    FieldInfo v2Field,
    Confidence confidence
) {

    /**
     * Confidence level for the renumber detection heuristic.
     */
    public enum Confidence {
        /** Same name and same type — very likely a renumbered field. */
        HIGH,
        /** Same name but compatible type change (widening, int-enum, etc.). */
        MEDIUM
    }

    /**
     * Creates a suggested FieldMapping configuration for this suspected renumber.
     *
     * @param v1Name the source version name
     * @param v2Name the target version name
     * @return a FieldMapping that would handle this renumbering
     */
    public FieldMapping toSuggestedMapping(String v1Name, String v2Name) {
        return new FieldMapping(messageName, fieldName,
            Map.of(v1Name, v1Number, v2Name, v2Number));
    }

    /**
     * Returns a human-readable description of the renumbering.
     *
     * @return formatted string describing the number change
     */
    public String getDescription() {
        return String.format("%s.%s: #%d -> #%d [%s]",
            messageName, fieldName, v1Number, v2Number, confidence);
    }
}
