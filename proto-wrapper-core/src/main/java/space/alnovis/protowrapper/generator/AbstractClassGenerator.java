package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Generates abstract base classes using template method pattern.
 *
 * <p>Example output:</p>
 * <pre>
 * public abstract class AbstractMoney&lt;PROTO extends Message&gt;
 *         extends AbstractProtoWrapper&lt;PROTO&gt;
 *         implements Money {
 *
 *     protected AbstractMoney(PROTO proto) {
 *         super(proto);
 *     }
 *
 *     protected abstract long extractBills(PROTO proto);
 *     protected abstract int extractCoins(PROTO proto);
 *
 *     &#64;Override
 *     public final long getBills() {
 *         return extractBills(proto);
 *     }
 *
 *     &#64;Override
 *     public final int getCoins() {
 *         return extractCoins(proto);
 *     }
 * }
 * </pre>
 */
public class AbstractClassGenerator extends BaseGenerator<MergedMessage> {

    // Legacy field - kept for backward compatibility
    @Deprecated
    private TypeResolver typeResolver;

    public AbstractClassGenerator(GeneratorConfig config) {
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
     * Generate abstract class for a merged message using context.
     * Nested messages are generated as static inner classes.
     *
     * @param message Merged message info
     * @param ctx Generation context
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        return generateInternal(message, resolver, ctx);
    }

    /**
     * Generate abstract class for a merged message.
     * Nested messages are generated as static inner classes.
     *
     * @param message Merged message info
     * @return Generated JavaFile
     * @deprecated Use {@link #generate(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
    public JavaFile generate(MergedMessage message) {
        if (typeResolver == null) {
            throw new IllegalStateException("Schema not set. Call setSchema() first or use generate(message, ctx)");
        }
        GenerationContext ctx = GenerationContext.create(typeResolver.getSchema(), config);
        return generateInternal(message, typeResolver, ctx);
    }

    private JavaFile generateInternal(MergedMessage message, TypeResolver resolver, GenerationContext ctx) {
        String className = message.getAbstractClassName();
        String interfaceName = message.getInterfaceName();

        // Type parameter: PROTO extends Message
        TypeVariableName protoType = TypeVariableName.get("PROTO",
                MESSAGE_CLASS);

        // Interface: Money (or whatever)
        ClassName interfaceType = ClassName.get(config.getApiPackage(), interfaceName);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(interfaceType)
                .addJavadoc("Abstract base class for $L implementations.\n\n", interfaceName)
                .addJavadoc("<p>Uses template method pattern - subclasses implement extract* methods.</p>\n")
                .addJavadoc("@param <PROTO> Protocol-specific message type\n");

        // Add protected field for convenient access
        classBuilder.addField(FieldSpec.builder(protoType, "proto", Modifier.PROTECTED, Modifier.FINAL)
                .addJavadoc("The underlying proto message.\n")
                .build());

        // Constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(protoType, "proto")
                .addStatement("this.proto = proto")
                .build());

        // Add abstract extract methods
        for (MergedField field : message.getFieldsSorted()) {
            addExtractMethods(classBuilder, field, protoType, message, resolver, ctx);
        }

        // Add concrete getter implementations
        for (MergedField field : message.getFieldsSorted()) {
            addGetterImplementation(classBuilder, field, message, resolver, ctx);
        }

        // Add common methods
        addCommonMethods(classBuilder, message, protoType);

        // Add Builder support if enabled
        if (config.isGenerateBuilders()) {
            addBuilderSupport(classBuilder, message, protoType, interfaceType, resolver, ctx);
        }

        // Add nested abstract classes for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            TypeSpec nestedClass = generateNestedAbstractClass(nested, message, resolver, ctx);
            classBuilder.addType(nestedClass);
        }

        TypeSpec classSpec = classBuilder.build();

        return JavaFile.builder(config.getApiPackage() + IMPL_PACKAGE_SUFFIX, classSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate a nested abstract class as a static inner class.
     * Recursively handles deeply nested messages.
     */
    private TypeSpec generateNestedAbstractClass(MergedMessage nested, MergedMessage parent,
                                                  TypeResolver resolver, GenerationContext ctx) {
        String className = nested.getName();
        String qualifiedInterfaceName = nested.getQualifiedInterfaceName();

        // Type parameter: PROTO extends Message
        TypeVariableName protoType = TypeVariableName.get("PROTO",
                MESSAGE_CLASS);

        // Interface: ParentMessage.NestedMessage
        ClassName interfaceType = resolver.buildNestedClassName(qualifiedInterfaceName);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(interfaceType)
                .addJavadoc("Abstract base class for $L implementations.\n\n", qualifiedInterfaceName)
                .addJavadoc("<p>Uses template method pattern - subclasses implement extract* methods.</p>\n")
                .addJavadoc("@param <PROTO> Protocol-specific message type\n");

        // Add protected field for convenient access
        classBuilder.addField(FieldSpec.builder(protoType, "proto", Modifier.PROTECTED, Modifier.FINAL)
                .addJavadoc("The underlying proto message.\n")
                .build());

        // Constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(protoType, "proto")
                .addStatement("this.proto = proto")
                .build());

