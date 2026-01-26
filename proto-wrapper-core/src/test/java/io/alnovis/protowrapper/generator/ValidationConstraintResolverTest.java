package io.alnovis.protowrapper.generator;

import io.alnovis.protowrapper.model.FieldConstraints;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationConstraintResolver}.
 *
 * @since 2.3.0
 */
@DisplayName("ValidationConstraintResolver")
class ValidationConstraintResolverTest {

    @TempDir
    Path tempDir;

    private ValidationConstraintResolver resolver;
    private Set<String> allVersions;

    @BeforeEach
    void setUp() {
        GeneratorConfig config = GeneratorConfig.builder()
                .outputDirectory(tempDir)
                .generateValidationAnnotations(true)
                .apiPackage("com.example.api")
                .implPackagePattern("com.example.{version}")
                .protoPackagePattern("com.example.proto.{version}")
                .build();

        resolver = new ValidationConstraintResolver(config);
        allVersions = Set.of("v1", "v2");
    }

    @Nested
    @DisplayName("when validation annotations disabled")
    class WhenDisabled {

        @Test
        @DisplayName("returns empty constraints")
        void returnsEmptyConstraints() {
            GeneratorConfig config = GeneratorConfig.builder()
                    .outputDirectory(tempDir)
                    .generateValidationAnnotations(false)
                    .apiPackage("com.example.api")
                    .implPackagePattern("com.example.{version}")
                    .protoPackagePattern("com.example.proto.{version}")
                    .build();

            ValidationConstraintResolver disabledResolver = new ValidationConstraintResolver(config);

            MergedMessage message = createMessage("Person");
            MergedField field = createStringField("name", 1);
            message.addField(field);

            FieldConstraints constraints = disabledResolver.resolve(field, message, allVersions);

            assertFalse(constraints.hasAnyConstraint());
        }
    }

    @Nested
    @DisplayName("@NotNull resolution")
    class NotNullResolution {

        @Test
        @DisplayName("skips primitive return types")
        void skipsPrimitiveReturnTypes() {
            MergedMessage message = createMessage("Counter");
            MergedField intField = createIntField("count", 1);
            message.addField(intField);

            FieldConstraints constraints = resolver.resolve(intField, message, allVersions);

            assertFalse(constraints.notNull());
        }

        @Test
        @DisplayName("applies to repeated fields")
        void appliesToRepeatedFields() {
            MergedMessage message = createMessage("Order");
            MergedField repeatedField = createRepeatedStringField("items", 1);
            message.addField(repeatedField);

            FieldConstraints constraints = resolver.resolve(repeatedField, message, allVersions);

            assertTrue(constraints.notNull());
        }

        @Test
        @DisplayName("skips version-specific fields")
        void skipsVersionSpecificFields() {
            MergedMessage message = createMessage("Person");
            message.addVersion("v1");
            message.addVersion("v2");

            // Field only in v2
            FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                    .setName("newField")
                    .setNumber(1)
                    .setType(Type.TYPE_STRING)
                    .setLabel(Label.LABEL_OPTIONAL)
                    .build();
            MergedField versionSpecificField = MergedField.builder()
                    .addVersionField("v2", new FieldInfo(proto))
                    .build();
            message.addField(versionSpecificField);

            FieldConstraints constraints = resolver.resolve(versionSpecificField, message, allVersions);

            assertFalse(constraints.notNull());
        }
    }

    @Nested
    @DisplayName("@Valid resolution")
    class ValidResolution {

        @Test
        @DisplayName("applies to message type fields")
        void appliesToMessageTypeFields() {
            MergedMessage message = createMessage("Customer");
            MergedField messageField = createMessageField("address", 1, ".example.Address");
            message.addField(messageField);

            FieldConstraints constraints = resolver.resolve(messageField, message, allVersions);

            assertTrue(constraints.valid());
        }

        @Test
        @DisplayName("skips primitive fields")
        void skipsPrimitiveFields() {
            MergedMessage message = createMessage("Person");
            MergedField stringField = createStringField("name", 1);
            message.addField(stringField);

            FieldConstraints constraints = resolver.resolve(stringField, message, allVersions);

            assertFalse(constraints.valid());
        }
    }

    // ==================== Helper Methods ====================

    private MergedMessage createMessage(String name) {
        MergedMessage message = new MergedMessage(name);
        message.addVersion("v1");
        message.addVersion("v2");
        return message;
    }

    private MergedField createIntField(String name, int number) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(Type.TYPE_INT32)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        return MergedField.builder()
                .addVersionField("v1", new FieldInfo(proto))
                .addVersionField("v2", new FieldInfo(proto))
                .build();
    }

    private MergedField createStringField(String name, int number) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        return MergedField.builder()
                .addVersionField("v1", new FieldInfo(proto))
                .addVersionField("v2", new FieldInfo(proto))
                .build();
    }

    private MergedField createRepeatedStringField(String name, int number) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_REPEATED)
                .build();
        return MergedField.builder()
                .addVersionField("v1", new FieldInfo(proto))
                .addVersionField("v2", new FieldInfo(proto))
                .build();
    }

    private MergedField createMessageField(String name, int number, String typeName) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(typeName)
                .setLabel(Label.LABEL_OPTIONAL)
                .build();
        return MergedField.builder()
                .addVersionField("v1", new FieldInfo(proto))
                .addVersionField("v2", new FieldInfo(proto))
                .build();
    }
}
