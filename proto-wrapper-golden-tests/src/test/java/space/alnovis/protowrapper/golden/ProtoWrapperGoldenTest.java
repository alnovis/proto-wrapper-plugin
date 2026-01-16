package space.alnovis.protowrapper.golden;

import com.google.protobuf.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.AllFieldTypes;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.ProtoWrapper;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.TestEnum;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden tests for ProtoWrapper interface.
 *
 * <p>Tests that ProtoWrapper interface is correctly implemented by all wrapper classes
 * and provides type-safe access to underlying protobuf messages without reflection.</p>
 *
 * @since 1.6.6
 */
@DisplayName("ProtoWrapper Golden Tests")
class ProtoWrapperGoldenTest {

    static Stream<VersionContext> allVersions() {
        return Stream.of(
            VersionContext.forVersion(1),
            VersionContext.forVersion(2)
        );
    }

    /**
     * Build AllFieldTypes with all required fields set to default values.
     * Proto2 requires all required fields to be set before build().
     */
    private AllFieldTypes.Builder buildWithRequiredFields(VersionContext ctx) {
        return ctx.newAllFieldTypesBuilder()
            .setRequiredInt32(1)
            .setRequiredInt64(1L)
            .setRequiredUint32(1)
            .setRequiredUint64(1L)
            .setRequiredSint32(1)
            .setRequiredSint64(1L)
            .setRequiredFixed32(1)
            .setRequiredFixed64(1L)
            .setRequiredSfixed32(1)
            .setRequiredSfixed64(1L)
            .setRequiredFloat(1.0f)
            .setRequiredDouble(1.0)
            .setRequiredBool(true)
            .setRequiredString("required")
            .setRequiredBytes("bytes".getBytes())
            .setRequiredMessage(ctx.newNestedMessageBuilder().setId(1).build())
            .setRequiredEnum(TestEnum.ONE);
    }

    @Nested
    @DisplayName("Interface Implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("AllFieldTypes implements ProtoWrapper")
        void allFieldTypes_implementsProtoWrapper() {
            VersionContext ctx = VersionContext.forVersion(1);
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            assertThat(msg).isInstanceOf(ProtoWrapper.class);
        }

        @Test
        @DisplayName("ProtoWrapper interface has expected methods")
        void protoWrapper_hasExpectedMethods() throws NoSuchMethodException {
            // Verify ProtoWrapper interface has all expected methods
            assertThat(ProtoWrapper.class.getMethod("getTypedProto")).isNotNull();
            assertThat(ProtoWrapper.class.getMethod("getWrapperVersion")).isNotNull();
            assertThat(ProtoWrapper.class.getMethod("toBytes")).isNotNull();
        }
    }

    @Nested
    @DisplayName("getTypedProto()")
    class GetTypedProto {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.ProtoWrapperGoldenTest#allVersions")
        @DisplayName("returns non-null Message")
        void getTypedProto_returnsNonNullMessage(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            Message proto = msg.getTypedProto();

            assertThat(proto).isNotNull();
            assertThat(proto).isInstanceOf(Message.class);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.ProtoWrapperGoldenTest#allVersions")
        @DisplayName("can access via ProtoWrapper interface cast")
        void getTypedProto_viaInterfaceCast(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            // This is the key use case - accessing proto without reflection
            ProtoWrapper wrapper = msg;
            Message proto = wrapper.getTypedProto();

            assertThat(proto).isNotNull();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.ProtoWrapperGoldenTest#allVersions")
        @DisplayName("proto serializes correctly via wrapper")
        void getTypedProto_serializesCorrectly(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            ProtoWrapper wrapper = msg;
            Message proto = wrapper.getTypedProto();

            // Proto should serialize to same bytes as wrapper
            assertThat(proto.toByteArray()).isEqualTo(wrapper.toBytes());
        }
    }

    @Nested
    @DisplayName("getWrapperVersion()")
    class GetWrapperVersion {

        @Test
        @DisplayName("returns version 1 for v1 wrapper")
        void getWrapperVersion_returnsVersion1() {
            VersionContext ctx = VersionContext.forVersion(1);
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            ProtoWrapper wrapper = msg;
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns version 2 for v2 wrapper")
        void getWrapperVersion_returnsVersion2() {
            VersionContext ctx = VersionContext.forVersion(2);
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            ProtoWrapper wrapper = msg;
            assertThat(wrapper.getWrapperVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("toBytes()")
    class ToBytes {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.ProtoWrapperGoldenTest#allVersions")
        @DisplayName("returns non-empty bytes")
        void toBytes_returnsNonEmptyBytes(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            ProtoWrapper wrapper = msg;
            byte[] bytes = wrapper.toBytes();

            assertThat(bytes).isNotNull();
            assertThat(bytes).isNotEmpty();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.ProtoWrapperGoldenTest#allVersions")
        @DisplayName("bytes can be parsed back")
        void toBytes_canBeParsedBack(VersionContext ctx) throws Exception {
            AllFieldTypes original = buildWithRequiredFields(ctx)
                .setRequiredInt32(42)
                .setRequiredString("test")
                .build();

            ProtoWrapper wrapper = original;
            byte[] bytes = wrapper.toBytes();

            AllFieldTypes parsed = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(parsed.getRequiredInt32()).isEqualTo(42);
            assertThat(parsed.getRequiredString()).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("Polymorphic Usage")
    class PolymorphicUsage {

        @Test
        @DisplayName("can process mixed version wrappers polymorphically")
        void canProcessMixedVersionWrappers() {
            // Create wrappers from different versions
            VersionContext v1Ctx = VersionContext.forVersion(1);
            VersionContext v2Ctx = VersionContext.forVersion(2);

            AllFieldTypes v1Msg = buildWithRequiredFields(v1Ctx)
                .setRequiredInt32(1)
                .setRequiredString("v1")
                .build();

            AllFieldTypes v2Msg = buildWithRequiredFields(v2Ctx)
                .setRequiredInt32(2)
                .setRequiredString("v2")
                .build();

            // Process both via ProtoWrapper interface - no reflection needed!
            int totalBytes = 0;
            for (ProtoWrapper wrapper : new ProtoWrapper[]{v1Msg, v2Msg}) {
                totalBytes += wrapper.toBytes().length;
                assertThat(wrapper.getTypedProto()).isNotNull();
            }

            assertThat(totalBytes).isGreaterThan(0);
        }

        @Test
        @DisplayName("instanceof check works correctly")
        void instanceOfCheck_worksCorrectly() {
            VersionContext ctx = VersionContext.forVersion(1);
            Object unknown = buildWithRequiredFields(ctx).build();

            // Pattern matching with ProtoWrapper
            if (unknown instanceof ProtoWrapper pw) {
                assertThat(pw.getTypedProto()).isNotNull();
                assertThat(pw.getWrapperVersion()).isEqualTo(1);
            } else {
                throw new AssertionError("Expected ProtoWrapper instance");
            }
        }
    }
}
