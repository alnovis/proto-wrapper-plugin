package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.generator.TypeResolver;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Helper class containing code generation utilities shared by conflict handlers.
 *
 * <p>This class centralizes common code generation patterns to avoid duplication
 * across different handlers.</p>
 */
public final class CodeGenerationHelper {

    private CodeGenerationHelper() {
        // Utility class
    }

    /**
     * Get the version-specific Java name for a field.
     */
    public static String getVersionSpecificJavaName(MergedField field, ProcessingContext ctx) {
        return Optional.ofNullable(ctx.version())
                .flatMap(version -> Optional.ofNullable(field.getVersionFields().get(version)))
                .map(vf -> ctx.capitalize(vf.getJavaName()))
                .orElseGet(() -> ctx.capitalize(field.getJavaName()));
    }

    /**
     * Get the wrapper class name for a message field.
     */
    public static String getWrapperClassName(MergedField field, ProcessingContext ctx) {
        TypeResolver resolver = ctx.resolver();
        GeneratorConfig config = ctx.config();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        String implPackage = ctx.implPackage();

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

    /**
     * Get the enum type for fromProtoValue conversion.
     */
    public static String getEnumTypeForFromProtoValue(MergedField field, ProcessingContext ctx) {
        if (!field.isEnum()) {
            return extractSimpleTypeName(field.getGetterType());
        }

        TypeResolver resolver = ctx.resolver();
        GeneratorConfig config = ctx.config();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
        String fullTypePath = field.getNestedTypePath(protoPackage);

        MergedSchema schema = ctx.schema();
        if (schema != null && schema.hasEquivalentTopLevelEnum(fullTypePath)) {
            return schema.getEquivalentTopLevelEnum(fullTypePath);
        }

        return fullTypePath;
    }

    /**
     * Extract simple type name from a potentially qualified name.
     */
    public static String extractSimpleTypeName(String typeName) {
        if (typeName == null) {
            return "Object";
        }
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    /**
     * Generate the proto getter call for a field.
     */
    public static String generateProtoGetterCall(MergedField field, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        TypeResolver resolver = ctx.resolver();

        String javaName = getVersionSpecificJavaName(field, ctx);
        String enumType = getEnumTypeForFromProtoValue(field, ctx);

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
                String wrapperClass = getWrapperClassName(field, ctx);
                return String.format("proto.get%sList().stream().map(%s::new).collect(java.util.stream.Collectors.toList())",
                        javaName, wrapperClass);
            } else if (field.getGetterType().contains("byte[]")) {
                // Convert List<ByteString> to List<byte[]>
                return String.format("proto.get%sList().stream().map(com.google.protobuf.ByteString::toByteArray).collect(java.util.stream.Collectors.toList())",
                        javaName);
            }
            return String.format("proto.get%sList()", javaName);
        } else if (field.isEnum()) {
            return String.format("%s.fromProtoValue(proto.get%s().getNumber())", enumType, javaName);
        } else if (field.isMessage()) {
            String wrapperClass = getWrapperClassName(field, ctx);
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

    /**
     * Get default value for a type.
     */
    public static String getDefaultValue(String javaType, TypeResolver resolver) {
        return resolver.getDefaultValue(javaType);
    }

    /**
     * Get the proto type for a message field.
     */
    public static String getProtoTypeForField(MergedField field, ProcessingContext ctx,
                                               String currentProtoClassName) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return "com.google.protobuf.Message";
        }

        String typeName = versionField.getTypeName();
        if (typeName.startsWith(".")) {
            typeName = typeName.substring(1);
        }

        GeneratorConfig config = ctx.config();
        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.resolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        String messagePath = versionField.extractNestedTypePath(protoPackage);

        MergedSchema schema = ctx.schema();
        String outerClassName = findOuterClassForType(messagePath, schema, version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + messagePath;
        }

        if (currentProtoClassName != null && currentProtoClassName.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProtoClassName.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + messagePath;
            }
        }

