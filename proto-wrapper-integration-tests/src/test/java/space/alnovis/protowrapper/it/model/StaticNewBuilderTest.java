package space.alnovis.protowrapper.it.model;

import space.alnovis.protowrapper.it.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the static newBuilder(VersionContext ctx) method on generated interfaces.
 * <p>
 * This feature allows creating builders using the interface type directly:
 * {@code Money.newBuilder(ctx).setAmount(100).build()}
 * instead of:
 * {@code ctx.newMoneyBuilder().setAmount(100).build()}
 */
@DisplayName("Static newBuilder(VersionContext ctx) Tests")
class StaticNewBuilderTest {

    @Nested
    @DisplayName("Top-level interfaces")
    class TopLevelInterfaceTests {

        @Nested
        @DisplayName("Money.newBuilder(ctx)")
        class MoneyNewBuilderTest {

            @Test
            @DisplayName("creates V1 builder when V1 context is provided")
            void createsV1BuilderWithV1Context() {
                VersionContext ctx = VersionContext.forVersion(1);

                Money money = Money.newBuilder(ctx)
                        .setAmount(1000)
                        .setCurrency("USD")
                        .build();

                assertThat(money).isNotNull();
                assertThat(money.getAmount()).isEqualTo(1000);
                assertThat(money.getCurrency()).isEqualTo("USD");
                assertThat(money.getWrapperVersion()).isEqualTo(1);
            }

            @Test
            @DisplayName("creates V2 builder when V2 context is provided")
            void createsV2BuilderWithV2Context() {
                VersionContext ctx = VersionContext.forVersion(2);

                Money money = Money.newBuilder(ctx)
                        .setAmount(2000)
                        .setCurrency("EUR")
                        .setExchangeRate(1.1)
                        .setOriginalCurrency("USD")
                        .build();

                assertThat(money).isNotNull();
                assertThat(money.getAmount()).isEqualTo(2000);
                assertThat(money.getCurrency()).isEqualTo("EUR");
                assertThat(money.getWrapperVersion()).isEqualTo(2);
                assertThat(money.hasExchangeRate()).isTrue();
                assertThat(money.getExchangeRate()).isEqualTo(1.1);
                assertThat(money.hasOriginalCurrency()).isTrue();
                assertThat(money.getOriginalCurrency()).isEqualTo("USD");
            }

            @Test
            @DisplayName("is equivalent to ctx.newMoneyBuilder()")
            void isEquivalentToContextMethod() {
                VersionContext ctx = VersionContext.forVersion(1);

                Money viaStaticMethod = Money.newBuilder(ctx)
                        .setAmount(500)
                        .setCurrency("GBP")
                        .build();

                Money viaContextMethod = ctx.newMoneyBuilder()
                        .setAmount(500)
                        .setCurrency("GBP")
                        .build();

                assertThat(viaStaticMethod.getAmount()).isEqualTo(viaContextMethod.getAmount());
                assertThat(viaStaticMethod.getCurrency()).isEqualTo(viaContextMethod.getCurrency());
                assertThat(viaStaticMethod.getWrapperVersion()).isEqualTo(viaContextMethod.getWrapperVersion());
            }
        }

        @Nested
        @DisplayName("Date.newBuilder(ctx)")
        class DateNewBuilderTest {

            @Test
            @DisplayName("creates V1 builder when V1 context is provided")
            void createsV1BuilderWithV1Context() {
                VersionContext ctx = VersionContext.forVersion(1);

                Date date = Date.newBuilder(ctx)
                        .setYear(2024)
                        .setMonth(12)
                        .setDay(25)
                        .build();

                assertThat(date).isNotNull();
                assertThat(date.getYear()).isEqualTo(2024);
                assertThat(date.getMonth()).isEqualTo(12);
                assertThat(date.getDay()).isEqualTo(25);
                assertThat(date.getWrapperVersion()).isEqualTo(1);
            }

