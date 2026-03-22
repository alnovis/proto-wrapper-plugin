package io.alnovis.protowrapper.ircraft;

import io.alnovis.ircraft.core.Module;
import io.alnovis.ircraft.core.*;
import io.alnovis.ircraft.dialect.proto.ops.*;
import io.alnovis.protowrapper.PluginLogger;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.*;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the ircraft pipeline within proto-wrapper-plugin.
 */
class IrcraftIntegrationTest {

    @TempDir
    Path outputDir;

    private GeneratorConfig config;
    private PluginLogger logger;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(outputDir)
                .apiPackage("com.example.api")
                .implPackagePattern("com.example.{version}")
                .build();
        logger = PluginLogger.console();
    }

    // ── Bridge Tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("IrcraftBridge")
    class BridgeTests {

        @Test
        @DisplayName("converts simple MergedSchema to SchemaOp")
        void convertsSimpleSchema() {
            MergedSchema schema = createSimpleSchema();
            IrcraftBridge bridge = new IrcraftBridge();

            SchemaOp protoIR = bridge.toProtoIR(schema);

            assertThat(protoIR.versions().size()).isEqualTo(2);
            assertThat(protoIR.messages().size()).isEqualTo(1);
            assertThat(protoIR.messages().apply(0).name()).isEqualTo("Money");
            assertThat(protoIR.messages().apply(0).fields().size()).isEqualTo(2);
        }

        @Test
        @DisplayName("converts schema with enums")
        void convertsSchemaWithEnums() {
            MergedSchema schema = new MergedSchema(List.of("v1"));
            MergedEnum currency = new MergedEnum("Currency");
            currency.addValue(new MergedEnumValue(new EnumInfo.EnumValue("USD", 0), "v1"));
            currency.addValue(new MergedEnumValue(new EnumInfo.EnumValue("EUR", 1), "v1"));
            currency.addVersion("v1");
            schema.addEnum(currency);

            IrcraftBridge bridge = new IrcraftBridge();
            SchemaOp protoIR = bridge.toProtoIR(schema);

            assertThat(protoIR.enums().size()).isEqualTo(1);
            assertThat(protoIR.enums().apply(0).name()).isEqualTo("Currency");
            assertThat(protoIR.enums().apply(0).values().size()).isEqualTo(2);
        }

        @Test
        @DisplayName("preserves field metadata")
        void preservesFieldMetadata() {
            MergedSchema schema = createSimpleSchema();
            IrcraftBridge bridge = new IrcraftBridge();

            SchemaOp protoIR = bridge.toProtoIR(schema);
            FieldOp amountField = protoIR.messages().apply(0).fields().apply(0);

            assertThat(amountField.name()).isEqualTo("amount");
            assertThat(amountField.javaName()).isEqualTo("amount");
            assertThat(amountField.number()).isEqualTo(1);
        }
    }

    // ── Generator Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("IrcraftGenerator")
    class GeneratorTests {

        @Test
        @DisplayName("generates Java files for simple schema")
        void generatesJavaFiles() throws IOException {
            MergedSchema schema = createSimpleSchema();
            IrcraftGenerator generator = new IrcraftGenerator(config, logger);

            int count = generator.generateAll(schema);

            assertThat(count).isGreaterThan(0);

            // Check generated files exist
            List<Path> javaFiles = Files.walk(outputDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertThat(javaFiles).isNotEmpty();
        }

        @Test
        @DisplayName("generates interface with getters")
        void generatesInterface() throws IOException {
            MergedSchema schema = createSimpleSchema();
            IrcraftGenerator generator = new IrcraftGenerator(config, logger);
            generator.generateAll(schema);

            Path interfaceFile = findFile(outputDir, "Money.java");
            assertThat(interfaceFile).isNotNull();

            String source = Files.readString(interfaceFile);
            assertThat(source).contains("package com.example.api;");
            assertThat(source).contains("interface Money");
            assertThat(source).contains("getAmount()");
            assertThat(source).contains("getCurrency()");
        }

        @Test
        @DisplayName("generates abstract class with extract methods")
        void generatesAbstractClass() throws IOException {
            MergedSchema schema = createSimpleSchema();
            IrcraftGenerator generator = new IrcraftGenerator(config, logger);
            generator.generateAll(schema);

            Path abstractFile = findFile(outputDir, "AbstractMoney.java");
            assertThat(abstractFile).isNotNull();

            String source = Files.readString(abstractFile);
            assertThat(source).contains("abstract class AbstractMoney");
            assertThat(source).contains("extractAmount()");
            assertThat(source).contains("extractCurrency()");
        }

        @Test
        @DisplayName("generates impl classes per version")
        void generatesImplClasses() throws IOException {
            MergedSchema schema = createSimpleSchema();
            IrcraftGenerator generator = new IrcraftGenerator(config, logger);
            generator.generateAll(schema);

            Path v1File = findFile(outputDir, "MoneyV1.java");
            Path v2File = findFile(outputDir, "MoneyV2.java");
            assertThat(v1File).isNotNull();
            assertThat(v2File).isNotNull();

            String v1Source = Files.readString(v1File);
            assertThat(v1Source).contains("package com.example.v1;");
            assertThat(v1Source).contains("class MoneyV1");
        }

        @Test
        @DisplayName("generates enum with constants")
        void generatesEnum() throws IOException {
            MergedSchema schema = new MergedSchema(List.of("v1"));
            MergedEnum status = new MergedEnum("Status");
            status.addValue(new MergedEnumValue(new EnumInfo.EnumValue("OK", 0), "v1"));
            status.addValue(new MergedEnumValue(new EnumInfo.EnumValue("ERROR", 1), "v1"));
            status.addVersion("v1");
            schema.addEnum(status);

            IrcraftGenerator generator = new IrcraftGenerator(config, logger);
            generator.generateAll(schema);

            Path enumFile = findFile(outputDir, "Status.java");
            assertThat(enumFile).isNotNull();

            String source = Files.readString(enumFile);
            assertThat(source).contains("enum Status");
            assertThat(source).contains("OK(0)");
            assertThat(source).contains("ERROR(1)");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private MergedSchema createSimpleSchema() {
        MergedSchema schema = new MergedSchema(List.of("v1", "v2"));
        MergedMessage money = new MergedMessage("Money");

        FieldInfo amountV1 = new FieldInfo("amount", "amount", 1,
                Type.TYPE_INT64, Label.LABEL_OPTIONAL, "int64",
                null, -1, null, null, true);
        FieldInfo amountV2 = new FieldInfo("amount", "amount", 1,
                Type.TYPE_INT64, Label.LABEL_OPTIONAL, "int64",
                null, -1, null, null, true);

        FieldInfo currencyV1 = new FieldInfo("currency", "currency", 2,
                Type.TYPE_STRING, Label.LABEL_OPTIONAL, "string",
                null, -1, null, null, true);
        FieldInfo currencyV2 = new FieldInfo("currency", "currency", 2,
                Type.TYPE_STRING, Label.LABEL_OPTIONAL, "string",
                null, -1, null, null, true);

        MergedField amount = MergedField.builder()
                .addVersionField("v1", amountV1)
                .addVersionField("v2", amountV2)
                .build();
        MergedField currency = MergedField.builder()
                .addVersionField("v1", currencyV1)
                .addVersionField("v2", currencyV2)
                .build();

        money.addField(amount);
        money.addField(currency);
        money.addVersion("v1");
        money.addVersion("v2");
        schema.addMessage(money);

        return schema;
    }

    private Path findFile(Path root, String fileName) throws IOException {
        return Files.walk(root)
                .filter(p -> p.getFileName().toString().equals(fileName))
                .findFirst()
                .orElse(null);
    }
}
