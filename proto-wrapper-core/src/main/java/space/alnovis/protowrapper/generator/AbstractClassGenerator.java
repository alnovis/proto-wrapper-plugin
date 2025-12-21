package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

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
        return generateInternal(message, resolver);
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
        return generateInternal(message, typeResolver);
    }

    private JavaFile generateInternal(MergedMessage message, TypeResolver resolver) {
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
            addExtractMethods(classBuilder, field, protoType, message, resolver);
        }

        // Add concrete getter implementations
        for (MergedField field : message.getFieldsSorted()) {
            addGetterImplementation(classBuilder, field, message, resolver);
        }

        // Add common methods
        addCommonMethods(classBuilder, message, protoType);

        // Add Builder support if enabled
        if (config.isGenerateBuilders()) {
            addBuilderSupport(classBuilder, message, protoType, interfaceType, resolver);
        }

        // Add nested abstract classes for nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            TypeSpec nestedClass = generateNestedAbstractClass(nested, message, resolver);
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
    private TypeSpec generateNestedAbstractClass(MergedMessage nested, MergedMessage parent, TypeResolver resolver) {
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
            addExtractMethods(classBuilder, field, protoType, nested, resolver);
        }

        // Add concrete getter implementations
        for (MergedField field : nested.getFieldsSorted()) {
            addGetterImplementation(classBuilder, field, nested, resolver);
        }

        // Recursively add deeply nested abstract classes
        for (MergedMessage deeplyNested : nested.getNestedMessages()) {
            TypeSpec deeplyNestedClass = generateNestedAbstractClass(deeplyNested, nested, resolver);
            classBuilder.addType(deeplyNestedClass);
        }

        // Add Builder support for nested message if enabled
        if (config.isGenerateBuilders()) {
            addNestedBuilderSupport(classBuilder, nested, interfaceType, resolver);
        }

        return classBuilder.build();
    }

    private void addNestedBuilderSupport(TypeSpec.Builder classBuilder, MergedMessage nested,
                                         ClassName interfaceType, TypeResolver resolver) {
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
        TypeSpec abstractBuilder = generateNestedAbstractBuilder(nested, interfaceType, resolver);
        classBuilder.addType(abstractBuilder);
    }

    private TypeSpec generateNestedAbstractBuilder(MergedMessage nested, ClassName interfaceType, TypeResolver resolver) {
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");
        TypeVariableName protoType = TypeVariableName.get("PROTO", MESSAGE_CLASS);

        TypeSpec.Builder builder = TypeSpec.classBuilder("AbstractBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(builderInterfaceType);

        // Add abstract doSet/doClear methods
        for (MergedField field : nested.getFieldsSorted()) {
            // Skip fields with non-convertible type conflicts
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
            // Skip fields with non-convertible type conflicts
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
                                   TypeVariableName protoType, MergedMessage message, TypeResolver resolver) {
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
    }

    private void addGetterImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                         MergedMessage message, TypeResolver resolver) {
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
                                   TypeVariableName protoType, ClassName interfaceType, TypeResolver resolver) {
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
        TypeSpec abstractBuilder = generateAbstractBuilder(message, interfaceType, resolver);
        classBuilder.addType(abstractBuilder);
    }

    private TypeSpec generateAbstractBuilder(MergedMessage message, ClassName interfaceType, TypeResolver resolver) {
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
            // Skip fields with non-convertible type conflicts
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
            // Skip fields with non-convertible type conflicts
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
