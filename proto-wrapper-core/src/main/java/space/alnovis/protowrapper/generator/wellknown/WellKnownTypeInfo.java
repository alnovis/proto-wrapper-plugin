package space.alnovis.protowrapper.generator.wellknown;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of Google Well-Known Types and their Java equivalents.
 *
 * <p>This enum provides mapping between protobuf well-known types and idiomatic Java types,
 * along with code generation patterns for extraction (proto to Java) and building (Java to proto).</p>
 *
 * <h2>Supported Types</h2>
 * <table>
 *   <tr><th>Proto Type</th><th>Java Type</th></tr>
 *   <tr><td>google.protobuf.Timestamp</td><td>java.time.Instant</td></tr>
 *   <tr><td>google.protobuf.Duration</td><td>java.time.Duration</td></tr>
 *   <tr><td>google.protobuf.StringValue</td><td>String</td></tr>
 *   <tr><td>google.protobuf.Int32Value</td><td>Integer</td></tr>
 *   <tr><td>google.protobuf.Int64Value</td><td>Long</td></tr>
 *   <tr><td>google.protobuf.BoolValue</td><td>Boolean</td></tr>
 *   <tr><td>google.protobuf.FloatValue</td><td>Float</td></tr>
 *   <tr><td>google.protobuf.DoubleValue</td><td>Double</td></tr>
 *   <tr><td>google.protobuf.BytesValue</td><td>byte[]</td></tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Check if a proto type is a well-known type
 * if (WellKnownTypeInfo.isWellKnownType(".google.protobuf.Timestamp")) {
 *     WellKnownTypeInfo wkt = WellKnownTypeInfo.fromProtoType(".google.protobuf.Timestamp").get();
 *     TypeName javaType = wkt.getJavaTypeName();  // java.time.Instant
 * }
 * }</pre>
 *
 * @since 1.3.0
 */
public enum WellKnownTypeInfo {

