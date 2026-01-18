package com.example.demo;

import com.example.model.api.*;
import com.example.proto.v1.Common;
import com.example.proto.v1.Order;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Demonstrates round-trip workflow using generated Builder pattern.
 *
 * <p>When generateBuilders=true, you can modify wrapper objects directly
 * without accessing the underlying proto:</p>
 *
 * <pre>
 * // Old way (without builders):
 * var impl = (OrderItemV1) wrapper;
 * var proto = impl.getTypedProto().toBuilder()
 *     .setQuantity(10)
 *     .build();
 * var modified = new OrderItemV1(proto);
 *
 * // New way (with builders):
 * var modified = wrapper.toBuilder()
 *     .setQuantity(10)
 *     .build();
 * </pre>
 */
public class BuilderRoundTripDemo {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        System.out.println("=== Proto Wrapper Builder Round-Trip Demo ===\n");

        // Step 1: Create original protobuf message
        Order.OrderItem originalProto = createSampleOrderItem();
        System.out.println("1. Original proto created:");
        printOrderItem(originalProto);

        // Step 2: Serialize to bytes (simulating network/storage)
        byte[] bytes = originalProto.toByteArray();
        System.out.println("\n2. Serialized to " + bytes.length + " bytes");

        // Step 3: Parse and wrap with version-agnostic interface
        Order.OrderItem parsedProto = Order.OrderItem.parseFrom(bytes);
        VersionContext ctx = VersionContext.forVersionId("v1");
        OrderItem wrapper = ctx.wrapOrderItem(parsedProto);
        System.out.println("\n3. Wrapped with VersionContext (version " + wrapper.getWrapperVersion() + ")");

        // Step 4: Modify using Builder (the new way!)
        System.out.println("\n4. Modifying via toBuilder()...");

        OrderItem modified = wrapper.toBuilder()
                .setQuantity(10)                          // Change quantity
                .setNotes("Modified via Builder API")     // Set notes
                .build();

        System.out.println("   Original quantity: " + wrapper.getQuantity());
        System.out.println("   Modified quantity: " + modified.getQuantity());
        System.out.println("   Modified notes: " + modified.getNotes());

        // Step 5: Chain multiple modifications
        System.out.println("\n5. Chaining modifications...");

        OrderItem fullyModified = modified.toBuilder()
                .setProductName("Premium Wireless Mouse")
                .setQuantity(5)
                .clearDiscount()                          // Remove discount
                .build();

        System.out.println("   New name: " + fullyModified.getProductName());
        System.out.println("   New quantity: " + fullyModified.getQuantity());
        System.out.println("   Has discount: " + fullyModified.hasDiscount());

        // Step 6: Modify nested objects
        System.out.println("\n6. Modifying nested objects...");

        // First create a new Money wrapper via proto (nested builders work similarly)
        Money newPrice = ctx.wrapMoney(Common.Money.newBuilder()
                .setAmount(4999)
                .setCurrency("EUR")
                .build());

        OrderItem withNewPrice = fullyModified.toBuilder()
                .setUnitPrice(newPrice)
                .build();

        System.out.println("   New price: " + withNewPrice.getUnitPrice().getAmount()
                + " " + withNewPrice.getUnitPrice().getCurrency());

        // Step 7: Serialize back to bytes
        byte[] modifiedBytes = withNewPrice.toBytes();
        System.out.println("\n7. Serialized modified data to " + modifiedBytes.length + " bytes");

        // Step 8: Verify round-trip
        Order.OrderItem verifyProto = Order.OrderItem.parseFrom(modifiedBytes);
        System.out.println("\n8. Verified round-trip:");
        System.out.println("   Name: " + verifyProto.getProductName());
        System.out.println("   Quantity: " + verifyProto.getQuantity());
        System.out.println("   Price: " + verifyProto.getUnitPrice().getAmount()
                + " " + verifyProto.getUnitPrice().getCurrency());
        System.out.println("   Has discount: " + verifyProto.hasDiscount());

        // Step 9: Compare old vs new approach
        System.out.println("\n9. Comparison - Old vs New approach:");
        demonstrateOldVsNew(wrapper, ctx);

        System.out.println("\n=== Builder Round-Trip Complete ===");
    }

    private static void demonstrateOldVsNew(OrderItem wrapper, VersionContext ctx) {
        // OLD WAY: Requires casting and proto builder access
        System.out.println("\n   OLD WAY (without generateBuilders):");
        System.out.println("   ```java");
        System.out.println("   var impl = (OrderItemV1) wrapper;");
        System.out.println("   var proto = impl.getTypedProto().toBuilder()");
        System.out.println("       .setQuantity(10)");
        System.out.println("       .build();");
        System.out.println("   var modified = new OrderItemV1(proto);");
        System.out.println("   ```");

        // NEW WAY: Clean fluent API
        System.out.println("\n   NEW WAY (with generateBuilders=true):");
        System.out.println("   ```java");
        System.out.println("   var modified = wrapper.toBuilder()");
        System.out.println("       .setQuantity(10)");
        System.out.println("       .build();");
        System.out.println("   ```");

        // Actually demonstrate both work the same
        System.out.println("\n   Both produce identical results:");

        // Old way
        var impl = (com.example.model.v1.OrderItem) wrapper;
        var protoBuilder = impl.getTypedProto().toBuilder().setQuantity(99).build();
        var oldWay = ctx.wrapOrderItem(protoBuilder);

        // New way
        var newWay = wrapper.toBuilder().setQuantity(99).build();

        System.out.println("   Old way quantity: " + oldWay.getQuantity());
        System.out.println("   New way quantity: " + newWay.getQuantity());
        System.out.println("   Results match: " + (oldWay.getQuantity() == newWay.getQuantity()));
    }

    private static Order.OrderItem createSampleOrderItem() {
        return Order.OrderItem.newBuilder()
                .setProductId("PROD-001")
                .setProductName("Wireless Mouse")
                .setQuantity(2)
                .setUnitPrice(Common.Money.newBuilder()
                        .setAmount(2999)
                        .setCurrency("USD")
                        .build())
                .setDiscount(Order.OrderItem.Discount.newBuilder()
                        .setType(Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                        .setValue(15)
                        .setCode("SAVE15")
                        .build())
                .build();
    }

    private static void printOrderItem(Order.OrderItem proto) {
        System.out.println("   Product: " + proto.getProductName());
        System.out.println("   Quantity: " + proto.getQuantity());
        System.out.println("   Price: " + proto.getUnitPrice().getAmount()
                + " " + proto.getUnitPrice().getCurrency());
        if (proto.hasDiscount()) {
            System.out.println("   Discount: " + proto.getDiscount().getValue()
                    + "% (" + proto.getDiscount().getCode() + ")");
        }
    }
}
