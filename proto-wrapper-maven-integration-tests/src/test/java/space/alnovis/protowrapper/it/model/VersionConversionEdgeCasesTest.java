package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.alnovis.protowrapper.it.model.api.Date;
import space.alnovis.protowrapper.it.model.api.Status;
import space.alnovis.protowrapper.it.model.api.VersionContext;
import space.alnovis.protowrapper.it.model.api.VersionSpecificFields;

import space.alnovis.protowrapper.it.model.api.Priority;

import static org.junit.jupiter.api.Assertions.*;

// Version-specific classes for asVersion() calls
// Using fully qualified names to avoid confusion with api.VersionSpecificFields

/**
 * Tests to investigate current behavior of version conversion edge cases.
 *
 * <p>This test class documents the CURRENT behavior, which may include
 * silent data loss that needs to be fixed.</p>
 */
@DisplayName("Version Conversion Edge Cases - Research")
public class VersionConversionEdgeCasesTest {

    @Nested
    @DisplayName("asVersion() data loss investigation")
    class AsVersionDataLoss {

        @Test
        @DisplayName("RESEARCH: v1 -> v2 conversion loses v1-only fields (legacyCode, oldFormat)")
        void v1ToV2LosesV1OnlyFields() {
            // Arrange: Create v1 object with v1-only fields populated
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("test-id")
                    .setName("test-name")
                    .setStatus(2)
                    .setDescription("description")
                    .setLegacyCode("LEGACY-123")  // v1-only
                    .setOldFormat(999)             // v1-only
                    .build();

            // Verify v1 has the data
            assertEquals("LEGACY-123", v1.getLegacyCode());
            assertEquals(Integer.valueOf(999), v1.getOldFormat());
            assertTrue(v1.hasLegacyCode());
            assertTrue(v1.hasOldFormat());

            // Act: Convert to v2
            VersionSpecificFields v2 = v1.asVersion(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class);

            // Assert: Document current behavior
            // CURRENT BEHAVIOR: Data is SILENTLY LOST
            assertNull(v2.getLegacyCode(),
                "CURRENT BEHAVIOR: legacyCode is null after conversion (DATA LOST)");
            assertNull(v2.getOldFormat(),
                "CURRENT BEHAVIOR: oldFormat is null after conversion (DATA LOST)");
            assertFalse(v2.hasLegacyCode());
            assertFalse(v2.hasOldFormat());

            // Common fields should be preserved
            assertEquals("test-id", v2.getId());
            assertEquals("test-name", v2.getName());
            assertEquals(2, v2.getStatus());
            assertEquals("description", v2.getDescription());

            // v2 version
            assertEquals(2, v2.getWrapperVersion());

            System.out.println("WARNING: v1->v2 conversion silently lost fields: legacyCode, oldFormat");
        }

        @Test
        @DisplayName("RESEARCH: v2 -> v1 conversion loses v2-only fields (newFormat, category, createdAt)")
        void v2ToV1LosesV2OnlyFields() {
            // Arrange: Create v2 object with v2-only fields populated
            VersionContext ctx2 = VersionContext.forVersion(2);

            Date date = ctx2.newDateBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("test-id")
                    .setName("test-name")
                    .setStatus(Status.CONFLICT_STATUS_ACTIVE)
                    .setDescription("description")
                    .setNewFormat("NEW-FORMAT-V2")  // v2-only
                    .setCategory("technology")      // v2-only
                    .setCreatedAt(date)             // v2-only
                    .build();

            // Verify v2 has the data
            assertEquals("NEW-FORMAT-V2", v2.getNewFormat());
            assertEquals("technology", v2.getCategory());
            assertNotNull(v2.getCreatedAt());
            assertTrue(v2.hasNewFormat());
            assertTrue(v2.hasCategory());
            assertTrue(v2.hasCreatedAt());

            // Act: Convert to v1
            VersionSpecificFields v1 = v2.asVersion(space.alnovis.protowrapper.it.model.v1.VersionSpecificFields.class);

            // Assert: Document current behavior
            // CURRENT BEHAVIOR: Data is SILENTLY LOST
            assertNull(v1.getNewFormat(),
                "CURRENT BEHAVIOR: newFormat is null after conversion (DATA LOST)");
            assertNull(v1.getCategory(),
                "CURRENT BEHAVIOR: category is null after conversion (DATA LOST)");
            assertNull(v1.getCreatedAt(),
                "CURRENT BEHAVIOR: createdAt is null after conversion (DATA LOST)");
            assertFalse(v1.hasNewFormat());
            assertFalse(v1.hasCategory());
            assertFalse(v1.hasCreatedAt());

            // Common fields should be preserved
            assertEquals("test-id", v1.getId());
            assertEquals("test-name", v1.getName());
            assertEquals(2, v1.getStatus());
            assertEquals("description", v1.getDescription());

            // v1 version
            assertEquals(1, v1.getWrapperVersion());

            System.out.println("WARNING: v2->v1 conversion silently lost fields: newFormat, category, createdAt");
        }

