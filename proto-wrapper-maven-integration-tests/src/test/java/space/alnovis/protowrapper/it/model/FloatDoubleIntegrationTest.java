package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.FloatDoubleConflicts;
import space.alnovis.protowrapper.it.proto.v1.Conflicts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for FLOAT_DOUBLE conflict type handling.
 *
 * <p>Tests the float to double type widening conflict resolution:</p>
 * <ul>
 *   <li>V1: float fields are automatically widened to double in unified interface</li>
 *   <li>V2: double fields work natively</li>
 *   <li>Builder validates float range when writing to V1</li>
 *   <li>Special values (NaN, Infinity) are preserved</li>
 * </ul>
 */
@DisplayName("FLOAT_DOUBLE Conflict Integration Tests")
class FloatDoubleIntegrationTest {

    // ==================== V1 Reading Tests ====================

    @Nested
    @DisplayName("V1 Reading (float -> double widening)")
    class V1ReadingTests {

        @Test
        @DisplayName("V1 float values are widened to double in unified interface")
        void v1FloatWidenedToDouble() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(23.5f)
                    .setHumidity(65.0f)
                    .setSensorId("SENSOR-V1-001")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Float values are automatically widened to double
            assertThat(wrapper.getTemperature()).isCloseTo(23.5, within(0.0001));
            assertThat(wrapper.getHumidity()).isCloseTo(65.0, within(0.0001));
            assertThat(wrapper.getSensorId()).isEqualTo("SENSOR-V1-001");
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V1 optional float fields work correctly")
        void v1OptionalFloatFields() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setPressure(1013.25f)
                    .setWindSpeed(5.5f)
                    .setSensorId("SENSOR-V1-002")
                    .setLocation("Test Location")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            assertThat(wrapper.hasPressure()).isTrue();
            assertThat(wrapper.getPressure()).isCloseTo(1013.25, within(0.01));

            assertThat(wrapper.hasWindSpeed()).isTrue();
            assertThat(wrapper.getWindSpeed()).isCloseTo(5.5, within(0.0001));

            assertThat(wrapper.hasLocation()).isTrue();
            assertThat(wrapper.getLocation()).isEqualTo("Test Location");
        }

