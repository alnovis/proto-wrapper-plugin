package io.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.alnovis.protowrapper.it.model.api.ParentRef;
import io.alnovis.protowrapper.it.model.api.ShipmentRequest;
import io.alnovis.protowrapper.it.model.api.TransferRecord;
import io.alnovis.protowrapper.it.model.api.VersionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for field mapping feature.
 *
 * <p>Tests name-based and explicit number-based field mapping that allows
 * matching fields by name when field numbers differ across protocol versions.</p>
 *
 * <h2>Test scenarios:</h2>
 * <ol>
 *   <li><b>ShipmentRequest</b> (name-based mapping):
 *     <ul>
 *       <li>parent_ref: field 17 in v1, field 15 in v2 — matched by name</li>
 *       <li>doc_number: field 15 in v1 only — displaced by parent_ref in v2</li>
 *       <li>priority: field 16 in v2 only — new field in v2</li>
 *     </ul>
 *   </li>
 *   <li><b>TransferRecord</b> (explicit number mapping):
 *     <ul>
 *       <li>amount: field 10 in v1, field 8 in v2 — matched by explicit numbers</li>
 *       <li>reference: field 4 in v2 only — new field in v2</li>
 *     </ul>
 *   </li>
 * </ol>
 */
@DisplayName("Field Mapping Integration Tests")
public class FieldMappingIntegrationTest {

    @Nested
    @DisplayName("Name-based mapping: ShipmentRequest.parent_ref")
    class NameBasedMappingTest {

        @Test
        @DisplayName("parent_ref is accessible in v1 (field 17)")
        void parentRefAccessibleInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            ParentRef parentRef = ctx.newParentRefBuilder()
                    .setId(100L)
                    .setCode("REF-001")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("Shipment A")
                    .setParentRef(parentRef)
                    .build();

            assertTrue(request.hasParentRef());
            assertEquals(100L, request.getParentRef().getId());
            assertEquals("REF-001", request.getParentRef().getCode());
        }

        @Test
        @DisplayName("parent_ref is accessible in v2 (field 15)")
        void parentRefAccessibleInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");

            ParentRef parentRef = ctx.newParentRefBuilder()
                    .setId(200L)
                    .setCode("REF-002")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("Shipment B")
                    .setParentRef(parentRef)
                    .build();