        // Add abstract extract methods
        for (MergedField field : nested.getFieldsSorted()) {
            addExtractMethods(classBuilder, field, protoType, nested, resolver, ctx);
        }

        // Add concrete getter implementations
        for (MergedField field : nested.getFieldsSorted()) {
            addGetterImplementation(classBuilder, field, nested, resolver, ctx);
        }

        // Recursively add deeply nested abstract classes
        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            TypeSpec deeplyNestedClass = generateNestedAbstractClass(deeplyNested, nested, resolver, ctx);
            classBuilder.addType(deeplyNestedClass);
        }

        // Add Builder support for nested message if enabled
        if (config.isGenerateBuilders()) {
            addNestedBuilderSupport(classBuilder, nested, interfaceType, resolver, ctx);
        }

        return classBuilder.build();
    }

    private void addNestedBuilderSupport(TypeSpec.Builder classBuilder, MergedMessage nested,
                                         ClassName interfaceType, TypeResolver resolver,
                                         GenerationContext ctx) {
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");

        // Abstract method to create builder
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(builderInterfaceType)
                .build());

        // toBuilder() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("return createBuilder()")
                .build());

        // Generate nested AbstractBuilder
        TypeSpec abstractBuilder = generateNestedAbstractBuilder(nested, interfaceType, resolver, ctx);
        classBuilder.addType(abstractBuilder);
    }

    private TypeSpec generateNestedAbstractBuilder(MergedMessage nested, ClassName interfaceType,
                                                     TypeResolver resolver, GenerationContext ctx) {
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");
        TypeVariableName protoType = TypeVariableName.get("PROTO", MESSAGE_CLASS);

        TypeSpec.Builder builder = TypeSpec.classBuilder("AbstractBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(builderInterfaceType);

        // Add abstract doSet/doClear methods
        for (MergedField field : nested.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumAbstractMethods(builder, field, enumInfoOpt.get(), resolver);
                    continue;
                }
            }

            // Handle WIDENING conflicts with wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningAbstractMethods(builder, field, resolver);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesAbstractMethods(builder, field, resolver);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, nested);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);

                builder.addMethod(MethodSpec.methodBuilder("doAdd" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("doAddAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("doSet" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("doClear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .build());
            } else {
                builder.addMethod(MethodSpec.methodBuilder("doSet" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("doClear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                            .build());
                }
            }
        }

        // Abstract doBuild
        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(interfaceType)
                .build());

        // Add concrete implementations
        for (MergedField field : nested.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumConcreteMethods(builder, field, enumInfoOpt.get(), resolver, builderInterfaceType);
                    continue;
                }
            }

            // Handle WIDENING conflicts with wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningConcreteMethods(builder, field, resolver, builderInterfaceType);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesConcreteMethods(builder, field, resolver, builderInterfaceType);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, nested);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);

                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doAdd$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doAddAll$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doSet$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(builderInterfaceType)
                        .addStatement("doClear$L()", resolver.capitalize(field.getJavaName()))
                        .addStatement("return this")
                        .build());
            } else {
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doSet$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(builderInterfaceType)
                            .addStatement("doClear$L()", resolver.capitalize(field.getJavaName()))
                            .addStatement("return this")
                            .build());
                }
            }
        }

        // build()
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(interfaceType)
                .addStatement("return doBuild()")
                .build());

        return builder.build();
    }

    private void addExtractMethods(TypeSpec.Builder classBuilder, MergedField field,
                                   TypeVariableName protoType, MergedMessage message,
                                   TypeResolver resolver, GenerationContext ctx) {
        TypeName returnType = resolver.parseFieldType(field, message);

        // For optional fields (not repeated), add extractHas method
        if (field.isOptional() && !field.isRepeated()) {
            classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(protoType, "proto")
                    .build());
        }

        // Main extract method
        classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(protoType, "proto")
                .build());

        // For INT_ENUM conflicts (scalar only), add enum extract method
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
            Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                    .getConflictEnum(message.getName(), field.getName());
            if (enumInfoOpt.isPresent()) {
                ConflictEnumInfo enumInfo = enumInfoOpt.get();
                ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
                String enumExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Enum";

                classBuilder.addMethod(MethodSpec.methodBuilder(enumExtractMethodName)
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .returns(enumType)
                        .addParameter(protoType, "proto")
                        .build());
            }
        }

        // For STRING_BYTES conflicts (scalar only), add bytes extract method
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
            String bytesExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Bytes";
            classBuilder.addMethod(MethodSpec.methodBuilder(bytesExtractMethodName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(ArrayTypeName.of(TypeName.BYTE))
                    .addParameter(protoType, "proto")
                    .build());
        }

        // For PRIMITIVE_MESSAGE conflicts (scalar only), add message extract method
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
            TypeName messageType = getMessageTypeForField(field, resolver);
            if (messageType != null) {
                String messageExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Message";
                classBuilder.addMethod(MethodSpec.methodBuilder(messageExtractMethodName)
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .returns(messageType)
                        .addParameter(protoType, "proto")
                        .build());
            }
        }
    }

    private void addGetterImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                         MergedMessage message, TypeResolver resolver,
                                         GenerationContext ctx) {
        TypeName returnType = resolver.parseFieldType(field, message);

        MethodSpec.Builder getter = MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);

        // For optional primitives, use has-check pattern to return boxed null
        if (field.needsHasCheck()) {
            getter.addStatement("return $L(proto) ? $L(proto) : null",
                    field.getExtractHasMethodName(), field.getExtractMethodName());
        } else {
            getter.addStatement("return $L(proto)", field.getExtractMethodName());
        }

        classBuilder.addMethod(getter.build());

        // Add hasXxx method for optional fields (not repeated)
        if (field.isOptional() && !field.isRepeated()) {
            MethodSpec has = MethodSpec.methodBuilder("has" + resolver.capitalize(field.getJavaName()))
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.BOOLEAN)
                    .addStatement("return $L(proto)", field.getExtractHasMethodName())
                    .build();
            classBuilder.addMethod(has);
        }

        // Add enum getter for INT_ENUM conflicts (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
            Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                    .getConflictEnum(message.getName(), field.getName());
            if (enumInfoOpt.isPresent()) {
                ConflictEnumInfo enumInfo = enumInfoOpt.get();
                ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
                String enumGetterName = "get" + resolver.capitalize(field.getJavaName()) + "Enum";
                String enumExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Enum";

                classBuilder.addMethod(MethodSpec.methodBuilder(enumGetterName)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(enumType)
                        .addStatement("return $L(proto)", enumExtractMethodName)
                        .build());
            }
        }

        // Add bytes getter for STRING_BYTES conflicts (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
            String bytesGetterName = "get" + resolver.capitalize(field.getJavaName()) + "Bytes";
            String bytesExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Bytes";

            classBuilder.addMethod(MethodSpec.methodBuilder(bytesGetterName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(ArrayTypeName.of(TypeName.BYTE))
                    .addStatement("return $L(proto)", bytesExtractMethodName)
                    .build());
        }

        // Add message getter for PRIMITIVE_MESSAGE conflicts (scalar only)
        if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
            TypeName messageType = getMessageTypeForField(field, resolver);
            if (messageType != null) {
                String messageGetterName = "get" + resolver.capitalize(field.getJavaName()) + "Message";
                String messageExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Message";

                classBuilder.addMethod(MethodSpec.methodBuilder(messageGetterName)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(messageType)
                        .addStatement("return $L(proto)", messageExtractMethodName)
                        .build());
            }
        }
    }

    private void addCommonMethods(TypeSpec.Builder classBuilder, MergedMessage message,
                                  TypeVariableName protoType) {
        // Abstract method for serialization
        classBuilder.addMethod(MethodSpec.methodBuilder("serializeToBytes")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(protoType, "proto")
                .build());

        // toBytes() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("toBytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addStatement("return serializeToBytes(proto)")
                .build());

        // Abstract getWrapperVersion - renamed to avoid conflict with protocol_version field
        classBuilder.addMethod(MethodSpec.methodBuilder("extractWrapperVersion")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addParameter(protoType, "proto")
                .build());

        // getWrapperVersion() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(TypeName.INT)
                .addStatement("return extractWrapperVersion(proto)")
                .build());

        // asVersion() implementation using converter
        String interfaceName = message.getInterfaceName();
        TypeVariableName typeVar = TypeVariableName.get("T",
                ClassName.get(config.getApiPackage(), interfaceName));

        classBuilder.addMethod(MethodSpec.methodBuilder("asVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addStatement("return convertToVersion(versionClass)")
                .build());

        // Protected helper method for conversion
        classBuilder.addMethod(MethodSpec.methodBuilder("convertToVersion")
                .addModifiers(Modifier.PROTECTED)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Override in subclass to implement version conversion.\n")
                .addStatement("throw new $T($S + versionClass)",
                        UnsupportedOperationException.class, "Version conversion not implemented for ")
                .build());

        // toString()
        classBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.format($S, getClass().getSimpleName(), getWrapperVersion())",
                        String.class, "%s[version=%d]")
                .build());
    }

    private void addBuilderSupport(TypeSpec.Builder classBuilder, MergedMessage message,
                                   TypeVariableName protoType, ClassName interfaceType,
                                   TypeResolver resolver, GenerationContext ctx) {
        // Builder interface type from the interface
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");

        // Abstract method to create builder
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(builderInterfaceType)
                .build());

        // toBuilder() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("return createBuilder()")
                .build());

        // Generate AbstractBuilder nested class
        TypeSpec abstractBuilder = generateAbstractBuilder(message, interfaceType, resolver, ctx);
        classBuilder.addType(abstractBuilder);
    }

    private TypeSpec generateAbstractBuilder(MergedMessage message, ClassName interfaceType,
                                              TypeResolver resolver, GenerationContext ctx) {
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");
        TypeVariableName protoType = TypeVariableName.get("PROTO", MESSAGE_CLASS);

        TypeSpec.Builder builder = TypeSpec.classBuilder("AbstractBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(builderInterfaceType)
                .addJavadoc("Abstract builder base class.\n")
                .addJavadoc("@param <PROTO> Protocol-specific message type\n");

        // Add abstract doSet/doClear/doAdd/doBuild methods
        for (MergedField field : message.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumAbstractMethods(builder, field, enumInfoOpt.get(), resolver);
                    continue;
                }
            }

            // Handle WIDENING conflicts with wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningAbstractMethods(builder, field, resolver);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesAbstractMethods(builder, field, resolver);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, message);

            if (field.isRepeated()) {
                // For repeated fields
                TypeName singleElementType = extractListElementType(fieldType);

                // doAdd
                builder.addMethod(MethodSpec.methodBuilder("doAdd" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(singleElementType, field.getJavaName())
                        .build());

                // doAddAll
                builder.addMethod(MethodSpec.methodBuilder("doAddAll" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                // doSet (replace all)
                builder.addMethod(MethodSpec.methodBuilder("doSet" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                // doClear
                builder.addMethod(MethodSpec.methodBuilder("doClear" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .build());
            } else {
                // doSet
                builder.addMethod(MethodSpec.methodBuilder("doSet" + resolver.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .addParameter(fieldType, field.getJavaName())
                        .build());

                // doClear for optional fields
                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("doClear" + resolver.capitalize(field.getJavaName()))
                            .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                            .build());
                }
            }
        }

        // Abstract doBuild method
        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(interfaceType)
                .build());

        // Add concrete implementations that delegate to abstract methods
        for (MergedField field : message.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    addIntEnumConcreteMethods(builder, field, enumInfoOpt.get(), resolver, builderInterfaceType);
                    continue;
                }
            }

            // Handle WIDENING conflicts with wider type (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                addWideningConcreteMethods(builder, field, resolver, builderInterfaceType);
                continue;
            }

            // Handle STRING_BYTES conflicts with dual setters (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                addStringBytesConcreteMethods(builder, field, resolver, builderInterfaceType);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, message);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);

                // add single
                builder.addMethod(MethodSpec.methodBuilder("add" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(singleElementType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doAdd$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                // addAll
                builder.addMethod(MethodSpec.methodBuilder("addAll" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doAddAll$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                // set (replace all)
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doSet$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                // clear
                builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .returns(builderInterfaceType)
                        .addStatement("doClear$L()", resolver.capitalize(field.getJavaName()))
                        .addStatement("return this")
                        .build());
            } else {
                // set
                builder.addMethod(MethodSpec.methodBuilder("set" + resolver.capitalize(field.getJavaName()))
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addParameter(fieldType, field.getJavaName())
                        .returns(builderInterfaceType)
                        .addStatement("doSet$L($L)", resolver.capitalize(field.getJavaName()), field.getJavaName())
                        .addStatement("return this")
                        .build());

                // clear for optional
                if (field.isOptional()) {
                    builder.addMethod(MethodSpec.methodBuilder("clear" + resolver.capitalize(field.getJavaName()))
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .returns(builderInterfaceType)
                            .addStatement("doClear$L()", resolver.capitalize(field.getJavaName()))
                            .addStatement("return this")
                            .build());
                }
            }
        }

        // build() implementation
        builder.addMethod(MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(interfaceType)
                .addStatement("return doBuild()")
                .build());

        return builder.build();
    }

    /**
     * Add abstract methods for INT_ENUM conflict field.
     */
    private void addIntEnumAbstractMethods(TypeSpec.Builder builder, MergedField field,
                                            ConflictEnumInfo enumInfo, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());

        // doSetXxx(int) - for int value
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(TypeName.INT, field.getJavaName())
                .build());

        // doSetXxx(EnumType) - for enum value
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(enumType, field.getJavaName())
                .build());

        // doClearXxx()
        builder.addMethod(MethodSpec.methodBuilder("doClear" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build());
    }

    /**
     * Add concrete implementations for INT_ENUM conflict field.
     */
    private void addIntEnumConcreteMethods(TypeSpec.Builder builder, MergedField field,
                                            ConflictEnumInfo enumInfo, TypeResolver resolver,
                                            ClassName builderInterfaceType) {
        String capName = resolver.capitalize(field.getJavaName());
        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());

        // setXxx(int)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderInterfaceType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // setXxx(EnumType)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(enumType, field.getJavaName())
                .returns(builderInterfaceType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx()
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("doClear$L()", capName)
                .addStatement("return this")
                .build());
    }

    /**
     * Add abstract methods for WIDENING conflict field.
     * Uses the wider type (e.g., long for int/long, double for int/double).
     */
    private void addWideningAbstractMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());

        // doSetXxx(widerType)
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(widerType, field.getJavaName())
                .build());

        // doClearXxx()
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("doClear" + capName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }
    }

    /**
     * Add concrete implementations for WIDENING conflict field.
     */
    private void addWideningConcreteMethods(TypeSpec.Builder builder, MergedField field,
                                             TypeResolver resolver, ClassName builderInterfaceType) {
        String capName = resolver.capitalize(field.getJavaName());
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());

        // setXxx(widerType)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(widerType, field.getJavaName())
                .returns(builderInterfaceType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx()
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(builderInterfaceType)
                    .addStatement("doClear$L()", capName)
                    .addStatement("return this")
                    .build());
        }
    }

    /**
     * Get the primitive TypeName for a wider type string.
     */
    private TypeName getWiderPrimitiveType(String javaType) {
        return switch (javaType) {
            case "long", "Long" -> TypeName.LONG;
            case "double", "Double" -> TypeName.DOUBLE;
            case "int", "Integer" -> TypeName.INT;
            default -> TypeName.LONG; // Default to long for numeric widening
        };
    }

    /**
     * Add abstract methods for STRING_BYTES conflict field.
     * Provides dual setters: one for String, one for byte[].
     */
    private void addStringBytesAbstractMethods(TypeSpec.Builder builder, MergedField field, TypeResolver resolver) {
        String capName = resolver.capitalize(field.getJavaName());

        // doSetXxx(String) - for String value
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(ClassName.get(String.class), field.getJavaName())
                .build());

        // doSetXxxBytes(byte[]) - for byte[] value
        builder.addMethod(MethodSpec.methodBuilder("doSet" + capName + "Bytes")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), field.getJavaName())
                .build());

        // doClearXxx() for optional fields
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("doClear" + capName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }
    }

    /**
     * Add concrete implementations for STRING_BYTES conflict field.
     */
    private void addStringBytesConcreteMethods(TypeSpec.Builder builder, MergedField field,
                                                TypeResolver resolver, ClassName builderInterfaceType) {
        String capName = resolver.capitalize(field.getJavaName());

        // setXxx(String)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ClassName.get(String.class), field.getJavaName())
                .returns(builderInterfaceType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // setXxxBytes(byte[])
        builder.addMethod(MethodSpec.methodBuilder("set" + capName + "Bytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), field.getJavaName())
                .returns(builderInterfaceType)
                .addStatement("doSet$L$L($L)", capName, "Bytes", field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx() for optional
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(builderInterfaceType)
                    .addStatement("doClear$L()", capName)
                    .addStatement("return this")
                    .build());
        }
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
        return ClassName.get(Object.class);
    }

    /**
     * Get the message type for a PRIMITIVE_MESSAGE conflict field.
     * Returns the wrapper interface type for the message version.
     */
    private TypeName getMessageTypeForField(MergedField field, TypeResolver resolver) {
        for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
            FieldInfo fieldInfo = entry.getValue();
            if (!fieldInfo.isPrimitive() && fieldInfo.getTypeName() != null) {
                // Found message version - extract simple type name and use api package
                String javaType = fieldInfo.getJavaType();
                String simpleTypeName = javaType.contains(".")
                        ? javaType.substring(javaType.lastIndexOf('.') + 1)
                        : javaType;
                // Return the type with the api package
                return ClassName.get(config.getApiPackage(), simpleTypeName);
            }
        }
        return null;
    }

    /**
     * Generate and write abstract class using context.
     */
    public Path generateAndWrite(MergedMessage message, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, ctx);
        writeToFile(javaFile);

        String relativePath = (config.getApiPackage() + ".impl").replace('.', '/')
                + "/" + message.getAbstractClassName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write abstract class.
     * @deprecated Use {@link #generateAndWrite(MergedMessage, GenerationContext)} instead
     */
    @Deprecated
    public Path generateAndWrite(MergedMessage message) throws IOException {
        JavaFile javaFile = generate(message);
        writeToFile(javaFile);

        String relativePath = (config.getApiPackage() + ".impl").replace('.', '/')
                + "/" + message.getAbstractClassName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
