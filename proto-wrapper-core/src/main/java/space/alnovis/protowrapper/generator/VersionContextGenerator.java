package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

    public VersionContextGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Generate VersionContext interface.
     *
     * @param schema Merged schema
     * @return Generated JavaFile
     */
    public JavaFile generateInterface(MergedSchema schema) {
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder("VersionContext")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Version context for creating version-specific wrapper instances.\n\n")
                .addJavadoc("<p>Provides factory methods for all wrapper types.</p>\n");

        // Static factory method
        String versionExamples = schema.getVersions().stream()
                .map(this::extractVersionNumber)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", "));

        MethodSpec.Builder forVersion = MethodSpec.methodBuilder("forVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(config.getApiPackage(), "VersionContext"))
                .addParameter(TypeName.INT, "version")
                .addJavadoc("Get VersionContext for a specific protocol version.\n")
                .addJavadoc("@param version Protocol version (e.g., $L)\n", versionExamples)
                .addJavadoc("@return VersionContext for the specified version\n")
                .beginControlFlow("switch (version)");

        for (String version : schema.getVersions()) {
            int versionNum = extractVersionNumber(version);
            String implPackage = config.getImplPackage(version);
            String contextClass = "VersionContext" + version.substring(0, 1).toUpperCase() + version.substring(1);
            ClassName contextClassName = ClassName.get(implPackage, contextClass);
            forVersion.addStatement("case $L: return $T.INSTANCE",
                    versionNum, contextClassName);
        }

        forVersion.addStatement("default: throw new $T($S + version)",
                        IllegalArgumentException.class, "Unsupported version: ")
                .endControlFlow();

        interfaceBuilder.addMethod(forVersion.build());

        // getVersion() method
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("@return Protocol version number\n")
                .build());

        // Wrap methods for each message type
        ClassName messageClass = MESSAGE_CLASS;

        for (MergedMessage message : schema.getMessages()) {
            ClassName returnType = ClassName.get(config.getApiPackage(), message.getInterfaceName());

            // Check if message exists in all versions
            boolean existsInAllVersions = message.getPresentInVersions().containsAll(schema.getVersions());

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("wrap" + message.getName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(messageClass, "proto")
                    .addJavadoc("Wrap a proto $L message.\n", message.getName())
                    .addJavadoc("@param proto Proto message\n")
                    .addJavadoc("@return Wrapped $L, or null if proto is null\n", message.getName());

            if (existsInAllVersions) {
                // Abstract method for messages in all versions
                methodBuilder.addModifiers(Modifier.ABSTRACT);
            } else {
                // Default method with UnsupportedOperationException for version-specific messages
                methodBuilder.addModifiers(Modifier.DEFAULT);
                methodBuilder.addJavadoc("@apiNote Present only in versions: $L\n", message.getPresentInVersions());
                methodBuilder.addStatement("throw new $T($S + $S)",
                        UnsupportedOperationException.class,
                        message.getName() + " is not available in this version. Present in: ",
                        message.getPresentInVersions().toString());
            }

            interfaceBuilder.addMethod(methodBuilder.build());

            // Add parseXxxFromBytes() method for version conversion
            ClassName invalidProtocolBufferException = ClassName.get("com.google.protobuf", "InvalidProtocolBufferException");
            MethodSpec.Builder parseMethod = MethodSpec.methodBuilder("parse" + message.getName() + "FromBytes")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                    .addException(invalidProtocolBufferException)
                    .addJavadoc("Parse bytes and wrap as $L.\n", message.getName())
                    .addJavadoc("@param bytes Protobuf-encoded bytes\n")
                    .addJavadoc("@return Wrapped $L, or null if bytes is null\n", message.getName())
                    .addJavadoc("@throws InvalidProtocolBufferException if bytes cannot be parsed\n");

            if (existsInAllVersions) {
                parseMethod.addModifiers(Modifier.ABSTRACT);
            } else {
                parseMethod.addModifiers(Modifier.DEFAULT);
                parseMethod.addJavadoc("@apiNote Present only in versions: $L\n", message.getPresentInVersions());
                parseMethod.addStatement("throw new $T($S + $S)",
                        UnsupportedOperationException.class,
                        message.getName() + " is not available in this version. Present in: ",
                        message.getPresentInVersions().toString());
            }

            interfaceBuilder.addMethod(parseMethod.build());

            // Add newXxxBuilder() method if builders are enabled
            if (config.isGenerateBuilders()) {
                ClassName builderType = returnType.nestedClass("Builder");

                MethodSpec.Builder newBuilderMethod = MethodSpec.methodBuilder("new" + message.getName() + "Builder")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType)
                        .addJavadoc("Create a new builder for $L.\n", message.getName())
                        .addJavadoc("@return Empty builder for $L\n", message.getName());

                if (existsInAllVersions) {
                    newBuilderMethod.addModifiers(Modifier.ABSTRACT);
                } else {
                    newBuilderMethod.addModifiers(Modifier.DEFAULT);
                    newBuilderMethod.addJavadoc("@apiNote Present only in versions: $L\n", message.getPresentInVersions());
                    newBuilderMethod.addStatement("throw new $T($S + $S)",
                            UnsupportedOperationException.class,
                            message.getName() + " is not available in this version. Present in: ",
                            message.getPresentInVersions().toString());
                }

                interfaceBuilder.addMethod(newBuilderMethod.build());
            }

            // Add nested type builder methods
            if (config.isGenerateBuilders()) {
                addNestedBuilderMethods(interfaceBuilder, message, new java.util.HashSet<>(schema.getVersions()));
            }
        }

        // Add convenience methods for Money if it exists with bills/coins fields
        if (config.isGenerateBuilders()) {
            schema.getMessages().stream()
                    .filter(m -> m.getName().equals("Money"))
                    .findFirst()
                    .ifPresent(money -> {
                        // Only generate convenience methods if Money has bills and coins fields
                        boolean hasBills = money.getFields().stream().anyMatch(f -> f.getName().equals("bills"));
                        boolean hasCoins = money.getFields().stream().anyMatch(f -> f.getName().equals("coins"));

                        if (hasBills && hasCoins) {
                            ClassName moneyType = ClassName.get(config.getApiPackage(), "Money");

                            // zeroMoney()
                            interfaceBuilder.addMethod(MethodSpec.methodBuilder("zeroMoney")
                                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                    .returns(moneyType)
                                    .addJavadoc("Create a Money with zero value.\n")
                                    .addJavadoc("@return Money with bills=0 and coins=0\n")
                                    .addStatement("return newMoneyBuilder().setBills(0L).setCoins(0).build()")
                                    .build());

                            // createMoney(long bills, int coins)
                            interfaceBuilder.addMethod(MethodSpec.methodBuilder("createMoney")
                                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                    .returns(moneyType)
                                    .addParameter(TypeName.LONG, "bills")
                                    .addParameter(TypeName.INT, "coins")
                                    .addJavadoc("Create a Money with specified values.\n")
                                    .addJavadoc("@param bills Number of bills\n")
                                    .addJavadoc("@param coins Number of coins\n")
                                    .addJavadoc("@return Money instance\n")
                                    .addStatement("return newMoneyBuilder().setBills(bills).setCoins(coins).build()")
                                    .build());
                        }
                    });
        }

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(config.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Build fully qualified ClassName for a nested message interface.
     * E.g., for TicketRequest.Item.Commodity returns ClassName representing
     * "api.package.TicketRequest.Item.Commodity"
     */
    private ClassName buildNestedInterfaceType(MergedMessage nested) {
        // Collect path from nested to root
        List<String> path = new ArrayList<>();
        MergedMessage current = nested;
        while (current != null) {
            path.add(current.getInterfaceName());
            current = current.getParent();
        }
        Collections.reverse(path);

        // First element is the top-level type
        ClassName result = ClassName.get(config.getApiPackage(), path.get(0));

        // Add nested classes
        for (int i = 1; i < path.size(); i++) {
            result = result.nestedClass(path.get(i));
        }

        return result;
    }

    /**
     * Build flattened method name for nested builder.
     * E.g., for TicketRequest.Item.Commodity returns "newTicketRequestItemCommodityBuilder"
     */
    private String buildNestedBuilderMethodName(MergedMessage nested) {
        return "new" + String.join("", collectMessageHierarchyNames(nested)) + "Builder";
    }

    /**
     * Build qualified display name for nested message (for javadoc).
     * E.g., "TicketRequest.Item.Commodity"
     */
    private String buildNestedQualifiedName(MergedMessage nested) {
        return String.join(".", collectMessageHierarchyNames(nested));
    }

    /**
     * Collect message names from root to current (e.g., ["Order", "Item"] for Order.Item).
     */
    private List<String> collectMessageHierarchyNames(MergedMessage message) {
        java.util.LinkedList<String> names = new java.util.LinkedList<>();
        for (MergedMessage current = message; current != null; current = current.getParent()) {
            names.addFirst(current.getName());
        }
        return names;
    }

    /**
     * Add builder methods for nested types recursively.
     */
    private void addNestedBuilderMethods(TypeSpec.Builder interfaceBuilder, MergedMessage parent,
                                          java.util.Set<String> allVersions) {
        for (MergedMessage nested : parent.getNestedMessages()) {
            // Build the fully qualified nested interface type
            ClassName nestedType = buildNestedInterfaceType(nested);
            ClassName builderType = nestedType.nestedClass("Builder");

            // Method name: newParentNestedBuilder (e.g., newTicketRequestItemCommodityBuilder)
            String methodName = buildNestedBuilderMethodName(nested);
            String qualifiedName = buildNestedQualifiedName(nested);

            boolean existsInAllVersions = nested.getPresentInVersions().containsAll(allVersions);

            MethodSpec.Builder newBuilderMethod = MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderType)
                    .addJavadoc("Create a new builder for $L.\n", qualifiedName)
                    .addJavadoc("@return Empty builder for $L\n", qualifiedName);

            if (existsInAllVersions) {
                newBuilderMethod.addModifiers(Modifier.ABSTRACT);
            } else {
                newBuilderMethod.addModifiers(Modifier.DEFAULT);
                newBuilderMethod.addJavadoc("@apiNote Present only in versions: $L\n", nested.getPresentInVersions());
                newBuilderMethod.addStatement("throw new $T($S + $S)",
                        UnsupportedOperationException.class,
                        qualifiedName + " is not available in this version. Present in: ",
                        nested.getPresentInVersions().toString());
            }

            interfaceBuilder.addMethod(newBuilderMethod.build());

            // Recursively add deeply nested types
            addNestedBuilderMethods(interfaceBuilder, nested, allVersions);
        }
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

        // getVersion()
        int versionNum = extractVersionNumber(version);
        classBuilder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addAnnotation(Override.class)
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
     * E.g., for TicketRequest.Item.Commodity in v202 returns ClassName representing
     * "impl.package.v202.TicketRequest.Item.Commodity"
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
        ClassName nestedInterfaceType = buildNestedInterfaceType(nested);
        ClassName builderType = nestedInterfaceType.nestedClass("Builder");

        // Build fully qualified impl type for newBuilder() call
        ClassName nestedImplType = buildNestedImplType(nested, implPackage, topLevelImplClassName);

        // Method name with full path (e.g., newTicketRequestItemCommodityBuilder)
        String methodName = buildNestedBuilderMethodName(nested);

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
     * Generate and write VersionContext interface.
     */
    public Path generateAndWriteInterface(MergedSchema schema) throws IOException {
        JavaFile javaFile = generateInterface(schema);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/VersionContext.java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write VersionContext implementation.
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
