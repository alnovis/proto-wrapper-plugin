package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.TypeNormalizer;
import io.alnovis.protowrapper.generator.TypeUtils;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.nio.charset.StandardCharsets;

import static io.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for repeated fields with type conflicts.
 *
 * <p>This handler processes repeated (list) fields that have type conflicts
 * between versions. It handles element-wise conversion when extracting values.</p>
 *
 * <h2>Supported Conflict Types</h2>
 * <ul>
 *   <li>{@code WIDENING} - repeated int32 vs repeated int64</li>
 *   <li>{@code FLOAT_DOUBLE} - repeated float vs repeated double</li>
 *   <li>{@code SIGNED_UNSIGNED} - repeated int32 vs repeated uint32, etc.</li>
 *   <li>{@code INT_ENUM} - repeated int32 vs repeated SomeEnum</li>
 *   <li>{@code STRING_BYTES} - repeated string vs repeated bytes</li>
 * </ul>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Order { repeated int32 tags = 1; }
 * // v2: message Order { repeated int64 tags = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The handler uses stream operations for element-wise conversion:</p>
 * <ul>
 *   <li>WIDENING: {@code list.stream().map(Integer::longValue).toList()}</li>
 *   <li>FLOAT_DOUBLE: {@code list.stream().map(Float::doubleValue).toList()}</li>
 *   <li>SIGNED_UNSIGNED: {@code list.stream().map(Integer::toUnsignedLong).toList()}</li>
 *   <li>INT_ENUM: {@code list.stream().map(Enum::getNumber).toList()}</li>
 *   <li>STRING_BYTES: {@code list.stream().map(b -> b.toString(UTF_8)).toList()}</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code List<Long> getTags()}</li>
 *   <li><b>Abstract:</b> {@code extractTags(proto)}</li>
 *   <li><b>Impl:</b> Version-specific extraction with stream conversion</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder methods are <b>not generated</b> for repeated fields with type conflicts.
 * The complexity of handling element-wise conversion in reverse makes it impractical.
 * Application code should use version-specific builders for such fields.</p>
 *
 * <h2>Performance Note</h2>
 * <p>Each getter call creates a new list via stream operations. For performance-critical
 * code accessing the same field multiple times, cache the result in a local variable.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see WideningHandler
 * @see IntEnumHandler
 * @see StringBytesHandler
 */
