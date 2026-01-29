package io.alnovis.protowrapper.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime metadata about a protocol version's schema.
 *
 * <p>Provides access to enum values and message field information
 * without requiring proto descriptor dependencies at runtime.</p>
 *
 * <p>This interface is implemented by generated classes for each protocol version,
 * allowing runtime introspection of schema structure.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V203);
 * SchemaInfo schema = ctx.getSchemaInfo();
 *
 * // Get enum values
 * EnumInfo taxType = schema.getEnum("TaxTypeEnum").orElseThrow();
 * for (EnumValue v : taxType.getValues()) {
 *     System.out.println(v.name() + " = " + v.number());
 * }
 *
 * // Check if message exists
 * boolean hasTicket = schema.hasMessage("TicketRequest");
 * }</pre>
 *
 * @since 2.3.0
 */
public interface SchemaInfo {

    /**
     * Returns the version identifier for this schema.
     *
     * @return version ID (e.g., "v203")
     */
    String getVersionId();

    /**
     * Returns all enum metadata in this schema.
     *
     * @return unmodifiable map of enum name to metadata
     */
    Map<String, EnumInfo> getEnums();

    /**
     * Returns enum metadata by name.
     *
     * @param enumName simple name of the enum (e.g., "TaxTypeEnum")
     * @return optional containing enum info if found
     */
    default Optional<EnumInfo> getEnum(String enumName) {
        return Optional.ofNullable(getEnums().get(enumName));
    }

    /**
     * Returns all message metadata in this schema.
     *
     * @return unmodifiable map of message name to metadata
     */
    Map<String, MessageInfo> getMessages();

    /**
     * Returns message metadata by name.
     *
     * @param messageName simple name of the message (e.g., "TicketRequest")
     * @return optional containing message info if found
     */
    default Optional<MessageInfo> getMessage(String messageName) {
        return Optional.ofNullable(getMessages().get(messageName));
    }

    /**
     * Checks if this schema contains the specified enum.
     *
     * @param enumName simple name of the enum
     * @return true if enum exists in this schema
     */
    default boolean hasEnum(String enumName) {
        return getEnums().containsKey(enumName);
    }

    /**
     * Checks if this schema contains the specified message.
     *
     * @param messageName simple name of the message
     * @return true if message exists in this schema
     */
    default boolean hasMessage(String messageName) {
        return getMessages().containsKey(messageName);
    }

    /**
     * Metadata about an enum type.
     */
    interface EnumInfo {
        /**
         * Returns the simple name of the enum.
         *
         * @return enum name (e.g., "TaxTypeEnum")
         */
        String getName();

        /**
         * Returns the full protobuf type name.
         *
         * @return full name (e.g., "kkm.proto.v203.TaxTypeEnum")
         */
        String getFullName();

        /**
         * Returns all enum values.
         *
         * @return unmodifiable list of enum values
         */
        List<EnumValue> getValues();

        /**
         * Finds enum value by name.
         *
         * @param name value name (e.g., "VAT")
         * @return optional containing value if found
         */
        default Optional<EnumValue> getValue(String name) {
            return getValues().stream()
                    .filter(v -> v.name().equals(name))
                    .findFirst();
        }

        /**
         * Finds enum value by number.
         *
         * @param number value number (e.g., 100)
         * @return optional containing value if found
         */
        default Optional<EnumValue> getValueByNumber(int number) {
            return getValues().stream()
                    .filter(v -> v.number() == number)
                    .findFirst();
        }
    }

    /**
     * Single enum value with name and number.
     *
     * @param name enum value name (e.g., "VAT")
     * @param number enum value number (e.g., 100)
     */
    record EnumValue(String name, int number) {
        @Override
        public String toString() {
            return name + " (" + number + ")";
        }
    }

    /**
     * Metadata about a message type.
     */
    interface MessageInfo {
        /**
         * Returns the simple name of the message.
         *
         * @return message name (e.g., "TicketRequest")
         */
        String getName();

        /**
         * Returns the full protobuf type name.
         *
         * @return full name (e.g., "kkm.proto.v203.Ticket.TicketRequest")
         */
        String getFullName();

        /**
         * Returns all fields in this message.
         *
         * @return unmodifiable map of field name to metadata
         */
        Map<String, FieldInfo> getFields();

        /**
         * Returns field metadata by name.
         *
         * @param fieldName field name
         * @return optional containing field info if found
         */
        default Optional<FieldInfo> getField(String fieldName) {
            return Optional.ofNullable(getFields().get(fieldName));
        }

        /**
         * Checks if this message has the specified field.
         *
         * @param fieldName field name
         * @return true if field exists
         */
        default boolean hasField(String fieldName) {
            return getFields().containsKey(fieldName);
        }
    }

    /**
     * Metadata about a message field.
     *
     * @param name field name
     * @param number field number
     * @param type field type (e.g., "int32", "string", "TaxTypeEnum")
     * @param repeated true if field is repeated
     */
    record FieldInfo(String name, int number, String type, boolean repeated) {
        @Override
        public String toString() {
            return (repeated ? "repeated " : "") + type + " " + name + " = " + number;
        }
    }
}
