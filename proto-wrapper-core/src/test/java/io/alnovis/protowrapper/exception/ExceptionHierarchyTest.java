package io.alnovis.protowrapper.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the proto-wrapper exception hierarchy.
 */
@DisplayName("Exception Hierarchy Tests")
class ExceptionHierarchyTest {

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTests {

        @Test
        @DisplayName("should have unique codes")
        void shouldHaveUniqueCodes() {
            Set<String> codes = new java.util.HashSet<>();
            for (ErrorCode code : ErrorCode.values()) {
                assertTrue(codes.add(code.getCode()),
                        "Duplicate error code: " + code.getCode());
            }
        }

        @Test
        @DisplayName("should format correctly")
        void shouldFormatCorrectly() {
            ErrorCode code = ErrorCode.SCHEMA_INCOMPATIBLE_TYPES;
            String formatted = code.format();
            assertTrue(formatted.contains("SCHEMA-001"));
            assertTrue(formatted.contains("Incompatible"));
        }

        @Test
        @DisplayName("toString should return code")
        void toStringShouldReturnCode() {
            assertEquals("SCHEMA-001", ErrorCode.SCHEMA_INCOMPATIBLE_TYPES.toString());
            assertEquals("CONV-001", ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED.toString());
        }
    }

    @Nested
    @DisplayName("ErrorContext")
    class ErrorContextTests {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("Order")
                    .fieldPath("Order.items[].name")
                    .fieldName("name")
                    .version("v3")
                    .fieldNumber(5)
                    .availableVersions(Set.of("v1", "v2"))
                    .addDetail("customKey", "customValue")
                    .build();

            assertEquals("Order", context.getMessageType());
            assertEquals("Order.items[].name", context.getFieldPath());
            assertEquals("name", context.getFieldName());
            assertEquals("v3", context.getVersion());
            assertEquals(5, context.getFieldNumber());
            assertEquals(Set.of("v1", "v2"), context.getAvailableVersions());
            assertEquals("customValue", context.getDetail("customKey"));
        }

        @Test
        @DisplayName("should create location string")
        void shouldCreateLocationString() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("Order")
                    .fieldPath("Order.status")
                    .version("v3")
                    .build();

