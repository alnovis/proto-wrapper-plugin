package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import io.alnovis.protowrapper.model.FieldConstraints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationAnnotationGenerator}.
 *
 * @since 2.3.0
 */
@DisplayName("ValidationAnnotationGenerator")
class ValidationAnnotationGeneratorTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("with Jakarta namespace")
    class WithJakartaNamespace {

        private ValidationAnnotationGenerator generator;

        @BeforeEach
        void setUp() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .generateValidationAnnotations(true)
                    .validationAnnotationStyle("jakarta")
                    .apiPackage("com.example.api")
                    .implPackagePattern("com.example.{version}")
                    .protoPackagePattern("com.example.proto.{version}")
                    .build();

            generator = new ValidationAnnotationGenerator(config);
        }

        @Test
        @DisplayName("uses jakarta.validation.constraints package")
        void usesJakartaPackage() {
            ClassName notNullClass = generator.getNotNullClass();

            assertEquals("jakarta.validation.constraints", notNullClass.packageName());
            assertEquals("NotNull", notNullClass.simpleName());
        }

        @Test
        @DisplayName("@Valid uses jakarta.validation package")
        void validUsesJakartaBasePackage() {
            ClassName validClass = generator.getValidClass();

            assertEquals("jakarta.validation", validClass.packageName());
            assertEquals("Valid", validClass.simpleName());
        }

        @Test
        @DisplayName("generates @NotNull annotation")
        void generatesNotNullAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .notNull(true)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            assertTrue(annotations.get(0).toString().contains("jakarta.validation.constraints.NotNull"));
        }

        @Test
        @DisplayName("generates @Valid annotation")
        void generatesValidAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .valid(true)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            assertTrue(annotations.get(0).toString().contains("jakarta.validation.Valid"));
        }

        @Test
        @DisplayName("generates @Min annotation with value")
        void generatesMinAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .min(0L)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            String annotationStr = annotations.get(0).toString();
            assertTrue(annotationStr.contains("jakarta.validation.constraints.Min"));
            assertTrue(annotationStr.contains("0"));
        }

        @Test
        @DisplayName("generates @Max annotation with value")
        void generatesMaxAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .max(100L)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            String annotationStr = annotations.get(0).toString();
            assertTrue(annotationStr.contains("jakarta.validation.constraints.Max"));
            assertTrue(annotationStr.contains("100"));
        }

        @Test
        @DisplayName("generates @Size annotation with min and max")
        void generatesSizeAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .sizeMin(1)
                    .sizeMax(100)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            String annotationStr = annotations.get(0).toString();
            assertTrue(annotationStr.contains("jakarta.validation.constraints.Size"));
            assertTrue(annotationStr.contains("min = 1"));
            assertTrue(annotationStr.contains("max = 100"));
        }

        @Test
        @DisplayName("generates @Size annotation with only min")
        void generatesSizeAnnotationWithOnlyMin() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .sizeMin(1)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            String annotationStr = annotations.get(0).toString();
            assertTrue(annotationStr.contains("jakarta.validation.constraints.Size"));
            assertTrue(annotationStr.contains("min = 1"));
            assertFalse(annotationStr.contains("max"));
        }

        @Test
        @DisplayName("generates @Pattern annotation with regexp")
        void generatesPatternAnnotation() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .pattern("^[a-z]+$")
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            String annotationStr = annotations.get(0).toString();
            assertTrue(annotationStr.contains("jakarta.validation.constraints.Pattern"));
            assertTrue(annotationStr.contains("^[a-z]+$"));
        }

        @Test
        @DisplayName("generates multiple annotations")
        void generatesMultipleAnnotations() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .notNull(true)
                    .valid(true)
                    .min(0L)
                    .max(100L)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(4, annotations.size());
        }

        @Test
        @DisplayName("returns empty list for null constraints")
        void returnsEmptyForNullConstraints() {
            List<AnnotationSpec> annotations = generator.generate(null);

            assertTrue(annotations.isEmpty());
        }

        @Test
        @DisplayName("returns empty list for empty constraints")
        void returnsEmptyForEmptyConstraints() {
            List<AnnotationSpec> annotations = generator.generate(FieldConstraints.none());

            assertTrue(annotations.isEmpty());
        }
    }

    @Nested
    @DisplayName("with Javax namespace")
    class WithJavaxNamespace {

        private ValidationAnnotationGenerator generator;

        @BeforeEach
        void setUp() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .generateValidationAnnotations(true)
                    .validationAnnotationStyle("javax")
                    .apiPackage("com.example.api")
                    .implPackagePattern("com.example.{version}")
                    .protoPackagePattern("com.example.proto.{version}")
                    .build();

            generator = new ValidationAnnotationGenerator(config);
        }

        @Test
        @DisplayName("uses javax.validation.constraints package")
        void usesJavaxPackage() {
            ClassName notNullClass = generator.getNotNullClass();

            assertEquals("javax.validation.constraints", notNullClass.packageName());
            assertEquals("NotNull", notNullClass.simpleName());
        }

        @Test
        @DisplayName("@Valid uses javax.validation package")
        void validUsesJavaxBasePackage() {
            ClassName validClass = generator.getValidClass();

            assertEquals("javax.validation", validClass.packageName());
            assertEquals("Valid", validClass.simpleName());
        }

        @Test
        @DisplayName("generates @NotNull with javax namespace")
        void generatesNotNullWithJavaxNamespace() {
            FieldConstraints constraints = FieldConstraints.builder()
                    .notNull(true)
                    .build();

            List<AnnotationSpec> annotations = generator.generate(constraints);

            assertEquals(1, annotations.size());
            assertTrue(annotations.get(0).toString().contains("javax.validation.constraints.NotNull"));
        }
    }

    @Nested
    @DisplayName("with Java 8 target")
    class WithJava8Target {

        private ValidationAnnotationGenerator generator;

        @BeforeEach
        void setUp() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .generateValidationAnnotations(true)
                    .validationAnnotationStyle("jakarta") // Should be overridden to javax
                    .targetJavaVersion(8)
                    .apiPackage("com.example.api")
                    .implPackagePattern("com.example.{version}")
                    .protoPackagePattern("com.example.proto.{version}")
                    .build();

            generator = new ValidationAnnotationGenerator(config);
        }

        @Test
        @DisplayName("automatically uses javax namespace for Java 8")
        void automaticallyUsesJavaxForJava8() {
            ClassName notNullClass = generator.getNotNullClass();

            assertEquals("javax.validation.constraints", notNullClass.packageName());
        }
    }
}