            assertTrue(request.hasParentRef());
            assertEquals(200L, request.getParentRef().getId());
            assertEquals("REF-002", request.getParentRef().getCode());
        }

        @Test
        @DisplayName("parent_ref works in both versions with same interface")
        void parentRefSameInterfaceBothVersions() {
            // Build in v1
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            ParentRef ref1 = ctx1.newParentRefBuilder()
                    .setId(42L)
                    .setCode("SHARED")
                    .build();
            ShipmentRequest req1 = ctx1.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("V1 Shipment")
                    .setParentRef(ref1)
                    .build();

            // Build in v2
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            ParentRef ref2 = ctx2.newParentRefBuilder()
                    .setId(42L)
                    .setCode("SHARED")
                    .build();
            ShipmentRequest req2 = ctx2.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("V2 Shipment")
                    .setParentRef(ref2)
                    .build();

            // Both have the same field accessible through the same interface
            assertEquals(req1.getParentRef().getId(), req2.getParentRef().getId());
            assertEquals(req1.getParentRef().getCode(), req2.getParentRef().getCode());
        }

        @Test
        @DisplayName("parent_ref hasParentRef() returns false when not set in both versions")
        void parentRefHasReturnsFalseWhenNotSet() {
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            ShipmentRequest req1 = ctx1.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("Test")
                    .build();

            VersionContext ctx2 = VersionContext.forVersionId("v2");
            ShipmentRequest req2 = ctx2.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("Test")
                    .build();

            // parent_ref is a common field (mapped by name) — no supportsXxx()
            // but hasXxx() should return false when not set
            assertFalse(req1.hasParentRef(),
                    "parent_ref should not be set in v1");
            assertFalse(req2.hasParentRef(),
                    "parent_ref should not be set in v2");
        }

        @Test
        @DisplayName("parent_ref returns default instance when not set in v1")
        void parentRefDefaultInstanceWhenNotSetV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("No Parent")
                    .build();

            assertFalse(request.hasParentRef());
            assertNotNull(request.getParentRef());
        }

        @Test
        @DisplayName("parent_ref returns default instance when not set in v2")
        void parentRefDefaultInstanceWhenNotSetV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("No Parent")
                    .build();

            assertFalse(request.hasParentRef());
            assertNotNull(request.getParentRef());
        }

        @Test
        @DisplayName("parent_ref clear works in v1")
        void parentRefClearInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(99L)
                    .setCode("TO_CLEAR")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("Clear Test")
                    .setParentRef(ref)
                    .clearParentRef()
                    .build();

            assertFalse(request.hasParentRef());
            assertNotNull(request.getParentRef());
        }

        @Test
        @DisplayName("parent_ref clear works in v2")
        void parentRefClearInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(99L)
                    .setCode("TO_CLEAR")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("Clear Test")
                    .setParentRef(ref)
                    .clearParentRef()
                    .build();

            assertFalse(request.hasParentRef());
            assertNotNull(request.getParentRef());
        }
    }

    @Nested
    @DisplayName("Displaced field: ShipmentRequest.doc_number (v1-only)")
    class DisplacedFieldTest {

        @Test
        @DisplayName("doc_number is accessible in v1 (field 15)")
        void docNumberAccessibleInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("With Doc")
                    .setDocNumber(12345)
                    .build();

            assertTrue(request.hasDocNumber());
            assertEquals(Integer.valueOf(12345), request.getDocNumber());
        }

        @Test
        @DisplayName("doc_number is v1-only — not supported in v2")
        void docNumberNotSupportedInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("No Doc")
                    .build();

            assertFalse(request.supportsDocNumber(),
                    "doc_number should NOT be supported in v2");
            assertNull(request.getDocNumber());
            assertFalse(request.hasDocNumber());
        }

        @Test
        @DisplayName("setting doc_number in v2 builder throws UnsupportedOperationException")
        void setDocNumberInV2Throws() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ShipmentRequest.Builder builder = ctx.newShipmentRequestBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setDocNumber(999),
                    "Setting v1-only field 'docNumber' in v2 should throw"
            );

            assertTrue(ex.getMessage().contains("docNumber"),
                    "Error should contain field name: " + ex.getMessage());
        }

        @Test
        @DisplayName("clearing doc_number in v2 is safe no-op")
        void clearDocNumberInV2Safe() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ShipmentRequest.Builder builder = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("Test");

            assertDoesNotThrow(() -> builder.clearDocNumber(),
                    "Clearing non-existent field should be safe");
        }

        @Test
        @DisplayName("doc_number and parent_ref coexist in v1 at different field numbers")
        void docNumberAndParentRefCoexistInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(10L)
                    .setCode("P-001")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("Both Fields")
                    .setDocNumber(777)
                    .setParentRef(ref)
                    .build();

            // Both fields should coexist in v1
            assertTrue(request.hasDocNumber());
            assertEquals(Integer.valueOf(777), request.getDocNumber());
            assertTrue(request.hasParentRef());
            assertEquals(10L, request.getParentRef().getId());
        }
    }

    @Nested
    @DisplayName("V2-only field: ShipmentRequest.priority")
    class V2OnlyFieldTest {

        @Test
        @DisplayName("priority is accessible in v2 (field 16)")
        void priorityAccessibleInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("High Priority")
                    .setPriority("HIGH")
                    .build();

            assertTrue(request.hasPriority());
            assertEquals("HIGH", request.getPriority());
        }

        @Test
        @DisplayName("priority is not supported in v1")
        void priorityNotSupportedInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("Test")
                    .build();

            assertFalse(request.supportsPriority(),
                    "priority should NOT be supported in v1");
            assertNull(request.getPriority());
        }

        @Test
        @DisplayName("setting priority in v1 throws UnsupportedOperationException")
        void setPriorityInV1Throws() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ShipmentRequest.Builder builder = ctx.newShipmentRequestBuilder();

            assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setPriority("HIGH"),
                    "Setting v2-only field 'priority' in v1 should throw"
            );
        }
    }

    @Nested
    @DisplayName("Explicit number mapping: TransferRecord.amount")
    class ExplicitNumberMappingTest {

        @Test
        @DisplayName("amount is accessible in v1 (field 10)")
        void amountAccessibleInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("Transfer A")
                    .setAmount(50000L)
                    .build();

            assertTrue(record.hasAmount());
            assertEquals(Long.valueOf(50000L), record.getAmount());
        }

        @Test
        @DisplayName("amount is accessible in v2 (field 8)")
        void amountAccessibleInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("Transfer B")
                    .setAmount(75000L)
                    .build();

            assertTrue(record.hasAmount());
            assertEquals(Long.valueOf(75000L), record.getAmount());
        }

        @Test
        @DisplayName("amount works in both versions with same interface")
        void amountSameInterfaceBothVersions() {
            // v1
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            TransferRecord rec1 = ctx1.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("V1 Transfer")
                    .setAmount(10000L)
                    .setCurrency("USD")
                    .build();

            // v2
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            TransferRecord rec2 = ctx2.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("V2 Transfer")
                    .setAmount(10000L)
                    .setCurrency("USD")
                    .build();

            assertEquals(rec1.getAmount(), rec2.getAmount());
            assertEquals(rec1.getCurrency(), rec2.getCurrency());
        }

        @Test
        @DisplayName("amount hasAmount() returns false when not set in both versions")
        void amountHasReturnsFalseWhenNotSet() {
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            TransferRecord rec1 = ctx1.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("Test")
                    .build();

            VersionContext ctx2 = VersionContext.forVersionId("v2");
            TransferRecord rec2 = ctx2.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("Test")
                    .build();

            // amount is a common field (mapped by explicit numbers) — no supportsXxx()
            // but hasXxx() should return false when not set
            assertFalse(rec1.hasAmount(),
                    "amount should not be set in v1");
            assertFalse(rec2.hasAmount(),
                    "amount should not be set in v2");
        }

        @Test
        @DisplayName("amount is null when not set in v1")
        void amountNullWhenNotSetV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("No Amount")
                    .build();

            assertFalse(record.hasAmount());
            assertNull(record.getAmount());
        }

        @Test
        @DisplayName("amount is null when not set in v2")
        void amountNullWhenNotSetV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("No Amount")
                    .build();

            assertFalse(record.hasAmount());
            assertNull(record.getAmount());
        }

        @Test
        @DisplayName("amount clear works in both versions")
        void amountClearWorks() {
            // v1
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            TransferRecord rec1 = ctx1.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("Clear Test")
                    .setAmount(99999L)
                    .clearAmount()
                    .build();
            assertFalse(rec1.hasAmount());
            assertNull(rec1.getAmount());

            // v2
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            TransferRecord rec2 = ctx2.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("Clear Test")
                    .setAmount(99999L)
                    .clearAmount()
                    .build();
            assertFalse(rec2.hasAmount());
            assertNull(rec2.getAmount());
        }
    }

    @Nested
    @DisplayName("V2-only field: TransferRecord.reference")
    class TransferV2OnlyFieldTest {

        @Test
        @DisplayName("reference is accessible in v2 (field 4)")
        void referenceAccessibleInV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("With Ref")
                    .setReference("TXN-12345")
                    .build();

            assertTrue(record.hasReference());
            assertEquals("TXN-12345", record.getReference());
        }

        @Test
        @DisplayName("reference is not supported in v1")
        void referenceNotSupportedInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("Test")
                    .build();

            assertFalse(record.supportsReference(),
                    "reference should NOT be supported in v1");
            assertNull(record.getReference());
        }

        @Test
        @DisplayName("setting reference in v1 throws UnsupportedOperationException")
        void setReferenceInV1Throws() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord.Builder builder = ctx.newTransferRecordBuilder();

            assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setReference("REF-X"),
                    "Setting v2-only field 'reference' in v1 should throw"
            );
        }
    }

    @Nested
    @DisplayName("ParentRef nested message across versions")
    class ParentRefNestedMessageTest {

        @Test
        @DisplayName("ParentRef basic fields work in v1")
        void parentRefBasicFieldsV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(42L)
                    .setCode("ABC")
                    .build();

            assertEquals(42L, ref.getId());
            assertEquals("ABC", ref.getCode());
        }

        @Test
        @DisplayName("ParentRef basic fields work in v2")
        void parentRefBasicFieldsV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(42L)
                    .setCode("ABC")
                    .build();

            assertEquals(42L, ref.getId());
            assertEquals("ABC", ref.getCode());
        }

        @Test
        @DisplayName("ParentRef v2-only label field")
        void parentRefLabelV2Only() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(1L)
                    .setCode("X")
                    .setLabel("Parent Label")
                    .build();

            assertTrue(ref.hasLabel());
            assertEquals("Parent Label", ref.getLabel());
        }

        @Test
        @DisplayName("ParentRef label not supported in v1")
        void parentRefLabelNotInV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(1L)
                    .setCode("X")
                    .build();

            assertFalse(ref.supportsLabel(),
                    "label should NOT be supported in v1");
            assertNull(ref.getLabel());
        }
    }

    @Nested
    @DisplayName("Combined scenarios — mapped and displaced fields together")
    class CombinedScenariosTest {

        @Test
        @DisplayName("Full v1 ShipmentRequest with all fields set")
        void fullV1ShipmentRequest() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(50L)
                    .setCode("PARENT-50")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(100L)
                    .setTitle("Full V1 Shipment")
                    .setDocNumber(42)
                    .setParentRef(ref)
                    .build();

            // All v1 fields should be accessible
            assertEquals(100L, request.getId());
            assertEquals("Full V1 Shipment", request.getTitle());
            assertEquals(Integer.valueOf(42), request.getDocNumber());
            assertEquals(50L, request.getParentRef().getId());
            assertEquals("PARENT-50", request.getParentRef().getCode());

            // v2-only field should not be supported
            assertFalse(request.supportsPriority());
            assertNull(request.getPriority());
        }

        @Test
        @DisplayName("Full v2 ShipmentRequest with all fields set")
        void fullV2ShipmentRequest() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(60L)
                    .setCode("PARENT-60")
                    .setLabel("Important")
                    .build();

            ShipmentRequest request = ctx.newShipmentRequestBuilder()
                    .setId(200L)
                    .setTitle("Full V2 Shipment")
                    .setParentRef(ref)
                    .setPriority("URGENT")
                    .build();

            // All v2 fields should be accessible
            assertEquals(200L, request.getId());
            assertEquals("Full V2 Shipment", request.getTitle());
            assertEquals(60L, request.getParentRef().getId());
            assertEquals("PARENT-60", request.getParentRef().getCode());
            assertEquals("URGENT", request.getPriority());

            // v1-only field should not be supported
            assertFalse(request.supportsDocNumber());
            assertNull(request.getDocNumber());
        }

        @Test
        @DisplayName("Full v1 TransferRecord with all fields set")
        void fullV1TransferRecord() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(1L)
                    .setDescription("V1 Transfer")
                    .setCurrency("KZT")
                    .setAmount(100000L)
                    .build();

            assertEquals(1L, record.getId());
            assertEquals("V1 Transfer", record.getDescription());
            assertEquals("KZT", record.getCurrency());
            assertEquals(Long.valueOf(100000L), record.getAmount());

            // v2-only
            assertFalse(record.supportsReference());
            assertNull(record.getReference());
        }

        @Test
        @DisplayName("Full v2 TransferRecord with all fields set")
        void fullV2TransferRecord() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(2L)
                    .setDescription("V2 Transfer")
                    .setCurrency("USD")
                    .setAmount(50000L)
                    .setReference("REF-999")
                    .build();

            assertEquals(2L, record.getId());
            assertEquals("V2 Transfer", record.getDescription());
            assertEquals("USD", record.getCurrency());
            assertEquals(Long.valueOf(50000L), record.getAmount());
            assertEquals("REF-999", record.getReference());
        }

        @Test
        @DisplayName("Version ID is correct on built objects")
        void versionIdCorrectOnBuiltObjects() {
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            ShipmentRequest req1 = ctx1.newShipmentRequestBuilder()
                    .setId(1L)
                    .setTitle("V1")
                    .build();
            assertEquals("v1", req1.getWrapperVersionId());

            VersionContext ctx2 = VersionContext.forVersionId("v2");
            ShipmentRequest req2 = ctx2.newShipmentRequestBuilder()
                    .setId(2L)
                    .setTitle("V2")
                    .build();
            assertEquals("v2", req2.getWrapperVersionId());
        }
    }

    @Nested
    @DisplayName("Builder round-trip — set, build, verify")
    class BuilderRoundTripTest {

        @Test
        @DisplayName("ShipmentRequest builder round-trip in v1")
        void shipmentRequestRoundTripV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(7L)
                    .setCode("ROUND-TRIP")
                    .build();

            ShipmentRequest original = ctx.newShipmentRequestBuilder()
                    .setId(55L)
                    .setTitle("Round Trip V1")
                    .setDocNumber(99)
                    .setParentRef(ref)
                    .build();

            // Verify all fields survived the build
            assertEquals(55L, original.getId());
            assertEquals("Round Trip V1", original.getTitle());
            assertEquals(Integer.valueOf(99), original.getDocNumber());
            assertNotNull(original.getParentRef());
            assertEquals(7L, original.getParentRef().getId());
            assertEquals("ROUND-TRIP", original.getParentRef().getCode());
        }

        @Test
        @DisplayName("ShipmentRequest builder round-trip in v2")
        void shipmentRequestRoundTripV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");

            ParentRef ref = ctx.newParentRefBuilder()
                    .setId(8L)
                    .setCode("ROUND-TRIP-V2")
                    .setLabel("Test Label")
                    .build();

            ShipmentRequest original = ctx.newShipmentRequestBuilder()
                    .setId(66L)
                    .setTitle("Round Trip V2")
                    .setParentRef(ref)
                    .setPriority("LOW")
                    .build();

            assertEquals(66L, original.getId());
            assertEquals("Round Trip V2", original.getTitle());
            assertNotNull(original.getParentRef());
            assertEquals(8L, original.getParentRef().getId());
            assertEquals("ROUND-TRIP-V2", original.getParentRef().getCode());
            assertEquals("Test Label", original.getParentRef().getLabel());
            assertEquals("LOW", original.getPriority());
        }

        @Test
        @DisplayName("TransferRecord builder round-trip in v1")
        void transferRecordRoundTripV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(11L)
                    .setDescription("RT V1")
                    .setCurrency("EUR")
                    .setAmount(25000L)
                    .build();

            assertEquals(11L, record.getId());
            assertEquals("RT V1", record.getDescription());
            assertEquals("EUR", record.getCurrency());
            assertEquals(Long.valueOf(25000L), record.getAmount());
        }

        @Test
        @DisplayName("TransferRecord builder round-trip in v2")
        void transferRecordRoundTripV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");
            TransferRecord record = ctx.newTransferRecordBuilder()
                    .setId(22L)
                    .setDescription("RT V2")
                    .setCurrency("GBP")
                    .setAmount(75000L)
                    .setReference("TX-007")
                    .build();

            assertEquals(22L, record.getId());
            assertEquals("RT V2", record.getDescription());
            assertEquals("GBP", record.getCurrency());
            assertEquals(Long.valueOf(75000L), record.getAmount());
            assertEquals("TX-007", record.getReference());
        }
    }
}
