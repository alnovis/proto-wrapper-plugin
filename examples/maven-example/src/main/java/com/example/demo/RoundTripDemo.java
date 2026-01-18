package com.example.demo;

import com.example.model.api.*;
import com.example.proto.v1.Common;
import com.example.proto.v1.Order;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Demonstrates round-trip workflow with proto-wrapper:
 *
 * 1. Parse byte[] to protobuf object
 * 2. Wrap with version-agnostic interface via VersionContext
 * 3. Read and work with fields
 * 4. Modify data (via proto builder)
 * 5. Convert back to protobuf and byte[]
 *
 * Note: Wrappers are read-only views. To modify data, you need to:
 * - Get the underlying proto via getTypedProto() on impl class
 * - Use proto's toBuilder() to create a mutable builder
 * - Build new proto and wrap it again
 */
public class RoundTripDemo {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        System.out.println("=== Proto Wrapper Round-Trip Demo ===\n");

        // Step 1: Create original protobuf message
        Order.OrderItem originalProto = createSampleOrderItem();
        System.out.println("1. Original proto created:");
        System.out.println("   Product: " + originalProto.getProductName());
        System.out.println("   Quantity: " + originalProto.getQuantity());
        System.out.println("   Price: " + originalProto.getUnitPrice().getAmount() + " "
                + originalProto.getUnitPrice().getCurrency());

        // Step 2: Serialize to bytes (simulating network/storage)
        byte[] bytes = originalProto.toByteArray();
        System.out.println("\n2. Serialized to " + bytes.length + " bytes");

        // Step 3: Parse bytes back to proto (unknown version scenario)
        Order.OrderItem parsedProto = Order.OrderItem.parseFrom(bytes);
        System.out.println("\n3. Parsed from bytes");

        // Step 4: Wrap with version-agnostic interface
        VersionContext ctx = VersionContext.forVersionId("v1");
        OrderItem wrapper = ctx.wrapOrderItem(parsedProto);
        System.out.println("\n4. Wrapped with VersionContext (version " + wrapper.getWrapperVersion() + ")");

        // Step 5: Work with fields via interface
        System.out.println("\n5. Reading fields via interface:");
        System.out.println("   Product ID: " + wrapper.getProductId());
        System.out.println("   Product Name: " + wrapper.getProductName());
        System.out.println("   Quantity: " + wrapper.getQuantity());
        System.out.println("   Unit Price: " + wrapper.getUnitPrice().getAmount() + " "
                + wrapper.getUnitPrice().getCurrency());

        if (wrapper.hasDiscount()) {
            OrderItem.Discount discount = wrapper.getDiscount();
            System.out.println("   Discount: " + discount.getValue() + "% (" + discount.getCode() + ")");
        }

        // Step 6: Modify data (create new proto with changes)
        System.out.println("\n6. Modifying data...");

        // Get typed proto from impl class
        com.example.model.v1.OrderItem implWrapper = (com.example.model.v1.OrderItem) wrapper;
        Order.OrderItem typedProto = implWrapper.getTypedProto();

        // Use builder to modify
        Order.OrderItem modifiedProto = typedProto.toBuilder()
                .setQuantity(10)  // Change quantity
                .setUnitPrice(Common.Money.newBuilder()
                        .setAmount(1500)  // New price
                        .setCurrency("EUR")
                        .build())
                .setNotes("Modified via round-trip")
                .build();

        // Wrap modified proto
        OrderItem modifiedWrapper = ctx.wrapOrderItem(modifiedProto);
        System.out.println("   New Quantity: " + modifiedWrapper.getQuantity());
        System.out.println("   New Price: " + modifiedWrapper.getUnitPrice().getAmount() + " "
                + modifiedWrapper.getUnitPrice().getCurrency());
        System.out.println("   Notes: " + modifiedWrapper.getNotes());

        // Step 7: Serialize back to bytes
        byte[] modifiedBytes = modifiedWrapper.toBytes();
        System.out.println("\n7. Serialized modified data to " + modifiedBytes.length + " bytes");

        // Step 8: Verify round-trip
        Order.OrderItem verifyProto = Order.OrderItem.parseFrom(modifiedBytes);
        System.out.println("\n8. Verified round-trip:");
        System.out.println("   Quantity matches: " + (verifyProto.getQuantity() == 10));
        System.out.println("   Currency matches: " + "EUR".equals(verifyProto.getUnitPrice().getCurrency()));

        System.out.println("\n=== Round-Trip Complete ===");
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
}
