package com.example.model;

import com.example.model.api.*;
import com.example.proto.v1.Telemetry;
import com.example.proto.v2.Telemetry.UnitTypeEnum;
import com.example.proto.v2.Telemetry.AlertSeverity;
import com.example.proto.v2.Telemetry.SyncStatus;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip tests for type conflict scenarios.
 *
 * <p>Verifies that:</p>
 * <ul>
 *   <li>Serialization and deserialization work correctly for each version</li>
 *   <li>Non-conflicting fields are preserved through round-trip</li>
 *   <li>Builder works for non-conflicting fields</li>
 *   <li>toBytes() and parsing work correctly</li>
 * </ul>
 */
@DisplayName("Type Conflict Round-Trip Tests")
class TypeConflictRoundTripTest {

    // ==================== V1 Round-Trip Tests ====================

    @Nested
    @DisplayName("V1 SensorReading Round-Trip")
    class V1SensorReadingRoundTripTest {

        @Test
        @DisplayName("V1 SensorReading survives serialize/deserialize")
        void v1RoundTripWithAllFields() throws InvalidProtocolBufferException {
            // Create V1 proto with all fields
            Telemetry.SensorReading original = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-V1-001")
                    .setDeviceName("Temperature Probe")
                    .setUnitType(2)
                    .setPrecisionLevel(4)
                    .setCalibrationId(99999)
                    .setRawValue(25000)
                    .setReadingDate(createV1Date(2024, 5, 15))
                    .setLocation("Building A, Room 101")
                    .build();

            // Wrap in API
            SensorReading reading = new com.example.model.v1.SensorReading(original);

            // Serialize to bytes
            byte[] bytes = reading.toBytes();

            // Parse back
            Telemetry.SensorReading parsed = Telemetry.SensorReading.parseFrom(bytes);
            SensorReading restored = new com.example.model.v1.SensorReading(parsed);

            // Verify all fields (including conflicting ones work in V1)
            assertThat(restored.getSensorId()).isEqualTo("SENSOR-V1-001");
            assertThat(restored.getDeviceName()).isEqualTo("Temperature Probe");
            assertThat(restored.getUnitType()).isEqualTo(2);
            assertThat(restored.getPrecisionLevel()).isEqualTo(4);
            assertThat(restored.hasCalibrationId()).isTrue();
            assertThat(restored.getCalibrationId()).isEqualTo(99999);
            assertThat(restored.getRawValue()).isEqualTo(25000);
            assertThat(restored.hasLocation()).isTrue();
            assertThat(restored.getLocation()).isEqualTo("Building A, Room 101");
            assertThat(restored.getWrapperVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("V1 SensorReading builder works for non-conflicting fields")
        void v1BuilderForNonConflictingFields() {
            Telemetry.SensorReading initial = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-INIT")
                    .setDeviceName("Initial")
                    .setUnitType(0)
                    .setPrecisionLevel(0)
                    .setRawValue(0)
                    .setReadingDate(createV1Date(2024, 1, 1))
                    .build();

            SensorReading reading = new com.example.model.v1.SensorReading(initial);

            // Use builder to modify non-conflicting fields
            SensorReading modified = reading.toBuilder()
                    .setSensorId("SENSOR-MODIFIED")
                    .setDeviceName("Modified Device")
                    .setLocation("New Location")
                    .build();

            assertThat(modified.getSensorId()).isEqualTo("SENSOR-MODIFIED");
            assertThat(modified.getDeviceName()).isEqualTo("Modified Device");
            assertThat(modified.hasLocation()).isTrue();
            assertThat(modified.getLocation()).isEqualTo("New Location");
        }
    }

    @Nested
    @DisplayName("V1 TelemetryReport Round-Trip")
    class V1TelemetryReportRoundTripTest {

        @Test
        @DisplayName("V1 TelemetryReport with checksum (string) survives round-trip")
        void v1ReportRoundTrip() throws InvalidProtocolBufferException {
            Telemetry.TelemetryReport original = Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-V1-001")
                    .setReading(createFullV1SensorReading())
                    .setChecksum("sha256:abcdef123456")
                    .setGeneratedAt(1704153600000L)
                    .setFormatVersion("1.0")
                    .build();

            TelemetryReport report = new com.example.model.v1.TelemetryReport(original);
            byte[] bytes = report.toBytes();

            Telemetry.TelemetryReport parsed = Telemetry.TelemetryReport.parseFrom(bytes);
            TelemetryReport restored = new com.example.model.v1.TelemetryReport(parsed);

            assertThat(restored.getReportNumber()).isEqualTo("RPT-V1-001");
            assertThat(restored.hasChecksum()).isTrue();
            assertThat(restored.getChecksum()).isEqualTo("sha256:abcdef123456");
            assertThat(restored.getGeneratedAt()).isEqualTo(1704153600000L);
            assertThat(restored.hasFormatVersion()).isTrue();
            assertThat(restored.getFormatVersion()).isEqualTo("1.0");

            // Nested reading
            assertThat(restored.getReading()).isNotNull();
            assertThat(restored.getReading().getSensorId()).isEqualTo("SENSOR-FULL");
        }
    }

    @Nested
    @DisplayName("V1 BatchTelemetry Round-Trip")
    class V1BatchTelemetryRoundTripTest {

        @Test
        @DisplayName("V1 BatchTelemetry with multiple readings survives round-trip")
        void v1BatchRoundTrip() throws InvalidProtocolBufferException {
            Telemetry.BatchTelemetry original = Telemetry.BatchTelemetry.newBuilder()
                    .setBatchId("BATCH-V1-001")
                    .addReadings(createFullV1SensorReading())
                    .addReadings(Telemetry.SensorReading.newBuilder()
                            .setSensorId("SENSOR-002")
                            .setDeviceName("Second Sensor")
                            .setUnitType(1)
                            .setPrecisionLevel(2)
                            .setRawValue(1000)
                            .setReadingDate(createV1Date(2024, 5, 16))
                            .build())
                    .setTotalCount(2)
                    .setSyncStatus(2)
                    .setCollectionDate(createV1Date(2024, 5, 15))
                    .build();

            BatchTelemetry batch = new com.example.model.v1.BatchTelemetry(original);
            byte[] bytes = batch.toBytes();

            Telemetry.BatchTelemetry parsed = Telemetry.BatchTelemetry.parseFrom(bytes);
            BatchTelemetry restored = new com.example.model.v1.BatchTelemetry(parsed);

            assertThat(restored.getBatchId()).isEqualTo("BATCH-V1-001");
            assertThat(restored.getTotalCount()).isEqualTo(2);
            assertThat(restored.getSyncStatus()).isEqualTo(2);
            assertThat(restored.getReadings()).hasSize(2);
            assertThat(restored.getReadings().get(0).getSensorId()).isEqualTo("SENSOR-FULL");
            assertThat(restored.getReadings().get(1).getSensorId()).isEqualTo("SENSOR-002");
        }
    }

    // ==================== V2 Round-Trip Tests ====================

    @Nested
    @DisplayName("V2 SensorReading Round-Trip")
    class V2SensorReadingRoundTripTest {

        @Test
        @DisplayName("V2 SensorReading with enum types survives round-trip via typed proto")
        void v2RoundTripWithEnums() throws InvalidProtocolBufferException {
            com.example.proto.v2.Telemetry.SensorReading original =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-V2-001")
                    .setDeviceName("Advanced Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_KELVIN)
                    .setPrecisionLevel(3.14159)
                    .setCalibrationInfo(com.example.proto.v2.Telemetry.CalibrationInfo.newBuilder()
                            .setCalibrationId("CAL-2024")
                            .setTechnicianId("TECH-001")
                            .setAccuracyRating(98)
                            .build())
                    .setRawValue(293_150_000L)  // ~293K in microkelvins
                    .setReadingDate(createV2Date(2024, 8, 10))
                    .setLocation("Lab 3")
                    .setFirmwareVersion("2.1.0")
                    .setNetworkId("NET-WIFI-01")
                    .build();

            com.example.model.v2.SensorReading reading =
                    new com.example.model.v2.SensorReading(original);

            // Non-conflicting fields work via unified interface
            assertThat(reading.getSensorId()).isEqualTo("SENSOR-V2-001");
            assertThat(reading.getDeviceName()).isEqualTo("Advanced Sensor");
            assertThat(reading.getLocation()).isEqualTo("Lab 3");
            assertThat(reading.getFirmwareVersion()).isEqualTo("2.1.0");
            assertThat(reading.getNetworkId()).isEqualTo("NET-WIFI-01");

            // Serialize and restore
            byte[] bytes = reading.toBytes();
            com.example.proto.v2.Telemetry.SensorReading parsed =
                    com.example.proto.v2.Telemetry.SensorReading.parseFrom(bytes);
            com.example.model.v2.SensorReading restored =
                    new com.example.model.v2.SensorReading(parsed);

            // Verify via typed proto
            assertThat(restored.getTypedProto().getUnitType()).isEqualTo(UnitTypeEnum.UNIT_KELVIN);
            assertThat(restored.getTypedProto().getPrecisionLevel()).isEqualTo(3.14159);
            assertThat(restored.getTypedProto().getRawValue()).isEqualTo(293_150_000L);
            assertThat(restored.getTypedProto().getCalibrationInfo().getCalibrationId())
                    .isEqualTo("CAL-2024");
        }

        @Test
        @DisplayName("V2 SensorReading builder works for non-conflicting fields")
        void v2BuilderForNonConflictingFields() {
            com.example.proto.v2.Telemetry.SensorReading initial =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-INIT")
                    .setDeviceName("Initial")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(0L)
                    .setReadingDate(createV2Date(2024, 1, 1))
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(initial);

            // Modify non-conflicting fields
            SensorReading modified = reading.toBuilder()
                    .setSensorId("SENSOR-V2-MODIFIED")
                    .setDeviceName("V2 Modified Device")
                    .setLocation("V2 Location")
                    .setFirmwareVersion("3.0.0")
                    .setNetworkId("NET-5G")
                    .build();

            assertThat(modified.getSensorId()).isEqualTo("SENSOR-V2-MODIFIED");
            assertThat(modified.getDeviceName()).isEqualTo("V2 Modified Device");
            assertThat(modified.getLocation()).isEqualTo("V2 Location");
            assertThat(modified.getFirmwareVersion()).isEqualTo("3.0.0");
            assertThat(modified.getNetworkId()).isEqualTo("NET-5G");
        }

        @Test
        @DisplayName("V2 SensorReading with nested metadata survives round-trip")
        void v2WithNestedMetadataRoundTrip() throws InvalidProtocolBufferException {
            com.example.proto.v2.Telemetry.SensorReading.SensorMetadata metadata =
                    com.example.proto.v2.Telemetry.SensorReading.SensorMetadata.newBuilder()
                    .setManufacturer("Acme Sensors")
                    .setModel("AT-2000")
                    .setSerialNumber("SN-123456789")
                    .setInstallYear(2023)
                    .build();

            com.example.proto.v2.Telemetry.SensorReading original =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-META")
                    .setDeviceName("Sensor with Metadata")
                    .setUnitType(UnitTypeEnum.UNIT_PERCENT)
                    .setPrecisionLevel(0.01)
                    .setRawValue(5000L)
                    .setReadingDate(createV2Date(2024, 9, 1))
                    .setMetadata(metadata)
                    .build();

            SensorReading reading = new com.example.model.v2.SensorReading(original);

            // Check metadata via unified interface
            assertThat(reading.hasMetadata()).isTrue();
            assertThat(reading.getMetadata()).isNotNull();
            assertThat(reading.getMetadata().getManufacturer()).isEqualTo("Acme Sensors");
            assertThat(reading.getMetadata().getModel()).isEqualTo("AT-2000");

            // Round-trip
            byte[] bytes = reading.toBytes();
            com.example.proto.v2.Telemetry.SensorReading parsed =
                    com.example.proto.v2.Telemetry.SensorReading.parseFrom(bytes);
            SensorReading restored = new com.example.model.v2.SensorReading(parsed);

            assertThat(restored.getMetadata().getSerialNumber()).isEqualTo("SN-123456789");
            assertThat(restored.getMetadata().getInstallYear()).isEqualTo(2023);
        }
    }

    @Nested
    @DisplayName("V2 TelemetryReport Round-Trip")
    class V2TelemetryReportRoundTripTest {

        @Test
        @DisplayName("V2 TelemetryReport with bytes checksum survives round-trip")
        void v2ReportWithBytesRoundTrip() throws InvalidProtocolBufferException {
            byte[] checksumData = new byte[]{
                    (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF,
                    0x01, 0x02, 0x03, 0x04
            };

            com.example.proto.v2.Telemetry.TelemetryReport original =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-V2-001")
                    .setReading(createFullV2SensorReading())
                    .setChecksum(ByteString.copyFrom(checksumData))
                    .setGeneratedAt(createV2Date(2024, 10, 20))
                    .setFormatVersion("2.0")
                    .setExportFormat("JSON")
                    .setCompressed(true)
                    .build();

            com.example.model.v2.TelemetryReport report =
                    new com.example.model.v2.TelemetryReport(original);

            // Non-conflicting fields via unified interface
            assertThat(report.getReportNumber()).isEqualTo("RPT-V2-001");
            assertThat(report.getFormatVersion()).isEqualTo("2.0");
            assertThat(report.getExportFormat()).isEqualTo("JSON");
            assertThat(report.isCompressed()).isTrue();

            // Conflicting fields return defaults
            assertThat(report.hasChecksum()).isFalse();
            assertThat(report.getGeneratedAt()).isEqualTo(0L);

            // Round-trip
            byte[] bytes = report.toBytes();
            com.example.proto.v2.Telemetry.TelemetryReport parsed =
                    com.example.proto.v2.Telemetry.TelemetryReport.parseFrom(bytes);
            com.example.model.v2.TelemetryReport restored =
                    new com.example.model.v2.TelemetryReport(parsed);

            // Verify via typed proto
            assertThat(restored.getTypedProto().getChecksum().toByteArray())
                    .isEqualTo(checksumData);
            assertThat(restored.getTypedProto().getGeneratedAt().getYear()).isEqualTo(2024);
            assertThat(restored.getTypedProto().getGeneratedAt().getMonth()).isEqualTo(10);
        }

        @Test
        @DisplayName("V2 TelemetryReport builder modifies non-conflicting fields")
        void v2ReportBuilderRoundTrip() {
            com.example.proto.v2.Telemetry.TelemetryReport initial =
                    com.example.proto.v2.Telemetry.TelemetryReport.newBuilder()
                    .setReportNumber("RPT-INIT")
                    .setReading(createFullV2SensorReading())
                    .setGeneratedAt(createV2Date(2024, 1, 1))
                    .build();

            TelemetryReport report = new com.example.model.v2.TelemetryReport(initial);

            // Modify via builder
            TelemetryReport modified = report.toBuilder()
                    .setReportNumber("RPT-MODIFIED")
                    .setFormatVersion("2.5")
                    .setExportFormat("XML")
                    .setCompressed(true)
                    .build();

            assertThat(modified.getReportNumber()).isEqualTo("RPT-MODIFIED");
            assertThat(modified.getFormatVersion()).isEqualTo("2.5");
            assertThat(modified.getExportFormat()).isEqualTo("XML");
            assertThat(modified.isCompressed()).isTrue();
        }
    }

    @Nested
    @DisplayName("V2 BatchTelemetry Round-Trip")
    class V2BatchTelemetryRoundTripTest {

        @Test
        @DisplayName("V2 BatchTelemetry with enum status and statistics survives round-trip")
        void v2BatchWithStatisticsRoundTrip() throws InvalidProtocolBufferException {
            com.example.proto.v2.Telemetry.BatchTelemetry.BatchStatistics stats =
                    com.example.proto.v2.Telemetry.BatchTelemetry.BatchStatistics.newBuilder()
                    .setMinValue(100)
                    .setMaxValue(500)
                    .setAverageValue(275.5)
                    .build();

            com.example.proto.v2.Telemetry.BatchTelemetry original =
                    com.example.proto.v2.Telemetry.BatchTelemetry.newBuilder()
                    .setBatchId("BATCH-V2-001")
                    .addReadings(createFullV2SensorReading())
                    .setTotalCount(1)
                    .setSyncStatus(SyncStatus.SYNC_COMPLETED)
                    .setCollectionDate(createV2Date(2024, 11, 5))
                    .setCollectorId("COLLECTOR-42")
                    .setSuccessCount(1)
                    .setErrorCount(0)
                    .setStatistics(stats)
                    .build();

            com.example.model.v2.BatchTelemetry batch =
                    new com.example.model.v2.BatchTelemetry(original);

            // Non-conflicting fields
            assertThat(batch.getBatchId()).isEqualTo("BATCH-V2-001");
            assertThat(batch.getTotalCount()).isEqualTo(1);
            assertThat(batch.getCollectorId()).isEqualTo("COLLECTOR-42");
            assertThat(batch.getSuccessCount()).isEqualTo(1);

            // syncStatus is conflicting, returns default
            assertThat(batch.getSyncStatus()).isEqualTo(0);

            // Nested statistics
            assertThat(batch.hasStatistics()).isTrue();
            assertThat(batch.getStatistics().getMinValue()).isEqualTo(100);
            assertThat(batch.getStatistics().getMaxValue()).isEqualTo(500);
            assertThat(batch.getStatistics().getAverageValue()).isEqualTo(275.5);

            // Round-trip
            byte[] bytes = batch.toBytes();
            com.example.proto.v2.Telemetry.BatchTelemetry parsed =
                    com.example.proto.v2.Telemetry.BatchTelemetry.parseFrom(bytes);
            com.example.model.v2.BatchTelemetry restored =
                    new com.example.model.v2.BatchTelemetry(parsed);

            // Verify enum via typed proto
            assertThat(restored.getTypedProto().getSyncStatus())
                    .isEqualTo(SyncStatus.SYNC_COMPLETED);
            assertThat(restored.getStatistics().getAverageValue()).isEqualTo(275.5);
        }
    }

    // ==================== Cross-Version Compatibility Tests ====================

    @Nested
    @DisplayName("Cross-Version Polymorphic Usage")
    class CrossVersionPolymorphicTest {

        @Test
        @DisplayName("Process mixed V1 and V2 SensorReadings uniformly")
        void processMixedVersions() {
            // Create V1 reading
            Telemetry.SensorReading v1Proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-V1")
                    .setDeviceName("V1 Sensor")
                    .setUnitType(3)
                    .setPrecisionLevel(2)
                    .setRawValue(12345)
                    .setReadingDate(createV1Date(2024, 6, 1))
                    .build();
            SensorReading v1Reading = new com.example.model.v1.SensorReading(v1Proto);

            // Create V2 reading
            com.example.proto.v2.Telemetry.SensorReading v2Proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("SENSOR-V2")
                    .setDeviceName("V2 Sensor")
                    .setUnitType(UnitTypeEnum.UNIT_BAR)
                    .setPrecisionLevel(2.5)
                    .setRawValue(67890L)
                    .setReadingDate(createV2Date(2024, 6, 1))
                    .build();
            SensorReading v2Reading = new com.example.model.v2.SensorReading(v2Proto);

            // Process uniformly via interface
            SensorReading[] readings = {v1Reading, v2Reading};

            for (SensorReading reading : readings) {
                // Non-conflicting fields work for both versions
                assertThat(reading.getSensorId()).startsWith("SENSOR-");
                assertThat(reading.getDeviceName()).endsWith("Sensor");
                assertThat(reading.getReadingDate()).isNotNull();
                assertThat(reading.getReadingDate().getYear()).isEqualTo(2024);

                // Can distinguish versions
                if (reading.getWrapperVersion() == 1) {
                    // V1 can access conflicting fields
                    assertThat(reading.getUnitType()).isEqualTo(3);
                    assertThat(reading.getRawValue()).isEqualTo(12345);
                } else {
                    // V2 conflicting fields return defaults
                    assertThat(reading.getUnitType()).isEqualTo(0);
                    assertThat(reading.getRawValue()).isEqualTo(0);
                }
            }
        }

        @Test
        @DisplayName("Builder modifications work for both versions")
        void builderWorksForBothVersions() {
            // V1 reading
            Telemetry.SensorReading v1Proto = Telemetry.SensorReading.newBuilder()
                    .setSensorId("V1")
                    .setDeviceName("V1 Device")
                    .setUnitType(0)
                    .setPrecisionLevel(0)
                    .setRawValue(0)
                    .setReadingDate(createV1Date(2024, 1, 1))
                    .build();
            SensorReading v1 = new com.example.model.v1.SensorReading(v1Proto);

            // V2 reading
            com.example.proto.v2.Telemetry.SensorReading v2Proto =
                    com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                    .setSensorId("V2")
                    .setDeviceName("V2 Device")
                    .setUnitType(UnitTypeEnum.UNIT_CELSIUS)
                    .setPrecisionLevel(1.0)
                    .setRawValue(0L)
                    .setReadingDate(createV2Date(2024, 1, 1))
                    .build();
            SensorReading v2 = new com.example.model.v2.SensorReading(v2Proto);

            // Modify both via unified builder interface
            SensorReading[] readings = {v1, v2};

            for (SensorReading reading : readings) {
                SensorReading modified = reading.toBuilder()
                        .setSensorId("MODIFIED-" + reading.getWrapperVersion())
                        .setLocation("Common Location")
                        .build();

                assertThat(modified.getSensorId())
                        .isEqualTo("MODIFIED-" + reading.getWrapperVersion());
                assertThat(modified.getLocation()).isEqualTo("Common Location");
                assertThat(modified.getWrapperVersion()).isEqualTo(reading.getWrapperVersion());
            }
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

    private Telemetry.SensorReading createFullV1SensorReading() {
        return Telemetry.SensorReading.newBuilder()
                .setSensorId("SENSOR-FULL")
                .setDeviceName("Full V1 Sensor")
                .setUnitType(1)
                .setPrecisionLevel(3)
                .setCalibrationId(42)
                .setRawValue(9999)
                .setReadingDate(createV1Date(2024, 5, 15))
                .setLocation("Main Hall")
                .build();
    }

    private com.example.proto.v2.Telemetry.SensorReading createFullV2SensorReading() {
        return com.example.proto.v2.Telemetry.SensorReading.newBuilder()
                .setSensorId("SENSOR-FULL-V2")
                .setDeviceName("Full V2 Sensor")
                .setUnitType(UnitTypeEnum.UNIT_FAHRENHEIT)
                .setPrecisionLevel(2.5)
                .setRawValue(77_000L)
                .setReadingDate(createV2Date(2024, 5, 15))
                .setLocation("Control Room")
                .setFirmwareVersion("2.0.0")
                .build();
    }
}
