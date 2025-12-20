package com.example.model;

import com.example.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify interfaces are generated correctly from proto files.
 */
@DisplayName("Interface Generation Tests")
class InterfaceGenerationTest {

    @Nested
    @DisplayName("Money interface")
    class MoneyInterfaceTest {

        @Test
        @DisplayName("Money interface exists and is public")
        void moneyInterfaceExists() {
            assertThat(Money.class).isInterface();
            assertThat(Modifier.isPublic(Money.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("Money has required getter methods from proto")
        void moneyHasRequiredMethods() {
            Set<String> methods = getPublicMethodNames(Money.class);

            // Fields from v1/common.proto: amount, currency
            assertThat(methods).contains("getAmount", "getCurrency");

            // V2 adds: exchange_rate, original_currency (optional fields)
            assertThat(methods).contains("getExchangeRate", "hasExchangeRate");
            assertThat(methods).contains("getOriginalCurrency", "hasOriginalCurrency");
        }
    }

    @Nested
    @DisplayName("Date interface")
    class DateInterfaceTest {

        @Test
        @DisplayName("Date interface has correct methods")
        void dateHasCorrectMethods() {
            Set<String> methods = getPublicMethodNames(Date.class);

            // Fields from v1/common.proto: year, month, day
            assertThat(methods).contains("getYear", "getMonth", "getDay");
        }
    }

    @Nested
    @DisplayName("Address interface")
    class AddressInterfaceTest {

        @Test
        @DisplayName("Address interface has correct methods")
        void addressHasCorrectMethods() {
            Set<String> methods = getPublicMethodNames(Address.class);

            // Required fields from v1/common.proto
            assertThat(methods).contains("getStreet", "getCity", "getCountry");

            // Optional fields
            assertThat(methods).contains("getBuilding", "hasBuilding");
            assertThat(methods).contains("getApartment", "hasApartment");
            assertThat(methods).contains("getPostalCode", "hasPostalCode");
            assertThat(methods).contains("getType");
        }

        @Test
        @DisplayName("Address has nested AddressType enum")
        void addressHasNestedEnum() {
            Class<?>[] innerClasses = Address.class.getDeclaredClasses();
            Set<String> innerClassNames = Arrays.stream(innerClasses)
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());

            assertThat(innerClassNames).contains("AddressType");
        }

        @Test
        @DisplayName("Address.AddressType has correct values")
        void addressTypeHasCorrectValues() {
            Address.AddressType[] values = Address.AddressType.values();
            Set<String> valueNames = Arrays.stream(values)
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            // From v1/common.proto enum AddressType
            assertThat(valueNames).contains("HOME", "WORK", "SHIPPING", "BILLING");
        }
    }

    @Nested
    @DisplayName("OrderItem interface")
    class OrderItemInterfaceTest {

        @Test
        @DisplayName("OrderItem has correct methods")
        void orderItemHasCorrectMethods() {
            Set<String> methods = getPublicMethodNames(OrderItem.class);

            // Fields from v1/order.proto
            assertThat(methods).contains("getProductId", "getProductName", "getQuantity");
            assertThat(methods).contains("getUnitPrice");
            assertThat(methods).contains("getDiscount", "hasDiscount");
            assertThat(methods).contains("getNotes", "hasNotes");
        }

        @Test
        @DisplayName("OrderItem has nested Discount interface")
        void orderItemHasNestedDiscount() {
            Class<?>[] innerClasses = OrderItem.class.getDeclaredClasses();
            Set<String> innerClassNames = Arrays.stream(innerClasses)
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());

            assertThat(innerClassNames).contains("Discount");
        }

        @Test
        @DisplayName("OrderItem.Discount has correct methods")
        void discountHasCorrectMethods() throws ClassNotFoundException {
            Class<?> discountClass = Class.forName("com.example.model.api.OrderItem$Discount");
            Set<String> methods = getPublicMethodNames(discountClass);

            // Fields from v1/order.proto OrderItem.Discount
            assertThat(methods).contains("getType", "getValue");
            assertThat(methods).contains("getCode", "hasCode");
        }

        @Test
        @DisplayName("OrderItem.Discount.DiscountType has correct values")
        void discountTypeHasCorrectValues() {
            OrderItem.Discount.DiscountType[] values = OrderItem.Discount.DiscountType.values();
            Set<String> valueNames = Arrays.stream(values)
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            // From v1/order.proto enum DiscountType
            assertThat(valueNames).contains("PERCENTAGE", "FIXED_AMOUNT");
        }
    }

