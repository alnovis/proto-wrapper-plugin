package io.alnovis.protowrapper.generator.versioncontext;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for VersionContextInterfaceComposer.
 *
 * <p>Verifies that the composer correctly assembles all components
 * into a complete VersionContext interface.</p>
 */
@DisplayName("VersionContextInterfaceComposer Tests")
class VersionContextInterfaceComposerTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private GeneratorConfig java8Config;
    private MergedSchema schema;

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

        // Add Money message with bills and coins
        MergedMessage money = new MergedMessage("Money");
        money.addVersion("v1");
        money.addVersion("v2");

        FieldDescriptorProto billsProto = FieldDescriptorProto.newBuilder()
                .setName("bills")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(billsProto)).build());

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(coinsProto)).build());

        schema.addMessage(money);
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTest {

        @Test
        @DisplayName("supports method chaining")
        void supportsMethodChaining() {
            VersionContextInterfaceComposer composer = new VersionContextInterfaceComposer(config, schema);

            // Should be able to chain all methods
            JavaFile result = composer
                    .addStaticFields()
                    .addStaticMethods()
                    .addInstanceMethods()
                    .addWrapMethods()
                    .addBuilderMethods()
                    .addConvenienceMethods()
                    .build();

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("each method returns same composer instance")
        void eachMethodReturnsSameInstance() {
            VersionContextInterfaceComposer composer = new VersionContextInterfaceComposer(config, schema);

            VersionContextInterfaceComposer result1 = composer.addStaticFields();
            VersionContextInterfaceComposer result2 = result1.addStaticMethods();

            assertThat(result1).isSameAs(composer);
            assertThat(result2).isSameAs(composer);
        }
    }

    @Nested
    @DisplayName("Java 9+ Generation")
    class Java9PlusGenerationTest {

        @Test
        @DisplayName("generates complete interface with all components")
        void generatesCompleteInterface() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .addStaticMethods()
                    .addInstanceMethods()
                    .addWrapMethods()
                    .addBuilderMethods()
                    .addConvenienceMethods()
                    .build();

            String code = javaFile.toString();

            // Static fields
            assertThat(code).contains("Map<String, VersionContext> CONTEXTS");
            assertThat(code).contains("List<String> SUPPORTED_VERSIONS");
            assertThat(code).contains("String DEFAULT_VERSION");

            // Static methods
            assertThat(code).contains("static VersionContext forVersionId");
            assertThat(code).contains("static Optional<VersionContext> find");
            assertThat(code).contains("static VersionContext getDefault");
            assertThat(code).contains("static List<String> supportedVersions");
            assertThat(code).contains("static String defaultVersion");
            assertThat(code).contains("static boolean isSupported");

            // Instance methods
            assertThat(code).contains("String getVersionId");
            assertThat(code).doesNotContain("int getVersion");  // Deprecated method removed

            // Wrap methods
            assertThat(code).contains("Money wrapMoney");
            assertThat(code).contains("Money parseMoneyFromBytes");

            // Builder methods
            assertThat(code).contains("Money.Builder newMoneyBuilder");

            // Convenience methods
            assertThat(code).contains("Money zeroMoney");
            assertThat(code).contains("Money createMoney");
        }

        @Test
        @DisplayName("uses List.of() for SUPPORTED_VERSIONS")
        void usesListOf() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("List.of(\"v1\", \"v2\")");
        }

        @Test
        @DisplayName("uses private static createContexts method")
        void usesPrivateStaticMethod() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("private static Map<String, VersionContext> createContexts()");
            assertThat(code).contains("CONTEXTS = createContexts()");
        }

        @Test
        @DisplayName("does not require helper class")
        void doesNotRequireHelperClass() {
            VersionContextInterfaceComposer composer = new VersionContextInterfaceComposer(config, schema);
            assertThat(composer.requiresHelperClass()).isFalse();
        }
    }

    @Nested
    @DisplayName("Java 8 Generation")
    class Java8GenerationTest {

        @Test
        @DisplayName("generates complete interface with all components")
        void generatesCompleteInterface() {
            JavaFile javaFile = new VersionContextInterfaceComposer(java8Config, schema)
                    .addStaticFields()
                    .addStaticMethods()
                    .addInstanceMethods()
                    .addWrapMethods()
                    .addBuilderMethods()
                    .addConvenienceMethods()
                    .build();

            String code = javaFile.toString();

            // All components should be present
            assertThat(code).contains("Map<String, VersionContext> CONTEXTS");
            assertThat(code).contains("static VersionContext forVersionId");
            assertThat(code).contains("String getVersionId");
            assertThat(code).contains("Money wrapMoney");
            assertThat(code).contains("Money.Builder newMoneyBuilder");
            assertThat(code).contains("Money zeroMoney");
        }

        @Test
        @DisplayName("uses Collections.unmodifiableList for SUPPORTED_VERSIONS")
        void usesCollectionsUnmodifiableList() {
            JavaFile javaFile = new VersionContextInterfaceComposer(java8Config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("Collections.unmodifiableList");
            assertThat(code).contains("Arrays.asList");
            assertThat(code).doesNotContain("List.of");
        }

        @Test
        @DisplayName("uses VersionContextHelper for CONTEXTS")
        void usesVersionContextHelper() {
            JavaFile javaFile = new VersionContextInterfaceComposer(java8Config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("VersionContextHelper.createContexts()");
            assertThat(code).doesNotContain("private static Map<String, VersionContext> createContexts()");
        }

        @Test
        @DisplayName("requires helper class")
        void requiresHelperClass() {
            VersionContextInterfaceComposer composer = new VersionContextInterfaceComposer(java8Config, schema);
            assertThat(composer.requiresHelperClass()).isTrue();
        }
    }

    @Nested
    @DisplayName("Selective Component Addition")
    class SelectiveComponentTest {

        @Test
        @DisplayName("can add only static fields")
        void canAddOnlyStaticFields() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("CONTEXTS");
            assertThat(code).contains("SUPPORTED_VERSIONS");
            // Note: forVersionId appears in javadoc example, check for method declaration instead
            assertThat(code).doesNotContain("static VersionContext forVersionId");
            assertThat(code).doesNotContain("wrapMoney");
        }

        @Test
        @DisplayName("can add only static methods")
        void canAddOnlyStaticMethods() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticMethods()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("forVersionId");
            assertThat(code).contains("find");
            assertThat(code).doesNotContain("wrapMoney");
        }

        @Test
        @DisplayName("can skip builder methods")
        void canSkipBuilderMethods() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .addStaticMethods()
                    .addInstanceMethods()
                    .addWrapMethods()
                    // Skip builder methods
                    .addConvenienceMethods()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("wrapMoney");
            // Builder methods not added by composer
            // But convenience methods depend on builders being enabled in config
            // Since config has generateBuilders=true, convenience methods will try to reference builders
        }
    }

    @Nested
    @DisplayName("Custom Components")
    class CustomComponentTest {

        @Test
        @DisplayName("supports adding custom component")
        void supportsCustomComponent() {
            InterfaceComponent customComponent = builder ->
                    builder.addMethod(com.squareup.javapoet.MethodSpec.methodBuilder("customMethod")
                            .addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                                    javax.lang.model.element.Modifier.DEFAULT)
                            .returns(void.class)
                            .addStatement("// custom implementation")
                            .build());

            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .addComponent(customComponent)
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("default void customMethod()");
            assertThat(code).contains("// custom implementation");
        }
    }

    @Nested
    @DisplayName("Output Format")
    class OutputFormatTest {

        @Test
        @DisplayName("generates valid Java file with package")
        void generatesValidJavaFile() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            assertThat(javaFile.packageName).isEqualTo("org.example.api");
            assertThat(javaFile.typeSpec.name).isEqualTo("VersionContext");
        }

        @Test
        @DisplayName("includes file comment")
        void includesFileComment() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("// Generated by proto-wrapper-maven-plugin");
        }

        @Test
        @DisplayName("uses 4-space indentation")
        void uses4SpaceIndentation() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            // Methods and fields should have 4-space indentation
            assertThat(code).contains("    Map<String, VersionContext> CONTEXTS");
        }

        @Test
        @DisplayName("generates interface with public modifier")
        void generatesPublicInterface() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("public interface VersionContext");
        }

        @Test
        @DisplayName("includes javadoc")
        void includesJavadoc() {
            JavaFile javaFile = new VersionContextInterfaceComposer(config, schema)
                    .addStaticFields()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("Version context for creating version-specific wrapper instances");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("handles schema with single version")
        void handlesSingleVersion() {
            MergedSchema singleVersionSchema = new MergedSchema(Arrays.asList("v1"));
            MergedMessage money = new MergedMessage("Money");
            money.addVersion("v1");
            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(amountProto)).build());
            singleVersionSchema.addMessage(money);

            JavaFile javaFile = new VersionContextInterfaceComposer(config, singleVersionSchema)
                    .addStaticFields()
                    .addStaticMethods()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("List.of(\"v1\")");
            assertThat(code).contains("DEFAULT_VERSION = \"v1\"");
        }

        @Test
        @DisplayName("handles schema with many versions")
        void handlesManyVersions() {
            MergedSchema manyVersionsSchema = new MergedSchema(
                    Arrays.asList("v1", "v2", "v3", "v4", "v5"));
            MergedMessage money = new MergedMessage("Money");
            for (String v : manyVersionsSchema.getVersions()) {
                money.addVersion(v);
            }
            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(amountProto)).build());
            manyVersionsSchema.addMessage(money);

            JavaFile javaFile = new VersionContextInterfaceComposer(config, manyVersionsSchema)
                    .addStaticFields()
                    .addStaticMethods()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("\"v1\", \"v2\", \"v3\", \"v4\", \"v5\"");
            assertThat(code).contains("DEFAULT_VERSION = \"v5\"");
        }

        @Test
        @DisplayName("handles non-numeric version names")
        void handlesNonNumericVersionNames() {
            MergedSchema customSchema = new MergedSchema(Arrays.asList("legacy", "current", "beta"));
            MergedMessage money = new MergedMessage("Money");
            for (String v : customSchema.getVersions()) {
                money.addVersion(v);
            }
            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            money.addField(MergedField.builder().addVersionField("legacy", new FieldInfo(amountProto)).build());
            customSchema.addMessage(money);

            JavaFile javaFile = new VersionContextInterfaceComposer(config, customSchema)
                    .addStaticFields()
                    .addStaticMethods()
                    .build();

            String code = javaFile.toString();
            assertThat(code).contains("\"legacy\", \"current\", \"beta\"");
            assertThat(code).contains("DEFAULT_VERSION = \"beta\"");
        }
    }
}
