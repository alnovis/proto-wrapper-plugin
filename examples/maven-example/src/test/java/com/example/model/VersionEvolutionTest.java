package com.example.model;

import com.example.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify version evolution is handled correctly.
 * V1 wrappers should return defaults for V2-only fields,
 * and V2 wrappers should have all fields available.
 */
@DisplayName("Version Evolution Tests")
class VersionEvolutionTest {

    @Nested
    @DisplayName("V1 wrapper handles V2-only fields")
    class V1HandlesV2OnlyFieldsTest {

        @Test
        @DisplayName("V1 Money returns false for hasExchangeRate")
        void v1MoneyHasNoExchangeRate() {
            com.example.proto.v1.Common.Money proto = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money money = new com.example.model.v1.Money(proto);

            // V2-only fields should return false/null/default
            assertThat(money.hasExchangeRate()).isFalse();
            assertThat(money.hasOriginalCurrency()).isFalse();
        }

        @Test
        @DisplayName("V1 AuthResponse returns false for hasChallenge")
        void v1AuthResponseHasNoChallenge() {
            com.example.proto.v1.User.AuthResponse proto = com.example.proto.v1.User.AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setToken("token")
                    .build();

            AuthResponse response = new com.example.model.v1.AuthResponse(proto);

            // SecurityChallenge is V2-only
            assertThat(response.hasChallenge()).isFalse();
            assertThat(response.getChallenge()).isNull();
        }

