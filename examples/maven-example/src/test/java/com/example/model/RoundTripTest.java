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
 * Tests for round-trip workflows:
 * bytes -> proto -> wrapper -> read/modify -> proto -> bytes
 */
@DisplayName("Round-Trip Tests")
class RoundTripTest {

    @Nested
    @DisplayName("Basic round-trip")
    class BasicRoundTripTest {

        @Test
        @DisplayName("Parse bytes, wrap, serialize back")
        void parseWrapSerialize() throws InvalidProtocolBufferException {
            // Create and serialize original proto
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("PROD-001")
                    .setProductName("Test Product")
                    .setQuantity(5)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build())
                    .build();

            byte[] originalBytes = original.toByteArray();

            // Parse from bytes
            Order.OrderItem parsed = Order.OrderItem.parseFrom(originalBytes);

            // Wrap with version context
            VersionContext ctx = VersionContext.forVersionId("v1");
            OrderItem wrapper = ctx.wrapOrderItem(parsed);

            // Serialize via wrapper
            byte[] wrapperBytes = wrapper.toBytes();

            // Verify bytes are identical
            assertThat(wrapperBytes).isEqualTo(originalBytes);

            // Parse again and verify fields
            Order.OrderItem verified = Order.OrderItem.parseFrom(wrapperBytes);
            assertThat(verified.getProductId()).isEqualTo("PROD-001");
            assertThat(verified.getProductName()).isEqualTo("Test Product");
            assertThat(verified.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Read all fields via wrapper after parsing")
        void readFieldsAfterParsing() throws InvalidProtocolBufferException {
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("SKU-123")
                    .setProductName("Gadget")
                    .setQuantity(3)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(2500)
                            .setCurrency("EUR")
                            .build())
                    .setDiscount(Order.OrderItem.Discount.newBuilder()
                            .setType(Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                            .setValue(10)
                            .setCode("DEAL10")
                            .build())
                    .setNotes("Handle with care")
                    .build();

            byte[] bytes = original.toByteArray();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);

            OrderItem wrapper = VersionContext.forVersionId("v1").wrapOrderItem(parsed);

            // Verify all fields readable
            assertThat(wrapper.getProductId()).isEqualTo("SKU-123");
            assertThat(wrapper.getProductName()).isEqualTo("Gadget");
            assertThat(wrapper.getQuantity()).isEqualTo(3);
            assertThat(wrapper.getUnitPrice().getAmount()).isEqualTo(2500);
            assertThat(wrapper.getUnitPrice().getCurrency()).isEqualTo("EUR");

            assertThat(wrapper.hasDiscount()).isTrue();
            assertThat(wrapper.getDiscount().getType())
                    .isEqualTo(OrderItem.Discount.DiscountType.PERCENTAGE);
            assertThat(wrapper.getDiscount().getValue()).isEqualTo(10);
            assertThat(wrapper.getDiscount().getCode()).isEqualTo("DEAL10");

            assertThat(wrapper.hasNotes()).isTrue();
            assertThat(wrapper.getNotes()).isEqualTo("Handle with care");
        }
    }

    @Nested
    @DisplayName("Modify and round-trip")
    class ModifyRoundTripTest {

        @Test
        @DisplayName("Modify proto via builder and re-wrap")
        void modifyViaBuilder() throws InvalidProtocolBufferException {
            // Original
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("ITEM-001")
                    .setProductName("Original Name")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(100)
                            .setCurrency("USD")
                            .build())
                    .build();

            // Wrap original
            VersionContext ctx = VersionContext.forVersionId("v1");
            OrderItem wrapper = ctx.wrapOrderItem(original);
            assertThat(wrapper.getQuantity()).isEqualTo(1);

            // Get typed proto and modify
            com.example.model.v1.OrderItem implWrapper = (com.example.model.v1.OrderItem) wrapper;
            Order.OrderItem typedProto = implWrapper.getTypedProto();

            Order.OrderItem modified = typedProto.toBuilder()
                    .setProductName("Modified Name")
                    .setQuantity(99)
                    .build();

            // Wrap modified
            OrderItem modifiedWrapper = ctx.wrapOrderItem(modified);

            // Verify modifications
            assertThat(modifiedWrapper.getProductName()).isEqualTo("Modified Name");
            assertThat(modifiedWrapper.getQuantity()).isEqualTo(99);
            // Original fields preserved
            assertThat(modifiedWrapper.getProductId()).isEqualTo("ITEM-001");
            assertThat(modifiedWrapper.getUnitPrice().getAmount()).isEqualTo(100);
        }

        @Test
        @DisplayName("Add optional field and round-trip")
        void addOptionalField() throws InvalidProtocolBufferException {
            // Original without notes
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("ITEM-002")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(500)
                            .setCurrency("USD")
                            .build())
                    .build();

            VersionContext ctx = VersionContext.forVersionId("v1");
            OrderItem wrapper = ctx.wrapOrderItem(original);
            assertThat(wrapper.hasNotes()).isFalse();

