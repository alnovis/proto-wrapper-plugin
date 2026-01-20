package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for VersionContextGenerator.
 *
 * <p>Verifies that the generator produces correct VersionContext interface,
 * implementations, and factory classes with proper API structure.</p>
 */
@DisplayName("VersionContextGenerator Tests")
class VersionContextGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private VersionContextGenerator generator;
    private MergedSchema schema;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .generateBuilders(true)
                .generateVersionContext(true)
                .build();
        generator = new VersionContextGenerator(config);

        // Create schema with two versions
        schema = new MergedSchema(Arrays.asList("v1", "v2"));

        // Add a Money message
        MergedMessage money = new MergedMessage("Money");
        money.addVersion("v1");
        money.addVersion("v2");

        FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                .setName("amount")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(amountProto)).build());

        FieldDescriptorProto currencyProto = FieldDescriptorProto.newBuilder()
                .setName("currency")
                .setNumber(2)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(currencyProto)).build());

        schema.addMessage(money);
    }

    @Nested
    @DisplayName("Interface Generation")
    class InterfaceGenerationTest {

        @Test
        @DisplayName("generates VersionContext interface with forVersionId method")
        void generatesForVersionIdMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("public interface VersionContext");
            assertThat(code).contains("static VersionContext forVersionId(String versionId)");
            // Now uses CONTEXTS map instead of switch
            assertThat(code).contains("CONTEXTS.get(versionId)");
        }

        @Test
        @DisplayName("generates getVersionId method")
        void generatesGetVersionIdMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("String getVersionId()");
        }

        @Test
        @DisplayName("does not generate deprecated forVersion method")
        void doesNotGenerateDeprecatedForVersionMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // Deprecated forVersion(int) should NOT be generated
            assertThat(code).doesNotContain("static VersionContext forVersion(int version)");
        }

        @Test
        @DisplayName("does not generate deprecated getVersion method")
        void doesNotGenerateDeprecatedGetVersionMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // Deprecated getVersion() should NOT be generated
            assertThat(code).doesNotContain("int getVersion()");
        }

        @Test
        @DisplayName("generates wrap methods for messages")
        void generatesWrapMethods() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("Money wrapMoney(Message proto)");
        }

        @Test
        @DisplayName("generates newBuilder methods when builders enabled")
        void generatesNewBuilderMethods() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("Money.Builder newMoneyBuilder()");
        }

        @Test
        @DisplayName("generates parseFromBytes methods")
        void generatesParseFromBytesMethods() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("Money parseMoneyFromBytes(byte[] bytes)");
            assertThat(code).contains("throws InvalidProtocolBufferException");
        }

        @Test
        @DisplayName("generates null/invalid version check for forVersionId")
        void generatesNullCheckForForVersionId() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // Now checks if ctx is null (handles both null versionId and unsupported versions)
            assertThat(code).contains("if (ctx == null)");
            assertThat(code).contains("throw new IllegalArgumentException");
            assertThat(code).contains("Unsupported version");
        }
    }

    @Nested
    @DisplayName("Implementation Generation")
    class ImplementationGenerationTest {

        @Test
        @DisplayName("generates V1 implementation with correct getVersionId")
        void generatesV1Implementation() {
            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.v1.Common.Money");

            JavaFile javaFile = generator.generateImpl(schema, "v1", protoMappings);
            String code = javaFile.toString();

            assertThat(code).contains("public final class VersionContextV1 implements VersionContext");
            assertThat(code).contains("public String getVersionId()");
            assertThat(code).contains("return \"v1\"");
        }

        @Test
        @DisplayName("generates V2 implementation with correct getVersionId")
        void generatesV2Implementation() {
            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.v2.Common.Money");

            JavaFile javaFile = generator.generateImpl(schema, "v2", protoMappings);
            String code = javaFile.toString();

            assertThat(code).contains("public final class VersionContextV2 implements VersionContext");
            assertThat(code).contains("return \"v2\"");
        }

        @Test
        @DisplayName("generates singleton pattern")
        void generatesSingletonPattern() {
            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.v1.Common.Money");

            JavaFile javaFile = generator.generateImpl(schema, "v1", protoMappings);
            String code = javaFile.toString();

            assertThat(code).contains("public static final VersionContextV1 INSTANCE = new VersionContextV1()");
            assertThat(code).contains("private VersionContextV1()");
        }

        @Test
        @DisplayName("does not generate deprecated getVersion in implementation")
        void doesNotGenerateDeprecatedGetVersionInImpl() {
            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.v1.Common.Money");

            JavaFile javaFile = generator.generateImpl(schema, "v1", protoMappings);
            String code = javaFile.toString();

            // Should have getVersionId(), not deprecated getVersion()
            assertThat(code).contains("public String getVersionId()");
            assertThat(code).contains("return \"v1\"");
            assertThat(code).doesNotContain("public int getVersion()");
        }

        @Test
        @DisplayName("generates wrap methods in implementation")
        void generatesWrapMethodsInImpl() {
            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.v1.Common.Money");

            JavaFile javaFile = generator.generateImpl(schema, "v1", protoMappings);
            String code = javaFile.toString();

            // The generated code uses short class name Money for return type
            assertThat(code).contains("Money wrapMoney(Message proto)");
            assertThat(code).contains("if (proto == null)");
            assertThat(code).contains("return null");
        }
    }

    @Nested
    @DisplayName("Static Factory Methods on Interface")
    class StaticFactoryMethodsTest {

        @Test
        @DisplayName("generates find method returning Optional")
        void generatesFindMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("static Optional<VersionContext> find(String versionId)");
            assertThat(code).contains("Optional.ofNullable(CONTEXTS.get(versionId))");
        }

        @Test
        @DisplayName("generates getDefault method")
        void generatesGetDefaultMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("static VersionContext getDefault()");
            assertThat(code).contains("CONTEXTS.get(DEFAULT_VERSION)");
        }

        @Test
        @DisplayName("generates supportedVersions method")
        void generatesSupportedVersionsMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("static List<String> supportedVersions()");
            assertThat(code).contains("return SUPPORTED_VERSIONS");
        }

        @Test
        @DisplayName("generates defaultVersion method")
        void generatesDefaultVersionMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("static String defaultVersion()");
            assertThat(code).contains("return DEFAULT_VERSION");
        }

        @Test
        @DisplayName("generates isSupported method")
        void generatesIsSupportedMethod() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            assertThat(code).contains("static boolean isSupported(String versionId)");
            assertThat(code).contains("CONTEXTS.containsKey(versionId)");
        }

        @Test
        @DisplayName("uses latest version as default")
        void usesLatestVersionAsDefault() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // v2 should be the default (last in list)
            assertThat(code).contains("DEFAULT_VERSION = \"v2\"");
        }

        @Test
        @DisplayName("generates static CONTEXTS map with createContexts method")
        void generatesContextsMap() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // CONTEXTS field with inline initialization
            assertThat(code).contains("Map<String, VersionContext> CONTEXTS = createContexts()");
            // Private static helper method
            assertThat(code).contains("private static Map<String, VersionContext> createContexts()");
            assertThat(code).contains("map.put(\"v1\"");
            assertThat(code).contains("map.put(\"v2\"");
        }

        @Test
        @DisplayName("generates SUPPORTED_VERSIONS list")
        void generatesSupportedVersionsList() {
            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // Inline initialization
            assertThat(code).contains("List<String> SUPPORTED_VERSIONS = List.of(\"v1\", \"v2\")");
        }
    }

    @Nested
    @DisplayName("Version-specific Messages")
    class VersionSpecificMessagesTest {

        @Test
        @DisplayName("generates default method for v2-only message in interface")
        void generatesDefaultMethodForV2OnlyMessage() {
            // Add a v2-only message
            MergedMessage calibration = new MergedMessage("CalibrationInfo");
            calibration.addVersion("v2"); // Only in v2

            FieldDescriptorProto timestampProto = FieldDescriptorProto.newBuilder()
                    .setName("timestamp")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_REQUIRED)
                    .build();
            calibration.addField(MergedField.builder().addVersionField("v2", new FieldInfo(timestampProto)).build());

            schema.addMessage(calibration);

            JavaFile javaFile = generator.generateInterface(schema);
            String code = javaFile.toString();

            // Should have default method that throws UnsupportedOperationException
            assertThat(code).contains("default CalibrationInfo wrapCalibrationInfo");
            assertThat(code).contains("throw new UnsupportedOperationException");
            assertThat(code).contains("Present only in versions: [v2]");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("handles single version schema")
        void handlesSingleVersionSchema() {
            MergedSchema singleVersionSchema = new MergedSchema(Arrays.asList("v1"));

            MergedMessage money = new MergedMessage("Money");
            money.addVersion("v1");

            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_REQUIRED)
                    .build();
            money.addField(MergedField.builder().addVersionField("v1", new FieldInfo(amountProto)).build());

            singleVersionSchema.addMessage(money);

            JavaFile javaFile = generator.generateInterface(singleVersionSchema);
            String code = javaFile.toString();

            // Uses createContexts method
            assertThat(code).contains("map.put(\"v1\"");
            assertThat(code).doesNotContain("map.put(\"v2\"");
            assertThat(code).contains("SUPPORTED_VERSIONS = List.of(\"v1\")");
        }

        @Test
        @DisplayName("handles non-numeric version names")
        void handlesNonNumericVersionNames() {
            MergedSchema customSchema = new MergedSchema(Arrays.asList("legacy", "current"));

            MergedMessage money = new MergedMessage("Money");
            money.addVersion("legacy");
            money.addVersion("current");

            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_REQUIRED)
                    .build();
            money.addField(MergedField.builder().addVersionField("legacy", new FieldInfo(amountProto)).build());

            customSchema.addMessage(money);

            JavaFile javaFile = generator.generateInterface(customSchema);
            String code = javaFile.toString();

            // Uses createContexts method
            assertThat(code).contains("map.put(\"legacy\"");
            assertThat(code).contains("map.put(\"current\"");
            assertThat(code).contains("forVersionId(String versionId)");
        }

        @Test
        @DisplayName("getVersionId returns original version string for non-numeric version")
        void getVersionIdReturnsOriginalStringForNonNumericVersion() {
            MergedSchema customSchema = new MergedSchema(Arrays.asList("legacy"));

            MergedMessage money = new MergedMessage("Money");
            money.addVersion("legacy");

            FieldDescriptorProto amountProto = FieldDescriptorProto.newBuilder()
                    .setName("amount")
                    .setNumber(1)
                    .setType(Type.TYPE_INT64)
                    .setLabel(Label.LABEL_REQUIRED)
                    .build();
            money.addField(MergedField.builder().addVersionField("legacy", new FieldInfo(amountProto)).build());

            customSchema.addMessage(money);

            Map<String, String> protoMappings = new HashMap<>();
            protoMappings.put("Money", "org.example.proto.legacy.Common.Money");

            JavaFile javaFile = generator.generateImpl(customSchema, "legacy", protoMappings);
            String code = javaFile.toString();

            // getVersionId() returns the original version string
            assertThat(code).contains("return \"legacy\"");
        }
    }
}
