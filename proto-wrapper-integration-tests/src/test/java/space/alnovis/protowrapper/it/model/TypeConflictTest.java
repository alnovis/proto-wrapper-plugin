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
        }

        @Test
        @DisplayName("V2 checksum (bytes) returns null through unified interface")
        void v2ReturnsNullForChecksum() {
            byte[] checksumBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

            space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport proto =
                    space.alnovis.protowrapper.it.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumBytes))
                    .setGeneratedAt(createV2Date(2024, 7, 20))
                    .build();

            TelemetryReport report = new space.alnovis.protowrapper.it.model.v2.TelemetryReport(proto);

            // Conflicting field returns false/null
            assertThat(report.hasChecksum()).isFalse();
            assertThat(report.getChecksum()).isNull();
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
