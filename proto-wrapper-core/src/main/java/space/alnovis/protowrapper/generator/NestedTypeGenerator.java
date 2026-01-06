package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.builder.BuilderInterfaceGenerator;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;

import javax.lang.model.element.Modifier;

/**
 * Generates nested types (interfaces and enums) for message interfaces.
 *
 * <p>This class handles the generation of:</p>
 * <ul>
 *   <li>Nested interfaces for nested message types</li>
 *   <li>Nested enums defined within messages</li>
 *   <li>Builder interfaces for nested messages (when enabled)</li>
 * </ul>
 *
 * <p>Extracted from {@link InterfaceGenerator} to reduce class size
 * and improve separation of concerns.</p>
 *
 * @see InterfaceGenerator
 * @see InterfaceMethodGenerator
 */
public final class NestedTypeGenerator {

    private final GeneratorConfig config;
    private final InterfaceMethodGenerator methodGenerator;

    /**
     * Create a new NestedTypeGenerator.
     *
     * @param config the generator configuration
     * @param methodGenerator the method generator for field methods
     */
    public NestedTypeGenerator(GeneratorConfig config, InterfaceMethodGenerator methodGenerator) {
        this.config = config;
        this.methodGenerator = methodGenerator;
    }

    /**
     * Generate a nested interface for a nested message.
     *
     * @param nested The nested message
     * @param ctx Generation context
     * @return Generated TypeSpec for the nested interface
     */
    public TypeSpec generateNestedInterface(MergedMessage nested, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(nested.getInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested interface for $L.\n", nested.getName());

        // Add field methods
        addFieldMethods(builder, nested, ctx, resolver);

        // Add nested enums
        nested.getNestedEnums().stream()
                .map(this::generateNestedEnum)
                .forEach(builder::addType);

        // Add deeply nested interfaces (recursive)
        nested.getNestedMessages().stream()
                .map(deeplyNested -> generateNestedInterface(deeplyNested, ctx))
                .forEach(builder::addType);

        // Add Builder interface if enabled
        if (config.isGenerateBuilders()) {
            addBuilderSupport(builder, nested, resolver, ctx);
        }

        return builder.build();
    }

    /**
     * Generate a nested enum type.
     *
     * @param enumInfo The enum information
     * @return Generated TypeSpec for the enum
     */
    public TypeSpec generateNestedEnum(MergedEnum enumInfo) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.getName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested enum for $L.\n", enumInfo.getName());

        // Add value field
        enumBuilder.addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL);

        // Add constructor
        enumBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value")
                .build());

        // Add getValue() method
        enumBuilder.addMethod(MethodSpec.methodBuilder("getValue")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return value")
                .build());

        // Add fromProtoValue() static method
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

        // Add enum constants
        for (MergedEnumValue value : enumInfo.getValues()) {
            enumBuilder.addEnumConstant(value.getName(),
                    TypeSpec.anonymousClassBuilder("$L", value.getNumber()).build());
        }

        return enumBuilder.build();
    }

    private void addFieldMethods(TypeSpec.Builder builder, MergedMessage nested,
                                  GenerationContext ctx, TypeResolver resolver) {
        for (MergedField field : nested.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                methodGenerator.generateMapFieldMethods(builder, field, resolver);
                continue;
            }

            // Use shared generateGetter for consistent Javadoc including conflict info
            MethodSpec getter = methodGenerator.generateGetter(field, nested, resolver);
            builder.addMethod(getter);

            // Add raw proto accessor for well-known type fields when enabled
            if (methodGenerator.shouldGenerateRawProtoAccessor(field)) {
                if (field.isRepeated()) {
                    builder.addMethod(methodGenerator.generateRepeatedRawProtoAccessor(field, resolver));
                } else {
                    builder.addMethod(methodGenerator.generateRawProtoAccessor(field, resolver));
                }
            }

            if (field.isOptional() && !field.isRepeated()) {
                builder.addMethod(methodGenerator.generateHasMethod(field, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName())
                        .map(enumInfo -> methodGenerator.generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(builder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = methodGenerator.generateBytesGetter(field, resolver);
                builder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = methodGenerator.generateMessageGetter(field, nested, resolver);
                if (messageGetter != null) {
                    builder.addMethod(messageGetter);
                    builder.addMethod(methodGenerator.generateSupportsMessageMethod(field, resolver));
                }
            }
        }
    }

    private void addBuilderSupport(TypeSpec.Builder builder, MergedMessage nested,
                                    TypeResolver resolver, GenerationContext ctx) {
        // Add toBuilder() method
        builder.addMethod(MethodSpec.methodBuilder("toBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("", "Builder"))
                .build());

        // Add emptyBuilder() method
        builder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("", "Builder"))
                .addJavadoc("Create a new empty builder of the same version as this instance.\n")
                .build());

        // Generate nested Builder interface
        BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
        TypeSpec nestedBuilder = builderGen.generateForNested(nested, resolver, ctx);
        builder.addType(nestedBuilder);

        // Add static newBuilder(VersionContext ctx) method
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
}
