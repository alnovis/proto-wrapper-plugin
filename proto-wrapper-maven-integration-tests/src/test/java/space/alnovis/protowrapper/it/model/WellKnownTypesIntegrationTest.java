package space.alnovis.protowrapper.it.model;

import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.*;
import space.alnovis.protowrapper.it.proto.v1.Wellknown;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for Well-Known Types (WKT) support.
 *
 * <p>Tests automatic conversion of Google protobuf well-known types to idiomatic Java types:</p>
 * <ul>
 *   <li>Timestamp -> java.time.Instant</li>
 *   <li>Duration -> java.time.Duration</li>
 *   <li>StringValue, Int32Value, etc. -> String, Integer, etc.</li>
 *   <li>FieldMask -> List&lt;String&gt;</li>
 *   <li>Struct -> Map&lt;String, Object&gt;</li>
 *   <li>Value -> Object</li>
 *   <li>ListValue -> List&lt;Object&gt;</li>
 * </ul>
 *
 * @since 1.3.0
 */
@DisplayName("Well-Known Types Integration Tests")
class WellKnownTypesIntegrationTest {

    // ==================== Timestamp and Duration ====================

    @Nested
    @DisplayName("Timestamp and Duration Tests")
    class TimestampDurationTests {

        @Test
        @DisplayName("V1 reads Timestamp as java.time.Instant")
        void v1ReadsTimestampAsInstant() {
            Wellknown.TimestampMessage proto = Wellknown.TimestampMessage.newBuilder()
                    .setId("TS-001")
                    .setCreatedAt(Timestamp.newBuilder()
                            .setSeconds(1704067200L)  // 2024-01-01 00:00:00 UTC
                            .setNanos(123456789)
                            .build())
                    .build();

            TimestampMessage msg = new space.alnovis.protowrapper.it.model.v1.TimestampMessage(proto);

            assertThat(msg.getId()).isEqualTo("TS-001");
            assertThat(msg.hasCreatedAt()).isTrue();
            assertThat(msg.getCreatedAt()).isNotNull();
            assertThat(msg.getCreatedAt().getEpochSecond()).isEqualTo(1704067200L);
            assertThat(msg.getCreatedAt().getNano()).isEqualTo(123456789);
            assertThat(msg.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V2 reads Timestamp as java.time.Instant with additional field")
        void v2ReadsTimestampAsInstant() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.TimestampMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.TimestampMessage.newBuilder()
                            .setId("TS-002")
                            .setCreatedAt(Timestamp.newBuilder()
                                    .setSeconds(1704067200L)
                                    .setNanos(0)
                                    .build())
                            .setDeletedAt(Timestamp.newBuilder()
                                    .setSeconds(1704153600L)  // 2024-01-02 00:00:00 UTC
                                    .setNanos(500000000)
                                    .build())
                            .build();

            TimestampMessage msg = new space.alnovis.protowrapper.it.model.v2.TimestampMessage(proto);

            assertThat(msg.hasDeletedAt()).isTrue();
            assertThat(msg.supportsDeletedAt()).isTrue();
            assertThat(msg.getDeletedAt().getEpochSecond()).isEqualTo(1704153600L);
            assertThat(msg.getDeletedAt().getNano()).isEqualTo(500000000);
        }

        @Test
        @DisplayName("V1 does not support deletedAt field")
        void v1DoesNotSupportDeletedAt() {
            Wellknown.TimestampMessage proto = Wellknown.TimestampMessage.newBuilder()
                    .setId("TS-003")
                    .build();

            TimestampMessage msg = new space.alnovis.protowrapper.it.model.v1.TimestampMessage(proto);

            assertThat(msg.supportsDeletedAt()).isFalse();
            assertThat(msg.hasDeletedAt()).isFalse();
            assertThat(msg.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("Duration is converted to java.time.Duration")
        void durationConvertedToJavaDuration() {
            Wellknown.TimestampMessage proto = Wellknown.TimestampMessage.newBuilder()
                    .setId("TS-004")
                    .setProcessingTime(Duration.newBuilder()
                            .setSeconds(3600)
                            .setNanos(500000000)
                            .build())
                    .setTimeout(Duration.newBuilder()
                            .setSeconds(30)
                            .setNanos(0)
                            .build())
                    .build();

            TimestampMessage msg = new space.alnovis.protowrapper.it.model.v1.TimestampMessage(proto);

            assertThat(msg.hasProcessingTime()).isTrue();
            assertThat(msg.getProcessingTime().getSeconds()).isEqualTo(3600);
            assertThat(msg.getProcessingTime().getNano()).isEqualTo(500000000);

            assertThat(msg.hasTimeout()).isTrue();
            assertThat(msg.getTimeout().getSeconds()).isEqualTo(30);
        }

        @Test
        @DisplayName("Builder sets Timestamp from java.time.Instant")
        void builderSetsTimestampFromInstant() {
            Instant createdAt = Instant.parse("2024-06-15T10:30:00Z");
            java.time.Duration processingTime = java.time.Duration.ofMinutes(5);

            TimestampMessage msg = space.alnovis.protowrapper.it.model.v1.TimestampMessage.newBuilder()
                    .setId("TS-005")
                    .setCreatedAt(createdAt)
                    .setProcessingTime(processingTime)
                    .build();

            assertThat(msg.getId()).isEqualTo("TS-005");
            assertThat(msg.getCreatedAt()).isEqualTo(createdAt);
            assertThat(msg.getProcessingTime()).isEqualTo(processingTime);
        }

        @Test
        @DisplayName("Null Timestamp returns null Instant")
        void nullTimestampReturnsNull() {
            Wellknown.TimestampMessage proto = Wellknown.TimestampMessage.newBuilder()
                    .setId("TS-006")
                    .build();

            TimestampMessage msg = new space.alnovis.protowrapper.it.model.v1.TimestampMessage(proto);

            assertThat(msg.hasCreatedAt()).isFalse();
            assertThat(msg.getCreatedAt()).isNull();
            assertThat(msg.hasUpdatedAt()).isFalse();
            assertThat(msg.getUpdatedAt()).isNull();
        }
    }

    // ==================== Wrapper Types ====================

    @Nested
    @DisplayName("Wrapper Types Tests")
    class WrapperTypesTests {

        @Test
        @DisplayName("StringValue is converted to String")
        void stringValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-001")
                    .setName(com.google.protobuf.StringValue.of("John Doe"))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasName()).isTrue();
            assertThat(msg.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Int32Value is converted to Integer")
        void int32ValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-002")
                    .setAge(com.google.protobuf.Int32Value.of(25))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasAge()).isTrue();
            assertThat(msg.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("Int64Value is converted to Long")
        void int64ValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-003")
                    .setBalance(com.google.protobuf.Int64Value.of(1_000_000_000_000L))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasBalance()).isTrue();
            assertThat(msg.getBalance()).isEqualTo(1_000_000_000_000L);
        }

        @Test
        @DisplayName("BoolValue is converted to Boolean")
        void boolValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-004")
                    .setActive(com.google.protobuf.BoolValue.of(true))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasActive()).isTrue();
            assertThat(msg.getActive()).isTrue();
        }

