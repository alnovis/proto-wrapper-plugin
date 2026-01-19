package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedOneof;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import space.alnovis.protowrapper.generator.conflict.BuilderImplContext;
import space.alnovis.protowrapper.generator.conflict.FieldProcessingChain;
import space.alnovis.protowrapper.generator.conflict.ProcessingContext;
import space.alnovis.protowrapper.generator.oneof.OneofGenerator;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates version-specific implementation classes.
 *
 * <p>Example output:</p>
 * <pre>
 * public class MoneyV1 extends AbstractMoney&lt;Common.Money&gt; {
 *
 *     public MoneyV1(Common.Money proto) {
 *         super(proto);
 *     }
 *
 *     &#64;Override
 *     protected long extractBills(Common.Money proto) {
 *         return proto.getBills();
 *     }
 *
 *     &#64;Override
 *     protected int extractCoins(Common.Money proto) {
 *         return proto.getCoins();
 *     }
 * }
 * </pre>
 */
public class ImplClassGenerator extends BaseGenerator<MergedMessage> {

    /**
     * Legacy field - kept for backward compatibility.
     * @deprecated Use {@link GenerationContext} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    private TypeResolver typeResolver;

    /**
     * Create a new ImplClassGenerator.
     *
     * @param config the generator configuration
     */
    public ImplClassGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @param schema The merged schema
     * @deprecated Use {@link #generate(MergedMessage, String, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public void setSchema(MergedSchema schema) {
        this.typeResolver = new TypeResolver(config, schema);
    }

