package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.Optional;

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

            // Add enum getter for INT_ENUM conflict fields
            if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    ConflictEnumInfo enumInfo = enumInfoOpt.get();
                    MethodSpec enumGetter = generateEnumGetter(field, enumInfo, resolver);
                    interfaceBuilder.addMethod(enumGetter);
                }
            }
        }

        // Add nested enums first
        for (MergedEnum nestedEnum : message.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            interfaceBuilder.addType(enumSpec);
        }

        // Add nested interfaces for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            TypeSpec nestedInterface = generateNestedInterface(nested, ctx);
            interfaceBuilder.addType(nestedInterface);
        }

        // Add common utility methods
        addCommonMethods(interfaceBuilder, message);

        // Add Builder interface if enabled
        if (config.isGenerateBuilders()) {
            interfaceBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a builder initialized with this instance's values.\n")
                    .addJavadoc("@return Builder for creating modified copies\n")
                    .build());

            TypeSpec builderInterface = generateBuilderInterface(message, resolver, ctx);
            interfaceBuilder.addType(builderInterface);
        }

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

    /**
     * Generate enum getter for INT_ENUM conflict field.
     */
    private MethodSpec generateEnumGetter(MergedField field, ConflictEnumInfo enumInfo, TypeResolver resolver) {
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
        String methodName = "get" + resolver.capitalize(field.getJavaName()) + "Enum";

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(enumType)
                .addJavadoc("Get $L as unified enum.\n", field.getJavaName())
                .addJavadoc("<p>This method provides type-safe enum access for a field that has\n")
                .addJavadoc("different types across protocol versions (int in some, enum in others).</p>\n")
                .addJavadoc("@return Unified enum value, or null if the value is not recognized\n")
                .build();
    }

    private TypeSpec generateNestedInterface(MergedMessage nested, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
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

            // Add enum getter for INT_ENUM conflict fields in nested messages
            if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    ConflictEnumInfo enumInfo = enumInfoOpt.get();
                    MethodSpec enumGetter = generateEnumGetter(field, enumInfo, resolver);
                    builder.addMethod(enumGetter);
                }
            }
        }

        for (MergedEnum nestedEnum : nested.getNestedEnums()) {
            TypeSpec enumSpec = generateNestedEnum(nestedEnum);
            builder.addType(enumSpec);
        }

        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            TypeSpec deeplyNestedInterface = generateNestedInterface(deeplyNested, ctx);
            builder.addType(deeplyNestedInterface);
        }

        // Add Builder interface for nested message if enabled
        if (config.isGenerateBuilders()) {
            builder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .build());

            TypeSpec nestedBuilder = generateNestedBuilderInterface(nested, resolver);
            builder.addType(nestedBuilder);
        }

        return builder.build();
    }

    private TypeSpec generateNestedBuilderInterface(MergedMessage nested, TypeResolver resolver) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for (MergedField field : nested.getFieldsSorted()) {
            // Skip builder methods for fields with non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, nested);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);

                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get("", "Builder"))
                        .build());
            } else {
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .build());

                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get("", "Builder"))
                            .build());
                }
            }
        }

        // Return type is the nested interface itself
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("", nested.getInterfaceName()))
                .build());

        return builder.build();
    }

    private TypeSpec generateBuilderInterface(MergedMessage message, TypeResolver resolver, GenerationContext ctx) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Builder for creating and modifying $L instances.\n", message.getInterfaceName());

        // Add setter methods for each field
        for (MergedField field : message.getFieldsSorted()) {
            // Handle INT_ENUM conflicts with overloaded setters
            if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), resolver);
                    continue;
                }
            }

            // Skip builder methods for fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                // Add Javadoc note about skipped field
                builder.addJavadoc("\n<p><b>Note:</b> {@code $L} setter not available due to type conflict ($L).</p>\n",
                        field.getJavaName(), field.getConflictType());
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, message);

            if (field.isRepeated()) {
                // For repeated fields: add, addAll, set (replace all), clear
                TypeName singleElementType = extractListElementType(fieldType);

                // add single element
                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Add a single $L element.\n", field.getJavaName())
                        .build());

                // addAll
                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Add all $L elements.\n", field.getJavaName())
                        .build());

                // set (replace all)
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Replace all $L elements.\n", field.getJavaName())
                        .build());

                // clear
                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Clear all $L elements.\n", field.getJavaName())
                        .build());
            } else {
                // Regular setter
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(ClassName.get("", "Builder"))
                        .addJavadoc("Set $L value.\n", field.getJavaName())
                        .build());

                // Clear method for optional fields
                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(ClassName.get("", "Builder"))
                            .addJavadoc("Clear $L value.\n", field.getJavaName())
                            .build());
                }
            }
        }

        // Add build method
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(config.getApiPackage(), message.getInterfaceName()))
                .addJavadoc("Build the $L instance.\n", message.getInterfaceName())
                .addJavadoc("@return New immutable instance\n")
                .build());

        return builder.build();
    }

    /**
     * Add overloaded builder methods for INT_ENUM conflict field.
     * Generates both int and enum setters.
     */
    private void addIntEnumBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           ConflictEnumInfo enumInfo, TypeResolver resolver) {
        String methodName = "set" + resolver.capitalize(field.getJavaName());
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
        ClassName builderType = ClassName.get("", "Builder");

        // int setter
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value using int.\n", field.getJavaName())
                .addJavadoc("@param $L The numeric value\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // enum setter
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(enumType, field.getJavaName())
                .returns(builderType)
                .addJavadoc("Set $L value using unified enum.\n", field.getJavaName())
                .addJavadoc("@param $L The enum value\n", field.getJavaName())
                .addJavadoc("@return This builder\n")
                .build());

        // clear method
        builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderType)
                .addJavadoc("Clear $L value.\n", field.getJavaName())
                .build());
    }

    /**
     * Extract element type from List<T> type.
     */
    private TypeName extractListElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterized = (ParameterizedTypeName) listType;
            if (!parameterized.typeArguments.isEmpty()) {
                return parameterized.typeArguments.get(0);
            }
        }
        // Fallback to Object
        return ClassName.get(Object.class);
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
