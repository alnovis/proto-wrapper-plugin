package io.alnovis.protowrapper.generator.metadata;

import com.squareup.javapoet.JavaFile;
import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.ChangeType;
import io.alnovis.protowrapper.diff.model.EnumDiff;
import io.alnovis.protowrapper.diff.model.EnumValueChange;
import io.alnovis.protowrapper.diff.model.FieldChange;
import io.alnovis.protowrapper.diff.model.MessageDiff;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SchemaDiffGenerator.
 */
@DisplayName("SchemaDiffGenerator Tests")
class SchemaDiffGeneratorTest {

    @TempDir
    Path tempDir;

    private GeneratorConfig config;

    @BeforeEach
    void setUp() {
        config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .apiPackage("com.example.api")
                .generateSchemaMetadata(true)
                .build();
    }

    @Nested
    @DisplayName("Class Generation")
    class ClassGenerationTest {

        @Test
        @DisplayName("generates SchemaDiff class with correct name")
        void generatesSchemaDiffClass() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();

            assertThat(javaFile.typeSpec.name).isEqualTo("SchemaDiffV1ToV2");
            assertThat(javaFile.packageName).isEqualTo("com.example.metadata");
        }

        @Test
        @DisplayName("generates correct class name for numeric versions")
        void generatesCorrectClassNameForNumericVersions() {
            SchemaDiff diff = createEmptyDiff("v201", "v203");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();

            assertThat(javaFile.typeSpec.name).isEqualTo("SchemaDiffV201ToV203");
        }

