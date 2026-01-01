package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedOneof;
import space.alnovis.protowrapper.model.MergedSchema;

import space.alnovis.protowrapper.generator.builder.BuilderInterfaceGenerator;
import space.alnovis.protowrapper.generator.oneof.OneofGenerator;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates version-agnostic Java interfaces from merged schema.
 *
 * <p>This class orchestrates interface generation by delegating to specialized generators:</p>
 * <ul>
 *   <li>{@link InterfaceMethodGenerator} - field getters, has/supports methods</li>
 *   <li>{@link InterfaceCommonMethodGenerator} - common utility methods (toBytes, asVersion, etc.)</li>
 *   <li>{@link BuilderInterfaceGenerator} - nested Builder interface</li>
 *   <li>{@link OneofGenerator} - oneof case enums and discriminators</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * public interface Money {
 *     long getBills();
 *     int getCoins();
 *     long toKopecks();
 * }
 * </pre>
 *
 * @see InterfaceMethodGenerator
 * @see InterfaceCommonMethodGenerator
 */
public class InterfaceGenerator extends BaseGenerator<MergedMessage> {

    private final InterfaceMethodGenerator methodGenerator;
    private final InterfaceCommonMethodGenerator commonMethodGenerator;

    /**
     * Legacy field - kept for backward compatibility.
     * @deprecated Use {@link GenerationContext} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    private TypeResolver typeResolver;

    public InterfaceGenerator(GeneratorConfig config) {
        super(config);
        this.methodGenerator = new InterfaceMethodGenerator(config);
        this.commonMethodGenerator = new InterfaceCommonMethodGenerator(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @param schema The merged schema
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public void setSchema(MergedSchema schema) {
        this.typeResolver = new TypeResolver(config, schema);
    }

    /**
     * Generate interface for a merged message using context.
     *
     * @param message Merged message info
     * @param ctx Generation context
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(message.getInterfaceName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Version-agnostic interface for $L.\n\n", message.getName())
                .addJavadoc("<p>Supported in versions: $L</p>\n", message.getPresentInVersions());

        // Add getter methods for all fields
        for (MergedField field : message.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                methodGenerator.generateMapFieldMethods(interfaceBuilder, field, resolver);

                // Add supportsXxx() for version-specific fields
                if (!field.isUniversal(message.getPresentInVersions())) {
                    interfaceBuilder.addMethod(methodGenerator.generateSupportsMethod(field, message, resolver));
                }
                continue;
            }

            MethodSpec getter = methodGenerator.generateGetter(field, message, resolver);
            interfaceBuilder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                interfaceBuilder.addMethod(methodGenerator.generateHasMethod(field, resolver));
            }

            // Add supportsXxx() for version-specific fields or fields with conflicts
            if (!field.isUniversal(message.getPresentInVersions()) ||
                (field.getConflictType() != null && field.getConflictType() != MergedField.ConflictType.NONE)) {
                interfaceBuilder.addMethod(methodGenerator.generateSupportsMethod(field, message, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields (scalar only - repeated handled differently)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName())
                        .map(enumInfo -> methodGenerator.generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(interfaceBuilder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = methodGenerator.generateBytesGetter(field, resolver);
                interfaceBuilder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = methodGenerator.generateMessageGetter(field, message, resolver);
                if (messageGetter != null) {
                    interfaceBuilder.addMethod(messageGetter);
                    // Also add supportsXxxMessage() method
                    interfaceBuilder.addMethod(methodGenerator.generateSupportsMessageMethod(field, resolver));
                }
            }
        }

        // Add oneof Case enums and discriminator methods
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            // Generate Case enum as nested type
            interfaceBuilder.addType(oneofGenerator.generateCaseEnum(oneof));

            // Add discriminator method getXxxCase()
            interfaceBuilder.addMethod(oneofGenerator.generateInterfaceCaseGetter(oneof, message));

            // Note: hasXxx() methods for oneof fields are generated by normal field processing
            // since oneof fields are regular fields with isInOneof() flag
        }

        // Add nested enums first
        message.getNestedEnums().stream()
                .map(this::generateNestedEnum)
                .forEach(interfaceBuilder::addType);

        // Add nested interfaces for nested messages
        message.getNestedMessages().stream()
                .map(nested -> generateNestedInterface(nested, ctx))
                .forEach(interfaceBuilder::addType);

        // Add common utility methods
        commonMethodGenerator.addCommonMethods(interfaceBuilder, message);

        // Add Builder interface if enabled
        if (config.isGenerateBuilders()) {
            interfaceBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a builder initialized with this instance's values.\n")
                    .addJavadoc("@return Builder for creating modified copies\n")
                    .build());

            BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
            TypeSpec builderInterface = builderGen.generate(message, resolver, ctx);
            interfaceBuilder.addType(builderInterface);

            // Add static newBuilder(VersionContext ctx) method
            ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
            ClassName builderType = ClassName.get(config.getApiPackage(), message.getInterfaceName())
                    .nestedClass("Builder");
            String builderMethodName = "new" + message.getName() + "Builder";

            interfaceBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(versionContextType, "ctx")
                    .returns(builderType)
                    .addJavadoc("Create a new builder for $L using the specified version context.\n", message.getName())
                    .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L()}.</p>\n", builderMethodName)
                    .addJavadoc("@param ctx Version context to use for builder creation\n")
                    .addJavadoc("@return Empty builder for $L\n", message.getName())
                    .addStatement("return ctx.$L()", builderMethodName)
                    .build());
        }

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(ctx.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate interface for a merged message.
     * @param message Merged message info
     * @return Generated JavaFile
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public JavaFile generate(MergedMessage message) {
        if (typeResolver == null) {
            throw new IllegalStateException("Schema not set. Call setSchema() first or use generate(message, ctx)");
        }
        GenerationContext ctx = GenerationContext.create(typeResolver.getSchema(), config);
        return generate(message, ctx);
    }

    // Method generation delegated to InterfaceMethodGenerator
    // Utility methods delegated to InterfaceUtilityGenerator

    private TypeSpec generateNestedInterface(MergedMessage nested, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(nested.getInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested interface for $L.\n", nested.getName());

        for (MergedField field : nested.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                methodGenerator.generateMapFieldMethods(builder, field, resolver);
                continue;
            }

            // Use shared generateGetter for consistent Javadoc including conflict info
            MethodSpec getter = methodGenerator.generateGetter(field, nested, resolver);
            builder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                builder.addMethod(methodGenerator.generateHasMethod(field, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName())
                        .map(enumInfo -> methodGenerator.generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(builder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = methodGenerator.generateBytesGetter(field, resolver);
                builder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields in nested messages (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = methodGenerator.generateMessageGetter(field, nested, resolver);
                if (messageGetter != null) {
                    builder.addMethod(messageGetter);
                    builder.addMethod(methodGenerator.generateSupportsMessageMethod(field, resolver));
                }
            }
        }

        nested.getNestedEnums().stream()
                .map(this::generateNestedEnum)
                .forEach(builder::addType);

        nested.getNestedMessages().stream()
                .map(deeplyNested -> generateNestedInterface(deeplyNested, ctx))
                .forEach(builder::addType);

        // Add Builder interface for nested message if enabled
        if (config.isGenerateBuilders()) {
            builder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .build());

            builder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a new empty builder of the same version as this instance.\n")
                    .build());

            BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
            TypeSpec nestedBuilder = builderGen.generateForNested(nested, resolver, ctx);
            builder.addType(nestedBuilder);

            // Add static newBuilder(VersionContext ctx) method for nested interface
            ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
            String builderMethodName = GeneratorUtils.buildNestedBuilderMethodName(nested);
            String qualifiedName = GeneratorUtils.buildNestedQualifiedName(nested);

            builder.addMethod(MethodSpec.methodBuilder("newBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(versionContextType, "ctx")
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a new builder for $L using the specified version context.\n", qualifiedName)
                    .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L()}.</p>\n", builderMethodName)
                    .addJavadoc("@param ctx Version context to use for builder creation\n")
                    .addJavadoc("@return Empty builder for $L\n", qualifiedName)
                    .addStatement("return ctx.$L()", builderMethodName)
                    .build());
        }

        return builder.build();
    }


    private TypeSpec generateNestedEnum(MergedEnum enumInfo) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested enum for $L.\n", enumInfo.getName());

        enumBuilder.addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL);

        enumBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value")
                .build());

        enumBuilder.addMethod(MethodSpec.methodBuilder("getValue")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return value")
                .build());

        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProtoValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", enumInfo.getName()))
                .addParameter(TypeName.INT, "value")
                .beginControlFlow("for ($L e : values())", enumInfo.getName())
                .beginControlFlow("if (e.value == value)")
                .addStatement("return e")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return null")
                .build());

        for (MergedEnumValue value : enumInfo.getValues()) {
            enumBuilder.addEnumConstant(value.getName(),
                    TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
        }

        return enumBuilder.build();
    }


    /**
     * Generate and write interface using context.
     */
    public Path generateAndWrite(MergedMessage message, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, ctx);
        writeToFile(javaFile);

        String relativePath = ctx.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write interface.
     * @param message Merged message info
     * @return Path to the generated file
     * @throws IOException if writing fails
     * @deprecated Use {@link #generateAndWrite(MergedMessage, GenerationContext)} instead. Will be removed in version 2.0.0.
     */
    @Deprecated(forRemoval = true, since = "1.2.0")
    public Path generateAndWrite(MergedMessage message) throws IOException {
        JavaFile javaFile = generate(message);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

}
