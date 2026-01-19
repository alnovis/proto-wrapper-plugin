package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

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
        generator.setSchema(schema);
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
        message.addField(new MergedField(new FieldInfo(billsProto), "v1"));

        FieldDescriptorProto coinsProto = FieldDescriptorProto.newBuilder()
                .setName("coins")
                .setNumber(2)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_REQUIRED)
                .build();
        message.addField(new MergedField(new FieldInfo(coinsProto), "v1"));

        return message;
    }

    @Nested
    @DisplayName("toString() generation")
    class ToStringTests {

        @Test
        @DisplayName("uses getWrapperVersionId() instead of getWrapperVersion()")
        void usesStringVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
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
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            assertThat(code).contains("public final String getWrapperVersionId()");
            assertThat(code).contains("return extractWrapperVersionId(proto)");
        }

        @Test
        @DisplayName("generates abstract extractWrapperVersionId() method")
        void generatesExtractWrapperVersionId() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            assertThat(code).contains("protected abstract String extractWrapperVersionId(PROTO proto)");
        }

        @Test
        @DisplayName("generates deprecated getWrapperVersion() with annotation")
        void generatesDeprecatedWrapperVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            // For Java 9+, should have full @Deprecated annotation (JavaPoet formats with line breaks)
            assertThat(code).contains("@Deprecated(");
            assertThat(code).contains("since = \"1.6.9\"");
            assertThat(code).contains("forRemoval = true");
            assertThat(code).contains("public final int getWrapperVersion()");
        }
    }

    @Nested
    @DisplayName("Error messages in conversion methods")
    class ConversionErrorMessageTests {

        @Test
        @DisplayName("asVersionStrict error uses getWrapperVersionId()")
        void asVersionStrictUsesStringVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            // Error message should use string format for source version
            assertThat(code).contains("\"Cannot convert from version %s to version %d:");
            assertThat(code).contains("getWrapperVersionId(), targetVersion");
        }

        @Test
        @DisplayName("convertToVersion error uses getWrapperVersionId()")
        void convertToVersionUsesStringVersion() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            // Error message should use string format for source version
            assertThat(code).contains("\"Failed to convert %s from version %s to version %d:");
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
            builderGenerator.setSchema(schema);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = builderGenerator.generate(message);
            String code = javaFile.toString();

            assertThat(code).contains("protected abstract String getVersionId()");
        }

        @Test
        @DisplayName("generates deprecated getVersion() with annotation")
        void generatesDeprecatedGetVersion() {
            GeneratorConfig builderConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .implPackagePattern("org.example.impl.{version}")
                    .targetJavaVersion(9)
                    .generateBuilders(true)
                    .build();
            AbstractClassGenerator builderGenerator = new AbstractClassGenerator(builderConfig);
            builderGenerator.setSchema(schema);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = builderGenerator.generate(message);
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

            assertThat(builderCode).contains("protected abstract int getVersion()");

            // Check that @Deprecated annotation is present somewhere before getVersion
            // The annotation is on a separate line, so just check both exist in builder
            assertThat(builderCode).contains("@Deprecated");
            assertThat(builderCode).contains("since = \"1.6.9\"");
        }
    }

    @Nested
    @DisplayName("Java 8 compatibility")
    class Java8CompatibilityTests {

        @Test
        @DisplayName("Java 8 uses simple @Deprecated annotation")
        void java8UsesSimpleDeprecated() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .implPackagePattern("org.example.impl.{version}")
                    .targetJavaVersion(8)
                    .build();
            AbstractClassGenerator java8Generator = new AbstractClassGenerator(java8Config);
            java8Generator.setSchema(schema);

            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = java8Generator.generate(message);
            String code = javaFile.toString();

            // Java 8 should have simple @Deprecated without parameters
            assertThat(code).contains("@Deprecated");
            assertThat(code).doesNotContain("@Deprecated(since");
        }

        @Test
        @DisplayName("Java 9+ uses @Deprecated with since and forRemoval")
        void java9UsesFullDeprecated() {
            MergedMessage message = createSimpleMoneyMessage();
            JavaFile javaFile = generator.generate(message);
            String code = javaFile.toString();

            // JavaPoet formats annotation with line breaks
            assertThat(code).contains("@Deprecated(");
            assertThat(code).contains("since = \"1.6.9\"");
            assertThat(code).contains("forRemoval = true");
        }
    }
}
