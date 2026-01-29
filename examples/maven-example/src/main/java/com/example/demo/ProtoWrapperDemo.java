package com.example.demo;

import com.example.model.api.*;
import com.google.protobuf.Message;

/**
 * Demonstration of proto-wrapper-maven-plugin usage.
 *
 * <p>This class shows how to use the generated version-agnostic wrappers
 * to work with multiple protobuf schema versions seamlessly.</p>
 *
 * <h2>Key Concepts:</h2>
 * <ul>
 *   <li><b>Interfaces</b> - Version-agnostic API in {@code com.example.model.api}</li>
 *   <li><b>Implementations</b> - Version-specific in {@code com.example.model.v1/v2}</li>
 *   <li><b>VersionContext</b> - Factory for creating wrappers at runtime</li>
 * </ul>
 *
 * <h2>Note on includeVersionSuffix:</h2>
 * <p>This example uses {@code includeVersionSuffix=false}, so implementation classes
 * are named by their package (e.g., {@code com.example.model.v1.Money}) rather than
 * with version suffix ({@code MoneyV1}).</p>
 */
public class ProtoWrapperDemo {

    /**
     * Demonstrates using VersionContext for runtime version selection.
     */
    public void demonstrateVersionContext() {
        // Get VersionContext for specific version (recommended String-based API)
        VersionContext v1Context = VersionContext.forVersionId("v1");
        VersionContext v2Context = VersionContext.forVersionId("v2");

        System.out.println("V1 Context version: " + v1Context.getVersionId());
        System.out.println("V2 Context version: " + v2Context.getVersionId());

        // Other useful static methods on VersionContext
        // VersionContext.find("v1")           -> Optional<VersionContext>
        // VersionContext.getDefault()         -> VersionContext (latest version)
        // VersionContext.supportedVersions()  -> List<String>
        // VersionContext.isSupported("v1")    -> boolean

        // Use context to wrap proto messages
        // Money protoMoney = ...; // from deserialization
        // com.example.model.api.Money money = v1Context.wrapMoney(protoMoney);
    }

    /**
     * Demonstrates working with simple messages.
     */
    public void demonstrateSimpleMessages() {
        // Create V1 proto message
        com.example.proto.v1.Common.Money protoMoneyV1 =
                com.example.proto.v1.Common.Money.newBuilder()
                        .setAmount(10000)  // $100.00
                        .setCurrency("USD")
                        .build();

        // Wrap with V1 implementation (using fully qualified name)
        Money moneyV1 = new com.example.model.v1.Money(protoMoneyV1);

        // Use version-agnostic interface
        System.out.println("Amount: " + moneyV1.getAmount());
        System.out.println("Currency: " + moneyV1.getCurrency());

        // V2 has additional fields
        com.example.proto.v2.Common.Money protoMoneyV2 =
                com.example.proto.v2.Common.Money.newBuilder()
                        .setAmount(10000)
                        .setCurrency("EUR")
                        .setExchangeRate(1.08)
                        .setOriginalCurrency("USD")
                        .build();

        Money moneyV2 = new com.example.model.v2.Money(protoMoneyV2);

        // Common fields work the same way
        System.out.println("V2 Amount: " + moneyV2.getAmount());
    }

    /**
     * Demonstrates working with enums.
     */
    public void demonstrateEnums() {
        // Top-level enums are available in the API package
        // Status, OrderStatus, PaymentMethod, etc.

        // Build a proto with enum
        com.example.proto.v1.Order.OrderResponse protoResponse =
                com.example.proto.v1.Order.OrderResponse.newBuilder()
                        .setOrderId("ORD-123")
                        .setStatus(com.example.proto.v1.Order.OrderStatus.ORDER_SHIPPED)
                        .build();

        // Wrap and access enum
        OrderResponse response = new com.example.model.v1.OrderResponse(protoResponse);
        System.out.println("Order ID: " + response.getOrderId());
        // Enum is converted to wrapper enum
        OrderStatus status = response.getStatus();
        System.out.println("Status: " + status);
    }

