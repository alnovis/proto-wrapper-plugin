package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.exception.*;
import space.alnovis.protowrapper.it.model.api.*;
import space.alnovis.protowrapper.it.proto.v1.Conflicts;
import space.alnovis.protowrapper.it.proto.v1.Telemetry;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the proto-wrapper exception hierarchy.
 *
 * <p>Tests exception throwing and handling in real wrapper usage scenarios:</p>
 * <ul>
 *   <li>Version not supported exceptions</li>
 *   <li>Field not available exceptions</li>
 *   <li>Type range exceeded exceptions</li>
 *   <li>Enum value not supported exceptions</li>
 *   <li>Error code and context usage</li>
 * </ul>
 */
@DisplayName("Exception Hierarchy Integration Tests")
class ExceptionHierarchyIntegrationTest {

    // ==================== ErrorCode Tests ====================

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTests {

        @Test
        @DisplayName("ErrorCode has correct prefix format")
        void errorCodeHasCorrectPrefixFormat() {
            // Schema errors start with SCHEMA-
            assertThat(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES.getCode()).startsWith("SCHEMA-");
            assertThat(ErrorCode.SCHEMA_FIELD_NUMBER_CONFLICT.getCode()).startsWith("SCHEMA-");
            assertThat(ErrorCode.SCHEMA_ONEOF_CONFLICT.getCode()).startsWith("SCHEMA-");

            // Conversion errors start with CONV-
            assertThat(ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED.getCode()).startsWith("CONV-");
            assertThat(ErrorCode.CONVERSION_TYPE_MISMATCH.getCode()).startsWith("CONV-");
            assertThat(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED.getCode()).startsWith("CONV-");
            assertThat(ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE.getCode()).startsWith("CONV-");

            // Version errors start with VER-
            assertThat(ErrorCode.VERSION_NOT_SUPPORTED.getCode()).startsWith("VER-");
        }

        @Test
        @DisplayName("ErrorCode format includes code and description")
        void errorCodeFormatIncludesCodeAndDescription() {
            String formatted = ErrorCode.SCHEMA_INCOMPATIBLE_TYPES.format();

            assertThat(formatted).contains("SCHEMA-001");
            assertThat(formatted).contains("Incompatible");
        }

        @Test
        @DisplayName("ErrorCode toString returns code only")
        void errorCodeToStringReturnsCodeOnly() {
            assertThat(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES.toString()).isEqualTo("SCHEMA-001");
            assertThat(ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED.toString()).isEqualTo("CONV-001");
            assertThat(ErrorCode.VERSION_NOT_SUPPORTED.toString()).isEqualTo("VER-001");
        }
    }

    // ==================== ErrorContext Tests ====================

    @Nested
    @DisplayName("ErrorContext")
    class ErrorContextTests {

        @Test
        @DisplayName("ErrorContext builder creates context with all fields")
        void errorContextBuilderCreatesContextWithAllFields() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("SensorReading")
                    .fieldPath("SensorReading.temperature")
                    .fieldName("temperature")
                    .version("v1")
                    .fieldNumber(1)
                    .availableVersions(Set.of("v2", "v3"))
                    .addDetail("sourceType", "float")
                    .addDetail("targetType", "double")
                    .build();

            assertThat(context.getMessageType()).isEqualTo("SensorReading");
            assertThat(context.getFieldPath()).isEqualTo("SensorReading.temperature");
            assertThat(context.getFieldName()).isEqualTo("temperature");
            assertThat(context.getVersion()).isEqualTo("v1");
            assertThat(context.getFieldNumber()).isEqualTo(1);
            assertThat(context.getAvailableVersions()).containsExactlyInAnyOrder("v2", "v3");
            assertThat((String) context.getDetail("sourceType")).isEqualTo("float");
            assertThat((String) context.getDetail("targetType")).isEqualTo("double");
        }

        @Test
        @DisplayName("ErrorContext factory methods create appropriate contexts")
        void errorContextFactoryMethodsCreateAppropriateContexts() {
            ErrorContext forMessage = ErrorContext.forMessage("SensorReading");
            assertThat(forMessage.getMessageType()).isEqualTo("SensorReading");

            ErrorContext forField = ErrorContext.forField("SensorReading", "temperature");
            assertThat(forField.getMessageType()).isEqualTo("SensorReading");
            assertThat(forField.getFieldName()).isEqualTo("temperature");

            ErrorContext forVersion = ErrorContext.forVersion("SensorReading", "v1");
            assertThat(forVersion.getMessageType()).isEqualTo("SensorReading");
            assertThat(forVersion.getVersion()).isEqualTo("v1");
        }

