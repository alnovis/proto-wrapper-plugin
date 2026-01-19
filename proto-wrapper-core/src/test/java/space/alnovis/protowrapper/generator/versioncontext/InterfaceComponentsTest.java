package space.alnovis.protowrapper.generator.versioncontext;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for InterfaceComponent implementations.
 *
 * <p>Verifies that each component generates correct methods and fields
 * for the VersionContext interface.</p>
 */
@DisplayName("InterfaceComponent Tests")
class InterfaceComponentsTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private GeneratorConfig java8Config;
    private MergedSchema schema;
    private TypeSpec.Builder builder;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .implPackagePattern("org.example.impl.{version}")
                .generateBuilders(true)
                .build();

        java8Config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .implPackagePattern("org.example.impl.{version}")
                .generateBuilders(true)
                .targetJavaVersion(8)
                .build();

        schema = new MergedSchema(Arrays.asList("v1", "v2"));

        // Add Money message
        MergedMessage money = new MergedMessage("Money");
        money.addVersion("v1");
        money.addVersion("v2");

        FieldDescriptorProto billsProto = FieldDescriptorProto.newBuilder()
                .setName("bills")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        money.addField(new MergedField(new FieldInfo(billsProto), "v1"));

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        money.addField(new MergedField(new FieldInfo(coinsProto), "v1"));

        schema.addMessage(money);

        builder = TypeSpec.interfaceBuilder("VersionContext")
                .addModifiers(Modifier.PUBLIC);
    }

    @Nested
    @DisplayName("StaticFieldsComponent")
    class StaticFieldsComponentTest {

        @Test
        @DisplayName("adds CONTEXTS field with Java 9+ codegen")
        void addsContextsFieldJava9() {
            JavaVersionCodegen codegen = Java9PlusCodegen.INSTANCE;
            new StaticFieldsComponent(codegen, config, schema).addTo(builder);

            String code = builder.build().toString();
            // JavaPoet generates fully qualified names in TypeSpec.toString()
            assertThat(code).contains("CONTEXTS = createContexts()");
            assertThat(code).contains("Map<");
            assertThat(code).contains("VersionContext> CONTEXTS");
        }

        @Test
        @DisplayName("adds CONTEXTS field with Java 8 codegen")
        void addsContextsFieldJava8() {
            JavaVersionCodegen codegen = Java8Codegen.INSTANCE;
            new StaticFieldsComponent(codegen, java8Config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("VersionContextHelper.createContexts()");
        }

        @Test
        @DisplayName("adds SUPPORTED_VERSIONS field")
        void addsSupportedVersionsField() {
            JavaVersionCodegen codegen = Java9PlusCodegen.INSTANCE;
            new StaticFieldsComponent(codegen, config, schema).addTo(builder);

            String code = builder.build().toString();
            // JavaPoet generates fully qualified names
            assertThat(code).contains("SUPPORTED_VERSIONS");
            assertThat(code).contains("List<");
            assertThat(code).contains("\"v1\"");
            assertThat(code).contains("\"v2\"");
        }

        @Test
        @DisplayName("adds DEFAULT_VERSION field with latest version")
        void addsDefaultVersionField() {
            JavaVersionCodegen codegen = Java9PlusCodegen.INSTANCE;
            new StaticFieldsComponent(codegen, config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("String DEFAULT_VERSION = \"v2\"");
        }

        @Test
        @DisplayName("adds createContexts method for Java 9+")
        void addsCreateContextsMethodJava9() {
            JavaVersionCodegen codegen = Java9PlusCodegen.INSTANCE;
            new StaticFieldsComponent(codegen, config, schema).addTo(builder);

            String code = builder.build().toString();
            // JavaPoet generates fully qualified names
            assertThat(code).contains("private static");
            assertThat(code).contains("createContexts()");
            assertThat(code).contains("map.put(\"v1\"");
            assertThat(code).contains("map.put(\"v2\"");
        }

        @Test
        @DisplayName("does not add createContexts method for Java 8")
        void doesNotAddCreateContextsMethodJava8() {
            JavaVersionCodegen codegen = Java8Codegen.INSTANCE;
            new StaticFieldsComponent(codegen, java8Config, schema).addTo(builder);

            String code = builder.build().toString();
            // Java 8 uses helper class, no private static method
            assertThat(code).doesNotContain("private static");
        }
    }

    @Nested
    @DisplayName("StaticMethodsComponent")
    class StaticMethodsComponentTest {

        @Test
        @DisplayName("adds forVersionId method")
        void addsForVersionIdMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("forVersionId(");
            assertThat(code).contains("String versionId");
            assertThat(code).contains("CONTEXTS.get(versionId)");
            assertThat(code).contains("IllegalArgumentException");
        }

        @Test
        @DisplayName("adds find method returning Optional")
        void addsFindMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("find(");
            assertThat(code).contains("Optional");
            assertThat(code).contains("CONTEXTS.get(versionId)");
        }

        @Test
        @DisplayName("adds getDefault method")
        void addsGetDefaultMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("getDefault()");
            assertThat(code).contains("CONTEXTS.get(DEFAULT_VERSION)");
        }

        @Test
        @DisplayName("adds supportedVersions method")
        void addsSupportedVersionsMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("supportedVersions()");
            assertThat(code).contains("return SUPPORTED_VERSIONS");
        }

        @Test
        @DisplayName("adds defaultVersion method")
        void addsDefaultVersionMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("defaultVersion()");
            assertThat(code).contains("return DEFAULT_VERSION");
        }

        @Test
        @DisplayName("adds isSupported method")
        void addsIsSupportedMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("isSupported(");
            assertThat(code).contains("CONTEXTS.containsKey(versionId)");
        }

        @Test
        @DisplayName("adds deprecated forVersion method")
        void addsDeprecatedForVersionMethod() {
            new StaticMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            // JavaPoet generates @java.lang.Deprecated
            assertThat(code).containsPattern("@(java\\.lang\\.)?Deprecated");
            assertThat(code).contains("since = \"1.6.7\"");
            assertThat(code).contains("forRemoval = true");
            assertThat(code).contains("forVersion(int version)");
            assertThat(code).contains("switch (version)");
            assertThat(code).contains("case 1:");
            assertThat(code).contains("case 2:");
        }
    }

    @Nested
    @DisplayName("InstanceMethodsComponent")
    class InstanceMethodsComponentTest {

        @Test
        @DisplayName("adds getVersionId abstract method")
        void addsGetVersionIdMethod() {
            new InstanceMethodsComponent(schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("String getVersionId()");
        }

        @Test
        @DisplayName("adds deprecated getVersion abstract method")
        void addsDeprecatedGetVersionMethod() {
            new InstanceMethodsComponent(schema).addTo(builder);

            String code = builder.build().toString();
            // JavaPoet generates @java.lang.Deprecated
            assertThat(code).containsPattern("@(java\\.lang\\.)?Deprecated");
            assertThat(code).contains("int getVersion()");
        }
    }

    @Nested
    @DisplayName("WrapMethodsComponent")
    class WrapMethodsComponentTest {

        @Test
        @DisplayName("adds wrapXxx method for each message")
        void addsWrapMethods() {
            new WrapMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("wrapMoney(");
            assertThat(code).contains("Message proto");
        }

        @Test
        @DisplayName("adds parseXxxFromBytes method for each message")
        void addsParseFromBytesMethods() {
            new WrapMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("parseMoneyFromBytes(byte[] bytes)");
            assertThat(code).contains("InvalidProtocolBufferException");
        }

        @Test
        @DisplayName("generates default method for version-specific messages")
        void generatesDefaultMethodForVersionSpecificMessages() {
            // Add v2-only message
            MergedMessage v2Only = new MergedMessage("V2OnlyMessage");
            v2Only.addVersion("v2");
            FieldDescriptorProto fieldProto = FieldDescriptorProto.newBuilder()
                    .setName("data")
                    .setNumber(1)
                    .setType(Type.TYPE_STRING)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            v2Only.addField(new MergedField(new FieldInfo(fieldProto), "v2"));
            schema.addMessage(v2Only);

            new WrapMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("default");
            assertThat(code).contains("wrapV2OnlyMessage");
            assertThat(code).contains("UnsupportedOperationException");
        }
    }

    @Nested
    @DisplayName("BuilderMethodsComponent")
    class BuilderMethodsComponentTest {

        @Test
        @DisplayName("adds newXxxBuilder method when builders enabled")
        void addsNewBuilderMethods() {
            new BuilderMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("newMoneyBuilder()");
            assertThat(code).contains("Builder");
        }

        @Test
        @DisplayName("does not add methods when builders disabled")
        void doesNotAddMethodsWhenBuildersDisabled() {
            GeneratorConfig noBuildersConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .generateBuilders(false)
                    .build();

            new BuilderMethodsComponent(noBuildersConfig, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).doesNotContain("newMoneyBuilder");
        }

        @Test
        @DisplayName("generates default method for version-specific message builders")
        void generatesDefaultMethodForVersionSpecificBuilders() {
            // Add v2-only message
            MergedMessage v2Only = new MergedMessage("V2OnlyMessage");
            v2Only.addVersion("v2");
            FieldDescriptorProto fieldProto = FieldDescriptorProto.newBuilder()
                    .setName("data")
                    .setNumber(1)
                    .setType(Type.TYPE_STRING)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            v2Only.addField(new MergedField(new FieldInfo(fieldProto), "v2"));
            schema.addMessage(v2Only);

            new BuilderMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("default");
            assertThat(code).contains("newV2OnlyMessageBuilder");
            assertThat(code).contains("UnsupportedOperationException");
        }

        @Test
        @DisplayName("adds builder methods for nested messages")
        void addsBuilderMethodsForNestedMessages() {
            // Add Order with nested Item
            MergedMessage order = new MergedMessage("Order");
            order.addVersion("v1");
            order.addVersion("v2");

            MergedMessage item = new MergedMessage("Item");
            item.addVersion("v1");
            item.addVersion("v2");
            item.setParent(order);
            order.addNestedMessage(item);

            schema.addMessage(order);

            new BuilderMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("newOrderBuilder()");
            assertThat(code).contains("newOrderItemBuilder()");
        }
    }

    @Nested
    @DisplayName("ConvenienceMethodsComponent")
    class ConvenienceMethodsComponentTest {

        @Test
        @DisplayName("adds zeroMoney method when Money has bills and coins")
        void addsZeroMoneyMethod() {
            new ConvenienceMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("default");
            assertThat(code).contains("zeroMoney()");
            assertThat(code).contains("newMoneyBuilder()");
            assertThat(code).contains("setBills(0L)");
            assertThat(code).contains("setCoins(0)");
        }

        @Test
        @DisplayName("adds createMoney method when Money has bills and coins")
        void addsCreateMoneyMethod() {
            new ConvenienceMethodsComponent(config, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).contains("default");
            assertThat(code).contains("createMoney(long bills, int coins)");
            assertThat(code).contains("newMoneyBuilder()");
            assertThat(code).contains("setBills(bills)");
            assertThat(code).contains("setCoins(coins)");
        }

        @Test
        @DisplayName("does not add methods when builders disabled")
        void doesNotAddMethodsWhenBuildersDisabled() {
            GeneratorConfig noBuildersConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .generateBuilders(false)
                    .build();

            new ConvenienceMethodsComponent(noBuildersConfig, schema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).doesNotContain("zeroMoney");
            assertThat(code).doesNotContain("createMoney");
        }

        @Test
        @DisplayName("does not add methods when Money does not have bills field")
        void doesNotAddMethodsWhenMoneyMissingBills() {
            // Create schema with Money without bills
            MergedSchema noBillsSchema = new MergedSchema(Arrays.asList("v1", "v2"));
            MergedMessage money = new MergedMessage("Money");
            money.addVersion("v1");
            money.addVersion("v2");

            FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                    .setName("coins")
                    .setNumber(1)
                    .setType(Type.TYPE_INT32)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            money.addField(new MergedField(new FieldInfo(coinsProto), "v1"));

            noBillsSchema.addMessage(money);

            new ConvenienceMethodsComponent(config, noBillsSchema).addTo(builder);

            String code = builder.build().toString();
            assertThat(code).doesNotContain("zeroMoney");
            assertThat(code).doesNotContain("createMoney");
        }
    }
}
