package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.alnovis.protowrapper.it.model.api.Date;
import space.alnovis.protowrapper.it.model.api.Status;
import space.alnovis.protowrapper.it.model.api.VersionContext;
import space.alnovis.protowrapper.it.model.api.VersionSpecificFields;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for version-specific fields behavior.
 *
 * <p>When a field exists only in certain protocol versions, setting it
 * in a version where it doesn't exist should throw UnsupportedOperationException.</p>
 *
 * <p>Test cases:</p>
 * <ul>
 *   <li>v1-only fields: legacyCode, oldFormat</li>
 *   <li>v2-only fields: newFormat, createdAt, category</li>
 * </ul>
 */
@DisplayName("Version-Specific Fields")
public class VersionSpecificFieldsTest {

    @Nested
    @DisplayName("V1-only fields in V2 builder")
    class V1OnlyFieldsInV2 {

        @Test
        @DisplayName("setLegacyCode in v2 should throw UnsupportedOperationException")
        void setLegacyCodeInV2Throws() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setLegacyCode("test"),
                    "Setting v1-only field 'legacyCode' in v2 should throw"
            );

            // Error message should be informative
            assertTrue(ex.getMessage().contains("legacyCode"),
                    "Error should contain field name: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("v1"),
                    "Error should mention which versions support this field: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("not available"),
                    "Error should indicate field is not available: " + ex.getMessage());
        }

        @Test
        @DisplayName("setOldFormat in v2 should throw UnsupportedOperationException")
        void setOldFormatInV2Throws() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setOldFormat(42),
                    "Setting v1-only field 'oldFormat' in v2 should throw"
            );

            assertTrue(ex.getMessage().contains("oldFormat"),
                    "Error should contain field name: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("v1"),
                    "Error should mention which versions support this field: " + ex.getMessage());
        }

        @Test
        @DisplayName("clearLegacyCode in v2 should NOT throw (safe no-op)")
        void clearLegacyCodeInV2DoesNotThrow() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            // Clear should be safe - it's a no-op for non-existent fields
            assertDoesNotThrow(() -> builder.clearLegacyCode(),
                    "Clearing non-existent field should be safe");
        }

        @Test
        @DisplayName("clearOldFormat in v2 should NOT throw (safe no-op)")
        void clearOldFormatInV2DoesNotThrow() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            assertDoesNotThrow(() -> builder.clearOldFormat(),
                    "Clearing non-existent field should be safe");
        }
    }

    @Nested
    @DisplayName("V2-only fields in V1 builder")
    class V2OnlyFieldsInV1 {

        @Test
        @DisplayName("setNewFormat in v1 should throw UnsupportedOperationException")
        void setNewFormatInV1Throws() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setNewFormat("new_value"),
                    "Setting v2-only field 'newFormat' in v1 should throw"
            );

            assertTrue(ex.getMessage().contains("newFormat"),
                    "Error should contain field name: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("v2"),
                    "Error should mention which versions support this field: " + ex.getMessage());
        }

        @Test
        @DisplayName("setCategory in v1 should throw UnsupportedOperationException")
        void setCategoryInV1Throws() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setCategory("tech"),
                    "Setting v2-only field 'category' in v1 should throw"
            );

            assertTrue(ex.getMessage().contains("category"),
                    "Error should contain field name: " + ex.getMessage());
        }

        @Test
        @DisplayName("setCreatedAt in v1 should throw UnsupportedOperationException")
        void setCreatedAtInV1Throws() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            // Create a Date for testing
            Date date = ctx.newDateBuilder()
                    .setYear(2024)
                    .setMonth(1)
                    .setDay(15)
                    .build();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setCreatedAt(date),
                    "Setting v2-only field 'createdAt' in v1 should throw"
            );

            assertTrue(ex.getMessage().contains("createdAt"),
                    "Error should contain field name: " + ex.getMessage());
        }

        @Test
        @DisplayName("clearNewFormat in v1 should NOT throw (safe no-op)")
        void clearNewFormatInV1DoesNotThrow() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            assertDoesNotThrow(() -> builder.clearNewFormat(),
                    "Clearing non-existent field should be safe");
        }

        @Test
        @DisplayName("clearCategory in v1 should NOT throw (safe no-op)")
        void clearCategoryInV1DoesNotThrow() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            assertDoesNotThrow(() -> builder.clearCategory(),
                    "Clearing non-existent field should be safe");
        }
    }

    @Nested
    @DisplayName("Fields in correct versions work normally")
    class FieldsInCorrectVersions {

        @Test
        @DisplayName("v1-only fields work in v1")
        void v1FieldsWorkInV1() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            // Should work without throwing
            assertDoesNotThrow(() -> builder.setLegacyCode("legacy_value"));
            assertDoesNotThrow(() -> builder.setOldFormat(123));

            VersionSpecificFields result = builder
                    .setId("id1")
                    .setName("test")
                    .setStatus(1)
                    .setLegacyCode("legacy")
                    .setOldFormat(99)
                    .build();

            assertEquals("legacy", result.getLegacyCode());
            assertEquals(Integer.valueOf(99), result.getOldFormat());
        }

        @Test
        @DisplayName("v2-only fields work in v2")
        void v2FieldsWorkInV2() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            Date date = ctx.newDateBuilder()
                    .setYear(2024)
                    .setMonth(6)
                    .setDay(15)
                    .build();

            // Should work without throwing
            assertDoesNotThrow(() -> builder.setNewFormat("new_format"));
            assertDoesNotThrow(() -> builder.setCategory("business"));
            assertDoesNotThrow(() -> builder.setCreatedAt(date));

            VersionSpecificFields result = builder
                    .setId("id2")
                    .setName("test2")
                    .setStatus(Status.CONFLICT_STATUS_ACTIVE)
                    .setNewFormat("v2_format")
                    .setCategory("tech")
                    .setCreatedAt(date)
                    .build();

            assertEquals("v2_format", result.getNewFormat());
            assertEquals("tech", result.getCategory());
            assertNotNull(result.getCreatedAt());
        }

        @Test
        @DisplayName("Common fields work in both versions")
        void commonFieldsWorkInBothVersions() {
            // v1
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("common_id")
                    .setName("common_name")
                    .setStatus(2)
                    .setDescription("desc")
                    .build();

            assertEquals("common_id", v1.getId());
            assertEquals("common_name", v1.getName());
            assertEquals(2, v1.getStatus());
            assertEquals("desc", v1.getDescription());

            // v2
            VersionContext ctx2 = VersionContext.forVersion(2);
            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("common_id")
                    .setName("common_name")
                    .setStatus(Status.CONFLICT_STATUS_COMPLETED)
                    .setDescription("desc")
                    .build();

            assertEquals("common_id", v2.getId());
            assertEquals("common_name", v2.getName());
            assertEquals(3, v2.getStatus());
            assertEquals("desc", v2.getDescription());
        }
    }

    @Nested
    @DisplayName("supportsXxx() methods")
    class SupportsMethodsTest {

        @Test
        @DisplayName("supportsLegacyCode returns correct values")
        void supportsLegacyCode() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            assertTrue(v1.supportsLegacyCode(), "v1 should support legacyCode");

            VersionContext ctx2 = VersionContext.forVersion(2);
            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            assertFalse(v2.supportsLegacyCode(), "v2 should NOT support legacyCode");
        }

        @Test
        @DisplayName("supportsNewFormat returns correct values")
        void supportsNewFormat() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            assertFalse(v1.supportsNewFormat(), "v1 should NOT support newFormat");

            VersionContext ctx2 = VersionContext.forVersion(2);
            VersionSpecificFields v2 = ctx2.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            assertTrue(v2.supportsNewFormat(), "v2 should support newFormat");
        }

        @Test
        @DisplayName("Can use supportsXxx() to guard field access")
        void useSupportsToGuardAccess() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            VersionSpecificFields.Builder builder = ctx1.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1);

            // Use supportsXxx() before setting - this pattern avoids exceptions
            if (builder.build().supportsLegacyCode()) {
                builder.setLegacyCode("legacy_value");
            }
            // This would throw: builder.setNewFormat("new_value");

            // Alternative pattern: check version directly
            VersionSpecificFields result = builder.build();
            if (result.getWrapperVersion() == 1) {
                // v1-specific logic
                assertNotNull(result.getLegacyCode());
            }
        }
    }

    @Nested
    @DisplayName("Error message quality")
    class ErrorMessageQuality {

        @Test
        @DisplayName("Error message format is consistent and informative")
        void errorMessageFormat() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setLegacyCode("test")
            );

            String message = ex.getMessage();

            // Should contain field name
            assertTrue(message.contains("legacyCode"),
                    "Should contain field name: " + message);

            // Should contain "not available"
            assertTrue(message.contains("not available"),
                    "Should indicate unavailability: " + message);

            // Should contain "protocol version"
            assertTrue(message.contains("protocol version"),
                    "Should mention protocol version: " + message);

            // Should contain the available versions
            assertTrue(message.contains("[v1]"),
                    "Should list available versions: " + message);
        }

        @Test
        @DisplayName("Error message contains current version number")
        void errorMessageContainsCurrentVersion() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields.Builder builder = ctx.newVersionSpecificFieldsBuilder();

            UnsupportedOperationException ex = assertThrows(
                    UnsupportedOperationException.class,
                    () -> builder.setLegacyCode("test")
            );

            // Message should indicate we're trying to use this in version 2
            assertTrue(ex.getMessage().contains("2"),
                    "Should contain current version: " + ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Getter behavior for missing fields")
    class GetterBehavior {

        @Test
        @DisplayName("Getting v1-only field in v2 returns null")
        void v1FieldInV2ReturnsNull() {
            VersionContext ctx = VersionContext.forVersion(2);
            VersionSpecificFields v2 = ctx.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            // v1-only fields return null in v2
            assertNull(v2.getLegacyCode());
            assertNull(v2.getOldFormat());
            assertFalse(v2.hasLegacyCode());
            assertFalse(v2.hasOldFormat());
        }

        @Test
        @DisplayName("Getting v2-only field in v1 returns null")
        void v2FieldInV1ReturnsNull() {
            VersionContext ctx = VersionContext.forVersion(1);
            VersionSpecificFields v1 = ctx.newVersionSpecificFieldsBuilder()
                    .setId("id")
                    .setName("name")
                    .setStatus(1)
                    .build();

            // v2-only fields return null in v1
            assertNull(v1.getNewFormat());
            assertNull(v1.getCategory());
            assertNull(v1.getCreatedAt());
            assertFalse(v1.hasNewFormat());
            assertFalse(v1.hasCategory());
            assertFalse(v1.hasCreatedAt());
        }
    }
}
