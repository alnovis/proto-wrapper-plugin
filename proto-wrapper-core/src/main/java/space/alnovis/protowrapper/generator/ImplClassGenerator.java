package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import space.alnovis.protowrapper.generator.conflict.BuilderImplContext;
import space.alnovis.protowrapper.generator.conflict.FieldProcessingChain;
import space.alnovis.protowrapper.generator.conflict.ProcessingContext;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // Legacy fields - kept for backward compatibility
    @Deprecated
    private TypeResolver typeResolver;

    public ImplClassGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Set the merged schema for cross-message type resolution.
     * @deprecated Use {@link #generate(MergedMessage, String, String, GenerationContext)} instead
     */
    @Deprecated
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

        // Add common implementation methods
        addCommonImplMethods(classBuilder, className, protoType, implPackage, ctx);

        // Add Builder support if enabled
        if (config.isGenerateBuilders()) {
            currentProtoClassName.set(protoClassName);
            try {
                addBuilderImpl(classBuilder, message, protoType, className, implPackage, ctx);
            } finally {
                currentProtoClassName.remove();
            }
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
     * Add common implementation methods (serialization, version, factory, typed proto).
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

        // Wrapper version method
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

        // getTypedProto() for VersionContext
        classBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addModifiers(Modifier.PUBLIC)
                .returns(protoType)
                .addStatement("return proto")
                .addJavadoc("Get the underlying typed proto message.\n")
                .addJavadoc("@return $T\n", protoType)
                .build());
    }

    /**
     * Generate implementation class for a specific version.
     * @deprecated Use {@link #generate(MergedMessage, String, GenerationContext)} instead
     */
    @Deprecated
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
            currentProtoClassName.set(protoClassName);
            try {
                addNestedBuilderImpl(classBuilder, nested, protoType, className, ctx);
            } finally {
                currentProtoClassName.remove();
            }
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

    private String getVersionSpecificJavaName(MergedField field, String version, TypeResolver resolver) {
        return Optional.ofNullable(field.getVersionFields().get(version))
                .map(vf -> resolver.capitalize(vf.getJavaName()))
                .orElseGet(() -> resolver.capitalize(field.getJavaName()));
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

        // createBuilder() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl(proto.toBuilder())")
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

        // Implement doSet/doClear/doAdd methods for each field
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
                    boolean presentInVersion = field.getPresentInVersions().contains(version);
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), presentInVersion, message, genCtx);
                    continue;
                }
            }

            // Handle WIDENING conflicts with range checking (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING) {
                boolean presentInVersion = field.getPresentInVersions().contains(version);
                addWideningBuilderMethods(builder, field, presentInVersion, message, genCtx);
                continue;
            }

            // Handle STRING_BYTES conflicts with UTF-8 conversion (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                boolean presentInVersion = field.getPresentInVersions().contains(version);
                addStringBytesBuilderMethods(builder, field, presentInVersion, message, genCtx);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, message);
            boolean presentInVersion = field.getPresentInVersions().contains(version);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);
                addRepeatedFieldBuilderMethods(builder, field, singleElementType, fieldType,
                        presentInVersion, message, genCtx);
            } else {
                addSingleFieldBuilderMethods(builder, field, fieldType, presentInVersion, message, genCtx);
            }
        }

        // doBuild() implementation
        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(interfaceType)
                .addStatement("return new $L(protoBuilder.build())", implClassName)
                .build());

        // Helper method to extract proto from wrapper
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

        return builder.build();
    }

    private void addBuilderImpl(TypeSpec.Builder classBuilder, MergedMessage message,
                                 ClassName protoType, String className, String implPackage, GenerationContext ctx) {
        // Create unified context for builder generation
        BuilderImplContext builderCtx = BuilderImplContext.forTopLevel(
                message, protoType, className, ctx, config);

        ClassName builderInterfaceType = builderCtx.builderInterfaceType();

        // createBuilder() implementation
        classBuilder.addMethod(MethodSpec.methodBuilder("createBuilder")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(builderInterfaceType)
                .addStatement("return new BuilderImpl(proto.toBuilder())")
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
     * Add builder methods for INT_ENUM conflict field.
     * Generates overloaded doSetXxx(int) and doSetXxx(EnumType) methods.
     */
    private void addIntEnumBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           ConflictEnumInfo enumInfo, boolean presentInVersion,
                                           MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        String capitalizedName = resolver.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());

        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();

        // doSetXxx(int value)
        MethodSpec.Builder doSetInt = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(TypeName.INT, field.getJavaName());

        if (presentInVersion) {
            if (versionIsEnum) {
                // Version has enum - need to convert int to proto enum
                String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
                String enumMethod = getEnumFromIntMethod();
                doSetInt.addStatement("protoBuilder.set$L($L.$L($L))",
                        versionJavaName, protoEnumType, enumMethod, field.getJavaName());
            } else {
                // Version has int - use directly
                doSetInt.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSetInt.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetInt.build());

        // doSetXxx(EnumType value)
        MethodSpec.Builder doSetEnum = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(enumType, field.getJavaName());

        if (presentInVersion) {
            if (versionIsEnum) {
                // Version has enum - need to convert unified enum to proto enum
                String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
                String enumMethod = getEnumFromIntMethod();
                doSetEnum.addStatement("protoBuilder.set$L($L.$L($L.getValue()))",
                        versionJavaName, protoEnumType, enumMethod, field.getJavaName());
            } else {
                // Version has int - get value from unified enum
                doSetEnum.addStatement("protoBuilder.set$L($L.getValue())",
                        versionJavaName, field.getJavaName());
            }
        } else {
            doSetEnum.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetEnum.build());

        // doClearXxx()
        MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        if (presentInVersion) {
            doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
        } else {
            doClear.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doClear.build());
    }

    /**
     * Add builder methods for WIDENING conflict field.
     * Generates doSetXxx(widerType) with range checking for versions with narrower types.
     */
    private void addWideningBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                            boolean presentInVersion, MergedMessage message,
                                            GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        String capitalizedName = resolver.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        // Determine the wider type (unified type) and the version's actual type
        String widerType = field.getJavaType(); // Already resolved to wider type
        TypeName widerTypeName = getWiderPrimitiveType(widerType);

        // Get the version-specific type
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionType = versionField != null ? versionField.getJavaType() : widerType;
        boolean needsNarrowing = !versionType.equals(widerType) && !versionType.equals("Long") && !versionType.equals("Double");

        // doSetXxx(widerType value)
        MethodSpec.Builder doSet = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(widerTypeName, field.getJavaName());

        if (presentInVersion) {
            if (needsNarrowing) {
                // Need to check range and narrow
                switch (widerType) {
                    case "long", "Long", "double", "Double" -> {
                        // Narrowing to int with range check
                        doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                                field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
                        doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                                IllegalArgumentException.class, field.getJavaName(), version);
                        doSet.endControlFlow();
                        doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, field.getJavaName());
                    }
                    default -> // Unknown narrowing - just cast
                            doSet.addStatement("protoBuilder.set$L(($L) $L)", versionJavaName, versionType, field.getJavaName());
                }
            } else {
                // No narrowing needed - use directly
                doSet.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSet.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSet.build());

        // doClearXxx() for optional fields
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED);

            if (presentInVersion) {
                doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
            } else {
                doClear.addComment("Field not present in this version - ignored");
            }
            builder.addMethod(doClear.build());
        }
    }

    /**
     * Add builder methods for STRING_BYTES conflict field.
     * Generates doSetXxx(String) and doSetXxxBytes(byte[]) with UTF-8 conversion.
     */
    private void addStringBytesBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                               boolean presentInVersion, MergedMessage message,
                                               GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        String capitalizedName = resolver.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        // Determine if this version uses bytes or string
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionType = versionField != null ? versionField.getJavaType() : "String";
        boolean versionIsBytes = "byte[]".equals(versionType) || "ByteString".equals(versionType);

        // doSetXxx(String value)
        MethodSpec.Builder doSetString = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ClassName.get(String.class), field.getJavaName());

        if (presentInVersion) {
            if (versionIsBytes) {
                // Version has bytes - convert String to ByteString using UTF-8
                doSetString.addStatement("protoBuilder.set$L($T.copyFrom($L, $T.UTF_8))",
                        versionJavaName, BYTE_STRING_CLASS, field.getJavaName(), StandardCharsets.class);
            } else {
                // Version has String - set directly
                doSetString.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSetString.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetString.build());

        // doSetXxxBytes(byte[] value)
        MethodSpec.Builder doSetBytes = MethodSpec.methodBuilder("doSet" + capitalizedName + "Bytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), field.getJavaName());

        if (presentInVersion) {
            if (versionIsBytes) {
                // Version has bytes - convert byte[] to ByteString
                doSetBytes.addStatement("protoBuilder.set$L($T.copyFrom($L))",
                        versionJavaName, BYTE_STRING_CLASS, field.getJavaName());
            } else {
                // Version has String - convert bytes to String using UTF-8
                doSetBytes.addStatement("protoBuilder.set$L(new $T($L, $T.UTF_8))",
                        versionJavaName, String.class, field.getJavaName(), StandardCharsets.class);
            }
        } else {
            doSetBytes.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetBytes.build());

        // doClearXxx() for optional fields
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED);

            if (presentInVersion) {
                doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
            } else {
                doClear.addComment("Field not present in this version - ignored");
            }
            builder.addMethod(doClear.build());
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

    private void addSingleFieldBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                               TypeName fieldType, boolean presentInVersion,
                                               MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        String capitalizedName = resolver.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        // doSet
        MethodSpec.Builder doSet = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(fieldType, field.getJavaName());

        if (presentInVersion) {
            String setterCall = generateProtoSetterCall(field, message, ctx);
            doSet.addStatement(setterCall, field.getJavaName());
        } else {
            doSet.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSet.build());

        // doClear for optional
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED);

            if (presentInVersion) {
                doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
            } else {
                doClear.addComment("Field not present in this version - ignored");
            }
            builder.addMethod(doClear.build());
        }
    }

    private void addRepeatedFieldBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                 TypeName singleElementType, TypeName listType,
                                                 boolean presentInVersion, MergedMessage message,
                                                 GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        String capitalizedName = resolver.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        // doAdd
        MethodSpec.Builder doAdd = MethodSpec.methodBuilder("doAdd" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(singleElementType, field.getJavaName());

        if (presentInVersion) {
            if (field.isMessage()) {
                String protoTypeName = getProtoTypeForField(field, message, ctx);
                doAdd.addStatement("protoBuilder.add$L(($L) extractProto($L))", versionJavaName, protoTypeName, field.getJavaName());
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
                String enumMethod = getEnumFromIntMethod();
                doAdd.addStatement("protoBuilder.add$L($L." + enumMethod + "($L.getValue()))", versionJavaName, protoEnumType, field.getJavaName());
            } else if (isBytesType(field)) {
                // Convert byte[] to ByteString for protobuf
                doAdd.addStatement("protoBuilder.add$L($T.copyFrom($L))", versionJavaName, BYTE_STRING_CLASS, field.getJavaName());
            } else {
                doAdd.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doAdd.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doAdd.build());

        // doAddAll
        MethodSpec.Builder doAddAll = MethodSpec.methodBuilder("doAddAll" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        if (presentInVersion) {
            if (field.isMessage()) {
                String protoTypeName = getProtoTypeForField(field, message, ctx);
                doAddAll.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) extractProto(e)))",
                        field.getJavaName(), versionJavaName, protoTypeName);
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
                String enumMethod = getEnumFromIntMethod();
                doAddAll.addStatement("$L.forEach(e -> protoBuilder.add$L($L." + enumMethod + "(e.getValue())))",
                        field.getJavaName(), versionJavaName, protoEnumType);
            } else if (isBytesType(field)) {
                // Convert each byte[] to ByteString for protobuf
                doAddAll.addStatement("$L.forEach(e -> protoBuilder.add$L($T.copyFrom(e)))",
                        field.getJavaName(), versionJavaName, BYTE_STRING_CLASS);
            } else {
                doAddAll.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doAddAll.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doAddAll.build());

        // doSet (replace all)
        MethodSpec.Builder doSetAll = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        if (presentInVersion) {
            doSetAll.addStatement("protoBuilder.clear$L()", versionJavaName);
            if (field.isMessage()) {
                String protoTypeName = getProtoTypeForField(field, message, ctx);
                doSetAll.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) extractProto(e)))",
                        field.getJavaName(), versionJavaName, protoTypeName);
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
                String enumMethod = getEnumFromIntMethod();
                doSetAll.addStatement("$L.forEach(e -> protoBuilder.add$L($L." + enumMethod + "(e.getValue())))",
                        field.getJavaName(), versionJavaName, protoEnumType);
            } else if (isBytesType(field)) {
                // Convert each byte[] to ByteString for protobuf
                doSetAll.addStatement("$L.forEach(e -> protoBuilder.add$L($T.copyFrom(e)))",
                        field.getJavaName(), versionJavaName, BYTE_STRING_CLASS);
            } else {
                doSetAll.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSetAll.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetAll.build());

        // doClear
        MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        if (presentInVersion) {
            doClear.addStatement("protoBuilder.clear$L()", versionJavaName);
        } else {
            doClear.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doClear.build());
    }

    private String generateProtoSetterCall(MergedField field, MergedMessage message, GenerationContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();
        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        if (field.isMessage()) {
            // Get proto type for casting
            String protoTypeName = getProtoTypeForField(field, message, ctx);
            return "protoBuilder.set" + versionJavaName + "((" + protoTypeName + ") extractProto($L))";
        } else if (field.isEnum()) {
            // Get proto enum type and use version-specific method to convert
            String protoEnumType = getProtoEnumTypeForField(field, message, ctx);
            String enumMethod = getEnumFromIntMethod();
            return "protoBuilder.set" + versionJavaName + "(" + protoEnumType + "." + enumMethod + "($L.getValue()))";
        } else if (isBytesType(field)) {
            // Convert byte[] to ByteString for protobuf
            return "protoBuilder.set" + versionJavaName + "(com.google.protobuf.ByteString.copyFrom($L))";
        } else {
            return "protoBuilder.set" + versionJavaName + "($L)";
        }
    }

    /**
     * Check if field is a bytes type (proto bytes -> Java byte[]).
     * Handles both scalar "byte[]" and repeated "List<byte[]>".
     */
    private boolean isBytesType(MergedField field) {
        String getterType = field.getGetterType();
        return "byte[]".equals(getterType) || getterType.contains("byte[]");
    }

    // Store current proto class name for type resolution during builder generation
    private ThreadLocal<String> currentProtoClassName = new ThreadLocal<>();

    private String getProtoTypeForField(MergedField field, MergedMessage message, GenerationContext ctx) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return "com.google.protobuf.Message";
        }

        String typeName = versionField.getTypeName();
        if (typeName.startsWith(".")) {
            typeName = typeName.substring(1);
        }

        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.getTypeResolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        // Extract message path (e.g., "OrderRequest.GiftOptions" or "Money")
        String messagePath = versionField.extractNestedTypePath(protoPackage);

        // Look up the message in MergedSchema to find its outer class
        MergedSchema schema = ctx.getSchema();
        String outerClassName = findOuterClassForType(messagePath, schema, version);

        if (outerClassName != null) {
            // Build full Java proto class: package.OuterClass.MessagePath
            return javaProtoPackage + "." + outerClassName + "." + messagePath;
        }

        // Fallback: try using current message's outer class for nested types
        String currentProto = currentProtoClassName.get();
        if (currentProto != null && currentProto.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProto.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + messagePath;
            }
        }

        // Last fallback
        return javaProtoPackage + "." + messagePath;
    }

    /**
     * Find the outer class name for a message type by looking it up in the schema.
     */
    private String findOuterClassForType(String messagePath, MergedSchema schema, String version) {
        if (schema == null || messagePath == null) {
            return null;
        }

        // messagePath could be "Money" or "OrderRequest.GiftOptions"
        String topLevelName = messagePath.contains(".")
                ? messagePath.substring(0, messagePath.indexOf('.'))
                : messagePath;

        // Look up the top-level message in the schema
        return schema.getMessage(topLevelName)
                .map(msg -> msg.getOuterClassName(version))
                .orElse(null);
    }

    private String getProtoEnumTypeForField(MergedField field, MergedMessage message, GenerationContext ctx) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return "Object";
        }

        String typeName = versionField.getTypeName();
        if (typeName.startsWith(".")) {
            typeName = typeName.substring(1);
        }

        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.getTypeResolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        // Extract enum path (e.g., "Priority" or "OrderItem.Discount.DiscountType")
        String enumPath = versionField.extractNestedTypePath(protoPackage);

        // Look up the enum in MergedSchema to find its outer class
        MergedSchema schema = ctx.getSchema();
        String outerClassName = findOuterClassForEnum(enumPath, schema, version);

        if (outerClassName != null) {
            // Build full Java proto class: package.OuterClass.EnumPath
            return javaProtoPackage + "." + outerClassName + "." + enumPath;
        }

        // Fallback: try using current message's outer class for nested enums
        String currentProto = currentProtoClassName.get();
        if (currentProto != null && currentProto.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProto.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + enumPath;
            }
        }

        // Last fallback
        return javaProtoPackage + "." + enumPath;
    }

    /**
     * Find the outer class name for an enum type by looking it up in the schema.
     */
    private String findOuterClassForEnum(String enumPath, MergedSchema schema, String version) {
        if (schema == null || enumPath == null) {
            return null;
        }

        // enumPath could be "Priority" (top-level) or "OrderItem.Discount.DiscountType" (nested)
        if (!enumPath.contains(".")) {
            // Top-level enum - look it up directly in the schema
            return schema.getEnum(enumPath)
                    .map(e -> e.getOuterClassName(version))
                    .orElse(null);
        } else {
            // Nested enum - get the top-level message and find its outer class
            String topLevelName = enumPath.substring(0, enumPath.indexOf('.'));
            return schema.getMessage(topLevelName)
                    .map(msg -> msg.getOuterClassName(version))
                    .orElse(null);
        }
    }

    /**
     * Get the enum conversion method name based on protobuf version.
     * Protobuf 2.x uses valueOf(int), protobuf 3.x uses forNumber(int).
     */
    private String getEnumFromIntMethod() {
        return config.isProtobuf2() ? "valueOf" : "forNumber";
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
     * Generate and write implementation class.
     * @deprecated Use {@link #generateAndWrite(MergedMessage, String, GenerationContext)} instead
     */
    @Deprecated
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
     */
    public Path generateAndWrite(MergedMessage message, String protoClassName, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, protoClassName, ctx);
        writeToFile(javaFile);

        String relativePath = ctx.getImplPackage().replace('.', '/')
                + "/" + ctx.getImplClassName(message.getName()) + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