        @Test
        @DisplayName("RESEARCH: Same version conversion preserves all data")
        void sameVersionConversionPreservesData() {
            // Arrange
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("test-id")
                    .setName("test-name")
                    .setStatus(2)
                    .setLegacyCode("LEGACY-123")
                    .setOldFormat(999)
                    .build();

            // Act: Convert to same version (should return same instance)
            VersionSpecificFields v1Same = v1.asVersion(space.alnovis.protowrapper.it.model.v1.VersionSpecificFields.class);

            // Assert: Should be the same instance (optimization)
            assertSame(v1, v1Same, "Same version conversion should return same instance");

            // All data preserved
            assertEquals("LEGACY-123", v1Same.getLegacyCode());
            assertEquals(Integer.valueOf(999), v1Same.getOldFormat());
        }

        @Test
        @DisplayName("RESEARCH: Conversion without version-specific data is lossless")
        void conversionWithoutVersionSpecificDataIsLossless() {
            // Arrange: Create v1 object with ONLY common fields
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("test-id")
                    .setName("test-name")
                    .setStatus(2)
                    .setDescription("description")
                    // NOT setting legacyCode or oldFormat
                    .build();

            assertFalse(v1.hasLegacyCode());
            assertFalse(v1.hasOldFormat());

            // Act: Convert to v2
            VersionSpecificFields v2 = v1.asVersion(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class);

            // Assert: All data preserved (no loss because no v1-only data was set)
            assertEquals("test-id", v2.getId());
            assertEquals("test-name", v2.getName());
            assertEquals(2, v2.getStatus());
            assertEquals("description", v2.getDescription());
            assertEquals(2, v2.getWrapperVersion());
        }
    }

    @Nested
    @DisplayName("supportsXxx() methods investigation")
    class SupportsMethodsInvestigation {

        @Test
        @DisplayName("RESEARCH: supportsXxx() correctly identifies version-specific fields")
        void supportsMethodsWork() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionContext ctx2 = VersionContext.forVersion(2);

            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id").setName("name").setStatus(1).build();
            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id").setName("name").setStatus(1).build();

            // v1 supports v1-only fields
            assertTrue(v1.supportsLegacyCode());
            assertTrue(v1.supportsOldFormat());
            assertFalse(v1.supportsNewFormat());
            assertFalse(v1.supportsCategory());
            assertFalse(v1.supportsCreatedAt());

            // v2 supports v2-only fields
            assertFalse(v2.supportsLegacyCode());
            assertFalse(v2.supportsOldFormat());
            assertTrue(v2.supportsNewFormat());
            assertTrue(v2.supportsCategory());
            assertTrue(v2.supportsCreatedAt());

            System.out.println("INFO: supportsXxx() methods work correctly");
        }

        @Test
        @DisplayName("RESEARCH: Can use supportsXxx() to detect potential data loss before conversion")
        void canDetectPotentialDataLoss() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .build();

            // Manual check: would conversion to v2 lose data?
            boolean wouldLoseData = false;

            // Check if v1-only fields have values
            if (v1.hasLegacyCode() && !v1.supportsLegacyCode()) {
                // This is wrong logic - supportsLegacyCode() returns true for v1
                // We need to check if TARGET version supports it
            }

            // PROBLEM: We can't easily check if TARGET version supports a field
            // We only have supportsXxx() for the CURRENT instance

            // What we need:
            // v1.wouldLoseDataConvertingTo(2) or
            // v1.getFieldsNotSupportedInVersion(2)

