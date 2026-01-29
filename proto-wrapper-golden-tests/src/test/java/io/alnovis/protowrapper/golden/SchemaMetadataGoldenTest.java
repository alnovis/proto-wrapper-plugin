package io.alnovis.protowrapper.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.alnovis.protowrapper.runtime.SchemaInfo;
import io.alnovis.protowrapper.runtime.VersionSchemaDiff;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden tests for schema metadata generation.
 *
 * <p>Tests verify that SchemaInfo and SchemaDiff classes are correctly generated
 * and provide accurate runtime metadata about the schema.</p>
 *
 * @since 2.3.0
 */
@DisplayName("Schema Metadata Golden Tests")
class SchemaMetadataGoldenTest {

    @Nested
    @DisplayName("SchemaInfo - Proto3")
    class Proto3SchemaInfoTests {

        @Test
        @DisplayName("V1 returns correct version ID")
        void v1ReturnsCorrectVersionId() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE;
            assertThat(info.getVersionId())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto3.wrapper.api.ProtocolVersions.V1);
        }

        @Test
        @DisplayName("V2 returns correct version ID")
        void v2ReturnsCorrectVersionId() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV2.INSTANCE;
            assertThat(info.getVersionId())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto3.wrapper.api.ProtocolVersions.V2);
        }

        @Test
        @DisplayName("V1 contains TestEnum with expected values")
        void v1ContainsTestEnumWithValues() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE;

            assertThat(info.getEnums()).containsKey("TestEnum");

            SchemaInfo.EnumInfo enumInfo = info.getEnums().get("TestEnum");
            assertThat(enumInfo.getName()).isEqualTo("TestEnum");
            assertThat(enumInfo.getFullName()).contains("proto3.v1.TestEnum");

            // Proto3 enums must have 0 as first value
            assertThat(enumInfo.getValues()).isNotEmpty();
            assertThat(enumInfo.getValues().get(0).number()).isEqualTo(0);
        }

        @Test
        @DisplayName("V1 contains expected messages")
        void v1ContainsExpectedMessages() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE;

            assertThat(info.getMessages()).containsKey("AllFieldTypes");
            assertThat(info.getMessages()).containsKey("NestedMessage");

            SchemaInfo.MessageInfo msgInfo = info.getMessages().get("AllFieldTypes");
            assertThat(msgInfo.getName()).isEqualTo("AllFieldTypes");
            assertThat(msgInfo.getFullName()).contains("proto3.v1.AllFieldTypes");
        }

        @Test
        @DisplayName("singleton instance is consistent")
        void singletonInstanceIsConsistent() {
            var v1 = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE;
            var v2 = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV2.INSTANCE;
            assertThat(v1).isSameAs(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE);
            assertThat(v2).isSameAs(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV2.INSTANCE);
        }
    }

    @Nested
    @DisplayName("SchemaInfo - Proto2")
    class Proto2SchemaInfoTests {

        @Test
        @DisplayName("V1 returns correct version ID")
        void v1ReturnsCorrectVersionId() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV1.INSTANCE;
            assertThat(info.getVersionId())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto2.wrapper.api.ProtocolVersions.V1);
        }

        @Test
        @DisplayName("V2 returns correct version ID")
        void v2ReturnsCorrectVersionId() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV2.INSTANCE;
            assertThat(info.getVersionId())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto2.wrapper.api.ProtocolVersions.V2);
        }

        @Test
        @DisplayName("V1 contains expected messages")
        void v1ContainsExpectedMessages() {
            SchemaInfo info = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV1.INSTANCE;

            assertThat(info.getMessages()).isNotEmpty();
        }

        @Test
        @DisplayName("singleton instance is consistent")
        void singletonInstanceIsConsistent() {
            var v1 = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV1.INSTANCE;
            var v2 = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV2.INSTANCE;
            assertThat(v1).isSameAs(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV1.INSTANCE);
            assertThat(v2).isSameAs(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV2.INSTANCE);
        }
    }

    @Nested
    @DisplayName("SchemaDiff - Proto3")
    class Proto3SchemaDiffTests {

        @Test
        @DisplayName("diff has correct version range")
        void diffHasCorrectVersionRange() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            assertThat(diff.getFromVersion())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto3.wrapper.api.ProtocolVersions.V1);
            assertThat(diff.getToVersion())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto3.wrapper.api.ProtocolVersions.V2);
        }

        @Test
        @DisplayName("diff returns field changes list (may be empty)")
        void diffReturnsFieldChangesList() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            // List should not be null, even if empty
            assertThat(diff.getFieldChanges()).isNotNull();
        }

        @Test
        @DisplayName("diff returns enum changes list (may be empty)")
        void diffReturnsEnumChangesList() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            // List should not be null, even if empty
            assertThat(diff.getEnumChanges()).isNotNull();
        }

        @Test
        @DisplayName("singleton instance is consistent")
        void singletonInstanceIsConsistent() {
            var diff = io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;
            assertThat(diff).isSameAs(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE);
        }
    }

    @Nested
    @DisplayName("SchemaDiff - Proto2")
    class Proto2SchemaDiffTests {

        @Test
        @DisplayName("diff has correct version range")
        void diffHasCorrectVersionRange() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            assertThat(diff.getFromVersion())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto2.wrapper.api.ProtocolVersions.V1);
            assertThat(diff.getToVersion())
                    .isEqualTo(io.alnovis.protowrapper.golden.proto2.wrapper.api.ProtocolVersions.V2);
        }

        @Test
        @DisplayName("diff returns field changes list (may be empty)")
        void diffReturnsFieldChangesList() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            assertThat(diff.getFieldChanges()).isNotNull();
        }

        @Test
        @DisplayName("diff returns enum changes list (may be empty)")
        void diffReturnsEnumChangesList() {
            VersionSchemaDiff diff = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;

            assertThat(diff.getEnumChanges()).isNotNull();
        }

        @Test
        @DisplayName("singleton instance is consistent")
        void singletonInstanceIsConsistent() {
            var diff = io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE;
            assertThat(diff).isSameAs(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE);
        }
    }

    @Nested
    @DisplayName("SchemaInfo Interface Contract")
    class SchemaInfoInterfaceContract {

        @Test
        @DisplayName("all SchemaInfo instances implement common interface")
        void allSchemaInfoImplementInterface() {
            assertThat(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV1.INSTANCE)
                    .isInstanceOf(SchemaInfo.class);
            assertThat(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaInfoV2.INSTANCE)
                    .isInstanceOf(SchemaInfo.class);
            assertThat(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV1.INSTANCE)
                    .isInstanceOf(SchemaInfo.class);
            assertThat(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaInfoV2.INSTANCE)
                    .isInstanceOf(SchemaInfo.class);
        }

        @Test
        @DisplayName("all SchemaDiff instances implement common interface")
        void allSchemaDiffImplementInterface() {
            assertThat(io.alnovis.protowrapper.golden.proto2.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE)
                    .isInstanceOf(VersionSchemaDiff.class);
            assertThat(io.alnovis.protowrapper.golden.proto3.wrapper.metadata.SchemaDiffV1ToV2.INSTANCE)
                    .isInstanceOf(VersionSchemaDiff.class);
        }
    }
}
