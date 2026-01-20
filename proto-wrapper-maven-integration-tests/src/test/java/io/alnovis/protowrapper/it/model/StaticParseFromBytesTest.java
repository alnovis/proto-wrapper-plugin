package io.alnovis.protowrapper.it.model;

import com.google.protobuf.InvalidProtocolBufferException;
import io.alnovis.protowrapper.it.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the static parseFromBytes(VersionContext ctx, byte[] bytes) method on generated interfaces.
 * <p>
 * This feature allows parsing bytes using the interface type directly:
 * {@code Money.parseFromBytes(ctx, bytes)}
 * instead of:
 * {@code ctx.parseMoneyFromBytes(bytes)}
 */
@DisplayName("Static parseFromBytes(VersionContext ctx, byte[] bytes) Tests")
class StaticParseFromBytesTest {

    @Nested
    @DisplayName("Top-level interfaces")
    class TopLevelInterfaceTests {

        @Nested
        @DisplayName("Money.parseFromBytes(ctx, bytes)")
        class MoneyParseFromBytesTest {

            @Test
            @DisplayName("parses V1 bytes when V1 context is provided")
            void parsesV1BytesWithV1Context() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v1");

                // Create and serialize a Money object
                Money original = Money.newBuilder(ctx)
                        .setAmount(1000)
                        .setCurrency("USD")
                        .build();
                byte[] bytes = original.toBytes();

                // Parse using static method
                Money parsed = Money.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getAmount()).isEqualTo(1000);
                assertThat(parsed.getCurrency()).isEqualTo("USD");
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v1");
            }

            @Test
            @DisplayName("parses V2 bytes when V2 context is provided")
            void parsesV2BytesWithV2Context() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v2");

                Money original = Money.newBuilder(ctx)
                        .setAmount(2000)
                        .setCurrency("EUR")
                        .setExchangeRate(1.1)
                        .setOriginalCurrency("USD")
                        .build();
                byte[] bytes = original.toBytes();