    /**
     * Generate implementation class for a specific version.
     *
     * @param message Merged message info
     * @param protoClassName Fully qualified proto class name
     * @param ctx Generation context with version
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, String protoClassName, GenerationContext ctx) {
        String version = ctx.requireVersion();
        String className = ctx.getImplClassName(message.getName());
        String implPackage = ctx.getImplPackage();
        TypeResolver resolver = ctx.getTypeResolver();

        // Proto type
        ClassName protoType = ClassName.bestGuess(protoClassName);

        // Superclass: AbstractMoney<Proto.Money>
        ClassName abstractClass = ClassName.get(
                ctx.getApiPackage() + IMPL_PACKAGE_SUFFIX,
                message.getAbstractClassName()
        );
        ParameterizedTypeName superType = ParameterizedTypeName.get(abstractClass, protoType);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(superType)
                .addJavadoc("$L implementation of $L interface.\n\n",
                        version.toUpperCase(), message.getInterfaceName())
                .addJavadoc("@see $L\n", message.getAbstractClassName());

        // Constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(protoType, "proto")
                .addStatement("super(proto)")
                .build());

        // Implement extract methods for fields using the handler chain
        ProcessingContext procCtx = ProcessingContext.forImpl(message, protoType, ctx, config);
        FieldProcessingChain.getInstance().addExtractImplementations(classBuilder, message, version, procCtx);

        // Add oneof extract implementations
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            classBuilder.addMethod(oneofGenerator.generateImplExtractCaseMethod(
                    oneof, message, protoType, version, ctx.getApiPackage()));
        }

        // Add common implementation methods
        addCommonImplMethods(classBuilder, className, protoType, implPackage, ctx);

        // Add Builder support if enabled
        if (config.isGenerateBuilders()) {
            addBuilderImpl(classBuilder, message, protoType, className, implPackage, ctx);
        }

        // Add nested impl classes for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            if (!nested.getPresentInVersions().contains(version)) {
                continue;
            }
            String nestedProtoClassName = protoClassName + "." + nested.getName();
            TypeSpec nestedClass = generateNestedImplClass(nested, message, nestedProtoClassName, ctx);
            classBuilder.addType(nestedClass);
        }

        TypeSpec classSpec = classBuilder.build();

        return JavaFile.builder(implPackage, classSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Add common implementation methods (serialization, version, factory, typed proto, context).
     */
    private void addCommonImplMethods(TypeSpec.Builder classBuilder, String className,
                                       ClassName protoType, String implPackage, GenerationContext ctx) {
        // Serialization method
        classBuilder.addMethod(MethodSpec.methodBuilder("serializeToBytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(protoType, "proto")
                .addStatement("return proto.toByteArray()")
                .build());

        // Wrapper version ID method - returns string version identifier (e.g., "v1", "v2")
        classBuilder.addMethod(MethodSpec.methodBuilder("extractWrapperVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ClassName.get(String.class))
                .addParameter(protoType, "proto")
                .addStatement("return $S", ctx.requireVersion())
                .build());

        // Wrapper version method - returns numeric version (deprecated)
        classBuilder.addMethod(MethodSpec.methodBuilder("extractWrapperVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addParameter(protoType, "proto")
                .addStatement("return $L", ctx.getVersionNumber())
                .build());

        // Factory method
        classBuilder.addMethod(MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(implPackage, className))
                .addParameter(protoType, "proto")
                .addStatement("return new $L(proto)", className)
                .build());

        // getTypedProto() override with covariant return type (from ProtoWrapper via abstract class)
        classBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(protoType)
                .addStatement("return proto")
                .addJavadoc("Get the underlying typed proto message.\n")
                .addJavadoc("@return $T\n", protoType)
                .build());

        // getVersionContext() implementation - returns VersionContext for this version
        String version = ctx.requireVersion();
        String versionContextClassName = "VersionContext" + version.toUpperCase();
        ClassName versionContextType = ClassName.get(ctx.getApiPackage(), "VersionContext");
        ClassName versionContextImplType = ClassName.get(implPackage, versionContextClassName);
        classBuilder.addMethod(MethodSpec.methodBuilder("getVersionContext")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(versionContextType)
                .addStatement("return $T.INSTANCE", versionContextImplType)
                .build());
    }

    /**
     * Generate implementation class for a specific version.
     * @param message Merged message info
     * @param version Protocol version (e.g., "v1", "v2")
     * @param protoClassName Fully qualified proto class name
     * @return Generated JavaFile
     * @deprecated Use {@link #generate(MergedMessage, String, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public JavaFile generate(MergedMessage message, String version, String protoClassName) {
        if (typeResolver == null) {
            throw new IllegalStateException("Schema not set. Call setSchema() first or use generate(message, protoClassName, ctx)");
        }
        GenerationContext ctx = GenerationContext.forVersion(typeResolver.getSchema(), config, version);
        return generate(message, protoClassName, ctx);
    }

    /**
     * Generate a nested impl class as a static inner class.
     */
    private TypeSpec generateNestedImplClass(MergedMessage nested, MergedMessage parent,
                                              String protoClassName, GenerationContext ctx) {
        String version = ctx.requireVersion();
        String className = nested.getName();

        ClassName protoType = ClassName.bestGuess(protoClassName);

        ClassName parentAbstractClass = ClassName.get(
                ctx.getApiPackage() + IMPL_PACKAGE_SUFFIX,
                parent.getTopLevelParent().getAbstractClassName()
        );
        ClassName abstractClass = buildNestedAbstractClassName(nested, parentAbstractClass);
        ParameterizedTypeName superType = ParameterizedTypeName.get(abstractClass, protoType);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(superType)
                .addJavadoc("$L implementation of $L interface.\n\n",
                        version.toUpperCase(), nested.getQualifiedInterfaceName());

        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(protoType, "proto")
                .addStatement("super(proto)")
                .build());

        // Implement extract methods for nested class using the handler chain
        ProcessingContext nestedProcCtx = ProcessingContext.forImpl(nested, protoType, ctx, config);
        FieldProcessingChain.getInstance().addExtractImplementations(classBuilder, nested, version, nestedProcCtx);

        // Add oneof extract implementations for nested class
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : nested.getOneofGroups()) {
            classBuilder.addMethod(oneofGenerator.generateImplExtractCaseMethod(
                    oneof, nested, protoType, version, ctx.getApiPackage()));
        }

        classBuilder.addMethod(MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", className))
                .addParameter(protoType, "proto")
                .addStatement("return new $L(proto)", className)
                .build());