            System.out.println("FINDING: Current API doesn't provide easy way to detect data loss BEFORE conversion");
            System.out.println("FINDING: supportsXxx() only works for current instance, not target version");
        }
    }

    @Nested
    @DisplayName("Round-trip conversion investigation")
    class RoundTripConversion {

        @Test
        @DisplayName("RESEARCH: v1 -> v2 -> v1 round-trip PRESERVES data via protobuf unknown fields")
        void roundTripPreservesDataViaUnknownFields() {
            // Arrange
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields original = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("test-id")
                    .setName("test-name")
                    .setStatus(2)
                    .setLegacyCode("LEGACY-123")
                    .setOldFormat(999)
                    .build();

            // Act: Round-trip v1 -> v2 -> v1
            VersionSpecificFields v2 = original.asVersion(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class);
            VersionSpecificFields backToV1 = v2.asVersion(space.alnovis.protowrapper.it.model.v1.VersionSpecificFields.class);

            // FINDING: Protobuf preserves unknown fields!
            // When v2 parses v1 bytes, it keeps unknown fields (legacyCode, oldFormat)
            // When converting back to v1, those fields are restored

            // v1-only data is PRESERVED via protobuf unknown fields mechanism
            assertEquals("LEGACY-123", backToV1.getLegacyCode(),
                "FINDING: legacyCode PRESERVED via protobuf unknown fields");
            assertEquals(Integer.valueOf(999), backToV1.getOldFormat(),
                "FINDING: oldFormat PRESERVED via protobuf unknown fields");

            // Common fields preserved
            assertEquals("test-id", backToV1.getId());
            assertEquals("test-name", backToV1.getName());

            // However, v2 instance CANNOT access the v1-only data!
            assertNull(v2.getLegacyCode(),
                "FINDING: v2 instance cannot access v1-only field even though bytes contain it");
            assertNull(v2.getOldFormat(),
                "FINDING: v2 instance cannot access v1-only field even though bytes contain it");

            System.out.println("FINDING: Round-trip preserves data via protobuf unknown fields!");
            System.out.println("FINDING: But intermediate v2 instance cannot access v1-only fields");
        }
    }

    @Nested
    @DisplayName("getFieldsInaccessibleInVersion() method")
    class GetFieldsInaccessibleInVersionTest {

        @Test
        @DisplayName("Returns v1-only fields when checking v2 target")
        void returnsV1FieldsWhenTargetIsV2() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .setOldFormat(42)
                    .build();

            java.util.List<String> inaccessible = v1.getFieldsInaccessibleInVersion(2);

            assertEquals(2, inaccessible.size());
            assertTrue(inaccessible.contains("legacyCode"));
            assertTrue(inaccessible.contains("oldFormat"));
        }

        @Test
        @DisplayName("Returns v2-only fields when checking v1 target")
        void returnsV2FieldsWhenTargetIsV1() {
            VersionContext ctx2 = VersionContext.forVersion(2);

            Date date = ctx2.newDateBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setNewFormat("new")
                    .setCategory("tech")
                    .setCreatedAt(date)
                    .build();

            java.util.List<String> inaccessible = v2.getFieldsInaccessibleInVersion(1);

            assertEquals(3, inaccessible.size());
            assertTrue(inaccessible.contains("newFormat"));
            assertTrue(inaccessible.contains("category"));
            assertTrue(inaccessible.contains("createdAt"));
        }

        @Test
        @DisplayName("Returns empty list when target is same version")
        void returnsEmptyWhenSameVersion() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .build();

            java.util.List<String> inaccessible = v1.getFieldsInaccessibleInVersion(1);

            assertTrue(inaccessible.isEmpty(), "Same version should have no inaccessible fields");
        }

        @Test
        @DisplayName("Returns empty list when no version-specific fields are set")
        void returnsEmptyWhenNoVersionSpecificFieldsSet() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    // NOT setting legacyCode or oldFormat
                    .build();

            java.util.List<String> inaccessible = v1.getFieldsInaccessibleInVersion(2);

            assertTrue(inaccessible.isEmpty(),
                    "Should be empty when no version-specific fields are populated");
        }
    }

    @Nested
    @DisplayName("canConvertLosslesslyTo() method")
    class CanConvertLosslesslyToTest {

        @Test
        @DisplayName("Returns false when conversion would make fields inaccessible")
        void returnsFalseWhenDataWouldBeInaccessible() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .build();

            assertFalse(v1.canConvertLosslesslyTo(2),
                    "Should return false when v1-only fields are populated");
        }

        @Test
        @DisplayName("Returns true when conversion is lossless")
        void returnsTrueWhenLossless() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    // NOT setting version-specific fields
                    .build();

            assertTrue(v1.canConvertLosslesslyTo(2),
                    "Should return true when no version-specific fields are populated");
        }

        @Test
        @DisplayName("Returns true for same version")
        void returnsTrueForSameVersion() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .build();

            assertTrue(v1.canConvertLosslesslyTo(1),
                    "Same version should always be lossless");
        }
    }

    @Nested
    @DisplayName("Enum conversion investigation")
    class EnumConversionInvestigation {

        @Test
        @DisplayName("RESEARCH: Enum values preserved in conversion")
        void enumValuesPreserved() {
            // Status enum exists in both versions
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(Status.CONFLICT_STATUS_COMPLETED) // value = 3
                    .build();

            assertEquals(3, v1.getStatus());
            assertEquals(Status.CONFLICT_STATUS_COMPLETED, v1.getStatusEnum());

            // Convert to v2
            VersionSpecificFields v2 = v1.asVersion(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class);

            // Enum value should be preserved
            assertEquals(3, v2.getStatus());
            assertEquals(Status.CONFLICT_STATUS_COMPLETED, v2.getStatusEnum());

            System.out.println("INFO: Enum values are preserved during conversion");
        }
    }

    @Nested
    @DisplayName("asVersionStrict() method")
    class AsVersionStrictTest {

        @Test
        @DisplayName("Throws IllegalStateException when fields would become inaccessible")
        void throwsWhenFieldsWouldBecomeInaccessible() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .setOldFormat(42)
                    .build();

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                v1.asVersionStrict(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class)
            );

            // Check error message contains useful info
            assertTrue(ex.getMessage().contains("version v1"), "Should mention source version");
            assertTrue(ex.getMessage().contains("version 2"), "Should mention target version");
            assertTrue(ex.getMessage().contains("legacyCode"), "Should mention inaccessible field");
            assertTrue(ex.getMessage().contains("oldFormat"), "Should mention inaccessible field");
            assertTrue(ex.getMessage().contains("asVersion()"), "Should suggest alternative");
        }

        @Test
        @DisplayName("Succeeds when no fields would become inaccessible")
        void succeedsWhenNoFieldsWouldBecomeInaccessible() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    // NOT setting version-specific fields
                    .build();

            // Should not throw
            VersionSpecificFields v2 = v1.asVersionStrict(space.alnovis.protowrapper.it.model.v2.VersionSpecificFields.class);

            assertEquals("id", v2.getId());
            assertEquals("name", v2.getName());
            assertEquals(2, v2.getWrapperVersion());
        }

        @Test
        @DisplayName("Returns same instance for same version")
        void returnsSameInstanceForSameVersion() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setLegacyCode("legacy")  // Has version-specific field
                    .build();

            // Should not throw and return same instance
            VersionSpecificFields same = v1.asVersionStrict(space.alnovis.protowrapper.it.model.v1.VersionSpecificFields.class);

            assertSame(v1, same);
        }

        @Test
        @DisplayName("v2 to v1 strict conversion throws when v2-only fields populated")
        void v2ToV1ThrowsWhenV2FieldsPopulated() {
            VersionContext ctx2 = VersionContext.forVersion(2);

            Date date = ctx2.newDateBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .setNewFormat("new")
                    .setCategory("tech")
                    .setCreatedAt(date)
                    .build();

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                v2.asVersionStrict(space.alnovis.protowrapper.it.model.v1.VersionSpecificFields.class)
            );

            assertTrue(ex.getMessage().contains("version v2"), "Should mention source version");
            assertTrue(ex.getMessage().contains("version 1"), "Should mention target version");
            assertTrue(ex.getMessage().contains("newFormat"), "Should mention inaccessible field");
            assertTrue(ex.getMessage().contains("category"), "Should mention inaccessible field");
            assertTrue(ex.getMessage().contains("createdAt"), "Should mention inaccessible field");
        }
    }

    @Nested
    @DisplayName("Version-specific enum field handling")
    class VersionSpecificEnumFieldTest {

        @Test
        @DisplayName("v2-only string field (newFormat) cannot be set in v1 builder")
        void v2OnlyFieldCannotBeSetInV1() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            // newFormat field only exists in v2
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
                ctx1.newVersionSpecificFieldsBuilder().setNewFormat("test")
            );

            assertTrue(ex.getMessage().contains("newFormat") || ex.getMessage().contains("v1"),
                    "Error should mention field or version: " + ex.getMessage());
        }

        @Test
        @DisplayName("v2-only string field (newFormat) can be set in v2 builder")
        void v2OnlyFieldCanBeSetInV2() {
            VersionContext ctx2 = VersionContext.forVersion(2);

            // This should work without throwing
            assertDoesNotThrow(() ->
                ctx2.newVersionSpecificFieldsBuilder().setNewFormat("test-value")
            );
        }

        @Test
        @DisplayName("v2-only field returns null in v1 object")
        void v2OnlyFieldReturnsNullInV1() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            // newFormat field doesn't exist in v1 - should return null
            assertNull(v1.getNewFormat(),
                    "v2-only field should return null in v1");
            assertFalse(v1.hasNewFormat(),
                    "hasNewFormat() should return false for v1");
        }

        @Test
        @DisplayName("Priority enum has proper validation methods")
        void priorityEnumValidationMethods() {
            // Test fromProtoValueOrThrow
            assertEquals(Priority.CONFLICT_PRIORITY_LOW, Priority.fromProtoValueOrThrow(0));
            assertEquals(Priority.CONFLICT_PRIORITY_HIGH, Priority.fromProtoValueOrThrow(2));

            // Test invalid value throws
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Priority.fromProtoValueOrThrow(999)
            );
            assertTrue(ex.getMessage().contains("999"), "Should mention invalid value");
            assertTrue(ex.getMessage().contains("Priority"), "Should mention enum name");
        }

        @Test
        @DisplayName("supportsXxx() returns correct values for version-specific fields")
        void supportsMethodsReturnsCorrectValues() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionContext ctx2 = VersionContext.forVersion(2);

            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id").setName("name").setStatus(1).build();
            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id").setName("name").setStatus(1).build();

            // v1 supports v1-only fields
            assertTrue(v1.supportsLegacyCode());
            assertTrue(v1.supportsOldFormat());
            assertFalse(v1.supportsNewFormat());
            assertFalse(v1.supportsCategory());

            // v2 supports v2-only fields
            assertFalse(v2.supportsLegacyCode());
            assertFalse(v2.supportsOldFormat());
            assertTrue(v2.supportsNewFormat());
            assertTrue(v2.supportsCategory());
        }
    }

    @Nested
    @DisplayName("Version-specific message handling")
    class VersionSpecificMessageTest {

        @Test
        @DisplayName("newCryptoBuilder() throws UnsupportedOperationException in v1")
        void newCryptoBuilderThrowsInV1() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            // Crypto message only exists in v2
            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> ctx1.newCryptoBuilder()
            );

            assertTrue(ex.getMessage().contains("Crypto"),
                    "Error should mention message name: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("v2") || ex.getMessage().contains("not available"),
                    "Error should mention available version or unavailability: " + ex.getMessage());
        }

        @Test
        @DisplayName("Crypto.newBuilder(ctx) throws UnsupportedOperationException for v1 context")
        void staticNewBuilderThrowsForV1Context() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            // Static method delegates to ctx.newCryptoBuilder()
            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> space.alnovis.protowrapper.it.model.api.Crypto.newBuilder(ctx1)
            );

            assertTrue(ex.getMessage().contains("Crypto"),
                    "Error should mention message name: " + ex.getMessage());
        }

        @Test
        @DisplayName("wrapCrypto() throws UnsupportedOperationException in v1")
        void wrapCryptoThrowsInV1() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> ctx1.wrapCrypto(null)
            );

            assertTrue(ex.getMessage().contains("Crypto"),
                    "Error should mention message name: " + ex.getMessage());
        }

        @Test
        @DisplayName("parseCryptoFromBytes() throws UnsupportedOperationException in v1")
        void parseCryptoFromBytesThrowsInV1() {
            VersionContext ctx1 = VersionContext.forVersion(1);

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> ctx1.parseCryptoFromBytes(new byte[0])
            );

            assertTrue(ex.getMessage().contains("Crypto"),
                    "Error should mention message name: " + ex.getMessage());
        }

        @Test
        @DisplayName("newCryptoBuilder() works in v2")
        void newCryptoBuilderWorksInV2() {
            VersionContext ctx2 = VersionContext.forVersion(2);

            // Should not throw
            assertDoesNotThrow(() -> ctx2.newCryptoBuilder());
        }

        @Test
        @DisplayName("Crypto.newBuilder(ctx) works for v2 context")
        void staticNewBuilderWorksForV2Context() {
            VersionContext ctx2 = VersionContext.forVersion(2);

            // Should not throw
            assertDoesNotThrow(() ->
                space.alnovis.protowrapper.it.model.api.Crypto.newBuilder(ctx2)
            );
        }
    }
}
