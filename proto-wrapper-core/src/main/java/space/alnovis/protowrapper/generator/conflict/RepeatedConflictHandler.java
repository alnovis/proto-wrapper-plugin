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

/**
 * Handler for repeated fields with type conflicts.
 *
 * <p>This handler processes repeated fields that have WIDENING, INT_ENUM, or STRING_BYTES
 * conflicts between versions. It handles the list conversion appropriately.</p>
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
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName listType = ctx.parseFieldType(field);
        TypeName singleElementType = extractListElementType(listType);

        // doAdd
        builder.addMethod(MethodSpec.methodBuilder("doAdd" + ctx.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(singleElementType, field.getJavaName())
                .build());

        // doAddAll
        builder.addMethod(MethodSpec.methodBuilder("doAddAll" + ctx.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .build());

        // doSet (replace all)
        builder.addMethod(MethodSpec.methodBuilder("doSet" + ctx.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(listType, field.getJavaName())
                .build());

        // doClear
        builder.addMethod(MethodSpec.methodBuilder("doClear" + ctx.capitalize(field.getJavaName()))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build());
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName listType = ctx.parseFieldType(field);
        TypeName singleElementType = extractListElementType(listType);
        String capitalizedName = ctx.capitalize(field.getJavaName());

        MergedField.ConflictType conflictType = field.getConflictType();
        String version = ctx.version();
        FieldInfo versionField = version != null ? field.getVersionFields().get(version) : null;

        // doAdd
        MethodSpec.Builder doAdd = MethodSpec.methodBuilder("doAdd" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(singleElementType, field.getJavaName());

        addVersionConditional(doAdd, presentInVersion, m ->
                addDoAddStatement(m, field, conflictType, versionField, versionJavaName, ctx));
        builder.addMethod(doAdd.build());

        // doAddAll
        MethodSpec.Builder doAddAll = MethodSpec.methodBuilder("doAddAll" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        addVersionConditional(doAddAll, presentInVersion, m ->
                addDoAddAllStatement(m, field, conflictType, versionField, versionJavaName, ctx));
        builder.addMethod(doAddAll.build());

        // doSet (replace all)
        MethodSpec.Builder doSet = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        addVersionConditional(doSet, presentInVersion, m -> {
            m.addStatement("protoBuilder.clear$L()", versionJavaName);
            addDoAddAllStatement(m, field, conflictType, versionField, versionJavaName, ctx);
        });
        builder.addMethod(doSet.build());

        // doClear
        MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        addVersionConditionalClear(doClear, presentInVersion, versionJavaName);
        builder.addMethod(doClear.build());
    }

    private void addWideningExtraction(MethodSpec.Builder extract, MergedField field,
                                        FieldInfo versionField, String versionJavaName) {
        String versionType = versionField != null ? versionField.getJavaType() : null;
        String targetType = field.getGetterType();

        String targetElementType = TypeNormalizer.extractListElementType(targetType);
        String versionElementType = versionType != null ? TypeNormalizer.extractListElementType(versionType) : null;

        if (versionElementType != null && needsWideningConversion(versionElementType, targetElementType)) {
            String wideningMethod = TypeNormalizer.getWideningMethodName(targetElementType);
            extract.addStatement("return proto.get$LList().stream().map(e -> e.$L()).toList()",
                    versionJavaName, wideningMethod);
        } else {
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private void addIntEnumExtraction(MethodSpec.Builder extract, FieldInfo versionField, String versionJavaName) {
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        if (versionIsEnum) {
            extract.addStatement("return proto.get$LList().stream().map(e -> e.getNumber()).toList()",
                    versionJavaName);
        } else {
            extract.addStatement("return proto.get$LList()", versionJavaName);
        }
    }

    private void addStringBytesExtraction(MethodSpec.Builder extract, FieldInfo versionField, String versionJavaName) {
        String versionType = versionField != null ? versionField.getJavaType() : "String";
        boolean versionIsBytes = versionType.contains("byte[]") || versionType.contains("ByteString");
        if (versionIsBytes) {
            extract.addStatement("return proto.get$LList().stream().map(b -> b.toString($T.UTF_8)).toList()",
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
