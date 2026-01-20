package io.alnovis.protowrapper.generator.conflict;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HandlerType} enum.
 */
class HandlerTypeTest {

    @Nested
    class EnumValuesTests {

        @Test
        void allHandlerTypesExist() {
            assertThat(HandlerType.values()).hasSize(13);
        }

        @Test
        void expectedTypesPresent() {
            assertThat(HandlerType.values()).containsExactlyInAnyOrder(
                    HandlerType.INT_ENUM,
                    HandlerType.ENUM_ENUM,
                    HandlerType.STRING_BYTES,
                    HandlerType.WIDENING,
                    HandlerType.FLOAT_DOUBLE,
                    HandlerType.SIGNED_UNSIGNED,
                    HandlerType.REPEATED_SINGLE,
                    HandlerType.PRIMITIVE_MESSAGE,
                    HandlerType.REPEATED_CONFLICT,
                    HandlerType.MAP_FIELD,
                    HandlerType.WELL_KNOWN_TYPE,
                    HandlerType.REPEATED_WELL_KNOWN_TYPE,
                    HandlerType.DEFAULT
            );
        }
    }

    @Nested
    class DescriptionTests {

        @ParameterizedTest
        @EnumSource(HandlerType.class)
        void allTypesHaveNonNullDescription(HandlerType type) {
            assertThat(type.getDescription()).isNotNull();
            assertThat(type.getDescription()).isNotEmpty();
        }

        @Test
        void intEnumDescription_isCorrect() {
            assertThat(HandlerType.INT_ENUM.getDescription()).isEqualTo("int/enum conflict");
        }

        @Test
        void enumEnumDescription_isCorrect() {
            assertThat(HandlerType.ENUM_ENUM.getDescription()).isEqualTo("enum/enum conflict");
        }

        @Test
        void stringBytesDescription_isCorrect() {
            assertThat(HandlerType.STRING_BYTES.getDescription()).isEqualTo("string/bytes conflict");
        }

        @Test
        void wideningDescription_isCorrect() {
            assertThat(HandlerType.WIDENING.getDescription()).isEqualTo("numeric widening");
        }

        @Test
        void floatDoubleDescription_isCorrect() {
            assertThat(HandlerType.FLOAT_DOUBLE.getDescription()).isEqualTo("float/double conflict");
        }

        @Test
        void signedUnsignedDescription_isCorrect() {
            assertThat(HandlerType.SIGNED_UNSIGNED.getDescription()).isEqualTo("signed/unsigned conflict");
        }

        @Test
        void repeatedSingleDescription_isCorrect() {
            assertThat(HandlerType.REPEATED_SINGLE.getDescription()).isEqualTo("repeated/single conflict");
        }

        @Test
        void primitiveMessageDescription_isCorrect() {
            assertThat(HandlerType.PRIMITIVE_MESSAGE.getDescription()).isEqualTo("primitive/message conflict");
        }

        @Test
        void repeatedConflictDescription_isCorrect() {
            assertThat(HandlerType.REPEATED_CONFLICT.getDescription()).isEqualTo("repeated element conflict");
        }

        @Test
        void mapFieldDescription_isCorrect() {
            assertThat(HandlerType.MAP_FIELD.getDescription()).isEqualTo("map field");
        }

        @Test
        void defaultDescription_isCorrect() {
            assertThat(HandlerType.DEFAULT.getDescription()).isEqualTo("default (no conflict)");
        }
    }

    @Nested
    class ToStringTests {

        @ParameterizedTest
        @EnumSource(HandlerType.class)
        void toString_includesNameAndDescription(HandlerType type) {
            String result = type.toString();
            assertThat(result).contains(type.name());
            assertThat(result).contains(type.getDescription());
        }

        @Test
        void toString_hasCorrectFormat() {
            String result = HandlerType.INT_ENUM.toString();
            assertThat(result).isEqualTo("INT_ENUM (int/enum conflict)");
        }
    }

    @Nested
    class HandlerMappingTests {

        @Test
        void intEnumHandler_returnsIntEnumType() {
            assertThat(IntEnumHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.INT_ENUM);
        }

        @Test
        void enumEnumHandler_returnsEnumEnumType() {
            assertThat(EnumEnumHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.ENUM_ENUM);
        }

        @Test
        void stringBytesHandler_returnsStringBytesType() {
            assertThat(StringBytesHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.STRING_BYTES);
        }

        @Test
        void wideningHandler_returnsWideningType() {
            assertThat(WideningHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.WIDENING);
        }

        @Test
        void floatDoubleHandler_returnsFloatDoubleType() {
            assertThat(FloatDoubleHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.FLOAT_DOUBLE);
        }

        @Test
        void signedUnsignedHandler_returnsSignedUnsignedType() {
            assertThat(SignedUnsignedHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.SIGNED_UNSIGNED);
        }

        @Test
        void repeatedSingleHandler_returnsRepeatedSingleType() {
            assertThat(RepeatedSingleHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.REPEATED_SINGLE);
        }

        @Test
        void primitiveMessageHandler_returnsPrimitiveMessageType() {
            assertThat(PrimitiveMessageHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.PRIMITIVE_MESSAGE);
        }

        @Test
        void repeatedConflictHandler_returnsRepeatedConflictType() {
            assertThat(RepeatedConflictHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.REPEATED_CONFLICT);
        }

        @Test
        void mapFieldHandler_returnsMapFieldType() {
            assertThat(MapFieldHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.MAP_FIELD);
        }

        @Test
        void defaultHandler_returnsDefaultType() {
            assertThat(DefaultHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.DEFAULT);
        }

        @Test
        void wellKnownTypeHandler_returnsWellKnownType() {
            assertThat(WellKnownTypeHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.WELL_KNOWN_TYPE);
        }

        @Test
        void repeatedWellKnownTypeHandler_returnsRepeatedWellKnownType() {
            assertThat(RepeatedWellKnownTypeHandler.INSTANCE.getHandlerType()).isEqualTo(HandlerType.REPEATED_WELL_KNOWN_TYPE);
        }
    }
}
