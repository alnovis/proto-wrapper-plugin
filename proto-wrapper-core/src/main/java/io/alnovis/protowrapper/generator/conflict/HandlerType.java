package io.alnovis.protowrapper.generator.conflict;

/**
 * Enumeration of conflict handler types.
 *
 * <p>Each type corresponds to a specific {@link ConflictHandler} implementation
 * and identifies the kind of field conflict or special case being handled.</p>
 *
 * <p>This enum is useful for:</p>
 * <ul>
 *   <li>Logging and debugging - identify which handler processed a field</li>
 *   <li>Metrics and monitoring - track conflict type distribution</li>
 *   <li>Testing - verify correct handler selection</li>
 * </ul>
 *
 * @since 1.2.0
 * @see ConflictHandler
 * @see FieldProcessingChain
 */
public enum HandlerType {

    /**
     * Handles INT to ENUM conflicts.
     * When a field is int in some versions and enum in others.
     *
     * @see IntEnumHandler
     */
    INT_ENUM("int/enum conflict"),

    /**
     * Handles ENUM to ENUM conflicts.
     * When a field is enum in all versions but with different enum types.
     *
     * @see EnumEnumHandler
     */
    ENUM_ENUM("enum/enum conflict"),

    /**
     * Handles STRING to BYTES conflicts.
     * When a field is String in some versions and bytes in others.
     *
     * @see StringBytesHandler
     */
    STRING_BYTES("string/bytes conflict"),

    /**
     * Handles numeric widening conflicts.
     * When a field needs to be widened (e.g., int to long).
     *
     * @see WideningHandler
     */
    WIDENING("numeric widening"),

    /**
     * Handles FLOAT to DOUBLE conflicts.
     * When a field is float in some versions and double in others.
     *
     * @see FloatDoubleHandler
     */
    FLOAT_DOUBLE("float/double conflict"),

    /**
     * Handles signed/unsigned integer conflicts.
     * When a field uses signed type in some versions and unsigned in others.
     *
     * @see SignedUnsignedHandler
     */
    SIGNED_UNSIGNED("signed/unsigned conflict"),

    /**
     * Handles repeated/single cardinality conflicts.
     * When a field is repeated in some versions and singular in others.
     *
     * @see RepeatedSingleHandler
     */
    REPEATED_SINGLE("repeated/single conflict"),

    /**
     * Handles primitive/message type conflicts.
     * When a field is primitive in some versions and message in others.
     *
     * @see PrimitiveMessageHandler
     */
    PRIMITIVE_MESSAGE("primitive/message conflict"),

    /**
     * Handles repeated fields with element type conflicts.
     * When a repeated field has different element types across versions.
     *
     * @see RepeatedConflictHandler
     */
    REPEATED_CONFLICT("repeated element conflict"),

    /**
     * Handles map fields.
     * Special processing for protobuf map fields.
     *
     * @see MapFieldHandler
     */
    MAP_FIELD("map field"),

    /**
     * Handles scalar well-known type fields.
     * Converts Google Well-Known Types (Timestamp, Duration, wrapper types) to idiomatic Java types.
     *
     * @since 1.3.0
     * @see WellKnownTypeHandler
     */
    WELL_KNOWN_TYPE("well-known type conversion"),

    /**
     * Handles repeated well-known type fields.
     * Converts repeated Google Well-Known Types to List of idiomatic Java types.
     *
     * @since 1.3.0
     * @see RepeatedWellKnownTypeHandler
     */
    REPEATED_WELL_KNOWN_TYPE("repeated well-known type conversion"),

    /**
     * Default handler for fields without conflicts.
     * Handles regular fields that have the same type across all versions.
     *
     * @see DefaultHandler
     */
    DEFAULT("default (no conflict)");

    private final String description;

    HandlerType(String description) {
        this.description = description;
    }

    /**
     * Get a human-readable description of this handler type.
     *
     * @return Description suitable for logging
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
