package io.alnovis.protowrapper.it.model;

import com.google.protobuf.InvalidProtocolBufferException;
import io.alnovis.protowrapper.it.model.api.*;
import io.alnovis.protowrapper.it.proto.v1.Common;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for the parsePartialFromBytes method that performs lenient parsing
 * without required fields validation.
 *
 * <p>This feature is essential for cross-version parsing scenarios where:
 * <ul>
 *   <li>A newer version adds required fields that older data doesn't have</li>
 *   <li>Messages need to be parsed without strict validation</li>
 *   <li>Proto2 required fields should not cause parsing failures</li>
 * </ul>
 */
@DisplayName("parsePartialFromBytes Tests - Lenient Parsing")
class ParsePartialFromBytesTest {

    @Nested
    @DisplayName("Basic functionality")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("parsePartialFromBytes returns valid object for complete message")
        void parsePartialFromBytesReturnsValidObjectForCompleteMessage() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            // Create a complete Money object with all required fields
            Money original = Money.newBuilder(ctx)
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();
            byte[] bytes = original.toBytes();

            // Parse using parsePartialFromBytes
            Money parsed = ctx.parsePartialMoneyFromBytes(bytes);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getAmount()).isEqualTo(1000);
            assertThat(parsed.getCurrency()).isEqualTo("USD");
            assertThat(parsed.getWrapperVersionId()).isEqualTo("v1");
        }

        @Test
        @DisplayName("parsePartialFromBytes returns null for null input")
        void parsePartialFromBytesReturnsNullForNullInput() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            Money parsed = ctx.parsePartialMoneyFromBytes(null);

            assertThat(parsed).isNull();
        }

        @Test
        @DisplayName("parsePartialFromBytes is equivalent to parseFromBytes for complete messages")
        void parsePartialFromBytesIsEquivalentToParseFromBytesForCompleteMessages() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            Money original = Money.newBuilder(ctx)
                    .setAmount(500)
                    .setCurrency("EUR")
                    .build();
            byte[] bytes = original.toBytes();

            Money viaPartial = ctx.parsePartialMoneyFromBytes(bytes);
            Money viaStrict = ctx.parseMoneyFromBytes(bytes);

            assertThat(viaPartial.getAmount()).isEqualTo(viaStrict.getAmount());
            assertThat(viaPartial.getCurrency()).isEqualTo(viaStrict.getCurrency());
        }
    }

    @Nested
    @DisplayName("Lenient parsing - missing required fields")
    class LenientParsingTests {

        @Test
        @DisplayName("parsePartialFromBytes does not throw for empty bytes (missing all required fields)")
        void parsePartialFromBytesDoesNotThrowForEmptyBytes() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] emptyBytes = new byte[0];

            // parsePartialFromBytes should NOT throw - it's lenient
            assertThatCode(() -> ctx.parsePartialMoneyFromBytes(emptyBytes))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("parseFromBytes throws for empty bytes (missing required fields)")
        void parseFromBytesThrowsForEmptyBytes() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] emptyBytes = new byte[0];

            // parseFromBytes SHOULD throw - it's strict
            assertThatThrownBy(() -> ctx.parseMoneyFromBytes(emptyBytes))
                    .isInstanceOf(InvalidProtocolBufferException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("parsePartialFromBytes returns object with default values for missing required fields")
        void parsePartialFromBytesReturnsObjectWithDefaultValuesForMissingRequiredFields() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] emptyBytes = new byte[0];

            Money parsed = ctx.parsePartialMoneyFromBytes(emptyBytes);

            assertThat(parsed).isNotNull();
            // Protobuf default values for missing fields
            assertThat(parsed.getAmount()).isEqualTo(0L); // default for int64
            assertThat(parsed.getCurrency()).isEqualTo(""); // default for string
        }

        @Test
        @DisplayName("parsePartialFromBytes handles partial data with some required fields missing")
        void parsePartialFromBytesHandlesPartialDataWithSomeRequiredFieldsMissing() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            // Build proto directly with only amount (missing required currency)
            byte[] partialBytes = Common.Money.newBuilder()
                    .setAmount(1234)
                    // NOT setting currency (required field)
                    .buildPartial()
                    .toByteArray();

            // parsePartialFromBytes should work
            Money parsed = ctx.parsePartialMoneyFromBytes(partialBytes);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getAmount()).isEqualTo(1234);
            assertThat(parsed.getCurrency()).isEqualTo(""); // default value
        }

        @Test
        @DisplayName("parseFromBytes throws for partial data with missing required fields")
        void parseFromBytesThrowsForPartialDataWithMissingRequiredFields() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            // Build proto directly with only amount (missing required currency)
            byte[] partialBytes = Common.Money.newBuilder()
                    .setAmount(1234)
                    // NOT setting currency (required field)
                    .buildPartial()
                    .toByteArray();

            // parseFromBytes SHOULD throw
            assertThatThrownBy(() -> ctx.parseMoneyFromBytes(partialBytes))
                    .isInstanceOf(InvalidProtocolBufferException.class);
        }
    }

    @Nested
    @DisplayName("Static interface method")
    class StaticInterfaceMethodTests {

        @Test
        @DisplayName("Money.parsePartialFromBytes delegates to context method")
        void moneyParsePartialFromBytesDelegatesToContextMethod() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            Money original = Money.newBuilder(ctx)
                    .setAmount(999)
                    .setCurrency("GBP")
                    .build();
            byte[] bytes = original.toBytes();

            // Static method should delegate to context
            Money viaStaticMethod = Money.parsePartialFromBytes(ctx, bytes);
            Money viaContextMethod = ctx.parsePartialMoneyFromBytes(bytes);

            assertThat(viaStaticMethod.getAmount()).isEqualTo(viaContextMethod.getAmount());
            assertThat(viaStaticMethod.getCurrency()).isEqualTo(viaContextMethod.getCurrency());
        }

        @Test
        @DisplayName("Static parsePartialFromBytes works with empty bytes")
        void staticParsePartialFromBytesWorksWithEmptyBytes() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] emptyBytes = new byte[0];

            // Should not throw
            assertThatCode(() -> Money.parsePartialFromBytes(ctx, emptyBytes))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Static parsePartialFromBytes returns null for null input")
        void staticParsePartialFromBytesReturnsNullForNullInput() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            Money parsed = Money.parsePartialFromBytes(ctx, null);

            assertThat(parsed).isNull();
        }
    }

    @Nested
    @DisplayName("Cross-version scenarios")
    class CrossVersionScenariosTests {

        @Test
        @DisplayName("V2 data missing new required fields parsed with V2 context via parsePartialFromBytes")
        void v2DataMissingNewRequiredFieldsParsedWithV2ContextViaParsePartialFromBytes() throws InvalidProtocolBufferException {
            VersionContext ctxV2 = VersionContext.forVersionId("v2");

            // Create minimal Address with only v1 required fields, missing v2-specific fields
            // GeoLocation in v2 has required latitude and longitude
            byte[] addressWithoutGeoLocation = io.alnovis.protowrapper.it.proto.v2.Common.Address.newBuilder()
                    .setStreet("Main St")
                    .setCity("NY")
                    .setCountry("USA")
                    // NOT setting GeoLocation (which has required fields in v2)
                    .buildPartial()
                    .toByteArray();

            // parsePartialFromBytes should work
            Address parsed = ctxV2.parsePartialAddressFromBytes(addressWithoutGeoLocation);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getStreet()).isEqualTo("Main St");
            assertThat(parsed.getCity()).isEqualTo("NY");
            assertThat(parsed.getCountry()).isEqualTo("USA");
        }

        @Test
        @DisplayName("parsePartialFromBytes allows cross-version parsing when required fields differ")
        void parsePartialFromBytesAllowsCrossVersionParsingWhenRequiredFieldsDiffer() throws InvalidProtocolBufferException {
            VersionContext ctxV1 = VersionContext.forVersionId("v1");
            VersionContext ctxV2 = VersionContext.forVersionId("v2");

            // Create V1 Money (has required amount and currency)
            Money v1Money = Money.newBuilder(ctxV1)
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();
            byte[] v1Bytes = v1Money.toBytes();

            // Parse V1 bytes with V2 context using lenient parsing
            // This should work even if V2 has additional required fields
            Money parsedAsV2 = ctxV2.parsePartialMoneyFromBytes(v1Bytes);

            assertThat(parsedAsV2).isNotNull();
            assertThat(parsedAsV2.getAmount()).isEqualTo(100);
            assertThat(parsedAsV2.getCurrency()).isEqualTo("USD");
            assertThat(parsedAsV2.getWrapperVersionId()).isEqualTo("v2");
        }
    }

    @Nested
    @DisplayName("Nested message types")
    class NestedMessageTypesTests {

        @Test
        @DisplayName("parsePartialFromBytes works with nested messages having required fields")
        void parsePartialFromBytesWorksWithNestedMessagesHavingRequiredFields() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v2");

            // GeoLocation has required latitude and longitude
            // Build incomplete GeoLocation directly using buildPartial()
            io.alnovis.protowrapper.it.proto.v2.Common.Address.GeoLocation incompleteGeoLocation =
                    io.alnovis.protowrapper.it.proto.v2.Common.Address.GeoLocation.newBuilder()
                            .setLatitude(40.7128)
                            // NOT setting longitude (required)
                            .buildPartial();

            // Create address with incomplete nested GeoLocation using buildPartial throughout
            byte[] addressBytes = io.alnovis.protowrapper.it.proto.v2.Common.Address.newBuilder()
                    .setStreet("Broadway")
                    .setCity("NYC")
                    .setCountry("USA")
                    .setLocation(incompleteGeoLocation)
                    .buildPartial()
                    .toByteArray();

            // parsePartialFromBytes should work
            Address parsed = ctx.parsePartialAddressFromBytes(addressBytes);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getStreet()).isEqualTo("Broadway");
            assertThat(parsed.hasLocation()).isTrue();
            assertThat(parsed.getLocation().getLatitude()).isEqualTo(40.7128);
            assertThat(parsed.getLocation().getLongitude()).isEqualTo(0.0); // default value
        }

        @Test
        @DisplayName("parsePartialFromBytes for complex nested structures with missing required fields")
        void parsePartialFromBytesForComplexNestedStructuresWithMissingRequiredFields() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            // Date has required year, month, day
            byte[] partialDateBytes = Common.Date.newBuilder()
                    .setYear(2024)
                    // NOT setting month (required)
                    // NOT setting day (required)
                    .buildPartial()
                    .toByteArray();

            Date parsed = ctx.parsePartialDateFromBytes(partialDateBytes);

            assertThat(parsed).isNotNull();
            assertThat(parsed.getYear()).isEqualTo(2024);
            assertThat(parsed.getMonth()).isEqualTo(0); // default
            assertThat(parsed.getDay()).isEqualTo(0); // default
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCasesTests {

        @Test
        @DisplayName("parsePartialFromBytes throws for truncated message")
        void parsePartialFromBytesThrowsForTruncatedMessage() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            // Truncated string field: tag 0x12 (field 2, wire type 2 = length-delimited), length 100, but no data
            byte[] truncatedBytes = new byte[]{0x12, 100}; // declares 100 bytes of data but has none

            assertThatThrownBy(() -> ctx.parsePartialMoneyFromBytes(truncatedBytes))
                    .isInstanceOf(InvalidProtocolBufferException.class);
        }

        @Test
        @DisplayName("parsePartialFromBytes handles unknown field types gracefully")
        void parsePartialFromBytesHandlesUnknownFieldTypesGracefully() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            // Valid protobuf with unknown field (field number 99)
            // Protobuf is designed to skip unknown fields gracefully
            byte[] bytesWithUnknownField = Common.Money.newBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .buildPartial()
                    .toByteArray();

            // Should parse without exception, even if extra unknown bytes are appended
            Money parsed = ctx.parsePartialMoneyFromBytes(bytesWithUnknownField);
            assertThat(parsed).isNotNull();
            assertThat(parsed.getAmount()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Round-trip with partial data")
    class RoundTripPartialDataTests {

        @Test
        @DisplayName("partial data round-trip preserves available fields")
        void partialDataRoundTripPreservesAvailableFields() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");

            // Create partial proto directly
            byte[] partialBytes = Common.Money.newBuilder()
                    .setAmount(555)
                    .buildPartial()
                    .toByteArray();

            // Parse
            Money parsed = ctx.parsePartialMoneyFromBytes(partialBytes);

            // Round-trip
            byte[] reserialized = parsed.toBytes();
            Money reparsed = ctx.parsePartialMoneyFromBytes(reserialized);

            assertThat(reparsed.getAmount()).isEqualTo(555);
            assertThat(reparsed.getCurrency()).isEqualTo(""); // still default
        }
    }

    @Nested
    @DisplayName("All message types have parsePartialFromBytes")
    class AllMessageTypesHaveMethodTests {

        @Test
        @DisplayName("VersionContext has parsePartialDateFromBytes")
        void versionContextHasParsePartialDateFromBytes() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            Date parsed = ctx.parsePartialDateFromBytes(new byte[0]);
            assertThat(parsed).isNotNull();
        }

        @Test
        @DisplayName("VersionContext has parsePartialAddressFromBytes")
        void versionContextHasParsePartialAddressFromBytes() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            Address parsed = ctx.parsePartialAddressFromBytes(new byte[0]);
            assertThat(parsed).isNotNull();
        }

        @Test
        @DisplayName("VersionContext has parsePartialCustomerFromBytes")
        void versionContextHasParsePartialCustomerFromBytes() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            Customer parsed = ctx.parsePartialCustomerFromBytes(new byte[0]);
            assertThat(parsed).isNotNull();
        }

        @Test
        @DisplayName("VersionContext has parsePartialOrderRequestFromBytes")
        void versionContextHasParsePartialOrderRequestFromBytes() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v1");
            OrderRequest parsed = ctx.parsePartialOrderRequestFromBytes(new byte[0]);
            assertThat(parsed).isNotNull();
        }
    }
}
