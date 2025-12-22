package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import space.alnovis.protowrapper.generator.conflict.AbstractBuilderContext;
import space.alnovis.protowrapper.generator.conflict.FieldProcessingChain;
import space.alnovis.protowrapper.generator.conflict.ProcessingContext;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

        // Add abstract extract methods and getter implementations using handler chain
        ProcessingContext procCtx = ProcessingContext.forAbstract(message, protoType, ctx, config);
        FieldProcessingChain.getInstance().addAbstractExtractMethods(classBuilder, message, procCtx);
        FieldProcessingChain.getInstance().addGetterImplementations(classBuilder, message, procCtx);

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

        // Add abstract extract methods and getter implementations using handler chain
        ProcessingContext nestedProcCtx = ProcessingContext.forAbstract(nested, protoType, ctx, config);
        FieldProcessingChain.getInstance().addAbstractExtractMethods(classBuilder, nested, nestedProcCtx);
        FieldProcessingChain.getInstance().addGetterImplementations(classBuilder, nested, nestedProcCtx);

        // Add toString/equals/hashCode for nested classes
        addNestedToString(classBuilder, nested);
        addNestedEqualsHashCode(classBuilder, nested);

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

        // Abstract method to create builder (copies values from current instance)
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

        // Abstract method to create empty builder (doesn't copy values)
        classBuilder.addMethod(MethodSpec.methodBuilder("createEmptyBuilder")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(builderInterfaceType)
                .build());

        // emptyBuilder() implementation - creates empty builder of same version
        classBuilder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("return createEmptyBuilder()")
                .build());

        // Generate nested AbstractBuilder using unified method
        AbstractBuilderContext builderCtx = AbstractBuilderContext.forNested(
                nested, interfaceType, resolver, ctx, config);
        TypeSpec abstractBuilder = generateAbstractBuilder(builderCtx);
        classBuilder.addType(abstractBuilder);
    }

    /**
     * Generate AbstractBuilder class using unified context.
     * This method is used by both top-level and nested abstract builder generation.
     */
    private TypeSpec generateAbstractBuilder(AbstractBuilderContext ctx) {
        MergedMessage message = ctx.message();
        ClassName interfaceType = ctx.interfaceType();
        TypeResolver resolver = ctx.resolver();
        GenerationContext genCtx = ctx.genCtx();
        ClassName builderInterfaceType = ctx.builderInterfaceType();
        TypeVariableName protoType = TypeVariableName.get("PROTO", MESSAGE_CLASS);

        TypeSpec.Builder builder = TypeSpec.classBuilder("AbstractBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(protoType)
                .addSuperinterface(builderInterfaceType);

        // Add Javadoc for top-level builders only
        if (ctx.isTopLevel()) {
            builder.addJavadoc("Abstract builder base class.\n")
                    .addJavadoc("@param <PROTO> Protocol-specific message type\n");
        }

        // Add abstract doSet/doClear/doAdd/doBuild methods
        for (MergedField field : message.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = genCtx.getSchema()
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

        // Abstract getVersion method - used for version-aware validation
        builder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .build());

        // Add concrete implementations that delegate to abstract methods
        for (MergedField field : message.getFieldsSorted()) {
            // Skip repeated fields with type conflicts (not supported in builder yet)
            if (field.isRepeated() && field.hasTypeConflict()) {
                continue;
            }

            // Handle INT_ENUM conflicts with overloaded methods (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = genCtx.getSchema()
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

        // Protected helper method for conversion via serialization
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        String parseMethodName = "parse" + message.getName() + "FromBytes";

        classBuilder.addMethod(MethodSpec.methodBuilder("convertToVersion")
                .addModifiers(Modifier.PROTECTED)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Convert to a specific version via serialization.\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                // If already the target type, return this
                .beginControlFlow("if (versionClass.isInstance(this))")
                .addStatement("return versionClass.cast(this)")
                .endControlFlow()
                // Extract version from package and convert
                .beginControlFlow("try")
                .addStatement("int targetVersion = extractVersionFromPackage(versionClass.getPackage().getName())")
                .addStatement("$T targetContext = $T.forVersion(targetVersion)", versionContextType, versionContextType)
                .addStatement("byte[] bytes = this.toBytes()")
                .addStatement("$T parseMethod = targetContext.getClass().getMethod($S, byte[].class)",
                        Method.class, parseMethodName)
                .addStatement("return versionClass.cast(parseMethod.invoke(targetContext, bytes))")
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new $T($S + versionClass + $S + e.getMessage(), e)",
                        RuntimeException.class, "Failed to convert to ", ": ")
                .endControlFlow()
                .build());

        // Helper method to extract version number from package name
        classBuilder.addMethod(MethodSpec.methodBuilder("extractVersionFromPackage")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TypeName.INT)
                .addParameter(String.class, "packageName")
                // Find version segment like "v1" or "v202" in package name
                .addStatement("int lastDot = packageName.lastIndexOf('.')")
                .beginControlFlow("if (lastDot > 0)")
                // Check last segment (class package)
                .addStatement("String lastSegment = packageName.substring(lastDot + 1)")
                .beginControlFlow("if (lastSegment.startsWith($S) && lastSegment.length() > 1)", "v")
                .beginControlFlow("try")
                .addStatement("return $T.parseInt(lastSegment.substring(1))", Integer.class)
                .nextControlFlow("catch ($T ignored)", NumberFormatException.class)
                .endControlFlow()
                .endControlFlow()
                // Check second-to-last segment (impl.v1.ClassName pattern)
                .addStatement("int secondLastDot = packageName.lastIndexOf('.', lastDot - 1)")
                .beginControlFlow("if (secondLastDot > 0)")
                .addStatement("String secondLastSegment = packageName.substring(secondLastDot + 1, lastDot)")
                .beginControlFlow("if (secondLastSegment.startsWith($S) && secondLastSegment.length() > 1)", "v")
                .beginControlFlow("try")
                .addStatement("return $T.parseInt(secondLastSegment.substring(1))", Integer.class)
                .nextControlFlow("catch ($T ignored)", NumberFormatException.class)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("throw new $T($S + packageName)",
                        IllegalArgumentException.class, "Cannot extract version from package: ")
                .build());

        // toString() - includes proto content for debugging
        classBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.format($S, getClass().getSimpleName(), getWrapperVersion(), proto.toString().replace($S, $S).trim())",
                        String.class, "%s[version=%d] %s", "\n", ", ")
                .build());

        // equals() - compare by version and proto content
        classBuilder.addMethod(MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(Object.class, "obj")
                .addStatement("if (this == obj) return true")
                .addStatement("if (obj == null || getClass() != obj.getClass()) return false")
                .addStatement("$L<?> other = ($L<?>) obj", message.getAbstractClassName(), message.getAbstractClassName())
                .addStatement("return this.getWrapperVersion() == other.getWrapperVersion() && $T.equals(this.proto, other.proto)",
                        Objects.class)
                .build());

        // hashCode() - based on version and proto content
        classBuilder.addMethod(MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return $T.hash(getWrapperVersion(), proto)", Objects.class)
                .build());

        // Abstract method for getting VersionContext (versionContextType already defined above)
        classBuilder.addMethod(MethodSpec.methodBuilder("getVersionContext")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(versionContextType)
                .build());

        // getContext() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("getContext")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(versionContextType)
                .addStatement("return getVersionContext()")
                .build());
    }

    /**
     * Add toString() method for nested classes.
     * Nested classes don't have getWrapperVersion(), so we only show class name and proto content.
     */
    private void addNestedToString(TypeSpec.Builder classBuilder, MergedMessage nested) {
        classBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.format($S, getClass().getSimpleName(), proto.toString().replace($S, $S).trim())",
                        String.class, "%s %s", "\n", ", ")
                .build());
    }

    /**
     * Add equals() and hashCode() methods for nested classes.
     * Nested classes don't have getWrapperVersion(), so we only compare proto content.
     */
    private void addNestedEqualsHashCode(TypeSpec.Builder classBuilder, MergedMessage nested) {
        String abstractClassName = nested.getName();

        // equals() - compare proto content only (nested classes don't have version)
        classBuilder.addMethod(MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(Object.class, "obj")
                .addStatement("if (this == obj) return true")
                .addStatement("if (obj == null || getClass() != obj.getClass()) return false")
                .addStatement("$L<?> other = ($L<?>) obj", abstractClassName, abstractClassName)
                .addStatement("return $T.equals(this.proto, other.proto)", Objects.class)
                .build());

        // hashCode() - based on proto content only
        classBuilder.addMethod(MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return $T.hash(proto)", Objects.class)
                .build());
    }

    private void addBuilderSupport(TypeSpec.Builder classBuilder, MergedMessage message,
                                   TypeVariableName protoType, ClassName interfaceType,
                                   TypeResolver resolver, GenerationContext ctx) {
        // Builder interface type from the interface
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");

        // Abstract method to create builder (copies values from current instance)
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

        // Abstract method to create empty builder (doesn't copy values)
        classBuilder.addMethod(MethodSpec.methodBuilder("createEmptyBuilder")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(builderInterfaceType)
                .build());

        // emptyBuilder() implementation - creates empty builder of same version
        classBuilder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("return createEmptyBuilder()")
                .build());

        // Generate AbstractBuilder nested class using unified method
        AbstractBuilderContext builderCtx = AbstractBuilderContext.forTopLevel(
                message, interfaceType, resolver, ctx, config);
        TypeSpec abstractBuilder = generateAbstractBuilder(builderCtx);
        classBuilder.addType(abstractBuilder);
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

        // Build version check condition for enum versions only
        // Validation should only happen for versions that use enum type (not int)
        Set<String> enumVersions = enumInfo.getEnumVersions();
        List<Integer> enumVersionNumbers = enumVersions.stream()
                .map(resolver::extractVersionNumber)
                .sorted()
                .toList();

        // Build setXxx(int) method with version-aware validation
        MethodSpec.Builder setIntMethod = MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderInterfaceType);

        // Add version-aware validation: only validate for versions that use enum type
        if (!enumVersionNumbers.isEmpty()) {
            String versionCondition = enumVersionNumbers.stream()
                    .map(v -> "getVersion() == " + v)
                    .reduce((a, b) -> a + " || " + b)
                    .orElse("false");

            setIntMethod.beginControlFlow("if ($L)", versionCondition)
                    .addStatement("$T.fromProtoValueOrThrow($L)", enumType, field.getJavaName())
                    .endControlFlow();
        }

        setIntMethod.addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this");

        builder.addMethod(setIntMethod.build());

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
        if (listType instanceof ParameterizedTypeName parameterized
                && !parameterized.typeArguments.isEmpty()) {
            return parameterized.typeArguments.get(0);
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
