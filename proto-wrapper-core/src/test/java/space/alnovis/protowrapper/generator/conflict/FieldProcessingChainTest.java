package space.alnovis.protowrapper.generator.conflict;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FieldProcessingChain}.
 */
class FieldProcessingChainTest {

    private FieldProcessingChain chain;

    @BeforeEach
    void setUp() {
        chain = FieldProcessingChain.getInstance();
    }

    @Nested
    class SingletonTests {

        @Test
        void getInstance_returnsSameInstance() {
            FieldProcessingChain instance1 = FieldProcessingChain.getInstance();
            FieldProcessingChain instance2 = FieldProcessingChain.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }

        @Test
        void getInstance_returnsNonNull() {
            assertThat(FieldProcessingChain.getInstance()).isNotNull();
        }
    }

    @Nested
    class FindHandlerTests {

        @Test
        void findHandler_noConflict_returnsDefaultHandler() {
            MergedField field = createField(MergedField.ConflictType.NONE);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.DEFAULT);
        }

        @Test
        void findHandler_intEnumConflict_returnsIntEnumHandler() {
            MergedField field = createField(MergedField.ConflictType.INT_ENUM);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.INT_ENUM);
        }

        @Test
        void findHandler_enumEnumConflict_returnsEnumEnumHandler() {
            MergedField field = createField(MergedField.ConflictType.ENUM_ENUM);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.ENUM_ENUM);
        }

        @Test
        void findHandler_stringBytesConflict_returnsStringBytesHandler() {
            MergedField field = createField(MergedField.ConflictType.STRING_BYTES);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.STRING_BYTES);
        }

        @Test
        void findHandler_wideningConflict_returnsWideningHandler() {
            MergedField field = createField(MergedField.ConflictType.WIDENING);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.WIDENING);
        }

        @Test
        void findHandler_floatDoubleConflict_returnsFloatDoubleHandler() {
            MergedField field = createField(MergedField.ConflictType.FLOAT_DOUBLE);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.FLOAT_DOUBLE);
        }

        @Test
        void findHandler_signedUnsignedConflict_returnsSignedUnsignedHandler() {
            MergedField field = createField(MergedField.ConflictType.SIGNED_UNSIGNED);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.SIGNED_UNSIGNED);
        }

        @Test
        void findHandler_repeatedSingleConflict_returnsRepeatedSingleHandler() {
            MergedField field = createField(MergedField.ConflictType.REPEATED_SINGLE);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.REPEATED_SINGLE);
        }

        @Test
        void findHandler_primitiveMessageConflict_returnsPrimitiveMessageHandler() {
            MergedField field = createField(MergedField.ConflictType.PRIMITIVE_MESSAGE);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.PRIMITIVE_MESSAGE);
        }
    }

    @Nested
    class ChainOrderingTests {

        @Test
        void defaultHandler_isAlwaysFallback() {
            MergedField field = createField(null);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.DEFAULT);
        }

        @Test
        void conflictHandler_hasPriorityOverDefault() {
            MergedField field = createField(MergedField.ConflictType.INT_ENUM);

            ConflictHandler handler = chain.findHandler(field, null);

            assertThat(handler.getHandlerType()).isNotEqualTo(HandlerType.DEFAULT);
            assertThat(handler.getHandlerType()).isEqualTo(HandlerType.INT_ENUM);
        }
    }

    private MergedField createField(MergedField.ConflictType conflictType) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        FieldInfo fieldInfo = new FieldInfo(proto);

        return MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .conflictType(conflictType)
                .build();
    }
}
