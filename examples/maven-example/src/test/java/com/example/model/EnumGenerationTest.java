package com.example.model;

import com.example.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify enums are generated correctly from proto files.
 */
@DisplayName("Enum Generation Tests")
class EnumGenerationTest {

    @Nested
    @DisplayName("Top-level enums")
    class TopLevelEnumTest {

        @Test
        @DisplayName("Status enum has correct values from common.proto")
        void statusEnumHasCorrectValues() {
            Set<String> values = getEnumValues(Status.class);

            // From v1/common.proto: enum Status (base values)
            assertThat(values).contains("UNKNOWN", "ACTIVE", "INACTIVE", "DELETED");

            // V2 adds: SUSPENDED, VERIFICATION
            assertThat(values).contains("SUSPENDED", "VERIFICATION");
        }

        @Test
        @DisplayName("OrderStatus enum has correct values")
        void orderStatusEnumHasCorrectValues() {
            Set<String> values = getEnumValues(OrderStatus.class);

            // From v1/order.proto: enum OrderStatus
            // V2 adds: ORDER_RETURNED
            assertThat(values).contains(
                    "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
            );

            // V2-only value should be present in merged enum
            assertThat(values).contains("RETURNED");
        }

        @Test
        @DisplayName("PaymentMethod enum has correct values")
        void paymentMethodEnumHasCorrectValues() {
            Set<String> values = getEnumValues(PaymentMethod.class);

            // From v1/order.proto: enum PaymentMethod
            assertThat(values).contains("CASH", "CARD", "BANK_TRANSFER");

            // V2 adds more methods
            assertThat(values).contains("CRYPTO", "WALLET", "INSTALLMENT");
        }

        @Test
        @DisplayName("UserRole enum has correct values")
        void userRoleEnumHasCorrectValues() {
            Set<String> values = getEnumValues(UserRole.class);

            // From v1/user.proto: enum UserRole (base values)
            assertThat(values).contains("GUEST", "USER", "ADMIN", "SUPER_ADMIN");

            // V2 adds: MODERATOR, SUPPORT, DEVELOPER
            assertThat(values).contains("MODERATOR", "SUPPORT", "DEVELOPER");
        }
    }

    @Nested
    @DisplayName("V2-only enums")
    class V2OnlyEnumTest {

        @Test
        @DisplayName("Priority enum exists (v2-only)")
        void priorityEnumExists() {
            Set<String> values = getEnumValues(Priority.class);

            // From v2/order.proto: enum Priority
            assertThat(values).containsExactlyInAnyOrder(
                    "LOW", "NORMAL", "HIGH", "URGENT"
            );
        }

        @Test
        @DisplayName("DeliveryMethod enum exists (v2-only)")
        void deliveryMethodEnumExists() {
            Set<String> values = getEnumValues(DeliveryMethod.class);

            // From v2/order.proto: enum DeliveryMethod
            assertThat(values).containsExactlyInAnyOrder(
                    "STANDARD", "EXPRESS", "SAME_DAY", "PICKUP"
            );
        }

        @Test
        @DisplayName("AuthMethod enum exists (v2-only)")
        void authMethodEnumExists() {
            Set<String> values = getEnumValues(AuthMethod.class);

            // From v2/user.proto: enum AuthMethod (with AUTH_ prefix)
            assertThat(values).containsExactlyInAnyOrder(
                    "PASSWORD", "OAUTH", "SSO", "TWO_FACTOR", "BIOMETRIC"
            );
        }
    }

    @Nested
    @DisplayName("Nested enums")
    class NestedEnumTest {

        @Test
        @DisplayName("Address.AddressType is generated correctly")
        void addressTypeNestedEnum() {
            Set<String> values = getEnumValues(Address.AddressType.class);

            // V1 base values
            assertThat(values).contains("HOME", "WORK", "SHIPPING", "BILLING");

            // V2 adds more types
            assertThat(values).contains("WAREHOUSE", "PICKUP_POINT");
        }

        @Test
        @DisplayName("OrderItem.Discount.DiscountType is generated correctly")
        void discountTypeNestedEnum() {
            Set<String> values = getEnumValues(OrderItem.Discount.DiscountType.class);

            // V1 base values
            assertThat(values).contains("PERCENTAGE", "FIXED_AMOUNT");

            // V2 adds more types
            assertThat(values).contains("BUNDLE", "BUY_X_GET_Y");
        }

        @Test
        @DisplayName("UserProfile.Preferences.DisplaySettings.Theme is generated correctly")
        void themeNestedEnum() {
            Set<String> values = getEnumValues(UserProfile.Preferences.DisplaySettings.Theme.class);

            // V1 base values
            assertThat(values).contains("THEME_LIGHT", "THEME_DARK", "THEME_AUTO");

            // V2 adds more themes
            assertThat(values).contains("THEME_HIGH_CONTRAST");
        }

        @Test
        @DisplayName("AuthResponse.SecurityChallenge.ChallengeType is generated correctly (v2-only nested)")
        void challengeTypeNestedEnum() {
            Set<String> values = getEnumValues(AuthResponse.SecurityChallenge.ChallengeType.class);

            // From v2/user.proto: AuthResponse.SecurityChallenge.ChallengeType
            assertThat(values).containsExactlyInAnyOrder(
                    "NONE", "EMAIL_VERIFICATION", "SMS_VERIFICATION", "CAPTCHA", "SECURITY_QUESTION"
            );
        }
    }

