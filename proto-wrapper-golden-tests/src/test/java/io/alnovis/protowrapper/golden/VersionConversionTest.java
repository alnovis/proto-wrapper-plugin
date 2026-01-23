package io.alnovis.protowrapper.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.alnovis.protowrapper.golden.proto2.wrapper.api.AllFieldTypes;
import io.alnovis.protowrapper.golden.proto2.wrapper.api.NestedMessage;
import io.alnovis.protowrapper.golden.proto2.wrapper.api.TestEnum;
import io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for version conversion functionality.
 *
 * <p>Tests asVersion(VersionContext) and asVersion(String) methods
 * for converting wrapper instances between protocol versions.</p>
 *
 * @since 2.2.0
 */
@DisplayName("Version Conversion Tests")
class VersionConversionTest {

    private static final VersionContext V1 = VersionContext.forVersionId("v1");
    private static final VersionContext V2 = VersionContext.forVersionId("v2");

    static Stream<VersionContext> allVersions() {
        return Stream.of(V1, V2);
    }

    static Stream<Arguments> versionPairs() {
        return Stream.of(
            Arguments.of(V1, V2),
            Arguments.of(V2, V1)
        );
    }

    /**
     * Build AllFieldTypes with all required fields set.
     */
    private AllFieldTypes.Builder buildWithRequiredFields(VersionContext ctx) {
        return ctx.newAllFieldTypesBuilder()
            .setRequiredInt32(42)
            .setRequiredInt64(100L)
            .setRequiredUint32(1)
            .setRequiredUint64(1L)
            .setRequiredSint32(1)
            .setRequiredSint64(1L)
            .setRequiredFixed32(1)
            .setRequiredFixed64(1L)
            .setRequiredSfixed32(1)
            .setRequiredSfixed64(1L)
            .setRequiredFloat(3.14f)
            .setRequiredDouble(2.718)
            .setRequiredBool(true)
            .setRequiredString("test-string")
            .setRequiredBytes("test-bytes".getBytes())
            .setRequiredMessage(ctx.newNestedMessageBuilder().setId(999).build())
            .setRequiredEnum(TestEnum.TWO);
    }

    // ==================== asVersion(VersionContext) Tests ====================

    @Nested
    @DisplayName("asVersion(VersionContext)")
    class AsVersionByContext {

        @ParameterizedTest(name = "from {0} to {1}")
        @MethodSource("io.alnovis.protowrapper.golden.VersionConversionTest#versionPairs")
        @DisplayName("converts between versions preserving data")
        void convertsPreservingData(VersionContext source, VersionContext target) {
            AllFieldTypes original = buildWithRequiredFields(source).build();

            AllFieldTypes converted = original.asVersion(target);

            assertThat(converted.getWrapperVersionId()).isEqualTo(target.getVersionId());
            assertThat(converted.getRequiredInt32()).isEqualTo(42);
            assertThat(converted.getRequiredInt64()).isEqualTo(100L);
            assertThat(converted.getRequiredFloat()).isEqualTo(3.14f);
            assertThat(converted.getRequiredDouble()).isEqualTo(2.718);
            assertThat(converted.isRequiredBool()).isTrue();
            assertThat(converted.getRequiredString()).isEqualTo("test-string");
            assertThat(converted.getRequiredEnum()).isEqualTo(TestEnum.TWO);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("io.alnovis.protowrapper.golden.VersionConversionTest#allVersions")
        @DisplayName("returns same instance when converting to same version")
        void returnsSameInstanceForSameVersion(VersionContext ctx) {
            AllFieldTypes original = buildWithRequiredFields(ctx).build();

            AllFieldTypes converted = original.asVersion(ctx);

            assertThat(converted).isSameAs(original);
        }

        @ParameterizedTest(name = "from {0} to {1}")
        @MethodSource("io.alnovis.protowrapper.golden.VersionConversionTest#versionPairs")
        @DisplayName("preserves nested message data")
        void preservesNestedMessageData(VersionContext source, VersionContext target) {
            NestedMessage nested = source.newNestedMessageBuilder()
                .setId(12345)
                .setName("nested-name")
                .build();

            AllFieldTypes original = buildWithRequiredFields(source)
                .setRequiredMessage(nested)
                .build();

            AllFieldTypes converted = original.asVersion(target);

            assertThat(converted.getRequiredMessage().getId()).isEqualTo(12345);
            assertThat(converted.getRequiredMessage().getName()).isEqualTo("nested-name");
        }

        @ParameterizedTest(name = "from {0} to {1}")
        @MethodSource("io.alnovis.protowrapper.golden.VersionConversionTest#versionPairs")
        @DisplayName("preserves bytes field")
        void preservesBytesField(VersionContext source, VersionContext target) {
            byte[] testBytes = {0x01, 0x02, 0x03, (byte) 0xFF};
            AllFieldTypes original = buildWithRequiredFields(source)
                .setRequiredBytes(testBytes)
                .build();

            AllFieldTypes converted = original.asVersion(target);

            assertThat(converted.getRequiredBytes()).isEqualTo(testBytes);
        }

        @Test
        @DisplayName("converted instance is independent from original")
        void convertedInstanceIsIndependent() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            AllFieldTypes converted = original.asVersion(V2);

            // Verify they are different instances
            assertThat(converted).isNotSameAs(original);
            // Verify different version IDs
            assertThat(original.getWrapperVersionId()).isEqualTo("v1");
            assertThat(converted.getWrapperVersionId()).isEqualTo("v2");
        }
    }