            assertEquals("Order.status (v3)", context.getLocation());
        }

        @Test
        @DisplayName("should create from factory methods")
        void shouldCreateFromFactoryMethods() {
            ErrorContext forMessage = ErrorContext.forMessage("Person");
            assertEquals("Person", forMessage.getMessageType());

            ErrorContext forField = ErrorContext.forField("Order", "status");
            assertEquals("Order", forField.getMessageType());
            assertEquals("status", forField.getFieldName());

            ErrorContext forVersion = ErrorContext.forVersion("Product", "v1");
            assertEquals("Product", forVersion.getMessageType());
            assertEquals("v1", forVersion.getVersion());
        }
    }

    @Nested
    @DisplayName("ProtoWrapperException")
    class ProtoWrapperExceptionTests {

        @Test
        @DisplayName("should include error code in message")
        void shouldIncludeErrorCodeInMessage() {
            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.VERSION_NOT_SUPPORTED,
                    "Version v999 not supported");

            assertTrue(ex.getMessage().contains("[VER-001]"));
            assertTrue(ex.getMessage().contains("Version v999"));
        }

        @Test
        @DisplayName("should include context location in message")
        void shouldIncludeContextLocationInMessage() {
            ErrorContext context = ErrorContext.forField("Order", "status");
            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE,
                    "Field not available",
                    context);

            assertTrue(ex.getMessage().contains("Order.status"));
        }

        @Test
        @DisplayName("should provide context accessors")
        void shouldProvideContextAccessors() {
            ErrorContext context = ErrorContext.builder()
                    .messageType("Order")
                    .fieldPath("Order.items")
                    .version("v3")
                    .build();

            ProtoWrapperException ex = new ProtoWrapperException(
                    ErrorCode.CONVERSION_TYPE_MISMATCH, "Type mismatch", context);

            assertEquals("Order", ex.getMessageType());
            assertEquals("Order.items", ex.getFieldPath());
            assertEquals("v3", ex.getVersion());
            assertEquals("Order.items (v3)", ex.getLocation());
        }
    }

    @Nested
    @DisplayName("SchemaValidationException")
    class SchemaValidationExceptionTests {

        @Test
        @DisplayName("should create incompatible types exception")
        void shouldCreateIncompatibleTypesException() {
            SchemaValidationException ex = SchemaValidationException.incompatibleTypes(
                    "Order", "status", "String", "int");

            assertInstanceOf(FieldConflictException.class, ex);
            assertEquals(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, ex.getErrorCode());

            FieldConflictException fce = (FieldConflictException) ex;
            assertEquals("String", fce.getType1());
            assertEquals("int", fce.getType2());
        }

        @Test
        @DisplayName("should create field number conflict exception")
        void shouldCreateFieldNumberConflictException() {
            SchemaValidationException ex = SchemaValidationException.fieldNumberConflict(
                    "Order", 5, "oldField", "newField");

            assertEquals(ErrorCode.SCHEMA_FIELD_NUMBER_CONFLICT, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("5"));
            assertTrue(ex.getMessage().contains("oldField"));
            assertTrue(ex.getMessage().contains("newField"));
        }
    }

    @Nested
    @DisplayName("FieldConflictException")
    class FieldConflictExceptionTests {

        @Test
        @DisplayName("should create with factory method")
        void shouldCreateWithFactoryMethod() {
            FieldConflictException ex = FieldConflictException.of(
                    "Person", "age", "int", "String");

            assertEquals("int", ex.getType1());
            assertEquals("String", ex.getType2());
            assertEquals(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("OneofConflictException")
    class OneofConflictExceptionTests {

        @Test
        @DisplayName("should create field membership change exception")
        void shouldCreateFieldMembershipChangeException() {
            OneofConflictException ex = OneofConflictException.fieldMembershipChange(
                    "Payment", "method", "creditCard",
                    Set.of("v1", "v2"), Set.of("v3"));

            assertEquals("method", ex.getOneofName());
            assertEquals(Set.of("v1", "v2"), ex.getVersionsAffected());
            assertEquals(ErrorCode.SCHEMA_ONEOF_CONFLICT, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("EnumValueNotSupportedException")
    class EnumValueNotSupportedExceptionTests {

        @Test
        @DisplayName("should create with factory method")
        void shouldCreateWithFactoryMethod() {
            EnumValueNotSupportedException ex = EnumValueNotSupportedException.of(
                    "StatusEnum", "DELETED", 4, "v1",
                    Set.of("ACTIVE", "INACTIVE"));

            assertEquals("StatusEnum", ex.getEnumTypeName());
            assertEquals("DELETED", ex.getEnumValue());
            assertEquals(4, ex.getEnumNumber());
            assertEquals(Set.of("ACTIVE", "INACTIVE"), ex.getValidValues());
            assertEquals(ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("TypeRangeException")
    class TypeRangeExceptionTests {

        @Test
        @DisplayName("should create long to int exception")
        void shouldCreateLongToIntException() {
            TypeRangeException ex = TypeRangeException.longToInt("count", 3_000_000_000L, "v1");

            assertEquals("long", ex.getSourceType());
            assertEquals("int", ex.getTargetType());
            assertEquals(3_000_000_000L, ex.getValue());
            assertEquals(Integer.MIN_VALUE, ex.getMinValue());
            assertEquals(Integer.MAX_VALUE, ex.getMaxValue());
            assertEquals(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should create double to float exception")
        void shouldCreateDoubleToFloatException() {
            TypeRangeException ex = TypeRangeException.doubleToFloat("value", Double.MAX_VALUE, "v1");

            assertEquals("double", ex.getSourceType());
            assertEquals("float", ex.getTargetType());
            assertEquals(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("FieldNotAvailableException")
    class FieldNotAvailableExceptionTests {

        @Test
        @DisplayName("should create with factory method")
        void shouldCreateWithFactoryMethod() {
            FieldNotAvailableException ex = FieldNotAvailableException.of(
                    "Order", "description", "v1", Set.of("v2", "v3"));

            assertEquals("description", ex.getFieldName());
            assertEquals(Set.of("v2", "v3"), ex.getAvailableInVersions());
            assertEquals(ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("v1"));
            assertTrue(ex.getMessage().contains("v2"));
        }
    }

    @Nested
    @DisplayName("VersionNotSupportedException")
    class VersionNotSupportedExceptionTests {

        @Test
        @DisplayName("should create with factory method")
        void shouldCreateWithFactoryMethod() {
            VersionNotSupportedException ex = VersionNotSupportedException.of(
                    "v999", Set.of("v1", "v2", "v3"));

            assertEquals("v999", ex.getRequestedVersion());
            assertEquals(Set.of("v1", "v2", "v3"), ex.getSupportedVersions());
            assertEquals(ErrorCode.VERSION_NOT_SUPPORTED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should create for message")
        void shouldCreateForMessage() {
            VersionNotSupportedException ex = VersionNotSupportedException.forMessage(
                    "LegacyOrder", "v3", Set.of("v1", "v2"));

            assertEquals("v3", ex.getRequestedVersion());
            assertTrue(ex.getMessage().contains("LegacyOrder"));
            assertTrue(ex.getMessage().contains("v3"));
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy")
    class HierarchyTests {

        @Test
        @DisplayName("all exceptions should extend ProtoWrapperException")
        void allExceptionsShouldExtendBase() {
            assertInstanceOf(ProtoWrapperException.class,
                    new SchemaValidationException(ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, "test"));
            assertInstanceOf(ProtoWrapperException.class,
                    new ConversionException(ErrorCode.CONVERSION_TYPE_MISMATCH, "test"));
            assertInstanceOf(ProtoWrapperException.class,
                    VersionNotSupportedException.of("v1", Set.of("v2")));
        }

        @Test
        @DisplayName("all exceptions should be RuntimeException")
        void allExceptionsShouldBeRuntimeException() {
            assertInstanceOf(RuntimeException.class,
                    new ProtoWrapperException(ErrorCode.VERSION_NOT_SUPPORTED, "test"));
        }

        @Test
        @DisplayName("subclasses should extend correct parent")
        void subclassesShouldExtendCorrectParent() {
            assertInstanceOf(SchemaValidationException.class,
                    FieldConflictException.of("M", "f", "a", "b"));
            assertInstanceOf(SchemaValidationException.class,
                    OneofConflictException.fieldMembershipChange("M", "o", "f", Set.of(), Set.of()));
            assertInstanceOf(ConversionException.class,
                    EnumValueNotSupportedException.of("E", "V", 1, "v1", Set.of()));
            assertInstanceOf(ConversionException.class,
                    TypeRangeException.longToInt("f", 1L, "v1"));
            assertInstanceOf(ConversionException.class,
                    FieldNotAvailableException.of("M", "f", "v1", Set.of()));
        }
    }
}
