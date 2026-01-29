package io.alnovis.protowrapper.generator.metadata;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.generator.BaseGenerator;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.MessageInfo;
import io.alnovis.protowrapper.runtime.SchemaInfo;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.alnovis.protowrapper.generator.ProtobufConstants.GENERATED_FILE_COMMENT;

/**
 * Generates SchemaInfo implementation classes for each protocol version.
 *
 * <p>Generated classes provide runtime access to schema metadata including
 * enum values and message field information.</p>
 *
 * <p>Example generated code:</p>
 * <pre>{@code
 * public final class SchemaInfoV203 implements SchemaInfo {
 *     public static final SchemaInfoV203 INSTANCE = new SchemaInfoV203();
 *
 *     private static final Map<String, EnumInfo> ENUMS = Map.of(
 *         "TaxTypeEnum", new EnumInfoImpl("TaxTypeEnum", "kkm.proto.v203.TaxTypeEnum",
 *             List.of(new EnumValue("VAT", 100)))
 *     );
 *
 *     @Override
 *     public String getVersionId() { return ProtocolVersions.V203; }
 *
 *     @Override
 *     public Map<String, EnumInfo> getEnums() { return ENUMS; }
 *     // ...
 * }
 * }</pre>
 *
 * @since 2.3.0
 */
public class SchemaInfoGenerator extends BaseGenerator<VersionSchema> {

    private final VersionSchema schema;

    /**
     * Create a new SchemaInfoGenerator.
     *
     * @param config generator configuration
     * @param schema version schema to generate metadata for
     */
    public SchemaInfoGenerator(GeneratorConfig config, VersionSchema schema) {
        super(config);
        this.schema = schema;
    }

