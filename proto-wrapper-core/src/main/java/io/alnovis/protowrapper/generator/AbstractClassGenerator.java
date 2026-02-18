package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.generator.conflict.AbstractBuilderContext;
import io.alnovis.protowrapper.generator.conflict.FieldProcessingChain;
import io.alnovis.protowrapper.generator.conflict.ProcessingContext;
import io.alnovis.protowrapper.generator.oneof.OneofGenerator;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedOneof;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import static io.alnovis.protowrapper.generator.ProtobufConstants.*;

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

    /**
     * Create a new abstract class generator.
     *
     * @param config the generator configuration
     */
    public AbstractClassGenerator(GeneratorConfig config) {
        super(config);
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

        // Add oneof support (abstract extract methods and implementations)
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            classBuilder.addMethod(oneofGenerator.generateAbstractExtractCaseMethod(oneof, message, protoType));
            classBuilder.addMethod(oneofGenerator.generateAbstractCaseGetter(oneof, message));
        }

        // Add common methods
        addCommonMethods(classBuilder, message, protoType, resolver);

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

        // Create processing context for handler chain
        ProcessingContext processingCtx = ProcessingContext.forAbstract(message, protoType, genCtx, config);

        // Add abstract doSet/doClear/doAdd/doBuild methods using handler chain
        FieldProcessingChain.getInstance().addAbstractBuilderMethods(builder, message, processingCtx);

        // Add oneof abstract methods
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            builder.addMethod(oneofGenerator.generateAbstractBuilderDoClear(oneof));
        }

        // Abstract doBuild method
        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(interfaceType)
                .build());

        // Abstract getVersionId method - returns version identifier (e.g., "v1", "v2")
        builder.addMethod(MethodSpec.methodBuilder("getVersionId")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(ClassName.get(String.class))
                .build());

        // Add concrete implementations that delegate to abstract methods using handler chain
        FieldProcessingChain.getInstance().addConcreteBuilderMethods(builder, message, builderInterfaceType, processingCtx);

        // Add oneof concrete methods
        for (MergedOneof oneof : message.getOneofGroups()) {
            builder.addMethod(oneofGenerator.generateAbstractBuilderClear(oneof, builderInterfaceType));
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
                                  TypeVariableName protoType, TypeResolver resolver) {
        // getTypedProto() implementation - returns proto as Message (from ProtoWrapper interface)
        classBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(MESSAGE_CLASS)
                .addStatement("return proto")
                .addJavadoc("Get the underlying protobuf message.\n")
                .addJavadoc("@return protobuf Message\n")
                .build());

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

        // Abstract extractWrapperVersionId - returns version identifier (e.g., "v1", "v2")
        classBuilder.addMethod(MethodSpec.methodBuilder("extractWrapperVersionId")
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(ClassName.get(String.class))
                .addParameter(protoType, "proto")
                .build());

        // getWrapperVersionId() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("getWrapperVersionId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(ClassName.get(String.class))
                .addStatement("return extractWrapperVersionId(proto)")
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

        // asVersionStrict() implementation - throws if data would become inaccessible
        classBuilder.addMethod(MethodSpec.methodBuilder("asVersionStrict")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .beginControlFlow("if (versionClass.isInstance(this))")
                .addStatement("return versionClass.cast(this)")
                .endControlFlow()
                .addStatement("int targetVersion = extractVersionFromPackage(versionClass.getPackage().getName())")
                .addStatement("$T<$T> inaccessibleFields = getFieldsInaccessibleInVersion(targetVersion)",
                        java.util.List.class, String.class)
                .beginControlFlow("if (!inaccessibleFields.isEmpty())")
                .addStatement("throw new $T($T.format($S, getWrapperVersionId(), targetVersion, inaccessibleFields))",
                        IllegalStateException.class, String.class,
                        "Cannot convert from version %s to version %d: the following fields have values " +
                        "but will become inaccessible in the target version: %s. " +
                        "Use asVersion() if you want to proceed anyway (data is preserved via protobuf unknown fields).")
                .endControlFlow()
                .addStatement("return convertToVersion(versionClass)")
                .build());

        // asVersion(VersionContext) implementation - direct conversion without reflection
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        ClassName interfaceType = ClassName.get(config.getApiPackage(), interfaceName);
        String parseMethodName = "parse" + message.getName() + "FromBytes";
        String parsePartialMethodName = "parsePartial" + message.getName() + "FromBytes";
        ClassName invalidProtocolBufferException = ClassName.get(
                "com.google.protobuf", "InvalidProtocolBufferException");

        classBuilder.addMethod(MethodSpec.methodBuilder("asVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceType)
                .addParameter(versionContextType, "targetContext")
                .beginControlFlow("if (targetContext.getVersionId().equals(getWrapperVersionId()))")
                .addStatement("return this")
                .endControlFlow()
                .beginControlFlow("try")
                .addStatement("return targetContext.$L(this.toBytes())", parsePartialMethodName)
                .nextControlFlow("catch ($T e)", invalidProtocolBufferException)
                .addStatement("throw new $T($T.format($S, getClass().getSimpleName(), " +
                        "getWrapperVersionId(), targetContext.getVersionId(), e.getMessage()), e)",
                        RuntimeException.class, String.class,
                        "Failed to convert %s from version %s to %s: %s")
                .endControlFlow()
                .build());

        // Protected helper method for conversion via serialization

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
                // Extract version ID from package and convert
                .addStatement("String targetVersionId = extractVersionIdFromPackage(versionClass.getPackage().getName())")
                .beginControlFlow("try")
                .addStatement("$T targetContext = $T.forVersionId(targetVersionId)", versionContextType, versionContextType)
                .addStatement("byte[] bytes = this.toBytes()")
                .addStatement("$T parseMethod = targetContext.getClass().getMethod($S, byte[].class)",
                        Method.class, parsePartialMethodName)
                .addStatement("return versionClass.cast(parseMethod.invoke(targetContext, bytes))")
                .nextControlFlow("catch ($T e)", java.lang.reflect.InvocationTargetException.class)
                // Get the real cause from InvocationTargetException
                .addStatement("$T cause = e.getCause() != null ? e.getCause() : e", Throwable.class)
                .addStatement("throw new $T($T.format($S, getClass().getSimpleName(), getWrapperVersionId(), targetVersionId, cause.getClass().getSimpleName(), cause.getMessage()), cause)",
                        RuntimeException.class, String.class,
                        "Failed to convert %s from version %s to %s: %s - %s")
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("throw new $T($T.format($S, getClass().getSimpleName(), getWrapperVersionId(), targetVersionId, e.getMessage()), e)",
                        RuntimeException.class, String.class,
                        "Failed to convert %s from version %s to %s: %s")
                .endControlFlow()
                .build());

        // Helper method to extract version ID (e.g., "v1", "v2") from package name
        classBuilder.addMethod(MethodSpec.methodBuilder("extractVersionIdFromPackage")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ClassName.get(String.class))
                .addParameter(String.class, "packageName")
                // Find version segment like "v1" or "v2" in package name
                .addStatement("int lastDot = packageName.lastIndexOf('.')")
                .beginControlFlow("if (lastDot > 0)")
                // Check last segment (class package)
                .addStatement("String lastSegment = packageName.substring(lastDot + 1)")
                .beginControlFlow("if (lastSegment.startsWith($S) && lastSegment.length() > 1)", "v")
                .addStatement("return lastSegment")
                .endControlFlow()
                // Check second-to-last segment (impl.v1.ClassName pattern)
                .addStatement("int secondLastDot = packageName.lastIndexOf('.', lastDot - 1)")
                .beginControlFlow("if (secondLastDot > 0)")
                .addStatement("String secondLastSegment = packageName.substring(secondLastDot + 1, lastDot)")
                .beginControlFlow("if (secondLastSegment.startsWith($S) && secondLastSegment.length() > 1)", "v")
                .addStatement("return secondLastSegment")
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("throw new $T($S + packageName)",
                        IllegalArgumentException.class, "Cannot extract version from package: ")
                .build());

        // Helper method to extract version number from package name (for internal use)
        classBuilder.addMethod(MethodSpec.methodBuilder("extractVersionFromPackage")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TypeName.INT)
                .addParameter(String.class, "packageName")
                .addStatement("String versionId = extractVersionIdFromPackage(packageName)")
                .addStatement("String numStr = versionId.replaceAll($S, $S)", "[^0-9]", "")
                .beginControlFlow("try")
                .addStatement("return $T.parseInt(numStr)", Integer.class)
                .nextControlFlow("catch ($T e)", NumberFormatException.class)
                .addStatement("throw new $T($S + versionId)", IllegalArgumentException.class, "Cannot parse version number from: ")
                .endControlFlow()
                .build());

        // toString() - includes proto content for debugging
        classBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.format($S, getClass().getSimpleName(), getWrapperVersionId(), proto.toString().replace($S, $S).trim())",
                        String.class, "%s[version=%s] %s", "\n", ", ")
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
                .addStatement("return $T.equals(this.getWrapperVersionId(), other.getWrapperVersionId()) && $T.equals(this.proto, other.proto)",
                        Objects.class, Objects.class)
                .build());

        // hashCode() - based on version and proto content
        classBuilder.addMethod(MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return $T.hash(getWrapperVersionId(), proto)", Objects.class)
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

        // getFieldsInaccessibleInVersion() implementation
        addGetFieldsInaccessibleInVersionMethod(classBuilder, message, resolver);
    }

    /**
     * Generate getFieldsInaccessibleInVersion() method that checks which fields
     * will become inaccessible when converting to a target version.
     */
    private void addGetFieldsInaccessibleInVersionMethod(TypeSpec.Builder classBuilder,
                                                          MergedMessage message,
                                                          TypeResolver resolver) {
        TypeName listOfString = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class),
                ClassName.get(String.class)
        );
        ClassName arrayList = ClassName.get(java.util.ArrayList.class);

        MethodSpec.Builder method = MethodSpec.methodBuilder("getFieldsInaccessibleInVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfString)
                .addParameter(TypeName.INT, "targetVersion")
                .addStatement("$T<$T> inaccessible = new $T<>()", java.util.List.class, String.class, arrayList);

        // Get all versions this message supports
        Set<String> allVersions = message.getPresentInVersions();

        // For each field, check if it's version-specific
        for (MergedField field : message.getFieldsSorted()) {
            Set<String> fieldVersions = field.getPresentInVersions();

            // Skip if field exists in all versions
            if (fieldVersions.containsAll(allVersions)) {
                continue;
            }

            // Generate version check for this field
            // Extract version numbers from version strings (e.g., "v1" -> 1, "v2" -> 2)
            String versionCheck = fieldVersions.stream()
                    .map(v -> v.replaceAll("[^0-9]", ""))
                    .filter(s -> !s.isEmpty())
                    .map(v -> "targetVersion == " + v)
                    .collect(java.util.stream.Collectors.joining(" || "));

            if (versionCheck.isEmpty()) {
                continue;
            }

            String fieldName = field.getJavaName();
            String hasMethod = "has" + resolver.capitalize(fieldName) + "()";

            // For fields with has*() method, check hasXxx()
            // For fields without has*() (proto3 implicit, required), always consider them as "having value"
            if (field.shouldGenerateHasMethod()) {
                method.beginControlFlow("if ($L && !($L))", hasMethod, versionCheck);
            } else if (field.isMap()) {
                // For map fields, use getXxxMap()
                String getMethod = field.getMapGetterName() + "()";
                method.beginControlFlow("if (!$L.isEmpty() && !($L))", getMethod, versionCheck);
            } else if (field.isRepeated()) {
                // For repeated fields, check if list is not empty
                String getMethod = field.getGetterName() + "()";
                method.beginControlFlow("if (!$L.isEmpty() && !($L))", getMethod, versionCheck);
            } else {
                // Required field - always has value, just check version
                method.beginControlFlow("if (!($L))", versionCheck);
            }

            method.addStatement("inaccessible.add($S)", fieldName);
            method.endControlFlow();
        }

        method.addStatement("return inaccessible");
        classBuilder.addMethod(method.build());
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
     * Generate and write abstract class using context.
     *
     * @param message the merged message to generate abstract class for
     * @param ctx the generation context
     * @return the path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite(MergedMessage message, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, ctx);
        writeToFile(javaFile);

        String relativePath = (config.getApiPackage() + ".impl").replace('.', '/')
                + "/" + message.getAbstractClassName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

}
