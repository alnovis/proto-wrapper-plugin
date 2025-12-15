package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedSchema;
import space.alnovis.protowrapper.model.MergedSchema.MergedField;
import space.alnovis.protowrapper.model.MergedSchema.MergedMessage;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
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
public class ImplClassGenerator {

    private final GeneratorConfig config;
    private MergedSchema schema;

    public ImplClassGenerator(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Set the merged schema for cross-message type resolution.
     */
    public void setSchema(MergedSchema schema) {
        this.schema = schema;
    }

    /**
     * Generate implementation class for a specific version.
     *
     * @param message Merged message info
     * @param version Version string (e.g., "v1")
     * @param protoClassName Fully qualified proto class name
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, String version, String protoClassName) {
        this.currentVersion = version;
        String className = message.getVersionClassName(version);
        String implPackage = config.getImplPackage(version);

        // Proto type
        ClassName protoType = ClassName.bestGuess(protoClassName);

        // Superclass: AbstractMoney<Proto.Money>
        ClassName abstractClass = ClassName.get(
                config.getApiPackage() + ".impl",
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
                // Check for incompatible type conflicts (e.g., message vs primitive)
                if (hasIncompatibleTypeConflict(field, version)) {
                    // Type in this version is incompatible with merged type - treat as missing
                    addMissingFieldImplementation(classBuilder, field, protoType, message);
                } else {
                    addExtractImplementation(classBuilder, field, protoType, message);
                }
            } else {
                // Field not present in this version - return default/null
                addMissingFieldImplementation(classBuilder, field, protoType, message);
            }
        }

        // Add serialization method
        classBuilder.addMethod(MethodSpec.methodBuilder("serializeToBytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(protoType, "proto")
                .addStatement("return proto.toByteArray()")
                .build());

        // Add wrapper version method
        int versionNum = extractVersionNumber(version);
        classBuilder.addMethod(MethodSpec.methodBuilder("extractWrapperVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addParameter(protoType, "proto")
                .addStatement("return $L", versionNum)
                .build());

        // Add factory method
        classBuilder.addMethod(MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(implPackage, className))
                .addParameter(protoType, "proto")
                .addStatement("return new $L(proto)", className)
                .build());

        // Add getTypedProto() for VersionContext
        classBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addModifiers(Modifier.PUBLIC)
                .returns(protoType)
                .addStatement("return proto")
                .addJavadoc("Get the underlying typed proto message.\n")
                .addJavadoc("@return $T\n", protoType)
                .build());

        TypeSpec classSpec = classBuilder.build();

        return JavaFile.builder(implPackage, classSpec)
                .addFileComment("Generated by proto-wrapper-maven-plugin. DO NOT EDIT.")
                .indent("    ")
                .build();
    }

    private void addExtractImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                          ClassName protoType, MergedMessage message) {
        TypeName returnType = parseFieldType(field, message);

        // Has method for optional fields (not repeated)
        if (field.isOptional() && !field.isRepeated()) {
            // Use version-specific field name
            String versionJavaName = getVersionSpecificJavaName(field);
            classBuilder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(protoType, "proto")
                    .addStatement("return proto.has$L()", versionJavaName)
                    .build());
        }

        // Main extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(protoType, "proto");

        // Generate getter call based on field type
        String getterCall = generateProtoGetterCall(field, message);
        extract.addStatement("return $L", getterCall);

        classBuilder.addMethod(extract.build());
    }

    private void addMissingFieldImplementation(TypeSpec.Builder classBuilder, MergedField field,
                                               ClassName protoType, MergedMessage message) {
        TypeName returnType = parseFieldType(field, message);

        // Has method - always return false for optional fields
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

        // Main extract - return default value
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(protoType, "proto")
                .addJavadoc("Field not present in this version.\n");

        // Return appropriate default
        String defaultValue = getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);

        classBuilder.addMethod(extract.build());
    }

    /**
     * Current version being generated - set during generate().
     */
    private String currentVersion;

    /**
     * Check if field type in this version is incompatible with merged type.
     * This happens when the same field number has completely different types in different versions
     * (e.g., primitive vs message, or different message types).
     */
    private boolean hasIncompatibleTypeConflict(MergedField field, String version) {
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return false; // Not present in this version
        }

        String mergedType = field.getGetterType();
        String versionType = versionField.getJavaType();

        // If types are the same, no conflict
        if (mergedType.equals(versionType)) {
            return false;
        }

        // Check for message vs primitive conflict
        boolean mergedIsMessage = field.isMessage();
        boolean versionIsMessage = versionField.isMessage();
        if (mergedIsMessage != versionIsMessage) {
            return true; // Incompatible: one is message, other is primitive
        }

        // Check for different message types
        if (mergedIsMessage && versionIsMessage) {
            String mergedTypeName = extractSimpleTypeName(mergedType);
            String versionTypeName = extractSimpleTypeName(versionType);
            if (!mergedTypeName.equals(versionTypeName)) {
                return true; // Different message types
            }
        }

        // Primitive type widening is OK (int -> long), handled in generateProtoGetterCall
        return false;
    }