    /**
     * Demonstrates working with nested messages.
     */
    public void demonstrateNestedMessages() {
        // Build proto with nested messages
        com.example.proto.v1.Order.OrderItem.Discount protoDiscount =
                com.example.proto.v1.Order.OrderItem.Discount.newBuilder()
                        .setType(com.example.proto.v1.Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                        .setValue(10)
                        .setCode("SAVE10")
                        .build();

        com.example.proto.v1.Order.OrderItem protoItem =
                com.example.proto.v1.Order.OrderItem.newBuilder()
                        .setProductId("PROD-001")
                        .setProductName("Widget")
                        .setQuantity(2)
                        .setUnitPrice(com.example.proto.v1.Common.Money.newBuilder()
                                .setAmount(2500)
                                .setCurrency("USD")
                                .build())
                        .setDiscount(protoDiscount)
                        .build();

        // Wrap and access nested structures
        OrderItem item = new com.example.model.v1.OrderItem(protoItem);
        System.out.println("Product: " + item.getProductName());
        System.out.println("Quantity: " + item.getQuantity());

        // Access nested message
        OrderItem.Discount discount = item.getDiscount();
        if (discount != null) {
            System.out.println("Discount type: " + discount.getType());
            System.out.println("Discount value: " + discount.getValue() + "%");
            System.out.println("Promo code: " + discount.getCode());
        }
    }

    /**
     * Demonstrates deeply nested structures.
     */
    public void demonstrateDeeplyNestedMessages() {
        // Build a complex nested structure
        com.example.proto.v1.User.UserProfile.Preferences.DisplaySettings protoDisplay =
                com.example.proto.v1.User.UserProfile.Preferences.DisplaySettings.newBuilder()
                        .setTheme(com.example.proto.v1.User.UserProfile.Preferences.DisplaySettings.Theme.THEME_DARK)
                        .setItemsPerPage(50)
                        .setCompactView(true)
                        .build();

        com.example.proto.v1.User.UserProfile.Preferences protoPrefs =
                com.example.proto.v1.User.UserProfile.Preferences.newBuilder()
                        .setLanguage("ru")
                        .setTimezone("Europe/Moscow")
                        .setEmailNotifications(false)
                        .setDisplay(protoDisplay)
                        .build();

        com.example.proto.v1.User.UserProfile protoUser =
                com.example.proto.v1.User.UserProfile.newBuilder()
                        .setUserId("USR-001")
                        .setUsername("john_doe")
                        .setEmail("john@example.com")
                        .setRole(com.example.proto.v1.User.UserRole.ROLE_ADMIN)
                        .setStatus(com.example.proto.v1.Common.Status.ACTIVE)
                        .setPreferences(protoPrefs)
                        .build();

        // Wrap and navigate the structure
        UserProfile user = new com.example.model.v1.UserProfile(protoUser);
        System.out.println("User: " + user.getUsername());
        System.out.println("Role: " + user.getRole());

        UserProfile.Preferences prefs = user.getPreferences();
        if (prefs != null) {
            System.out.println("Language: " + prefs.getLanguage());
            System.out.println("Timezone: " + prefs.getTimezone());

            UserProfile.Preferences.DisplaySettings display = prefs.getDisplay();
            if (display != null) {
                System.out.println("Theme: " + display.getTheme());
                System.out.println("Items per page: " + display.getItemsPerPage());
            }
        }
    }

    /**
     * Demonstrates version-specific fields.
     *
     * <p>Fields that exist only in certain versions are accessible
     * through the version-specific implementation classes.</p>
     */
    public void demonstrateVersionSpecificFields() {
        // V2 has additional fields not present in V1
        com.example.proto.v2.Common.Address.GeoLocation protoGeo =
                com.example.proto.v2.Common.Address.GeoLocation.newBuilder()
                        .setLatitude(55.7558)
                        .setLongitude(37.6173)
                        .build();

        com.example.proto.v2.Common.Address protoAddress =
                com.example.proto.v2.Common.Address.newBuilder()
                        .setStreet("Red Square")
                        .setCity("Moscow")
                        .setCountry("Russia")
                        .setLocation(protoGeo)  // V2-only field
                        .setDeliveryInstructions("Ring the bell")  // V2-only field
                        .build();

        // Through the interface, you get common fields
        com.example.model.v2.Address address = new com.example.model.v2.Address(protoAddress);
        System.out.println("Street: " + address.getStreet());
        System.out.println("City: " + address.getCity());

        // V2-specific fields are available directly (since we know it's V2)
        Address.GeoLocation geo = address.getLocation();
        if (geo != null) {
            System.out.println("Latitude: " + geo.getLatitude());
            System.out.println("Longitude: " + geo.getLongitude());
        }
    }

    /**
     * Demonstrates polymorphic usage.
     *
     * <p>Process messages without knowing their version at compile time.</p>
     */
    public void demonstratePolymorphicUsage(String versionId, Message protoOrder) {
        // Get the appropriate VersionContext using String identifier
        VersionContext ctx = VersionContext.forVersionId(versionId);

        // Wrap the proto message - works for any version
        OrderResponse order = ctx.wrapOrderResponse(protoOrder);

        // Use the version-agnostic interface
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Status: " + order.getStatus());

        // Access nested data uniformly
        OrderResponse.PaymentInfo payment = order.getPayment();
        if (payment != null) {
            System.out.println("Payment method: " + payment.getMethod());
            System.out.println("Amount: " + payment.getAmount().getAmount());
        }
    }

    public static void main(String[] args) {
        ProtoWrapperDemo demo = new ProtoWrapperDemo();

        System.out.println("=== Version Context ===");
        demo.demonstrateVersionContext();

        System.out.println("\n=== Simple Messages ===");
        demo.demonstrateSimpleMessages();

        System.out.println("\n=== Enums ===");
        demo.demonstrateEnums();

        System.out.println("\n=== Nested Messages ===");
        demo.demonstrateNestedMessages();

        System.out.println("\n=== Deeply Nested Messages ===");
        demo.demonstrateDeeplyNestedMessages();

        System.out.println("\n=== Version-Specific Fields ===");
        demo.demonstrateVersionSpecificFields();
    }
}
