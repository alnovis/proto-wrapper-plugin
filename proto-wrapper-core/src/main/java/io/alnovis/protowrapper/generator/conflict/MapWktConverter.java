package io.alnovis.protowrapper.generator.conflict;

import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;

/**
 * Utility class for Well-Known Type conversions in map field processing.
 *
 * <p>This class handles conversion between protobuf Well-Known Types and
 * their Java equivalents for map values.</p>
 *
 * <p>All methods are static and thread-safe.</p>
 *
 * @since 2.3.1
 * @see MapFieldHandler
 */
public final class MapWktConverter {

    private MapWktConverter() {
        // Utility class - no instantiation
    }

    /**
     * Generate extraction code for WKT map value (Proto to Java).
     *
     * @param wkt the Well-Known Type info
     * @return the extraction code string
     */
    static String generateWktMapValueExtraction(WellKnownTypeInfo wkt) {
        return switch (wkt) {
            case TIMESTAMP -> "v != null ? java.time.Instant.ofEpochSecond(((com.google.protobuf.Timestamp) v).getSeconds(), ((com.google.protobuf.Timestamp) v).getNanos()) : null";
            case DURATION -> "v != null ? java.time.Duration.ofSeconds(((com.google.protobuf.Duration) v).getSeconds(), ((com.google.protobuf.Duration) v).getNanos()) : null";
            case STRING_VALUE -> "v != null ? ((com.google.protobuf.StringValue) v).getValue() : null";
            case INT32_VALUE -> "v != null ? ((com.google.protobuf.Int32Value) v).getValue() : null";
            case INT64_VALUE -> "v != null ? ((com.google.protobuf.Int64Value) v).getValue() : null";
            case UINT32_VALUE -> "v != null ? Integer.toUnsignedLong(((com.google.protobuf.UInt32Value) v).getValue()) : null";
            case UINT64_VALUE -> "v != null ? ((com.google.protobuf.UInt64Value) v).getValue() : null";
            case BOOL_VALUE -> "v != null ? ((com.google.protobuf.BoolValue) v).getValue() : null";
            case FLOAT_VALUE -> "v != null ? ((com.google.protobuf.FloatValue) v).getValue() : null";
            case DOUBLE_VALUE -> "v != null ? ((com.google.protobuf.DoubleValue) v).getValue() : null";
            case BYTES_VALUE -> "v != null ? ((com.google.protobuf.BytesValue) v).getValue().toByteArray() : null";
            default -> "v"; // For complex types like Struct, Value - pass through
        };
    }

    /**
     * Generate building code for WKT map value (Java to Proto).
     *
     * @param wkt the Well-Known Type info
     * @param valueName the variable name containing the value
     * @param fieldName the capitalized field name for putXxx method
     * @return the building code string
     */
    static String generateWktMapValueBuild(WellKnownTypeInfo wkt, String valueName, String fieldName) {
        return switch (wkt) {
            case TIMESTAMP -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.Timestamp.newBuilder()" +
                            ".setSeconds(%s.getEpochSecond()).setNanos(%s.getNano()).build()); }",
                    valueName, fieldName, valueName, valueName);
            case DURATION -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.Duration.newBuilder()" +
                            ".setSeconds(%s.getSeconds()).setNanos(%s.getNano()).build()); }",
                    valueName, fieldName, valueName, valueName);
            case STRING_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.StringValue.of(%s)); }",
                    valueName, fieldName, valueName);
            case INT32_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.Int32Value.of(%s)); }",
                    valueName, fieldName, valueName);
            case INT64_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.Int64Value.of(%s)); }",
                    valueName, fieldName, valueName);
            case UINT32_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.UInt32Value.of(%s.intValue())); }",
                    valueName, fieldName, valueName);
            case UINT64_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.UInt64Value.of(%s)); }",
                    valueName, fieldName, valueName);
            case BOOL_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.BoolValue.of(%s)); }",
                    valueName, fieldName, valueName);
            case FLOAT_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.FloatValue.of(%s)); }",
                    valueName, fieldName, valueName);
            case DOUBLE_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.DoubleValue.of(%s)); }",
                    valueName, fieldName, valueName);
            case BYTES_VALUE -> String.format(
                    "if (%s != null) { protoBuilder.put%s(key, com.google.protobuf.BytesValue.of(com.google.protobuf.ByteString.copyFrom(%s))); }",
                    valueName, fieldName, valueName);
            default -> String.format("protoBuilder.put%s(key, %s)", fieldName, valueName);
        };
    }
}
