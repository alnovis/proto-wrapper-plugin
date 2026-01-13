package space.alnovis.protowrapper.contract;

/**
 * Represents field presence semantics in protobuf.
 *
 * <p>This enum captures the different ways protobuf tracks whether a field
 * has been explicitly set, which directly impacts the wrapper API behavior.</p>
 *
 * <h2>Proto2 vs Proto3 Semantics</h2>
 * <p>Proto2 and proto3 have fundamentally different approaches to field presence:</p>
 * <ul>
 *   <li><b>Proto2:</b> Every field can be explicitly checked for presence</li>
 *   <li><b>Proto3:</b> Only some fields support presence checking</li>
 * </ul>
 */
public enum FieldPresence {

    /**
     * Proto2 optional field.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>has*() method exists and tracks explicit assignment</li>
     *   <li>Getter returns null when not set</li>
     *   <li>Can distinguish "not set" from "set to default"</li>
     * </ul>
     */
    PROTO2_OPTIONAL,

    /**
     * Proto2 required field.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>has*() method exists (always returns true for valid message)</li>
     *   <li>Getter never returns null</li>
     *   <li>Proto validation ensures field is always set</li>
     * </ul>
     */
    PROTO2_REQUIRED,

    /**
     * Proto3 implicit presence (no 'optional' keyword).
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>has*() method does NOT exist for scalar types</li>
     *   <li>Getter returns default value (0, false, "") when not set</li>
     *   <li>Cannot distinguish "not set" from "set to default"</li>
     *   <li>Exception: message fields still have has*() method</li>
     * </ul>
     */
    PROTO3_IMPLICIT,

    /**
     * Proto3 explicit presence (with 'optional' keyword).
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>has*() method exists for all types including scalars</li>
     *   <li>Getter returns null when not set</li>
     *   <li>Can distinguish "not set" from "set to default"</li>
     *   <li>Implemented via synthetic oneof in proto3</li>
     * </ul>
     */
    PROTO3_EXPLICIT_OPTIONAL;

    /**
     * @return true if has*() method is available for scalar fields
     */
    public boolean hasMethodAvailableForScalars() {
        return this != PROTO3_IMPLICIT;
    }

    /**
     * @return true if this is a proto2 presence mode
     */
    public boolean isProto2() {
        return this == PROTO2_OPTIONAL || this == PROTO2_REQUIRED;
    }

    /**
     * @return true if this is a proto3 presence mode
     */
    public boolean isProto3() {
        return this == PROTO3_IMPLICIT || this == PROTO3_EXPLICIT_OPTIONAL;
    }

    /**
     * @return true if field value is guaranteed to be present (required fields)
     */
    public boolean isRequired() {
        return this == PROTO2_REQUIRED;
    }

    /**
     * @return true if scalar getters should return null when unset
     */
    public boolean scalarNullable() {
        return this == PROTO2_OPTIONAL || this == PROTO3_EXPLICIT_OPTIONAL;
    }
}
