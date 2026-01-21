package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.generator.TypeResolver;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedSchema;
import io.alnovis.protowrapper.model.VersionFieldSnapshot;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Helper class containing code generation utilities shared by conflict handlers.
 *
 * <p>This class centralizes common code generation patterns to avoid duplication
 * across different handlers. All methods are static utilities.</p>
 *
 * <h2>Method Categories</h2>
 *
 * <h3>Type Resolution</h3>
 * <ul>
 *   <li>{@link #getVersionSpecificJavaName} - Get field name for current version</li>
 *   <li>{@link #getWrapperClassName} - Get wrapper class for message fields</li>
 *   <li>{@link #getEnumTypeForFromProtoValue} - Get enum type for conversion</li>
 *   <li>{@link #getProtoTypeForField} - Get proto message type</li>
 *   <li>{@link #getProtoEnumTypeForField} - Get proto enum type</li>
 *   <li>{@link #getMessageTypeForField} - Get JavaPoet TypeName for message</li>
 * </ul>
 *
 * <h3>Code Generation</h3>
 * <ul>
 *   <li>{@link #generateProtoGetterCall} - Generate proto.getXxx() expression</li>
 *   <li>{@link #generateProtoSetterCall} - Generate protoBuilder.setXxx() expression</li>
 *   <li>{@link #addEnumSetterWithValidation} - Add enum setter with null check</li>
 * </ul>
 *
 * <h3>Version-Conditional Helpers</h3>
 * <ul>
 *   <li>{@link #addVersionConditional} - Add code only if field present</li>
 *   <li>{@link #addVersionConditionalOrThrow} - Add code or throw if not present</li>
 *   <li>{@link #addVersionConditionalClear} - Add protoBuilder.clearXxx()</li>
 *   <li>{@link #addFieldNotInVersionError} - Generate UnsupportedOperationException</li>
 * </ul>
 *
 * <h3>Field Type Dispatch</h3>
 * <ul>
 *   <li>{@link FieldTypeDispatcher} - Dispatch based on message/enum/bytes/primitive</li>
 *   <li>{@link #ADD_SINGLE_DISPATCHER} - Dispatcher for protoBuilder.addXxx()</li>
 *   <li>{@link #ADD_ALL_DISPATCHER} - Dispatcher for addAll operations</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In a ConflictHandler implementation:
 * String getterCall = CodeGenerationHelper.generateProtoGetterCall(field, ctx);
 * method.addStatement("return $L", getterCall);
 *
 * // Version-conditional setter:
 * CodeGenerationHelper.addVersionConditionalOrThrow(method, presentInVersion, field,
 *     m -> m.addStatement("protoBuilder.set$L($L)", javaName, paramName));
 * }</pre>
 *
 * @see ConflictHandler
 * @see AbstractConflictHandler
 * @see ProcessingContext
 */
public final class CodeGenerationHelper {

    private CodeGenerationHelper() {
        // Utility class
    }

    /**
     * Get the version-specific Java name for a field.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the capitalized Java name for this version
     */
    public static String getVersionSpecificJavaName(MergedField field, ProcessingContext ctx) {
        String version = ctx.version();
        if (version == null) {
            return ctx.capitalize(field.getJavaName());
        }
        VersionFieldSnapshot snapshot = VersionFieldSnapshot.of(field, version);
        return ctx.capitalize(snapshot.javaNameOr(field.getJavaName()));
    }

    /**
     * Get the wrapper class name for a message field.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the fully qualified wrapper class name
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
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the enum type name for conversion
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
     *
     * @param typeName the potentially qualified type name
     * @return the simple type name without package
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
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the proto getter call expression
     */
    public static String generateProtoGetterCall(MergedField field, ProcessingContext ctx) {
        String javaName = getVersionSpecificJavaName(field, ctx);
        String enumType = getEnumTypeForFromProtoValue(field, ctx);

        VersionFieldSnapshot snapshot = ctx.versionSnapshot(field);
        boolean versionIsEnum = snapshot.isEnum();
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
            String versionType = snapshot.javaTypeOr(mergedType);

            if (("Long".equals(mergedType) || "long".equals(mergedType)) && "int".equals(versionType)) {
                return String.format("(long) proto.get%s()", javaName);
            }

            return String.format("proto.get%s()", javaName);
        }
    }

    /**
     * Get default value for a type.
     *
     * @param javaType the Java type name
     * @param resolver the type resolver
     * @return the default value expression
     */
    public static String getDefaultValue(String javaType, TypeResolver resolver) {
        return resolver.getDefaultValue(javaType);
    }

    /**
     * Get the proto type for a message field.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @param currentProtoClassName the current proto class name for context
     * @return the fully qualified proto type name
     */
    public static String getProtoTypeForField(MergedField field, ProcessingContext ctx,
                                               String currentProtoClassName) {
        VersionFieldSnapshot snapshot = ctx.versionSnapshot(field);
        if (snapshot.isAbsent()) {
            return "com.google.protobuf.Message";
        }

        String version = ctx.requireVersion();
        GeneratorConfig config = ctx.config();
        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.resolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        String path = snapshot.extractNestedTypePath(protoPackage);

        MergedSchema schema = ctx.schema();
        String outerClassName = findOuterClassForType(path, schema, version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + path;
        }

        if (currentProtoClassName != null && currentProtoClassName.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProtoClassName.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + path;
            }
        }

        return javaProtoPackage + "." + path;
    }

    /**
     * Find the outer class name for a message type.
     *
     * @param messagePath the message path
     * @param schema the merged schema
     * @param version the version string
     * @return the outer class name, or null if not found
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
     *
     * @param field the merged field
     * @param ctx the processing context
     * @param currentProtoClassName the current proto class name for context
     * @return the fully qualified proto enum type name
     */
    public static String getProtoEnumTypeForField(MergedField field, ProcessingContext ctx,
                                                    String currentProtoClassName) {
        VersionFieldSnapshot snapshot = ctx.versionSnapshot(field);
        if (snapshot.isAbsent()) {
            return "Object";
        }

        String version = ctx.requireVersion();
        GeneratorConfig config = ctx.config();
        String javaProtoPackage = config.getProtoPackage(version);
        TypeResolver resolver = ctx.resolver();
        String protoPackage = resolver.extractProtoPackage(config.getProtoPackagePattern());

        String path = snapshot.extractNestedTypePath(protoPackage);

        MergedSchema schema = ctx.schema();
        String outerClassName = findOuterClassForEnum(path, schema, version);

        if (outerClassName != null) {
            return javaProtoPackage + "." + outerClassName + "." + path;
        }

        if (currentProtoClassName != null && currentProtoClassName.startsWith(javaProtoPackage + ".")) {
            String afterPackage = currentProtoClassName.substring(javaProtoPackage.length() + 1);
            int dotIdx = afterPackage.indexOf('.');
            if (dotIdx > 0) {
                String currentOuterClass = afterPackage.substring(0, dotIdx);
                return javaProtoPackage + "." + currentOuterClass + "." + path;
            }
        }

        return javaProtoPackage + "." + path;
    }

    /**
     * Find the outer class name for an enum type.
     *
     * @param enumPath the enum path
     * @param schema the merged schema
     * @param version the version string
     * @return the outer class name, or null if not found
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
     *
     * @param config the generator configuration
     * @return "valueOf" for proto2, "forNumber" for proto3
     * @deprecated Since 2.2.0. Use {@link #getEnumFromIntMethod(ProcessingContext)} for per-version syntax support.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public static String getEnumFromIntMethod(GeneratorConfig config) {
        return config.getDefaultSyntax().isProto2() ? "valueOf" : "forNumber";
    }

    /**
     * Get the enum conversion method name based on version-specific proto syntax.
     *
     * <p>This method uses per-version syntax from MergedSchema when available,
     * allowing different versions to use different enum conversion methods:</p>
     * <ul>
     *   <li>proto2: uses {@code valueOf(int)} which returns null for unknown values</li>
     *   <li>proto3: uses {@code forNumber(int)} which also returns null for unknown values</li>
     * </ul>
     *
     * @param ctx the processing context (must have version set for per-version support)
     * @return "valueOf" for proto2, "forNumber" for proto3
     * @since 2.2.0
     */
    public static String getEnumFromIntMethod(ProcessingContext ctx) {
        String version = ctx.version();
        MergedSchema schema = ctx.schema();
        if (version != null && schema != null && schema.hasVersionSyntax(version)) {
            return schema.getVersionSyntax(version).isProto2() ? "valueOf" : "forNumber";
        }
        // Fallback to config default (inlined to avoid calling deprecated method)
        return ctx.config().getDefaultSyntax().isProto2() ? "valueOf" : "forNumber";
    }

    /**
     * Get message type for PRIMITIVE_MESSAGE conflict field.
     * Handles nested types correctly (e.g., TicketRequest.ParentTicket).
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the message TypeName, or null if not found
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
                        return ClassName.get(ctx.apiPackage(), parts[0], nestedParts);
                    } else {
                        // Top-level type
                        return ClassName.get(ctx.apiPackage(), fullTypePath);
                    }
                })
                .orElse(null);
    }

    /**
     * Get the message wrapper class name for a PRIMITIVE_MESSAGE conflict field.
     *
     * @param field the merged field
     * @param ctx the processing context
     * @return the wrapper class name, or null if not found
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
        /**
         * Apply the action.
         *
         * @param method the method builder
         * @param field the merged field
         * @param versionJavaName the version-specific Java name
         * @param ctx the processing context
         */
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
            FieldTypeAction bytesAction,
            FieldTypeAction primitiveAction
    ) {
        /**
         * Constructor for backward compatibility (without bytesAction).
         *
         * @param messageAction action for message fields
         * @param enumAction action for enum fields
         * @param primitiveAction action for primitive fields
         */
        public FieldTypeDispatcher(FieldTypeAction messageAction, FieldTypeAction enumAction,
                                    FieldTypeAction primitiveAction) {
            this(messageAction, enumAction, null, primitiveAction);
        }

        /**
         * Dispatch to the appropriate action based on field type.
         *
         * @param method the method builder
         * @param field the merged field
         * @param versionJavaName the Java name for this version
         * @param ctx the processing context
         */
        public void dispatch(MethodSpec.Builder method, MergedField field,
                             String versionJavaName, ProcessingContext ctx) {
            if (field.isMessage()) {
                messageAction.apply(method, field, versionJavaName, ctx);
            } else if (field.isEnum()) {
                enumAction.apply(method, field, versionJavaName, ctx);
            } else if (bytesAction != null && isBytesType(field)) {
                bytesAction.apply(method, field, versionJavaName, ctx);
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
                String enumMethod = getEnumFromIntMethod(c);
                m.addStatement("protoBuilder.add$L($L.$L($L.getValue()))",
                        n, protoEnumType, enumMethod, f.getJavaName());
            },
            // Bytes - convert byte[] to ByteString
            (m, f, n, c) -> m.addStatement("protoBuilder.add$L($T.copyFrom($L))",
                    n, ClassName.get("com.google.protobuf", "ByteString"), f.getJavaName()),
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
                String enumMethod = getEnumFromIntMethod(c);
                m.addStatement("$L.forEach(e -> protoBuilder.add$L($L.$L(e.getValue())))",
                        f.getJavaName(), n, protoEnumType, enumMethod);
            },
            // Bytes - convert each byte[] to ByteString
            (m, f, n, c) -> m.addStatement("$L.forEach(e -> protoBuilder.add$L($T.copyFrom(e)))",
                    f.getJavaName(), n, ClassName.get("com.google.protobuf", "ByteString")),
            // Primitive
            (m, f, n, c) -> m.addStatement("protoBuilder.addAll$L($L)", n, f.getJavaName())
    );

    /**
     * Generate a proto setter call string for single (non-repeated) fields.
     * Returns a format string with $L placeholder for the field value.
     *
     * <p>Note: For enum fields, this generates a simple setter without null validation.
     * Use {@link #addEnumSetterWithValidation} instead for proper error messages.</p>
     *
     * @param field the merged field
     * @param versionJavaName the Java name for this version
     * @param ctx the processing context
     * @return the setter call format string
     */
    public static String generateProtoSetterCall(MergedField field, String versionJavaName, ProcessingContext ctx) {
        if (field.isMessage()) {
            String protoTypeName = getProtoTypeForField(field, ctx, null);
            return "protoBuilder.set" + versionJavaName + "((" + protoTypeName + ") extractProto($L))";
        } else if (field.isEnum()) {
            String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
            String enumMethod = getEnumFromIntMethod(ctx);
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
     * Handles both scalar "byte[]" and repeated "List&lt;byte[]&gt;".
     *
     * @param field the merged field
     * @return true if the field is a bytes type
     */
    public static boolean isBytesType(MergedField field) {
        String getterType = field.getGetterType();
        return "byte[]".equals(getterType) || getterType.contains("byte[]");
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
        String enumMethod = getEnumFromIntMethod(ctx);
        String localVarName = "protoEnumValue";
        String enumTypeName = field.getGetterType();

        // ProtoEnumType protoEnumValue = ProtoEnumType.valueOf(param.getValue());
        method.addStatement("$L $L = $L.$L($L.getValue())",
                protoEnumType, localVarName, protoEnumType, enumMethod, fieldParamName);

        // if (protoEnumValue == null) { throw ... }
        method.beginControlFlow("if ($L == null)", localVarName);
        method.addStatement("throw new $T($S + $L.name() + $S + $L.getValue() + $S + getVersionId())",
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
     *
     * @param field the merged field
     * @return true if the field is an enum
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
     *     "Field 'exciseStamp' is not available in protocol version 3. " +
     *     "This field exists only in versions: [v2]");
     * </pre>
     *
     * @param method The method builder to add the throw statement to
     * @param field The field being accessed
     */
    public static void addFieldNotInVersionError(MethodSpec.Builder method, MergedField field) {
        String fieldName = field.getJavaName();
        // getPresentInVersions() returns version strings like "v2", "v3"
        String versionsStr = String.join(", ", field.getPresentInVersions());

        method.addStatement("throw new $T($S + getVersionId() + $S)",
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
