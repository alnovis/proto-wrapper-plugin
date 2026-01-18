package com.example.model;

import com.example.model.api.*;
import com.example.proto.v1.Common;
import com.example.proto.v1.Order;
import com.example.proto.v1.Invoice;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for round-trip workflows using generated Builder pattern.
 *
 * <p>These tests verify that modifications via toBuilder() produce
 * correct protobuf data when serialized and re-parsed.</p>
 */
@DisplayName("Builder Round-Trip Tests")
class BuilderRoundTripTest {

    private final VersionContext ctx = VersionContext.forVersionId("v1");

    @Nested
    @DisplayName("Basic builder modifications")
    class BasicBuilderTest {

        @Test
        @DisplayName("Modify single field via builder")
        void modifySingleField() throws InvalidProtocolBufferException {
            // Create original
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-001")
                    .setProductName("Original Product")
                    .setQuantity(5)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);

            // Modify via builder
            OrderItem modified = wrapper.toBuilder()
                    .setQuantity(10)
                    .build();

            // Verify modification
            assertThat(modified.getQuantity()).isEqualTo(10);
            // Other fields preserved
            assertThat(modified.getProductId()).isEqualTo("PROD-001");
            assertThat(modified.getProductName()).isEqualTo("Original Product");
            assertThat(modified.getUnitPrice().getAmount()).isEqualTo(1000);

            // Round-trip: serialize and parse
            byte[] bytes = modified.toBytes();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);