        @Test
        @DisplayName("ErrorContext getLocation creates human-readable location")
        void errorContextGetLocationCreatesHumanReadableLocation() {
            ErrorContext context = ErrorContext.builder()
                    .fieldPath("SensorReading.temperature")
                    .version("v1")
                    .build();

            String location = context.getLocation();
            assertThat(location).contains("SensorReading.temperature");
            assertThat(location).contains("v1");
        }
    }

    // ==================== ProtoWrapperException Tests ====================

    @Nested
    @DisplayName("ProtoWrapperException")
    class ProtoWrapperExceptionTests {

        @Test
        @DisplayName("ProtoWrapperException includes error code in message")
        void protoWrapperExceptionIncludesErrorCodeInMessage() {
            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.VERSION_NOT_SUPPORTED,
                    "Version v999 is not supported");

            assertThat(ex.getMessage()).contains("[VER-001]");
            assertThat(ex.getMessage()).contains("v999");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VERSION_NOT_SUPPORTED);
        }

        @Test
        @DisplayName("ProtoWrapperException includes context location in message")
        void protoWrapperExceptionIncludesContextLocationInMessage() {
            ErrorContext context = ErrorContext.forField("SensorReading", "temperature");

            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE,
                    "Field not available",
                    context);

            assertThat(ex.getMessage()).contains("SensorReading");
            assertThat(ex.getContext()).isSameAs(context);
        }

        @Test
        @DisplayName("ProtoWrapperException provides context accessors")
        void protoWrapperExceptionProvidesContextAccessors() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("SensorReading")
                    .fieldPath("SensorReading.temperature")
                    .version("v1")
                    .build();

            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.CONVERSION_TYPE_MISMATCH, "Type mismatch", context);

            assertThat(ex.getMessageType()).isEqualTo("SensorReading");
            assertThat(ex.getFieldPath()).isEqualTo("SensorReading.temperature");
            assertThat(ex.getVersion()).isEqualTo("v1");
            assertThat(ex.getLocation()).contains("SensorReading.temperature");
        }

        @Test
        @DisplayName("ProtoWrapperException is a RuntimeException")
        void protoWrapperExceptionIsRuntimeException() {
            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.VERSION_NOT_SUPPORTED, "Test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }

    // ==================== VersionNotSupportedException Tests ====================

    @Nested
    @DisplayName("VersionNotSupportedException")
    class VersionNotSupportedExceptionTests {

        @Test
        @DisplayName("VersionNotSupportedException created with factory method")
        void versionNotSupportedExceptionCreatedWithFactoryMethod() {
            VersionNotSupportedException ex = VersionNotSupportedException.of(
                    "v999", Set.of("v1", "v2"));

            assertThat(ex.getRequestedVersion()).isEqualTo("v999");
            assertThat(ex.getSupportedVersions()).containsExactlyInAnyOrder("v1", "v2");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VERSION_NOT_SUPPORTED);
            assertThat(ex.getMessage()).contains("v999");
            assertThat(ex.getMessage()).contains("v1");
            assertThat(ex.getMessage()).contains("v2");
        }

        @Test
        @DisplayName("VersionNotSupportedException for message includes message type")
        void versionNotSupportedExceptionForMessageIncludesMessageType() {
            VersionNotSupportedException ex = VersionNotSupportedException.forMessage(
                    "SensorReading", "v3", Set.of("v1", "v2"));

            assertThat(ex.getMessage()).contains("SensorReading");
            assertThat(ex.getMessage()).contains("v3");
            assertThat(ex.getRequestedVersion()).isEqualTo("v3");
        }
    }

    // ==================== FieldNotAvailableException Tests ====================

    @Nested
    @DisplayName("FieldNotAvailableException")
    class FieldNotAvailableExceptionTests {

        @Test
        @DisplayName("FieldNotAvailableException created with factory method")
        void fieldNotAvailableExceptionCreatedWithFactoryMethod() {
            FieldNotAvailableException ex = FieldNotAvailableException.of(
                    "SensorReading", "calibrationId", "v1", Set.of("v2"));

            assertThat(ex.getFieldName()).isEqualTo("calibrationId");
            assertThat(ex.getAvailableInVersions()).containsExactly("v2");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE);
            assertThat(ex.getMessage()).contains("calibrationId");
            assertThat(ex.getMessage()).contains("v1");
            assertThat(ex.getMessage()).contains("v2");
        }
    }

    // ==================== TypeRangeException Tests ====================

    @Nested
    @DisplayName("TypeRangeException")
    class TypeRangeExceptionTests {

        @Test
        @DisplayName("TypeRangeException created for long to int narrowing")
        void typeRangeExceptionCreatedForLongToInt() {
            TypeRangeException ex = TypeRangeException.longToInt("count", 3_000_000_000L, "v1");

            assertThat(ex.getSourceType()).isEqualTo("long");
            assertThat(ex.getTargetType()).isEqualTo("int");
            assertThat(ex.getValue()).isEqualTo(3_000_000_000L);
            assertThat(ex.getMinValue()).isEqualTo(Integer.MIN_VALUE);
            assertThat(ex.getMaxValue()).isEqualTo(Integer.MAX_VALUE);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED);
        }

        @Test
        @DisplayName("TypeRangeException created for double to float narrowing")
        void typeRangeExceptionCreatedForDoubleToFloat() {
            TypeRangeException ex = TypeRangeException.doubleToFloat("temperature", Double.MAX_VALUE, "v1");

            assertThat(ex.getSourceType()).isEqualTo("double");
            assertThat(ex.getTargetType()).isEqualTo("float");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED);
        }
    }

    // ==================== EnumValueNotSupportedException Tests ====================

    @Nested
    @DisplayName("EnumValueNotSupportedException")
    class EnumValueNotSupportedExceptionTests {

        @Test
        @DisplayName("EnumValueNotSupportedException created with factory method")
        void enumValueNotSupportedExceptionCreatedWithFactoryMethod() {
            EnumValueNotSupportedException ex = EnumValueNotSupportedException.of(
                    "UnitType", "UNIT_DELETED", 99, "v1", Set.of("UNIT_CELSIUS", "UNIT_FAHRENHEIT"));

            assertThat(ex.getEnumTypeName()).isEqualTo("UnitType");
            assertThat(ex.getEnumValue()).isEqualTo("UNIT_DELETED");
            assertThat(ex.getEnumNumber()).isEqualTo(99);
            assertThat(ex.getValidValues()).containsExactlyInAnyOrder("UNIT_CELSIUS", "UNIT_FAHRENHEIT");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED);
        }
    }

    // ==================== SchemaValidationException Tests ====================

    @Nested
    @DisplayName("SchemaValidationException")
    class SchemaValidationExceptionTests {

        @Test
        @DisplayName("SchemaValidationException for incompatible types")
        void schemaValidationExceptionForIncompatibleTypes() {
            SchemaValidationException ex = SchemaValidationException.incompatibleTypes(
                    "SensorReading", "value", "int", "String");

            assertThat(ex).isInstanceOf(FieldConflictException.class);
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES);

            FieldConflictException fce = (FieldConflictException) ex;
            assertThat(fce.getType1()).isEqualTo("int");
            assertThat(fce.getType2()).isEqualTo("String");
        }

        @Test
        @DisplayName("SchemaValidationException for field number conflict")
        void schemaValidationExceptionForFieldNumberConflict() {
            SchemaValidationException ex = SchemaValidationException.fieldNumberConflict(
                    "SensorReading", 5, "oldField", "newField");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCHEMA_FIELD_NUMBER_CONFLICT);
            assertThat(ex.getMessage()).contains("5");
            assertThat(ex.getMessage()).contains("oldField");
            assertThat(ex.getMessage()).contains("newField");
        }
    }

    // ==================== OneofConflictException Tests ====================

    @Nested
    @DisplayName("OneofConflictException")
    class OneofConflictExceptionTests {

        @Test
        @DisplayName("OneofConflictException for field membership change")
        void oneofConflictExceptionForFieldMembershipChange() {
            OneofConflictException ex = OneofConflictException.fieldMembershipChange(
                    "Payment", "method", "card", Set.of("v1", "v2"), Set.of("v3"));

            assertThat(ex.getOneofName()).isEqualTo("method");
            assertThat(ex.getVersionsAffected()).containsExactlyInAnyOrder("v1", "v2");
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCHEMA_ONEOF_CONFLICT);
        }
    }

    // ==================== Exception Hierarchy Tests ====================

    @Nested
    @DisplayName("Exception Hierarchy")
    class HierarchyTests {

        @Test
        @DisplayName("All exceptions extend ProtoWrapperException")
        void allExceptionsExtendProtoWrapperException() {
            assertThat(new SchemaValidationException(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, "test"))
                    .isInstanceOf(ProtoWrapperException.class);

            assertThat(new ConversionException(ErrorCode.CONVERSION_TYPE_MISMATCH, "test"))
                    .isInstanceOf(ProtoWrapperException.class);

            assertThat(VersionNotSupportedException.of("v1", Set.of("v2")))
                    .isInstanceOf(ProtoWrapperException.class);
        }

        @Test
        @DisplayName("FieldConflictException extends SchemaValidationException")
        void fieldConflictExceptionExtendsSchemaValidationException() {
            FieldConflictException ex = FieldConflictException.of("M", "f", "int", "String");

            assertThat(ex).isInstanceOf(SchemaValidationException.class);
            assertThat(ex).isInstanceOf(ProtoWrapperException.class);
        }

        @Test
        @DisplayName("TypeRangeException extends ConversionException")
        void typeRangeExceptionExtendsConversionException() {
            TypeRangeException ex = TypeRangeException.longToInt("f", 1L, "v1");

            assertThat(ex).isInstanceOf(ConversionException.class);
            assertThat(ex).isInstanceOf(ProtoWrapperException.class);
        }

        @Test
        @DisplayName("Exception hierarchy allows catch by base type")
        void exceptionHierarchyAllowsCatchByBaseType() {
            // This simulates catching exceptions in application code
            try {
                throw VersionNotSupportedException.of("v999", Set.of("v1", "v2"));
            } catch (ProtoWrapperException ex) {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VERSION_NOT_SUPPORTED);
            }

            try {
                throw FieldConflictException.of("M", "f", "int", "String");
            } catch (SchemaValidationException ex) {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES);
            }

            try {
                throw TypeRangeException.longToInt("f", 1L, "v1");
            } catch (ConversionException ex) {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED);
            }
        }
    }

    // ==================== Real Usage Scenario Tests ====================

    @Nested
    @DisplayName("Real Usage Scenarios")
    class RealUsageScenarioTests {

        @Test
        @DisplayName("Error handling pattern for version-specific fields")
        void errorHandlingPatternForVersionSpecificFields() {
            // Simulate checking if a field is available in current version
            Conflicts.VersionSpecificFields proto = Conflicts.VersionSpecificFields.newBuilder()
                    .setId("TEST-001")
                    .setName("Test")
                    .setStatus(1)
                    .build();

            VersionSpecificFields wrapper = new space.alnovis.protowrapper.it.model.v1.VersionSpecificFields(proto);

            // V2-only field should return false for hasXxx in V1
            if (!wrapper.hasNewFormat()) {
                // In real code, might throw or handle differently
                ErrorContext context = ErrorContext.builder()
                        .messageType("VersionSpecificFields")
                        .fieldName("newFormat")
                        .version("v1")
                        .availableVersions(Set.of("v2"))
                        .build();

                FieldNotAvailableException ex = new FieldNotAvailableException(
                        "Field 'newFormat' is not available in v1",
                        context,
                        "newFormat",
                        Set.of("v2"));

                assertThat(ex.getFieldName()).isEqualTo("newFormat");
                assertThat(ex.getAvailableInVersions()).contains("v2");
            }
        }

        @Test
        @DisplayName("Switch on error code for different handling")
        void switchOnErrorCodeForDifferentHandling() {
            // Simulate different exception types
            ProtoWrapperException[] exceptions = {
                    VersionNotSupportedException.of("v999", Set.of("v1", "v2")),
                    FieldNotAvailableException.of("Order", "newField", "v1", Set.of("v2")),
                    TypeRangeException.longToInt("count", 3_000_000_000L, "v1")
            };

            for (ProtoWrapperException ex : exceptions) {
                String handling = switch (ex.getErrorCode()) {
                    case VERSION_NOT_SUPPORTED -> "Upgrade to supported version";
                    case CONVERSION_FIELD_NOT_AVAILABLE -> "Field not in this version";
                    case CONVERSION_TYPE_RANGE_EXCEEDED -> "Value out of range";
                    default -> "Unknown error";
                };

                assertThat(handling).isNotEqualTo("Unknown error");
            }
        }

        @Test
        @DisplayName("Logging pattern with error context")
        void loggingPatternWithErrorContext() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("SensorReading")
                    .fieldPath("SensorReading.calibrationId")
                    .version("v1")
                    .fieldNumber(10)
                    .availableVersions(Set.of("v2"))
                    .addDetail("requestedOperation", "set")
                    .build();

            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE,
                    "Field not available in version",
                    context);

            // Simulate logging
            String logMessage = String.format(
                    "[%s] %s at %s - available in: %s",
                    ex.getErrorCode(),
                    ex.getMessage(),
                    context.getLocation(),
                    context.getAvailableVersions()
            );

            assertThat(logMessage).contains("CONV-003");
            assertThat(logMessage).contains("SensorReading.calibrationId");
            assertThat(logMessage).contains("v2");
        }
    }
}
