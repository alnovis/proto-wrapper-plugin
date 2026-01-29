package io.alnovis.protowrapper.generator.metadata;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.EnumInfo.EnumValue;
import io.alnovis.protowrapper.model.MessageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SchemaInfoGenerator.
 */
@DisplayName("SchemaInfoGenerator Tests")
class SchemaInfoGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("com.example.api")
                .generateSchemaMetadata(true)
                .build();
    }

    @Nested
    @DisplayName("Class Generation")
    class ClassGenerationTest {

        @Test
        @DisplayName("generates SchemaInfo class with correct name")
        void generatesSchemaInfoClass() {
            VersionSchema schema = createSchema("v1");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();

            assertThat(javaFile.typeSpec.name).isEqualTo("SchemaInfoV1");
            assertThat(javaFile.packageName).isEqualTo("com.example.metadata");
        }

        @Test
        @DisplayName("generated class implements SchemaInfo interface")
        void implementsSchemaInfoInterface() {
            VersionSchema schema = createSchema("v2");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("implements SchemaInfo");
        }

        @Test
        @DisplayName("generates INSTANCE singleton field")
        void generatesInstanceSingleton() {
            VersionSchema schema = createSchema("v1");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static final SchemaInfoV1 INSTANCE = new SchemaInfoV1()");
        }

        @Test
        @DisplayName("generates private constructor")
        void generatesPrivateConstructor() {
            VersionSchema schema = createSchema("v1");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private SchemaInfoV1()");
        }
    }

    @Nested
    @DisplayName("Version ID Generation")
    class VersionIdTest {

        @Test
        @DisplayName("getVersionId returns correct version using ProtocolVersions constant")
        void getVersionIdReturnsCorrectVersion() {
            VersionSchema schema = createSchema("v1");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public String getVersionId()");
            assertThat(code).contains("return VERSION_ID");
            assertThat(code).contains("ProtocolVersions.V1");
        }
    }

    @Nested
    @DisplayName("Enum Metadata Generation")
    class EnumMetadataTest {

        @Test
        @DisplayName("generates ENUMS map with enum metadata")
        void generatesEnumsMap() {
            VersionSchema schema = createSchemaWithEnums();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Map<String, SchemaInfo.EnumInfo> ENUMS");
            assertThat(code).contains("getEnums()");
            assertThat(code).contains("return ENUMS");
        }

        @Test
        @DisplayName("includes enum values with names and numbers")
        void includesEnumValues() {
            VersionSchema schema = createSchemaWithEnums();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("TaxTypeEnum");
            assertThat(code).contains("VAT");
            assertThat(code).contains("100");
        }

        @Test
        @DisplayName("generates empty map when no enums present")
        void generatesEmptyMapWhenNoEnums() {
            VersionSchema schema = createSchema("v1");
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Should use Map.of() or Collections.emptyMap()
            assertThat(code).containsAnyOf("Map.of()", "Collections.emptyMap()");
        }
    }

    @Nested
    @DisplayName("Message Metadata Generation")
    class MessageMetadataTest {

        @Test
        @DisplayName("generates MESSAGES map with message metadata")
        void generatesMessagesMap() {
            VersionSchema schema = createSchemaWithMessages();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Map<String, SchemaInfo.MessageInfo> MESSAGES");
            assertThat(code).contains("getMessages()");
            assertThat(code).contains("return MESSAGES");
        }

        @Test
        @DisplayName("includes message name and full name")
        void includesMessageNameAndFullName() {
            VersionSchema schema = createSchemaWithMessages();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Order");
        }
    }

    @Nested
    @DisplayName("Inner Classes Generation")
    class InnerClassesTest {

        @Test
        @DisplayName("generates EnumInfoImpl inner class")
        void generatesEnumInfoImplClass() {
            VersionSchema schema = createSchemaWithEnums();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private static final class EnumInfoImpl implements SchemaInfo.EnumInfo");
        }

        @Test
        @DisplayName("generates MessageInfoImpl inner class")
        void generatesMessageInfoImplClass() {
            VersionSchema schema = createSchemaWithMessages();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private static final class MessageInfoImpl implements SchemaInfo.MessageInfo");
        }
    }

    @Nested
    @DisplayName("Java 8 Compatibility")
    class Java8CompatibilityTest {

        @Test
        @DisplayName("uses Collections.unmodifiableMap for Java 8")
        void usesCollectionsForJava8() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.example.api")
                    .generateSchemaMetadata(true)
                    .targetJavaVersion(8)
                    .build();

            VersionSchema schema = createSchemaWithEnums();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(java8Config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Collections.unmodifiableMap");
        }

        @Test
        @DisplayName("uses Map.ofEntries for Java 9+")
        void usesMapOfEntriesForJava9Plus() {
            GeneratorConfig java9Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.example.api")
                    .generateSchemaMetadata(true)
                    .targetJavaVersion(9)
                    .build();

            VersionSchema schema = createSchemaWithEnums();
            SchemaInfoGenerator generator = new SchemaInfoGenerator(java9Config, schema);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Map.ofEntries");
        }
    }

    // Helper methods using real objects instead of mocks

    private VersionSchema createSchema(String version) {
        return new VersionSchema(version);
    }

    private VersionSchema createSchemaWithEnums() {
        VersionSchema schema = new VersionSchema("v1");

        EnumInfo enumInfo = new EnumInfo(
                "TaxTypeEnum",
                "com.example.proto.v1.TaxTypeEnum",
                List.of(
                        new EnumValue("VAT", 100),
                        new EnumValue("EXCISE", 200)
                )
        );
        schema.addEnum(enumInfo);

        return schema;
    }

    private VersionSchema createSchemaWithMessages() {
        VersionSchema schema = new VersionSchema("v1");

        MessageInfo messageInfo = new MessageInfo(
                "Order",
                "com.example.proto.v1.Order",
                "com.example.proto.v1",
                List.of(),
                List.of(),
                List.of()
        );
        schema.addMessage(messageInfo);

        return schema;
    }
}