                Money parsed = Money.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getAmount()).isEqualTo(2000);
                assertThat(parsed.getCurrency()).isEqualTo("EUR");
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v2");
                assertThat(parsed.hasExchangeRate()).isTrue();
                assertThat(parsed.getExchangeRate()).isEqualTo(1.1);
                assertThat(parsed.hasOriginalCurrency()).isTrue();
                assertThat(parsed.getOriginalCurrency()).isEqualTo("USD");
            }

            @Test
            @DisplayName("is equivalent to ctx.parseMoneyFromBytes()")
            void isEquivalentToContextMethod() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v1");

                Money original = Money.newBuilder(ctx)
                        .setAmount(500)
                        .setCurrency("GBP")
                        .build();
                byte[] bytes = original.toBytes();

                Money viaStaticMethod = Money.parseFromBytes(ctx, bytes);
                Money viaContextMethod = ctx.parseMoneyFromBytes(bytes);

                assertThat(viaStaticMethod.getAmount()).isEqualTo(viaContextMethod.getAmount());
                assertThat(viaStaticMethod.getCurrency()).isEqualTo(viaContextMethod.getCurrency());
                assertThat(viaStaticMethod.getWrapperVersionId()).isEqualTo(viaContextMethod.getWrapperVersionId());
            }
        }

        @Nested
        @DisplayName("Date.parseFromBytes(ctx, bytes)")
        class DateParseFromBytesTest {

            @Test
            @DisplayName("parses V1 bytes when V1 context is provided")
            void parsesV1BytesWithV1Context() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v1");

                Date original = Date.newBuilder(ctx)
                        .setYear(2024)
                        .setMonth(12)
                        .setDay(25)
                        .build();
                byte[] bytes = original.toBytes();

                Date parsed = Date.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getYear()).isEqualTo(2024);
                assertThat(parsed.getMonth()).isEqualTo(12);
                assertThat(parsed.getDay()).isEqualTo(25);
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v1");
            }

            @Test
            @DisplayName("parses V2 bytes when V2 context is provided")
            void parsesV2BytesWithV2Context() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v2");

                Date original = Date.newBuilder(ctx)
                        .setYear(2025)
                        .setMonth(1)
                        .setDay(1)
                        .build();
                byte[] bytes = original.toBytes();

                Date parsed = Date.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getYear()).isEqualTo(2025);
                assertThat(parsed.getMonth()).isEqualTo(1);
                assertThat(parsed.getDay()).isEqualTo(1);
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v2");
            }
        }

        @Nested
        @DisplayName("Address.parseFromBytes(ctx, bytes)")
        class AddressParseFromBytesTest {

            @Test
            @DisplayName("parses V1 bytes when V1 context is provided")
            void parsesV1BytesWithV1Context() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v1");

                Address original = Address.newBuilder(ctx)
                        .setStreet("Main Street")
                        .setCity("New York")
                        .setCountry("USA")
                        .build();
                byte[] bytes = original.toBytes();

                Address parsed = Address.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getStreet()).isEqualTo("Main Street");
                assertThat(parsed.getCity()).isEqualTo("New York");
                assertThat(parsed.getCountry()).isEqualTo("USA");
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v1");
            }

            @Test
            @DisplayName("parses V2 bytes with nested GeoLocation")
            void parsesV2BytesWithNestedGeoLocation() throws InvalidProtocolBufferException {
                VersionContext ctx = VersionContext.forVersionId("v2");

                Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
                        .setLatitude(40.7128)
                        .setLongitude(-74.0060)
                        .setAccuracy(10.0)
                        .build();

                Address original = Address.newBuilder(ctx)
                        .setStreet("Broadway")
                        .setCity("New York")
                        .setCountry("USA")
                        .setLocation(location)
                        .setDeliveryInstructions("Leave at door")
                        .build();
                byte[] bytes = original.toBytes();

                Address parsed = Address.parseFromBytes(ctx, bytes);

                assertThat(parsed).isNotNull();
                assertThat(parsed.getStreet()).isEqualTo("Broadway");
                assertThat(parsed.getWrapperVersionId()).isEqualTo("v2");
                assertThat(parsed.hasLocation()).isTrue();
                assertThat(parsed.getLocation().getLatitude()).isEqualTo(40.7128);
                assertThat(parsed.getLocation().getLongitude()).isEqualTo(-74.0060);
                assertThat(parsed.hasDeliveryInstructions()).isTrue();
                assertThat(parsed.getDeliveryInstructions()).isEqualTo("Leave at door");
            }
        }
    }

    @Nested
    @DisplayName("Round-trip serialization")
    class RoundTripTests {

        @Test
        @DisplayName("Money round-trip via parseFromBytes preserves all data")
        void moneyRoundTrip() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v2");

            Money original = Money.newBuilder(ctx)
                    .setAmount(999)
                    .setCurrency("JPY")
                    .setExchangeRate(0.0067)
                    .setOriginalCurrency("USD")
                    .build();

            byte[] bytes = original.toBytes();
            Money restored = Money.parseFromBytes(ctx, bytes);

            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getCurrency()).isEqualTo(original.getCurrency());
            assertThat(restored.getExchangeRate()).isEqualTo(original.getExchangeRate());
            assertThat(restored.getOriginalCurrency()).isEqualTo(original.getOriginalCurrency());
        }

        @Test
        @DisplayName("complex object graph round-trip via parseFromBytes")
        void complexObjectGraphRoundTrip() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v2");

            Customer original = Customer.newBuilder(ctx)
                    .setId("CUST-001")
                    .setName("John Doe")
                    .setEmail("john@example.com")
                    .setShippingAddress(Address.newBuilder(ctx)
                            .setStreet("123 Main St")
                            .setCity("Boston")
                            .setCountry("USA")
                            .setLocation(Address.GeoLocation.newBuilder(ctx)
                                    .setLatitude(42.3601)
                                    .setLongitude(-71.0589)
                                    .build())
                            .build())
                    .build();

            byte[] bytes = original.toBytes();
            Customer restored = Customer.parseFromBytes(ctx, bytes);

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getName()).isEqualTo(original.getName());
            assertThat(restored.getEmail()).isEqualTo(original.getEmail());
            assertThat(restored.getShippingAddress().getStreet())
                    .isEqualTo(original.getShippingAddress().getStreet());
            assertThat(restored.getShippingAddress().getLocation().getLatitude())
                    .isEqualTo(original.getShippingAddress().getLocation().getLatitude());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("throws InvalidProtocolBufferException for invalid bytes")
        void throwsExceptionForInvalidBytes() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};

            assertThatThrownBy(() -> Money.parseFromBytes(ctx, invalidBytes))
                    .isInstanceOf(InvalidProtocolBufferException.class);
        }

        @Test
        @DisplayName("throws exception for empty bytes when required fields exist")
        void throwsExceptionForEmptyBytesWithRequiredFields() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            byte[] emptyBytes = new byte[0];

            // Money has required fields (amount, currency), so empty bytes should fail
            assertThatThrownBy(() -> Money.parseFromBytes(ctx, emptyBytes))
                    .isInstanceOf(InvalidProtocolBufferException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Cross-version parsing")
    class CrossVersionParsingTests {

        @Test
        @DisplayName("V1 bytes parsed with V2 context can access V1 fields")
        void v1BytesParsedWithV2ContextCanAccessV1Fields() throws InvalidProtocolBufferException {
            VersionContext ctxV1 = VersionContext.forVersionId("v1");
            VersionContext ctxV2 = VersionContext.forVersionId("v2");

            // Create V1 object and serialize
            Money originalV1 = Money.newBuilder(ctxV1)
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();
            byte[] bytes = originalV1.toBytes();

            // Parse with V2 context (protobuf is forward compatible)
            Money parsedAsV2 = Money.parseFromBytes(ctxV2, bytes);

            assertThat(parsedAsV2.getWrapperVersionId()).isEqualTo("v2");
            assertThat(parsedAsV2.getAmount()).isEqualTo(1000);
            assertThat(parsedAsV2.getCurrency()).isEqualTo("USD");
            // V2-only fields should not be present
            assertThat(parsedAsV2.hasExchangeRate()).isFalse();
        }

        @Test
        @DisplayName("V2 bytes parsed with V1 context preserve unknown fields")
        void v2BytesParsedWithV1ContextPreserveUnknownFields() throws InvalidProtocolBufferException {
            VersionContext ctxV1 = VersionContext.forVersionId("v1");
            VersionContext ctxV2 = VersionContext.forVersionId("v2");

            // Create V2 object with V2-only fields and serialize
            Money originalV2 = Money.newBuilder(ctxV2)
                    .setAmount(2000)
                    .setCurrency("EUR")
                    .setExchangeRate(1.1)
                    .setOriginalCurrency("USD")
                    .build();
            byte[] bytes = originalV2.toBytes();

            // Parse with V1 context
            Money parsedAsV1 = Money.parseFromBytes(ctxV1, bytes);

            assertThat(parsedAsV1.getWrapperVersionId()).isEqualTo("v1");
            assertThat(parsedAsV1.getAmount()).isEqualTo(2000);
            assertThat(parsedAsV1.getCurrency()).isEqualTo("EUR");

            // V2-only fields are not accessible through V1 API
            assertThat(parsedAsV1.hasExchangeRate()).isFalse();
            assertThat(parsedAsV1.getExchangeRate()).isNull();

            // But data is preserved - re-serialize and parse as V2
            byte[] reserialized = parsedAsV1.toBytes();
            Money backToV2 = Money.parseFromBytes(ctxV2, reserialized);

            assertThat(backToV2.getExchangeRate()).isEqualTo(1.1);
            assertThat(backToV2.getOriginalCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("getContext() integration")
    class GetContextIntegrationTests {

        @Test
        @DisplayName("parsed object's getContext() can be used to parse more objects")
        void parsedObjectContextCanParseMoreObjects() throws InvalidProtocolBufferException {
            VersionContext ctx = VersionContext.forVersionId("v2");

            Money money = Money.newBuilder(ctx)
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();
            byte[] moneyBytes = money.toBytes();

            Money parsedMoney = Money.parseFromBytes(ctx, moneyBytes);

            // Use the context from the parsed object to parse another object
            Date date = Date.newBuilder(parsedMoney.getContext())
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();
            byte[] dateBytes = date.toBytes();

            Date parsedDate = Date.parseFromBytes(parsedMoney.getContext(), dateBytes);

            assertThat(parsedDate.getWrapperVersionId()).isEqualTo("v2");
            assertThat(parsedDate.getYear()).isEqualTo(2024);
        }
    }
}
