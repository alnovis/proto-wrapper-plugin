package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.generator.GeneratorConfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JavaVersionCodegen strategy implementations.
 *
 * <p>Verifies that Java8Codegen and Java9PlusCodegen generate
 * correct version-specific code.</p>
 */
@DisplayName("JavaVersionCodegen Tests")
class JavaVersionCodegenTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;
    private ClassName versionContextType;
    private ParameterizedTypeName mapType;
    private ParameterizedTypeName listType;
    private List<String> versions;
    private String versionsJoined;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("org.example.api")
                .implPackagePattern("org.example.impl.{version}")
                .build();

        versionContextType = ClassName.get("org.example.api", "VersionContext");
        mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                versionContextType);
        listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(String.class));
        versions = Arrays.asList("v1", "v2");
        versionsJoined = "\"v1\", \"v2\"";
    }

    @Nested
    @DisplayName("Java8Codegen")
    class Java8CodegenTest {

        private Java8Codegen codegen;

        @BeforeEach
        void setUp() {
            codegen = Java8Codegen.INSTANCE;
        }

        @Test
        @DisplayName("is singleton")
        void isSingleton() {
            assertThat(Java8Codegen.INSTANCE).isSameAs(Java8Codegen.INSTANCE);
        }

        @Test
        @DisplayName("requires helper class")
        void requiresHelperClass() {
            assertThat(codegen.requiresHelperClass()).isTrue();
        }

        @Test
        @DisplayName("creates CONTEXTS field using helper class")
        void createsContextsFieldUsingHelper() {
            FieldSpec field = codegen.createContextsField(mapType, versionContextType, versions, config);

            assertThat(field.name).isEqualTo("CONTEXTS");
            String code = field.toString();
            assertThat(code).contains("VersionContextHelper.createContexts()");
        }

        @Test
        @DisplayName("creates SUPPORTED_VERSIONS using Collections.unmodifiableList")
        void createsSupportedVersionsUsingCollections() {
            FieldSpec field = codegen.createSupportedVersionsField(listType, versionsJoined);

            assertThat(field.name).isEqualTo("SUPPORTED_VERSIONS");
            String code = field.toString();
            assertThat(code).contains("Collections.unmodifiableList");
            assertThat(code).contains("Arrays.asList");
            assertThat(code).contains("\"v1\"");
            assertThat(code).contains("\"v2\"");
        }

        @Test
        @DisplayName("returns empty Optional for createContextsMethod")
        void returnsEmptyForCreateContextsMethod() {
            Optional<MethodSpec> method = codegen.createContextsMethod(
                    mapType, versionContextType, versions, config);

            assertThat(method).isEmpty();
        }

        @Test
        @DisplayName("does not support private interface methods")
        void doesNotSupportPrivateInterfaceMethods() {
            assertThat(codegen.supportsPrivateInterfaceMethods()).isFalse();
        }

        @Test
        @DisplayName("creates simple @Deprecated annotation without parameters")
        void createsSimpleDeprecatedAnnotation() {
            AnnotationSpec annotation = codegen.deprecatedAnnotation("1.6.9", true);

            String code = annotation.toString();
            assertThat(code).isEqualTo("@java.lang.Deprecated");
            assertThat(code).doesNotContain("since");
            assertThat(code).doesNotContain("forRemoval");
        }

        @Test
        @DisplayName("creates immutable list using Collections.unmodifiableList")
        void createsImmutableListUsingCollections() {
            CodeBlock code = codegen.immutableListOf("\"a\"", "\"b\"");

            String codeStr = code.toString();
            assertThat(codeStr).contains("Collections.unmodifiableList");
            assertThat(codeStr).contains("Arrays.asList");
            assertThat(codeStr).contains("\"a\"");
            assertThat(codeStr).contains("\"b\"");
        }

        @Test
        @DisplayName("creates immutable set using Collections.unmodifiableSet")
        void createsImmutableSetUsingCollections() {
            CodeBlock code = codegen.immutableSetOf("\"x\"", "\"y\"");

            String codeStr = code.toString();
            assertThat(codeStr).contains("Collections.unmodifiableSet");
            assertThat(codeStr).contains("HashSet");
            assertThat(codeStr).contains("Arrays.asList");
            assertThat(codeStr).contains("\"x\"");
            assertThat(codeStr).contains("\"y\"");
        }
    }

    @Nested
    @DisplayName("Java9PlusCodegen")
    class Java9PlusCodegenTest {

        private Java9PlusCodegen codegen;

        @BeforeEach
        void setUp() {
            codegen = Java9PlusCodegen.INSTANCE;
        }

        @Test
        @DisplayName("is singleton")
        void isSingleton() {
            assertThat(Java9PlusCodegen.INSTANCE).isSameAs(Java9PlusCodegen.INSTANCE);
        }

        @Test
        @DisplayName("does not require helper class")
        void doesNotRequireHelperClass() {
            assertThat(codegen.requiresHelperClass()).isFalse();
        }

        @Test
        @DisplayName("creates CONTEXTS field calling createContexts()")
        void createsContextsFieldCallingMethod() {
            FieldSpec field = codegen.createContextsField(mapType, versionContextType, versions, config);

            assertThat(field.name).isEqualTo("CONTEXTS");
            String code = field.toString();
            assertThat(code).contains("createContexts()");
            assertThat(code).doesNotContain("VersionContextHelper");
        }

        @Test
        @DisplayName("creates SUPPORTED_VERSIONS using List.of()")
        void createsSupportedVersionsUsingListOf() {
            FieldSpec field = codegen.createSupportedVersionsField(listType, versionsJoined);

            assertThat(field.name).isEqualTo("SUPPORTED_VERSIONS");
            String code = field.toString();
            assertThat(code).contains("List.of");
            assertThat(code).contains("\"v1\"");
            assertThat(code).contains("\"v2\"");
            assertThat(code).doesNotContain("Collections");
            assertThat(code).doesNotContain("Arrays");
        }

        @Test
        @DisplayName("creates private static createContexts method")
        void createsCreateContextsMethod() {
            Optional<MethodSpec> methodOpt = codegen.createContextsMethod(
                    mapType, versionContextType, versions, config);

            assertThat(methodOpt).isPresent();
            MethodSpec method = methodOpt.get();

            assertThat(method.name).isEqualTo("createContexts");
            String code = method.toString();
            assertThat(code).contains("private static");
            // JavaPoet generates fully qualified names
            assertThat(code).contains("Map<");
            assertThat(code).contains("VersionContext>");
            assertThat(code).contains("LinkedHashMap");
            assertThat(code).contains("map.put(\"v1\"");
            assertThat(code).contains("map.put(\"v2\"");
            assertThat(code).contains("VersionContextV1.INSTANCE");
            assertThat(code).contains("VersionContextV2.INSTANCE");
            assertThat(code).contains("unmodifiableMap(map)");
        }

        @Test
        @DisplayName("createContexts method generates correct impl package references")
        void createContextsMethodUsesCorrectImplPackage() {
            GeneratorConfig customConfig = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.custom.api")
                    .implPackagePattern("com.custom.impl.{version}")
                    .build();

            Optional<MethodSpec> methodOpt = codegen.createContextsMethod(
                    mapType, versionContextType, versions, customConfig);

            assertThat(methodOpt).isPresent();
            String code = methodOpt.get().toString();
            assertThat(code).contains("com.custom.impl.v1.VersionContextV1");
            assertThat(code).contains("com.custom.impl.v2.VersionContextV2");
        }

        @Test
        @DisplayName("supports private interface methods")
        void supportsPrivateInterfaceMethods() {
            assertThat(codegen.supportsPrivateInterfaceMethods()).isTrue();
        }

        @Test
        @DisplayName("creates @Deprecated annotation with since and forRemoval")
        void createsDeprecatedAnnotationWithParameters() {
            AnnotationSpec annotation = codegen.deprecatedAnnotation("1.6.9", true);

            String code = annotation.toString();
            assertThat(code).contains("@java.lang.Deprecated");
            assertThat(code).contains("since = \"1.6.9\"");
            assertThat(code).contains("forRemoval = true");
        }

        @Test
        @DisplayName("creates @Deprecated annotation with forRemoval = false")
        void createsDeprecatedAnnotationWithoutForRemoval() {
            AnnotationSpec annotation = codegen.deprecatedAnnotation("1.5.0", false);

            String code = annotation.toString();
            assertThat(code).contains("since = \"1.5.0\"");
            assertThat(code).contains("forRemoval = false");
        }

        @Test
        @DisplayName("creates immutable list using List.of()")
        void createsImmutableListUsingListOf() {
            CodeBlock code = codegen.immutableListOf("\"a\"", "\"b\"");

            String codeStr = code.toString();
            assertThat(codeStr).contains("List.of");
            assertThat(codeStr).contains("\"a\"");
            assertThat(codeStr).contains("\"b\"");
            assertThat(codeStr).doesNotContain("Collections");
            assertThat(codeStr).doesNotContain("Arrays");
        }

        @Test
        @DisplayName("creates immutable set using Set.of()")
        void createsImmutableSetUsingSetOf() {
            CodeBlock code = codegen.immutableSetOf("\"x\"", "\"y\"");

            String codeStr = code.toString();
            assertThat(codeStr).contains("Set.of");
            assertThat(codeStr).contains("\"x\"");
            assertThat(codeStr).contains("\"y\"");
            assertThat(codeStr).doesNotContain("Collections");
            assertThat(codeStr).doesNotContain("HashSet");
        }
    }

    @Nested
    @DisplayName("Selection based on config")
    class CodegenSelectionTest {

        @Test
        @DisplayName("Java 8 config selects Java8Codegen")
        void java8ConfigSelectsJava8Codegen() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(8)
                    .build();

            JavaVersionCodegen codegen = java8Config.isJava8Compatible()
                    ? Java8Codegen.INSTANCE
                    : Java9PlusCodegen.INSTANCE;

            assertThat(codegen).isInstanceOf(Java8Codegen.class);
            assertThat(codegen.requiresHelperClass()).isTrue();
        }

        @Test
        @DisplayName("Java 9 config selects Java9PlusCodegen")
        void java9ConfigSelectsJava9PlusCodegen() {
            GeneratorConfig java9Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(9)
                    .build();

            JavaVersionCodegen codegen = java9Config.isJava8Compatible()
                    ? Java8Codegen.INSTANCE
                    : Java9PlusCodegen.INSTANCE;

            assertThat(codegen).isInstanceOf(Java9PlusCodegen.class);
            assertThat(codegen.requiresHelperClass()).isFalse();
        }

        @Test
        @DisplayName("Java 17 config selects Java9PlusCodegen")
        void java17ConfigSelectsJava9PlusCodegen() {
            GeneratorConfig java17Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(17)
                    .build();

            JavaVersionCodegen codegen = java17Config.isJava8Compatible()
                    ? Java8Codegen.INSTANCE
                    : Java9PlusCodegen.INSTANCE;

            assertThat(codegen).isInstanceOf(Java9PlusCodegen.class);
        }

        @Test
        @DisplayName("getJavaVersionCodegen returns Java8Codegen for Java 8")
        void getJavaVersionCodegenReturnsJava8CodegenForJava8() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(8)
                    .build();

            assertThat(java8Config.getJavaVersionCodegen()).isInstanceOf(Java8Codegen.class);
        }

        @Test
        @DisplayName("getJavaVersionCodegen returns Java9PlusCodegen for Java 9+")
        void getJavaVersionCodegenReturnsJava9PlusCodegenForJava9Plus() {
            GeneratorConfig java9Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(9)
                    .build();

            assertThat(java9Config.getJavaVersionCodegen()).isInstanceOf(Java9PlusCodegen.class);

            GeneratorConfig java17Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("org.example.api")
                    .targetJavaVersion(17)
                    .build();

            assertThat(java17Config.getJavaVersionCodegen()).isInstanceOf(Java9PlusCodegen.class);
        }
    }
}