        // getTypedProto() for nested types - required for builder extractProto() reflection
        classBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addModifiers(Modifier.PUBLIC)
                .returns(protoType)
                .addStatement("return proto")
                .addJavadoc("Get the underlying typed proto message.\n")
                .addJavadoc("@return $T\n", protoType)
                .build());

        // Add builder support for nested impl class if enabled
        if (config.isGenerateBuilders()) {
            addNestedBuilderImpl(classBuilder, nested, protoType, className, ctx);
        }

        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            if (!deeplyNested.getPresentInVersions().contains(version)) {
                continue;
            }
            String deeplyNestedProtoClassName = protoClassName + "." + deeplyNested.getName();
            TypeSpec deeplyNestedClass = generateNestedImplClass(deeplyNested, nested, deeplyNestedProtoClassName, ctx);
            classBuilder.addType(deeplyNestedClass);
        }

        return classBuilder.build();
    }

    private ClassName buildNestedAbstractClassName(MergedMessage nested, ClassName topLevelAbstract) {
        // Build path from nested to parent using Stream.iterate(), then reverse and reduce
        return Stream.iterate(nested, m -> m.getParent() != null, MergedMessage::getParent)
                .map(MergedMessage::getName)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        names -> {
                            Collections.reverse(names);
                            return names.stream().reduce(topLevelAbstract, ClassName::nestedClass, (a, b) -> b);
                        }
                ));
    }

    private void addNestedBuilderImpl(TypeSpec.Builder classBuilder, MergedMessage nested,
                                        ClassName protoType, String className, GenerationContext ctx) {
        // Get the parent abstract class and find nested abstract class for context creation
        ClassName topLevelAbstract = ClassName.get(ctx.getApiPackage() + IMPL_PACKAGE_SUFFIX,
                nested.getTopLevelParent().getAbstractClassName());
        ClassName abstractClass = buildNestedAbstractClassName(nested, topLevelAbstract);
        ClassName abstractBuilderType = abstractClass.nestedClass("AbstractBuilder");

        // Create unified context for builder generation
        BuilderImplContext builderCtx = BuilderImplContext.forNested(
                nested, protoType, abstractBuilderType, className, ctx, config);

        ClassName builderInterfaceType = builderCtx.builderInterfaceType();

        // createBuilder() implementation - copies values from current instance
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl(proto.toBuilder())")
                .build());

        // createEmptyBuilder() implementation - creates empty builder (doesn't copy values)
        classBuilder.addMethod(MethodSpec.methodBuilder("createEmptyBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl($T.newBuilder())", protoType)
                .build());

        // Static newBuilder() factory method
        classBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl($T.newBuilder())", protoType)
                .build());

        // Generate BuilderImpl class using unified method
        TypeSpec builderImpl = generateBuilderImplClass(builderCtx);
        classBuilder.addType(builderImpl);
    }

    /**
     * Generate BuilderImpl class using unified context.
     * This method is used by both top-level and nested builder generation.
     */
    private TypeSpec generateBuilderImplClass(BuilderImplContext ctx) {
        MergedMessage message = ctx.message();
        ClassName protoType = ctx.protoType();
        ClassName protoBuilderType = ctx.protoBuilderType();
        ClassName abstractBuilderType = ctx.abstractBuilderType();
        ClassName interfaceType = ctx.interfaceType();
        String implClassName = ctx.implClassName();
        String version = ctx.version();
        TypeResolver resolver = ctx.resolver();
        GenerationContext genCtx = ctx.genCtx();

        // BuilderImpl extends AbstractBuilder<ProtoType>
        ParameterizedTypeName superType = ParameterizedTypeName.get(abstractBuilderType, protoType);

        TypeSpec.Builder builder = TypeSpec.classBuilder("BuilderImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(superType);

        // Field: proto builder
        builder.addField(protoBuilderType, "protoBuilder", Modifier.PRIVATE, Modifier.FINAL);

        // Constructor
        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(protoBuilderType, "protoBuilder")
                .addStatement("this.protoBuilder = protoBuilder")
                .build());

        // getVersionId() - returns the version identifier for this builder (e.g., "v1", "v2")
        String versionId = genCtx.requireVersion();
        builder.addMethod(MethodSpec.methodBuilder("getVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ClassName.get(String.class))
                .addStatement("return $S", versionId)
                .build());

        // getVersion() - returns the version number for this builder (deprecated)
        int versionNumber = genCtx.getVersionNumber();
        builder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addStatement("return $L", versionNumber)
                .build());

        // Implement doSet/doClear/doAdd methods for each field using handler chain
        ProcessingContext procCtx = ProcessingContext.forImpl(message, protoType, genCtx, config);
        FieldProcessingChain.getInstance().addBuilderImplMethods(builder, message, version, procCtx);

        // Add oneof builder methods
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            builder.addMethod(oneofGenerator.generateImplBuilderDoClear(oneof, version));
        }

        // doBuild() implementation
        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(interfaceType)
                .addStatement("return new $L(protoBuilder.build())", implClassName)
                .build());

        // Helper method to extract proto from wrapper - only needed for message fields (not map)
        boolean hasMessageFields = message.getFields().stream()
                .anyMatch(f -> f.isMessage() && !f.isMap());
        if (hasMessageFields) {
            builder.addMethod(MethodSpec.methodBuilder("extractProto")
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(Object.class, "wrapper")
                    .returns(MESSAGE_CLASS)
                    .beginControlFlow("if (wrapper == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .beginControlFlow("try")
                    .addStatement("$T method = wrapper.getClass().getMethod($S)", java.lang.reflect.Method.class, "getTypedProto")
                    .addStatement("return ($T) method.invoke(wrapper)", MESSAGE_CLASS)
                    .nextControlFlow("catch ($T e)", Exception.class)
                    .addStatement("throw new $T($S + wrapper.getClass(), e)",
                            RuntimeException.class, "Cannot extract proto from ")
                    .endControlFlow()
                    .build());
        }

        return builder.build();
    }

    private void addBuilderImpl(TypeSpec.Builder classBuilder, MergedMessage message,
                                 ClassName protoType, String className, String implPackage, GenerationContext ctx) {
        // Create unified context for builder generation
        BuilderImplContext builderCtx = BuilderImplContext.forTopLevel(
                message, protoType, className, ctx, config);

        ClassName builderInterfaceType = builderCtx.builderInterfaceType();

        // createBuilder() implementation - copies values from current instance
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl(proto.toBuilder())")
                .build());

        // createEmptyBuilder() implementation - creates empty builder (doesn't copy values)
        classBuilder.addMethod(MethodSpec.methodBuilder("createEmptyBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl($T.newBuilder())", protoType)
                .build());

        // Static newBuilder() factory method
        classBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl($T.newBuilder())", protoType)
                .build());

        // Generate BuilderImpl class using unified method
        TypeSpec builderImpl = generateBuilderImplClass(builderCtx);
        classBuilder.addType(builderImpl);
    }

    /**
     * Generate and write implementation class.
     * @param message Merged message info
     * @param version Protocol version (e.g., "v1", "v2")
     * @param protoClassName Fully qualified proto class name
     * @return Path to the generated file
     * @throws IOException if writing fails
     * @deprecated Use {@link #generateAndWrite(MergedMessage, String, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public Path generateAndWrite(MergedMessage message, String version, String protoClassName) throws IOException {
        JavaFile javaFile = generate(message, version, protoClassName);
        writeToFile(javaFile);

        String implPackage = config.getImplPackage(version);
        String relativePath = implPackage.replace('.', '/')
                + "/" + config.getImplClassName(message.getName(), version) + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write implementation class using context.
     *
     * @param message the merged message
     * @param protoClassName the proto class name
     * @param ctx the generation context
     * @return the path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite(MergedMessage message, String protoClassName, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, protoClassName, ctx);
        writeToFile(javaFile);

        String relativePath = ctx.getImplPackage().replace('.', '/')
                + "/" + ctx.getImplClassName(message.getName()) + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

}
