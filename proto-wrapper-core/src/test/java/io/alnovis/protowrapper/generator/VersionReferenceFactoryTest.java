package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for VersionReferenceFactory.
 *
 * <p>Verifies that the factory correctly generates version references
 * based on configuration (ProtocolVersions enabled or disabled).</p>
 *
 * @since 2.1.0
 */
@DisplayName("VersionReferenceFactory Tests")
class VersionReferenceFactoryTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("With ProtocolVersions Enabled")
    class WithProtocolVersionsEnabled {

        private VersionReferenceFactory factory;

        @BeforeEach
        void setUp() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .generateProtocolVersions(true)
                    .build();
            factory = VersionReferenceFactory.create(config);
        }

        @Test
        @DisplayName("useConstants() returns true")
        void useConstantsReturnsTrue() {
            assertThat(factory.useConstants()).isTrue();
        }

        @Test
        @DisplayName("getProtocolVersionsClass() returns ClassName")
        void getProtocolVersionsClassReturnsClassName() {
            assertThat(factory.getProtocolVersionsClass())
                    .isEqualTo(ClassName.get("org.example.api", "ProtocolVersions"));
        }

        @Test
        @DisplayName("addReturnStatement() uses ProtocolVersions constant")
        void addReturnStatementUsesConstant() {
            MethodSpec.Builder method = MethodSpec.methodBuilder("test")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class);

            factory.addReturnStatement(method, "v1");

            String code = method.build().toString();
            // JavaPoet uses fully qualified names in toString()
            assertThat(code).contains("ProtocolVersions.V1");
            assertThat(code).doesNotContain("\"v1\"");
        }

        @Test
        @DisplayName("addMapPut() uses ProtocolVersions constant")
        void addMapPutUsesConstant() {
            MethodSpec.Builder method = MethodSpec.methodBuilder("test")
                    .addModifiers(Modifier.PUBLIC);

            ClassName valueType = ClassName.get("org.example", "VersionContextV1");
            factory.addMapPut(method, "v1", valueType, "INSTANCE");

            String code = method.build().toString();
            // JavaPoet uses fully qualified names in toString()
            assertThat(code).contains("ProtocolVersions.V1");
            assertThat(code).contains("VersionContextV1.INSTANCE");
        }

        @Test
        @DisplayName("fieldInitializer() returns ProtocolVersions reference")
        void fieldInitializerReturnsConstant() {
            CodeBlock initializer = factory.fieldInitializer("v2");

            assertThat(initializer.toString()).contains("ProtocolVersions.V2");
        }

        @Test
        @DisplayName("formatForList() returns ProtocolVersions reference")
        void formatForListReturnsConstant() {
            String formatted = factory.formatForList("v1");

            assertThat(formatted).isEqualTo("ProtocolVersions.V1");
        }

        @Test
        @DisplayName("formatVersionsList() returns comma-separated constants")
        void formatVersionsListReturnsConstants() {
            List<String> versions = Arrays.asList("v1", "v2", "v3");

            String formatted = factory.formatVersionsList(versions);

            assertThat(formatted).isEqualTo("ProtocolVersions.V1, ProtocolVersions.V2, ProtocolVersions.V3");
        }
    }

    @Nested
    @DisplayName("With ProtocolVersions Disabled")
    class WithProtocolVersionsDisabled {

        private VersionReferenceFactory factory;

        @BeforeEach
        void setUp() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .generateProtocolVersions(false)
                    .build();
            factory = VersionReferenceFactory.create(config);
        }

        @Test
        @DisplayName("useConstants() returns false")
        void useConstantsReturnsFalse() {
            assertThat(factory.useConstants()).isFalse();
        }

        @Test
        @DisplayName("getProtocolVersionsClass() returns null")
        void getProtocolVersionsClassReturnsNull() {
            assertThat(factory.getProtocolVersionsClass()).isNull();
        }

        @Test
        @DisplayName("addReturnStatement() uses string literal")
        void addReturnStatementUsesLiteral() {
            MethodSpec.Builder method = MethodSpec.methodBuilder("test")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class);

            factory.addReturnStatement(method, "v1");

            String code = method.build().toString();
            assertThat(code).contains("return \"v1\"");
            assertThat(code).doesNotContain("ProtocolVersions");
        }

        @Test
        @DisplayName("addMapPut() uses string literal")
        void addMapPutUsesLiteral() {
            MethodSpec.Builder method = MethodSpec.methodBuilder("test")
                    .addModifiers(Modifier.PUBLIC);

            ClassName valueType = ClassName.get("org.example", "VersionContextV1");
            factory.addMapPut(method, "v1", valueType, "INSTANCE");

            String code = method.build().toString();
            // JavaPoet uses fully qualified names in toString()
            assertThat(code).contains("map.put(\"v1\"");
            assertThat(code).contains("VersionContextV1.INSTANCE");
            assertThat(code).doesNotContain("ProtocolVersions");
        }

        @Test
        @DisplayName("fieldInitializer() returns string literal")
        void fieldInitializerReturnsLiteral() {
            CodeBlock initializer = factory.fieldInitializer("v2");

            assertThat(initializer.toString()).isEqualTo("\"v2\"");
        }

        @Test
        @DisplayName("formatForList() returns quoted string")
        void formatForListReturnsQuotedString() {
            String formatted = factory.formatForList("v1");

            assertThat(formatted).isEqualTo("\"v1\"");
        }

        @Test
        @DisplayName("formatVersionsList() returns comma-separated quoted strings")
        void formatVersionsListReturnsQuotedStrings() {
            List<String> versions = Arrays.asList("v1", "v2");

            String formatted = factory.formatVersionsList(versions);

            assertThat(formatted).isEqualTo("\"v1\", \"v2\"");
        }
    }

    @Nested
    @DisplayName("Static Methods")
    class StaticMethods {

        @Test
        @DisplayName("toConstant() converts to uppercase")
        void toConstantConvertsToUppercase() {
            assertThat(VersionReferenceFactory.toConstant("v1")).isEqualTo("V1");
            assertThat(VersionReferenceFactory.toConstant("v2")).isEqualTo("V2");
            assertThat(VersionReferenceFactory.toConstant("legacy")).isEqualTo("LEGACY");
        }
    }
}
