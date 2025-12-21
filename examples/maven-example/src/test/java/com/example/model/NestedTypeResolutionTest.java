package com.example.model;

import com.example.model.api.*;
import com.example.proto.v1.Invoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested type resolution when proto package differs from java_package.
 *
 * The invoice.proto files use:
 * - proto package: billing.v1 / billing.v2
 * - java_package: com.example.proto.v1 / com.example.proto.v2
 *
 * This mismatch requires special handling in the generator to correctly
 * resolve nested type paths like InvoiceDocument.Fee, InvoiceDocument.LineItem, etc.
 */
@DisplayName("Nested Type Resolution Tests (different proto/java packages)")
class NestedTypeResolutionTest {

    @Nested
    @DisplayName("InvoiceDocument nested types")
    class InvoiceDocumentNestedTypesTest {

        @Test
        @DisplayName("Fee nested type is correctly resolved")
        void feeNestedTypeWorks() {
            Invoice.InvoiceDocument.Fee feeProto = Invoice.InvoiceDocument.Fee.newBuilder()
                    .setFeeCode(1)
                    .setRate(10)
                    .setCalculated(com.example.proto.v1.Common.Money.newBuilder()
                            .setAmount(100)
                            .setCurrency("USD")
                            .build())
                    .build();

            Invoice.InvoiceDocument proto = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-001")
                    .setIssueDate(com.example.proto.v1.Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .addAllFees(feeProto)
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .setFeeAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(100).setCurrency("USD").build())
                            .setTotalDue(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1100).setCurrency("USD").build())
                            .build())
                    .build();

            InvoiceDocument invoice = new com.example.model.v1.InvoiceDocument(proto);

            List<InvoiceDocument.Fee> fees = invoice.getAllFees();
            assertThat(fees).hasSize(1);