    /**
     * Get the version-specific Java field name capitalized.
     * Field names can differ between versions (e.g., "tax_type" in v1 vs "type" in v2).
     */
    private String getVersionSpecificJavaName(MergedField field) {
        Map<String, FieldInfo> versionFields = field.getVersionFields();
        FieldInfo versionField = versionFields.get(currentVersion);
        if (versionField != null) {
            return capitalize(versionField.getJavaName());
        }
        // Fallback to merged name
        return capitalize(field.getJavaName());
    }

    private String generateProtoGetterCall(MergedField field, MergedMessage context) {
        // Use version-specific field name, since field names can differ between versions
        String javaName = getVersionSpecificJavaName(field);
        // For enums, use full nested path (e.g., "Product.CategoryEnum" for nested enums)
        String enumType = getEnumTypeForFromProtoValue(field, context);

        // Check if version-specific type is different from merged type
        // This happens when a field is int in v1 but enum in v2 (or vice versa)
        FieldInfo versionField = field.getVersionFields().get(currentVersion);
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        boolean mergedIsInt = "int".equals(field.getGetterType()) || "Integer".equals(field.getGetterType())
                || "long".equals(field.getGetterType()) || "Long".equals(field.getGetterType());

        // If version type is enum but merged type expects int/Integer, convert via getNumber()
        boolean needsEnumToIntConversion = versionIsEnum && mergedIsInt;

        if (field.isRepeated()) {
            if (field.isEnum()) {
                // For repeated enums, need to convert each value
                return String.format("proto.get%sList().stream().map(e -> %s.fromProtoValue(e.getNumber())).collect(java.util.stream.Collectors.toList())",
                    javaName, enumType);
            } else if (field.isMessage()) {
                // For repeated messages, need to wrap each element
                String wrapperClass = getWrapperClassName(field, context);
                return String.format("proto.get%sList().stream().map(%s::new).collect(java.util.stream.Collectors.toList())",
                    javaName, wrapperClass);
            }
            return String.format("proto.get%sList()", javaName);
        } else if (field.isEnum()) {
            // Convert proto enum to our enum via numeric value
            return String.format("%s.fromProtoValue(proto.get%s().getNumber())", enumType, javaName);
        } else if (field.isMessage()) {
            // For nested messages, need to wrap
            String wrapperClass = getWrapperClassName(field, context);
            return String.format("new %s(proto.get%s())", wrapperClass, javaName);
        } else if ("byte[]".equals(field.getGetterType())) {
            // ByteString to byte[] conversion
            return String.format("proto.get%s().toByteArray()", javaName);
        } else if (needsEnumToIntConversion) {
            // Version has enum but merged type is int - convert via getNumber()
            return String.format("proto.get%s().getNumber()", javaName);
        } else {
            // Check for primitive type widening (e.g., int -> Long)
            String mergedType = field.getGetterType();
            String versionType = versionField != null ? versionField.getJavaType() : mergedType;

            // int to Long/long widening
            if (("Long".equals(mergedType) || "long".equals(mergedType)) && "int".equals(versionType)) {
                return String.format("(long) proto.get%s()", javaName);
            }

            return String.format("proto.get%s()", javaName);
        }
    }