            assertThat(parsed.getQuantity()).isEqualTo(10);
            assertThat(parsed.getProductId()).isEqualTo("PROD-001");
        }

        @Test
        @DisplayName("Modify multiple fields via builder chain")
        void modifyMultipleFields() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-002")
                    .setProductName("Old Name")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(500)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);

            // Chain multiple modifications
            OrderItem modified = wrapper.toBuilder()
                    .setProductName("New Name")
                    .setQuantity(99)
                    .setNotes("Added via builder")
                    .build();

            assertThat(modified.getProductName()).isEqualTo("New Name");
            assertThat(modified.getQuantity()).isEqualTo(99);
            assertThat(modified.getNotes()).isEqualTo("Added via builder");
            assertThat(modified.getProductId()).isEqualTo("PROD-002"); // Preserved

            // Round-trip
            byte[] bytes = modified.toBytes();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);

            assertThat(parsed.getProductName()).isEqualTo("New Name");
            assertThat(parsed.getQuantity()).isEqualTo(99);
            assertThat(parsed.getNotes()).isEqualTo("Added via builder");
        }

        @Test
        @DisplayName("Set and clear optional field")
        void setAndClearOptionalField() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-003")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(100)
                            .setCurrency("USD")
                            .build())
                    .setNotes("Initial notes")
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);
            assertThat(wrapper.hasNotes()).isTrue();

            // Clear notes
            OrderItem cleared = wrapper.toBuilder()
                    .clearNotes()
                    .build();

            assertThat(cleared.hasNotes()).isFalse();

            // Set again
            OrderItem restored = cleared.toBuilder()
                    .setNotes("Restored notes")
                    .build();

            assertThat(restored.hasNotes()).isTrue();
            assertThat(restored.getNotes()).isEqualTo("Restored notes");

            // Round-trip cleared version
            byte[] clearedBytes = cleared.toBytes();
            Order.OrderItem parsedCleared = Order.OrderItem.parseFrom(clearedBytes);
            assertThat(parsedCleared.hasNotes()).isFalse();
        }
    }

    @Nested
    @DisplayName("Nested message builder modifications")
    class NestedMessageBuilderTest {

        @Test
        @DisplayName("Replace nested message via builder")
        void replaceNestedMessage() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-004")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);

            // Create new Money wrapper
            Money newPrice = ctx.wrapMoney(Common.Money.newBuilder()
                    .setAmount(2500)
                    .setCurrency("EUR")
                    .build());

            // Replace via builder
            OrderItem modified = wrapper.toBuilder()
                    .setUnitPrice(newPrice)
                    .build();

            assertThat(modified.getUnitPrice().getAmount()).isEqualTo(2500);
            assertThat(modified.getUnitPrice().getCurrency()).isEqualTo("EUR");

            // Round-trip
            byte[] bytes = modified.toBytes();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);

            assertThat(parsed.getUnitPrice().getAmount()).isEqualTo(2500);
            assertThat(parsed.getUnitPrice().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Set and clear nested optional message")
        void setAndClearNestedMessage() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-005")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(500)
                            .setCurrency("USD")
                            .build())
                    .setDiscount(Order.OrderItem.Discount.newBuilder()
                            .setType(Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                            .setValue(10)
                            .setCode("SAVE10")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);
            assertThat(wrapper.hasDiscount()).isTrue();
            assertThat(wrapper.getDiscount().getValue()).isEqualTo(10);

            // Clear discount
            OrderItem cleared = wrapper.toBuilder()
                    .clearDiscount()
                    .build();

            assertThat(cleared.hasDiscount()).isFalse();

            // Round-trip
            byte[] bytes = cleared.toBytes();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);
            assertThat(parsed.hasDiscount()).isFalse();
        }
    }

    @Nested
    @DisplayName("Sequential builder modifications")
    class SequentialBuilderTest {

        @Test
        @DisplayName("Multiple sequential toBuilder calls")
        void sequentialBuilderCalls() {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-006")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(100)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem v1 = ctx.wrapOrderItem(original);

            // First modification
            OrderItem v2 = v1.toBuilder()
                    .setQuantity(2)
                    .build();

            // Second modification
            OrderItem v3 = v2.toBuilder()
                    .setQuantity(3)
                    .setNotes("Note")
                    .build();

            // Third modification
            OrderItem v4 = v3.toBuilder()
                    .setProductName("Updated Name")
                    .clearNotes()
                    .build();

            // Verify each version is independent
            assertThat(v1.getQuantity()).isEqualTo(1);
            assertThat(v2.getQuantity()).isEqualTo(2);
            assertThat(v3.getQuantity()).isEqualTo(3);
            assertThat(v4.getQuantity()).isEqualTo(3);

            assertThat(v3.hasNotes()).isTrue();
            assertThat(v4.hasNotes()).isFalse();
            assertThat(v4.getProductName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Builder preserves version")
        void builderPreservesVersion() {
            Order.OrderItem proto = Order.OrderItem.newBuilder()
                    .setProductId("PROD-007")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(100)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(proto);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);

            OrderItem modified = wrapper.toBuilder()
                    .setQuantity(10)
                    .build();

            assertThat(modified.getWrapperVersion()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Builder vs proto builder comparison")
    class BuilderComparisonTest {

        @Test
        @DisplayName("Builder produces same result as proto builder")
        void builderMatchesProtoBuilder() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-008")
                    .setProductName("Product")
                    .setQuantity(5)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build())
                    .build();

            OrderItem wrapper = ctx.wrapOrderItem(original);

            // NEW WAY: Using wrapper builder
            OrderItem viaWrapperBuilder = wrapper.toBuilder()
                    .setQuantity(10)
                    .setNotes("Test")
                    .build();

            // OLD WAY: Using proto builder directly
            com.example.model.v1.OrderItem impl = (com.example.model.v1.OrderItem) wrapper;
            Order.OrderItem modifiedProto = impl.getTypedProto().toBuilder()
                    .setQuantity(10)
                    .setNotes("Test")
                    .build();
            OrderItem viaProtoBuilder = ctx.wrapOrderItem(modifiedProto);

            // Both should produce identical bytes
            byte[] wrapperBytes = viaWrapperBuilder.toBytes();
            byte[] protoBytes = viaProtoBuilder.toBytes();

            assertThat(wrapperBytes).isEqualTo(protoBytes);

            // And identical field values
            assertThat(viaWrapperBuilder.getQuantity()).isEqualTo(viaProtoBuilder.getQuantity());
            assertThat(viaWrapperBuilder.getNotes()).isEqualTo(viaProtoBuilder.getNotes());
        }
    }

    @Nested
    @DisplayName("Complex round-trip with builders")
    class ComplexBuilderRoundTripTest {

        @Test
        @DisplayName("Full workflow: parse -> wrap -> modify -> serialize -> parse")
        void fullWorkflow() throws InvalidProtocolBufferException {
            // Step 1: Create original proto
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("WORKFLOW-001")
                    .setProductName("Original Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(999)
                            .setCurrency("USD")
                            .build())
                    .build();

            // Step 2: Serialize (simulating storage/network)
            byte[] originalBytes = original.toByteArray();

            // Step 3: Parse from bytes (simulating receiving data)
            Order.OrderItem parsed = Order.OrderItem.parseFrom(originalBytes);

            // Step 4: Wrap with version context
            OrderItem wrapper = ctx.wrapOrderItem(parsed);

            // Step 5: Modify via builder (business logic)
            Money newPrice = ctx.wrapMoney(Common.Money.newBuilder()
                    .setAmount(1499)
                    .setCurrency("EUR")
                    .build());

            OrderItem modified = wrapper.toBuilder()
                    .setProductName("Premium Product")
                    .setQuantity(5)
                    .setUnitPrice(newPrice)
                    .setNotes("Upgraded order")
                    .build();

            // Step 6: Serialize modified
            byte[] modifiedBytes = modified.toBytes();

            // Step 7: Parse again (simulating reading stored data)
            Order.OrderItem finalParsed = Order.OrderItem.parseFrom(modifiedBytes);

            // Step 8: Verify all modifications persisted
            assertThat(finalParsed.getProductId()).isEqualTo("WORKFLOW-001");
            assertThat(finalParsed.getProductName()).isEqualTo("Premium Product");
            assertThat(finalParsed.getQuantity()).isEqualTo(5);
            assertThat(finalParsed.getUnitPrice().getAmount()).isEqualTo(1499);
            assertThat(finalParsed.getUnitPrice().getCurrency()).isEqualTo("EUR");
            assertThat(finalParsed.getNotes()).isEqualTo("Upgraded order");
        }
    }

    @Nested
    @DisplayName("V2 builder round-trip")
    class V2BuilderRoundTripTest {

        private final VersionContext ctxV2 = VersionContext.forVersionId("v2");

        @Test
        @DisplayName("V2 builder with version-specific fields")
        void v2BuilderWithNewFields() throws InvalidProtocolBufferException {
            // Create V2 OrderItem with V2-specific fields
            com.example.proto.v2.Order.OrderItem original =
                    com.example.proto.v2.Order.OrderItem.newBuilder()
                            .setProductId("V2-PROD-001")
                            .setProductName("V2 Product")
                            .setQuantity(1)
                            .setUnitPrice(com.example.proto.v2.Common.Money.newBuilder()
                                    .setAmount(1000)
                                    .setCurrency("USD")
                                    .build())
                            .setSku("SKU-123")        // V2-only field
                            .setCategory("Electronics") // V2-only field
                            .build();

            OrderItem wrapper = ctxV2.wrapOrderItem(original);
            assertThat(wrapper.getSku()).isEqualTo("SKU-123");
            assertThat(wrapper.getCategory()).isEqualTo("Electronics");

            // Modify via builder
            OrderItem modified = wrapper.toBuilder()
                    .setQuantity(10)
                    .setSku("SKU-456")
                    .setCategory("Premium Electronics")
                    .build();

            assertThat(modified.getQuantity()).isEqualTo(10);
            assertThat(modified.getSku()).isEqualTo("SKU-456");
            assertThat(modified.getCategory()).isEqualTo("Premium Electronics");

            // Round-trip
            byte[] bytes = modified.toBytes();
            com.example.proto.v2.Order.OrderItem parsed =
                    com.example.proto.v2.Order.OrderItem.parseFrom(bytes);

            assertThat(parsed.getQuantity()).isEqualTo(10);
            assertThat(parsed.getSku()).isEqualTo("SKU-456");
            assertThat(parsed.getCategory()).isEqualTo("Premium Electronics");
        }

        @Test
        @DisplayName("Clear V2-specific fields")
        void clearV2SpecificFields() throws InvalidProtocolBufferException {
            com.example.proto.v2.Order.OrderItem original =
                    com.example.proto.v2.Order.OrderItem.newBuilder()
                            .setProductId("V2-PROD-002")
                            .setProductName("Product")
                            .setQuantity(1)
                            .setUnitPrice(com.example.proto.v2.Common.Money.newBuilder()
                                    .setAmount(500)
                                    .setCurrency("USD")
                                    .build())
                            .setSku("SKU-789")
                            .setCategory("Books")
                            .build();

            OrderItem wrapper = ctxV2.wrapOrderItem(original);
            assertThat(wrapper.hasSku()).isTrue();
            assertThat(wrapper.hasCategory()).isTrue();

            // Clear V2-specific fields
            OrderItem cleared = wrapper.toBuilder()
                    .clearSku()
                    .clearCategory()
                    .build();

            assertThat(cleared.hasSku()).isFalse();
            assertThat(cleared.hasCategory()).isFalse();

            // Round-trip
            byte[] bytes = cleared.toBytes();
            com.example.proto.v2.Order.OrderItem parsed =
                    com.example.proto.v2.Order.OrderItem.parseFrom(bytes);

            assertThat(parsed.hasSku()).isFalse();
            assertThat(parsed.hasCategory()).isFalse();
        }
    }
}
