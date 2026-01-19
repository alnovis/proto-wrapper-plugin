package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.generator.versioncontext.VersionContextInterfaceComposer;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        for (String version : versions) {
            String implPackage = config.getImplPackage(version);
            String contextClass = "VersionContext" + version.substring(0, 1).toUpperCase() + version.substring(1);
            ClassName contextClassName = ClassName.get(implPackage, contextClass);
            createContextsMethod.addStatement("map.put($S, $T.INSTANCE)", version, contextClassName);
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
        classBuilder.addMethod(MethodSpec.methodBuilder("getVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", version)
                .build());

        // getVersion() - deprecated
        // Java 8 doesn't support @Deprecated(since, forRemoval), use simple @Deprecated
        int versionNum = extractVersionNumber(version);
        AnnotationSpec.Builder deprecatedImplBuilder = AnnotationSpec.builder(Deprecated.class);
        if (!config.isJava8Compatible()) {
            deprecatedImplBuilder.addMember("since", "$S", "1.6.7")
                    .addMember("forRemoval", "$L", true);
        }
        AnnotationSpec deprecatedImpl = deprecatedImplBuilder.build();

        classBuilder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addAnnotation(Override.class)
                .addAnnotation(deprecatedImpl)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return $L", versionNum)
                .build());

        // Wrap methods
        ClassName messageClass = MESSAGE_CLASS;

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
                    .addParameter(messageClass, "proto")
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
     * Generate VersionContextFactory class.
     *
     * @param schema Merged schema
     * @return Generated JavaFile
     * @deprecated since 1.7.0, for removal. Factory methods are now part of VersionContext interface.
     *             Use {@link #generateInterface(MergedSchema)} instead.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public JavaFile generateFactory(MergedSchema schema) {
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        ClassName factoryClassName = ClassName.get(config.getApiPackage(), "VersionContextFactory");

        TypeSpec.Builder factoryBuilder = TypeSpec.classBuilder("VersionContextFactory")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Factory for obtaining VersionContext instances by version string.\n\n")
                .addJavadoc("<p>This class provides type-safe access to VersionContext implementations\n")
                .addJavadoc("without requiring reflection.</p>\n\n")
                .addJavadoc("<p>Usage:</p>\n")
                .addJavadoc("<pre>{@code\n")
                .addJavadoc("VersionContext ctx = VersionContextFactory.get(\"v2\");\n")
                .addJavadoc("Order order = ctx.wrapOrder(protoMessage);\n")
                .addJavadoc("}</pre>\n");

        // Private constructor
        factoryBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        // Static CONTEXTS map
        ParameterizedTypeName mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                versionContextType);

        // Build static initializer for CONTEXTS map
        CodeBlock.Builder staticInit = CodeBlock.builder()
                .addStatement("$T<$T, $T> map = new $T<>()",
                        Map.class, String.class, versionContextType,
                        java.util.LinkedHashMap.class);

        List<String> versions = schema.getVersions();
        for (String version : versions) {
            String implPackage = config.getImplPackage(version);
            String contextClass = "VersionContext" + version.substring(0, 1).toUpperCase() + version.substring(1);
            ClassName contextClassName = ClassName.get(implPackage, contextClass);
            staticInit.addStatement("map.put($S, $T.INSTANCE)", version, contextClassName);
        }

        staticInit.addStatement("CONTEXTS = $T.unmodifiableMap(map)", java.util.Collections.class);

        // Build SUPPORTED_VERSIONS list
        String versionsJoined = versions.stream()
                .map(v -> "\"" + v + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
        staticInit.addStatement("SUPPORTED_VERSIONS = $T.of($L)", List.class, versionsJoined);

        // Default version is the last one (highest)
        String defaultVersion = versions.get(versions.size() - 1);
        staticInit.addStatement("DEFAULT_VERSION = $S", defaultVersion);

        // Add fields
        factoryBuilder.addField(FieldSpec.builder(mapType, "CONTEXTS",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build());

        ParameterizedTypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(String.class));
        factoryBuilder.addField(FieldSpec.builder(listType, "SUPPORTED_VERSIONS",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build());

        factoryBuilder.addField(FieldSpec.builder(String.class, "DEFAULT_VERSION",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build());

        // Add static initializer
        factoryBuilder.addStaticBlock(staticInit.build());

        // get(String version) method
        factoryBuilder.addMethod(MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(versionContextType)
                .addParameter(String.class, "version")
                .addJavadoc("Get VersionContext for the specified version.\n\n")
                .addJavadoc("@param version Version string (e.g., \"v1\", \"v2\")\n")
                .addJavadoc("@return VersionContext for the specified version\n")
                .addJavadoc("@throws IllegalArgumentException if version is not supported\n")
                .addStatement("$T ctx = CONTEXTS.get(version)", versionContextType)
                .beginControlFlow("if (ctx == null)")
                .addStatement("throw new $T($S + version + $S + SUPPORTED_VERSIONS)",
                        IllegalArgumentException.class,
                        "Unsupported version: '",
                        "'. Supported: ")
                .endControlFlow()
                .addStatement("return ctx")
                .build());

        // find(String version) method
        ParameterizedTypeName optionalType = ParameterizedTypeName.get(
                ClassName.get(java.util.Optional.class),
                versionContextType);
        factoryBuilder.addMethod(MethodSpec.methodBuilder("find")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(optionalType)
                .addParameter(String.class, "version")
                .addJavadoc("Find VersionContext for the specified version.\n\n")
                .addJavadoc("@param version Version string (e.g., \"v1\", \"v2\")\n")
                .addJavadoc("@return Optional containing VersionContext, or empty if not supported\n")
                .addStatement("return $T.ofNullable(CONTEXTS.get(version))", java.util.Optional.class)
                .build());

        // getDefault() method
        factoryBuilder.addMethod(MethodSpec.methodBuilder("getDefault")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(versionContextType)
                .addJavadoc("Get the default VersionContext (latest version).\n\n")
                .addJavadoc("@return Default VersionContext\n")
                .addStatement("return CONTEXTS.get(DEFAULT_VERSION)")
                .build());

        // supportedVersions() method
        factoryBuilder.addMethod(MethodSpec.methodBuilder("supportedVersions")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(listType)
                .addJavadoc("Get list of supported version strings.\n\n")
                .addJavadoc("@return Immutable list of supported versions (e.g., [\"v1\", \"v2\"])\n")
                .addStatement("return SUPPORTED_VERSIONS")
                .build());

        // defaultVersion() method
        factoryBuilder.addMethod(MethodSpec.methodBuilder("defaultVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addJavadoc("Get the default version string.\n\n")
                .addJavadoc("@return Default version string (e.g., \"v2\")\n")
                .addStatement("return DEFAULT_VERSION")
                .build());

        // isSupported(String version) method
        factoryBuilder.addMethod(MethodSpec.methodBuilder("isSupported")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(String.class, "version")
                .addJavadoc("Check if a version is supported.\n\n")
                .addJavadoc("@param version Version string to check\n")
                .addJavadoc("@return true if version is supported\n")
                .addStatement("return CONTEXTS.containsKey(version)")
                .build());

        TypeSpec factorySpec = factoryBuilder.build();

        return JavaFile.builder(config.getApiPackage(), factorySpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate and write VersionContextFactory.
     *
     * @param schema the merged schema
     * @return the path to the generated file
     * @throws IOException if writing fails
     * @deprecated since 1.7.0, for removal. Factory methods are now part of VersionContext interface.
     *             Use {@link #generateAndWriteInterface(MergedSchema)} instead.
     */
    @Deprecated(since = "1.7.0", forRemoval = true)
    public Path generateAndWriteFactory(MergedSchema schema) throws IOException {
        JavaFile javaFile = generateFactory(schema);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/VersionContextFactory.java";
        return config.getOutputDirectory().resolve(relativePath);
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