        @Test
        @DisplayName("generated class implements VersionSchemaDiff interface")
        void implementsVersionSchemaDiffInterface() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("implements VersionSchemaDiff");
        }

        @Test
        @DisplayName("generates INSTANCE singleton field")
        void generatesInstanceSingleton() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public static final SchemaDiffV1ToV2 INSTANCE");
        }

        @Test
        @DisplayName("generates private constructor")
        void generatesPrivateConstructor() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("private SchemaDiffV1ToV2()");
        }

        @Test
        @DisplayName("imports VersionSchemaDiff from runtime package")
        void importsVersionSchemaDiff() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("import io.alnovis.protowrapper.runtime.VersionSchemaDiff");
        }
    }

    @Nested
    @DisplayName("Version Methods Generation")
    class VersionMethodsTest {

        @Test
        @DisplayName("getFromVersion returns source version using ProtocolVersions")
        void getFromVersionReturnsSourceVersion() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public String getFromVersion()");
            assertThat(code).contains("ProtocolVersions.V1");
        }

        @Test
        @DisplayName("getToVersion returns target version using ProtocolVersions")
        void getToVersionReturnsTargetVersion() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("public String getToVersion()");
            assertThat(code).contains("ProtocolVersions.V2");
        }

        @Test
        @DisplayName("version methods use constants for numeric versions")
        void versionMethodsUseConstantsForNumericVersions() {
            SchemaDiff diff = createEmptyDiff("v202", "v203");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("ProtocolVersions.V202");
            assertThat(code).contains("ProtocolVersions.V203");
        }
    }

    @Nested
    @DisplayName("Field Changes Generation")
    class FieldChangesTest {

        @Test
        @DisplayName("generates FIELD_CHANGES list")
        void generatesFieldChangesList() {
            SchemaDiff diff = createDiffWithFieldChanges();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("List<VersionSchemaDiff.FieldChange> FIELD_CHANGES");
            assertThat(code).contains("getFieldChanges()");
        }

        @Test
        @DisplayName("includes added field change with message name and field name")
        void includesAddedFieldChange() {
            SchemaDiff diff = createDiffWithAddedField();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("FieldChange.added");
            assertThat(code).contains("\"Order\"");  // message name
            assertThat(code).contains("\"newField\"");  // field name
            // Note: field numbers are not included in the generated code
        }

        @Test
        @DisplayName("includes removed field change")
        void includesRemovedFieldChange() {
            SchemaDiff diff = createDiffWithRemovedField();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("FieldChange.removed");
            assertThat(code).contains("\"Payment\"");
            assertThat(code).contains("\"oldField\"");
            assertThat(code).contains("3");
        }

        @Test
        @DisplayName("includes type changed field")
        void includesTypeChangedField() {
            SchemaDiff diff = createDiffWithTypeChange();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("FieldChange.typeChanged");
            assertThat(code).contains("\"status\"");
        }

        @Test
        @DisplayName("includes multiple field changes in single message")
        void includesMultipleFieldChangesInSingleMessage() {
            FieldChange change1 = new FieldChange(1, "field1", ChangeType.ADDED, null, null, List.of());
            FieldChange change2 = new FieldChange(2, "field2", ChangeType.REMOVED, null, null, List.of());
            FieldChange change3 = new FieldChange(3, "field3", ChangeType.TYPE_CHANGED, null, null, List.of());

            MessageDiff messageDiff = new MessageDiff(
                    "Order",
                    ChangeType.MODIFIED,
                    null, null,
                    List.of(change1, change2, change3),
                    List.of(),
                    List.of()
            );

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"field1\"");
            assertThat(code).contains("\"field2\"");
            assertThat(code).contains("\"field3\"");
        }

        @Test
        @DisplayName("includes field changes from multiple messages")
        void includesFieldChangesFromMultipleMessages() {
            FieldChange orderChange = new FieldChange(1, "orderField", ChangeType.ADDED, null, null, List.of());
            FieldChange paymentChange = new FieldChange(2, "paymentField", ChangeType.REMOVED, null, null, List.of());

            MessageDiff orderDiff = new MessageDiff("Order", ChangeType.MODIFIED, null, null,
                    List.of(orderChange), List.of(), List.of());
            MessageDiff paymentDiff = new MessageDiff("Payment", ChangeType.MODIFIED, null, null,
                    List.of(paymentChange), List.of(), List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(orderDiff, paymentDiff), List.of(), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"Order\"");
            assertThat(code).contains("\"orderField\"");
            assertThat(code).contains("\"Payment\"");
            assertThat(code).contains("\"paymentField\"");
        }

        @Test
        @DisplayName("generates migration hints based on change type")
        void generatesMigrationHints() {
            FieldChange changeWithHint = new FieldChange(
                    1, "taxType", ChangeType.TYPE_CHANGED, null, null,
                    List.of("Use TaxTypeEnum.valueOf() to convert")
            );

            MessageDiff messageDiff = new MessageDiff("Tax", ChangeType.MODIFIED, null, null,
                    List.of(changeWithHint), List.of(), List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Generator creates migration hints based on change type
            assertThat(code).contains("Field 'Tax.taxType' changed type");
            assertThat(code).contains("in v2");
        }

        @Test
        @DisplayName("generates empty list when no field changes")
        void generatesEmptyListWhenNoChanges() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).containsAnyOf("List.of()", "Collections.emptyList()");
        }
    }

    @Nested
    @DisplayName("Enum Changes Generation")
    class EnumChangesTest {

        @Test
        @DisplayName("generates ENUM_CHANGES list")
        void generatesEnumChangesList() {
            SchemaDiff diff = createDiffWithEnumValueChanges();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("List<VersionSchemaDiff.EnumChange> ENUM_CHANGES");
            assertThat(code).contains("getEnumChanges()");
        }

        @Test
        @DisplayName("includes added enum with enum name")
        void includesAddedEnumWithName() {
            // EnumDiff for a newly added enum
            EnumDiff enumDiff = new EnumDiff("TaxTypeEnum", ChangeType.ADDED, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"TaxTypeEnum\"");
            assertThat(code).contains("EnumChange.added");
        }

        @Test
        @DisplayName("includes removed enum with enum name")
        void includesRemovedEnumWithName() {
            EnumDiff enumDiff = new EnumDiff("StatusEnum", ChangeType.REMOVED, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"StatusEnum\"");
            assertThat(code).contains("EnumChange.removed");
        }

        @Test
        @DisplayName("includes modified enum with values changed")
        void includesModifiedEnumWithValuesChanged() {
            EnumValueChange valueChange = new EnumValueChange("CHANGED_VALUE", ChangeType.ADDED, null, 10);
            EnumDiff enumDiff = new EnumDiff("TaxTypeEnum", ChangeType.MODIFIED, null, null, List.of(valueChange));

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"TaxTypeEnum\"");
            // MODIFIED enums use valuesChanged
            assertThat(code).contains("valuesChanged");
        }

        @Test
        @DisplayName("includes added enum (new enum type)")
        void includesAddedEnum() {
            EnumDiff addedEnum = new EnumDiff("NewEnum", ChangeType.ADDED, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(addedEnum), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"NewEnum\"");
            assertThat(code).contains("EnumChange.added");
        }

        @Test
        @DisplayName("includes removed enum (deleted enum type)")
        void includesRemovedEnum() {
            EnumDiff removedEnum = new EnumDiff("OldEnum", ChangeType.REMOVED, null, null, List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(removedEnum), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("\"OldEnum\"");
            assertThat(code).contains("EnumChange.removed");
        }

        @Test
        @DisplayName("generates empty enum list when no enum changes")
        void generatesEmptyEnumListWhenNoChanges() {
            SchemaDiff diff = createEmptyDiff("v1", "v2");
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // ENUM_CHANGES should be empty
            assertThat(code).containsPattern("ENUM_CHANGES.*=.*(?:List\\.of\\(\\)|Collections\\.emptyList\\(\\))");
        }
    }

    @Nested
    @DisplayName("Java 8 Compatibility")
    class Java8CompatibilityTest {

        @Test
        @DisplayName("uses Collections.unmodifiableList for Java 8")
        void usesCollectionsForJava8() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.example.api")
                    .generateSchemaMetadata(true)
                    .targetJavaVersion(8)
                    .build();

            SchemaDiff diff = createDiffWithFieldChanges();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(java8Config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Java 8 uses Collections.unmodifiableList with static initialization block
            assertThat(code).contains("Collections.unmodifiableList");
        }

        @Test
        @DisplayName("uses List.of for Java 9+")
        void usesListOfForJava9Plus() {
            GeneratorConfig java9Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.example.api")
                    .generateSchemaMetadata(true)
                    .targetJavaVersion(9)
                    .build();

            SchemaDiff diff = createDiffWithFieldChanges();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(java9Config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("List.of(");
        }

        @Test
        @DisplayName("uses static block for initialization in Java 8")
        void usesStaticBlockForJava8() {
            GeneratorConfig java8Config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .apiPackage("com.example.api")
                    .generateSchemaMetadata(true)
                    .targetJavaVersion(8)
                    .build();

            SchemaDiff diff = createDiffWithFieldChanges();
            SchemaDiffGenerator generator = new SchemaDiffGenerator(java8Config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Java 8 uses static block
            assertThat(code).contains("static {");
        }
    }

    @Nested
    @DisplayName("Message Changes Generation")
    class MessageChangesTest {

        @Test
        @DisplayName("handles added message")
        void handlesAddedMessage() {
            MessageDiff addedMessage = new MessageDiff("NewMessage", ChangeType.ADDED,
                    null, null, List.of(), List.of(), List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(addedMessage), List.of(), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            // Added message should still be represented in some way
            assertThat(code).contains("getFieldChanges()");
        }

        @Test
        @DisplayName("handles removed message")
        void handlesRemovedMessage() {
            MessageDiff removedMessage = new MessageDiff("OldMessage", ChangeType.REMOVED,
                    null, null, List.of(), List.of(), List.of());

            SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(removedMessage), List.of(), List.of());
            SchemaDiffGenerator generator = new SchemaDiffGenerator(config, diff);

            JavaFile javaFile = generator.generate();
            String code = javaFile.toString();

            assertThat(code).contains("getFieldChanges()");
        }
    }

    // Helper methods

    private SchemaDiff createEmptyDiff(String v1, String v2) {
        return new SchemaDiff(v1, v2, List.of(), List.of(), List.of());
    }

    private SchemaDiff createDiffWithFieldChanges() {
        FieldChange fieldChange = new FieldChange(5, "newField", ChangeType.ADDED, null, null, List.of());

        MessageDiff messageDiff = new MessageDiff(
                "Order",
                ChangeType.MODIFIED,
                null, null,
                List.of(fieldChange),
                List.of(),
                List.of()
        );

        return new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
    }

    private SchemaDiff createDiffWithAddedField() {
        FieldChange fieldChange = new FieldChange(5, "newField", ChangeType.ADDED, null, null, List.of());

        MessageDiff messageDiff = new MessageDiff(
                "Order",
                ChangeType.MODIFIED,
                null, null,
                List.of(fieldChange),
                List.of(),
                List.of()
        );

        return new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
    }

    private SchemaDiff createDiffWithRemovedField() {
        FieldChange fieldChange = new FieldChange(3, "oldField", ChangeType.REMOVED, null, null, List.of());

        MessageDiff messageDiff = new MessageDiff(
                "Payment",
                ChangeType.MODIFIED,
                null, null,
                List.of(fieldChange),
                List.of(),
                List.of()
        );

        return new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
    }

    private SchemaDiff createDiffWithTypeChange() {
        FieldChange fieldChange = new FieldChange(2, "status", ChangeType.TYPE_CHANGED, null, null, List.of());

        MessageDiff messageDiff = new MessageDiff(
                "Order",
                ChangeType.MODIFIED,
                null, null,
                List.of(fieldChange),
                List.of(),
                List.of()
        );

        return new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());
    }

    private SchemaDiff createDiffWithEnumValueChanges() {
        // EnumValueChange(valueName, changeType, v1Number, v2Number)
        EnumValueChange addedValue = new EnumValueChange("NEW_TAX", ChangeType.ADDED, null, 100);
        // EnumDiff(enumName, changeType, v1Enum, v2Enum, valueChanges)
        EnumDiff enumDiff = new EnumDiff("TaxTypeEnum", ChangeType.MODIFIED, null, null, List.of(addedValue));

        return new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());
    }
}