    /**
     * Get the version-specific wrapper class name for a message field.
     * E.g., for field type "DateTime" in v2, returns fully qualified "com.example.model.v2.DateTimeV2".
     * For nested types like "Report.Section" returns "com.example.model.v2.ReportSectionV2".
     */
    private String getWrapperClassName(MergedField field, MergedMessage context) {
        // Get the full type path from proto type name
        String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        // Check if it's a nested type (contains dots like "Report.Section")
        String flattenedTypeName;
        if (fullTypePath.contains(".")) {
            // Nested type - flatten the name
            flattenedTypeName = fullTypePath.replace(".", "");
        } else {
            flattenedTypeName = fullTypePath;
        }

        // Build the wrapper class name with version suffix
        String versionSuffix = currentVersion.substring(0, 1).toUpperCase() + currentVersion.substring(1);
        String simpleClassName = flattenedTypeName + versionSuffix;

        // Return fully qualified name
        String implPackage = config.getImplPackage(currentVersion);
        return implPackage + "." + simpleClassName;
    }

    private String extractSimpleTypeName(String typeName) {
        if (typeName == null) return "Object";
        // Handle List<EnumType> -> EnumType
        if (typeName.startsWith("java.util.List<")) {
            typeName = typeName.substring("java.util.List<".length(), typeName.length() - 1);
        }
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    /**
     * Get enum type string for fromProtoValue calls.
     * For nested enums (e.g., Product.CategoryEnum), returns the full nested path.
     * For top-level enums, returns the simple name.
     * If a nested enum has an equivalent top-level enum, returns the top-level name.
     */
    private String getEnumTypeForFromProtoValue(MergedField field, MergedMessage context) {
        if (!field.isEnum()) {
            return extractSimpleTypeName(field.getGetterType());
        }

        // Use the nested type path which includes parent message for nested enums
        String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        // Check if this nested enum path has an equivalent top-level enum
        if (schema != null && schema.hasEquivalentTopLevelEnum(fullTypePath)) {
            return schema.getEquivalentTopLevelEnum(fullTypePath);
        }

        // fullTypePath is like "Product.CategoryEnum" for nested enums
        // or just "CommandTypeEnum" for top-level enums
        return fullTypePath;
    }

    private String getDefaultValue(String typeName) {
        switch (typeName) {
            case "int": return "0";
            case "long": return "0L";
            case "double": return "0.0";
            case "float": return "0.0f";
            case "boolean": return "false";
            default: return "null";
        }
    }

    /**
     * Parse field type using the full nested type path from proto type name.
     */
    private TypeName parseFieldType(MergedField field, MergedMessage context) {
        String getterType = field.getGetterType();

        // Handle primitives
        switch (getterType) {
            case "int": return TypeName.INT;
            case "long": return TypeName.LONG;
            case "double": return TypeName.DOUBLE;
            case "float": return TypeName.FLOAT;
            case "boolean": return TypeName.BOOLEAN;
            case "byte[]": return ArrayTypeName.of(TypeName.BYTE);
            case "String": return ClassName.get(String.class);
            case "Integer": return ClassName.get(Integer.class);
            case "Long": return ClassName.get(Long.class);
            case "Double": return ClassName.get(Double.class);
            case "Float": return ClassName.get(Float.class);
            case "Boolean": return ClassName.get(Boolean.class);
        }

        // Handle List<T>
        if (getterType.startsWith("java.util.List<")) {
            // Extract the inner type from the getter type
            String innerTypeName = getterType.substring("java.util.List<".length(), getterType.length() - 1);

            // Check if it's a primitive/wrapper type first
            TypeName innerType = parsePrimitiveOrWrapperType(innerTypeName);
            if (innerType != null) {
                return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
            }

            // For message/enum types, use the full nested type path
            String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
            String fullTypePath = field.getNestedTypePath(protoPackage);
            innerType = resolveTypePath(fullTypePath, context);
            return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
        }

        // For message and enum types, use full nested type path
        String protoPackage = extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);
        return resolveTypePath(fullTypePath, context);
    }

    /**
     * Parse primitive and wrapper types. Returns null if not a primitive/wrapper type.
     */
    private TypeName parsePrimitiveOrWrapperType(String typeName) {
        switch (typeName) {
            case "int": return TypeName.INT;
            case "long": return TypeName.LONG;
            case "double": return TypeName.DOUBLE;
            case "float": return TypeName.FLOAT;
            case "boolean": return TypeName.BOOLEAN;
            case "byte[]": return ArrayTypeName.of(TypeName.BYTE);
            case "String": return ClassName.get(String.class);
            case "Integer": return ClassName.get(Integer.class);
            case "Long": return ClassName.get(Long.class);
            case "Double": return ClassName.get(Double.class);
            case "Float": return ClassName.get(Float.class);
            case "Boolean": return ClassName.get(Boolean.class);
            default: return null;
        }
    }