            @Test
            @DisplayName("creates V2 builder when V2 context is provided")
            void createsV2BuilderWithV2Context() {
                VersionContext ctx = VersionContext.forVersion(2);

                Date date = Date.newBuilder(ctx)
                        .setYear(2025)
                        .setMonth(1)
                        .setDay(1)
                        .build();

                assertThat(date).isNotNull();
                assertThat(date.getYear()).isEqualTo(2025);
                assertThat(date.getMonth()).isEqualTo(1);
                assertThat(date.getDay()).isEqualTo(1);
                assertThat(date.getWrapperVersion()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("Address.newBuilder(ctx)")
        class AddressNewBuilderTest {

            @Test
            @DisplayName("creates V1 builder when V1 context is provided")
            void createsV1BuilderWithV1Context() {
                VersionContext ctx = VersionContext.forVersion(1);

                Address address = Address.newBuilder(ctx)
                        .setStreet("Main Street")
                        .setCity("New York")
                        .setCountry("USA")
                        .build();

                assertThat(address).isNotNull();
                assertThat(address.getStreet()).isEqualTo("Main Street");
                assertThat(address.getCity()).isEqualTo("New York");
                assertThat(address.getCountry()).isEqualTo("USA");
                assertThat(address.getWrapperVersion()).isEqualTo(1);
            }

            @Test
            @DisplayName("creates V2 builder with nested GeoLocation")
            void createsV2BuilderWithNestedGeoLocation() {
                VersionContext ctx = VersionContext.forVersion(2);

                Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
                        .setLatitude(40.7128)
                        .setLongitude(-74.0060)
                        .setAccuracy(10.0)
                        .build();

                Address address = Address.newBuilder(ctx)
                        .setStreet("Broadway")
                        .setCity("New York")
                        .setCountry("USA")
                        .setLocation(location)
                        .setDeliveryInstructions("Leave at door")
                        .build();

                assertThat(address).isNotNull();
                assertThat(address.getStreet()).isEqualTo("Broadway");
                assertThat(address.getWrapperVersion()).isEqualTo(2);
                assertThat(address.hasLocation()).isTrue();
                assertThat(address.getLocation().getLatitude()).isEqualTo(40.7128);
                assertThat(address.getLocation().getLongitude()).isEqualTo(-74.0060);
                assertThat(address.hasDeliveryInstructions()).isTrue();
                assertThat(address.getDeliveryInstructions()).isEqualTo("Leave at door");
            }
        }

        @Nested
        @DisplayName("UserProfile.newBuilder(ctx)")
        class UserProfileNewBuilderTest {

            @Test
            @DisplayName("creates builder with enum fields")
            void createsBuilderWithEnumFields() {
                VersionContext ctx = VersionContext.forVersion(1);

                UserProfile user = UserProfile.newBuilder(ctx)
                        .setUserId("USER-123")
                        .setUsername("testuser")
                        .setEmail("test@example.com")
                        .setRole(UserRole.ADMIN)
                        .setStatus(Status.CONFLICT_STATUS_ACTIVE)
                        .build();

                assertThat(user).isNotNull();
                assertThat(user.getUserId()).isEqualTo("USER-123");
                assertThat(user.getUsername()).isEqualTo("testuser");
                assertThat(user.getEmail()).isEqualTo("test@example.com");
                assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
                assertThat(user.getStatus()).isEqualTo(Status.CONFLICT_STATUS_ACTIVE);
            }
        }
    }

    @Nested
    @DisplayName("Nested interfaces")
    class NestedInterfaceTests {

        @Nested
        @DisplayName("Address.GeoLocation.newBuilder(ctx)")
        class GeoLocationNewBuilderTest {

            @Test
            @DisplayName("creates V2 builder for nested GeoLocation")
            void createsV2BuilderForNestedGeoLocation() {
                VersionContext ctx = VersionContext.forVersion(2);

                Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
                        .setLatitude(51.5074)
                        .setLongitude(-0.1278)
                        .setAccuracy(5.0)
                        .build();

                assertThat(location).isNotNull();
                assertThat(location.getLatitude()).isEqualTo(51.5074);
                assertThat(location.getLongitude()).isEqualTo(-0.1278);
                assertThat(location.hasAccuracy()).isTrue();
                assertThat(location.getAccuracy()).isEqualTo(5.0);
            }

            @Test
            @DisplayName("is equivalent to ctx.newAddressGeoLocationBuilder()")
            void isEquivalentToContextMethod() {
                VersionContext ctx = VersionContext.forVersion(2);

                Address.GeoLocation viaStaticMethod = Address.GeoLocation.newBuilder(ctx)
                        .setLatitude(48.8566)
                        .setLongitude(2.3522)
                        .build();

                Address.GeoLocation viaContextMethod = ctx.newAddressGeoLocationBuilder()
                        .setLatitude(48.8566)
                        .setLongitude(2.3522)
                        .build();

                assertThat(viaStaticMethod.getLatitude()).isEqualTo(viaContextMethod.getLatitude());
                assertThat(viaStaticMethod.getLongitude()).isEqualTo(viaContextMethod.getLongitude());
            }
        }

        @Nested
        @DisplayName("AuthResponse.SecurityChallenge.newBuilder(ctx)")
        class SecurityChallengeNewBuilderTest {

            @Test
            @DisplayName("creates V2 builder for nested SecurityChallenge")
            void createsV2BuilderForNestedSecurityChallenge() {
                VersionContext ctx = VersionContext.forVersion(2);

                AuthResponse.SecurityChallenge challenge = AuthResponse.SecurityChallenge.newBuilder(ctx)
                        .setType(AuthResponse.SecurityChallenge.ChallengeType.SMS_VERIFICATION)
                        .setChallengeId("CHAL-456")
                        .setHint("Enter 6-digit code")
                        .build();

                assertThat(challenge).isNotNull();
                assertThat(challenge.getType()).isEqualTo(AuthResponse.SecurityChallenge.ChallengeType.SMS_VERIFICATION);
                assertThat(challenge.getChallengeId()).isEqualTo("CHAL-456");
                assertThat(challenge.getHint()).isEqualTo("Enter 6-digit code");
            }

            @Test
            @DisplayName("is equivalent to ctx.newAuthResponseSecurityChallengeBuilder()")
            void isEquivalentToContextMethod() {
                VersionContext ctx = VersionContext.forVersion(2);

                AuthResponse.SecurityChallenge viaStaticMethod = AuthResponse.SecurityChallenge.newBuilder(ctx)
                        .setType(AuthResponse.SecurityChallenge.ChallengeType.EMAIL_VERIFICATION)
                        .setChallengeId("CHAL-789")
                        .build();

                AuthResponse.SecurityChallenge viaContextMethod = ctx.newAuthResponseSecurityChallengeBuilder()
                        .setType(AuthResponse.SecurityChallenge.ChallengeType.EMAIL_VERIFICATION)
                        .setChallengeId("CHAL-789")
                        .build();

                assertThat(viaStaticMethod.getType()).isEqualTo(viaContextMethod.getType());
                assertThat(viaStaticMethod.getChallengeId()).isEqualTo(viaContextMethod.getChallengeId());
            }
        }
    }

    @Nested
    @DisplayName("Builder chaining")
    class BuilderChainingTests {

        @Test
        @DisplayName("supports fluent builder pattern with all setters")
        void supportsFluentBuilderPattern() {
            VersionContext ctx = VersionContext.forVersion(2);

            OrderItem item = OrderItem.newBuilder(ctx)
                    .setProductId("PROD-001")
                    .setProductName("Test Product")
                    .setQuantity(3)
                    .setUnitPrice(Money.newBuilder(ctx)
                            .setAmount(1500)
                            .setCurrency("USD")
                            .build())
                    .build();

            assertThat(item).isNotNull();
            assertThat(item.getProductId()).isEqualTo("PROD-001");
            assertThat(item.getProductName()).isEqualTo("Test Product");
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getUnitPrice().getAmount()).isEqualTo(1500);
        }

        @Test
        @DisplayName("supports clear methods in chain")
        void supportsClearMethodsInChain() {
            VersionContext ctx = VersionContext.forVersion(2);

            Money money = Money.newBuilder(ctx)
                    .setAmount(1000)
                    .setCurrency("USD")
                    .setExchangeRate(1.5)
                    .clearExchangeRate()
                    .build();

            assertThat(money.hasExchangeRate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Round-trip serialization")
    class RoundTripTests {

        @Test
        @DisplayName("Money built via static newBuilder serializes and deserializes correctly")
        void moneyRoundTrip() throws Exception {
            VersionContext ctx = VersionContext.forVersion(1);

            Money original = Money.newBuilder(ctx)
                    .setAmount(999)
                    .setCurrency("JPY")
                    .build();

            byte[] bytes = original.toBytes();
            Money restored = ctx.wrapMoney(
                    space.alnovis.protowrapper.it.proto.v1.Common.Money.parseFrom(bytes));

            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getCurrency()).isEqualTo(original.getCurrency());
        }

        @Test
        @DisplayName("Address with nested GeoLocation serializes and deserializes correctly")
        void addressWithNestedRoundTrip() throws Exception {
            VersionContext ctx = VersionContext.forVersion(2);

            Address.GeoLocation location = Address.GeoLocation.newBuilder(ctx)
                    .setLatitude(35.6762)
                    .setLongitude(139.6503)
                    .build();

            Address original = Address.newBuilder(ctx)
                    .setStreet("Shibuya")
                    .setCity("Tokyo")
                    .setCountry("Japan")
                    .setLocation(location)
                    .build();

            byte[] bytes = original.toBytes();
            Address restored = ctx.wrapAddress(
                    space.alnovis.protowrapper.it.proto.v2.Common.Address.parseFrom(bytes));

            assertThat(restored.getStreet()).isEqualTo(original.getStreet());
            assertThat(restored.getCity()).isEqualTo(original.getCity());
            assertThat(restored.hasLocation()).isTrue();
            assertThat(restored.getLocation().getLatitude()).isEqualTo(original.getLocation().getLatitude());
            assertThat(restored.getLocation().getLongitude()).isEqualTo(original.getLocation().getLongitude());
        }
    }

    @Nested
    @DisplayName("Cross-version compatibility")
    class CrossVersionCompatibilityTests {

        @Test
        @DisplayName("V1 Money can be converted to V2 and accessed via static builder result")
        void v1MoneyConvertedToV2() {
            VersionContext ctxV1 = VersionContext.forVersion(1);
            VersionContext ctxV2 = VersionContext.forVersion(2);

            Money moneyV1 = Money.newBuilder(ctxV1)
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            // Convert V1 to V2
            Money moneyV2 = moneyV1.asVersion(space.alnovis.protowrapper.it.model.v2.Money.class);

            assertThat(moneyV2.getWrapperVersion()).isEqualTo(2);
            assertThat(moneyV2.getAmount()).isEqualTo(1000);
            assertThat(moneyV2.getCurrency()).isEqualTo("USD");
            // V2-only fields are not set
            assertThat(moneyV2.hasExchangeRate()).isFalse();
        }

        @Test
        @DisplayName("V2 Money with V2-only fields reports conversion warnings")
        void v2MoneyReportsConversionWarnings() {
            VersionContext ctx = VersionContext.forVersion(2);

            Money moneyV2 = Money.newBuilder(ctx)
                    .setAmount(2000)
                    .setCurrency("EUR")
                    .setExchangeRate(1.1)
                    .setOriginalCurrency("USD")
                    .build();

            // Check which fields will be inaccessible in V1
            assertThat(moneyV2.getFieldsInaccessibleInVersion(1))
                    .contains("exchangeRate", "originalCurrency");
            assertThat(moneyV2.canConvertLosslesslyTo(1)).isFalse();
            assertThat(moneyV2.canConvertLosslesslyTo(2)).isTrue();
        }
    }

    @Nested
    @DisplayName("getContext() integration")
    class GetContextIntegrationTests {

        @Test
        @DisplayName("built object's getContext() can be used to create more builders")
        void builtObjectContextCanCreateMoreBuilders() {
            VersionContext ctx = VersionContext.forVersion(2);

            Money money = Money.newBuilder(ctx)
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            // Use the context from the built object to create another builder
            Date date = Date.newBuilder(money.getContext())
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            assertThat(date.getWrapperVersion()).isEqualTo(2);
            assertThat(date.getYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("context from static builder matches context from context method")
        void contextMatchesBetweenApproaches() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money viaStatic = Money.newBuilder(ctx)
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            Money viaContext = ctx.newMoneyBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            assertThat(viaStatic.getContext().getVersion())
                    .isEqualTo(viaContext.getContext().getVersion());
        }
    }

    @Nested
    @DisplayName("Complex object graphs")
    class ComplexObjectGraphTests {

        @Test
        @DisplayName("can build OrderRequest with multiple nested objects using static builders")
        void canBuildOrderRequestWithNestedObjects() {
            VersionContext ctx = VersionContext.forVersion(2);

            Customer customer = Customer.newBuilder(ctx)
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

            OrderItem item1 = OrderItem.newBuilder(ctx)
                    .setProductId("PROD-001")
                    .setProductName("Widget A")
                    .setQuantity(2)
                    .setUnitPrice(Money.newBuilder(ctx)
                            .setAmount(2500)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem item2 = OrderItem.newBuilder(ctx)
                    .setProductId("PROD-002")
                    .setProductName("Widget B")
                    .setQuantity(1)
                    .setUnitPrice(Money.newBuilder(ctx)
                            .setAmount(4500)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderRequest order = OrderRequest.newBuilder(ctx)
                    .setOrderId("ORD-001")
                    .setCustomer(customer)
                    .addItems(item1)
                    .addItems(item2)
                    .setTotalAmount(Money.newBuilder(ctx)
                            .setAmount(9500)
                            .setCurrency("USD")
                            .build())
                    .setPaymentMethod(PaymentMethod.CARD)
                    .setOrderDate(Date.newBuilder(ctx)
                            .setYear(2024)
                            .setMonth(12)
                            .setDay(30)
                            .build())
                    .build();

            assertThat(order).isNotNull();
            assertThat(order.getOrderId()).isEqualTo("ORD-001");
            assertThat(order.getCustomer().getName()).isEqualTo("John Doe");
            assertThat(order.getCustomer().getShippingAddress().hasLocation()).isTrue();
            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getTotalAmount().getAmount()).isEqualTo(9500);
        }

        @Test
        @DisplayName("can build AuthResponse with SecurityChallenge using static builders")
        void canBuildAuthResponseWithSecurityChallenge() {
            VersionContext ctx = VersionContext.forVersion(2);

            AuthResponse response = AuthResponse.newBuilder(ctx)
                    .setSuccess(false)
                    .setChallenge(AuthResponse.SecurityChallenge.newBuilder(ctx)
                            .setType(AuthResponse.SecurityChallenge.ChallengeType.CAPTCHA)
                            .setChallengeId("CAP-001")
                            .setHint("Complete the captcha challenge")
                            .build())
                    .build();

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.hasChallenge()).isTrue();
            assertThat(response.getChallenge().getType())
                    .isEqualTo(AuthResponse.SecurityChallenge.ChallengeType.CAPTCHA);
            assertThat(response.getChallenge().getChallengeId()).isEqualTo("CAP-001");
        }
    }

    @Nested
    @DisplayName("toBuilder() integration")
    class ToBuilderIntegrationTests {

        @Test
        @DisplayName("can modify object created via static newBuilder using toBuilder")
        void canModifyViaToBuilder() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money original = Money.newBuilder(ctx)
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            Money modified = original.toBuilder()
                    .setAmount(200)
                    .build();

            assertThat(original.getAmount()).isEqualTo(100);
            assertThat(modified.getAmount()).isEqualTo(200);
            assertThat(modified.getCurrency()).isEqualTo("USD");
            assertThat(modified.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("emptyBuilder() on static-built object creates new empty builder")
        void emptyBuilderCreatesNewEmptyBuilder() {
            VersionContext ctx = VersionContext.forVersion(2);

            Money original = Money.newBuilder(ctx)
                    .setAmount(1000)
                    .setCurrency("USD")
                    .setExchangeRate(1.5)
                    .build();

            Money empty = original.emptyBuilder()
                    .setAmount(0)
                    .setCurrency("EUR")
                    .build();

            assertThat(empty.getAmount()).isEqualTo(0);
            assertThat(empty.getCurrency()).isEqualTo("EUR");
            assertThat(empty.hasExchangeRate()).isFalse();
            assertThat(empty.getWrapperVersion()).isEqualTo(2);
        }
    }
}
