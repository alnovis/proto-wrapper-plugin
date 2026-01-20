package io.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.alnovis.protowrapper.it.model.api.Money;
import io.alnovis.protowrapper.it.model.api.VersionContext;
import io.alnovis.protowrapper.it.model.api.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for version conversion via asVersion() method.
 *
 * The asVersion() method allows converting between version-specific implementations
 * by serializing to bytes and parsing in the target version's context.
 */
@DisplayName("Version Conversion (asVersion)")
public class VersionConversionTest {

    @Nested
    @DisplayName("Money conversion")
    class MoneyConversion {

        @Test
        @DisplayName("Convert Money from v1 to v2")
        void convertMoneyV1ToV2() {
            // Create Money in v1
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            Money v1 = ctx1.newMoneyBuilder()
                    .setAmount(1000L)
                    .setCurrency("USD")
                    .build();

            assertEquals("v1", v1.getWrapperVersionId());
            assertEquals(1000L, v1.getAmount());
            assertEquals("USD", v1.getCurrency());

            // Convert to v2
            io.alnovis.protowrapper.it.model.v2.Money v2 =
                    v1.asVersion(io.alnovis.protowrapper.it.model.v2.Money.class);

            // Verify v2 has correct data
            assertNotNull(v2);
            assertEquals("v2", v2.getWrapperVersionId());
            assertEquals(1000L, v2.getAmount());
            assertEquals("USD", v2.getCurrency());

            // v2 may have additional fields that are null/default
            assertNull(v2.getExchangeRate());
        }

        @Test
        @DisplayName("Convert Money from v2 to v1")
        void convertMoneyV2ToV1() {
            // Create Money in v2 with extra fields
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            Money v2 = ctx2.newMoneyBuilder()
                    .setAmount(2000L)
                    .setCurrency("EUR")
                    .setExchangeRate(1.1)
                    .build();

            assertEquals("v2", v2.getWrapperVersionId());
            assertEquals(2000L, v2.getAmount());
            assertEquals("EUR", v2.getCurrency());
            assertEquals(1.1, v2.getExchangeRate());

            // Convert to v1
            io.alnovis.protowrapper.it.model.v1.Money v1 =
                    v2.asVersion(io.alnovis.protowrapper.it.model.v1.Money.class);

            // Verify v1 has correct data (exchangeRate lost during conversion)
            assertNotNull(v1);
            assertEquals("v1", v1.getWrapperVersionId());
            assertEquals(2000L, v1.getAmount());
            assertEquals("EUR", v1.getCurrency());
            // exchangeRate is v2-only field, should be null in v1
            assertNull(v1.getExchangeRate());
        }

        @Test
        @DisplayName("asVersion returns same instance when already correct type")
        void asVersionReturnsSameInstanceWhenCorrectType() {
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            io.alnovis.protowrapper.it.model.v1.Money v1 = (io.alnovis.protowrapper.it.model.v1.Money)
                    ctx1.newMoneyBuilder()
                            .setAmount(500L)
                            .setCurrency("GBP")
                            .build();

            // Convert to same type should return same instance
            io.alnovis.protowrapper.it.model.v1.Money result =
                    v1.asVersion(io.alnovis.protowrapper.it.model.v1.Money.class);

            assertSame(v1, result);
        }
    }

    @Nested
    @DisplayName("Date conversion")
    class DateConversion {

        @Test
        @DisplayName("Convert Date from v1 to v2")
        void convertDateV1ToV2() {
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            Date v1 = ctx1.newDateBuilder()
                    .setYear(2025)
                    .setMonth(12)
                    .setDay(22)
                    .build();

            assertEquals("v1", v1.getWrapperVersionId());

            // Convert to v2
            io.alnovis.protowrapper.it.model.v2.Date v2 =
                    v1.asVersion(io.alnovis.protowrapper.it.model.v2.Date.class);

            assertNotNull(v2);
            assertEquals("v2", v2.getWrapperVersionId());
            assertEquals(2025, v2.getYear());
            assertEquals(12, v2.getMonth());
            assertEquals(22, v2.getDay());
        }

        @Test
        @DisplayName("Convert Date from v2 to v1")
        void convertDateV2ToV1() {
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            Date v2 = ctx2.newDateBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            assertEquals("v2", v2.getWrapperVersionId());

            // Convert to v1
            io.alnovis.protowrapper.it.model.v1.Date v1 =
                    v2.asVersion(io.alnovis.protowrapper.it.model.v1.Date.class);

            assertNotNull(v1);
            assertEquals("v1", v1.getWrapperVersionId());
            assertEquals(2024, v1.getYear());
            assertEquals(6, v1.getMonth());
            assertEquals(15, v1.getDay());
        }
    }

    @Nested
    @DisplayName("Polymorphic usage")
    class PolymorphicUsage {

        @Test
        @DisplayName("Use interface reference for conversion")
        void useInterfaceReferenceForConversion() {
            // Create using interface type
            Money money = VersionContext.forVersionId("v1")
                    .newMoneyBuilder()
                    .setAmount(750L)
                    .setCurrency("JPY")
                    .build();

            // Convert using asVersion from interface
            io.alnovis.protowrapper.it.model.v2.Money v2 =
                    money.asVersion(io.alnovis.protowrapper.it.model.v2.Money.class);

            assertNotNull(v2);
            assertEquals("v2", v2.getWrapperVersionId());
            assertEquals(750L, v2.getAmount());
            assertEquals("JPY", v2.getCurrency());
        }

        @Test
        @DisplayName("Round-trip conversion preserves data")
        void roundTripConversionPreservesData() {
            // Start with v1
            Money original = VersionContext.forVersionId("v1")
                    .newMoneyBuilder()
                    .setAmount(999L)
                    .setCurrency("CHF")
                    .build();

            // Convert v1 -> v2 -> v1
            Money v2 = original.asVersion(io.alnovis.protowrapper.it.model.v2.Money.class);
            Money backToV1 = v2.asVersion(io.alnovis.protowrapper.it.model.v1.Money.class);

            // Data should be preserved
            assertEquals(original.getAmount(), backToV1.getAmount());
            assertEquals(original.getCurrency(), backToV1.getCurrency());
            assertEquals("v1", backToV1.getWrapperVersionId());
        }
    }

    @Nested
    @DisplayName("VersionContext parseFromBytes")
    class ParseFromBytes {

        @Test
        @DisplayName("parseMoneyFromBytes works correctly")
        void parseMoneyFromBytesWorks() throws Exception {
            // Create Money and serialize
            Money original = VersionContext.forVersionId("v1")
                    .newMoneyBuilder()
                    .setAmount(1234L)
                    .setCurrency("CAD")
                    .build();

            byte[] bytes = original.toBytes();

            // Parse in different version context
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            Money parsed = ctx2.parseMoneyFromBytes(bytes);

            assertNotNull(parsed);
            assertEquals("v2", parsed.getWrapperVersionId());
            assertEquals(1234L, parsed.getAmount());
            assertEquals("CAD", parsed.getCurrency());
        }

        @Test
        @DisplayName("parseFromBytes handles null")
        void parseFromBytesHandlesNull() throws Exception {
            VersionContext ctx = VersionContext.forVersionId("v1");
            Money result = ctx.parseMoneyFromBytes(null);
            assertNull(result);
        }
    }
}
