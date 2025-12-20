package com.example.model;

import com.example.model.api.*;
import com.google.protobuf.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that verify VersionContext factory works correctly.
 */
@DisplayName("VersionContext Tests")
class VersionContextTest {

    @Nested
    @DisplayName("forVersion factory method")
    class ForVersionTest {

        @Test
        @DisplayName("forVersion(1) returns V1 context")
        void forVersion1ReturnsV1Context() {
            VersionContext ctx = VersionContext.forVersion(1);

            assertThat(ctx).isNotNull();
            assertThat(ctx.getVersion()).isEqualTo(1);
            assertThat(ctx).isInstanceOf(com.example.model.v1.VersionContextV1.class);
        }

        @Test
        @DisplayName("forVersion(2) returns V2 context")
        void forVersion2ReturnsV2Context() {
            VersionContext ctx = VersionContext.forVersion(2);

            assertThat(ctx).isNotNull();
            assertThat(ctx.getVersion()).isEqualTo(2);
            assertThat(ctx).isInstanceOf(com.example.model.v2.VersionContextV2.class);
        }

        @Test
        @DisplayName("forVersion with invalid version throws exception")
        void forVersionInvalidThrows() {
            assertThatThrownBy(() -> VersionContext.forVersion(99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("forVersion(0) throws exception")
        void forVersionZeroThrows() {
            assertThatThrownBy(() -> VersionContext.forVersion(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("V1 VersionContext wrapping")
    class V1VersionContextWrappingTest {

        private final VersionContext ctx = VersionContext.forVersion(1);

        @Test
        @DisplayName("wrapMoney wraps V1 proto correctly")
        void wrapMoneyV1() {
            com.example.proto.v1.Common.Money proto = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money money = ctx.wrapMoney(proto);

            assertThat(money).isNotNull();
            assertThat(money.getAmount()).isEqualTo(1000);
            assertThat(money.getCurrency()).isEqualTo("USD");
            assertThat(money.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("wrapDate wraps V1 proto correctly")
        void wrapDateV1() {
            com.example.proto.v1.Common.Date proto = com.example.proto.v1.Common.Date.newBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            Date date = ctx.wrapDate(proto);

            assertThat(date).isNotNull();
            assertThat(date.getYear()).isEqualTo(2024);
            assertThat(date.getMonth()).isEqualTo(6);
            assertThat(date.getDay()).isEqualTo(15);
        }

        @Test
        @DisplayName("wrapAddress wraps V1 proto correctly")
        void wrapAddressV1() {
            com.example.proto.v1.Common.Address proto = com.example.proto.v1.Common.Address.newBuilder()
                    .setStreet("Test Street")
                    .setCity("Test City")
                    .setCountry("Test Country")
                    .build();

            Address address = ctx.wrapAddress(proto);

            assertThat(address).isNotNull();
            assertThat(address.getStreet()).isEqualTo("Test Street");
            assertThat(address.getCity()).isEqualTo("Test City");
        }

        @Test
        @DisplayName("wrapOrderItem wraps V1 proto correctly")
        void wrapOrderItemV1() {
            com.example.proto.v1.Order.OrderItem proto = com.example.proto.v1.Order.OrderItem.newBuilder()
                    .setProductId("PROD-1")
                    .setProductName("Product")
                    .setQuantity(5)
                    .setUnitPrice(com.example.proto.v1.Common.Money.newBuilder()
                            .setAmount(500)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem item = ctx.wrapOrderItem(proto);

            assertThat(item).isNotNull();
            assertThat(item.getProductId()).isEqualTo("PROD-1");
            assertThat(item.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("wrapUserProfile wraps V1 proto correctly")
        void wrapUserProfileV1() {
            com.example.proto.v1.User.UserProfile proto = com.example.proto.v1.User.UserProfile.newBuilder()
                    .setUserId("USER-1")
                    .setUsername("testuser")
                    .setEmail("test@test.com")
                    .setRole(com.example.proto.v1.User.UserRole.ROLE_USER)
                    .setStatus(com.example.proto.v1.Common.Status.ACTIVE)
                    .build();

            UserProfile user = ctx.wrapUserProfile(proto);

            assertThat(user).isNotNull();
            assertThat(user.getUserId()).isEqualTo("USER-1");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("wrapAuthResponse wraps V1 proto correctly")
        void wrapAuthResponseV1() {
            com.example.proto.v1.User.AuthResponse proto = com.example.proto.v1.User.AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setToken("token123")
                    .build();

            AuthResponse response = ctx.wrapAuthResponse(proto);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getToken()).isEqualTo("token123");
        }
    }

    @Nested
    @DisplayName("V2 VersionContext wrapping")
    class V2VersionContextWrappingTest {

        private final VersionContext ctx = VersionContext.forVersion(2);

        @Test
        @DisplayName("wrapMoney wraps V2 proto with additional fields")
        void wrapMoneyV2() {
            com.example.proto.v2.Common.Money proto = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(2000)
                    .setCurrency("EUR")
                    .setExchangeRate(1.1)
                    .setOriginalCurrency("USD")
                    .build();

            Money money = ctx.wrapMoney(proto);

            assertThat(money).isNotNull();
            assertThat(money.getAmount()).isEqualTo(2000);
            assertThat(money.getCurrency()).isEqualTo("EUR");
            assertThat(money.getWrapperVersion()).isEqualTo(2);

            // V2-specific fields
            assertThat(money.hasExchangeRate()).isTrue();
            assertThat(money.getExchangeRate()).isEqualTo(1.1);
            assertThat(money.hasOriginalCurrency()).isTrue();
            assertThat(money.getOriginalCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("wrapAddress wraps V2 proto with GeoLocation")
        void wrapAddressV2WithGeoLocation() {
            com.example.proto.v2.Common.Address proto = com.example.proto.v2.Common.Address.newBuilder()
                    .setStreet("V2 Street")
                    .setCity("V2 City")
                    .setCountry("V2 Country")
                    .setLocation(com.example.proto.v2.Common.Address.GeoLocation.newBuilder()
                            .setLatitude(51.5074)
                            .setLongitude(-0.1278)
                            .build())
                    .build();

            Address address = ctx.wrapAddress(proto);

            assertThat(address).isNotNull();
            assertThat(address.hasLocation()).isTrue();
            assertThat(address.getLocation().getLatitude()).isEqualTo(51.5074);
            assertThat(address.getLocation().getLongitude()).isEqualTo(-0.1278);
        }

        @Test
        @DisplayName("wrapAuthResponse wraps V2 proto with SecurityChallenge")
        void wrapAuthResponseV2WithChallenge() {
            com.example.proto.v2.User.AuthResponse proto = com.example.proto.v2.User.AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setChallenge(com.example.proto.v2.User.AuthResponse.SecurityChallenge.newBuilder()
                            .setType(com.example.proto.v2.User.AuthResponse.SecurityChallenge
                                    .ChallengeType.SMS_VERIFICATION)
                            .setChallengeId("CHAL-123")
                            .setHint("Last 4 digits: 1234")
                            .build())
                    .build();

            AuthResponse response = ctx.wrapAuthResponse(proto);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.hasChallenge()).isTrue();

            AuthResponse.SecurityChallenge challenge = response.getChallenge();
            assertThat(challenge.getType()).isEqualTo(AuthResponse.SecurityChallenge.ChallengeType.SMS_VERIFICATION);
            assertThat(challenge.getChallengeId()).isEqualTo("CHAL-123");
            assertThat(challenge.getHint()).isEqualTo("Last 4 digits: 1234");
        }
    }

    @Nested
    @DisplayName("Polymorphic usage")
    class PolymorphicUsageTest {

        @Test
        @DisplayName("Can process different versions with same interface")
        void canProcessDifferentVersionsWithSameInterface() {
            // Create V1 and V2 money protos
            com.example.proto.v1.Common.Money protoV1 = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            com.example.proto.v2.Common.Money protoV2 = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(2000)
                    .setCurrency("EUR")
                    .build();

            // Wrap using respective contexts
            Money moneyV1 = VersionContext.forVersion(1).wrapMoney(protoV1);
            Money moneyV2 = VersionContext.forVersion(2).wrapMoney(protoV2);

            // Process using common interface
            long total = processMoneyPolymorphically(moneyV1, moneyV2);

            assertThat(total).isEqualTo(3000);
        }

        private long processMoneyPolymorphically(Money... monies) {
            long total = 0;
            for (Money money : monies) {
                total += money.getAmount();
            }
            return total;
        }

        @Test
        @DisplayName("VersionContext wraps generic Message")
        void versionContextWrapsGenericMessage() {
            Message protoV1 = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(500)
                    .setCurrency("GBP")
                    .build();

            VersionContext ctx = VersionContext.forVersion(1);
            Money money = ctx.wrapMoney(protoV1);

            assertThat(money.getAmount()).isEqualTo(500);
            assertThat(money.getCurrency()).isEqualTo("GBP");
        }
    }

    @Nested
    @DisplayName("Supported versions")
    class SupportedVersionsTest {

        @Test
        @DisplayName("forVersion supports version 1")
        void supportsVersion1() {
            VersionContext ctx = VersionContext.forVersion(1);
            assertThat(ctx).isNotNull();
            assertThat(ctx.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("forVersion supports version 2")
        void supportsVersion2() {
            VersionContext ctx = VersionContext.forVersion(2);
            assertThat(ctx).isNotNull();
            assertThat(ctx.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Can iterate through known versions")
        void canIterateThroughVersions() {
            int[] supportedVersions = {1, 2};
            for (int version : supportedVersions) {
                VersionContext ctx = VersionContext.forVersion(version);
                assertThat(ctx.getVersion()).isEqualTo(version);
            }
        }
    }
}
