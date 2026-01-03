package space.alnovis.protowrapper.it.model;

import space.alnovis.protowrapper.it.model.api.*;
import space.alnovis.protowrapper.it.proto.v1.Telemetry;
import space.alnovis.protowrapper.it.proto.v2.Telemetry.UnitTypeEnum;
import space.alnovis.protowrapper.it.proto.v2.Telemetry.AlertSeverity;
import space.alnovis.protowrapper.it.proto.v2.Telemetry.SyncStatus;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for type conflict handling between protocol versions.
 *
 * <p>Covers all conflict types:</p>
 * <ul>
 *   <li>INT_ENUM: int32 ↔ enum</li>
 *   <li>WIDENING: int32 → int64, int32 → double</li>
 *   <li>PRIMITIVE_MESSAGE: int32 → message</li>
 *   <li>STRING_BYTES: string ↔ bytes</li>
 * </ul>
 */
@DisplayName("Type Conflict Handling Tests")
class TypeConflictTest {

    // ==================== INT_ENUM Conflicts ====================

    @Nested
    @DisplayName("INT_ENUM Conflict: unitType (int32 ↔ UnitTypeEnum)")
    class IntEnumUnitTypeConflictTest {

        @Test
        @DisplayName("V1 reads unitType as int correctly")
        void v1ReadsUnitTypeAsInt() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Temperature Sensor")
                    .setUnitType(2)  // KELVIN in v2
                    .setPrecisionLevel(3)
                    .setRawValue(2500)
                    .setReadingDate(createV1Date(2024, 1, 15))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            assertThat(reading.getUnitType()).isEqualTo(2);
            assertThat(reading.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V2 reads unitType as enum, unified interface returns int value")
        void v2ReturnsDefaultForConflictingField() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Pressure Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PASCAL)  // enum value 3
                    .setPrecisionLevel(2.5)
                    .setRawValue(101325L)
                    .setReadingDate(createV2Date(2024, 1, 15))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // Phase 2: Conflicting field now returns actual int value from enum
            assertThat(reading.getUnitType()).isEqualTo(3);  // UNIT_PASCAL = 3
            assertThat(reading.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 can access actual enum value via typed proto")
        void v2CanAccessActualEnumViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Pressure Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_BAR)
                    .setPrecisionLevel(2.5)
                    .setRawValue(1013L)
                    .setReadingDate(createV2Date(2024, 1, 15))
                    .build();

            space.alnovis.protowrapper.it.model.v2.SensorReading reading =
                    new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // Access actual enum via typed proto
            assertThat(reading.getTypedProto().getUnitType()).isEqualTo(UnitTypeEnum.UNIT_BAR);
            assertThat(reading.getTypedProto().getUnitType().getNumber()).isEqualTo(4);
        }