        @Test
        @DisplayName("V1 AuthResponse.SessionInfo returns false for V2-only fields")
        void v1SessionInfoHasNoV2Fields() {
            com.example.proto.v1.User.AuthResponse.SessionInfo sessionProto =
                    com.example.proto.v1.User.AuthResponse.SessionInfo.newBuilder()
                            .setSessionId("SESSION-123")
                            .setExpiresAt(System.currentTimeMillis() + 3600000)
                            .setRefreshToken("refresh-token")
                            .build();

            com.example.proto.v1.User.AuthResponse proto = com.example.proto.v1.User.AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setSession(sessionProto)
                    .build();

            AuthResponse response = new com.example.model.v1.AuthResponse(proto);
            AuthResponse.SessionInfo session = response.getSession();

            // V1 fields work
            assertThat(session.getSessionId()).isEqualTo("SESSION-123");
            assertThat(session.hasRefreshToken()).isTrue();
            assertThat(session.getRefreshToken()).isEqualTo("refresh-token");

            // V2-only fields return false/null
            assertThat(session.hasDeviceName()).isFalse();
            assertThat(session.getDeviceName()).isNull();
            assertThat(session.hasCreatedAt()).isFalse();
            assertThat(session.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("V1 Address returns false for hasLocation (V2-only)")
        void v1AddressHasNoLocation() {
            com.example.proto.v1.Common.Address proto = com.example.proto.v1.Common.Address.newBuilder()
                    .setStreet("Main Street")
                    .setCity("City")
                    .setCountry("Country")
                    .build();

            Address address = new com.example.model.v1.Address(proto);

            // GeoLocation is V2-only
            assertThat(address.hasLocation()).isFalse();
            assertThat(address.getLocation()).isNull();

            // delivery_instructions is V2-only
            assertThat(address.hasDeliveryInstructions()).isFalse();
        }
    }

    @Nested
    @DisplayName("V2 wrapper has all fields")
    class V2HasAllFieldsTest {

        @Test
        @DisplayName("V2 Money has exchange rate fields")
        void v2MoneyHasExchangeRateFields() {
            com.example.proto.v2.Common.Money proto = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(1000)
                    .setCurrency("EUR")
                    .setExchangeRate(1.08)
                    .setOriginalCurrency("USD")
                    .build();

            Money money = new com.example.model.v2.Money(proto);

            // Common fields
            assertThat(money.getAmount()).isEqualTo(1000);
            assertThat(money.getCurrency()).isEqualTo("EUR");

            // V2-only fields
            assertThat(money.hasExchangeRate()).isTrue();
            assertThat(money.getExchangeRate()).isEqualTo(1.08);
            assertThat(money.hasOriginalCurrency()).isTrue();
            assertThat(money.getOriginalCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("V2 AuthResponse has SecurityChallenge")
        void v2AuthResponseHasSecurityChallenge() {
            com.example.proto.v2.User.AuthResponse.SecurityChallenge challengeProto =
                    com.example.proto.v2.User.AuthResponse.SecurityChallenge.newBuilder()
                            .setType(com.example.proto.v2.User.AuthResponse.SecurityChallenge
                                    .ChallengeType.CAPTCHA)
                            .setChallengeId("CHAL-456")
                            .setHint("Enter the captcha code")
                            .build();

            com.example.proto.v2.User.AuthResponse proto = com.example.proto.v2.User.AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setChallenge(challengeProto)
                    .build();

            AuthResponse response = new com.example.model.v2.AuthResponse(proto);

            assertThat(response.hasChallenge()).isTrue();
            AuthResponse.SecurityChallenge challenge = response.getChallenge();
            assertThat(challenge).isNotNull();
            assertThat(challenge.getType()).isEqualTo(AuthResponse.SecurityChallenge.ChallengeType.CAPTCHA);
            assertThat(challenge.getChallengeId()).isEqualTo("CHAL-456");
        }

        @Test
        @DisplayName("V2 AuthResponse.SessionInfo has all fields")
        void v2SessionInfoHasAllFields() {
            com.example.proto.v2.User.AuthResponse.SessionInfo sessionProto =
                    com.example.proto.v2.User.AuthResponse.SessionInfo.newBuilder()
                            .setSessionId("SESSION-V2")
                            .setExpiresAt(System.currentTimeMillis() + 7200000)
                            .setRefreshToken("refresh-v2")
                            .setDeviceName("MacBook Pro")
                            .setCreatedAt(com.example.proto.v2.Common.Date.newBuilder()
                                    .setYear(2024)
                                    .setMonth(12)
                                    .setDay(25)
                                    .build())
                            .build();

            com.example.proto.v2.User.AuthResponse proto = com.example.proto.v2.User.AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setToken("token-v2")
                    .setSession(sessionProto)
                    .build();

            AuthResponse response = new com.example.model.v2.AuthResponse(proto);
            AuthResponse.SessionInfo session = response.getSession();

            // Common fields
            assertThat(session.getSessionId()).isEqualTo("SESSION-V2");
            assertThat(session.hasRefreshToken()).isTrue();

            // V2-only fields
            assertThat(session.hasDeviceName()).isTrue();
            assertThat(session.getDeviceName()).isEqualTo("MacBook Pro");
            assertThat(session.hasCreatedAt()).isTrue();
            Date createdAt = session.getCreatedAt();
            assertThat(createdAt.getYear()).isEqualTo(2024);
            assertThat(createdAt.getMonth()).isEqualTo(12);
            assertThat(createdAt.getDay()).isEqualTo(25);
        }

        @Test
        @DisplayName("V2 Address has GeoLocation")
        void v2AddressHasGeoLocation() {
            com.example.proto.v2.Common.Address proto = com.example.proto.v2.Common.Address.newBuilder()
                    .setStreet("V2 Street")
                    .setCity("V2 City")
                    .setCountry("V2 Country")
                    .setLocation(com.example.proto.v2.Common.Address.GeoLocation.newBuilder()
                            .setLatitude(48.8566)
                            .setLongitude(2.3522)
                            .setAccuracy(5.0)
                            .build())
                    .setDeliveryInstructions("Leave at door")
                    .build();

            Address address = new com.example.model.v2.Address(proto);

            // Common fields
            assertThat(address.getStreet()).isEqualTo("V2 Street");

            // V2-only nested type
            assertThat(address.hasLocation()).isTrue();
            Address.GeoLocation geo = address.getLocation();
            assertThat(geo.getLatitude()).isEqualTo(48.8566);
            assertThat(geo.getLongitude()).isEqualTo(2.3522);
            assertThat(geo.hasAccuracy()).isTrue();
            assertThat(geo.getAccuracy()).isEqualTo(5.0);

            // V2-only field
            assertThat(address.hasDeliveryInstructions()).isTrue();
            assertThat(address.getDeliveryInstructions()).isEqualTo("Leave at door");
        }
    }

    @Nested
    @DisplayName("Enum evolution")
    class EnumEvolutionTest {

        @Test
        @DisplayName("OrderStatus has V2-only RETURNED value")
        void orderStatusHasReturnedValue() {
            // RETURNED was added in V2
            OrderStatus returned = OrderStatus.RETURNED;
            assertThat(returned).isNotNull();
            assertThat(returned.name()).isEqualTo("RETURNED");
        }

        @Test
        @DisplayName("PaymentMethod has V2-only values")
        void paymentMethodHasV2Values() {
            // CRYPTO, WALLET, INSTALLMENT were added in V2
            PaymentMethod crypto = PaymentMethod.CRYPTO;
            PaymentMethod wallet = PaymentMethod.WALLET;
            PaymentMethod installment = PaymentMethod.INSTALLMENT;

            assertThat(crypto).isNotNull();
            assertThat(wallet).isNotNull();
            assertThat(installment).isNotNull();
        }

        @Test
        @DisplayName("V2-only enums exist")
        void v2OnlyEnumsExist() {
            // These enums only exist in V2
            Priority[] priorities = Priority.values();
            DeliveryMethod[] deliveryMethods = DeliveryMethod.values();
            AuthMethod[] authMethods = AuthMethod.values();

            assertThat(priorities).hasSizeGreaterThanOrEqualTo(4); // At least LOW, NORMAL, HIGH, URGENT
            assertThat(deliveryMethods).hasSizeGreaterThanOrEqualTo(4); // At least STANDARD, EXPRESS, SAME_DAY, PICKUP
            assertThat(authMethods).hasSizeGreaterThanOrEqualTo(4); // At least PASSWORD, OAUTH, SSO, BIOMETRIC
        }
    }

    @Nested
    @DisplayName("Cross-version compatibility")
    class CrossVersionCompatibilityTest {

        @Test
        @DisplayName("V1 and V2 wrappers implement same interface")
        void v1AndV2ImplementSameInterface() {
            com.example.proto.v1.Common.Money protoV1 = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            com.example.proto.v2.Common.Money protoV2 = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(200)
                    .setCurrency("EUR")
                    .build();

            Money moneyV1 = new com.example.model.v1.Money(protoV1);
            Money moneyV2 = new com.example.model.v2.Money(protoV2);

            // Both implement Money interface
            assertThat(moneyV1).isInstanceOf(Money.class);
            assertThat(moneyV2).isInstanceOf(Money.class);

            // Can be used interchangeably
            Money[] monies = {moneyV1, moneyV2};
            long total = 0;
            for (Money m : monies) {
                total += m.getAmount();
            }
            assertThat(total).isEqualTo(300);
        }

        @Test
        @DisplayName("Can distinguish versions at runtime")
        void canDistinguishVersionsAtRuntime() {
            com.example.proto.v1.Common.Money protoV1 = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            com.example.proto.v2.Common.Money protoV2 = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(200)
                    .setCurrency("EUR")
                    .build();

            Money moneyV1 = new com.example.model.v1.Money(protoV1);
            Money moneyV2 = new com.example.model.v2.Money(protoV2);

            assertThat(moneyV1.getWrapperVersionId()).isEqualTo("v1");
            assertThat(moneyV2.getWrapperVersionId()).isEqualTo("v2");
        }
    }
}