            // Add notes
            com.example.model.v1.OrderItem implWrapper = (com.example.model.v1.OrderItem) wrapper;
            Order.OrderItem modified = implWrapper.getTypedProto().toBuilder()
                    .setNotes("Added note")
                    .build();

            // Serialize and parse
            byte[] bytes = modified.toByteArray();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);
            OrderItem parsedWrapper = ctx.wrapOrderItem(parsed);

            // Verify note persisted
            assertThat(parsedWrapper.hasNotes()).isTrue();
            assertThat(parsedWrapper.getNotes()).isEqualTo("Added note");
        }

        @Test
        @DisplayName("Modify nested message")
        void modifyNestedMessage() throws InvalidProtocolBufferException {
            // Original with discount
            Order.OrderItem original = Order.OrderItem.newBuilder()
                    .setProductId("ITEM-003")
                    .setProductName("Product")
                    .setQuantity(1)
                    .setUnitPrice(Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build())
                    .setDiscount(Order.OrderItem.Discount.newBuilder()
                            .setType(Order.OrderItem.Discount.DiscountType.PERCENTAGE)
                            .setValue(5)
                            .build())
                    .build();

            VersionContext ctx = VersionContext.forVersionId("v1");
            OrderItem wrapper = ctx.wrapOrderItem(original);
            assertThat(wrapper.getDiscount().getValue()).isEqualTo(5);

            // Modify discount
            com.example.model.v1.OrderItem implWrapper = (com.example.model.v1.OrderItem) wrapper;
            Order.OrderItem modified = implWrapper.getTypedProto().toBuilder()
                    .setDiscount(Order.OrderItem.Discount.newBuilder()
                            .setType(Order.OrderItem.Discount.DiscountType.FIXED_AMOUNT)
                            .setValue(200)
                            .setCode("FLAT200")
                            .build())
                    .build();

            // Round-trip
            byte[] bytes = modified.toByteArray();
            Order.OrderItem parsed = Order.OrderItem.parseFrom(bytes);
            OrderItem parsedWrapper = ctx.wrapOrderItem(parsed);

            // Verify
            assertThat(parsedWrapper.getDiscount().getType())
                    .isEqualTo(OrderItem.Discount.DiscountType.FIXED_AMOUNT);
            assertThat(parsedWrapper.getDiscount().getValue()).isEqualTo(200);
            assertThat(parsedWrapper.getDiscount().getCode()).isEqualTo("FLAT200");
        }
    }

    @Nested
    @DisplayName("Cross-version round-trip")
    class CrossVersionRoundTripTest {

        @Test
        @DisplayName("V1 data read via V2 context returns defaults for new fields")
        void v1DataViaV2Context() throws InvalidProtocolBufferException {
            // Create V1 Money
            Common.Money v1Money = Common.Money.newBuilder()
                    .setAmount(5000)
                    .setCurrency("GBP")
                    .build();

            byte[] bytes = v1Money.toByteArray();

            // Parse as V2 (V2 has additional fields like exchange_rate)
            com.example.proto.v2.Common.Money v2Parsed =
                    com.example.proto.v2.Common.Money.parseFrom(bytes);

            // Wrap with V2 context
            Money wrapper = VersionContext.forVersionId("v2").wrapMoney(v2Parsed);

            // V1 fields present
            assertThat(wrapper.getAmount()).isEqualTo(5000);
            assertThat(wrapper.getCurrency()).isEqualTo("GBP");

            // V2-only fields return defaults
            assertThat(wrapper.hasExchangeRate()).isFalse();
            assertThat(wrapper.hasOriginalCurrency()).isFalse();
        }

        @Test
        @DisplayName("V2 data with new fields serializes and parses correctly")
        void v2DataRoundTrip() throws InvalidProtocolBufferException {
            // Create V2 Money with V2-only fields
            com.example.proto.v2.Common.Money v2Money =
                    com.example.proto.v2.Common.Money.newBuilder()
                            .setAmount(10000)
                            .setCurrency("EUR")
                            .setExchangeRate(1.08)
                            .setOriginalCurrency("USD")
                            .build();

            byte[] bytes = v2Money.toByteArray();

            // Parse and wrap
            com.example.proto.v2.Common.Money parsed =
                    com.example.proto.v2.Common.Money.parseFrom(bytes);
            Money wrapper = VersionContext.forVersionId("v2").wrapMoney(parsed);

            // All fields present
            assertThat(wrapper.getAmount()).isEqualTo(10000);
            assertThat(wrapper.getCurrency()).isEqualTo("EUR");
            assertThat(wrapper.hasExchangeRate()).isTrue();
            assertThat(wrapper.getExchangeRate()).isEqualTo(1.08);
            assertThat(wrapper.hasOriginalCurrency()).isTrue();
            assertThat(wrapper.getOriginalCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("Complex nested round-trip")
    class ComplexNestedRoundTripTest {

        @Test
        @DisplayName("InvoiceDocument with deeply nested types")
        void invoiceDocumentRoundTrip() throws InvalidProtocolBufferException {
            // Create complex invoice
            Invoice.InvoiceDocument original = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-2024-001")
                    .setIssueDate(Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .addLineItems(Invoice.InvoiceDocument.LineItem.newBuilder()
                            .setArticle(Invoice.InvoiceDocument.LineItem.Article.newBuilder()
                                    .setSku("SKU-001")
                                    .setTitle("Widget")
                                    .setEan("1234567890123")
                                    .build())
                            .setMeasure(Invoice.InvoiceDocument.LineItem.Measure.newBuilder()
                                    .setCount(10)
                                    .setUnitName("pcs")
                                    .build())
                            .setUnitCost(Common.Money.newBuilder()
                                    .setAmount(100).setCurrency("USD").build())
                            .setLineTotal(Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .addFees(Invoice.InvoiceDocument.Fee.newBuilder()
                                    .setFeeCode(1)
                                    .setRate(10)
                                    .setCalculated(Common.Money.newBuilder()
                                            .setAmount(100).setCurrency("USD").build())
                                    .build())
                            .build())
                    .addSettlements(Invoice.InvoiceDocument.Settlement.newBuilder()
                            .setType(Invoice.InvoiceDocument.Settlement.SettlementType.SETTLEMENT_TRANSFER)
                            .setPaid(Common.Money.newBuilder()
                                    .setAmount(1100).setCurrency("USD").build())
                            .setPaymentRef("TXN-123")
                            .build())
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .setFeeAmount(Common.Money.newBuilder()
                                    .setAmount(100).setCurrency("USD").build())
                            .setTotalDue(Common.Money.newBuilder()
                                    .setAmount(1100).setCurrency("USD").build())
                            .build())
                    .setMemo("Test invoice")
                    .build();

            // Serialize
            byte[] bytes = original.toByteArray();

            // Parse and wrap
            Invoice.InvoiceDocument parsed = Invoice.InvoiceDocument.parseFrom(bytes);
            InvoiceDocument wrapper = VersionContext.forVersionId("v1").wrapInvoiceDocument(parsed);

            // Verify all nested levels
            assertThat(wrapper.getInvoiceNumber()).isEqualTo("INV-2024-001");
            assertThat(wrapper.getIssueDate().getYear()).isEqualTo(2024);

            // LineItem
            assertThat(wrapper.getLineItems()).hasSize(1);
            InvoiceDocument.LineItem item = wrapper.getLineItems().get(0);
            assertThat(item.getArticle().getSku()).isEqualTo("SKU-001");
            assertThat(item.getArticle().getTitle()).isEqualTo("Widget");
            assertThat(item.getMeasure().getCount()).isEqualTo(10);
            assertThat(item.getFees()).hasSize(1);
            assertThat(item.getFees().get(0).getRate()).isEqualTo(10);

            // Settlement
            assertThat(wrapper.getSettlements()).hasSize(1);
            assertThat(wrapper.getSettlements().get(0).getType())
                    .isEqualTo(InvoiceDocument.Settlement.SettlementType.SETTLEMENT_TRANSFER);
            assertThat(wrapper.getSettlements().get(0).getPaymentRef()).isEqualTo("TXN-123");

            // Summary
            assertThat(wrapper.getSummary().getTotalDue().getAmount()).isEqualTo(1100);

            // Re-serialize and verify
            byte[] reserializedBytes = wrapper.toBytes();
            assertThat(reserializedBytes).isEqualTo(bytes);
        }
    }

    @Nested
    @DisplayName("Version identification")
    class VersionIdentificationTest {

        @Test
        @DisplayName("Wrapper correctly reports its version")
        void wrapperReportsVersion() {
            Common.Money v1Proto = Common.Money.newBuilder()
                    .setAmount(100).setCurrency("USD").build();

            com.example.proto.v2.Common.Money v2Proto =
                    com.example.proto.v2.Common.Money.newBuilder()
                            .setAmount(200).setCurrency("EUR").build();

            Money v1Wrapper = VersionContext.forVersionId("v1").wrapMoney(v1Proto);
            Money v2Wrapper = VersionContext.forVersionId("v2").wrapMoney(v2Proto);

            assertThat(v1Wrapper.getWrapperVersionId()).isEqualTo("v1");
            assertThat(v2Wrapper.getWrapperVersionId()).isEqualTo("v2");
        }

        @Test
        @DisplayName("Can route processing based on version")
        void routeByVersion() {
            Common.Money proto = Common.Money.newBuilder()
                    .setAmount(100).setCurrency("USD").build();

            Money wrapper = VersionContext.forVersionId("v1").wrapMoney(proto);

            String result = switch (wrapper.getWrapperVersionId()) {
                case "v1" -> "Processing V1 format";
                case "v2" -> "Processing V2 format";
                default -> "Unknown version";
            };

            assertThat(result).isEqualTo("Processing V1 format");
        }
    }
}