        return javaProtoPackage + "." + messagePath;
    }

    /**
     * Find the outer class name for a message type.
     */
    public static String findOuterClassForType(String messagePath, MergedSchema schema, String version) {
        if (schema == null || messagePath == null) {
            return null;
        }

        String topLevelName = messagePath.contains(".")
                ? messagePath.substring(0, messagePath.indexOf('.'))
                : messagePath;

        return schema.getMessage(topLevelName)
                .map(msg -> msg.getOuterClassName(version))
                .orElse(null);
    }

    /**
     * Get the proto enum type for a field.
     */
    public static String getProtoEnumTypeForField(MergedField field, ProcessingContext ctx,
                                                    String currentProtoClassName) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        if (versionField == null) {
            return "Object";
        }

        String typeName = versionField.getTypeName();
        if (typeName.startsWith(".")) {
            typeName = typeName.substring(1);
        }

        GeneratorConfig config = ctx.config();
        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.resolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        String enumPath = versionField.extractNestedTypePath(protoPackage);

        MergedSchema schema = ctx.schema();
        String outerClassName = findOuterClassForEnum(enumPath, schema, version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + enumPath;
        }

        if (currentProtoClassName != null && currentProtoClassName.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProtoClassName.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + enumPath;
            }
        }

        return javaProtoPackage + "." + enumPath;
    }

    /**
     * Find the outer class name for an enum type.
     */
    public static String findOuterClassForEnum(String enumPath, MergedSchema schema, String version) {
        if (schema == null || enumPath == null) {
            return null;
        }

        if (!enumPath.contains(".")) {
            return schema.getEnum(enumPath)
                    .map(e -> e.getOuterClassName(version))
                    .orElse(null);
        } else {
            String topLevelName = enumPath.substring(0, enumPath.indexOf('.'));
            return schema.getMessage(topLevelName)
                    .map(msg -> msg.getOuterClassName(version))
                    .orElse(null);
        }
    }

    /**
     * Get the enum conversion method name based on protobuf version.
     */
    public static String getEnumFromIntMethod(GeneratorConfig config) {
        return config.isProtobuf2() ? "valueOf" : "forNumber";
    }

    /**
     * Get message type for PRIMITIVE_MESSAGE conflict field.
     * Handles nested types correctly (e.g., TicketRequest.ParentTicket).
     */
    public static TypeName getMessageTypeForField(MergedField field, ProcessingContext ctx) {
        return field.getVersionFields().values().stream()
                .filter(fieldInfo -> !fieldInfo.isPrimitive() && fieldInfo.getTypeName() != null)
                .findFirst()
                .map(fieldInfo -> {
                    TypeResolver resolver = ctx.resolver();
                    GeneratorConfig config = ctx.config();
                    String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
                    String fullTypePath = fieldInfo.extractNestedTypePath(protoPackage);

                    if (fullTypePath.contains(".")) {
                        // Nested type: e.g., "TicketRequest.ParentTicket"
                        String[] parts = fullTypePath.split("\\.");
                        // First part is the outer class, rest are nested
                        String[] nestedParts = java.util.Arrays.copyOfRange(parts, 1, parts.length);
                        return (TypeName) ClassName.get(ctx.apiPackage(), parts[0], nestedParts);
                    } else {
                        // Top-level type
                        return (TypeName) ClassName.get(ctx.apiPackage(), fullTypePath);
                    }
                })
                .orElse(null);
    }

    /**
     * Get the message wrapper class name for a PRIMITIVE_MESSAGE conflict field.
     */
    public static String getMessageWrapperClassName(MergedField field, ProcessingContext ctx) {
        String version = ctx.version();
        for (Map.Entry<String, FieldInfo> entry : field.getVersionFields().entrySet()) {
            if (entry.getKey().equals(version)) {
                FieldInfo fieldInfo = entry.getValue();
                if (!fieldInfo.isPrimitive() && fieldInfo.getTypeName() != null) {
                    TypeResolver resolver = ctx.resolver();
                    GeneratorConfig config = ctx.config();
                    String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());
                    String fullTypePath = fieldInfo.extractNestedTypePath(protoPackage);
                    String implPackage = ctx.implPackage();

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
            }
        }
        return null;
    }

    // ============== Version-Conditional Helpers ==============

    private static final String VERSION_NOT_PRESENT_COMMENT = "Field not present in this version - ignored";

    /**
     * Add a version-conditional block to a method.
     * If presentInVersion is true, executes the ifPresent consumer.
     * Otherwise, adds a comment indicating the field is not present.
     *
     * @param method          The method builder to add code to
     * @param presentInVersion Whether the field is present in the current version
     * @param ifPresent       Consumer that adds code when field is present
     */
    public static void addVersionConditional(MethodSpec.Builder method,
                                              boolean presentInVersion,
                                              Consumer<MethodSpec.Builder> ifPresent) {
        if (presentInVersion) {
            ifPresent.accept(method);
        } else {
            method.addComment(VERSION_NOT_PRESENT_COMMENT);
        }
    }

    /**
     * Add a version-conditional statement to a method.
     * If presentInVersion is true, adds the statement with format and args.
     * Otherwise, adds a comment indicating the field is not present.
     *
     * @param method          The method builder to add code to
     * @param presentInVersion Whether the field is present in the current version
     * @param format          The statement format string
     * @param args            Arguments for the format string
     */
    public static void addVersionConditionalStatement(MethodSpec.Builder method,
                                                       boolean presentInVersion,
                                                       String format,
                                                       Object... args) {
        if (presentInVersion) {
            method.addStatement(format, args);
        } else {
            method.addComment(VERSION_NOT_PRESENT_COMMENT);
        }
    }

    /**
     * Add a version-conditional clear statement to a method.
     * Common pattern for doClear methods in builders.
     *
     * @param method          The method builder to add code to
     * @param presentInVersion Whether the field is present in the current version
     * @param versionJavaName The capitalized Java name for this version
     */
    public static void addVersionConditionalClear(MethodSpec.Builder method,
                                                   boolean presentInVersion,
                                                   String versionJavaName) {
        addVersionConditionalStatement(method, presentInVersion,
                "protoBuilder.clear$L()", versionJavaName);
    }

    // ============== Field-Type Dispatch ==============

    /**
     * Functional interface for field-type-specific actions during code generation.
     */
    @FunctionalInterface
    public interface FieldTypeAction {
        void apply(MethodSpec.Builder method, MergedField field,
                   String versionJavaName, ProcessingContext ctx);
    }

    /**
     * Dispatcher for field-type-specific code generation.
     * Eliminates repeated if(isMessage)/else if(isEnum)/else patterns.
     */
    public record FieldTypeDispatcher(
            FieldTypeAction messageAction,
            FieldTypeAction enumAction,
            FieldTypeAction primitiveAction
    ) {
        /**
         * Dispatch to the appropriate action based on field type.
         */
        public void dispatch(MethodSpec.Builder method, MergedField field,
                             String versionJavaName, ProcessingContext ctx) {
            if (field.isMessage()) {
                messageAction.apply(method, field, versionJavaName, ctx);
            } else if (field.isEnum()) {
                enumAction.apply(method, field, versionJavaName, ctx);
            } else {
                primitiveAction.apply(method, field, versionJavaName, ctx);
            }
        }
    }

    /**
     * Dispatcher for adding a single element to a repeated field.
     * Uses protoBuilder.add$L(...).
     */
    public static final FieldTypeDispatcher ADD_SINGLE_DISPATCHER = new FieldTypeDispatcher(
            // Message
            (m, f, n, c) -> {
                String protoTypeName = getProtoTypeForField(f, c, null);
                m.addStatement("protoBuilder.add$L(($L) extractProto($L))",
                        n, protoTypeName, f.getJavaName());
            },
            // Enum
            (m, f, n, c) -> {
                String protoEnumType = getProtoEnumTypeForField(f, c, null);
                String enumMethod = getEnumFromIntMethod(c.config());
                m.addStatement("protoBuilder.add$L($L.$L($L.getValue()))",
                        n, protoEnumType, enumMethod, f.getJavaName());
            },
            // Primitive
            (m, f, n, c) -> m.addStatement("protoBuilder.add$L($L)", n, f.getJavaName())
    );

    /**
     * Dispatcher for adding all elements to a repeated field.
     * Uses forEach for message/enum or addAll$L for primitives.
     */
    public static final FieldTypeDispatcher ADD_ALL_DISPATCHER = new FieldTypeDispatcher(
            // Message
            (m, f, n, c) -> {
                String protoTypeName = getProtoTypeForField(f, c, null);
                m.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) extractProto(e)))",
                        f.getJavaName(), n, protoTypeName);
            },
            // Enum
            (m, f, n, c) -> {
                String protoEnumType = getProtoEnumTypeForField(f, c, null);
                String enumMethod = getEnumFromIntMethod(c.config());
                m.addStatement("$L.forEach(e -> protoBuilder.add$L($L.$L(e.getValue())))",
                        f.getJavaName(), n, protoEnumType, enumMethod);
            },
            // Primitive
            (m, f, n, c) -> m.addStatement("protoBuilder.addAll$L($L)", n, f.getJavaName())
    );

    /**
     * Generate a proto setter call string for single (non-repeated) fields.
     * Returns a format string with $L placeholder for the field value.
     *
     * <p>Note: For enum fields, this generates a simple setter without null validation.
     * Use {@link #addEnumSetterWithValidation} instead for proper error messages.</p>
     */
    public static String generateProtoSetterCall(MergedField field, String versionJavaName, ProcessingContext ctx) {
        if (field.isMessage()) {
            String protoTypeName = getProtoTypeForField(field, ctx, null);
            return "protoBuilder.set" + versionJavaName + "((" + protoTypeName + ") extractProto($L))";
        } else if (field.isEnum()) {
            String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
            String enumMethod = getEnumFromIntMethod(ctx.config());
            return "protoBuilder.set" + versionJavaName + "(" + protoEnumType + "." + enumMethod + "($L.getValue()))";
        } else {
            return "protoBuilder.set" + versionJavaName + "($L)";
        }
    }

    /**
     * Add enum setter statements with null validation to a method builder.
     *
     * <p>Generates code like:</p>
     * <pre>
     * ProtoEnumType protoEnumValue = ProtoEnumType.valueOf(param.getValue());
     * if (protoEnumValue == null) {
     *     throw new IllegalArgumentException(
     *         "EnumType.VALUE (value=N) is not supported in protocol version V");
     * }
     * protoBuilder.setFieldName(protoEnumValue);
     * </pre>
     *
     * @param method The method builder to add statements to
     * @param field The enum field being set
     * @param versionJavaName The Java name of the field in this version
     * @param fieldParamName The parameter name holding the enum value
     * @param ctx Processing context
     */
    public static void addEnumSetterWithValidation(MethodSpec.Builder method, MergedField field,
                                                    String versionJavaName, String fieldParamName,
                                                    ProcessingContext ctx) {
        String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
        String enumMethod = getEnumFromIntMethod(ctx.config());
        String localVarName = "protoEnumValue";
        String enumTypeName = field.getGetterType();

        // ProtoEnumType protoEnumValue = ProtoEnumType.valueOf(param.getValue());
        method.addStatement("$L $L = $L.$L($L.getValue())",
                protoEnumType, localVarName, protoEnumType, enumMethod, fieldParamName);

        // if (protoEnumValue == null) { throw ... }
        method.beginControlFlow("if ($L == null)", localVarName);
        method.addStatement("throw new $T($S + $L.name() + $S + $L.getValue() + $S + getVersion())",
                IllegalArgumentException.class,
                enumTypeName + ".",
                fieldParamName,
                " (value=",
                fieldParamName,
                ") is not supported in protocol version ");
        method.endControlFlow();

        // protoBuilder.setFieldName(protoEnumValue);
        method.addStatement("protoBuilder.set$L($L)", versionJavaName, localVarName);
    }

    /**
     * Check if a field is an enum type.
     */
    public static boolean isEnumField(MergedField field) {
        return field.isEnum();
    }

    // ============== Version-Specific Field Validation ==============

    /**
     * Add a throw statement for when trying to set a field that doesn't exist in the current version.
     *
     * <p>Generates code like:</p>
     * <pre>
     * throw new UnsupportedOperationException(
     *     "Field 'exciseStamp' is not available in protocol version 203. " +
     *     "This field exists only in versions: [v202]");
     * </pre>
     *
     * @param method The method builder to add the throw statement to
     * @param field The field being accessed
     */
    public static void addFieldNotInVersionError(MethodSpec.Builder method, MergedField field) {
        String fieldName = field.getJavaName();
        // getPresentInVersions() returns version strings like "v202", "v203"
        String versionsStr = String.join(", ", field.getPresentInVersions());

        method.addStatement("throw new $T($S + getVersion() + $S)",
                UnsupportedOperationException.class,
                "Field '" + fieldName + "' is not available in protocol version ",
                ". This field exists only in versions: [" + versionsStr + "]");
    }

    /**
     * Add version-conditional code for setter operations.
     * If presentInVersion is true, executes the ifPresent consumer.
     * Otherwise, throws UnsupportedOperationException with clear message.
     *
     * <p>Use this for setter operations (doSet, doAdd, doAddAll) where silently
     * ignoring the value would be confusing to the user.</p>
     *
     * @param method          The method builder to add code to
     * @param presentInVersion Whether the field is present in the current version
     * @param field           The field being set (for error message)
     * @param ifPresent       Consumer that adds code when field is present
     */
    public static void addVersionConditionalOrThrow(MethodSpec.Builder method,
                                                     boolean presentInVersion,
                                                     MergedField field,
                                                     Consumer<MethodSpec.Builder> ifPresent) {
        if (presentInVersion) {
            ifPresent.accept(method);
        } else {
            addFieldNotInVersionError(method, field);
        }
    }
}