        @Test
        @DisplayName("V1 handles maximum float value correctly")
        void v1HandlesMaxFloat() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(Float.MAX_VALUE)
                    .setHumidity(0.0f)
                    .setSensorId("SENSOR-MAX")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Float.MAX_VALUE should be preserved when widened to double
            assertThat(wrapper.getTemperature()).isEqualTo((double) Float.MAX_VALUE);
        }

        @Test
        @DisplayName("V1 handles minimum float value correctly")
        void v1HandlesMinFloat() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(-Float.MAX_VALUE)
                    .setHumidity(0.0f)
                    .setSensorId("SENSOR-MIN")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            assertThat(wrapper.getTemperature()).isEqualTo((double) -Float.MAX_VALUE);
        }

        @Test
        @DisplayName("V1 handles special float values (NaN)")
        void v1HandlesNaN() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(Float.NaN)
                    .setHumidity(0.0f)
                    .setSensorId("SENSOR-NAN")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            assertThat(Double.isNaN(wrapper.getTemperature())).isTrue();
        }

        @Test
        @DisplayName("V1 handles special float values (Infinity)")
        void v1HandlesInfinity() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(Float.POSITIVE_INFINITY)
                    .setHumidity(Float.NEGATIVE_INFINITY)
                    .setSensorId("SENSOR-INF")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            assertThat(Double.isInfinite(wrapper.getTemperature())).isTrue();
            assertThat(wrapper.getTemperature()).isPositive();

            assertThat(Double.isInfinite(wrapper.getHumidity())).isTrue();
            assertThat(wrapper.getHumidity()).isNegative();
        }
    }

    // ==================== V2 Reading Tests ====================

    @Nested
    @DisplayName("V2 Reading (native double)")
    class V2ReadingTests {

        @Test
        @DisplayName("V2 double values work natively")
        void v2DoubleValuesWorkNatively() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(23.456789012345)
                            .setHumidity(65.123456789012)
                            .setSensorId("SENSOR-V2-001")
                            .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(proto);

            // Double precision is preserved
            assertThat(wrapper.getTemperature()).isEqualTo(23.456789012345);
            assertThat(wrapper.getHumidity()).isEqualTo(65.123456789012);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 handles values exceeding float range")
        void v2HandlesValuesExceedingFloatRange() {
            double largeValue = Double.MAX_VALUE / 2;  // Much larger than Float.MAX_VALUE

            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(largeValue)
                            .setHumidity(0.0)
                            .setSensorId("SENSOR-LARGE")
                            .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(proto);

            assertThat(wrapper.getTemperature()).isEqualTo(largeValue);
        }

        @Test
        @DisplayName("V2 can access V2-only fields")
        void v2CanAccessV2OnlyFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(25.0)
                            .setHumidity(60.0)
                            .setSensorId("SENSOR-V2-002")
                            .setCalibrationId("CAL-2024-001")
                            .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(proto);

            assertThat(wrapper.hasCalibrationId()).isTrue();
            assertThat(wrapper.getCalibrationId()).isEqualTo("CAL-2024-001");
        }

        @Test
        @DisplayName("V1 cannot access V2-only fields")
        void v1CannotAccessV2OnlyFields() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(25.0f)
                    .setHumidity(60.0f)
                    .setSensorId("SENSOR-V1-003")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // V2-only fields return false/null for V1
            assertThat(wrapper.hasCalibrationId()).isFalse();
            assertThat(wrapper.getCalibrationId()).isNull();
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder (double -> float narrowing)")
    class BuilderTests {

        @Test
        @DisplayName("V1 builder accepts values within float range")
        void v1BuilderAcceptsValuesWithinFloatRange() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setSensorId("SENSOR-BUILDER-001")
                    .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Modify using builder with double values that fit in float
            FloatDoubleConflicts modified = original.toBuilder()
                    .setTemperature(25.5)
                    .setHumidity(70.0)
                    .build();

            assertThat(modified.getTemperature()).isCloseTo(25.5, within(0.0001));
            assertThat(modified.getHumidity()).isCloseTo(70.0, within(0.0001));
        }

        @Test
        @DisplayName("V1 builder rejects values exceeding float range")
        void v1BuilderRejectsValuesExceedingFloatRange() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setSensorId("SENSOR-RANGE-TEST")
                    .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Value exceeding float range should throw
            double exceedingValue = Double.MAX_VALUE / 2;

            assertThatThrownBy(() -> original.toBuilder()
                    .setTemperature(exceedingValue)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("float range");
        }

        @Test
        @DisplayName("V1 builder accepts NaN without validation")
        void v1BuilderAcceptsNaN() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setSensorId("SENSOR-NAN-BUILDER")
                    .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // NaN should be accepted without validation
            FloatDoubleConflicts modified = original.toBuilder()
                    .setTemperature(Double.NaN)
                    .build();

            assertThat(Double.isNaN(modified.getTemperature())).isTrue();
        }

        @Test
        @DisplayName("V1 builder accepts Infinity without validation")
        void v1BuilderAcceptsInfinity() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setSensorId("SENSOR-INF-BUILDER")
                    .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Infinity should be accepted without validation
            FloatDoubleConflicts modified = original.toBuilder()
                    .setTemperature(Double.POSITIVE_INFINITY)
                    .setHumidity(Double.NEGATIVE_INFINITY)
                    .build();

            assertThat(Double.isInfinite(modified.getTemperature())).isTrue();
            assertThat(Double.isInfinite(modified.getHumidity())).isTrue();
        }

        @Test
        @DisplayName("V2 builder accepts any double value")
        void v2BuilderAcceptsAnyDoubleValue() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(20.0)
                            .setHumidity(50.0)
                            .setSensorId("SENSOR-V2-BUILDER")
                            .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(proto);

            // V2 should accept any double value without validation
            double largeValue = Double.MAX_VALUE / 2;
            FloatDoubleConflicts modified = original.toBuilder()
                    .setTemperature(largeValue)
                    .build();

            assertThat(modified.getTemperature()).isEqualTo(largeValue);
        }

        @Test
        @DisplayName("Builder preserves non-conflicting fields")
        void builderPreservesNonConflictingFields() {
            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(20.0f)
                    .setHumidity(50.0f)
                    .setSensorId("SENSOR-PRESERVE")
                    .setLocation("Test Location")
                    .build();

            FloatDoubleConflicts original = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // Modify only temperature
            FloatDoubleConflicts modified = original.toBuilder()
                    .setTemperature(30.0)
                    .build();

            // Other fields should be preserved
            assertThat(modified.getTemperature()).isCloseTo(30.0, within(0.0001));
            assertThat(modified.getHumidity()).isCloseTo(50.0, within(0.0001));
            assertThat(modified.getSensorId()).isEqualTo("SENSOR-PRESERVE");
            assertThat(modified.getLocation()).isEqualTo("Test Location");
        }
    }

    // ==================== Version Conversion Tests ====================

    @Nested
    @DisplayName("Version Conversion")
    class VersionConversionTests {

        @Test
        @DisplayName("V1 to V2 conversion preserves float precision")
        void v1ToV2PreservesFloatPrecision() {
            // Create V1 wrapper
            Conflicts.FloatDoubleConflicts v1Proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(23.5f)
                    .setHumidity(65.0f)
                    .setSensorId("SENSOR-CONVERT")
                    .build();

            FloatDoubleConflicts v1Wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(v1Proto);

            // Read as unified interface
            double temperature = v1Wrapper.getTemperature();
            double humidity = v1Wrapper.getHumidity();

            // Values should match original float values when converted to double
            assertThat(temperature).isCloseTo(23.5, within(0.0001));
            assertThat(humidity).isCloseTo(65.0, within(0.0001));
        }

        @Test
        @DisplayName("Both versions use same unified interface")
        void bothVersionsUseSameUnifiedInterface() {
            // Create V1 wrapper
            Conflicts.FloatDoubleConflicts v1Proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(25.0f)
                    .setHumidity(60.0f)
                    .setSensorId("SENSOR-V1")
                    .build();

            // Create V2 wrapper
            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(25.0)
                            .setHumidity(60.0)
                            .setSensorId("SENSOR-V2")
                            .build();

            FloatDoubleConflicts v1 = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(v1Proto);
            FloatDoubleConflicts v2 = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(v2Proto);

            // Both should have same unified getter return type (double)
            assertThat(v1.getTemperature()).isCloseTo(v2.getTemperature(), within(0.0001));
            assertThat(v1.getHumidity()).isCloseTo(v2.getHumidity(), within(0.0001));
        }
    }

    // ==================== Precision Tests ====================

    @Nested
    @DisplayName("Precision Handling")
    class PrecisionTests {

        @Test
        @DisplayName("V2 preserves full double precision")
        void v2PreservesFullDoublePrecision() {
            // Double value with more precision than float can represent
            double preciseValue = 3.141592653589793238;

            space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.FloatDoubleConflicts.newBuilder()
                            .setTemperature(preciseValue)
                            .setHumidity(0.0)
                            .setSensorId("SENSOR-PRECISION")
                            .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v2.FloatDoubleConflicts(proto);

            // V2 should preserve full precision
            assertThat(wrapper.getTemperature()).isEqualTo(preciseValue);
        }

        @Test
        @DisplayName("V1 has float precision limit")
        void v1HasFloatPrecisionLimit() {
            // Value that has more precision than float can represent
            float floatValue = 3.14159265f;  // Float precision is ~7 decimal digits

            Conflicts.FloatDoubleConflicts proto = Conflicts.FloatDoubleConflicts.newBuilder()
                    .setTemperature(floatValue)
                    .setHumidity(0.0f)
                    .setSensorId("SENSOR-FLOAT-PRECISION")
                    .build();

            FloatDoubleConflicts wrapper = new space.alnovis.protowrapper.it.model.v1.FloatDoubleConflicts(proto);

            // V1 value is limited to float precision even when returned as double
            assertThat(wrapper.getTemperature()).isEqualTo((double) floatValue);
        }
    }
}
