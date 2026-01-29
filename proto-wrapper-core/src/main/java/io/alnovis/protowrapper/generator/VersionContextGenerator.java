package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.generator.versioncontext.VersionContextInterfaceComposer;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;
import io.alnovis.protowrapper.runtime.SchemaInfo;
import io.alnovis.protowrapper.runtime.VersionSchemaDiff;

import static io.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Generates VersionContext interface and version-specific implementations.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>VersionContext interface with all wrap/create methods</li>
 *   <li>VersionContextV1, VersionContextV2, etc. implementations</li>
 * </ul>
 */
public class VersionContextGenerator extends BaseGenerator<MergedSchema> {

    /**
     * Create a new VersionContextGenerator with the specified configuration.
     *
     * @param config the generator configuration
     */
    public VersionContextGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Generate VersionContext interface.
     *
     * <p>Uses {@link VersionContextInterfaceComposer} to build the interface
     * from reusable components, supporting both Java 8 and Java 9+ targets.</p>
     *
     * @param schema Merged schema
     * @return Generated JavaFile
     */
    public JavaFile generateInterface(MergedSchema schema) {
        return new VersionContextInterfaceComposer(config, schema)
                .addStaticFields()
                .addStaticMethods()
                .addInstanceMethods()
                .addWrapMethods()
                .addBuilderMethods()
                .addConvenienceMethods()
                .addMetadataMethods()
                .build();
    }

    /**
     * Generate VersionContextHelper class for Java 8 compatibility.
     *
     * <p>This helper class is only generated when targetJavaVersion is 8,
     * since Java 8 interfaces cannot have private static methods.</p>
     *
     * @param schema Merged schema
     * @return Generated JavaFile, or null if not needed (Java 9+)
     */
    public JavaFile generateHelper(MergedSchema schema) {
        if (!config.isJava8Compatible()) {
            return null;
        }

        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        List<String> versions = schema.getVersions();

        ParameterizedTypeName mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                versionContextType);

        TypeSpec.Builder helperBuilder = TypeSpec.classBuilder("VersionContextHelper")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Helper class for VersionContext initialization.\n")
                .addJavadoc("Generated for Java 8 compatibility (interfaces cannot have private static methods).\n");

        // Private constructor
        helperBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // createContexts() static method
        MethodSpec.Builder createContextsMethod = MethodSpec.methodBuilder("createContexts")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(mapType)
                .addStatement("$T<$T, $T> map = new $T<>()",
                        Map.class, String.class, versionContextType,
                        LinkedHashMap.class);

        VersionReferenceFactory vrf = VersionReferenceFactory.create(config);

        for (String version : versions) {
            String implPackage = config.getImplPackage(version);
            String contextClass = "VersionContext" + version.substring(0, 1).toUpperCase() + version.substring(1);
            ClassName contextClassName = ClassName.get(implPackage, contextClass);

            vrf.addMapPut(createContextsMethod, version, contextClassName, "INSTANCE");
        }

        createContextsMethod.addStatement("return $T.unmodifiableMap(map)", Collections.class);
        helperBuilder.addMethod(createContextsMethod.build());

        TypeSpec helperSpec = helperBuilder.build();

        return JavaFile.builder(config.getApiPackage(), helperSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }



