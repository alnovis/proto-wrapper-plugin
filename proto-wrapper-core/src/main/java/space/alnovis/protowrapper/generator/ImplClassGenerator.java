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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        // Implement extract methods for fields present in this version
        for (MergedField field : message.getFieldsSorted()) {
            if (field.getPresentInVersions().contains(version)) {
                // Handle INT_ENUM conflicts specially
                if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                    addIntEnumExtractImplementation(classBuilder, field, protoType, message, ctx);
                } else if (hasIncompatibleTypeConflict(field, version)) {
                    addMissingFieldImplementation(classBuilder, field, protoType, message, ctx);
                } else {
                    addExtractImplementation(classBuilder, field, protoType, message, ctx);
                }
            } else {
                addMissingFieldImplementation(classBuilder, field, protoType, message, ctx);
            }
        }

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

        for (MergedField field : nested.getFieldsSorted()) {
            if (field.getPresentInVersions().contains(version)) {
                // Handle INT_ENUM conflicts specially
                if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                    addIntEnumExtractImplementation(classBuilder, field, protoType, nested, ctx);
                } else if (hasIncompatibleTypeConflict(field, version)) {
                    addMissingFieldImplementation(classBuilder, field, protoType, nested, ctx);
                } else {
                    addExtractImplementation(classBuilder, field, protoType, nested, ctx);
                }
            } else {
                addMissingFieldImplementation(classBuilder, field, protoType, nested, ctx);
            }
        }

        classBuilder.addMethod(MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", className))
                .addParameter(protoType, "proto")
                .addStatement("return new $L(proto)", className)
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
        List<String> path = new ArrayList<>();
        MergedMessage current = nested;
        while (current.getParent() != null) {
            path.add(0, current.getName());
            current = current.getParent();
        }

        ClassName result = topLevelAbstract;
        for (String name : path) {
            result = result.nestedClass(name);
        }
        return result;
    }

    private void addExtractImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                          ClassName protoType, MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeName returnType = resolver.parseFieldType(field, message);
        String version = ctx.requireVersion();

        if (field.isOptional() && !field.isRepeated()) {
            String versionJavaName = getVersionSpecificJavaName(field, version, resolver);
            classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(protoType, "proto")
                    .addStatement("return proto.has$L()", versionJavaName)
                    .build());
        }

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(protoType, "proto");

        String getterCall = generateProtoGetterCall(field, message, ctx);
        extract.addStatement("return $L", getterCall);

        classBuilder.addMethod(extract.build());
    }

    /**
     * Add extract implementations for INT_ENUM conflict fields.
     * This generates both int and enum getter implementations.
     */
    private void addIntEnumExtractImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                                   ClassName protoType, MergedMessage message,
                                                   GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();

        String versionJavaName = getVersionSpecificJavaName(field, version, resolver);

        // Extract int value - unified getter type is int
        TypeName intReturnType = resolver.parseFieldType(field, message);

        // Add extractHas for optional fields
        if (field.isOptional() && !field.isRepeated()) {
            classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(protoType, "proto")
                    .addStatement("return proto.has$L()", versionJavaName)
                    .build());
        }

        // Main extract - returns int
        MethodSpec.Builder extractInt = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(intReturnType)
                .addParameter(protoType, "proto");

        if (versionIsEnum) {
            // Version has enum type - convert to int
            extractInt.addStatement("return proto.get$L().getNumber()", versionJavaName);
        } else {
            // Version has int type - return directly
            extractInt.addStatement("return proto.get$L()", versionJavaName);
        }
        classBuilder.addMethod(extractInt.build());

        // Enum extract - returns unified enum
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                .getConflictEnum(message.getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
            String enumExtractMethodName = "extract" + resolver.capitalize(field.getJavaName()) + "Enum";

            MethodSpec.Builder extractEnum = MethodSpec.methodBuilder(enumExtractMethodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(enumType)
                    .addParameter(protoType, "proto");

            if (versionIsEnum) {
                // Version has enum type - convert proto enum value to unified enum
                extractEnum.addStatement("return $T.fromProtoValue(proto.get$L().getNumber())",
                        enumType, versionJavaName);
            } else {
                // Version has int type - convert int to unified enum
                extractEnum.addStatement("return $T.fromProtoValue(proto.get$L())",
                        enumType, versionJavaName);
            }
            classBuilder.addMethod(extractEnum.build());
        }
    }

    private void addMissingFieldImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                               ClassName protoType, MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        TypeName returnType = resolver.parseFieldType(field, message);

        if (field.isOptional() && !field.isRepeated()) {
            classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(protoType, "proto")
                    .addStatement("return false")
                    .addJavadoc("Field not present in this version.\n")
                    .build());
        }

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(protoType, "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = resolver.getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);

        classBuilder.addMethod(extract.build());
    }

    /**
     * Check if a field has a type conflict that makes it incompatible for this version.
     * For the hybrid approach, significant type mismatches are considered incompatible.
     * Fields with type conflicts are treated as "not present" for that version.
     */
    private boolean hasIncompatibleTypeConflict(MergedField field, String version) {
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return false;
        }

        String mergedType = field.getGetterType();
        String versionType = versionField.getJavaType();

        // Exact match - no conflict
        if (mergedType.equals(versionType)) {
            return false;
        }

        // Treat primitive/wrapper as equivalent
        if (areEquivalentTypes(mergedType, versionType)) {
            return false;
        }

        // Check for message type mismatch (different message types)
        boolean mergedIsMessage = field.isMessage();
        boolean versionIsMessage = versionField.isMessage();
        if (mergedIsMessage != versionIsMessage) {
            return true; // One is message, other is not
        }

        // Check for enum vs non-enum
        boolean mergedIsEnum = field.isEnum();
        boolean versionIsEnum = versionField.isEnum();
        if (mergedIsEnum != versionIsEnum) {
            return true; // One is enum, other is not
        }

        // Same category (both primitives/wrappers) - check if widening is safe
        if (!mergedIsMessage && !mergedIsEnum) {
            // Safe widening cases: int→long, int→double (read), float→double
            if (isWideningConversion(mergedType, versionType)) {
                return false;
            }
        }

        // All other mismatches are incompatible
        return true;
    }

    /**
     * Check if two types are equivalent (primitive vs wrapper of same type).
     */
    private boolean areEquivalentTypes(String type1, String type2) {
        return normalizeType(type1).equals(normalizeType(type2));
    }

    /**
     * Normalize a type to its primitive form for comparison.
     */
    private String normalizeType(String type) {
        return switch (type) {
            case "Integer" -> "int";
            case "Long" -> "long";
            case "Double" -> "double";
            case "Float" -> "float";
            case "Boolean" -> "boolean";
            case "Short" -> "short";
            case "Byte" -> "byte";
            case "Character" -> "char";
            default -> type;
        };
    }

    /**
     * Check if reading versionType and returning mergedType is a safe widening conversion.
     */
    private boolean isWideningConversion(String mergedType, String versionType) {
        String normalizedMerged = normalizeType(mergedType);
        String normalizedVersion = normalizeType(versionType);

        // int can be safely widened to long or double
        if ("int".equals(normalizedVersion) && ("long".equals(normalizedMerged) || "double".equals(normalizedMerged))) {
            return true;
        }
        // float can be safely widened to double
        if ("float".equals(normalizedVersion) && "double".equals(normalizedMerged)) {
            return true;
        }
        // long can be safely widened to double (with potential precision loss, but still compiles)
        if ("long".equals(normalizedVersion) && "double".equals(normalizedMerged)) {
            return true;
        }
        return false;
    }

    private String extractSimpleTypeName(String typeName) {
        if (typeName == null) return "Object";
        if (typeName.startsWith("java.util.List<")) {
            typeName = typeName.substring("java.util.List<".length(), typeName.length() - 1);
        }
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    private String getVersionSpecificJavaName(MergedField field, String version, TypeResolver resolver) {
        Map<String, FieldInfo> versionFields = field.getVersionFields();
        FieldInfo versionField = versionFields.get(version);
        if (versionField != null) {
            return resolver.capitalize(versionField.getJavaName());
        }
        return resolver.capitalize(field.getJavaName());
    }

    private String generateProtoGetterCall(MergedField field, MergedMessage context, GenerationContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();

        String javaName = getVersionSpecificJavaName(field, version, resolver);
        String enumType = getEnumTypeForFromProtoValue(field, context, ctx);

        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        boolean mergedIsInt = "int".equals(field.getGetterType()) || "Integer".equals(field.getGetterType())
                || "long".equals(field.getGetterType()) || "Long".equals(field.getGetterType());

        boolean needsEnumToIntConversion = versionIsEnum && mergedIsInt;

        if (field.isRepeated()) {
            if (field.isEnum()) {
                return String.format("proto.get%sList().stream().map(e -> %s.fromProtoValue(e.getNumber())).collect(java.util.stream.Collectors.toList())",
                    javaName, enumType);
            } else if (field.isMessage()) {
                String wrapperClass = getWrapperClassName(field, context, ctx);
                return String.format("proto.get%sList().stream().map(%s::new).collect(java.util.stream.Collectors.toList())",
                    javaName, wrapperClass);
            }
            return String.format("proto.get%sList()", javaName);
        } else if (field.isEnum()) {
            return String.format("%s.fromProtoValue(proto.get%s().getNumber())", enumType, javaName);
        } else if (field.isMessage()) {
            String wrapperClass = getWrapperClassName(field, context, ctx);
            return String.format("new %s(proto.get%s())", wrapperClass, javaName);
        } else if ("byte[]".equals(field.getGetterType())) {
            return String.format("proto.get%s().toByteArray()", javaName);
        } else if (needsEnumToIntConversion) {
            return String.format("proto.get%s().getNumber()", javaName);
        } else {
            String mergedType = field.getGetterType();
            String versionType = versionField != null ? versionField.getJavaType() : mergedType;

            if (("Long".equals(mergedType) || "long".equals(mergedType)) && "int".equals(versionType)) {
                return String.format("(long) proto.get%s()", javaName);
            }

            return String.format("proto.get%s()", javaName);
        }
    }

    private String getWrapperClassName(MergedField field, MergedMessage context, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        String implPackage = ctx.getImplPackage();

        if (fullTypePath.contains(".")) {
            String[] parts = fullTypePath.split("\\.");
            StringBuilder result = new StringBuilder(implPackage);
            result.append(".").append(ctx.getImplClassName(parts[0]));
            for (int i = 1; i < parts.length; i++) {
                result.append(".").append(parts[i]);
            }
            return result.toString();
        } else {
            return implPackage + "." + ctx.getImplClassName(fullTypePath);
        }
    }

    private void addNestedBuilderImpl(TypeSpec.Builder classBuilder, MergedMessage nested,
                                        ClassName protoType, String className, GenerationContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();

        // Get the nested interface type
        ClassName nestedInterfaceType = resolver.buildNestedClassName(nested.getQualifiedInterfaceName());
        ClassName builderInterfaceType = nestedInterfaceType.nestedClass("Builder");

        // Get the parent abstract class and find nested abstract class
        ClassName topLevelAbstract = ClassName.get(ctx.getApiPackage() + IMPL_PACKAGE_SUFFIX,
                nested.getTopLevelParent().getAbstractClassName());
        ClassName abstractClass = buildNestedAbstractClassName(nested, topLevelAbstract);
        ClassName abstractBuilderType = abstractClass.nestedClass("AbstractBuilder");

        // Proto builder type
        ClassName protoBuilderType = protoType.nestedClass("Builder");

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

        // Generate BuilderImpl class
        TypeSpec builderImpl = generateNestedBuilderImplClass(nested, protoType, protoBuilderType,
                abstractBuilderType, nestedInterfaceType, className, ctx);
        classBuilder.addType(builderImpl);
    }

    private TypeSpec generateNestedBuilderImplClass(MergedMessage nested, ClassName protoType,
                                                     ClassName protoBuilderType, ClassName abstractBuilderType,
                                                     ClassName interfaceType, String implClassName,
                                                     GenerationContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();

        ParameterizedTypeName superType = ParameterizedTypeName.get(abstractBuilderType, protoType);

        TypeSpec.Builder builder = TypeSpec.classBuilder("BuilderImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(superType);

        builder.addField(protoBuilderType, "protoBuilder", Modifier.PRIVATE, Modifier.FINAL);

        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(protoBuilderType, "protoBuilder")
                .addStatement("this.protoBuilder = protoBuilder")
                .build());

        for (MergedField field : nested.getFieldsSorted()) {
            // Handle INT_ENUM conflicts with overloaded methods
            if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(nested.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    boolean presentInVersion = field.getPresentInVersions().contains(version);
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), presentInVersion, nested, ctx);
                    continue;
                }
            }

            // Handle WIDENING conflicts with range checking
            if (field.getConflictType() == MergedField.ConflictType.WIDENING) {
                boolean presentInVersion = field.getPresentInVersions().contains(version);
                addWideningBuilderMethods(builder, field, presentInVersion, nested, ctx);
                continue;
            }

            // Skip fields with other non-convertible type conflicts
            if (field.shouldSkipBuilderSetter()) {
                continue;
            }

            TypeName fieldType = resolver.parseFieldType(field, nested);
            boolean presentInVersion = field.getPresentInVersions().contains(version);

            if (field.isRepeated()) {
                TypeName singleElementType = extractListElementType(fieldType);
                addRepeatedFieldBuilderMethods(builder, field, singleElementType, fieldType,
                        presentInVersion, nested, ctx);
            } else {
                addSingleFieldBuilderMethods(builder, field, fieldType, presentInVersion, nested, ctx);
            }
        }

        builder.addMethod(MethodSpec.methodBuilder("doBuild")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(interfaceType)
                .addStatement("return new $L(protoBuilder.build())", implClassName)
                .build());

        // Helper method
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
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();

        // Interface builder type
        ClassName interfaceType = ClassName.get(ctx.getApiPackage(), message.getInterfaceName());
        ClassName builderInterfaceType = interfaceType.nestedClass("Builder");

        // Abstract builder from abstract class
        ClassName abstractClass = ClassName.get(ctx.getApiPackage() + IMPL_PACKAGE_SUFFIX, message.getAbstractClassName());
        ClassName abstractBuilderType = abstractClass.nestedClass("AbstractBuilder");

        // Proto builder type
        ClassName protoBuilderType = protoType.nestedClass("Builder");

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

        // Generate BuilderImpl class
        TypeSpec builderImpl = generateBuilderImplClass(message, protoType, protoBuilderType,
                abstractBuilderType, interfaceType, className, implPackage, ctx);
        classBuilder.addType(builderImpl);
    }

    private TypeSpec generateBuilderImplClass(MergedMessage message, ClassName protoType,
                                               ClassName protoBuilderType, ClassName abstractBuilderType,
                                               ClassName interfaceType, String implClassName,
                                               String implPackage, GenerationContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.getTypeResolver();

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
            // Handle INT_ENUM conflicts with overloaded methods
            if (field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                Optional<ConflictEnumInfo> enumInfoOpt = ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName());
                if (enumInfoOpt.isPresent()) {
                    boolean presentInVersion = field.getPresentInVersions().contains(version);
                    addIntEnumBuilderMethods(builder, field, enumInfoOpt.get(), presentInVersion, message, ctx);
                    continue;
                }
            }

            // Handle WIDENING conflicts with range checking
            if (field.getConflictType() == MergedField.ConflictType.WIDENING) {
                boolean presentInVersion = field.getPresentInVersions().contains(version);
                addWideningBuilderMethods(builder, field, presentInVersion, message, ctx);
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
                        presentInVersion, message, ctx);
            } else {
                addSingleFieldBuilderMethods(builder, field, fieldType, presentInVersion, message, ctx);
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
                .addComment("Use reflection to call getTypedProto() on wrapper")
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
                if ("long".equals(widerType) || "Long".equals(widerType)) {
                    // long -> int narrowing
                    doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                            field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
                    doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                            IllegalArgumentException.class, field.getJavaName(), version);
                    doSet.endControlFlow();
                    doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, field.getJavaName());
                } else if ("double".equals(widerType) || "Double".equals(widerType)) {
                    // double -> int narrowing (with range check)
                    doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                            field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
                    doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                            IllegalArgumentException.class, field.getJavaName(), version);
                    doSet.endControlFlow();
                    doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, field.getJavaName());
                } else {
                    // Unknown narrowing - just cast
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
        } else {
            return "protoBuilder.set" + versionJavaName + "($L)";
        }
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
        if (listType instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterized = (ParameterizedTypeName) listType;
            if (!parameterized.typeArguments.isEmpty()) {
                return parameterized.typeArguments.get(0);
            }
        }
        return ClassName.get(Object.class);
    }

    private String getEnumTypeForFromProtoValue(MergedField field, MergedMessage context, GenerationContext ctx) {
        if (!field.isEnum()) {
            return extractSimpleTypeName(field.getGetterType());
        }

        TypeResolver resolver = ctx.getTypeResolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        MergedSchema schema = ctx.getSchema();
        if (schema != null && schema.hasEquivalentTopLevelEnum(fullTypePath)) {
            return schema.getEquivalentTopLevelEnum(fullTypePath);
        }

        return fullTypePath;
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
