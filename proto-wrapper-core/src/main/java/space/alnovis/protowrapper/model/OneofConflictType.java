package space.alnovis.protowrapper.model;

/**
 * Types of conflicts that can occur with oneof fields across protocol versions.
 */
public enum OneofConflictType {

    /**
     * Oneof exists only in some versions, not all.
     * Example: V1 has no oneof, V2 has oneof "method".
     */
    PARTIAL_EXISTENCE("Oneof exists only in some versions"),

    /**
     * Different fields in the oneof group across versions.
     * Example: V1 has {credit_card, bank_transfer}, V2 adds {crypto}.
     */
    FIELD_SET_DIFFERENCE("Different fields in oneof across versions"),

    /**
     * Field type conflict within oneof field.
     * Example: V1 has string text=1, V2 has bytes text=1.
     */
    FIELD_TYPE_CONFLICT("Field type conflict within oneof"),

    /**
     * Oneof was renamed between versions (detected by same field numbers).
     * Example: V1 has "payment_method", V2 has "method" with same fields.
     */
    RENAMED("Oneof renamed between versions"),

    /**
     * Field moved into or out of oneof between versions.
     * Example: V1 has regular field credit_card=10, V2 has it in oneof.
     */
    FIELD_MEMBERSHIP_CHANGE("Field moved in/out of oneof"),

    /**
     * Field number changed for semantically same field (detected by name).
     * Example: V1 has credit_card=10, V2 has credit_card=15.
     */
    FIELD_NUMBER_CHANGE("Field number changed within oneof"),

    /**
     * Field was removed from oneof in newer version.
     * Example: V1 has {credit_card, cash}, V2 has only {credit_card}.
     */
    FIELD_REMOVED("Field removed from oneof in some version"),

    /**
     * Oneof contains fields with incompatible types that cannot be merged.
     */
    INCOMPATIBLE_TYPES("Incompatible field types in oneof");

    private final String description;

    OneofConflictType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