    /**
     * Generate VersionContext implementation for a specific version.
     *
     * @param schema Merged schema
     * @param version Version string (e.g., "v1")
     * @param protoMappings Map of message name to proto class name
     * @return Generated JavaFile
     */
    public JavaFile generateImpl(MergedSchema schema, String version,
                                 Map<String, String> protoMappings) {
        String className = "VersionContext" + version.toUpperCase();
        String implPackage = config.getImplPackage(version);

        ClassName versionContextInterface = ClassName.get(config.getApiPackage(), "VersionContext");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(versionContextInterface)
                .addJavadoc("$L implementation of VersionContext.\n", version.toUpperCase());

        // Singleton instance
        classBuilder.addField(FieldSpec.builder(
                        ClassName.get(implPackage, className),
                        "INSTANCE",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $L()", className)
                .build());

        // Private constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // getVersionId() - primary method
        VersionReferenceFactory vrf = VersionReferenceFactory.create(config);
        MethodSpec.Builder getVersionIdMethod = MethodSpec.methodBuilder("getVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);

        vrf.addReturnStatement(getVersionIdMethod, version);
        classBuilder.addMethod(getVersionIdMethod.build());

        // Add metadata methods if enabled
        if (config.isGenerateSchemaMetadata()) {
            addMetadataImplMethods(classBuilder, schema, version);
        }

        // Wrap methods
        for (MergedMessage message : schema.getMessages()) {
            // Skip if message is not present in this version
            // (will use default method from interface which throws UnsupportedOperationException)
            if (!message.getPresentInVersions().contains(version)) {
                continue;
            }

            String protoClassName = protoMappings.get(message.getName());
            if (protoClassName == null) {
                continue; // Skip if no proto mapping
            }

            ClassName returnType = ClassName.get(config.getApiPackage(), message.getInterfaceName());
            ClassName protoType = ClassName.bestGuess(protoClassName);
            ClassName implType = ClassName.get(implPackage, config.getImplClassName(message.getName(), version));

            MethodSpec.Builder wrapMethod = MethodSpec.methodBuilder("wrap" + message.getName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(MESSAGE_CLASS, "proto")
                    .beginControlFlow("if (proto == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("return new $T(($T) proto)", implType, protoType);

            classBuilder.addMethod(wrapMethod.build());

            // Add parseXxxFromBytes() implementation for version conversion
            ClassName invalidProtocolBufferException = ClassName.get("com.google.protobuf", "InvalidProtocolBufferException");
            classBuilder.addMethod(MethodSpec.methodBuilder("parse" + message.getName() + "FromBytes")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                    .addException(invalidProtocolBufferException)
                    .beginControlFlow("if (bytes == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("return new $T($T.parseFrom(bytes))", implType, protoType)
                    .build());

            // Add parsePartialXxxFromBytes() implementation - lenient parsing without required fields check
            // Uses newBuilder().mergeFrom().buildPartial() to avoid required fields validation
            classBuilder.addMethod(MethodSpec.methodBuilder("parsePartial" + message.getName() + "FromBytes")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                    .addException(invalidProtocolBufferException)
                    .beginControlFlow("if (bytes == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("return new $T($T.newBuilder().mergeFrom(bytes).buildPartial())", implType, protoType)
                    .build());

            // Add newXxxBuilder() implementation if builders are enabled
            if (config.isGenerateBuilders()) {
                ClassName builderType = returnType.nestedClass("Builder");

                classBuilder.addMethod(MethodSpec.methodBuilder("new" + message.getName() + "Builder")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .addStatement("return $T.newBuilder()", implType)
                        .build());

                // Add nested type builder implementations
                String topLevelImplClassName = config.getImplClassName(message.getName(), version);
                for (MergedMessage nested : message.getNestedMessages()) {
                    addNestedBuilderImplMethods(classBuilder, nested, version, implPackage, topLevelImplClassName);
                }
            }
        }

        TypeSpec classSpec = classBuilder.build();

        return JavaFile.builder(implPackage, classSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Build fully qualified ClassName for a nested message impl class.
     * E.g., for TicketRequest.Item.Commodity in v2 returns ClassName representing
     * "impl.package.v2.TicketRequest.Item.Commodity"
     */
    private ClassName buildNestedImplType(MergedMessage nested, String implPackage, String topLevelImplClassName) {
        // Collect path from nested to root (excluding root since we start from topLevelImplClassName)
        List<String> path = collectNestedPathWithoutRoot(nested);

        // Build nested ClassName using reduce
        ClassName base = ClassName.get(implPackage, topLevelImplClassName);
        return path.stream().reduce(base, ClassName::nestedClass, (a, b) -> b);
    }

    /**
     * Collect message names excluding root (e.g., ["Item", "Commodity"] for Order.Item.Commodity).
     */
    private List<String> collectNestedPathWithoutRoot(MergedMessage message) {
        java.util.LinkedList<String> path = new java.util.LinkedList<>();
        for (MergedMessage current = message; current != null && current.getParent() != null; current = current.getParent()) {
            path.addFirst(current.getName());
        }
        return path;
    }

    /**
     * Add nested type builder implementations recursively.
     */
    private void addNestedBuilderImplMethods(TypeSpec.Builder classBuilder, MergedMessage nested,
                                              String version, String implPackage,
                                              String topLevelImplClassName) {
        // Skip if nested message is not present in this version
        if (!nested.getPresentInVersions().contains(version)) {
            return;
        }

        // Build fully qualified interface type for return
        ClassName nestedInterfaceType = GeneratorUtils.buildNestedInterfaceType(nested, config.getApiPackage());
        ClassName builderType = nestedInterfaceType.nestedClass("Builder");

        // Build fully qualified impl type for newBuilder() call
        ClassName nestedImplType = buildNestedImplType(nested, implPackage, topLevelImplClassName);

        // Method name with full path (e.g., newTicketRequestItemCommodityBuilder)
        String methodName = GeneratorUtils.buildNestedBuilderMethodName(nested);

        classBuilder.addMethod(MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(builderType)
                .addStatement("return $T.newBuilder()", nestedImplType)
                .build());

        // Recursively add deeply nested types
        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            addNestedBuilderImplMethods(classBuilder, deeplyNested, version, implPackage, topLevelImplClassName);
        }
    }

    private int extractVersionNumber(String version) {
        String numStr = version.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Add getSchemaInfo() and getDiffFrom() implementations.
     */
    private void addMetadataImplMethods(TypeSpec.Builder classBuilder, MergedSchema schema, String version) {
        String metadataPackage = config.getMetadataPackage();
        String versionClassName = version.substring(0, 1).toUpperCase() + version.substring(1);

        // getSchemaInfo() implementation
        ClassName schemaInfoClass = ClassName.get(metadataPackage, "SchemaInfo" + versionClassName);
        classBuilder.addMethod(MethodSpec.methodBuilder("getSchemaInfo")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(SchemaInfo.class)
                .addStatement("return $T.INSTANCE", schemaInfoClass)
                .build());

        // getDiffFrom(String) implementation
        List<String> versions = schema.getVersions();
        int currentIndex = versions.indexOf(version);

        ParameterizedTypeName optionalDiffType = ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                ClassName.get(VersionSchemaDiff.class));

        MethodSpec.Builder getDiffMethod = MethodSpec.methodBuilder("getDiffFrom")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(optionalDiffType)
                .addParameter(String.class, "previousVersion");

        // Only add diff references if this is not the first version
        if (currentIndex > 0) {
            String previousVersion = versions.get(currentIndex - 1);
            String prevClassName = previousVersion.substring(0, 1).toUpperCase() + previousVersion.substring(1);
            ClassName schemaDiffClass = ClassName.get(metadataPackage,
                    "SchemaDiff" + prevClassName + "To" + versionClassName);

            ClassName protocolVersionsClass = ClassName.get(config.getApiPackage(), "ProtocolVersions");
            String prevConstant = previousVersion.toUpperCase();

            getDiffMethod.beginControlFlow("if ($T.$L.equals(previousVersion))",
                            protocolVersionsClass, prevConstant)
                    .addStatement("return $T.of($T.INSTANCE)", Optional.class, schemaDiffClass)
                    .endControlFlow();
        }

        getDiffMethod.addStatement("return $T.empty()", Optional.class);
        classBuilder.addMethod(getDiffMethod.build());
    }

    /**
     * Generate and write VersionContext interface.
     *
     * <p>For Java 8 compatibility, this also generates VersionContextHelper class.</p>
     *
     * @param schema the merged schema
     * @return the path to the generated interface file
     * @throws IOException if writing fails
     */
    public Path generateAndWriteInterface(MergedSchema schema) throws IOException {
        JavaFile javaFile = generateInterface(schema);
        writeToFile(javaFile);

        // For Java 8, also generate helper class
        if (config.isJava8Compatible()) {
            generateAndWriteHelper(schema);
        }

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/VersionContext.java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write VersionContextHelper class for Java 8 compatibility.
     *
     * @param schema the merged schema
     * @return the path to the generated file, or null if not needed (Java 9+)
     * @throws IOException if writing fails
     */
    public Path generateAndWriteHelper(MergedSchema schema) throws IOException {
        JavaFile javaFile = generateHelper(schema);
        if (javaFile == null) {
            return null;
        }
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/VersionContextHelper.java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write VersionContext implementation.
     *
     * @param schema the merged schema
     * @param version the version string
     * @param protoMappings map of message name to proto class name
     * @return the path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWriteImpl(MergedSchema schema, String version,
                                     Map<String, String> protoMappings) throws IOException {
        JavaFile javaFile = generateImpl(schema, version, protoMappings);
        writeToFile(javaFile);

        String implPackage = config.getImplPackage(version);
        String relativePath = implPackage.replace('.', '/')
                + "/VersionContext" + version.toUpperCase() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