    /**
     * Generate the SchemaInfo implementation class.
     *
     * @return generated JavaFile
     */
    public JavaFile generate() {
        String versionId = schema.getVersion();
        String className = "SchemaInfo" + toClassName(versionId);
        String metadataPackage = config.getMetadataPackage();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(SchemaInfo.class)
                .addJavadoc("Schema metadata for protocol version $L.\n\n", versionId)
                .addJavadoc("<p>Provides runtime access to enum values and message field information.</p>\n\n")
                .addJavadoc("@since 2.3.0\n");

        // Add INSTANCE singleton
        classBuilder.addField(FieldSpec.builder(
                        ClassName.get(metadataPackage, className),
                        "INSTANCE",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $L()", className)
                .addJavadoc("Singleton instance.\n")
                .build());

        // Add VERSION_ID constant using ProtocolVersions
        ClassName protocolVersionsClass = ClassName.get(config.getApiPackage(), "ProtocolVersions");
        String versionConstant = versionId.toUpperCase();
        classBuilder.addField(FieldSpec.builder(String.class, "VERSION_ID",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$L", protocolVersionsClass, versionConstant)
                .build());

        // Generate ENUMS map
        addEnumsField(classBuilder);

        // Generate MESSAGES map
        addMessagesField(classBuilder);

        // Add getVersionId() method
        classBuilder.addMethod(MethodSpec.methodBuilder("getVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return VERSION_ID")
                .build());

        // Add getEnums() method
        ParameterizedTypeName enumMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(SchemaInfo.EnumInfo.class));
        classBuilder.addMethod(MethodSpec.methodBuilder("getEnums")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(enumMapType)
                .addStatement("return ENUMS")
                .build());

        // Add getMessages() method
        ParameterizedTypeName messageMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(SchemaInfo.MessageInfo.class));
        classBuilder.addMethod(MethodSpec.methodBuilder("getMessages")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(messageMapType)
                .addStatement("return MESSAGES")
                .build());

        // Private constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // Add inner classes for EnumInfo and MessageInfo implementations
        addEnumInfoImplClass(classBuilder);
        addMessageInfoImplClass(classBuilder);

        return JavaFile.builder(metadataPackage, classBuilder.build())
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    private void addEnumsField(TypeSpec.Builder classBuilder) {
        Collection<EnumInfo> enums = schema.getEnums();

        ParameterizedTypeName enumMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(SchemaInfo.EnumInfo.class));

        if (enums.isEmpty()) {
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(enumMapType, "ENUMS",
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
            if (config.isJava8Compatible()) {
                fieldBuilder.initializer("$T.emptyMap()", java.util.Collections.class);
            } else {
                fieldBuilder.initializer("$T.of()", Map.class);
            }
            classBuilder.addField(fieldBuilder.build());
            return;
        }

        CodeBlock.Builder initBlock = CodeBlock.builder();
        if (config.isJava8Compatible()) {
            initBlock.add("$T.<String, $T>builder()",
                    ClassName.get("java.util.stream", "Stream"),
                    ClassName.get(SchemaInfo.EnumInfo.class));
            // Java 8 requires different approach - use static initializer block
            // For simplicity, use a helper method
            initBlock.add("\n// Initialized in static block");
        } else {
            initBlock.add("$T.ofEntries(\n", Map.class);
            boolean first = true;
            for (EnumInfo enumInfo : enums) {
                if (!first) initBlock.add(",\n");
                first = false;
                initBlock.add("    $T.entry($S, createEnum($S, $S, $T.of(\n",
                        Map.class,
                        enumInfo.getName(),
                        enumInfo.getName(),
                        enumInfo.getFullName(),
                        List.class);

                boolean firstValue = true;
                for (var value : enumInfo.getValues()) {
                    if (!firstValue) initBlock.add(",\n");
                    firstValue = false;
                    initBlock.add("        new $T($S, $L)",
                            SchemaInfo.EnumValue.class,
                            value.getName(),
                            value.getNumber());
                }
                initBlock.add(")))\n");
            }
            initBlock.add(")");
        }

        if (config.isJava8Compatible()) {
            // For Java 8, initialize in static block
            classBuilder.addField(FieldSpec.builder(enumMapType, "ENUMS",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .build());

            CodeBlock.Builder staticBlock = CodeBlock.builder();
            staticBlock.addStatement("$T<String, $T> enumMap = new $T<>()",
                    java.util.Map.class, SchemaInfo.EnumInfo.class, java.util.HashMap.class);
            for (EnumInfo enumInfo : enums) {
                staticBlock.add("enumMap.put($S, createEnum($S, $S, $T.asList(\n",
                        enumInfo.getName(),
                        enumInfo.getName(),
                        enumInfo.getFullName(),
                        java.util.Arrays.class);
                boolean firstValue = true;
                for (var value : enumInfo.getValues()) {
                    if (!firstValue) staticBlock.add(",\n");
                    firstValue = false;
                    staticBlock.add("    new $T($S, $L)",
                            SchemaInfo.EnumValue.class,
                            value.getName(),
                            value.getNumber());
                }
                staticBlock.add(")));\n");
            }
            staticBlock.addStatement("ENUMS = $T.unmodifiableMap(enumMap)", java.util.Collections.class);
            classBuilder.addStaticBlock(staticBlock.build());
        } else {
            classBuilder.addField(FieldSpec.builder(enumMapType, "ENUMS",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initBlock.build())
                    .build());
        }

        // Add helper method
        classBuilder.addMethod(MethodSpec.methodBuilder("createEnum")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(SchemaInfo.EnumInfo.class)
                .addParameter(String.class, "name")
                .addParameter(String.class, "fullName")
                .addParameter(ParameterizedTypeName.get(List.class, SchemaInfo.EnumValue.class), "values")
                .addStatement("return new EnumInfoImpl(name, fullName, values)")
                .build());
    }

    private void addMessagesField(TypeSpec.Builder classBuilder) {
        Collection<MessageInfo> messages = schema.getMessages();

        ParameterizedTypeName messageMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(SchemaInfo.MessageInfo.class));

        if (messages.isEmpty()) {
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(messageMapType, "MESSAGES",
                    Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
            if (config.isJava8Compatible()) {
                fieldBuilder.initializer("$T.emptyMap()", java.util.Collections.class);
            } else {
                fieldBuilder.initializer("$T.of()", Map.class);
            }
            classBuilder.addField(fieldBuilder.build());
            return;
        }

        // For simplicity, just store message names without field details
        // Full field info can be added later if needed
        if (config.isJava8Compatible()) {
            classBuilder.addField(FieldSpec.builder(messageMapType, "MESSAGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .build());

            CodeBlock.Builder staticBlock = CodeBlock.builder();
            staticBlock.addStatement("$T<String, $T> msgMap = new $T<>()",
                    java.util.Map.class, SchemaInfo.MessageInfo.class, java.util.HashMap.class);
            for (MessageInfo msg : messages) {
                staticBlock.addStatement("msgMap.put($S, createMessage($S, $S))",
                        msg.getName(), msg.getName(), msg.getFullName());
            }
            staticBlock.addStatement("MESSAGES = $T.unmodifiableMap(msgMap)", java.util.Collections.class);
            classBuilder.addStaticBlock(staticBlock.build());
        } else {
            CodeBlock.Builder initBlock = CodeBlock.builder();
            initBlock.add("$T.ofEntries(\n", Map.class);
            boolean first = true;
            for (MessageInfo msg : messages) {
                if (!first) initBlock.add(",\n");
                first = false;
                initBlock.add("    $T.entry($S, createMessage($S, $S))",
                        Map.class, msg.getName(), msg.getName(), msg.getFullName());
            }
            initBlock.add("\n)");

            classBuilder.addField(FieldSpec.builder(messageMapType, "MESSAGES",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(initBlock.build())
                    .build());
        }

        // Add helper method
        classBuilder.addMethod(MethodSpec.methodBuilder("createMessage")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(SchemaInfo.MessageInfo.class)
                .addParameter(String.class, "name")
                .addParameter(String.class, "fullName")
                .addStatement("return new MessageInfoImpl(name, fullName)")
                .build());
    }

    private void addEnumInfoImplClass(TypeSpec.Builder classBuilder) {
        ParameterizedTypeName valuesListType = ParameterizedTypeName.get(
                List.class, SchemaInfo.EnumValue.class);

        TypeSpec enumInfoImpl = TypeSpec.classBuilder("EnumInfoImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(SchemaInfo.EnumInfo.class)
                .addField(String.class, "name", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "fullName", Modifier.PRIVATE, Modifier.FINAL)
                .addField(valuesListType, "values", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "name")
                        .addParameter(String.class, "fullName")
                        .addParameter(valuesListType, "values")
                        .addStatement("this.name = name")
                        .addStatement("this.fullName = fullName")
                        .addStatement("this.values = values")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getName")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return name")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getFullName")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return fullName")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getValues")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(valuesListType)
                        .addStatement("return values")
                        .build())
                .build();

        classBuilder.addType(enumInfoImpl);
    }

    private void addMessageInfoImplClass(TypeSpec.Builder classBuilder) {
        ParameterizedTypeName fieldsMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(SchemaInfo.FieldInfo.class));

        TypeSpec messageInfoImpl = TypeSpec.classBuilder("MessageInfoImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(SchemaInfo.MessageInfo.class)
                .addField(String.class, "name", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "fullName", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "name")
                        .addParameter(String.class, "fullName")
                        .addStatement("this.name = name")
                        .addStatement("this.fullName = fullName")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getName")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return name")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getFullName")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return fullName")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getFields")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(fieldsMapType)
                        .addStatement(config.isJava8Compatible()
                                ? "return $T.emptyMap()"
                                : "return $T.of()",
                                config.isJava8Compatible()
                                        ? java.util.Collections.class
                                        : Map.class)
                        .build())
                .build();

        classBuilder.addType(messageInfoImpl);
    }

    /**
     * Convert version ID to class name suffix (e.g., "v203" -> "V203").
     */
    private String toClassName(String versionId) {
        return versionId.substring(0, 1).toUpperCase() + versionId.substring(1);
    }

    /**
     * Generate and write SchemaInfo class.
     *
     * @return path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite() throws IOException {
        JavaFile javaFile = generate();
        writeToFile(javaFile);

        String className = "SchemaInfo" + toClassName(schema.getVersion());
        String relativePath = config.getMetadataPackage().replace('.', '/')
                + "/" + className + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