    @Nested
    @DisplayName("Enum value properties")
    class EnumValuePropertiesTest {

        @Test
        @DisplayName("Enum values have correct proto numbers")
        void enumValuesHaveCorrectNumbers() {
            // Status enum from proto
            assertThat(Status.UNKNOWN.getValue()).isEqualTo(0);
            assertThat(Status.ACTIVE.getValue()).isEqualTo(1);
            assertThat(Status.INACTIVE.getValue()).isEqualTo(2);
            assertThat(Status.DELETED.getValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("Enum can be looked up by proto value")
        void enumLookupByProtoValue() {
            assertThat(Status.fromProtoValue(0)).isEqualTo(Status.UNKNOWN);
            assertThat(Status.fromProtoValue(1)).isEqualTo(Status.ACTIVE);
            assertThat(Status.fromProtoValue(2)).isEqualTo(Status.INACTIVE);
            assertThat(Status.fromProtoValue(3)).isEqualTo(Status.DELETED);
        }

        @Test
        @DisplayName("Unknown proto values return null")
        void unknownProtoValuesReturnNull() {
            // Unknown values should return null
            Status unknown = Status.fromProtoValue(999);
            assertThat(unknown).isNull();
        }

        @Test
        @DisplayName("OrderStatus enum values have correct proto numbers")
        void orderStatusProtoNumbers() {
            assertThat(OrderStatus.PENDING.getValue()).isEqualTo(0);
            assertThat(OrderStatus.CONFIRMED.getValue()).isEqualTo(1);
            assertThat(OrderStatus.SHIPPED.getValue()).isEqualTo(2);
            assertThat(OrderStatus.DELIVERED.getValue()).isEqualTo(3);
            assertThat(OrderStatus.CANCELLED.getValue()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Proto enum conversion methods")
    class ProtoEnumConversionTest {

        @Test
        @DisplayName("fromProto converts v1 proto enum to wrapper enum")
        void fromProtoConvertsV1() {
            // V1 proto enum
            com.example.proto.v1.Common.Status protoStatus = com.example.proto.v1.Common.Status.ACTIVE;

            // Convert to wrapper enum
            Status wrapperStatus = Status.fromProto(protoStatus);

            assertThat(wrapperStatus).isEqualTo(Status.ACTIVE);
            assertThat(wrapperStatus.getValue()).isEqualTo(protoStatus.getNumber());
        }

        @Test
        @DisplayName("fromProto converts v2 proto enum to wrapper enum")
        void fromProtoConvertsV2() {
            // V2 proto enum
            com.example.proto.v2.Common.Status protoStatus = com.example.proto.v2.Common.Status.SUSPENDED;

            // Convert to wrapper enum
            Status wrapperStatus = Status.fromProto(protoStatus);

            assertThat(wrapperStatus).isEqualTo(Status.SUSPENDED);
            assertThat(wrapperStatus.getValue()).isEqualTo(protoStatus.getNumber());
        }

        @Test
        @DisplayName("fromProto returns null for null input")
        void fromProtoReturnsNullForNull() {
            assertThat(Status.fromProto(null)).isNull();
        }

        @Test
        @DisplayName("fromProto throws for non-proto enum")
        void fromProtoThrowsForNonProtoEnum() {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Status.fromProto("not an enum");
            });
        }

        @Test
        @DisplayName("matches returns true for matching v1 proto enum")
        void matchesReturnsTrueForV1() {
            com.example.proto.v1.Common.Status protoStatus = com.example.proto.v1.Common.Status.ACTIVE;

            assertThat(Status.ACTIVE.matches(protoStatus)).isTrue();
            assertThat(Status.INACTIVE.matches(protoStatus)).isFalse();
        }

        @Test
        @DisplayName("matches returns true for matching v2 proto enum")
        void matchesReturnsTrueForV2() {
            com.example.proto.v2.Common.Status protoStatus = com.example.proto.v2.Common.Status.ACTIVE;

            assertThat(Status.ACTIVE.matches(protoStatus)).isTrue();
            assertThat(Status.DELETED.matches(protoStatus)).isFalse();
        }

        @Test
        @DisplayName("matches works across different proto versions")
        void matchesWorksCrossVersion() {
            com.example.proto.v1.Common.Status v1Status = com.example.proto.v1.Common.Status.DELETED;
            com.example.proto.v2.Common.Status v2Status = com.example.proto.v2.Common.Status.DELETED;

            // Same wrapper enum matches both versions
            assertThat(Status.DELETED.matches(v1Status)).isTrue();
            assertThat(Status.DELETED.matches(v2Status)).isTrue();
        }

        @Test
        @DisplayName("matches returns false for null")
        void matchesReturnsFalseForNull() {
            assertThat(Status.ACTIVE.matches(null)).isFalse();
        }

        @Test
        @DisplayName("matches returns false for non-proto enum")
        void matchesReturnsFalseForNonProtoEnum() {
            assertThat(Status.ACTIVE.matches("not an enum")).isFalse();
        }
    }

    private <E extends Enum<E>> Set<String> getEnumValues(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
