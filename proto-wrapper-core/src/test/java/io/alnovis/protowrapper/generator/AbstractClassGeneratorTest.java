package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import java.nio.file.Path;
import java.util.Arrays;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AbstractClassGenerator.
 *
 * <p>Verifies that generated abstract classes have correct implementations
 * for common methods like toString(), equals(), hashCode(), and version accessors.</p>
 */
@DisplayName("AbstractClassGenerator Tests")
class AbstractClassGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private AbstractClassGenerator generator;
    private MergedSchema schema;
    private GenerationContext ctx;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .implPackagePattern("org.example.impl.{version}")
                .targetJavaVersion(9)
                .build();
        generator = new AbstractClassGenerator(config);
        schema = new MergedSchema(Arrays.asList("v1", "v2"));
        ctx = GenerationContext.create(schema, config);
    }

    private MergedMessage createSimpleMoneyMessage() {
        MergedMessage message = new MergedMessage("Money");
        message.addVersion("v1");
        message.addVersion("v2");

        FieldDescriptorProto billsProto = FieldDescriptorProto.newBuilder()
                .setName("bills")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(billsProto)).build());

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(MergedField.builder().addVersionField("v1", new FieldInfo(coinsProto)).build());

        return message;
    }

    @Nested
    @DisplayName("toString() generation")
    class ToStringTests {

        @Test
        @DisplayName("uses getWrapperVersionId() instead of getWrapperVersion()")
        void usesStringVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            assertThat(code).contains("getWrapperVersionId()");
            assertThat(code).contains("toString()");
            // Should use %s for version (string), not %d (int)
            assertThat(code).contains("\"%s[version=%s] %s\"");
        }

        @Test
        @DisplayName("does not use deprecated getWrapperVersion() in toString")
        void doesNotUseDeprecatedVersionInToString() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Find the toString method and verify it doesn't call getWrapperVersion()
            int toStringStart = code.indexOf("public String toString()");
            int toStringEnd = code.indexOf("}", toStringStart);
            String toStringMethod = code.substring(toStringStart, toStringEnd);

            assertThat(toStringMethod).contains("getWrapperVersionId()");
            assertThat(toStringMethod).doesNotContain("getWrapperVersion()");
        }
    }

    @Nested
    @DisplayName("equals() generation")
    class EqualsTests {

        @Test
        @DisplayName("compares using getWrapperVersionId()")
        void comparesUsingStringVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Find the equals method
            int equalsStart = code.indexOf("public boolean equals(Object obj)");
            int equalsEnd = code.indexOf("}", equalsStart);
            String equalsMethod = code.substring(equalsStart, equalsEnd);

            // Should use Objects.equals for string comparison
            assertThat(equalsMethod).contains("Objects.equals(this.getWrapperVersionId(), other.getWrapperVersionId())");
        }

        @Test
        @DisplayName("does not use deprecated getWrapperVersion() == comparison")
        void doesNotUseIntComparison() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Find the equals method
            int equalsStart = code.indexOf("public boolean equals(Object obj)");
            int equalsEnd = code.indexOf("}", equalsStart);
            String equalsMethod = code.substring(equalsStart, equalsEnd);

            // Should NOT use == comparison for version
            assertThat(equalsMethod).doesNotContain("getWrapperVersion() ==");
            assertThat(equalsMethod).doesNotContain("== other.getWrapperVersion()");
        }
    }

    @Nested
    @DisplayName("hashCode() generation")
    class HashCodeTests {

        @Test
        @DisplayName("uses getWrapperVersionId()")
        void usesStringVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Find the hashCode method
            int hashCodeStart = code.indexOf("public int hashCode()");
            int hashCodeEnd = code.indexOf("}", hashCodeStart);
            String hashCodeMethod = code.substring(hashCodeStart, hashCodeEnd);

            assertThat(hashCodeMethod).contains("getWrapperVersionId()");
        }

        @Test
        @DisplayName("does not use deprecated getWrapperVersion()")
        void doesNotUseDeprecatedVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Find the hashCode method
            int hashCodeStart = code.indexOf("public int hashCode()");
            int hashCodeEnd = code.indexOf("}", hashCodeStart);
            String hashCodeMethod = code.substring(hashCodeStart, hashCodeEnd);

            assertThat(hashCodeMethod).doesNotContain("getWrapperVersion()");
        }

        @Test
        @DisplayName("uses Objects.hash() with versionId and proto")
        void usesObjectsHash() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            assertThat(code).contains("Objects.hash(getWrapperVersionId(), proto)");
        }
    }

    @Nested
    @DisplayName("Version accessor methods")
    class VersionAccessorTests {

        @Test
        @DisplayName("generates getWrapperVersionId() implementation")
        void generatesWrapperVersionIdImpl() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            assertThat(code).contains("public final String getWrapperVersionId()");
            assertThat(code).contains("return extractWrapperVersionId(proto)");
        }

        @Test
        @DisplayName("generates abstract extractWrapperVersionId() method")
        void generatesExtractWrapperVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            assertThat(code).contains("protected abstract String extractWrapperVersionId(PROTO proto)");
        }

        @Test
        @DisplayName("does not generate deprecated getWrapperVersion() method")
        void doesNotGenerateDeprecatedWrapperVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Deprecated method should NOT be generated
            assertThat(code).doesNotContain("public final int getWrapperVersion()");
            assertThat(code).doesNotContain("extractWrapperVersion(proto)");
        }
    }

    @Nested
    @DisplayName("Error messages in conversion methods")
    class ConversionErrorMessageTests {

        @Test
        @DisplayName("asVersionStrict error uses getWrapperVersionId()")
        void asVersionStrictUsesStringVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Error message should use string format for both source and target version
            assertThat(code).contains("\"Cannot convert from version %s to version %d:");
            assertThat(code).contains("getWrapperVersionId(), targetVersion");
        }

        @Test
        @DisplayName("convertToVersion error uses string versions")
        void convertToVersionUsesStringVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message, ctx);
            String code = javaFile.toString();

            // Error message should use string format for version identifiers
            assertThat(code).contains("\"Failed to convert %s from version %s to %s:");
        }
    }

    @Nested
    @DisplayName("AbstractBuilder generation")
    class AbstractBuilderTests {

        @Test
        @DisplayName("generates getVersionId() abstract method")
        void generatesGetVersionId() {
            GeneratorConfig builderConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .implPackagePattern("org.example.impl.{version}")
                    .targetJavaVersion(9)
                    .generateBuilders(true)
                    .build();
            AbstractClassGenerator builderGenerator = new AbstractClassGenerator(builderConfig);
            GenerationContext builderCtx = GenerationContext.create(schema, builderConfig);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = builderGenerator.generate(message, builderCtx);
            String code = javaFile.toString();

            assertThat(code).contains("protected abstract String getVersionId()");
        }

        @Test
        @DisplayName("does not generate deprecated getVersion() method")
        void doesNotGenerateDeprecatedGetVersion() {
            GeneratorConfig builderConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .implPackagePattern("org.example.impl.{version}")
                    .targetJavaVersion(9)
                    .generateBuilders(true)
                    .build();
            AbstractClassGenerator builderGenerator = new AbstractClassGenerator(builderConfig);
            GenerationContext builderCtx = GenerationContext.create(schema, builderConfig);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = builderGenerator.generate(message, builderCtx);
            String code = javaFile.toString();

            // Find AbstractBuilder class (JavaPoet generates "public abstract static class")
            int builderStart = code.indexOf("public abstract static class AbstractBuilder");
            assertThat(builderStart)
                    .as("AbstractBuilder class should exist")
                    .isGreaterThan(0);

            // Extract builder class code (find next class or end of file)
            int builderEnd = code.indexOf("public abstract static class", builderStart + 1);
            if (builderEnd == -1) {
                builderEnd = code.length();
            }
            String builderCode = code.substring(builderStart, builderEnd);

            // Verify deprecated getVersion() is NOT generated (removed in favor of getVersionId)
            assertThat(builderCode).doesNotContain("protected abstract int getVersion()");
            // Verify getVersionId() is generated
            assertThat(builderCode).contains("protected abstract String getVersionId()");
        }
    }

    @Nested
    @DisplayName("Java 8 compatibility")
    class Java8CompatibilityTests {

        @Test
        @DisplayName("generates correctly for Java 8")
        void generatesForJava8() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .implPackagePattern("org.example.impl.{version}")
                    .targetJavaVersion(8)
                    .build();
            AbstractClassGenerator java8Generator = new AbstractClassGenerator(java8Config);
            GenerationContext java8Ctx = GenerationContext.create(schema, java8Config);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = java8Generator.generate(message, java8Ctx);
            String code = javaFile.toString();

            // Basic generation should work for Java 8
            assertThat(code).contains("public abstract class AbstractMoney");
            assertThat(code).contains("getWrapperVersionId()");
        }
    }
}
