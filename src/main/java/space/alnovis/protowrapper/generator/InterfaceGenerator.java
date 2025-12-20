package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates version-agnostic Java interfaces from merged schema.
 *
 * <p>Example output:</p>
 * <pre>
 * public interface Money {
 *     long getBills();
 *     int getCoins();
 *     long toKopecks();
 * }
 * </pre>
 */
public class InterfaceGenerator extends BaseGenerator<MergedMessage> {

    // Legacy field - kept for backward compatibility
    @Deprecated
    private TypeResolver typeResolver;

    public InterfaceGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @param schema The merged schema
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
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
            MethodSpec getter = generateGetter(field, message, resolver);
            interfaceBuilder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                interfaceBuilder.addMethod(generateHasMethod(field, resolver));
            }
        }

        // Add nested enums first
        for (MergedEnum nestedEnum : message.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            interfaceBuilder.addType(enumSpec);
        }

        // Add nested interfaces for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            TypeSpec nestedInterface = generateNestedInterface(nested, resolver);
            interfaceBuilder.addType(nestedInterface);
        }

        // Add common utility methods
        addCommonMethods(interfaceBuilder, message);

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(ctx.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate interface for a merged message.
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
    public JavaFile generate(MergedMessage message) {
        if (typeResolver == null) {
            throw new IllegalStateException("Schema not set. Call setSchema() first or use generate(message, ctx)");
        }
        GenerationContext ctx = GenerationContext.create(typeResolver.getSchema(), config);
        return generate(message, ctx);
    }

    private MethodSpec generateGetter(MergedField field, MergedMessage message, TypeResolver resolver) {
        TypeName returnType = resolver.parseFieldType(field, message);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(field.getGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(returnType);

        if (!field.isUniversal(message.getPresentInVersions())) {
            builder.addJavadoc("@return $L value, or null if not present in this version\n",
                    field.getJavaName());
            builder.addJavadoc("@apiNote Present in versions: $L\n", field.getPresentInVersions());
        } else {
            builder.addJavadoc("@return $L value\n", field.getJavaName());
        }

        return builder.build();
    }

    private MethodSpec generateHasMethod(MergedField field, TypeResolver resolver) {
        return MethodSpec.methodBuilder("has" + resolver.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if $L is present.\n", field.getJavaName())
                .addJavadoc("@return true if the field has a value\n")
                .build();
    }

    private TypeSpec generateNestedInterface(MergedMessage nested, TypeResolver resolver) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(nested.getInterfaceName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Nested interface for $L.\n", nested.getName());

        for (MergedField field : nested.getFieldsSorted()) {
            TypeName returnType = resolver.parseFieldType(field, nested);
            MethodSpec getter = MethodSpec.methodBuilder(field.getGetterName())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(returnType)
                    .build();
            builder.addMethod(getter);

            if (field.isOptional() && !field.isRepeated()) {
                builder.addMethod(MethodSpec.methodBuilder("has" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(TypeName.BOOLEAN)
                        .build());
            }
        }

        for (MergedEnum nestedEnum : nested.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            builder.addType(enumSpec);
        }

        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            TypeSpec deeplyNestedInterface = generateNestedInterface(deeplyNested, resolver);
            builder.addType(deeplyNestedInterface);
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

    private void addCommonMethods(TypeSpec.Builder builder, MergedMessage message) {
        builder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the wrapper protocol version this instance was created from.\n")
                .addJavadoc("@return Protocol version (e.g., 1, 2)\n")
                .build());

        builder.addMethod(MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Serialize to protobuf bytes.\n")
                .addJavadoc("@return Protobuf-encoded bytes\n")
                .build());

        TypeVariableName typeVar = TypeVariableName.get("T", ClassName.get(config.getApiPackage(), message.getInterfaceName()));
        builder.addMethod(MethodSpec.methodBuilder("asVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Convert to a specific version implementation.\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                .build());
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
     * @deprecated Use {@link #generateAndWrite(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
    public Path generateAndWrite(MergedMessage message) throws IOException {
        JavaFile javaFile = generate(message);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