    /**
     * google.protobuf.Timestamp -> java.time.Instant
     */
    TIMESTAMP(
            ".google.protobuf.Timestamp",
            "google.protobuf.Timestamp",
            ClassName.get(Instant.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format(
                    "%s ? java.time.Instant.ofEpochSecond(%s.getSeconds(), %s.getNanos()) : null",
                    hasCheck, protoGetter, protoGetter
            );
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.Timestamp.newBuilder()" +
                            ".setSeconds(%s.getEpochSecond()).setNanos(%s.getNano()).build() : null",
                    value, value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.Timestamp.newBuilder()" +
                            ".setSeconds(%s.getEpochSecond()).setNanos(%s.getNano()).build()); }",
                    value, builderVar, setterName, value, value
            );
        }
    },

    /**
     * google.protobuf.Duration -> java.time.Duration
     */
    DURATION(
            ".google.protobuf.Duration",
            "google.protobuf.Duration",
            ClassName.get(Duration.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format(
                    "%s ? java.time.Duration.ofSeconds(%s.getSeconds(), %s.getNanos()) : null",
                    hasCheck, protoGetter, protoGetter
            );
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.Duration.newBuilder()" +
                            ".setSeconds(%s.getSeconds()).setNanos(%s.getNano()).build() : null",
                    value, value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.Duration.newBuilder()" +
                            ".setSeconds(%s.getSeconds()).setNanos(%s.getNano()).build()); }",
                    value, builderVar, setterName, value, value
            );
        }
    },

    /**
     * google.protobuf.StringValue -> String (nullable)
     */
    STRING_VALUE(
            ".google.protobuf.StringValue",
            "google.protobuf.StringValue",
            ClassName.get(String.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.StringValue.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.StringValue.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.Int32Value -> Integer (nullable)
     */
    INT32_VALUE(
            ".google.protobuf.Int32Value",
            "google.protobuf.Int32Value",
            ClassName.get(Integer.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.Int32Value.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.Int32Value.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.UInt32Value -> Long (nullable, unsigned)
     */
    UINT32_VALUE(
            ".google.protobuf.UInt32Value",
            "google.protobuf.UInt32Value",
            ClassName.get(Long.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? Integer.toUnsignedLong(%s.getValue()) : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.UInt32Value.of(%s.intValue()) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.UInt32Value.of(%s.intValue())); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.UInt64Value -> Long (nullable, unsigned treated as signed)
     */
    UINT64_VALUE(
            ".google.protobuf.UInt64Value",
            "google.protobuf.UInt64Value",
            ClassName.get(Long.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.UInt64Value.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.UInt64Value.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.Int64Value -> Long (nullable)
     */
    INT64_VALUE(
            ".google.protobuf.Int64Value",
            "google.protobuf.Int64Value",
            ClassName.get(Long.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.Int64Value.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.Int64Value.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.BoolValue -> Boolean (nullable)
     */
    BOOL_VALUE(
            ".google.protobuf.BoolValue",
            "google.protobuf.BoolValue",
            ClassName.get(Boolean.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.BoolValue.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.BoolValue.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.FloatValue -> Float (nullable)
     */
    FLOAT_VALUE(
            ".google.protobuf.FloatValue",
            "google.protobuf.FloatValue",
            ClassName.get(Float.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.FloatValue.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.FloatValue.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.DoubleValue -> Double (nullable)
     */
    DOUBLE_VALUE(
            ".google.protobuf.DoubleValue",
            "google.protobuf.DoubleValue",
            ClassName.get(Double.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.DoubleValue.of(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.DoubleValue.of(%s)); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.BytesValue -> byte[] (nullable)
     */
    BYTES_VALUE(
            ".google.protobuf.BytesValue",
            "google.protobuf.BytesValue",
            ArrayTypeName.of(TypeName.BYTE),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getValue().toByteArray() : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? com.google.protobuf.BytesValue.of(" +
                            "com.google.protobuf.ByteString.copyFrom(%s)) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(com.google.protobuf.BytesValue.of(" +
                            "com.google.protobuf.ByteString.copyFrom(%s))); }",
                    value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.FieldMask -> List&lt;String&gt;
     */
    FIELD_MASK(
            ".google.protobuf.FieldMask",
            "google.protobuf.FieldMask",
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), ClassName.get(String.class)),
            "java.util.Collections.emptyList()"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? %s.getPathsList() : java.util.Collections.emptyList()", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null && !%s.isEmpty() ? com.google.protobuf.FieldMask.newBuilder()" +
                            ".addAllPaths(%s).build() : null",
                    value, value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null && !%s.isEmpty()) { %s.set%s(com.google.protobuf.FieldMask.newBuilder()" +
                            ".addAllPaths(%s).build()); }",
                    value, value, builderVar, setterName, value
            );
        }
    },

    /**
     * google.protobuf.Struct -> Map&lt;String, Object&gt;
     * <p>Requires StructConverter utility class for conversion.</p>
     */
    STRUCT(
            ".google.protobuf.Struct",
            "google.protobuf.Struct",
            ParameterizedTypeName.get(
                    ClassName.get(java.util.Map.class),
                    ClassName.get(String.class),
                    ClassName.get(Object.class)
            ),
            "java.util.Collections.emptyMap()"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? StructConverter.toMap(%s) : java.util.Collections.emptyMap()", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? StructConverter.toStruct(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(StructConverter.toStruct(%s)); }",
                    value, builderVar, setterName, value
            );
        }

        @Override
        public boolean requiresUtilityClass() {
            return true;
        }
    },

    /**
     * google.protobuf.Value -> Object
     * <p>Requires StructConverter utility class for conversion.</p>
     */
    VALUE(
            ".google.protobuf.Value",
            "google.protobuf.Value",
            ClassName.get(Object.class),
            "null"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? StructConverter.toObject(%s) : null", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format("StructConverter.toValue(%s)", value);
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "%s.set%s(StructConverter.toValue(%s));",
                    builderVar, setterName, value
            );
        }

        @Override
        public boolean requiresUtilityClass() {
            return true;
        }
    },

    /**
     * google.protobuf.ListValue -> List&lt;Object&gt;
     * <p>Requires StructConverter utility class for conversion.</p>
     */
    LIST_VALUE(
            ".google.protobuf.ListValue",
            "google.protobuf.ListValue",
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), ClassName.get(Object.class)),
            "java.util.Collections.emptyList()"
    ) {
        @Override
        public String getExtractionCode(String protoGetter, String hasCheck) {
            return String.format("%s ? StructConverter.toList(%s) : java.util.Collections.emptyList()", hasCheck, protoGetter);
        }

        @Override
        public String getBuildingCode(String value) {
            return String.format(
                    "%s != null ? StructConverter.toListValue(%s) : null",
                    value, value
            );
        }

        @Override
        public String getBuilderSetterCode(String builderVar, String setterName, String value) {
            return String.format(
                    "if (%s != null) { %s.set%s(StructConverter.toListValue(%s)); }",
                    value, builderVar, setterName, value
            );
        }

        @Override
        public boolean requiresUtilityClass() {
            return true;
        }
    };

    private final String protoTypeFull;    // with leading dot: ".google.protobuf.Timestamp"
    private final String protoTypeShort;   // without leading dot: "google.protobuf.Timestamp"
    private final TypeName javaTypeName;
    private final String defaultValue;

    private static final Map<String, WellKnownTypeInfo> BY_PROTO_TYPE = new HashMap<>();

    static {
        for (WellKnownTypeInfo info : values()) {
            BY_PROTO_TYPE.put(info.protoTypeFull, info);
            BY_PROTO_TYPE.put(info.protoTypeShort, info);
        }
    }

    WellKnownTypeInfo(String protoTypeFull, String protoTypeShort,
                      TypeName javaTypeName, String defaultValue) {
        this.protoTypeFull = protoTypeFull;
        this.protoTypeShort = protoTypeShort;
        this.javaTypeName = javaTypeName;
        this.defaultValue = defaultValue;
    }

    /**
     * Get the full proto type name (with leading dot).
     *
     * @return Proto type name, e.g., ".google.protobuf.Timestamp"
     */
    public String getProtoTypeFull() {
        return protoTypeFull;
    }

    /**
     * Get the short proto type name (without leading dot).
     *
     * @return Proto type name, e.g., "google.protobuf.Timestamp"
     */
    public String getProtoTypeShort() {
        return protoTypeShort;
    }

    /**
     * Get the JavaPoet TypeName for this well-known type.
     *
     * @return Java type name for code generation
     */
    public TypeName getJavaTypeName() {
        return javaTypeName;
    }

    /**
     * Get the simple Java type name as string.
     *
     * @return Simple type name, e.g., "Instant", "Duration", "String"
     */
    public String getJavaTypeSimpleName() {
        if (javaTypeName instanceof ClassName) {
            return ((ClassName) javaTypeName).simpleName();
        }
        return javaTypeName.toString();
    }

    /**
     * Get the default value for this type.
     *
     * @return Default value string (always "null" for well-known types)
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Generate extraction code that converts proto type to Java type.
     *
     * @param protoGetter Expression to get the proto value, e.g., "proto.getCreatedAt()"
     * @param hasCheck    Expression to check if field has value, e.g., "proto.hasCreatedAt()"
     * @return Code snippet for extraction
     */
    public abstract String getExtractionCode(String protoGetter, String hasCheck);

    /**
     * Generate building code that converts Java type to proto type.
     *
     * @param value Variable name holding the Java value
     * @return Code snippet for building proto value
     */
    public abstract String getBuildingCode(String value);

    /**
     * Generate builder setter code that sets the proto field from Java value.
     *
     * @param builderVar Variable name of the proto builder
     * @param setterName Name of the setter method (capitalized field name)
     * @param value      Variable name holding the Java value
     * @return Code snippet for setting builder field
     */
    public abstract String getBuilderSetterCode(String builderVar, String setterName, String value);

    /**
     * Check if a proto type is a well-known type.
     *
     * @param protoType Proto type name (with or without leading dot)
     * @return true if the type is a supported well-known type
     */
    public static boolean isWellKnownType(String protoType) {
        return BY_PROTO_TYPE.containsKey(protoType);
    }

    /**
     * Get WellKnownTypeInfo for a proto type.
     *
     * @param protoType Proto type name (with or without leading dot)
     * @return Optional containing the WellKnownTypeInfo, or empty if not a well-known type
     */
    public static Optional<WellKnownTypeInfo> fromProtoType(String protoType) {
        return Optional.ofNullable(BY_PROTO_TYPE.get(protoType));
    }

    /**
     * Check if this is a wrapper type (StringValue, Int32Value, etc.).
     *
     * @return true if this is a wrapper type
     */
    public boolean isWrapperType() {
        return switch (this) {
            case STRING_VALUE, INT32_VALUE, INT64_VALUE, UINT32_VALUE, UINT64_VALUE,
                 BOOL_VALUE, FLOAT_VALUE, DOUBLE_VALUE, BYTES_VALUE -> true;
            default -> false;
        };
    }

    /**
     * Check if this is a temporal type (Timestamp or Duration).
     *
     * @return true if this is a temporal type
     */
    public boolean isTemporalType() {
        return this == TIMESTAMP || this == DURATION;
    }

    /**
     * Check if this is a struct type (Struct, Value, or ListValue).
     *
     * @return true if this is a struct type requiring StructConverter
     */
    public boolean isStructType() {
        return this == STRUCT || this == VALUE || this == LIST_VALUE;
    }

    /**
     * Check if this type requires a utility class for conversion.
     * <p>Struct, Value, and ListValue require the StructConverter utility class.</p>
     *
     * @return true if this type requires StructConverter
     */
    public boolean requiresUtilityClass() {
        return false;
    }

    /**
     * Check if any of the types in the schema require the StructConverter utility class.
     *
     * @return true if StructConverter should be generated
     */
    public static boolean anyRequiresUtilityClass(java.util.Collection<WellKnownTypeInfo> types) {
        return types.stream().anyMatch(WellKnownTypeInfo::requiresUtilityClass);
    }
}
