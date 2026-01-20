package io.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.alnovis.protowrapper.it.model.api.Status;
import io.alnovis.protowrapper.it.model.api.Priority;
import io.alnovis.protowrapper.it.model.api.VersionContext;
import io.alnovis.protowrapper.it.model.api.IntEnumConflicts;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for INT_ENUM error messages.
 *
 * When a field is int in one version and enum in another, setting an invalid
 * int value should throw an informative IllegalArgumentException instead of NPE.
 */
@DisplayName("INT_ENUM Error Messages")
public class IntEnumErrorMessageTest {

    @Nested
    @DisplayName("fromProtoValueOrThrow method")
    class FromProtoValueOrThrowMethod {

        @Test
        @DisplayName("fromProtoValueOrThrow returns value for valid input")
        void returnsValueForValidInput() {
            // Valid values for Status: 0-5
            assertEquals(Status.CONFLICT_STATUS_UNKNOWN, Status.fromProtoValueOrThrow(0));
            assertEquals(Status.CONFLICT_STATUS_PENDING, Status.fromProtoValueOrThrow(1));
            assertEquals(Status.CONFLICT_STATUS_ACTIVE, Status.fromProtoValueOrThrow(2));
            assertEquals(Status.CONFLICT_STATUS_COMPLETED, Status.fromProtoValueOrThrow(3));
            assertEquals(Status.CONFLICT_STATUS_CANCELLED, Status.fromProtoValueOrThrow(4));
            assertEquals(Status.CONFLICT_STATUS_FAILED, Status.fromProtoValueOrThrow(5));
        }

        @Test
        @DisplayName("fromProtoValueOrThrow throws for invalid input")
        void throwsForInvalidInput() {
            // Invalid value 999 should throw
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Status.fromProtoValueOrThrow(999)
            );

            // Error message should contain the invalid value
            assertTrue(ex.getMessage().contains("999"),
                    "Error message should contain invalid value: " + ex.getMessage());

            // Error message should contain enum name
            assertTrue(ex.getMessage().contains("Status"),
                    "Error message should contain enum name: " + ex.getMessage());

            // Error message should contain valid values
            assertTrue(ex.getMessage().contains("CONFLICT_STATUS"),
                    "Error message should contain valid values: " + ex.getMessage());
        }

        @Test
        @DisplayName("fromProtoValueOrThrow throws for negative value")
        void throwsForNegativeValue() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Status.fromProtoValueOrThrow(-1)
            );

            assertTrue(ex.getMessage().contains("-1"),
                    "Error message should contain invalid value");
        }
    }

    @Nested
    @DisplayName("Builder setXxx(int) validation")
    class BuilderValidation {

        @Test
        @DisplayName("setStatus(int) with valid value should work in all versions")
        void setStatusWithValidValueWorks() {
            // v1 (int version) - valid values work
            VersionContext ctx1 = VersionContext.forVersionId("v1");
            IntEnumConflicts.Builder builder1 = ctx1.newIntEnumConflictsBuilder();
            assertDoesNotThrow(() -> builder1.setStatus(0));
            assertDoesNotThrow(() -> builder1.setStatus(1));
            assertDoesNotThrow(() -> builder1.setStatus(5));

            // v2 (enum version) - valid values work
            VersionContext ctx2 = VersionContext.forVersionId("v2");
            IntEnumConflicts.Builder builder2 = ctx2.newIntEnumConflictsBuilder();
            assertDoesNotThrow(() -> builder2.setStatus(0));
            assertDoesNotThrow(() -> builder2.setStatus(1));
            assertDoesNotThrow(() -> builder2.setStatus(5));
        }

        @Test
        @DisplayName("setStatus(int) with invalid value in v1 (int version) should NOT throw")
        void setStatusWithInvalidValueInIntVersionDoesNotThrow() {
            // v1 uses int type, so any int value is valid
            VersionContext ctx = VersionContext.forVersionId("v1");
            IntEnumConflicts.Builder builder = ctx.newIntEnumConflictsBuilder();

            // Invalid value should NOT throw in int version - any int is allowed
            assertDoesNotThrow(() -> builder.setStatus(999),
                    "v1 uses int type, so any int value should be allowed");
        }

        @Test
        @DisplayName("setStatus(int) with invalid value in v2 (enum version) should throw IllegalArgumentException")
        void setStatusWithInvalidValueInEnumVersionThrows() {
            // v2 uses enum type, so only valid enum values are allowed
            VersionContext ctx = VersionContext.forVersionId("v2");
            IntEnumConflicts.Builder builder = ctx.newIntEnumConflictsBuilder();

            // Invalid value should throw IllegalArgumentException in enum version
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> builder.setStatus(999),
                    "v2 uses enum type, so invalid int value should throw"
            );

            // Error message should be informative
            assertTrue(ex.getMessage().contains("999"),
                    "Error should contain invalid value: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("Status"),
                    "Error should contain field/enum name: " + ex.getMessage());
        }

        @Test
        @DisplayName("setStatus(enum) with valid enum should work")
        void setStatusWithEnumWorks() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            IntEnumConflicts.Builder builder = ctx.newIntEnumConflictsBuilder();

            // Enum value should always work
            assertDoesNotThrow(() -> builder.setStatus(Status.CONFLICT_STATUS_ACTIVE));

            IntEnumConflicts result = builder
                    .setStatus(Status.CONFLICT_STATUS_PENDING)
                    .setName("test")
                    .build();

            assertEquals(1, result.getStatus());
            assertEquals(Status.CONFLICT_STATUS_PENDING, result.getStatusEnum());
        }
    }

    @Nested
    @DisplayName("Error message quality")
    class ErrorMessageQuality {

        @Test
        @DisplayName("Error message should be human-readable")
        void errorMessageIsHumanReadable() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> Status.fromProtoValueOrThrow(42)
            );

            String message = ex.getMessage();

            // Should have a clear structure
            assertTrue(message.startsWith("Invalid value"),
                    "Should start with 'Invalid value': " + message);

            // Should mention valid options
            assertTrue(message.contains("Valid values:"),
                    "Should mention valid values: " + message);

            // Should list all enum constants
            assertTrue(message.contains("CONFLICT_STATUS_UNKNOWN"),
                    "Should list all valid constants: " + message);
        }
    }
}
