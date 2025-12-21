package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.ConflictEnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.util.Optional;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for INT_ENUM conflict fields.
 *
 * <p>This handler processes fields where one version uses an int type and another
 * uses an enum type. It generates both int and enum accessor methods.</p>
 */
public final class IntEnumHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final IntEnumHandler INSTANCE = new IntEnumHandler();

    private IntEnumHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method (returns int)
        addAbstractExtractMethod(builder, field, returnType, ctx);

        // Add enum extract method
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
            String enumExtractMethodName = field.getExtractEnumMethodName();

            builder.addMethod(MethodSpec.methodBuilder(enumExtractMethodName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(enumType)
                    .addParameter(ctx.protoType(), "proto")
                    .build());
        }
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
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName intReturnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Main extract - returns int
        MethodSpec.Builder extractInt = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(intReturnType)
                .addParameter(ctx.protoClassName(), "proto");

        if (versionIsEnum) {
            extractInt.addStatement("return proto.get$L().getNumber()", versionJavaName);
        } else {
            extractInt.addStatement("return proto.get$L()", versionJavaName);
        }
        builder.addMethod(extractInt.build());

        // Enum extract - returns unified enum
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
            String enumExtractMethodName = field.getExtractEnumMethodName();

            MethodSpec.Builder extractEnum = MethodSpec.methodBuilder(enumExtractMethodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(enumType)
                    .addParameter(ctx.protoClassName(), "proto");

            if (versionIsEnum) {
                extractEnum.addStatement("return $T.fromProtoValue(proto.get$L().getNumber())",
                        enumType, versionJavaName);
            } else {
                extractEnum.addStatement("return $T.fromProtoValue(proto.get$L())",
                        enumType, versionJavaName);
            }
            builder.addMethod(extractEnum.build());
        }
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add standard getter (returns int)
        addStandardGetterImpl(builder, field, returnType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);

        // Add enum getter
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
            String enumGetterName = "get" + ctx.capitalize(field.getJavaName()) + "Enum";
            String enumExtractMethodName = field.getExtractEnumMethodName();

            MethodSpec.Builder enumGetter = MethodSpec.methodBuilder(enumGetterName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(enumType);

            if (field.needsHasCheck()) {
                enumGetter.addStatement("return $L(proto) ? $L(proto) : null",
                        field.getExtractHasMethodName(), enumExtractMethodName);
            } else {
                enumGetter.addStatement("return $L(proto)", enumExtractMethodName);
            }

            builder.addMethod(enumGetter.build());
        }
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName intType = ctx.parseFieldType(field);

        // doSet (int)
        builder.addMethod(MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(intType, field.getJavaName())
                .build());

        // Add enum setter
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());

            builder.addMethod(MethodSpec.methodBuilder(field.getDoSetMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(enumType, field.getJavaName())
                    .build());
        }

        // doClear for optional
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getDoClearMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        boolean versionIsEnum = versionField != null && versionField.isEnum();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName intType = ctx.parseFieldType(field);

        // doSet (int)
        MethodSpec.Builder doSetInt = MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(intType, field.getJavaName());

        if (presentInVersion) {
            if (versionIsEnum) {
                String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                String enumMethod = getEnumFromIntMethod(ctx.config());
                doSetInt.addStatement("protoBuilder.set$L($L.$L($L))",
                        versionJavaName, protoEnumType, enumMethod, field.getJavaName());
            } else {
                doSetInt.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSetInt.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSetInt.build());

        // doSet (enum)
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());

            MethodSpec.Builder doSetEnum = MethodSpec.methodBuilder(field.getDoSetMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(enumType, field.getJavaName());

            if (presentInVersion) {
                if (versionIsEnum) {
                    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                    String enumMethod = getEnumFromIntMethod(ctx.config());
                    doSetEnum.addStatement("protoBuilder.set$L($L.$L($L.getValue()))",
                            versionJavaName, protoEnumType, enumMethod, field.getJavaName());
                } else {
                    doSetEnum.addStatement("protoBuilder.set$L($L.getValue())",
                            versionJavaName, field.getJavaName());
                }
            } else {
                doSetEnum.addComment("Field not present in this version - ignored");
            }
            builder.addMethod(doSetEnum.build());
        }

        // doClear for optional
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder(field.getDoClearMethodName())
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

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing int extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);
        builder.addMethod(extract.build());

        // Add missing enum extract method
        Optional<ConflictEnumInfo> enumInfoOpt = ctx.schema()
                .getConflictEnum(ctx.message().getName(), field.getName());
        if (enumInfoOpt.isPresent()) {
            ConflictEnumInfo enumInfo = enumInfoOpt.get();
            ClassName enumType = ClassName.get(ctx.apiPackage(), enumInfo.getEnumName());
            String enumExtractMethodName = field.getExtractEnumMethodName();

            builder.addMethod(MethodSpec.methodBuilder(enumExtractMethodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(enumType)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addJavadoc("Field not present in this version.\n")
                    .addStatement("return null")
                    .build());
        }
    }
}