    @Nested
    @DisplayName("UserProfile interface")
    class UserProfileInterfaceTest {

        @Test
        @DisplayName("UserProfile has deeply nested Preferences.DisplaySettings")
        void userProfileHasDeeplyNestedTypes() throws ClassNotFoundException {
            // Check Preferences exists
            Class<?> prefsClass = Class.forName("com.example.model.api.UserProfile$Preferences");
            assertThat(prefsClass).isNotNull();

            // Check DisplaySettings exists
            Class<?> displayClass = Class.forName(
                    "com.example.model.api.UserProfile$Preferences$DisplaySettings");
            assertThat(displayClass).isNotNull();

            // Check Theme enum exists
            Class<?> themeClass = Class.forName(
                    "com.example.model.api.UserProfile$Preferences$DisplaySettings$Theme");
            assertThat(themeClass.isEnum()).isTrue();
        }

        @Test
        @DisplayName("UserProfile.Preferences.DisplaySettings.Theme has correct values")
        void themeEnumHasCorrectValues() {
            UserProfile.Preferences.DisplaySettings.Theme[] values =
                    UserProfile.Preferences.DisplaySettings.Theme.values();
            Set<String> valueNames = Arrays.stream(values)
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            // From v1/user.proto enum Theme
            assertThat(valueNames).contains("THEME_LIGHT", "THEME_DARK", "THEME_AUTO");
        }
    }

    @Nested
    @DisplayName("AuthResponse interface")
    class AuthResponseInterfaceTest {

        @Test
        @DisplayName("AuthResponse has correct methods")
        void authResponseHasCorrectMethods() {
            Set<String> methods = getPublicMethodNames(AuthResponse.class);

            // Common fields
            assertThat(methods).contains("isSuccess");
            assertThat(methods).contains("getToken", "hasToken");
            assertThat(methods).contains("getUser", "hasUser");
            assertThat(methods).contains("getErrorMessage", "hasErrorMessage");
            assertThat(methods).contains("getSession", "hasSession");

            // V2-only fields (should still be in interface)
            assertThat(methods).contains("getChallenge", "hasChallenge");
        }

        @Test
        @DisplayName("AuthResponse has nested SessionInfo")
        void authResponseHasSessionInfo() throws ClassNotFoundException {
            Class<?> sessionClass = Class.forName("com.example.model.api.AuthResponse$SessionInfo");
            Set<String> methods = getPublicMethodNames(sessionClass);

            // Common fields
            assertThat(methods).contains("getSessionId", "getExpiresAt");
            assertThat(methods).contains("getRefreshToken", "hasRefreshToken");

            // V2-only fields
            assertThat(methods).contains("getDeviceName", "hasDeviceName");
            assertThat(methods).contains("getCreatedAt", "hasCreatedAt");
        }

        @Test
        @DisplayName("AuthResponse has nested SecurityChallenge (v2-only)")
        void authResponseHasSecurityChallenge() throws ClassNotFoundException {
            Class<?> challengeClass = Class.forName("com.example.model.api.AuthResponse$SecurityChallenge");
            Set<String> methods = getPublicMethodNames(challengeClass);

            assertThat(methods).contains("getType");
            assertThat(methods).contains("getChallengeId", "hasChallengeId");
            assertThat(methods).contains("getHint", "hasHint");
        }
    }

    @Nested
    @DisplayName("OrderResponse interface")
    class OrderResponseInterfaceTest {

        @Test
        @DisplayName("OrderResponse has PaymentInfo nested type")
        void orderResponseHasPaymentInfo() throws ClassNotFoundException {
            Class<?> paymentClass = Class.forName("com.example.model.api.OrderResponse$PaymentInfo");
            Set<String> methods = getPublicMethodNames(paymentClass);

            assertThat(methods).contains("getMethod", "getAmount");
            assertThat(methods).contains("getTransactionId", "hasTransactionId");
            assertThat(methods).contains("getPaymentDate", "hasPaymentDate");
        }
    }

    private Set<String> getPublicMethodNames(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
