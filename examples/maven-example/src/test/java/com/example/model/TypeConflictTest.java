package com.example.model;

import com.example.model.api.*;
import com.example.proto.v1.Telemetry;
import com.example.proto.v2.Telemetry.UnitTypeEnum;
import com.example.proto.v2.Telemetry.AlertSeverity;
import com.example.proto.v2.Telemetry.SyncStatus;
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

            SensorReading reading = new com.example.model.v1.SensorReading(proto);

            assertThat(reading.getUnitType()).isEqualTo(2);
            assertThat(reading.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V2 reads unitType as enum, unified interface returns 0 (default)")
        void v2ReturnsDefaultForConflictingField() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Pressure Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PASCAL)  // enum value 3
                    .setPrecisionLevel(2.5)
                    .setRawValue(101325L)
                    .setReadingDate(createV2Date(2024, 1, 15))
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(proto);

            // Conflicting field returns default (0) through unified interface
            assertThat(reading.getUnitType()).isEqualTo(0);
            assertThat(reading.getWrapperVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 can access actual enum value via typed proto")
        void v2CanAccessActualEnumViaTypedProto() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Pressure Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_BAR)
                    .setPrecisionLevel(2.5)
                    .setRawValue(1013L)
                    .setReadingDate(createV2Date(2024, 1, 15))
                    .build();

            com.example.model.v2.SensorReading reading =
                    new com.example.model.v2.SensorReading(proto);

            // Access actual enum via typed proto
            assertThat(reading.getTypedProto().getUnitType()).isEqualTo(UnitTypeEnum.UNIT_BAR);
            assertThat(reading.getTypedProto().getUnitType().getNumber()).isEqualTo(4);
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

            AlertNotification alert = new com.example.model.v1.AlertNotification(proto);

            assertThat(alert.getSeverityCode()).isEqualTo(3);
        }

        @Test
        @DisplayName("V2 severity returns default through unified interface")
        void v2ReturnsDefaultForSeverity() {
            com.example.proto.v2.Telemetry.AlertNotification proto =
                    com.example.proto.v2.Telemetry.AlertNotification.newBuilder()
                    .setAlertId("ALERT-002")
                    .setSensorId("SENSOR-002")
                    .setThresholdValue(200)
                    .setSeverity(AlertSeverity.SEVERITY_EMERGENCY)
                    .build();

            AlertNotification alert = new com.example.model.v2.AlertNotification(proto);

            // Conflicting field returns default
            assertThat(alert.getSeverityCode()).isEqualTo(0);
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

            BatchTelemetry batch = new com.example.model.v1.BatchTelemetry(proto);

            assertThat(batch.getSyncStatus()).isEqualTo(2);
        }

        @Test
        @DisplayName("V2 syncStatus returns default through unified interface")
        void v2ReturnsDefaultForSyncStatus() {
            com.example.proto.v2.Telemetry.BatchTelemetry proto =
                    com.example.proto.v2.Telemetry.BatchTelemetry.newBuilder()
                    .setBatchId("BATCH-002")
                    .setTotalCount(20)
                    .setSyncStatus(SyncStatus.SYNC_COMPLETED)
                    .setCollectionDate(createV2Date(2024, 1, 15))
                    .build();

            BatchTelemetry batch = new com.example.model.v2.BatchTelemetry(proto);

            assertThat(batch.getSyncStatus()).isEqualTo(0);
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

            SensorReading reading = new com.example.model.v1.SensorReading(proto);

            assertThat(reading.getPrecisionLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("V2 precisionLevel (double) returns default through unified interface")
        void v2ReturnsDefaultForPrecision() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Precision Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PERCENT)
                    .setPrecisionLevel(3.14159)  // double value
                    .setRawValue(9999L)
                    .setReadingDate(createV2Date(2024, 2, 20))
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(proto);

            // Conflicting field returns default (0)
            assertThat(reading.getPrecisionLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("V2 can access actual double precision via typed proto")
        void v2CanAccessActualDoubleViaTypedProto() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Precision Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_PERCENT)
                    .setPrecisionLevel(3.14159)
                    .setRawValue(9999L)
                    .setReadingDate(createV2Date(2024, 2, 20))
                    .build();

            com.example.model.v2.SensorReading reading =
                    new com.example.model.v2.SensorReading(proto);

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

            SensorReading reading = new com.example.model.v1.SensorReading(proto);

            assertThat(reading.getRawValue()).isEqualTo(2147483647);
        }

        @Test
        @DisplayName("V2 rawValue (int64) returns default through unified interface")
        void v2ReturnsDefaultForRawValue() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Big Counter")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(9_999_999_999L)  // exceeds int range
                    .setReadingDate(createV2Date(2024, 3, 10))
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(proto);

            // Conflicting field returns default (0)
            assertThat(reading.getRawValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("V2 can access actual long value via typed proto")
        void v2CanAccessActualLongViaTypedProto() {
            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("Big Counter")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(9_999_999_999L)
                    .setReadingDate(createV2Date(2024, 3, 10))
                    .build();

            com.example.model.v2.SensorReading reading =
                    new com.example.model.v2.SensorReading(proto);

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

            SensorReading reading = new com.example.model.v1.SensorReading(proto);

            assertThat(reading.hasCalibrationId()).isTrue();
            assertThat(reading.getCalibrationId()).isEqualTo(12345);
        }

        @Test
        @DisplayName("V2 calibrationInfo (message) returns null through unified interface")
        void v2ReturnsNullForCalibrationId() {
            com.example.proto.v2.Telemetry.CalibrationInfo calibInfo =
                    com.example.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                    .setCalibrationId("CAL-2024-001")
                    .setTechnicianId("TECH-42")
                    .setCalibrationDate(createV2Date(2024, 4, 1))
                    .setAccuracyRating(99)
                    .build();

            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("V2 Calibrated Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(2.0)
                    .setCalibrationInfo(calibInfo)
                    .setRawValue(500L)
                    .setReadingDate(createV2Date(2024, 4, 5))
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(proto);

            // Conflicting field returns false/null
            assertThat(reading.hasCalibrationId()).isFalse();
            assertThat(reading.getCalibrationId()).isNull();
        }

        @Test
        @DisplayName("V2 can access CalibrationInfo message via typed proto")
        void v2CanAccessCalibrationInfoViaTypedProto() {
            com.example.proto.v2.Telemetry.CalibrationInfo calibInfo =
                    com.example.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                    .setCalibrationId("CAL-2024-001")
                    .setTechnicianId("TECH-42")
                    .setAccuracyRating(99)
                    .build();

            com.example.proto.v2.Telemetry.SensorReading proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-002")
                    .setDeviceName("V2 Calibrated Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(2.0)
                    .setCalibrationInfo(calibInfo)
                    .setRawValue(500L)
                    .setReadingDate(createV2Date(2024, 4, 5))
                    .build();

            com.example.model.v2.SensorReading reading =
                    new com.example.model.v2.SensorReading(proto);

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

            TelemetryReport report = new com.example.model.v1.TelemetryReport(proto);

            assertThat(report.getGeneratedAt()).isEqualTo(1704067200000L);
        }

        @Test
        @DisplayName("V2 generatedAt (Date) returns default through unified interface")
        void v2ReturnsDefaultForGeneratedAt() {
            com.example.proto.v2.Telemetry.TelemetryReport proto =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setGeneratedAt(createV2Date(2024, 6, 15))
                    .build();

            TelemetryReport report = new com.example.model.v2.TelemetryReport(proto);

            // Conflicting field returns default (0L)
            assertThat(report.getGeneratedAt()).isEqualTo(0L);
        }

        @Test
        @DisplayName("V2 can access Date message via typed proto")
        void v2CanAccessDateViaTypedProto() {
            com.example.proto.v2.Telemetry.TelemetryReport proto =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setGeneratedAt(createV2Date(2024, 6, 15))
                    .build();

            com.example.model.v2.TelemetryReport report =
                    new com.example.model.v2.TelemetryReport(proto);

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

            TelemetryReport report = new com.example.model.v1.TelemetryReport(proto);

            assertThat(report.hasChecksum()).isTrue();
            assertThat(report.getChecksum()).isEqualTo("abc123def456");
        }

        @Test
        @DisplayName("V2 checksum (bytes) returns null through unified interface")
        void v2ReturnsNullForChecksum() {
            byte[] checksumBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

            com.example.proto.v2.Telemetry.TelemetryReport proto =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumBytes))
                    .setGeneratedAt(createV2Date(2024, 7, 20))
                    .build();

            TelemetryReport report = new com.example.model.v2.TelemetryReport(proto);

            // Conflicting field returns false/null
            assertThat(report.hasChecksum()).isFalse();
            assertThat(report.getChecksum()).isNull();
        }

        @Test
        @DisplayName("V2 can access ByteString checksum via typed proto")
        void v2CanAccessBytesViaTypedProto() {
            byte[] checksumBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

            com.example.proto.v2.Telemetry.TelemetryReport proto =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-002")
                    .setReading(createMinimalV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumBytes))
                    .setGeneratedAt(createV2Date(2024, 7, 20))
                    .build();

            com.example.model.v2.TelemetryReport report =
                    new com.example.model.v2.TelemetryReport(proto);

            assertThat(report.getTypedProto().hasChecksum()).isTrue();
            assertThat(report.getTypedProto().getChecksum().toByteArray())
                    .isEqualTo(checksumBytes);
        }
    }

    // ==================== Helper Methods ====================

    private com.example.proto.v1.Common.Date createV1Date(int year, int month, int day) {
        return com.example.proto.v1.Common.Date.newBuilder()
                .setYear(year)
                .setMonth(month)
                .setDay(day)
                .build();
    }

    private com.example.proto.v2.Common.Date createV2Date(int year, int month, int day) {
        return com.example.proto.v2.Common.Date.newBuilder()
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

    private com.example.proto.v2.Telemetry.SensorReading createMinimalV2SensorReading() {
        return com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                .setSensorId("SENSOR-MIN")
                .setDeviceName("Minimal")
                .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                .setPrecisionLevel(0.0)
                .setRawValue(0L)
                .setReadingDate(createV2Date(2024, 1, 1))
                .build();
    }
}
