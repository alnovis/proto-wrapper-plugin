package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.TypeNormalizer;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.nio.charset.StandardCharsets;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;
import static space.alnovis.protowrapper.generator.TypeUtils.*;

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

    public static final RepeatedConflictHandler INSTANCE = new RepeatedConflictHandler();

    private RepeatedConflictHandler() {
        // Singleton
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
        // Repeated fields with type conflicts don't have builder methods in the interface
        // (InterfaceGenerator skips them with "type conflict" note)
        // And ImplClassGenerator also skips them in generateBuilderImplClass
        // So we don't generate abstract builder methods either to maintain consistency
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
        // Repeated fields with type conflicts don't have builder methods
        // ImplClassGenerator skips them in generateBuilderImplClass (line 400-403)
        // So we don't generate impl methods either to maintain consistency
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // Repeated fields with type conflicts don't have builder methods in the interface
        // (InterfaceGenerator skips them with "type conflict" note)
        // So we don't generate concrete implementations either
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
                    String castType = versionElementType.toLowerCase();
                    method.addStatement("protoBuilder.add$L(($L) $L)", versionJavaName, castType, field.getJavaName());
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
                    String castType = versionElementType.toLowerCase();
                    method.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) e))",
                            field.getJavaName(), versionJavaName, castType);
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
            default -> method.addStatement("protoBuilder.addAll$L($L)", versionJavaName, field.getJavaName());
        }
    }

    private boolean needsNarrowingForBuilder(String widerType, String narrowerType) {
        // When building, we may need to narrow from long back to int
        return TypeNormalizer.isWideningConversion(narrowerType, widerType);
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