    // ==================== asVersion(String) Tests ====================

    @Nested
    @DisplayName("asVersion(String)")
    class AsVersionByVersionId {

        @Test
        @DisplayName("converts from v1 to v2 by version ID")
        void convertsFromV1ToV2() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            AllFieldTypes converted = original.asVersion("v2");

            assertThat(converted.getWrapperVersionId()).isEqualTo("v2");
            assertThat(converted.getRequiredInt32()).isEqualTo(42);
            assertThat(converted.getRequiredString()).isEqualTo("test-string");
        }

        @Test
        @DisplayName("converts from v2 to v1 by version ID")
        void convertsFromV2ToV1() {
            AllFieldTypes original = buildWithRequiredFields(V2).build();

            AllFieldTypes converted = original.asVersion("v1");

            assertThat(converted.getWrapperVersionId()).isEqualTo("v1");
            assertThat(converted.getRequiredInt32()).isEqualTo(42);
            assertThat(converted.getRequiredString()).isEqualTo("test-string");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("io.alnovis.protowrapper.golden.VersionConversionTest#allVersions")
        @DisplayName("returns same instance when converting to same version by ID")
        void returnsSameInstanceForSameVersionId(VersionContext ctx) {
            AllFieldTypes original = buildWithRequiredFields(ctx).build();

            AllFieldTypes converted = original.asVersion(ctx.getVersionId());

            assertThat(converted).isSameAs(original);
        }

        @Test
        @DisplayName("throws exception for invalid version ID")
        void throwsForInvalidVersionId() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            assertThatThrownBy(() -> original.asVersion("v999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("v999");
        }

        @Test
        @DisplayName("throws exception for null version ID")
        void throwsForNullVersionId() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            assertThatThrownBy(() -> original.asVersion((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== Round-trip Conversion Tests ====================

    @Nested
    @DisplayName("Round-trip Conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("v1 -> v2 -> v1 preserves all data")
        void v1ToV2ToV1PreservesData() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            AllFieldTypes toV2 = original.asVersion(V2);
            AllFieldTypes backToV1 = toV2.asVersion(V1);

            assertThat(backToV1.getWrapperVersionId()).isEqualTo("v1");
            assertThat(backToV1.getRequiredInt32()).isEqualTo(original.getRequiredInt32());
            assertThat(backToV1.getRequiredInt64()).isEqualTo(original.getRequiredInt64());
            assertThat(backToV1.getRequiredString()).isEqualTo(original.getRequiredString());
            assertThat(backToV1.getRequiredEnum()).isEqualTo(original.getRequiredEnum());
            assertThat(backToV1.getRequiredMessage().getId())
                .isEqualTo(original.getRequiredMessage().getId());
        }

        @Test
        @DisplayName("v2 -> v1 -> v2 preserves all data")
        void v2ToV1ToV2PreservesData() {
            AllFieldTypes original = buildWithRequiredFields(V2).build();

            AllFieldTypes toV1 = original.asVersion(V1);
            AllFieldTypes backToV2 = toV1.asVersion(V2);

            assertThat(backToV2.getWrapperVersionId()).isEqualTo("v2");
            assertThat(backToV2.getRequiredInt32()).isEqualTo(original.getRequiredInt32());
            assertThat(backToV2.getRequiredInt64()).isEqualTo(original.getRequiredInt64());
            assertThat(backToV2.getRequiredString()).isEqualTo(original.getRequiredString());
            assertThat(backToV2.getRequiredEnum()).isEqualTo(original.getRequiredEnum());
        }

        @Test
        @DisplayName("bytes are identical after round-trip")
        void bytesIdenticalAfterRoundTrip() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();
            byte[] originalBytes = original.toBytes();

            AllFieldTypes roundTripped = original.asVersion(V2).asVersion(V1);
            byte[] roundTrippedBytes = roundTripped.toBytes();

            assertThat(roundTrippedBytes).isEqualTo(originalBytes);
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("can mix asVersion(VersionContext) and asVersion(String)")
        void canMixConversionMethods() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            // Convert using VersionContext, then back using String
            AllFieldTypes step1 = original.asVersion(V2);
            AllFieldTypes step2 = step1.asVersion("v1");

            assertThat(step2.getWrapperVersionId()).isEqualTo("v1");
            assertThat(step2.getRequiredInt32()).isEqualTo(42);
        }

        @Test
        @DisplayName("getContext returns correct context after conversion")
        void getContextReturnsCorrectContextAfterConversion() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();
            assertThat(original.getContext().getVersionId()).isEqualTo("v1");

            AllFieldTypes converted = original.asVersion(V2);
            assertThat(converted.getContext().getVersionId()).isEqualTo("v2");
        }

        @Test
        @DisplayName("converted wrapper can be used to create new instances")
        void convertedWrapperCanCreateNewInstances() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();
            AllFieldTypes converted = original.asVersion(V2);

            // Use the converted wrapper's context to create a new message
            VersionContext ctx = converted.getContext();
            NestedMessage newNested = ctx.newNestedMessageBuilder()
                .setId(777)
                .setName("created-from-converted")
                .build();

            assertThat(newNested.getWrapperVersionId()).isEqualTo("v2");
            assertThat(newNested.getId()).isEqualTo(777);
        }

        @Test
        @DisplayName("chained conversions work correctly")
        void chainedConversionsWork() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            // Chain multiple conversions
            AllFieldTypes result = original
                .asVersion(V2)
                .asVersion("v1")
                .asVersion(V2)
                .asVersion("v1");

            assertThat(result.getWrapperVersionId()).isEqualTo("v1");
            assertThat(result.getRequiredInt32()).isEqualTo(42);
            assertThat(result.getRequiredString()).isEqualTo("test-string");
        }

        @Test
        @DisplayName("conversion works with nested message conversion")
        void conversionWorksWithNestedMessages() {
            // Create nested message in v1
            NestedMessage nestedV1 = V1.newNestedMessageBuilder()
                .setId(123)
                .setName("nested")
                .build();

            // Convert nested message to v2
            NestedMessage nestedV2 = nestedV1.asVersion(V2);

            // Use converted nested in AllFieldTypes
            AllFieldTypes msg = buildWithRequiredFields(V2)
                .setRequiredMessage(nestedV2)
                .build();

            assertThat(msg.getRequiredMessage().getId()).isEqualTo(123);
            assertThat(msg.getRequiredMessage().getName()).isEqualTo("nested");
            assertThat(msg.getRequiredMessage().getWrapperVersionId()).isEqualTo("v2");
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("conversion preserves default values")
        void conversionPreservesDefaultValues() {
            // Create message with only required fields (optional fields use defaults)
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            AllFieldTypes converted = original.asVersion(V2);

            // Optional fields should have default values
            assertThat(converted.hasOptionalInt32()).isFalse();
            assertThat(converted.hasOptionalString()).isFalse();
        }

        @Test
        @DisplayName("conversion preserves optional fields when set")
        void conversionPreservesOptionalFieldsWhenSet() {
            AllFieldTypes original = buildWithRequiredFields(V1)
                .setOptionalInt32(999)
                .setOptionalString("optional-value")
                .build();

            AllFieldTypes converted = original.asVersion(V2);

            assertThat(converted.hasOptionalInt32()).isTrue();
            assertThat(converted.getOptionalInt32()).isEqualTo(999);
            assertThat(converted.hasOptionalString()).isTrue();
            assertThat(converted.getOptionalString()).isEqualTo("optional-value");
        }

        @Test
        @DisplayName("empty repeated fields remain empty after conversion")
        void emptyRepeatedFieldsRemainEmpty() {
            AllFieldTypes original = buildWithRequiredFields(V1).build();

            AllFieldTypes converted = original.asVersion(V2);

            assertThat(converted.getRepeatedInt32()).isEmpty();
            assertThat(converted.getRepeatedString()).isEmpty();
        }

        @Test
        @DisplayName("repeated fields are preserved after conversion")
        void repeatedFieldsPreserved() {
            AllFieldTypes original = buildWithRequiredFields(V1)
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .addRepeatedInt32(3)
                .addRepeatedString("a")
                .addRepeatedString("b")
                .build();

            AllFieldTypes converted = original.asVersion(V2);

            assertThat(converted.getRepeatedInt32()).containsExactly(1, 2, 3);
            assertThat(converted.getRepeatedString()).containsExactly("a", "b");
        }
    }
}