    /**
     * Extract the proto package from the pattern.
     */
    private String extractProtoPackage(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return "";
        }
        String javaPackage = pattern.replace("{version}", "v1");
        String[] parts = javaPackage.split("\\.");
        if (parts.length >= 3) {
            StringBuilder protoPackage = new StringBuilder();
            for (int i = Math.max(0, parts.length - 3); i < parts.length; i++) {
                if (protoPackage.length() > 0) {
                    protoPackage.append(".");
                }
                protoPackage.append(parts[i]);
            }
            return protoPackage.toString();
        }
        return javaPackage;
    }

    /**
     * Resolve a type path that might be a cross-message nested type.
     */
    private TypeName resolveTypePath(String typePath, MergedMessage context) {
        // If typePath contains dots, it might be a cross-message path like "Order.ShippingInfo"
        if (typePath.contains(".")) {
            // Check if this nested path has an equivalent top-level enum
            if (schema != null && schema.hasEquivalentTopLevelEnum(typePath)) {
                String topLevelName = schema.getEquivalentTopLevelEnum(typePath);
                return ClassName.get(config.getApiPackage(), topLevelName);
            }

            String[] parts = typePath.split("\\.");
            String parentMessageName = parts[0];
            MergedMessage topLevel = context.getTopLevelParent();

            if (topLevel.getName().equals(parentMessageName)) {
                return buildNestedClassName(typePath);
            }

            // Cross-message reference
            if (schema != null) {
                java.util.Optional<MergedMessage> crossMessage = schema.findMessageByPath(typePath);
                if (crossMessage.isPresent()) {
                    return buildNestedClassName(typePath);
                }
                java.util.Optional<MergedSchema.MergedEnum> crossEnum = schema.findEnumByPath(typePath);
                if (crossEnum.isPresent()) {
                    return buildNestedClassName(typePath);
                }
            }

            return buildNestedClassName(typePath);
        }

        // Simple name - check if this enum has an equivalent top-level
        String nestedPath = context.getQualifiedInterfaceName() + "." + typePath;
        if (schema != null && schema.hasEquivalentTopLevelEnum(nestedPath)) {
            String topLevelName = schema.getEquivalentTopLevelEnum(nestedPath);
            return ClassName.get(config.getApiPackage(), topLevelName);
        }

        // Simple name - check in context hierarchy first
        java.util.Optional<MergedMessage> nestedOpt = context.findNestedMessageRecursive(typePath);
        if (nestedOpt.isPresent()) {
            return buildNestedClassName(nestedOpt.get().getQualifiedInterfaceName());
        }

        java.util.Optional<MergedSchema.MergedEnum> nestedEnumOpt = context.findNestedEnumRecursive(typePath);
        if (nestedEnumOpt.isPresent()) {
            // Check if nested enum has an equivalent top-level
            String enumPath = context.getTopLevelParent().getName() + "." + typePath;
            if (schema != null && schema.hasEquivalentTopLevelEnum(enumPath)) {
                String topLevelName = schema.getEquivalentTopLevelEnum(enumPath);
                return ClassName.get(config.getApiPackage(), topLevelName);
            }
            return buildNestedClassName(enumPath);
        }

        if (context.isNested()) {
            MergedMessage topLevel = context.getTopLevelParent();
            java.util.Optional<MergedMessage> siblingOpt = topLevel.findNestedMessageRecursive(typePath);
            if (siblingOpt.isPresent()) {
                return buildNestedClassName(siblingOpt.get().getQualifiedInterfaceName());
            }
            java.util.Optional<MergedSchema.MergedEnum> siblingEnumOpt = topLevel.findNestedEnumRecursive(typePath);
            if (siblingEnumOpt.isPresent()) {
                // Check if sibling enum has an equivalent top-level
                String siblingEnumPath = topLevel.getName() + "." + typePath;
                if (schema != null && schema.hasEquivalentTopLevelEnum(siblingEnumPath)) {
                    String topLevelName = schema.getEquivalentTopLevelEnum(siblingEnumPath);
                    return ClassName.get(config.getApiPackage(), topLevelName);
                }
                return buildNestedClassName(siblingEnumPath);
            }
        }

        // Not a nested type - assume it's a top-level type
        return ClassName.get(config.getApiPackage(), typePath);
    }

    private TypeName parseTypeName(String typeName, MergedMessage context) {
        switch (typeName) {
            case "int": return TypeName.INT;
            case "long": return TypeName.LONG;
            case "double": return TypeName.DOUBLE;
            case "float": return TypeName.FLOAT;
            case "boolean": return TypeName.BOOLEAN;
            case "byte[]": return ArrayTypeName.of(TypeName.BYTE);
            case "String": return ClassName.get(String.class);
            case "Integer": return ClassName.get(Integer.class);
            case "Long": return ClassName.get(Long.class);
            case "Double": return ClassName.get(Double.class);
            case "Float": return ClassName.get(Float.class);
            case "Boolean": return ClassName.get(Boolean.class);
        }

        if (typeName.startsWith("java.util.List<")) {
            String inner = typeName.substring("java.util.List<".length(), typeName.length() - 1);
            TypeName innerType = parseTypeName(inner, context);
            return ParameterizedTypeName.get(ClassName.get(java.util.List.class), innerType);
        }

        // Use resolveTypePath for everything else
        return resolveTypePath(typeName, context);
    }

    /**
     * Build a ClassName for a nested type like "Order.Tax" or "Order.Item.Detail".
     */
    private ClassName buildNestedClassName(String qualifiedPath) {
        String[] parts = qualifiedPath.split("\\.");
        if (parts.length == 1) {
            return ClassName.get(config.getApiPackage(), parts[0]);
        }
        // For nested classes, first part is top-level class, rest are nested
        ClassName result = ClassName.get(config.getApiPackage(), parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = result.nestedClass(parts[i]);
        }
        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int extractVersionNumber(String version) {
        // Extract number from "v1" -> 1
        String numStr = version.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Write generated class to file.
     */
    public void writeToFile(JavaFile javaFile) throws IOException {
        javaFile.writeTo(config.getOutputDirectory());
    }

    /**
     * Generate and write implementation class.
     */
    public Path generateAndWrite(MergedMessage message, String version, String protoClassName) throws IOException {
        JavaFile javaFile = generate(message, version, protoClassName);
        writeToFile(javaFile);

        String implPackage = config.getImplPackage(version);
        String relativePath = implPackage.replace('.', '/')
                + "/" + message.getVersionClassName(version) + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate and write implementation class for a nested message (with flattened name).
     */
    public Path generateAndWriteNested(MergedMessage nested, String version, String protoClassName,
                                       MergedMessage parent) throws IOException {
        JavaFile javaFile = generateNested(nested, version, protoClassName);
        writeToFile(javaFile);

        String implPackage = config.getImplPackage(version);
        String relativePath = implPackage.replace('.', '/')
                + "/" + nested.getFlattenedVersionClassName(version) + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Generate impl class for a nested message with flattened class name.
     * E.g., for Report.Section in v2, generates ReportSectionV2.
     */
    public JavaFile generateNested(MergedMessage nested, String version, String protoClassName) {
        this.currentVersion = version;
        String className = nested.getFlattenedVersionClassName(version);
        String implPackage = config.getImplPackage(version);

        // Proto type
        ClassName protoType = ClassName.bestGuess(protoClassName);

        // Superclass: AbstractReportSection<Proto.Report.Section>
        ClassName abstractClass = ClassName.get(
                config.getApiPackage() + ".impl",
                nested.getFlattenedAbstractClassName()
        );
        ParameterizedTypeName superType = ParameterizedTypeName.get(abstractClass, protoType);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(superType)
                .addJavadoc("$L implementation of $L interface.\n\n",
                        version.toUpperCase(), nested.getQualifiedInterfaceName())
                .addJavadoc("@see $L\n", nested.getFlattenedAbstractClassName());

        // Constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(protoType, "proto")
                .addStatement("super(proto)")
                .build());

        // Implement extract methods for fields present in this version
        for (MergedField field : nested.getFieldsSorted()) {
            if (field.getPresentInVersions().contains(version)) {
                addExtractImplementation(classBuilder, field, protoType, nested);
            } else {
                addMissingFieldImplementation(classBuilder, field, protoType, nested);
            }
        }

        // Add factory method
        classBuilder.addMethod(MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(implPackage, className))
                .addParameter(protoType, "proto")
                .addStatement("return new $L(proto)", className)
                .build());

        TypeSpec classSpec = classBuilder.build();

        return JavaFile.builder(implPackage, classSpec)
                .addFileComment("Generated by proto-wrapper-maven-plugin. DO NOT EDIT.")
                .indent("    ")
                .build();
    }
}
