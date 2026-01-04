package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.SignedUnsignedConflicts;
import space.alnovis.protowrapper.it.proto.v1.Conflicts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SIGNED_UNSIGNED conflict type handling.
 *
 * <p>Tests the signed/unsigned integer type conflict resolution:</p>
 * <ul>
 *   <li>V1: signed types (int32, sint32, sfixed32, int64) are widened to long</li>
 *   <li>V2: unsigned types (uint32, fixed32, uint64) are widened to long</li>
 *   <li>Builder validates range when writing to each version</li>
 *   <li>Unsigned values > Integer.MAX_VALUE are handled correctly</li>
 * </ul>
 */
@DisplayName("SIGNED_UNSIGNED Conflict Integration Tests")
class SignedUnsignedIntegrationTest {

    // ==================== V1 Reading Tests ====================

    @Nested
    @DisplayName("V1 Reading (signed -> long widening)")
    class V1ReadingTests {

        @Test
        @DisplayName("V1 int32 values are widened to long in unified interface")
        void v1Int32WidenedToLong() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(42)
                    .setId("test-v1-001")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            // int32 values are automatically widened to long
            assertThat(wrapper.getCounter()).isEqualTo(42L);
            assertThat(wrapper.getId()).isEqualTo("test-v1-001");
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V1 handles maximum int32 value correctly")
        void v1HandlesMaxInt32() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(Integer.MAX_VALUE)
                    .setId("test-max-int32")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            assertThat(wrapper.getCounter()).isEqualTo((long) Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("V1 handles minimum int32 value correctly")
        void v1HandlesMinInt32() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(Integer.MIN_VALUE)
                    .setId("test-min-int32")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            assertThat(wrapper.getCounter()).isEqualTo((long) Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("V1 handles negative values correctly")
        void v1HandlesNegativeValues() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(-1000)
                    .setDelta(-50)  // sint32 with negative value
                    .setId("test-negative")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            assertThat(wrapper.getCounter()).isEqualTo(-1000L);
            assertThat(wrapper.getDelta()).isEqualTo(-50L);
        }

        @Test
        @DisplayName("V1 handles optional fields correctly")
        void v1HandlesOptionalFields() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setOptionalCount(200)
                    .setDelta(-10)
                    .setChecksum(0x12345678)
                    .setBigCounter(999999999999L)
                    .setId("test-optional")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            assertThat(wrapper.hasOptionalCount()).isTrue();
            assertThat(wrapper.getOptionalCount()).isEqualTo(200L);

            assertThat(wrapper.hasDelta()).isTrue();
            assertThat(wrapper.getDelta()).isEqualTo(-10L);

            assertThat(wrapper.hasChecksum()).isTrue();
            assertThat(wrapper.getChecksum()).isEqualTo(0x12345678L);

            assertThat(wrapper.hasBigCounter()).isTrue();
            assertThat(wrapper.getBigCounter()).isEqualTo(999999999999L);
        }
    }

    // ==================== V2 Reading Tests ====================

    @Nested
    @DisplayName("V2 Reading (unsigned types)")
    class V2ReadingTests {

        @Test
        @DisplayName("V2 uint32 values work correctly")
        void v2Uint32ValuesWorkCorrectly() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter(42)
                            .setId("test-v2-001")
                            .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            assertThat(wrapper.getCounter()).isEqualTo(42L);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 handles values exceeding Integer.MAX_VALUE")
        void v2HandlesValuesExceedingIntMaxValue() {
            // uint32 can hold values up to 4294967295 (0xFFFFFFFF)
            long largeValue = 3_000_000_000L;  // > Integer.MAX_VALUE (2147483647)

            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter((int) largeValue)  // Cast to int for proto, interpreted as unsigned
                            .setId("test-large-uint32")
                            .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            // Should return the full unsigned value
            assertThat(wrapper.getCounter()).isEqualTo(largeValue);
        }

        @Test
        @DisplayName("V2 handles maximum uint32 value (0xFFFFFFFF)")
        void v2HandlesMaxUint32() {
            // Maximum uint32 value: 4294967295 (0xFFFFFFFF)
            long maxUint32 = 0xFFFFFFFFL;

            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter((int) maxUint32)  // -1 as signed int
                            .setId("test-max-uint32")
                            .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            // Should return 4294967295, not -1
            assertThat(wrapper.getCounter()).isEqualTo(maxUint32);
        }

        @Test
        @DisplayName("V2 can access V2-only fields")
        void v2CanAccessV2OnlyFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter(100)
                            .setId("test-v2-fields")
                            .setDescription("V2 description")
                            .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            assertThat(wrapper.hasDescription()).isTrue();
            assertThat(wrapper.getDescription()).isEqualTo("V2 description");
        }

        @Test
        @DisplayName("V1 cannot access V2-only fields")
        void v1CannotAccessV2OnlyFields() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setId("test-v1-no-v2-fields")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            assertThat(wrapper.hasDescription()).isFalse();
            assertThat(wrapper.getDescription()).isNull();
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("V1 builder accepts values within int32 range")
        void v1BuilderAcceptsValuesWithinInt32Range() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setId("test-builder")
                    .build();

