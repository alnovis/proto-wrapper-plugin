package io.alnovis.protowrapper.generator.metadata;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.ChangeType;
import io.alnovis.protowrapper.diff.model.EnumDiff;
import io.alnovis.protowrapper.diff.model.FieldChange;
import io.alnovis.protowrapper.diff.model.MessageDiff;
import io.alnovis.protowrapper.generator.BaseGenerator;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.runtime.VersionSchemaDiff;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.alnovis.protowrapper.generator.ProtobufConstants.GENERATED_FILE_COMMENT;

/**
 * Generates VersionSchemaDiff implementation classes for version pairs.
 *
 * @since 2.3.0
 */
public class SchemaDiffGenerator extends BaseGenerator<SchemaDiff> {

    private final SchemaDiff schemaDiff;
    private final String fromVersion;
    private final String toVersion;

    public SchemaDiffGenerator(GeneratorConfig config, SchemaDiff schemaDiff) {
        super(config);
        this.schemaDiff = schemaDiff;
        this.fromVersion = schemaDiff.getV1Name();
        this.toVersion = schemaDiff.getV2Name();
    }

    public JavaFile generate() {
        String className = "SchemaDiff" + toClassName(fromVersion) + "To" + toClassName(toVersion);
        String metadataPackage = config.getMetadataPackage();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(VersionSchemaDiff.class)
                .addJavadoc("Schema diff from $L to $L.\n\n", fromVersion, toVersion)
                .addJavadoc("@since 2.3.0\n");

        // INSTANCE singleton
        classBuilder.addField(FieldSpec.builder(
                        ClassName.get(metadataPackage, className),
                        "INSTANCE",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $L()", className)
                .build());

        // Version constants
        ClassName protocolVersionsClass = ClassName.get(config.getApiPackage(), "ProtocolVersions");
        classBuilder.addField(FieldSpec.builder(String.class, "FROM_VERSION",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", protocolVersionsClass, fromVersion.toUpperCase())
                .build());
        classBuilder.addField(FieldSpec.builder(String.class, "TO_VERSION",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", protocolVersionsClass, toVersion.toUpperCase())
                .build());

        // Collect all field changes with message names
        List<FieldChangeWithMessage> fieldChanges = collectFieldChanges();
        addFieldChangesField(classBuilder, fieldChanges);

        // Collect enum changes
        addEnumChangesField(classBuilder);

        // Interface methods
        addInterfaceMethods(classBuilder);

        // Private constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        return JavaFile.builder(metadataPackage, classBuilder.build())
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Helper record to associate FieldChange with its parent message name.
     */
    private record FieldChangeWithMessage(String messageName, FieldChange fieldChange) {}

    private List<FieldChangeWithMessage> collectFieldChanges() {
        List<FieldChangeWithMessage> result = new ArrayList<>();
        for (MessageDiff md : schemaDiff.getMessageDiffs()) {
            if (md.changeType() == ChangeType.MODIFIED) {
                for (FieldChange fc : md.fieldChanges()) {
                    if (fc.changeType() != ChangeType.UNCHANGED) {
                        result.add(new FieldChangeWithMessage(md.messageName(), fc));
                    }
                }
            }
        }
        return result;
    }

    private void addFieldChangesField(TypeSpec.Builder classBuilder, List<FieldChangeWithMessage> changes) {
        ParameterizedTypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(VersionSchemaDiff.FieldChange.class));

        if (changes.isEmpty()) {
            classBuilder.addField(FieldSpec.builder(listType, "FIELD_CHANGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(config.isJava8Compatible()
                            ? "$T.emptyList()" : "$T.of()",
                            config.isJava8Compatible() ? Collections.class : List.class)
                    .build());
            return;
        }

        if (config.isJava8Compatible()) {
            classBuilder.addField(FieldSpec.builder(listType, "FIELD_CHANGES",
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build());

            CodeBlock.Builder staticBlock = CodeBlock.builder();
            staticBlock.addStatement("$T<$T> list = new $T<>()",
                    List.class, VersionSchemaDiff.FieldChange.class, ArrayList.class);

            for (FieldChangeWithMessage fcm : changes) {
                addFieldChangeToList(staticBlock, fcm, "list");
            }

            staticBlock.addStatement("FIELD_CHANGES = $T.unmodifiableList(list)", Collections.class);
            classBuilder.addStaticBlock(staticBlock.build());
        } else {
            CodeBlock.Builder initBlock = CodeBlock.builder();
            initBlock.add("$T.of(\n", List.class);

            boolean first = true;
            for (FieldChangeWithMessage fcm : changes) {
                if (!first) initBlock.add(",\n");
                first = false;
                addFieldChangeInline(initBlock, fcm);
            }
            initBlock.add("\n)");

            classBuilder.addField(FieldSpec.builder(listType, "FIELD_CHANGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initBlock.build())
                    .build());
        }
    }

    private void addFieldChangeToList(CodeBlock.Builder block, FieldChangeWithMessage fcm, String listVar) {
        String messageName = fcm.messageName();
        FieldChange fc = fcm.fieldChange();
        String fieldName = fc.fieldName();
        ChangeType changeType = fc.changeType();
        String hint = generateMigrationHint(messageName, fc);

        switch (changeType) {
            case ADDED:
                block.addStatement("$L.add($T.added($S, $S, $S, $S))",
                        listVar, VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v2Field() != null ? FieldChange.formatType(fc.v2Field()) : "unknown",
                        hint);
                break;
            case REMOVED:
                block.addStatement("$L.add($T.removed($S, $S, $S, $S))",
                        listVar, VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v1Field() != null ? FieldChange.formatType(fc.v1Field()) : "unknown",
                        hint);
                break;
            case TYPE_CHANGED:
                block.addStatement("$L.add($T.typeChanged($S, $S, $S, $S, $S))",
                        listVar, VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v1Field() != null ? FieldChange.formatType(fc.v1Field()) : "unknown",
                        fc.v2Field() != null ? FieldChange.formatType(fc.v2Field()) : "unknown",
                        hint);
                break;
            default:
                block.addStatement("$L.add($T.removed($S, $S, $S, $S))",
                        listVar, VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName, "unknown", hint);
        }
    }

    private void addFieldChangeInline(CodeBlock.Builder block, FieldChangeWithMessage fcm) {
        String messageName = fcm.messageName();
        FieldChange fc = fcm.fieldChange();
        String fieldName = fc.fieldName();
        ChangeType changeType = fc.changeType();
        String hint = generateMigrationHint(messageName, fc);

        switch (changeType) {
            case ADDED:
                block.add("    $T.added($S, $S, $S, $S)",
                        VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v2Field() != null ? FieldChange.formatType(fc.v2Field()) : "unknown",
                        hint);
                break;
            case REMOVED:
                block.add("    $T.removed($S, $S, $S, $S)",
                        VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v1Field() != null ? FieldChange.formatType(fc.v1Field()) : "unknown",
                        hint);
                break;
            case TYPE_CHANGED:
                block.add("    $T.typeChanged($S, $S, $S, $S, $S)",
                        VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName,
                        fc.v1Field() != null ? FieldChange.formatType(fc.v1Field()) : "unknown",
                        fc.v2Field() != null ? FieldChange.formatType(fc.v2Field()) : "unknown",
                        hint);
                break;
            default:
                block.add("    $T.removed($S, $S, $S, $S)",
                        VersionSchemaDiff.FieldChange.class,
                        messageName, fieldName, "unknown", hint);
        }
    }

    private void addEnumChangesField(TypeSpec.Builder classBuilder) {
        ParameterizedTypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(VersionSchemaDiff.EnumChange.class));

        List<EnumDiff> enumDiffs = schemaDiff.getEnumDiffs().stream()
                .filter(ed -> ed.changeType() != ChangeType.UNCHANGED)
                .toList();

        if (enumDiffs.isEmpty()) {
            classBuilder.addField(FieldSpec.builder(listType, "ENUM_CHANGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(config.isJava8Compatible()
                            ? "$T.emptyList()" : "$T.of()",
                            config.isJava8Compatible() ? Collections.class : List.class)
                    .build());
            return;
        }

        if (config.isJava8Compatible()) {
            classBuilder.addField(FieldSpec.builder(listType, "ENUM_CHANGES",
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build());

            CodeBlock.Builder staticBlock = CodeBlock.builder();
            staticBlock.addStatement("$T<$T> list = new $T<>()",
                    List.class, VersionSchemaDiff.EnumChange.class, ArrayList.class);

            for (EnumDiff ed : enumDiffs) {
                addEnumChangeToList(staticBlock, ed, "list");
            }

            staticBlock.addStatement("ENUM_CHANGES = $T.unmodifiableList(list)", Collections.class);
            classBuilder.addStaticBlock(staticBlock.build());
        } else {
            CodeBlock.Builder initBlock = CodeBlock.builder();
            initBlock.add("$T.of(\n", List.class);

            boolean first = true;
            for (EnumDiff ed : enumDiffs) {
                if (!first) initBlock.add(",\n");
                first = false;
                addEnumChangeInline(initBlock, ed);
            }
            initBlock.add("\n)");

            classBuilder.addField(FieldSpec.builder(listType, "ENUM_CHANGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initBlock.build())
                    .build());
        }
    }

    private void addEnumChangeToList(CodeBlock.Builder block, EnumDiff ed, String listVar) {
        String enumName = ed.enumName();
        ChangeType changeType = ed.changeType();

        switch (changeType) {
            case ADDED:
                List<String> addedValues = ed.v2Enum() != null
                        ? ed.v2Enum().getValues().stream().map(v -> v.getName()).toList()
                        : List.of();
                block.addStatement("$L.add($T.added($S, $T.asList($L), $S))",
                        listVar, VersionSchemaDiff.EnumChange.class,
                        enumName, java.util.Arrays.class,
                        formatStringList(addedValues),
                        "New enum added in " + toVersion);
                break;
            case REMOVED:
                List<String> removedValues = ed.v1Enum() != null
                        ? ed.v1Enum().getValues().stream().map(v -> v.getName()).toList()
                        : List.of();
                block.addStatement("$L.add($T.removed($S, $T.asList($L), $S))",
                        listVar, VersionSchemaDiff.EnumChange.class,
                        enumName, java.util.Arrays.class,
                        formatStringList(removedValues),
                        "Enum removed in " + toVersion);
                break;
            default:
                block.addStatement("$L.add($T.valuesChanged($S, $T.emptyList(), $T.emptyList(), $S))",
                        listVar, VersionSchemaDiff.EnumChange.class,
                        enumName, Collections.class, Collections.class,
                        "Enum values changed in " + toVersion);
        }
    }

    private void addEnumChangeInline(CodeBlock.Builder block, EnumDiff ed) {
        String enumName = ed.enumName();
        ChangeType changeType = ed.changeType();

        switch (changeType) {
            case ADDED:
                List<String> addedValues = ed.v2Enum() != null
                        ? ed.v2Enum().getValues().stream().map(v -> v.getName()).toList()
                        : List.of();
                block.add("    $T.added($S, $T.of($L), $S)",
                        VersionSchemaDiff.EnumChange.class,
                        enumName, List.class,
                        formatStringList(addedValues),
                        "New enum added in " + toVersion);
                break;
            case REMOVED:
                List<String> removedValues = ed.v1Enum() != null
                        ? ed.v1Enum().getValues().stream().map(v -> v.getName()).toList()
                        : List.of();
                block.add("    $T.removed($S, $T.of($L), $S)",
                        VersionSchemaDiff.EnumChange.class,
                        enumName, List.class,
                        formatStringList(removedValues),
                        "Enum removed in " + toVersion);
                break;
            default:
                block.add("    $T.valuesChanged($S, $T.of(), $T.of(), $S)",
                        VersionSchemaDiff.EnumChange.class,
                        enumName, List.class, List.class,
                        "Enum values changed in " + toVersion);
        }
    }

    private void addInterfaceMethods(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(MethodSpec.methodBuilder("getFromVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return FROM_VERSION")
                .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("getToVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return TO_VERSION")
                .build());

        ParameterizedTypeName fieldChangesType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(VersionSchemaDiff.FieldChange.class));
        classBuilder.addMethod(MethodSpec.methodBuilder("getFieldChanges")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldChangesType)
                .addStatement("return FIELD_CHANGES")
                .build());

        ParameterizedTypeName enumChangesType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(VersionSchemaDiff.EnumChange.class));
        classBuilder.addMethod(MethodSpec.methodBuilder("getEnumChanges")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(enumChangesType)
                .addStatement("return ENUM_CHANGES")
                .build());
    }

    private String generateMigrationHint(String messageName, FieldChange fc) {
        String fieldName = fc.fieldName();

        switch (fc.changeType()) {
            case REMOVED:
                return String.format("Field '%s.%s' was removed in %s.", messageName, fieldName, toVersion);
            case ADDED:
                return String.format("Field '%s.%s' was added in %s.", messageName, fieldName, toVersion);
            case TYPE_CHANGED:
                String oldType = fc.v1Field() != null ? FieldChange.formatType(fc.v1Field()) : "unknown";
                String newType = fc.v2Field() != null ? FieldChange.formatType(fc.v2Field()) : "unknown";
                return String.format("Field '%s.%s' changed type from %s to %s in %s.",
                        messageName, fieldName, oldType, newType, toVersion);
            default:
                return String.format("Field '%s.%s' changed in %s.", messageName, fieldName, toVersion);
        }
    }

    private String formatStringList(List<String> values) {
        if (values.isEmpty()) return "";
        return values.stream()
                .map(v -> "\"" + v + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String toClassName(String versionId) {
        return versionId.substring(0, 1).toUpperCase() + versionId.substring(1);
    }

    public Path generateAndWrite() throws IOException {
        JavaFile javaFile = generate();
        writeToFile(javaFile);

        String className = "SchemaDiff" + toClassName(fromVersion) + "To" + toClassName(toVersion);
        String relativePath = config.getMetadataPackage().replace('.', '/')
                + "/" + className + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
