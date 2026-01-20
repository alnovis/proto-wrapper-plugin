package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ProtocolVersionsGenerator.
 *
 * <p>Verifies that the generator produces correct ProtocolVersions class
 * with version constants, utilities, and correct Java version compatibility.</p>
 *
 * @since 2.1.0
 */
@DisplayName("ProtocolVersionsGenerator Tests")
class ProtocolVersionsGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private List<String> versions;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .generateProtocolVersions(true)
                .build();
        versions = Arrays.asList("v1", "v2");
    }

    @Nested
    @DisplayName("Basic Generation")
    class BasicGenerationTest {

        @Test
        @DisplayName("generates ProtocolVersions class with constants")
        void generatesClassWithConstants() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Package and class declaration
            assertThat(code).contains("package org.example.api;");
            assertThat(code).contains("public final class ProtocolVersions");

            // Version constants
            assertThat(code).contains("public static final String V1 = \"v1\";");
            assertThat(code).contains("public static final String V2 = \"v2\";");
        }

        @Test
        @DisplayName("generates private constructor")
        void generatesPrivateConstructor() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private ProtocolVersions()");
        }

        @Test
        @DisplayName("generates SUPPORTED set")
        void generatesSupportedSet() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private static final Set<String> SUPPORTED");
        }

        @Test
        @DisplayName("generates supported() method")
        void generatesSupportedMethod() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static Set<String> supported()");
            assertThat(code).contains("return SUPPORTED;");
        }

        @Test
        @DisplayName("generates isSupported() method")
        void generatesIsSupportedMethod() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static boolean isSupported(String versionId)");
            assertThat(code).contains("return SUPPORTED.contains(versionId);");
        }

        @Test
        @DisplayName("generates requireSupported() method")
        void generatesRequireSupportedMethod() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static String requireSupported(String versionId)");
            assertThat(code).contains("throw new IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("Java Version Compatibility")
    class JavaVersionCompatibilityTest {

        @Test
        @DisplayName("generates Java 9+ code when targetJavaVersion >= 9")
        void generatesJava9PlusCode() {
            GeneratorConfig java17Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(17)
                    .generateProtocolVersions(true)
                    .build();

            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(java17Config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Java 9+ Set.of()
            assertThat(code).contains("Set.of(V1, V2)");
            assertThat(code).doesNotContain("Collections.unmodifiableSet");
        }

        @Test
        @DisplayName("generates Java 8 compatible code when targetJavaVersion is 8")
        void generatesJava8CompatibleCode() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(8)
                    .generateProtocolVersions(true)
                    .build();

            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(java8Config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Java 8 compatible
            assertThat(code).contains("Collections.unmodifiableSet");
            assertThat(code).contains("new HashSet<>");
            assertThat(code).contains("Arrays.asList");
            assertThat(code).doesNotContain("Set.of");
        }
    }

    @Nested
    @DisplayName("Custom Version Names")
    class CustomVersionNamesTest {

        @Test
        @DisplayName("generates constants for numeric versions")
        void generatesConstantsForNumericVersions() {
            List<String> numericVersions = Arrays.asList("v10", "v20");
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, numericVersions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static final String V10 = \"v10\";");
            assertThat(code).contains("public static final String V20 = \"v20\";");
        }

        @Test
        @DisplayName("generates constants for custom names")
        void generatesConstantsForCustomNames() {
            List<String> customVersions = Arrays.asList("legacy", "current");
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, customVersions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static final String LEGACY = \"legacy\";");
            assertThat(code).contains("public static final String CURRENT = \"current\";");
        }

        @Test
        @DisplayName("handles single version")
        void handlesSingleVersion() {
            List<String> singleVersion = Arrays.asList("v1");
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, singleVersion);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static final String V1 = \"v1\";");
            assertThat(code).contains("V1)"); // In SUPPORTED set
        }
    }

    @Nested
    @DisplayName("JavaDoc")
    class JavaDocTest {

        @Test
        @DisplayName("includes class-level javadoc")
        void includesClassJavadoc() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Protocol version constants and utilities");
            assertThat(code).contains("@since 2.1.0");
        }

        @Test
        @DisplayName("includes constant javadoc")
        void includesConstantJavadoc() {
            ProtocolVersionsGenerator generator = new ProtocolVersionsGenerator(config, versions);
            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("Version identifier: v1");
            assertThat(code).contains("Version identifier: v2");
        }
    }
}