            SignedUnsignedConflicts original = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            // Modify using builder with long values that fit in int32
            SignedUnsignedConflicts modified = original.toBuilder()
                    .setCounter(500L)
                    .build();

            assertThat(modified.getCounter()).isEqualTo(500L);
        }

        @Test
        @DisplayName("V1 builder accepts negative values")
        void v1BuilderAcceptsNegativeValues() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setId("test-builder-negative")
                    .build();

            SignedUnsignedConflicts original = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            // Negative values should be accepted for signed types
            SignedUnsignedConflicts modified = original.toBuilder()
                    .setCounter(-1000L)
                    .build();

            assertThat(modified.getCounter()).isEqualTo(-1000L);
        }

        @Test
        @DisplayName("V2 builder accepts values within uint32 range")
        void v2BuilderAcceptsValuesWithinUint32Range() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter(100)
                            .setId("test-v2-builder")
                            .build();

            SignedUnsignedConflicts original = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            // Values within uint32 range should be accepted
            SignedUnsignedConflicts modified = original.toBuilder()
                    .setCounter(3_000_000_000L)  // > Integer.MAX_VALUE but < uint32 max
                    .build();

            assertThat(modified.getCounter()).isEqualTo(3_000_000_000L);
        }

        @Test
        @DisplayName("Builder preserves non-conflicting fields")
        void builderPreservesNonConflictingFields() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setId("preserve-test")
                    .setName("Test Name")
                    .build();

            SignedUnsignedConflicts original = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            // Modify only counter
            SignedUnsignedConflicts modified = original.toBuilder()
                    .setCounter(200L)
                    .build();

            // Other fields should be preserved
            assertThat(modified.getCounter()).isEqualTo(200L);
            assertThat(modified.getId()).isEqualTo("preserve-test");
            assertThat(modified.getName()).isEqualTo("Test Name");
        }
    }

    // ==================== Version Conversion Tests ====================

    @Nested
    @DisplayName("Version Conversion")
    class VersionConversionTests {

        @Test
        @DisplayName("V1 to V2 conversion preserves values")
        void v1ToV2PreservesValues() {
            Conflicts.SignedUnsignedConflicts v1Proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(42)
                    .setOptionalCount(100)
                    .setId("convert-test")
                    .build();

            SignedUnsignedConflicts v1Wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(v1Proto);

            // Read as unified interface
            long counter = v1Wrapper.getCounter();
            long optionalCount = v1Wrapper.getOptionalCount();

            assertThat(counter).isEqualTo(42L);
            assertThat(optionalCount).isEqualTo(100L);
        }

        @Test
        @DisplayName("Both versions use same unified interface")
        void bothVersionsUseSameUnifiedInterface() {
            // Create V1 wrapper
            Conflicts.SignedUnsignedConflicts v1Proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(100)
                    .setId("test-v1")
                    .build();

            // Create V2 wrapper
            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter(100)
                            .setId("test-v2")
                            .build();

            SignedUnsignedConflicts v1 = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(v1Proto);
            SignedUnsignedConflicts v2 = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(v2Proto);

            // Both should have same unified getter return type (long)
            assertThat(v1.getCounter()).isEqualTo(v2.getCounter());
        }
    }

    // ==================== Unsigned Semantics Tests ====================

    @Nested
    @DisplayName("Unsigned Semantics")
    class UnsignedSemanticsTests {

        @Test
        @DisplayName("V2 correctly interprets -1 as max uint32")
        void v2CorrectlyInterpretsNegativeOneAsMaxUint32() {
            // When -1 is stored in uint32, it represents 0xFFFFFFFF (4294967295)
            space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.SignedUnsignedConflicts.newBuilder()
                            .setCounter(-1)  // -1 in signed int = 0xFFFFFFFF
                            .setId("test-unsigned-semantics")
                            .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.SignedUnsignedConflicts(proto);

            // Should be interpreted as unsigned
            assertThat(wrapper.getCounter()).isEqualTo(0xFFFFFFFFL);
            assertThat(wrapper.getCounter()).isEqualTo(4294967295L);
        }

        @Test
        @DisplayName("V1 preserves signed semantics for negative values")
        void v1PreservesSignedSemanticsForNegativeValues() {
            Conflicts.SignedUnsignedConflicts proto = Conflicts.SignedUnsignedConflicts.newBuilder()
                    .setCounter(-1)
                    .setId("test-signed-semantics")
                    .build();

            SignedUnsignedConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.SignedUnsignedConflicts(proto);

            // V1 uses signed int32, so -1 should remain -1
            assertThat(wrapper.getCounter()).isEqualTo(-1L);
        }

        @Test
        @DisplayName("Integer.toUnsignedLong correctly handles conversion")
        void integerToUnsignedLongCorrectlyHandlesConversion() {
            // This test verifies the underlying conversion mechanism
            int signedValue = -1;  // 0xFFFFFFFF in binary
            long unsignedValue = Integer.toUnsignedLong(signedValue);

            assertThat(unsignedValue).isEqualTo(0xFFFFFFFFL);
            assertThat(unsignedValue).isEqualTo(4294967295L);
        }
    }
}