public final class RepeatedConflictHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final RepeatedConflictHandler INSTANCE = new RepeatedConflictHandler();

    /** Private constructor for singleton. */
    private RepeatedConflictHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.REPEATED_CONFLICT;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return field.isRepeated() && field.hasTypeConflict();
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add main extract method (returns List<T> where T is the unified type)
        addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Get unified element type based on conflict type
        TypeName elementType = TypeUtils.getRepeatedConflictElementType(field.getConflictType());
        TypeName listType = com.squareup.javapoet.ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), elementType);

        // Use template method for repeated abstract builder methods
        addAbstractRepeatedBuilderMethods(builder, field, elementType, listType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, ctx);
            return;
        }

        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName returnType = ctx.parseFieldType(field);
        MergedField.ConflictType conflictType = field.getConflictType();

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto");

        switch (conflictType) {
            case WIDENING -> addWideningExtraction(extract, field, versionField, versionJavaName);
            case FLOAT_DOUBLE -> addFloatDoubleExtraction(extract, versionField, versionJavaName);
            case SIGNED_UNSIGNED -> addSignedUnsignedExtraction(extract, versionField, versionJavaName);
            case INT_ENUM -> addIntEnumExtraction(extract, versionField, versionJavaName);
            case STRING_BYTES -> addStringBytesExtraction(extract, versionField, versionJavaName);
            default -> extract.addStatement("return proto.get$LList()", versionJavaName);
        }

        builder.addMethod(extract.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add standard getter (returns List<T>)
        MethodSpec getter = MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType)
                .addStatement("return $L(proto)", field.getExtractMethodName())
                .build();
        builder.addMethod(getter);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        MergedField.ConflictType conflictType = field.getConflictType();

        FieldInfo versionField = field.getVersionFields().get(version);

        // Get unified element type based on conflict type
        TypeName elementType = TypeUtils.getRepeatedConflictElementType(conflictType);
        TypeName listType = com.squareup.javapoet.ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), elementType);

        // doAdd - add single element
        buildDoSetImpl(builder, "doAdd" + capitalizedName, elementType, field.getJavaName(),
                presentInVersion, m -> addDoAddStatement(m, field, conflictType, versionField, versionJavaName, ctx));

        // doAddAll - add list of elements
        buildDoSetImpl(builder, "doAddAll" + capitalizedName, listType, field.getJavaName(),
                presentInVersion, m -> addDoAddAllStatement(m, field, conflictType, versionField, versionJavaName, ctx));

        // doSet - clear and add all
        buildDoSetImpl(builder, "doSet" + capitalizedName, listType, field.getJavaName(),
                presentInVersion, m -> {
                    m.addStatement("protoBuilder.clear$L()", versionJavaName);
                    addDoAddAllStatement(m, field, conflictType, versionField, versionJavaName, ctx);
                });

        // doClear - clear the list
        buildDoClearImpl(builder, "doClear" + capitalizedName, presentInVersion, versionJavaName);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // Get unified element type based on conflict type
        TypeName elementType = TypeUtils.getRepeatedConflictElementType(field.getConflictType());
        TypeName listType = com.squareup.javapoet.ParameterizedTypeName.get(
                ClassName.get(java.util.List.class), elementType);

        // Use template method for repeated concrete builder methods
        addRepeatedConcreteBuilderMethods(builder, field, elementType, listType, builderReturnType, ctx);
    }

    private void addWideningExtraction(MethodSpec.Builder extract, MergedField field,
                                        FieldInfo versionField, String versionJavaName) {
        String versionType = versionField != null ? versionField.getJavaType() : null;
        String targetType = field.getGetterType();

        String targetElementType = TypeNormalizer.extractListElementType(targetType);
        String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;

        if (versionElementType != null && needsWideningConversion(versionElementType, targetElementType)) {
            String wideningMethod = TypeNormalizer.getWideningMethodName(targetElementType);
            extract.addStatement("return proto.get$LList().stream().map(e -> e.$L()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, wideningMethod);
        } else {
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private void addFloatDoubleExtraction(MethodSpec.Builder extract, FieldInfo versionField,
                                           String versionJavaName) {
        // For float -> double conversion, use doubleValue()
        String versionType = versionField != null ? versionField.getJavaType() : null;
        String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;

        if (TypeNormalizer.isFloatType(versionElementType)) {
            // Version has List<Float>, unified type is List<Double> - need conversion
            extract.addStatement("return proto.get$LList().stream().map(e -> e.doubleValue()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
        } else {
            // Version already has List<Double> - no conversion needed
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private void addSignedUnsignedExtraction(MethodSpec.Builder extract, FieldInfo versionField,
                                              String versionJavaName) {
        // For signed/unsigned conversion, unified type is List<Long>
        // Need to handle unsigned 32-bit values properly
        if (versionField == null) {
            extract.addStatement("return proto.get$LList()", versionJavaName);
            return;
        }

        com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type type = versionField.getType();
        boolean isUnsigned32 = type == com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 ||
                               type == com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32;

        if (isUnsigned32) {
            // uint32/fixed32: values > Integer.MAX_VALUE appear as negative
            // Use Integer.toUnsignedLong() for proper conversion
            extract.addStatement("return proto.get$LList().stream().map(e -> $T.toUnsignedLong(e)).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, Integer.class);
        } else {
            // Signed 32-bit or any 64-bit: simple widening to long
            extract.addStatement("return proto.get$LList().stream().map(e -> e.longValue()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
        }
    }

    private void addIntEnumExtraction(MethodSpec.Builder extract, FieldInfo versionField, String versionJavaName) {
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        if (versionIsEnum) {
            extract.addStatement("return proto.get$LList().stream().map(e -> e.getNumber()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
        } else {
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private void addStringBytesExtraction(MethodSpec.Builder extract, FieldInfo versionField, String versionJavaName) {
        String versionType = versionField != null ? versionField.getJavaType() : "String";
        boolean versionIsBytes = versionType.contains("byte[]") || versionType.contains("ByteString");
        if (versionIsBytes) {
            extract.addStatement("return proto.get$LList().stream().map(b -> b.toString($T.UTF_8)).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, StandardCharsets.class);
        } else {
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private boolean needsWideningConversion(String fromType, String toType) {
        if (TypeNormalizer.areEquivalent(fromType, toType)) {
            return false;
        }
        return TypeNormalizer.isWideningConversion(fromType, toType);
    }

    private void addDoAddStatement(MethodSpec.Builder method, MergedField field,
                                    MergedField.ConflictType conflictType, FieldInfo versionField,
                                    String versionJavaName, ProcessingContext ctx) {
        switch (conflictType) {
            case WIDENING -> {
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String targetType = field.getGetterType();
                String targetElementType = TypeNormalizer.extractListElementType(targetType);
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;

                if (versionElementType != null && needsNarrowingForBuilder(targetElementType, versionElementType)) {
                    String version = ctx.requireVersion();
                    // Add range validation for narrowing long -> int
                    if (isNarrowingLongToInt(targetElementType, versionElementType)) {
                        method.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                                field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
                        method.addStatement("throw new $T($S + $L + $S)",
                                IllegalArgumentException.class,
                                "Value ", field.getJavaName(), " exceeds int32 range for " + version);
                        method.endControlFlow();
                    }
                    String castType = versionElementType.toLowerCase();
                    // Use (type) (primitive) to handle boxing correctly
                    String primitiveUnbox = TypeUtils.getPrimitiveUnboxCast(targetElementType);
                    method.addStatement("protoBuilder.add$L(($L) $L $L)", versionJavaName, castType, primitiveUnbox, field.getJavaName());
                } else {
                    method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case INT_ENUM -> {
                boolean versionIsEnum = versionField != null && versionField.isEnum();
                if (versionIsEnum) {
                    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                    String enumMethod = getEnumFromIntMethod(ctx.config());
                    method.addStatement("protoBuilder.add$L($L.$L($L))",
                            versionJavaName, protoEnumType, enumMethod, field.getJavaName());
                } else {
                    method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case STRING_BYTES -> {
                String versionType = versionField != null ? versionField.getJavaType() : "String";
                boolean versionIsBytes = versionType.contains("byte[]") || versionType.contains("ByteString");
                if (versionIsBytes) {
                    method.addStatement("protoBuilder.add$L($T.copyFromUtf8($L))",
                            versionJavaName, ClassName.get("com.google.protobuf", "ByteString"), field.getJavaName());
                } else {
                    method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case SIGNED_UNSIGNED -> {
                // Unified type is Long. Version type may be int or long depending on the proto type.
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;
                boolean versionIsInt = versionElementType != null && TypeUtils.isIntType(versionElementType);
                if (versionIsInt) {
                    // Need to narrow Long -> int with validation
                    // Check if version field is unsigned (uint32) - requires value >= 0
                    // versionField is guaranteed non-null here since versionIsInt depends on it
                    boolean isUnsigned = versionField.getType() != null &&
                            versionField.getType().name().contains("UINT");
                    String version = ctx.requireVersion();
                    if (isUnsigned) {
                        // uint32: 0 to 4294967295
                        method.beginControlFlow("if ($L < 0 || $L > 0xFFFFFFFFL)",
                                field.getJavaName(), field.getJavaName());
                    } else {
                        // int32: -2147483648 to 2147483647
                        method.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                                field.getJavaName(), Integer.class, field.getJavaName(), Integer.class);
                    }
                    method.addStatement("throw new $T($S + $L + $S)",
                            IllegalArgumentException.class,
                            "Value ", field.getJavaName(), " exceeds " + (isUnsigned ? "uint32" : "int32") + " range for " + version);
                    method.endControlFlow();
                    method.addStatement("protoBuilder.add$L((int) (long) $L)", versionJavaName, field.getJavaName());
                } else {
                    method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case FLOAT_DOUBLE -> {
                // Unified type is Double. Version type may be float or double.
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;
                boolean versionIsFloat = versionElementType != null && TypeUtils.isFloatType(versionElementType);
                if (versionIsFloat) {
                    // Need to narrow Double -> float with validation
                    String version = ctx.requireVersion();
                    method.beginControlFlow("if ($T.isFinite($L) && ($L < -$T.MAX_VALUE || $L > $T.MAX_VALUE))",
                            Double.class, field.getJavaName(),
                            field.getJavaName(), Float.class, field.getJavaName(), Float.class);
                    method.addStatement("throw new $T($S + $L + $S)",
                            IllegalArgumentException.class,
                            "Value ", field.getJavaName(), " exceeds float range for " + version);
                    method.endControlFlow();
                    method.addStatement("protoBuilder.add$L((float) (double) $L)", versionJavaName, field.getJavaName());
                } else {
                    method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
                }
            }
            default -> method.addStatement("protoBuilder.add$L($L)", versionJavaName, field.getJavaName());
        }
    }

    private void addDoAddAllStatement(MethodSpec.Builder method, MergedField field,
                                       MergedField.ConflictType conflictType, FieldInfo versionField,
                                       String versionJavaName, ProcessingContext ctx) {
        switch (conflictType) {
            case WIDENING -> {
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String targetType = field.getGetterType();
                String targetElementType = TypeNormalizer.extractListElementType(targetType);
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;

                if (versionElementType != null && needsNarrowingForBuilder(targetElementType, versionElementType)) {
                    String version = ctx.requireVersion();
                    String castType = versionElementType.toLowerCase();
                    // Add range validation for narrowing long -> int in addAll
                    if (isNarrowingLongToInt(targetElementType, versionElementType)) {
                        method.addStatement("$L.forEach(e -> { " +
                                "if (e < $T.MIN_VALUE || e > $T.MAX_VALUE) " +
                                "throw new $T($S + e + $S); " +
                                "protoBuilder.add$L(($L) (long) e); })",
                                field.getJavaName(), Integer.class, Integer.class,
                                IllegalArgumentException.class,
                                "Value ", " exceeds int32 range for " + version,
                                versionJavaName, castType);
                    } else {
                        method.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) e))",
                                field.getJavaName(), versionJavaName, castType);
                    }
                } else {
                    method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case INT_ENUM -> {
                boolean versionIsEnum = versionField != null && versionField.isEnum();
                if (versionIsEnum) {
                    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                    String enumMethod = getEnumFromIntMethod(ctx.config());
                    method.addStatement("$L.forEach(e -> protoBuilder.add$L($L.$L(e)))",
                            field.getJavaName(), versionJavaName, protoEnumType, enumMethod);
                } else {
                    method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case STRING_BYTES -> {
                String versionType = versionField != null ? versionField.getJavaType() : "String";
                boolean versionIsBytes = versionType.contains("byte[]") || versionType.contains("ByteString");
                if (versionIsBytes) {
                    method.addStatement("$L.forEach(e -> protoBuilder.add$L($T.copyFromUtf8(e)))",
                            field.getJavaName(), versionJavaName, ClassName.get("com.google.protobuf", "ByteString"));
                } else {
                    method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case SIGNED_UNSIGNED -> {
                // Unified type is Long. Version type may be int or long depending on the proto type.
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;
                boolean versionIsInt = versionElementType != null && TypeUtils.isIntType(versionElementType);
                if (versionIsInt) {
                    // versionField is guaranteed non-null here since versionIsInt depends on it
                    boolean isUnsigned = versionField.getType() != null &&
                            versionField.getType().name().contains("UINT");
                    String version = ctx.requireVersion();
                    if (isUnsigned) {
                        method.addStatement("$L.forEach(e -> { " +
                                "if (e < 0 || e > 0xFFFFFFFFL) " +
                                "throw new $T($S + e + $S); " +
                                "protoBuilder.add$L((int) (long) e); })",
                                field.getJavaName(),
                                IllegalArgumentException.class,
                                "Value ", " exceeds uint32 range for " + version,
                                versionJavaName);
                    } else {
                        method.addStatement("$L.forEach(e -> { " +
                                "if (e < $T.MIN_VALUE || e > $T.MAX_VALUE) " +
                                "throw new $T($S + e + $S); " +
                                "protoBuilder.add$L((int) (long) e); })",
                                field.getJavaName(), Integer.class, Integer.class,
                                IllegalArgumentException.class,
                                "Value ", " exceeds int32 range for " + version,
                                versionJavaName);
                    }
                } else {
                    method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                }
            }
            case FLOAT_DOUBLE -> {
                // Unified type is Double. Version type may be float or double.
                String versionType = versionField != null ? versionField.getJavaType() : null;
                String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;
                boolean versionIsFloat = versionElementType != null && TypeUtils.isFloatType(versionElementType);
                if (versionIsFloat) {
                    String version = ctx.requireVersion();
                    method.addStatement("$L.forEach(e -> { " +
                            "if ($T.isFinite(e) && (e < -$T.MAX_VALUE || e > $T.MAX_VALUE)) " +
                            "throw new $T($S + e + $S); " +
                            "protoBuilder.add$L((float) (double) e); })",
                            field.getJavaName(), Double.class, Float.class, Float.class,
                            IllegalArgumentException.class,
                            "Value ", " exceeds float range for " + version,
                            versionJavaName);
                } else {
                    method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
                }
            }
            default -> method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
        }
    }

    private boolean needsNarrowingForBuilder(String widerType, String narrowerType) {
        // When building, we may need to narrow from long back to int
        return TypeNormalizer.isWideningConversion(narrowerType, widerType);
    }

    /**
     * Check if this is a long to int narrowing that needs range validation.
     */
    private boolean isNarrowingLongToInt(String targetElementType, String versionElementType) {
        return TypeUtils.isLongType(targetElementType) && TypeUtils.isIntType(versionElementType);
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add missing extract method
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return java.util.Collections.emptyList()")
                .build());
    }
}
