package com.example.model;

import com.example.model.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify proto fields are correctly mapped to wrapper classes.
 */
@DisplayName("Field Mapping Tests")
class FieldMappingTest {

    @Nested
    @DisplayName("V1 Money field mapping")
    class V1MoneyFieldMappingTest {

        @Test
        @DisplayName("Money wrapper correctly maps proto fields")
        void moneyFieldsAreMapped() {
            // Build proto message
            com.example.proto.v1.Common.Money proto = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(12345)
                    .setCurrency("USD")
                    .build();

            // Wrap with V1 implementation
            Money money = new com.example.model.v1.Money(proto);

            // Verify fields
            assertThat(money.getAmount()).isEqualTo(12345);
            assertThat(money.getCurrency()).isEqualTo("USD");

            // V2-only fields should return defaults for V1
            assertThat(money.hasExchangeRate()).isFalse();
            assertThat(money.hasOriginalCurrency()).isFalse();
        }
    }

    @Nested
    @DisplayName("V2 Money field mapping")
    class V2MoneyFieldMappingTest {

        @Test
        @DisplayName("V2 Money has additional fields")
        void v2MoneyHasAdditionalFields() {
            com.example.proto.v2.Common.Money proto = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(10000)
                    .setCurrency("EUR")
                    .setExchangeRate(1.08)
                    .setOriginalCurrency("USD")
                    .build();

            Money money = new com.example.model.v2.Money(proto);

            // Common fields
            assertThat(money.getAmount()).isEqualTo(10000);
            assertThat(money.getCurrency()).isEqualTo("EUR");

            // V2-only fields
            assertThat(money.hasExchangeRate()).isTrue();
            assertThat(money.getExchangeRate()).isEqualTo(1.08);
            assertThat(money.hasOriginalCurrency()).isTrue();
            assertThat(money.getOriginalCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("Date field mapping")
    class DateFieldMappingTest {

        @Test
        @DisplayName("Date fields are mapped correctly")
        void dateFieldsAreMapped() {
            com.example.proto.v1.Common.Date proto = com.example.proto.v1.Common.Date.newBuilder()
                    .setYear(2024)
                    .setMonth(12)
                    .setDay(25)
                    .build();

            Date date = new com.example.model.v1.Date(proto);

            assertThat(date.getYear()).isEqualTo(2024);
            assertThat(date.getMonth()).isEqualTo(12);
            assertThat(date.getDay()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Address field mapping")
    class AddressFieldMappingTest {

        @Test
        @DisplayName("Address required fields are mapped")
        void addressRequiredFields() {
            com.example.proto.v1.Common.Address proto = com.example.proto.v1.Common.Address.newBuilder()
                    .setStreet("Main Street")
                    .setCity("New York")
                    .setCountry("USA")
                    .build();

            Address address = new com.example.model.v1.Address(proto);

            assertThat(address.getStreet()).isEqualTo("Main Street");
            assertThat(address.getCity()).isEqualTo("New York");
            assertThat(address.getCountry()).isEqualTo("USA");
        }

        @Test
        @DisplayName("Address optional fields with hasXxx methods")
        void addressOptionalFields() {
            com.example.proto.v1.Common.Address proto = com.example.proto.v1.Common.Address.newBuilder()
                    .setStreet("Main Street")
                    .setCity("New York")
                    .setCountry("USA")
                    .setBuilding("42A")
                    .setApartment("12")
                    .setPostalCode("10001")
                    .build();

            Address address = new com.example.model.v1.Address(proto);

            assertThat(address.hasBuilding()).isTrue();
            assertThat(address.getBuilding()).isEqualTo("42A");
            assertThat(address.hasApartment()).isTrue();
            assertThat(address.getApartment()).isEqualTo("12");
            assertThat(address.hasPostalCode()).isTrue();
            assertThat(address.getPostalCode()).isEqualTo("10001");
        }

        @Test
        @DisplayName("Address enum field mapping")
        void addressEnumField() {
            com.example.proto.v1.Common.Address proto = com.example.proto.v1.Common.Address.newBuilder()
                    .setStreet("Main Street")
                    .setCity("New York")
                    .setCountry("USA")
                    .setType(com.example.proto.v1.Common.Address.AddressType.WORK)
                    .build();

            Address address = new com.example.model.v1.Address(proto);

            assertThat(address.getType()).isEqualTo(Address.AddressType.WORK);
        }

        @Test
        @DisplayName("V2 Address has GeoLocation nested type")
        void v2AddressHasGeoLocation() {
            com.example.proto.v2.Common.Address.GeoLocation geoProto =
                    com.example.proto.v2.Common.Address.GeoLocation.newBuilder()
                            .setLatitude(40.7128)
                            .setLongitude(-74.0060)
                            .build();

            com.example.proto.v2.Common.Address proto = com.example.proto.v2.Common.Address.newBuilder()
                    .setStreet("Main Street")
                    .setCity("New York")
                    .setCountry("USA")
                    .setLocation(geoProto)
                    .build();

            Address address = new com.example.model.v2.Address(proto);

            assertThat(address.hasLocation()).isTrue();
            Address.GeoLocation geo = address.getLocation();
            assertThat(geo).isNotNull();
            assertThat(geo.getLatitude()).isEqualTo(40.7128);
            assertThat(geo.getLongitude()).isEqualTo(-74.0060);
        }
    }

    @Nested
    @DisplayName("Nested message field mapping")
    class NestedMessageFieldMappingTest {

        @Test
        @DisplayName("OrderItem with nested Discount")
        void orderItemWithDiscount() {
            com.example.proto.v1.Order.OrderItem.Discount discountProto =
                    com.example.proto.v1.Order.OrderItem.Discount.newBuilder()
                            .setType(com.example.proto.v1.Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                            .setValue(15)
                            .setCode("SAVE15")
                            .build();

            com.example.proto.v1.Order.OrderItem proto =
                    com.example.proto.v1.Order.OrderItem.newBuilder()
                            .setProductId("PROD-001")
                            .setProductName("Widget")
                            .setQuantity(3)
                            .setUnitPrice(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000)
                                    .setCurrency("USD")
                                    .build())
                            .setDiscount(discountProto)
                            .build();

            OrderItem item = new com.example.model.v1.OrderItem(proto);

            assertThat(item.getProductId()).isEqualTo("PROD-001");
            assertThat(item.getProductName()).isEqualTo("Widget");
            assertThat(item.getQuantity()).isEqualTo(3);

            // Nested Money
            assertThat(item.getUnitPrice()).isNotNull();
            assertThat(item.getUnitPrice().getAmount()).isEqualTo(1000);

            // Nested Discount
            assertThat(item.hasDiscount()).isTrue();
            OrderItem.Discount discount = item.getDiscount();
            assertThat(discount.getType()).isEqualTo(OrderItem.Discount.DiscountType.PERCENTAGE);
            assertThat(discount.getValue()).isEqualTo(15);
            assertThat(discount.hasCode()).isTrue();
            assertThat(discount.getCode()).isEqualTo("SAVE15");
        }
    }

    @Nested
    @DisplayName("Deeply nested field mapping")
    class DeeplyNestedFieldMappingTest {

        @Test
        @DisplayName("UserProfile with Preferences.DisplaySettings")
        void userProfileWithDeepNesting() {
            com.example.proto.v1.User.UserProfile.Preferences.DisplaySettings displayProto =
                    com.example.proto.v1.User.UserProfile.Preferences.DisplaySettings.newBuilder()
                            .setTheme(com.example.proto.v1.User.UserProfile.Preferences
                                    .DisplaySettings.Theme.THEME_DARK)
                            .setItemsPerPage(25)
                            .setCompactView(true)
                            .build();

            com.example.proto.v1.User.UserProfile.Preferences prefsProto =
                    com.example.proto.v1.User.UserProfile.Preferences.newBuilder()
                            .setLanguage("en")
                            .setTimezone("UTC")
                            .setEmailNotifications(true)
                            .setDisplay(displayProto)
                            .build();

            com.example.proto.v1.User.UserProfile proto =
                    com.example.proto.v1.User.UserProfile.newBuilder()
                            .setUserId("USER-123")
                            .setUsername("testuser")
                            .setEmail("test@example.com")
                            .setRole(com.example.proto.v1.User.UserRole.ROLE_USER)
                            .setStatus(com.example.proto.v1.Common.Status.ACTIVE)
                            .setPreferences(prefsProto)
                            .build();

            UserProfile user = new com.example.model.v1.UserProfile(proto);

            // Top-level fields
            assertThat(user.getUserId()).isEqualTo("USER-123");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);

            // First level nesting: Preferences
            assertThat(user.hasPreferences()).isTrue();
            UserProfile.Preferences prefs = user.getPreferences();
            assertThat(prefs.getLanguage()).isEqualTo("en");
            assertThat(prefs.getTimezone()).isEqualTo("UTC");
            assertThat(prefs.isEmailNotifications()).isTrue();

            // Second level nesting: DisplaySettings
            assertThat(prefs.hasDisplay()).isTrue();
            UserProfile.Preferences.DisplaySettings display = prefs.getDisplay();
            assertThat(display.getTheme())
                    .isEqualTo(UserProfile.Preferences.DisplaySettings.Theme.THEME_DARK);
            assertThat(display.getItemsPerPage()).isEqualTo(25);
            assertThat(display.isCompactView()).isTrue();
        }
    }

    @Nested
    @DisplayName("Boolean field mapping")
    class BooleanFieldMappingTest {

        @Test
        @DisplayName("Boolean fields use isXxx naming")
        void booleanFieldsUseIsNaming() {
            com.example.proto.v1.User.AuthResponse proto =
                    com.example.proto.v1.User.AuthResponse.newBuilder()
                            .setSuccess(true)
                            .setToken("token123")
                            .build();

            AuthResponse response = new com.example.model.v1.AuthResponse(proto);

            // Boolean fields use isXxx
            assertThat(response.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("toBytes serialization")
    class ToBytesTest {

        @Test
        @DisplayName("toBytes returns valid proto serialization")
        void toBytesReturnsValidSerialization() throws Exception {
            com.example.proto.v1.Common.Money proto = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(5000)
                    .setCurrency("EUR")
                    .build();

            Money money = new com.example.model.v1.Money(proto);

            byte[] bytes = money.toBytes();

            // Deserialize and verify
            com.example.proto.v1.Common.Money deserialized =
                    com.example.proto.v1.Common.Money.parseFrom(bytes);

            assertThat(deserialized.getAmount()).isEqualTo(5000);
            assertThat(deserialized.getCurrency()).isEqualTo("EUR");
        }
    }

    @Nested
    @DisplayName("Wrapper version")
    class WrapperVersionTest {

        @Test
        @DisplayName("V1 wrapper returns version 1")
        void v1WrapperReturnsVersion1() {
            com.example.proto.v1.Common.Money proto = com.example.proto.v1.Common.Money.newBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            Money money = new com.example.model.v1.Money(proto);

            assertThat(money.getWrapperVersionId()).isEqualTo("v1");
        }

        @Test
        @DisplayName("V2 wrapper returns version 2")
        void v2WrapperReturnsVersion2() {
            com.example.proto.v2.Common.Money proto = com.example.proto.v2.Common.Money.newBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            Money money = new com.example.model.v2.Money(proto);

            assertThat(money.getWrapperVersionId()).isEqualTo("v2");
        }
    }
}