            InvoiceDocument.Fee fee = fees.get(0);
            assertThat(fee.getFeeCode()).isEqualTo(1);
            assertThat(fee.getRate()).isEqualTo(10);
            assertThat(fee.getCalculated().getAmount()).isEqualTo(100);
        }

        @Test
        @DisplayName("LineItem with nested Article and Measure works")
        void lineItemWithDeeplyNestedTypesWorks() {
            Invoice.InvoiceDocument.LineItem.Article articleProto =
                    Invoice.InvoiceDocument.LineItem.Article.newBuilder()
                            .setSku("SKU-001")
                            .setTitle("Test Product")
                            .setEan("1234567890123")
                            .build();

            Invoice.InvoiceDocument.LineItem.Measure measureProto =
                    Invoice.InvoiceDocument.LineItem.Measure.newBuilder()
                            .setCount(5)
                            .setUnitName("pcs")
                            .build();

            Invoice.InvoiceDocument.LineItem lineItemProto =
                    Invoice.InvoiceDocument.LineItem.newBuilder()
                            .setArticle(articleProto)
                            .setMeasure(measureProto)
                            .setUnitCost(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(200).setCurrency("USD").build())
                            .setLineTotal(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .build();

            Invoice.InvoiceDocument proto = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-002")
                    .setIssueDate(com.example.proto.v1.Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .addLineItems(lineItemProto)
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .setFeeAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(0).setCurrency("USD").build())
                            .setTotalDue(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .build())
                    .build();

            InvoiceDocument invoice = new com.example.model.v1.InvoiceDocument(proto);

            List<InvoiceDocument.LineItem> items = invoice.getLineItems();
            assertThat(items).hasSize(1);

            InvoiceDocument.LineItem item = items.get(0);

            // Check deeply nested Article
            InvoiceDocument.LineItem.Article article = item.getArticle();
            assertThat(article.getSku()).isEqualTo("SKU-001");
            assertThat(article.getTitle()).isEqualTo("Test Product");
            assertThat(article.getEan()).isEqualTo("1234567890123");

            // Check deeply nested Measure
            InvoiceDocument.LineItem.Measure measure = item.getMeasure();
            assertThat(measure.getCount()).isEqualTo(5);
            assertThat(measure.getUnitName()).isEqualTo("pcs");
        }

        @Test
        @DisplayName("Settlement with enum works")
        void settlementWithEnumWorks() {
            Invoice.InvoiceDocument.Settlement settlementProto =
                    Invoice.InvoiceDocument.Settlement.newBuilder()
                            .setType(Invoice.InvoiceDocument.Settlement.SettlementType.SETTLEMENT_TRANSFER)
                            .setPaid(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(500).setCurrency("EUR").build())
                            .setPaymentRef("PAY-123")
                            .build();

            Invoice.InvoiceDocument proto = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-003")
                    .setIssueDate(com.example.proto.v1.Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .addSettlements(settlementProto)
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(500).setCurrency("EUR").build())
                            .setFeeAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(0).setCurrency("EUR").build())
                            .setTotalDue(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(500).setCurrency("EUR").build())
                            .build())
                    .build();

            InvoiceDocument invoice = new com.example.model.v1.InvoiceDocument(proto);

            List<InvoiceDocument.Settlement> settlements = invoice.getSettlements();
            assertThat(settlements).hasSize(1);

            InvoiceDocument.Settlement settlement = settlements.get(0);
            assertThat(settlement.getType()).isEqualTo(InvoiceDocument.Settlement.SettlementType.SETTLEMENT_TRANSFER);
            assertThat(settlement.getPaid().getAmount()).isEqualTo(500);
            assertThat(settlement.getPaymentRef()).isEqualTo("PAY-123");
        }

        @Test
        @DisplayName("Summary nested type works")
        void summaryNestedTypeWorks() {
            Invoice.InvoiceDocument proto = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-004")
                    .setIssueDate(com.example.proto.v1.Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(800).setCurrency("GBP").build())
                            .setFeeAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(50).setCurrency("GBP").build())
                            .setTotalDue(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(850).setCurrency("GBP").build())
                            .setRebate(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(20).setCurrency("GBP").build())
                            .build())
                    .build();

            InvoiceDocument invoice = new com.example.model.v1.InvoiceDocument(proto);

            InvoiceDocument.Summary summary = invoice.getSummary();
            assertThat(summary.getNetAmount().getAmount()).isEqualTo(800);
            assertThat(summary.getFeeAmount().getAmount()).isEqualTo(50);
            assertThat(summary.getTotalDue().getAmount()).isEqualTo(850);
            assertThat(summary.hasRebate()).isTrue();
            assertThat(summary.getRebate().getAmount()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("InvoiceResult nested types")
    class InvoiceResultNestedTypesTest {

        @Test
        @DisplayName("Outcome nested type works")
        void outcomeNestedTypeWorks() {
            Invoice.InvoiceResult proto = Invoice.InvoiceResult.newBuilder()
                    .setInvoiceNumber("INV-005")
                    .setOutcome(Invoice.InvoiceResult.Outcome.newBuilder()
                            .setAccepted(true)
                            .build())
                    .setConfirmationCode("CONF-123")
                    .build();

            InvoiceResult result = new com.example.model.v1.InvoiceResult(proto);

            assertThat(result.getInvoiceNumber()).isEqualTo("INV-005");

            InvoiceResult.Outcome outcome = result.getOutcome();
            assertThat(outcome.isAccepted()).isTrue();
            assertThat(outcome.hasRejectionCode()).isFalse();
        }

        @Test
        @DisplayName("Outcome with rejection works")
        void outcomeWithRejectionWorks() {
            Invoice.InvoiceResult proto = Invoice.InvoiceResult.newBuilder()
                    .setInvoiceNumber("INV-006")
                    .setOutcome(Invoice.InvoiceResult.Outcome.newBuilder()
                            .setAccepted(false)
                            .setRejectionCode("ERR-001")
                            .setRejectionReason("Invalid amount")
                            .build())
                    .build();

            InvoiceResult result = new com.example.model.v1.InvoiceResult(proto);

            InvoiceResult.Outcome outcome = result.getOutcome();
            assertThat(outcome.isAccepted()).isFalse();
            assertThat(outcome.hasRejectionCode()).isTrue();
            assertThat(outcome.getRejectionCode()).isEqualTo("ERR-001");
            assertThat(outcome.getRejectionReason()).isEqualTo("Invalid amount");
        }
    }

    @Nested
    @DisplayName("V2 specific nested types")
    class V2SpecificNestedTypesTest {

        @Test
        @DisplayName("V2 Shipping nested type works")
        void v2ShippingNestedTypeWorks() {
            com.example.proto.v2.Invoice.InvoiceDocument proto =
                    com.example.proto.v2.Invoice.InvoiceDocument.newBuilder()
                            .setInvoiceNumber("INV-V2-001")
                            .setIssueDate(com.example.proto.v2.Common.Date.newBuilder()
                                    .setYear(2024).setMonth(12).setDay(21).build())
                            .setSummary(com.example.proto.v2.Invoice.InvoiceDocument.Summary.newBuilder()
                                    .setNetAmount(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(1000).setCurrency("USD").build())
                                    .setFeeAmount(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(0).setCurrency("USD").build())
                                    .setTotalDue(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(1000).setCurrency("USD").build())
                                    .build())
                            .setShipping(com.example.proto.v2.Invoice.InvoiceDocument.Shipping.newBuilder()
                                    .setDestination(com.example.proto.v2.Common.Address.newBuilder()
                                            .setStreet("123 Main St")
                                            .setCity("New York")
                                            .setCountry("USA")
                                            .build())
                                    .setCarrier("FedEx")
                                    .setTrackingCode("TRACK-123")
                                    .build())
                            .build();

            InvoiceDocument invoice = new com.example.model.v2.InvoiceDocument(proto);

            assertThat(invoice.hasShipping()).isTrue();

            InvoiceDocument.Shipping shipping = invoice.getShipping();
            assertThat(shipping.getDestination().getStreet()).isEqualTo("123 Main St");
            assertThat(shipping.getCarrier()).isEqualTo("FedEx");
            assertThat(shipping.getTrackingCode()).isEqualTo("TRACK-123");
        }

        @Test
        @DisplayName("V2 LineItem.Warranty nested type works")
        void v2WarrantyNestedTypeWorks() {
            com.example.proto.v2.Invoice.InvoiceDocument.LineItem lineItemProto =
                    com.example.proto.v2.Invoice.InvoiceDocument.LineItem.newBuilder()
                            .setArticle(com.example.proto.v2.Invoice.InvoiceDocument.LineItem.Article.newBuilder()
                                    .setSku("SKU-V2")
                                    .setTitle("V2 Product")
                                    .setCategory("Electronics")
                                    .setBrand("TechBrand")
                                    .build())
                            .setMeasure(com.example.proto.v2.Invoice.InvoiceDocument.LineItem.Measure.newBuilder()
                                    .setCount(1)
                                    .setUnitName("unit")
                                    .setDecimals(2)
                                    .build())
                            .setUnitCost(com.example.proto.v2.Common.Money.newBuilder()
                                    .setAmount(500).setCurrency("USD").build())
                            .setLineTotal(com.example.proto.v2.Common.Money.newBuilder()
                                    .setAmount(500).setCurrency("USD").build())
                            .setWarranty(com.example.proto.v2.Invoice.InvoiceDocument.LineItem.Warranty.newBuilder()
                                    .setMonths(24)
                                    .setTerms("Full coverage")
                                    .build())
                            .setSerialNumber("SN-12345")
                            .build();

            com.example.proto.v2.Invoice.InvoiceDocument proto =
                    com.example.proto.v2.Invoice.InvoiceDocument.newBuilder()
                            .setInvoiceNumber("INV-V2-002")
                            .setIssueDate(com.example.proto.v2.Common.Date.newBuilder()
                                    .setYear(2024).setMonth(12).setDay(21).build())
                            .addLineItems(lineItemProto)
                            .setSummary(com.example.proto.v2.Invoice.InvoiceDocument.Summary.newBuilder()
                                    .setNetAmount(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(500).setCurrency("USD").build())
                                    .setFeeAmount(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(0).setCurrency("USD").build())
                                    .setTotalDue(com.example.proto.v2.Common.Money.newBuilder()
                                            .setAmount(500).setCurrency("USD").build())
                                    .build())
                            .build();

            InvoiceDocument invoice = new com.example.model.v2.InvoiceDocument(proto);
            InvoiceDocument.LineItem item = invoice.getLineItems().get(0);

            // Check V2-only Article fields
            assertThat(item.getArticle().hasCategory()).isTrue();
            assertThat(item.getArticle().getCategory()).isEqualTo("Electronics");
            assertThat(item.getArticle().getBrand()).isEqualTo("TechBrand");

            // Check V2-only Measure fields
            assertThat(item.getMeasure().hasDecimals()).isTrue();
            assertThat(item.getMeasure().getDecimals()).isEqualTo(2);

            // Check V2-only Warranty
            assertThat(item.hasWarranty()).isTrue();
            InvoiceDocument.LineItem.Warranty warranty = item.getWarranty();
            assertThat(warranty.getMonths()).isEqualTo(24);
            assertThat(warranty.getTerms()).isEqualTo("Full coverage");

            // Check V2-only serial number
            assertThat(item.hasSerialNumber()).isTrue();
            assertThat(item.getSerialNumber()).isEqualTo("SN-12345");
        }

        @Test
        @DisplayName("V2 Audit nested type works")
        void v2AuditNestedTypeWorks() {
            com.example.proto.v2.Invoice.InvoiceResult proto =
                    com.example.proto.v2.Invoice.InvoiceResult.newBuilder()
                            .setInvoiceNumber("INV-V2-003")
                            .setOutcome(com.example.proto.v2.Invoice.InvoiceResult.Outcome.newBuilder()
                                    .setAccepted(true)
                                    .build())
                            .setAudit(com.example.proto.v2.Invoice.InvoiceResult.Audit.newBuilder()
                                    .setProcessedBy("admin")
                                    .setProcessedAt(com.example.proto.v2.Common.Date.newBuilder()
                                            .setYear(2024).setMonth(12).setDay(21).build())
                                    .setNotes("Approved")
                                    .build())
                            .build();

            InvoiceResult result = new com.example.model.v2.InvoiceResult(proto);

            assertThat(result.hasAudit()).isTrue();

            InvoiceResult.Audit audit = result.getAudit();
            assertThat(audit.getProcessedBy()).isEqualTo("admin");
            assertThat(audit.getProcessedAt().getYear()).isEqualTo(2024);
            assertThat(audit.getNotes()).isEqualTo("Approved");
        }
    }

    @Nested
    @DisplayName("Cross-version compatibility")
    class CrossVersionCompatibilityTest {

        @Test
        @DisplayName("V1 wrapper handles V2-only nested types gracefully")
        void v1WrapperHandlesV2OnlyTypesGracefully() {
            Invoice.InvoiceDocument proto = Invoice.InvoiceDocument.newBuilder()
                    .setInvoiceNumber("INV-COMPAT")
                    .setIssueDate(com.example.proto.v1.Common.Date.newBuilder()
                            .setYear(2024).setMonth(12).setDay(21).build())
                    .setSummary(Invoice.InvoiceDocument.Summary.newBuilder()
                            .setNetAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .setFeeAmount(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(0).setCurrency("USD").build())
                            .setTotalDue(com.example.proto.v1.Common.Money.newBuilder()
                                    .setAmount(1000).setCurrency("USD").build())
                            .build())
                    .build();

            InvoiceDocument invoice = new com.example.model.v1.InvoiceDocument(proto);

            // V2-only Shipping should return false for hasShipping
            assertThat(invoice.hasShipping()).isFalse();
            assertThat(invoice.getShipping()).isNull();

            // V2-only due_date should return false for hasDueDate
            assertThat(invoice.hasDueDate()).isFalse();
            assertThat(invoice.getDueDate()).isNull();
        }
    }
}
