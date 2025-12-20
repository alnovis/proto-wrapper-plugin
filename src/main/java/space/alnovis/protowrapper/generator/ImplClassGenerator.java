package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
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
                if (hasIncompatibleTypeConflict(field, version)) {
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
                addExtractImplementation(classBuilder, field, protoType, nested, ctx);
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

    private boolean hasIncompatibleTypeConflict(MergedField field, String version) {
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return false;
        }

        String mergedType = field.getGetterType();
        String versionType = versionField.getJavaType();

        if (mergedType.equals(versionType)) {
            return false;
        }

        boolean mergedIsMessage = field.isMessage();
        boolean versionIsMessage = versionField.isMessage();
        if (mergedIsMessage != versionIsMessage) {
            return true;
        }

        if (mergedIsMessage && versionIsMessage) {
            String mergedTypeName = extractSimpleTypeName(mergedType);
            String versionTypeName = extractSimpleTypeName(versionType);
            if (!mergedTypeName.equals(versionTypeName)) {
                return true;
            }
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