        @Test
        @DisplayName("FloatValue is converted to Float")
        void floatValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-005")
                    .setRating(com.google.protobuf.FloatValue.of(4.5f))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasRating()).isTrue();
            assertThat(msg.getRating()).isCloseTo(4.5f, within(0.001f));
        }

        @Test
        @DisplayName("DoubleValue is converted to Double")
        void doubleValueConverted() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-006")
                    .setPrice(com.google.protobuf.DoubleValue.of(99.99))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasPrice()).isTrue();
            assertThat(msg.getPrice()).isCloseTo(99.99, within(0.001));
        }

        @Test
        @DisplayName("BytesValue is converted to byte[]")
        void bytesValueConverted() {
            byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-007")
                    .setData(com.google.protobuf.BytesValue.of(ByteString.copyFrom(data)))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasData()).isTrue();
            assertThat(msg.getData()).isEqualTo(data);
        }

        @Test
        @DisplayName("UInt32Value is converted to Long (unsigned)")
        void uint32ValueConverted() {
            // 0xFFFFFFFF in unsigned is 4294967295
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-008")
                    .setCounter(com.google.protobuf.UInt32Value.of(-1))  // 0xFFFFFFFF
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasCounter()).isTrue();
            assertThat(msg.getCounter()).isEqualTo(4294967295L);
        }

        @Test
        @DisplayName("UInt64Value is converted to Long")
        void uint64ValueConverted() {
            // Large unsigned value
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-008a")
                    .setBigCounter(com.google.protobuf.UInt64Value.of(9_000_000_000_000_000_000L))
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasBigCounter()).isTrue();
            assertThat(msg.getBigCounter()).isEqualTo(9_000_000_000_000_000_000L);
        }

        @Test
        @DisplayName("Missing wrapper values return null")
        void missingWrapperValuesReturnNull() {
            Wellknown.WrapperTypesMessage proto = Wellknown.WrapperTypesMessage.newBuilder()
                    .setId("WT-009")
                    .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage(proto);

            assertThat(msg.hasName()).isFalse();
            assertThat(msg.getName()).isNull();
            assertThat(msg.hasAge()).isFalse();
            assertThat(msg.getAge()).isNull();
            assertThat(msg.hasActive()).isFalse();
            assertThat(msg.getActive()).isNull();
        }

        @Test
        @DisplayName("Builder sets wrapper types from Java primitives")
        void builderSetsWrapperTypes() {
            WrapperTypesMessage msg = space.alnovis.protowrapper.it.model.v1.WrapperTypesMessage.newBuilder()
                    .setId("WT-010")
                    .setName("Jane Doe")
                    .setAge(30)
                    .setBalance(500_000L)
                    .setActive(false)
                    .setRating(4.8f)
                    .setPrice(149.99)
                    .build();

            assertThat(msg.getName()).isEqualTo("Jane Doe");
            assertThat(msg.getAge()).isEqualTo(30);
            assertThat(msg.getBalance()).isEqualTo(500_000L);
            assertThat(msg.getActive()).isFalse();
            assertThat(msg.getRating()).isCloseTo(4.8f, within(0.001f));
            assertThat(msg.getPrice()).isCloseTo(149.99, within(0.001));
        }

        @Test
        @DisplayName("V2 has additional wrapper fields")
        void v2HasAdditionalWrapperFields() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.WrapperTypesMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.WrapperTypesMessage.newBuilder()
                            .setId("WT-011")
                            .setDescription(com.google.protobuf.StringValue.of("New in V2"))
                            .setVerified(com.google.protobuf.BoolValue.of(true))
                            .build();

            WrapperTypesMessage msg = new space.alnovis.protowrapper.it.model.v2.WrapperTypesMessage(proto);

            assertThat(msg.supportsDescription()).isTrue();
            assertThat(msg.getDescription()).isEqualTo("New in V2");
            assertThat(msg.supportsVerified()).isTrue();
            assertThat(msg.getVerified()).isTrue();
        }
    }

    // ==================== FieldMask ====================

    @Nested
    @DisplayName("FieldMask Tests")
    class FieldMaskTests {

        @Test
        @DisplayName("FieldMask is converted to List<String>")
        void fieldMaskConvertedToList() {
            Wellknown.FieldMaskMessage proto = Wellknown.FieldMaskMessage.newBuilder()
                    .setResourceName("projects/123/resources/456")
                    .setUpdateMask(com.google.protobuf.FieldMask.newBuilder()
                            .addPaths("name")
                            .addPaths("description")
                            .addPaths("labels")
                            .build())
                    .build();

            FieldMaskMessage msg = new space.alnovis.protowrapper.it.model.v1.FieldMaskMessage(proto);

            assertThat(msg.hasUpdateMask()).isTrue();
            assertThat(msg.getUpdateMask()).containsExactly("name", "description", "labels");
        }

        @Test
        @DisplayName("Empty FieldMask returns empty list")
        void emptyFieldMaskReturnsEmptyList() {
            Wellknown.FieldMaskMessage proto = Wellknown.FieldMaskMessage.newBuilder()
                    .setResourceName("projects/123")
                    .build();

            FieldMaskMessage msg = new space.alnovis.protowrapper.it.model.v1.FieldMaskMessage(proto);

            assertThat(msg.hasUpdateMask()).isFalse();
            assertThat(msg.getUpdateMask()).isEmpty();
        }

        @Test
        @DisplayName("Builder sets FieldMask from List<String>")
        void builderSetsFieldMask() {
            FieldMaskMessage msg = space.alnovis.protowrapper.it.model.v1.FieldMaskMessage.newBuilder()
                    .setResourceName("projects/456")
                    .setUpdateMask(List.of("field1", "field2"))
                    .setReadMask(List.of("field3"))
                    .build();

            assertThat(msg.getUpdateMask()).containsExactly("field1", "field2");
            assertThat(msg.getReadMask()).containsExactly("field3");
        }
    }

    // ==================== Struct, Value, ListValue ====================

    @Nested
    @DisplayName("Struct/Value/ListValue Tests")
    class StructValueTests {

        @Test
        @DisplayName("Struct is converted to Map<String, Object>")
        void structConvertedToMap() {
            Struct metadata = Struct.newBuilder()
                    .putFields("key1", Value.newBuilder().setStringValue("value1").build())
                    .putFields("key2", Value.newBuilder().setNumberValue(42.0).build())
                    .putFields("key3", Value.newBuilder().setBoolValue(true).build())
                    .build();

            Wellknown.StructMessage proto = Wellknown.StructMessage.newBuilder()
                    .setId("ST-001")
                    .setMetadata(metadata)
                    .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v1.StructMessage(proto);

            assertThat(msg.hasMetadata()).isTrue();
            Map<String, Object> map = msg.getMetadata();
            assertThat(map).containsEntry("key1", "value1");
            assertThat(map).containsEntry("key2", 42.0);
            assertThat(map).containsEntry("key3", true);
        }

        @Test
        @DisplayName("Value is converted to Object (various types)")
        void valueConvertedToObject() {
            // String value
            Wellknown.StructMessage protoString = Wellknown.StructMessage.newBuilder()
                    .setId("ST-002")
                    .setDynamicValue(Value.newBuilder().setStringValue("hello").build())
                    .build();
            StructMessage msgString = new space.alnovis.protowrapper.it.model.v1.StructMessage(protoString);
            assertThat(msgString.getDynamicValue()).isEqualTo("hello");

            // Number value
            Wellknown.StructMessage protoNumber = Wellknown.StructMessage.newBuilder()
                    .setId("ST-003")
                    .setDynamicValue(Value.newBuilder().setNumberValue(3.14).build())
                    .build();
            StructMessage msgNumber = new space.alnovis.protowrapper.it.model.v1.StructMessage(protoNumber);
            assertThat(msgNumber.getDynamicValue()).isEqualTo(3.14);

            // Boolean value
            Wellknown.StructMessage protoBool = Wellknown.StructMessage.newBuilder()
                    .setId("ST-004")
                    .setDynamicValue(Value.newBuilder().setBoolValue(false).build())
                    .build();
            StructMessage msgBool = new space.alnovis.protowrapper.it.model.v1.StructMessage(protoBool);
            assertThat(msgBool.getDynamicValue()).isEqualTo(false);

            // Null value
            Wellknown.StructMessage protoNull = Wellknown.StructMessage.newBuilder()
                    .setId("ST-005")
                    .setDynamicValue(Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build())
                    .build();
            StructMessage msgNull = new space.alnovis.protowrapper.it.model.v1.StructMessage(protoNull);
            assertThat(msgNull.getDynamicValue()).isNull();
        }

        @Test
        @DisplayName("ListValue is converted to List<Object>")
        void listValueConvertedToList() {
            ListValue tags = ListValue.newBuilder()
                    .addValues(Value.newBuilder().setStringValue("tag1").build())
                    .addValues(Value.newBuilder().setStringValue("tag2").build())
                    .addValues(Value.newBuilder().setNumberValue(100.0).build())
                    .build();

            Wellknown.StructMessage proto = Wellknown.StructMessage.newBuilder()
                    .setId("ST-006")
                    .setTags(tags)
                    .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v1.StructMessage(proto);

            assertThat(msg.hasTags()).isTrue();
            List<Object> list = msg.getTags();
            assertThat(list).containsExactly("tag1", "tag2", 100.0);
        }

        @Test
        @DisplayName("Nested Struct/List values are properly converted")
        void nestedStructsConverted() {
            Struct nested = Struct.newBuilder()
                    .putFields("nestedKey", Value.newBuilder().setStringValue("nestedValue").build())
                    .build();

            Struct metadata = Struct.newBuilder()
                    .putFields("nested", Value.newBuilder().setStructValue(nested).build())
                    .putFields("array", Value.newBuilder().setListValue(
                            ListValue.newBuilder()
                                    .addValues(Value.newBuilder().setNumberValue(1).build())
                                    .addValues(Value.newBuilder().setNumberValue(2).build())
                                    .build()
                    ).build())
                    .build();

            Wellknown.StructMessage proto = Wellknown.StructMessage.newBuilder()
                    .setId("ST-007")
                    .setMetadata(metadata)
                    .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v1.StructMessage(proto);

            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) msg.getMetadata().get("nested");
            assertThat(nestedMap).containsEntry("nestedKey", "nestedValue");

            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) msg.getMetadata().get("array");
            assertThat(array).containsExactly(1.0, 2.0);
        }

        @Test
        @DisplayName("Empty Struct returns empty Map")
        void emptyStructReturnsEmptyMap() {
            Wellknown.StructMessage proto = Wellknown.StructMessage.newBuilder()
                    .setId("ST-008")
                    .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v1.StructMessage(proto);

            assertThat(msg.hasMetadata()).isFalse();
            assertThat(msg.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("Builder sets Struct from Map")
        void builderSetsStruct() {
            Map<String, Object> metadata = Map.of(
                    "string", "value",
                    "number", 42,
                    "bool", true
            );

            StructMessage msg = space.alnovis.protowrapper.it.model.v1.StructMessage.newBuilder()
                    .setId("ST-009")
                    .setMetadata(metadata)
                    .build();

            assertThat(msg.getMetadata()).containsEntry("string", "value");
            assertThat(msg.getMetadata().get("number")).isEqualTo(42.0);  // Numbers are Double
            assertThat(msg.getMetadata()).containsEntry("bool", true);
        }

        @Test
        @DisplayName("Builder sets ListValue from List")
        void builderSetsListValue() {
            List<Object> tags = List.of("tag1", "tag2", 123, true);

            StructMessage msg = space.alnovis.protowrapper.it.model.v1.StructMessage.newBuilder()
                    .setId("ST-010")
                    .setTags(tags)
                    .build();

            assertThat(msg.getTags()).containsExactly("tag1", "tag2", 123.0, true);
        }

        @Test
        @DisplayName("Builder sets Value from Object")
        void builderSetsValue() {
            StructMessage msg = space.alnovis.protowrapper.it.model.v1.StructMessage.newBuilder()
                    .setId("ST-011")
                    .setDynamicValue("dynamic string")
                    .build();

            assertThat(msg.getDynamicValue()).isEqualTo("dynamic string");
        }

        @Test
        @DisplayName("V2 has additional Struct field (config)")
        void v2HasConfigField() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.StructMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.StructMessage.newBuilder()
                            .setId("ST-012")
                            .setConfig(Struct.newBuilder()
                                    .putFields("timeout", Value.newBuilder().setNumberValue(30).build())
                                    .build())
                            .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v2.StructMessage(proto);

            assertThat(msg.supportsConfig()).isTrue();
            assertThat(msg.hasConfig()).isTrue();
            assertThat(msg.getConfig()).containsEntry("timeout", 30.0);
        }

        @Test
        @DisplayName("V1 does not support config field")
        void v1DoesNotSupportConfig() {
            Wellknown.StructMessage proto = Wellknown.StructMessage.newBuilder()
                    .setId("ST-013")
                    .build();

            StructMessage msg = new space.alnovis.protowrapper.it.model.v1.StructMessage(proto);

            assertThat(msg.supportsConfig()).isFalse();
            assertThat(msg.hasConfig()).isFalse();
            assertThat(msg.getConfig()).isNull();
        }
    }

    // ==================== Repeated WKT ====================

    @Nested
    @DisplayName("Repeated WKT Tests")
    class RepeatedWktTests {

        @Test
        @DisplayName("Repeated Timestamp is converted to List<Instant>")
        void repeatedTimestampConverted() {
            Wellknown.RepeatedWktMessage proto = Wellknown.RepeatedWktMessage.newBuilder()
                    .setId("RW-001")
                    .addTimestamps(Timestamp.newBuilder().setSeconds(1704067200L).setNanos(0).build())
                    .addTimestamps(Timestamp.newBuilder().setSeconds(1704153600L).setNanos(0).build())
                    .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage(proto);

            assertThat(msg.getTimestamps()).hasSize(2);
            assertThat(msg.getTimestamps().get(0).getEpochSecond()).isEqualTo(1704067200L);
            assertThat(msg.getTimestamps().get(1).getEpochSecond()).isEqualTo(1704153600L);
        }

        @Test
        @DisplayName("Repeated Duration is converted to List<Duration>")
        void repeatedDurationConverted() {
            Wellknown.RepeatedWktMessage proto = Wellknown.RepeatedWktMessage.newBuilder()
                    .setId("RW-002")
                    .addDurations(Duration.newBuilder().setSeconds(60).setNanos(0).build())
                    .addDurations(Duration.newBuilder().setSeconds(120).setNanos(500000000).build())
                    .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage(proto);

            assertThat(msg.getDurations()).hasSize(2);
            assertThat(msg.getDurations().get(0)).isEqualTo(java.time.Duration.ofSeconds(60));
            assertThat(msg.getDurations().get(1)).isEqualTo(java.time.Duration.ofSeconds(120, 500000000));
        }

        @Test
        @DisplayName("Repeated StringValue is converted to List<String>")
        void repeatedStringValueConverted() {
            Wellknown.RepeatedWktMessage proto = Wellknown.RepeatedWktMessage.newBuilder()
                    .setId("RW-003")
                    .addNames(com.google.protobuf.StringValue.of("Alice"))
                    .addNames(com.google.protobuf.StringValue.of("Bob"))
                    .addNames(com.google.protobuf.StringValue.of("Charlie"))
                    .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage(proto);

            assertThat(msg.getNames()).containsExactly("Alice", "Bob", "Charlie");
        }

        @Test
        @DisplayName("Repeated Int32Value is converted to List<Integer>")
        void repeatedInt32ValueConverted() {
            Wellknown.RepeatedWktMessage proto = Wellknown.RepeatedWktMessage.newBuilder()
                    .setId("RW-004")
                    .addNumbers(com.google.protobuf.Int32Value.of(10))
                    .addNumbers(com.google.protobuf.Int32Value.of(20))
                    .addNumbers(com.google.protobuf.Int32Value.of(30))
                    .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage(proto);

            assertThat(msg.getNumbers()).containsExactly(10, 20, 30);
        }

        @Test
        @DisplayName("Empty repeated WKT returns empty list")
        void emptyRepeatedReturnsEmptyList() {
            Wellknown.RepeatedWktMessage proto = Wellknown.RepeatedWktMessage.newBuilder()
                    .setId("RW-005")
                    .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage(proto);

            assertThat(msg.getTimestamps()).isEmpty();
            assertThat(msg.getDurations()).isEmpty();
            assertThat(msg.getNames()).isEmpty();
            assertThat(msg.getNumbers()).isEmpty();
        }

        @Test
        @DisplayName("V2 has additional repeated BoolValue field")
        void v2HasAdditionalRepeatedField() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.RepeatedWktMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.RepeatedWktMessage.newBuilder()
                            .setId("RW-006")
                            .addFlags(com.google.protobuf.BoolValue.of(true))
                            .addFlags(com.google.protobuf.BoolValue.of(false))
                            .addFlags(com.google.protobuf.BoolValue.of(true))
                            .build();

            RepeatedWktMessage msg = new space.alnovis.protowrapper.it.model.v2.RepeatedWktMessage(proto);

            assertThat(msg.supportsFlags()).isTrue();
            assertThat(msg.getFlags()).containsExactly(true, false, true);
        }

        @Test
        @DisplayName("Builder adds single Timestamp element")
        void builderAddsSingleTimestamp() {
            Instant ts1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant ts2 = Instant.parse("2024-01-02T00:00:00Z");

            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-007")
                    .addTimestamps(ts1)
                    .addTimestamps(ts2)
                    .build();

            assertThat(msg.getTimestamps()).containsExactly(ts1, ts2);
        }

        @Test
        @DisplayName("Builder adds all Timestamp elements")
        void builderAddsAllTimestamps() {
            List<Instant> timestamps = List.of(
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-02T00:00:00Z"),
                    Instant.parse("2024-01-03T00:00:00Z")
            );

            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-008")
                    .addAllTimestamps(timestamps)
                    .build();

            assertThat(msg.getTimestamps()).containsExactlyElementsOf(timestamps);
        }

        @Test
        @DisplayName("Builder sets (replaces) all Timestamp elements")
        void builderSetsTimestamps() {
            List<Instant> initial = List.of(Instant.parse("2024-01-01T00:00:00Z"));
            List<Instant> replacement = List.of(
                    Instant.parse("2024-06-01T00:00:00Z"),
                    Instant.parse("2024-06-02T00:00:00Z")
            );

            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-009")
                    .addAllTimestamps(initial)
                    .setTimestamps(replacement)
                    .build();

            assertThat(msg.getTimestamps()).containsExactlyElementsOf(replacement);
        }

        @Test
        @DisplayName("Builder clears all Timestamp elements")
        void builderClearsTimestamps() {
            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-010")
                    .addTimestamps(Instant.now())
                    .addTimestamps(Instant.now())
                    .clearTimestamps()
                    .build();

            assertThat(msg.getTimestamps()).isEmpty();
        }

        @Test
        @DisplayName("Builder adds repeated Duration elements")
        void builderAddsDurations() {
            java.time.Duration d1 = java.time.Duration.ofMinutes(5);
            java.time.Duration d2 = java.time.Duration.ofHours(1);

            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-011")
                    .addDurations(d1)
                    .addDurations(d2)
                    .build();

            assertThat(msg.getDurations()).containsExactly(d1, d2);
        }

        @Test
        @DisplayName("Builder adds repeated StringValue elements")
        void builderAddsNames() {
            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-012")
                    .addNames("Alice")
                    .addNames("Bob")
                    .addAllNames(List.of("Charlie", "Diana"))
                    .build();

            assertThat(msg.getNames()).containsExactly("Alice", "Bob", "Charlie", "Diana");
        }

        @Test
        @DisplayName("Builder adds repeated Int32Value elements")
        void builderAddsNumbers() {
            RepeatedWktMessage msg = space.alnovis.protowrapper.it.model.v1.RepeatedWktMessage.newBuilder()
                    .setId("RW-013")
                    .addNumbers(10)
                    .addNumbers(20)
                    .setNumbers(List.of(100, 200, 300))
                    .build();

            assertThat(msg.getNumbers()).containsExactly(100, 200, 300);
        }
    }

    // ==================== Map with WKT Values ====================

    @Nested
    @DisplayName("Map WKT Tests")
    class MapWktTests {

        @Test
        @DisplayName("Map<String, Timestamp> is converted to Map<String, Instant>")
        void mapTimestampConverted() {
            Wellknown.MapWktMessage proto = Wellknown.MapWktMessage.newBuilder()
                    .setId("MW-001")
                    .putTimestampMap("created", Timestamp.newBuilder().setSeconds(1704067200L).build())
                    .putTimestampMap("updated", Timestamp.newBuilder().setSeconds(1704153600L).build())
                    .build();

            MapWktMessage msg = new space.alnovis.protowrapper.it.model.v1.MapWktMessage(proto);

            assertThat(msg.getTimestampMapMap()).hasSize(2);
            assertThat(msg.getTimestampMapMap().get("created").getEpochSecond()).isEqualTo(1704067200L);
            assertThat(msg.getTimestampMapMap().get("updated").getEpochSecond()).isEqualTo(1704153600L);
        }

        @Test
        @DisplayName("Map<String, StringValue> is converted to Map<String, String>")
        void mapStringValueConverted() {
            Wellknown.MapWktMessage proto = Wellknown.MapWktMessage.newBuilder()
                    .setId("MW-002")
                    .putStringMap("key1", com.google.protobuf.StringValue.of("value1"))
                    .putStringMap("key2", com.google.protobuf.StringValue.of("value2"))
                    .build();

            MapWktMessage msg = new space.alnovis.protowrapper.it.model.v1.MapWktMessage(proto);

            assertThat(msg.getStringMapMap()).containsEntry("key1", "value1");
            assertThat(msg.getStringMapMap()).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("Map<int32, Int64Value> is converted to Map<Integer, Long>")
        void mapInt64ValueConverted() {
            Wellknown.MapWktMessage proto = Wellknown.MapWktMessage.newBuilder()
                    .setId("MW-003")
                    .putNumberMap(1, com.google.protobuf.Int64Value.of(1000L))
                    .putNumberMap(2, com.google.protobuf.Int64Value.of(2000L))
                    .build();

            MapWktMessage msg = new space.alnovis.protowrapper.it.model.v1.MapWktMessage(proto);

            assertThat(msg.getNumberMapMap()).containsEntry(1, 1000L);
            assertThat(msg.getNumberMapMap()).containsEntry(2, 2000L);
        }

        @Test
        @DisplayName("Empty map returns empty Map")
        void emptyMapReturnsEmptyMap() {
            Wellknown.MapWktMessage proto = Wellknown.MapWktMessage.newBuilder()
                    .setId("MW-004")
                    .build();

            MapWktMessage msg = new space.alnovis.protowrapper.it.model.v1.MapWktMessage(proto);

            assertThat(msg.getTimestampMapMap()).isEmpty();
            assertThat(msg.getStringMapMap()).isEmpty();
            assertThat(msg.getNumberMapMap()).isEmpty();
        }

        @Test
        @DisplayName("V2 has additional Duration map")
        void v2HasDurationMap() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.MapWktMessage proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.MapWktMessage.newBuilder()
                            .setId("MW-005")
                            .putDurationMap("short", Duration.newBuilder().setSeconds(30).build())
                            .putDurationMap("long", Duration.newBuilder().setSeconds(3600).build())
                            .build();

            MapWktMessage msg = new space.alnovis.protowrapper.it.model.v2.MapWktMessage(proto);

            assertThat(msg.supportsDurationMap()).isTrue();
            assertThat(msg.getDurationMapMap()).hasSize(2);
            assertThat(msg.getDurationMapMap().get("short")).isEqualTo(java.time.Duration.ofSeconds(30));
            assertThat(msg.getDurationMapMap().get("long")).isEqualTo(java.time.Duration.ofSeconds(3600));
        }

        @Test
        @DisplayName("Builder puts single Timestamp map entry")
        void builderPutsTimestampEntry() {
            Instant ts1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant ts2 = Instant.parse("2024-01-02T00:00:00Z");

            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-006")
                    .putTimestampMap("first", ts1)
                    .putTimestampMap("second", ts2)
                    .build();

            assertThat(msg.getTimestampMapMap()).hasSize(2);
            assertThat(msg.getTimestampMapMap().get("first")).isEqualTo(ts1);
            assertThat(msg.getTimestampMapMap().get("second")).isEqualTo(ts2);
        }

        @Test
        @DisplayName("Builder puts all Timestamp map entries")
        void builderPutsAllTimestampEntries() {
            Map<String, Instant> entries = Map.of(
                    "a", Instant.parse("2024-01-01T00:00:00Z"),
                    "b", Instant.parse("2024-01-02T00:00:00Z"),
                    "c", Instant.parse("2024-01-03T00:00:00Z")
            );

            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-007")
                    .putAllTimestampMap(entries)
                    .build();

            assertThat(msg.getTimestampMapMap()).hasSize(3);
            assertThat(msg.getTimestampMapMap()).containsAllEntriesOf(entries);
        }

        @Test
        @DisplayName("Builder removes Timestamp map entry")
        void builderRemovesTimestampEntry() {
            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-008")
                    .putTimestampMap("keep", Instant.parse("2024-01-01T00:00:00Z"))
                    .putTimestampMap("remove", Instant.parse("2024-01-02T00:00:00Z"))
                    .removeTimestampMap("remove")
                    .build();

            assertThat(msg.getTimestampMapMap()).hasSize(1);
            assertThat(msg.getTimestampMapMap()).containsKey("keep");
            assertThat(msg.getTimestampMapMap()).doesNotContainKey("remove");
        }

        @Test
        @DisplayName("Builder clears Timestamp map")
        void builderClearsTimestampMap() {
            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-009")
                    .putTimestampMap("a", Instant.now())
                    .putTimestampMap("b", Instant.now())
                    .clearTimestampMap()
                    .build();

            assertThat(msg.getTimestampMapMap()).isEmpty();
        }

        @Test
        @DisplayName("Builder puts StringValue map entries")
        void builderPutsStringMapEntries() {
            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-010")
                    .putStringMap("key1", "value1")
                    .putStringMap("key2", "value2")
                    .putAllStringMap(Map.of("key3", "value3", "key4", "value4"))
                    .build();

            assertThat(msg.getStringMapMap()).hasSize(4);
            assertThat(msg.getStringMapMap()).containsEntry("key1", "value1");
            assertThat(msg.getStringMapMap()).containsEntry("key3", "value3");
        }

        @Test
        @DisplayName("Builder puts Int64Value map entries")
        void builderPutsNumberMapEntries() {
            MapWktMessage msg = space.alnovis.protowrapper.it.model.v1.MapWktMessage.newBuilder()
                    .setId("MW-011")
                    .putNumberMap(1, 1000L)
                    .putNumberMap(2, 2000L)
                    .build();

            assertThat(msg.getNumberMapMap()).hasSize(2);
            assertThat(msg.getNumberMapMap()).containsEntry(1, 1000L);
            assertThat(msg.getNumberMapMap()).containsEntry(2, 2000L);
        }
    }

    // ==================== Cross-Version Tests ====================

    @Nested
    @DisplayName("Cross-Version Conversion Tests")
    class CrossVersionTests {

        @Test
        @DisplayName("V1 to V2 conversion preserves WKT data")
        void v1ToV2ConversionPreservesData() {
            Wellknown.TimestampMessage v1Proto = Wellknown.TimestampMessage.newBuilder()
                    .setId("CV-001")
                    .setCreatedAt(Timestamp.newBuilder().setSeconds(1704067200L).setNanos(123456).build())
                    .setProcessingTime(Duration.newBuilder().setSeconds(60).build())
                    .build();

            TimestampMessage v1 = new space.alnovis.protowrapper.it.model.v1.TimestampMessage(v1Proto);
            TimestampMessage v2 = v1.asVersion(space.alnovis.protowrapper.it.model.v2.TimestampMessage.class);

            assertThat(v2.getWrapperVersion()).isEqualTo(2);
            assertThat(v2.getCreatedAt().getEpochSecond()).isEqualTo(1704067200L);
            assertThat(v2.getCreatedAt().getNano()).isEqualTo(123456);
            assertThat(v2.getProcessingTime().getSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("V2 to V1 conversion preserves common WKT data")
        void v2ToV1ConversionPreservesCommonData() {
            space.alnovis.protowrapper.it.proto.v2.Wellknown.TimestampMessage v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.Wellknown.TimestampMessage.newBuilder()
                            .setId("CV-002")
                            .setCreatedAt(Timestamp.newBuilder().setSeconds(1704067200L).build())
                            .setDeletedAt(Timestamp.newBuilder().setSeconds(1704153600L).build())
                            .build();

            TimestampMessage v2 = new space.alnovis.protowrapper.it.model.v2.TimestampMessage(v2Proto);
            TimestampMessage v1 = v2.asVersion(space.alnovis.protowrapper.it.model.v1.TimestampMessage.class);

            assertThat(v1.getWrapperVersion()).isEqualTo(1);
            assertThat(v1.getCreatedAt().getEpochSecond()).isEqualTo(1704067200L);
            // deletedAt is not accessible in V1 but data is preserved
            assertThat(v1.supportsDeletedAt()).isFalse();
        }

        @Test
        @DisplayName("Round-trip conversion preserves all data")
        void roundTripPreservesData() {
            Instant now = Instant.now();
            java.time.Duration timeout = java.time.Duration.ofMinutes(5);

            TimestampMessage original = space.alnovis.protowrapper.it.model.v1.TimestampMessage.newBuilder()
                    .setId("CV-003")
                    .setCreatedAt(now)
                    .setTimeout(timeout)
                    .build();

            // V1 -> V2 -> V1
            TimestampMessage v2 = original.asVersion(space.alnovis.protowrapper.it.model.v2.TimestampMessage.class);
            TimestampMessage v1Again = v2.asVersion(space.alnovis.protowrapper.it.model.v1.TimestampMessage.class);

            assertThat(v1Again.getId()).isEqualTo("CV-003");
            assertThat(v1Again.getCreatedAt()).isEqualTo(now);
            assertThat(v1Again.getTimeout()).isEqualTo(timeout);
        }
    }
}