        @Test
        @DisplayName("Phase 2: getUnitTypeEnum returns unified enum for V1")
        void v1ReturnsUnifiedEnum() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Temperature Sensor")
                    .setUnitType(3)  // UNIT_PASCAL
                    .setPrecisionLevel(2)
                    .setRawValue(1000)
                    .setReadingDate(createV1Date(2024, 1, 15))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            // Unified enum getter
            assertThat(reading.getUnitTypeEnum()).isEqualTo(UnitType.UNIT_PASCAL);
            assertThat(reading.getUnitTypeEnum().getValue()).isEqualTo(3);
        }

        @Test
        @DisplayName("Phase 2: getUnitTypeEnum returns unified enum for V2")
        void v2ReturnsUnifiedEnum() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Pressure Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_BAR)
                    .setPrecisionLevel(2.5)
                    .setRawValue(1013L)
                    .setReadingDate(createV2Date(2024, 1, 15))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // Unified enum getter
            assertThat(reading.getUnitTypeEnum()).isEqualTo(UnitType.UNIT_BAR);
            assertThat(reading.getUnitTypeEnum().getValue()).isEqualTo(4);
        }

        @Test
        @DisplayName("Phase 2: Builder accepts unified enum type")
        void builderAcceptsUnifiedEnum() {
            // V1 builder with unified enum
            SensorReading v1Reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(
                    Telemetry.SensorReading.newBuilder()
                            .setSensorId("SENSOR-001")
                            .setDeviceName("Test")
                            .setUnitType(0)
                            .setPrecisionLevel(0)
                            .setRawValue(0)
                            .setReadingDate(createV1Date(2024, 1, 1))
                            .build());

            SensorReading modified = v1Reading.toBuilder()
                    .setUnitType(UnitType.UNIT_KELVIN)  // Use unified enum
                    .build();

            assertThat(modified.getUnitType()).isEqualTo(2);  // UNIT_KELVIN = 2
            assertThat(modified.getUnitTypeEnum()).isEqualTo(UnitType.UNIT_KELVIN);
        }

        @Test
        @DisplayName("Phase 2: Both int and enum setters produce same result")
        void bothSettersProduceSameResult() {
            Telemetry.SensorReading baseProto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Test")
                    .setUnitType(0)
                    .setPrecisionLevel(0)
                    .setRawValue(0)
                    .setReadingDate(createV1Date(2024, 1, 1))
                    .build();

            SensorReading base = new space.alnovis.protowrapper.it.model.v1.SensorReading(baseProto);

            // Set via int
            SensorReading viaInt = base.toBuilder()
                    .setUnitType(5)  // UNIT_PERCENT = 5
                    .build();

            // Set via enum
            SensorReading viaEnum = base.toBuilder()
                    .setUnitType(UnitType.UNIT_PERCENT)
                    .build();

            // Both produce same result
            assertThat(viaInt.getUnitType()).isEqualTo(viaEnum.getUnitType());
            assertThat(viaInt.getUnitTypeEnum()).isEqualTo(viaEnum.getUnitTypeEnum());
            assertThat(viaInt.getUnitType()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("INT_ENUM Conflict: severity (int32 ↔ AlertSeverity)")
    class IntEnumSeverityConflictTest {

        @Test
        @DisplayName("V1 reads severityCode as int correctly")
        void v1ReadsSeverityAsInt() {
            Telemetry.AlertNotification proto = Telemetry.AlertNotification.newBuilder()
                    .setAlertId("ALERT-001")
                    .setSensorId("SENSOR-001")
                    .setThresholdValue(100)
                    .setSeverityCode(3)  // CRITICAL in v2
                    .build();

            AlertNotification alert = new space.alnovis.protowrapper.it.model.v1.AlertNotification(proto);

            assertThat(alert.getSeverityCode()).isEqualTo(3);
        }

        @Test
        @DisplayName("V2 severity returns int value from enum")
        void v2ReturnsDefaultForSeverity() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.AlertNotification proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.AlertNotification.newBuilder()
                    .setAlertId("ALERT-002")
                    .setSensorId("SENSOR-002")
                    .setThresholdValue(200)
                    .setSeverity(AlertSeverity.SEVERITY_EMERGENCY)
                    .build();

            AlertNotification alert = new space.alnovis.protowrapper.it.model.v2.AlertNotification(proto);

            // Phase 2: Conflicting field now returns actual int value from enum
            assertThat(alert.getSeverityCode()).isEqualTo(4);  // SEVERITY_EMERGENCY = 4
        }
    }

    @Nested
    @DisplayName("INT_ENUM Conflict: syncStatus (int32 ↔ SyncStatus)")
    class IntEnumSyncStatusConflictTest {

        @Test
        @DisplayName("V1 reads syncStatus as int correctly")
        void v1ReadsSyncStatusAsInt() {
            Telemetry.BatchTelemetry proto = Telemetry.BatchTelemetry.newBuilder()
                    .setBatchId("BATCH-001")
                    .setTotalCount(10)
                    .setSyncStatus(2)  // COMPLETED in v2
                    .setCollectionDate(createV1Date(2024, 1, 15))
                    .build();

            BatchTelemetry batch = new space.alnovis.protowrapper.it.model.v1.BatchTelemetry(proto);

            assertThat(batch.getSyncStatus()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 syncStatus returns int value from enum")
        void v2ReturnsDefaultForSyncStatus() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.BatchTelemetry proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.BatchTelemetry.newBuilder()
                    .setBatchId("BATCH-002")
                    .setTotalCount(20)
                    .setSyncStatus(SyncStatus.SYNC_COMPLETED)
                    .setCollectionDate(createV2Date(2024, 1, 15))
                    .build();

            BatchTelemetry batch = new space.alnovis.protowrapper.it.model.v2.BatchTelemetry(proto);

            // Phase 2: Conflicting field now returns actual int value from enum
            assertThat(batch.getSyncStatus()).isEqualTo(2);  // SYNC_COMPLETED = 2
        }
    }

    // ==================== WIDENING Conflicts ====================

    @Nested
    @DisplayName("WIDENING Conflict: precisionLevel (int32 → double)")
    class WideningPrecisionConflictTest {

        @Test
        @DisplayName("V1 reads precisionLevel as int correctly")
        void v1ReadsPrecisionAsInt() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Humidity Sensor")
                    .setUnitType(5)
                    .setPrecisionLevel(4)
                    .setRawValue(6500)
                    .setReadingDate(createV1Date(2024, 2, 20))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            assertThat(reading.getPrecisionLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("V2 precisionLevel (double) is accessible through unified interface")
        void v2ReturnsPrecisionThroughUnifiedInterface() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Precision Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PERCENT)
                    .setPrecisionLevel(3.14159)  // double value
                    .setRawValue(9999L)
                    .setReadingDate(createV2Date(2024, 2, 20))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // WIDENING: unified interface uses double (wider type)
            assertThat(reading.getPrecisionLevel()).isEqualTo(3.14159);
        }

        @Test
        @DisplayName("V2 can access actual double precision via typed proto")
        void v2CanAccessActualDoubleViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Precision Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PERCENT)
                    .setPrecisionLevel(3.14159)
                    .setRawValue(9999L)
                    .setReadingDate(createV2Date(2024, 2, 20))
                    .build();

            space.alnovis.protowrapper.it.model.v2.SensorReading reading =
                    new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            assertThat(reading.getTypedProto().getPrecisionLevel()).isEqualTo(3.14159);
        }
    }

    @Nested
    @DisplayName("WIDENING Conflict: rawValue (int32 → int64)")
    class WideningRawValueConflictTest {

        @Test
        @DisplayName("V1 reads rawValue as int correctly")
        void v1ReadsRawValueAsInt() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Counter Sensor")
                    .setUnitType(0)
                    .setPrecisionLevel(0)
                    .setRawValue(2147483647)  // max int
                    .setReadingDate(createV1Date(2024, 3, 10))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            assertThat(reading.getRawValue()).isEqualTo(2147483647);
        }

        @Test
        @DisplayName("V2 rawValue (int64) is accessible through unified interface")
        void v2ReturnsRawValueThroughUnifiedInterface() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Big Counter")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(9_999_999_999L)  // exceeds int range
                    .setReadingDate(createV2Date(2024, 3, 10))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // WIDENING: unified interface uses long (wider type)
            assertThat(reading.getRawValue()).isEqualTo(9_999_999_999L);
        }

        @Test
        @DisplayName("V2 can access actual long value via typed proto")
        void v2CanAccessActualLongViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Big Counter")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(9_999_999_999L)
                    .setReadingDate(createV2Date(2024, 3, 10))
                    .build();

            space.alnovis.protowrapper.it.model.v2.SensorReading reading =
                    new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            assertThat(reading.getTypedProto().getRawValue()).isEqualTo(9_999_999_999L);
        }
    }

    // ==================== PRIMITIVE_MESSAGE Conflicts ====================

    @Nested
    @DisplayName("PRIMITIVE_MESSAGE Conflict: calibrationId (int32 → CalibrationInfo)")
    class PrimitiveMessageCalibrationConflictTest {

        @Test
        @DisplayName("V1 reads calibrationId as int correctly")
        void v1ReadsCalibrationIdAsInt() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Calibrated Sensor")
                    .setUnitType(1)
                    .setPrecisionLevel(2)
                    .setCalibrationId(12345)
                    .setRawValue(500)
                    .setReadingDate(createV1Date(2024, 4, 5))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            assertThat(reading.hasCalibrationId()).isTrue();
            assertThat(reading.getCalibrationId()).isEqualTo(12345);
        }

        @Test
        @DisplayName("V2 calibrationInfo (message) returns null through unified interface")
        void v2ReturnsNullForCalibrationId() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo calibInfo =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                    .setCalibrationId("CAL-2024-001")
                    .setTechnicianId("TECH-42")
                    .setCalibrationDate(createV2Date(2024, 4, 1))
                    .setAccuracyRating(99)
                    .build();

            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("V2 Calibrated Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(2.0)
                    .setCalibrationInfo(calibInfo)
                    .setRawValue(500L)
                    .setReadingDate(createV2Date(2024, 4, 5))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // Conflicting field returns false/null
            assertThat(reading.hasCalibrationId()).isFalse();
            assertThat(reading.getCalibrationId()).isNull();
        }

        @Test
        @DisplayName("V2 can access CalibrationInfo message via typed proto")
        void v2CanAccessCalibrationInfoViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo calibInfo =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                    .setCalibrationId("CAL-2024-001")
                    .setTechnicianId("TECH-42")
                    .setAccuracyRating(99)
                    .build();

            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("V2 Calibrated Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(2.0)
                    .setCalibrationInfo(calibInfo)
                    .setRawValue(500L)
                    .setReadingDate(createV2Date(2024, 4, 5))
                    .build();

            space.alnovis.protowrapper.it.model.v2.SensorReading reading =
                    new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            assertThat(reading.getTypedProto().hasCalibrationInfo()).isTrue();
            assertThat(reading.getTypedProto().getCalibrationInfo().getCalibrationId())
                    .isEqualTo("CAL-2024-001");
            assertThat(reading.getTypedProto().getCalibrationInfo().getTechnicianId())
                    .isEqualTo("TECH-42");
        }

        @Test
        @DisplayName("V1 supportsCalibrationId returns true, supportsCalibrationIdMessage returns false")
        void v1SupportsPrimitiveOnly() {
            Telemetry.SensorReading proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-001")
                    .setDeviceName("Sensor")
                    .setUnitType(1)
                    .setPrecisionLevel(2)
                    .setCalibrationId(12345)
                    .setRawValue(500)
                    .setReadingDate(createV1Date(2024, 4, 5))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v1.SensorReading(proto);

            // V1 supports primitive, not message
            assertThat(reading.supportsCalibrationId()).isTrue();
            assertThat(reading.supportsCalibrationIdMessage()).isFalse();

            // Message getter returns null for v1
            assertThat(reading.getCalibrationIdMessage()).isNull();
        }

        @Test
        @DisplayName("V2 supportsCalibrationIdMessage returns true, can access CalibrationInfo via message getter")
        void v2SupportsMessageOnly() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo calibInfo =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                    .setCalibrationId("CAL-2024-001")
                    .setTechnicianId("TECH-42")
                    .setAccuracyRating(99)
                    .build();

            space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("V2 Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(2.0)
                    .setCalibrationInfo(calibInfo)
                    .setRawValue(500L)
                    .setReadingDate(createV2Date(2024, 4, 5))
                    .build();

            SensorReading reading = new space.alnovis.protowrapper.it.model.v2.SensorReading(proto);

            // V2 supports message, not primitive
            assertThat(reading.supportsCalibrationId()).isFalse();
            assertThat(reading.supportsCalibrationIdMessage()).isTrue();

            // Can access CalibrationInfo via unified message getter
            CalibrationInfo calibWrapper = reading.getCalibrationIdMessage();
            assertThat(calibWrapper).isNotNull();
            assertThat(calibWrapper.getCalibrationId()).isEqualTo("CAL-2024-001");
            assertThat(calibWrapper.getTechnicianId()).isEqualTo("TECH-42");
            assertThat(calibWrapper.getAccuracyRating()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("PRIMITIVE_MESSAGE Conflict: generatedAt (int64 → Date)")
    class PrimitiveMessageTimestampConflictTest {

        @Test
        @DisplayName("V1 reads generatedAt as long correctly")
        void v1ReadsGeneratedAtAsLong() {
            Telemetry.TelemetryReport proto = Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-001")
                    .setReading(createMinimalV1SensorReading())
                    .setGeneratedAt(1704067200000L)  // 2024-01-01 00:00:00 UTC
                    .build();

            TelemetryReport report = new space.alnovis.protowrapper.it.model.v1.TelemetryReport(proto);

            assertThat(report.getGeneratedAt()).isEqualTo(1704067200000L);
        }

        @Test
        @DisplayName("V2 generatedAt (Date) returns default through unified interface")
        void v2ReturnsDefaultForGeneratedAt() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setGeneratedAt(createV2Date(2024, 6, 15))
                    .build();

            TelemetryReport report = new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto);

            // Conflicting field returns default (0L)
            assertThat(report.getGeneratedAt()).isEqualTo(0L);
        }

        @Test
        @DisplayName("V2 can access Date message via typed proto")
        void v2CanAccessDateViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setGeneratedAt(createV2Date(2024, 6, 15))
                    .build();

            space.alnovis.protowrapper.it.model.v2.TelemetryReport report =
                    new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto);

            assertThat(report.getTypedProto().getGeneratedAt().getYear()).isEqualTo(2024);
            assertThat(report.getTypedProto().getGeneratedAt().getMonth()).isEqualTo(6);
            assertThat(report.getTypedProto().getGeneratedAt().getDay()).isEqualTo(15);
        }
    }

    // ==================== STRING_BYTES Conflict ====================

    @Nested
    @DisplayName("STRING_BYTES Conflict: checksum (string ↔ bytes)")
    class StringBytesChecksumConflictTest {

        @Test
        @DisplayName("V1 reads checksum as string correctly")
        void v1ReadsChecksumAsString() {
            Telemetry.TelemetryReport proto = Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-001")
                    .setReading(createMinimalV1SensorReading())
                    .setChecksum("abc123def456")
                    .setGeneratedAt(1704067200000L)
                    .build();

            TelemetryReport report = new space.alnovis.protowrapper.it.model.v1.TelemetryReport(proto);

            assertThat(report.hasChecksum()).isTrue();
            assertThat(report.getChecksum()).isEqualTo("abc123def456");
            // Bytes getter converts String to UTF-8 bytes
            assertThat(report.getChecksumBytes())
                    .isEqualTo("abc123def456".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("V2 checksum (bytes) can be accessed as String via UTF-8 conversion")
        void v2ChecksumAccessibleAsStringViaUtf8() {
            // Use a valid UTF-8 string encoded as bytes
            String originalChecksum = "checksum123";
            byte[] checksumBytes = originalChecksum.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumBytes))
                    .setGeneratedAt(createV2Date(2024, 7, 20))
                    .build();

            TelemetryReport report = new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto);

            // With STRING_BYTES unified access, bytes are converted to String via UTF-8
            assertThat(report.hasChecksum()).isTrue();
            assertThat(report.getChecksum()).isEqualTo(originalChecksum);
            // Also verify bytes getter
            assertThat(report.getChecksumBytes()).isEqualTo(checksumBytes);
        }

        @Test
        @DisplayName("V2 can access ByteString checksum via typed proto")
        void v2CanAccessBytesViaTypedProto() {
            byte[] checksumBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumBytes))
                    .setGeneratedAt(createV2Date(2024, 7, 20))
                    .build();

            space.alnovis.protowrapper.it.model.v2.TelemetryReport report =
                    new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto);

            assertThat(report.getTypedProto().hasChecksum()).isTrue();
            assertThat(report.getTypedProto().getChecksum().toByteArray())
                    .isEqualTo(checksumBytes);
        }

        @Test
        @DisplayName("V1 builder can set checksum via String and Bytes")
        void v1BuilderStringBytesSetters() {
            // Set via String - note: generatedAt is a required field in proto but has PRIMITIVE_MESSAGE conflict
            // So we build using the proto builder first to satisfy required fields
            Telemetry.TelemetryReport proto1 = Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-001")
                    .setReading(createMinimalV1SensorReading())
                    .setChecksum("initial")
                    .setGeneratedAt(1704067200000L)
                    .build();

            TelemetryReport report1 = new space.alnovis.protowrapper.it.model.v1.TelemetryReport(proto1)
                    .toBuilder()
                    .setChecksum("test-checksum")
                    .build();

            assertThat(report1.getChecksum()).isEqualTo("test-checksum");
            assertThat(report1.getChecksumBytes())
                    .isEqualTo("test-checksum".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Set via Bytes (will be converted to String for V1)
            byte[] checksumBytes = "bytes-checksum".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            TelemetryReport report2 = new space.alnovis.protowrapper.it.model.v1.TelemetryReport(proto1)
                    .toBuilder()
                    .setChecksumBytes(checksumBytes)
                    .build();

            assertThat(report2.getChecksum()).isEqualTo("bytes-checksum");
            assertThat(report2.getChecksumBytes()).isEqualTo(checksumBytes);
        }

        @Test
        @DisplayName("V2 builder can set checksum via String and Bytes")
        void v2BuilderStringBytesSetters() {
            // Build using proto builder first to satisfy required fields
            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto1 =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                            .setReportNumber("RPT-001")
                            .setReading(createMinimalV2SensorReading())
                            .setChecksum(ByteString.copyFrom("initial".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                            .setGeneratedAt(createV2Date(2024, 1, 1))
                            .build();

            // Set via String (will be converted to bytes for V2)
            TelemetryReport report1 = new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto1)
                    .toBuilder()
                    .setChecksum("test-checksum")
                    .build();

            assertThat(report1.getChecksum()).isEqualTo("test-checksum");
            assertThat(report1.getChecksumBytes())
                    .isEqualTo("test-checksum".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Set via Bytes
            byte[] checksumBytes = "bytes-checksum".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            TelemetryReport report2 = new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto1)
                    .toBuilder()
                    .setChecksumBytes(checksumBytes)
                    .build();

            assertThat(report2.getChecksum()).isEqualTo("bytes-checksum");
            assertThat(report2.getChecksumBytes()).isEqualTo(checksumBytes);
        }
    }

    // ==================== NESTED PRIMITIVE_MESSAGE Conflicts ====================

    @Nested
    @DisplayName("NESTED PRIMITIVE_MESSAGE Conflict: parent_id (int32 → NestedPrimitiveMessageConflicts.ParentRef)")
    class NestedPrimitiveMessageConflictTest {

        @Test
        @DisplayName("V1 reads parent_id as int correctly")
        void v1ReadsParentIdAsInt() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("TEST-001")
                            .setName("Test Item")
                            .setParentId(12345)
                            .setDescription("Test description")
                            .build();

            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.NestedPrimitiveMessageConflicts(proto);

            assertThat(wrapper.hasParentId()).isTrue();
            assertThat(wrapper.getParentId()).isEqualTo(12345);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V1 supportsParentId returns true, supportsParentIdMessage returns false")
        void v1SupportsPrimitiveOnly() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("TEST-001")
                            .setName("Test Item")
                            .setParentId(12345)
                            .build();

            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.NestedPrimitiveMessageConflicts(proto);

            // V1 supports primitive, not message
            assertThat(wrapper.supportsParentId()).isTrue();
            assertThat(wrapper.supportsParentIdMessage()).isFalse();

            // Message getter returns null for V1
            assertThat(wrapper.getParentIdMessage()).isNull();
        }

        @Test
        @DisplayName("V2 reads parent_id as nested message (ParentRef)")
        void v2ReadsParentIdAsNestedMessage() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef parentRef =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef.newBuilder()
                            .setRefNumber("REF-2024-001")
                            .setRefType("PARENT")
                            .build();

            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("TEST-002")
                            .setName("Test Item V2")
                            .setParentId(parentRef)
                            .setDescription("Test description V2")
                            .build();

            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.NestedPrimitiveMessageConflicts(proto);

            // Primitive getter returns null/default for V2 (message type)
            assertThat(wrapper.hasParentId()).isFalse();
            assertThat(wrapper.getParentId()).isNull();
            assertThat(wrapper.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 supportsParentIdMessage returns true, can access nested ParentRef via message getter")
        void v2SupportsNestedMessageOnly() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef parentRef =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef.newBuilder()
                            .setRefNumber("REF-2024-002")
                            .setRefType("CHILD")
                            .build();

            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("TEST-003")
                            .setName("Test V2")
                            .setParentId(parentRef)
                            .build();

            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.NestedPrimitiveMessageConflicts(proto);

            // V2 supports message, not primitive
            assertThat(wrapper.supportsParentId()).isFalse();
            assertThat(wrapper.supportsParentIdMessage()).isTrue();

            // Access nested message via unified message getter
            // The type should be NestedPrimitiveMessageConflicts.ParentRef (nested interface)
            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts.ParentRef parentWrapper =
                    wrapper.getParentIdMessage();
            assertThat(parentWrapper).isNotNull();
            assertThat(parentWrapper.getRefNumber()).isEqualTo("REF-2024-002");
            assertThat(parentWrapper.getRefType()).isEqualTo("CHILD");
        }

        @Test
        @DisplayName("V2 can access nested ParentRef via typed proto")
        void v2CanAccessNestedMessageViaTypedProto() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef parentRef =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.ParentRef.newBuilder()
                            .setRefNumber("REF-TYPED")
                            .setRefType("TYPED_ACCESS")
                            .build();

            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("TEST-004")
                            .setName("Typed Test")
                            .setParentId(parentRef)
                            .build();

            space.alnovis.protowrapper.it.model.v2.NestedPrimitiveMessageConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.NestedPrimitiveMessageConflicts(proto);

            // Direct access via typed proto
            assertThat(wrapper.getTypedProto().hasParentId()).isTrue();
            assertThat(wrapper.getTypedProto().getParentId().getRefNumber()).isEqualTo("REF-TYPED");
            assertThat(wrapper.getTypedProto().getParentId().getRefType()).isEqualTo("TYPED_ACCESS");
        }

        @Test
        @DisplayName("Non-conflicting fields work across versions")
        void nonConflictingFieldsWork() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts v1Proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("ID-V1")
                            .setName("Name V1")
                            .setDescription("Desc V1")
                            .build();

            space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.NestedPrimitiveMessageConflicts.newBuilder()
                            .setId("ID-V2")
                            .setName("Name V2")
                            .setDescription("Desc V2")
                            .setCreatedAt(createV2Date(2024, 12, 21))
                            .build();

            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts v1 =
                    new space.alnovis.protowrapper.it.model.v1.NestedPrimitiveMessageConflicts(v1Proto);
            space.alnovis.protowrapper.it.model.api.NestedPrimitiveMessageConflicts v2 =
                    new space.alnovis.protowrapper.it.model.v2.NestedPrimitiveMessageConflicts(v2Proto);

            // Common fields work in both versions
            assertThat(v1.getId()).isEqualTo("ID-V1");
            assertThat(v1.getName()).isEqualTo("Name V1");
            assertThat(v1.getDescription()).isEqualTo("Desc V1");

            assertThat(v2.getId()).isEqualTo("ID-V2");
            assertThat(v2.getName()).isEqualTo("Name V2");
            assertThat(v2.getDescription()).isEqualTo("Desc V2");

            // createdAt only in v2
            assertThat(v1.hasCreatedAt()).isFalse();
            assertThat(v2.hasCreatedAt()).isTrue();
        }
    }

    // ==================== REPEATED Conflicts ====================

    @Nested
    @DisplayName("REPEATED field conflicts (WIDENING, INT_ENUM, STRING_BYTES)")
    class RepeatedConflictsTest {

        @Test
        @DisplayName("V1 repeated int32 widened to List<Long>")
        void v1RepeatedIntWidenedToLong() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(100)
                            .addNumbers(200)
                            .addNumbers(300)
                            .setBatchId("BATCH-V1-001")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto);

            // Widening: int32 → long
            assertThat(wrapper.getNumbers()).containsExactly(100L, 200L, 300L);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V2 repeated int64 returns List<Long> directly")
        void v2RepeatedLongReturnsList() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(1_000_000_000_000L)
                            .addNumbers(2_000_000_000_000L)
                            .setBatchId("BATCH-V2-001")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto);

            assertThat(wrapper.getNumbers()).containsExactly(1_000_000_000_000L, 2_000_000_000_000L);
            assertThat(wrapper.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V1 repeated float widened to List<Double>")
        void v1RepeatedFloatWidenedToDouble() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addValues(1.5f)
                            .addValues(2.5f)
                            .addValues(3.5f)
                            .setBatchId("BATCH-V1-002")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto);

            // Widening: float → double
            assertThat(wrapper.getValues()).containsExactly(1.5d, 2.5d, 3.5d);
        }

        @Test
        @DisplayName("V2 repeated double returns List<Double> directly")
        void v2RepeatedDoubleReturnsList() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addValues(3.141592653589793)
                            .addValues(2.718281828459045)
                            .setBatchId("BATCH-V2-002")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto);

            assertThat(wrapper.getValues()).containsExactly(3.141592653589793, 2.718281828459045);
        }

        @Test
        @DisplayName("V1 repeated int32 returns List<Integer> for INT_ENUM")
        void v1RepeatedIntForIntEnum() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addCodes(1)
                            .addCodes(2)
                            .addCodes(3)
                            .setBatchId("BATCH-V1-003")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto);

            // INT_ENUM: returns int values
            assertThat(wrapper.getCodes()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("V2 repeated enum converted to List<Integer>")
        void v2RepeatedEnumConvertedToInt() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addCodes(space.alnovis.protowrapper.it.proto.v2.Conflicts.CodeEnum.CODE_SUCCESS)
                            .addCodes(space.alnovis.protowrapper.it.proto.v2.Conflicts.CodeEnum.CODE_WARNING)
                            .addCodes(space.alnovis.protowrapper.it.proto.v2.Conflicts.CodeEnum.CODE_ERROR)
                            .setBatchId("BATCH-V2-003")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto);

            // INT_ENUM: enum converted to int values (SUCCESS=1, WARNING=2, ERROR=3)
            assertThat(wrapper.getCodes()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("V1 repeated string returns List<String> for STRING_BYTES")
        void v1RepeatedStringForStringBytes() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addTexts("hello")
                            .addTexts("world")
                            .addTexts("test")
                            .setBatchId("BATCH-V1-004")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto);

            assertThat(wrapper.getTexts()).containsExactly("hello", "world", "test");
        }

        @Test
        @DisplayName("V2 repeated bytes converted to List<String> via UTF-8")
        void v2RepeatedBytesConvertedToString() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addTexts(ByteString.copyFromUtf8("привет"))
                            .addTexts(ByteString.copyFromUtf8("мир"))
                            .addTexts(ByteString.copyFromUtf8("тест"))
                            .setBatchId("BATCH-V2-004")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto);

            // STRING_BYTES: bytes converted to String via UTF-8
            assertThat(wrapper.getTexts()).containsExactly("привет", "мир", "тест");
        }

        @Test
        @DisplayName("Non-conflicting fields work across versions")
        void nonConflictingFieldsWork() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts v1Proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-001")
                            .build();

            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts v2Proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-002")
                            .setItemCount(42)
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts v1 =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(v1Proto);
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts v2 =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(v2Proto);

            // BatchId works in both
            assertThat(v1.getBatchId()).isEqualTo("BATCH-001");
            assertThat(v2.getBatchId()).isEqualTo("BATCH-002");

            // ItemCount only in v2
            assertThat(v1.hasItemCount()).isFalse();
            assertThat(v1.getItemCount()).isNull();
            assertThat(v2.hasItemCount()).isTrue();
            assertThat(v2.getItemCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("Builder supports repeated conflict fields (v1.4.0)")
        void builderSupportsRepeatedConflictFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(100L)
                            .addCodes(space.alnovis.protowrapper.it.proto.v2.Conflicts.CodeEnum.CODE_SUCCESS)
                            .addTexts(ByteString.copyFromUtf8("text"))
                            .addValues(1.5)
                            .setBatchId("INITIAL")
                            .setItemCount(10)
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts wrapper =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto);

            // v1.4.0: Builder now supports repeated conflict fields
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts modified = wrapper.toBuilder()
                    .setBatchId("MODIFIED")
                    .setItemCount(20)
                    .addNumbers(200L)      // Add to numbers
                    .addCodes(2)           // Add to codes
                    .addTexts("more")      // Add to texts
                    .addValues(2.5)        // Add to values
                    .build();

            assertThat(modified.getBatchId()).isEqualTo("MODIFIED");
            assertThat(modified.getItemCount()).isEqualTo(20);

            // Repeated conflict fields now support modification
            assertThat(modified.getNumbers()).containsExactly(100L, 200L);
            assertThat(modified.getCodes()).containsExactly(1, 2);
            assertThat(modified.getTexts()).containsExactly("text", "more");
            assertThat(modified.getValues()).containsExactly(1.5, 2.5);
        }
    }

    // ==================== Helper Methods ====================

    private space.alnovis.protowrapper.it.proto.v1.Common.Date createV1Date(int year, int month, int day) {
        return space.alnovis.protowrapper.it.proto.v1.Common.Date.newBuilder()
                .setYear(year)
                .setMonth(month)
                .setDay(day)
                .build();
    }

    private space.alnovis.protowrapper.it.proto.v2.Common.Date createV2Date(int year, int month, int day) {
        return space.alnovis.protowrapper.it.proto.v2.Common.Date.newBuilder()
                .setYear(year)
                .setMonth(month)
                .setDay(day)
                .build();
    }

    private Telemetry.SensorReading createMinimalV1SensorReading() {
        return Telemetry.SensorReading.newBuilder()
                .setSensorId("SENSOR-MIN")
                .setDeviceName("Minimal")
                .setUnitType(0)
                .setPrecisionLevel(0)
                .setRawValue(0)
                .setReadingDate(createV1Date(2024, 1, 1))
                .build();
    }

    private space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading createMinimalV2SensorReading() {
        return space.alnovis.protowrapper.it.proto.v2.Telemetry.SensorReading.newBuilder()
                .setSensorId("SENSOR-MIN")
                .setDeviceName("Minimal")
                .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                .setPrecisionLevel(0.0)
                .setRawValue(0L)
                .setReadingDate(createV2Date(2024, 1, 1))
                .build();
    }
}
