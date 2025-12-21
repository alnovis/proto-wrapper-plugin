package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Default handler for fields without type conflicts.
 *
 * <p>This handler processes standard fields that have the same type across all versions
 * or no special conflict handling required.</p>
 */
public final class DefaultHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final DefaultHandler INSTANCE = new DefaultHandler();

    private DefaultHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        // Default handler handles fields that don't match any specific conflict handler
        // It's always used as the fallback
        return true;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method
        addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, returnType, ctx);
            return;
        }

        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        // Add has method for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Add extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto");

        String getterCall = generateProtoGetterCall(field, ctx);
        extract.addStatement("return $L", getterCall);

        builder.addMethod(extract.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add standard getter
        addStandardGetterImpl(builder, field, returnType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Skip fields with type conflicts that should be skipped
        if (field.shouldSkipBuilderSetter()) {
            return;
        }

        TypeName fieldType = ctx.parseFieldType(field);

        if (field.isRepeated()) {
            TypeName singleElementType = extractListElementType(fieldType);

            // doAdd
            builder.addMethod(MethodSpec.methodBuilder("doAdd" + ctx.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(singleElementType, field.getJavaName())
                    .build());

            // doAddAll
            builder.addMethod(MethodSpec.methodBuilder("doAddAll" + ctx.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(fieldType, field.getJavaName())
                    .build());

            // doSet (replace all)
            builder.addMethod(MethodSpec.methodBuilder("doSet" + ctx.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(fieldType, field.getJavaName())
                    .build());

            // doClear
            builder.addMethod(MethodSpec.methodBuilder("doClear" + ctx.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        } else {
            // doSet
            builder.addMethod(MethodSpec.methodBuilder("doSet" + ctx.capitalize(field.getJavaName()))
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(fieldType, field.getJavaName())
                    .build());

            // doClear for optional fields
            if (field.isOptional()) {
                builder.addMethod(MethodSpec.methodBuilder("doClear" + ctx.capitalize(field.getJavaName()))
                        .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                        .build());
            }
        }
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        // Skip fields with type conflicts that should be skipped
        if (field.shouldSkipBuilderSetter()) {
            return;
        }

        TypeName fieldType = ctx.parseFieldType(field);
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        if (field.isRepeated()) {
            TypeName singleElementType = extractListElementType(fieldType);
            addRepeatedFieldBuilderMethods(builder, field, singleElementType, fieldType,
                    presentInVersion, capitalizedName, versionJavaName, ctx);
        } else {
            addSingleFieldBuilderMethods(builder, field, fieldType, presentInVersion,
                    capitalizedName, versionJavaName, ctx);
        }
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field,
                                                TypeName returnType, ProcessingContext ctx) {
        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);

        builder.addMethod(extract.build());
    }

    private void addSingleFieldBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                               TypeName fieldType, boolean presentInVersion,
                                               String capitalizedName, String versionJavaName,
                                               ProcessingContext ctx) {
        // doSet
        MethodSpec.Builder doSet = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(fieldType, field.getJavaName());

        if (presentInVersion) {
            String setterCall = generateProtoSetterCall(field, versionJavaName, ctx);
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
                                                 boolean presentInVersion,
                                                 String capitalizedName, String versionJavaName,
                                                 ProcessingContext ctx) {
        // doAdd
        MethodSpec.Builder doAdd = MethodSpec.methodBuilder("doAdd" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(singleElementType, field.getJavaName());

        if (presentInVersion) {
            if (field.isMessage()) {
                String protoTypeName = getProtoTypeForField(field, ctx, null);
                doAdd.addStatement("protoBuilder.add$L(($L) extractProto($L))",
                        versionJavaName, protoTypeName, field.getJavaName());
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                String enumMethod = getEnumFromIntMethod(ctx.config());
                doAdd.addStatement("protoBuilder.add$L($L.$L($L.getValue()))",
                        versionJavaName, protoEnumType, enumMethod, field.getJavaName());
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
                String protoTypeName = getProtoTypeForField(field, ctx, null);
                doAddAll.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) extractProto(e)))",
                        field.getJavaName(), versionJavaName, protoTypeName);
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                String enumMethod = getEnumFromIntMethod(ctx.config());
                doAddAll.addStatement("$L.forEach(e -> protoBuilder.add$L($L.$L(e.getValue())))",
                        field.getJavaName(), versionJavaName, protoEnumType, enumMethod);
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
                String protoTypeName = getProtoTypeForField(field, ctx, null);
                doSetAll.addStatement("$L.forEach(e -> protoBuilder.add$L(($L) extractProto(e)))",
                        field.getJavaName(), versionJavaName, protoTypeName);
            } else if (field.isEnum()) {
                String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                String enumMethod = getEnumFromIntMethod(ctx.config());
                doSetAll.addStatement("$L.forEach(e -> protoBuilder.add$L($L.$L(e.getValue())))",
                        field.getJavaName(), versionJavaName, protoEnumType, enumMethod);
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

    private String generateProtoSetterCall(MergedField field, String versionJavaName, ProcessingContext ctx) {
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
}
