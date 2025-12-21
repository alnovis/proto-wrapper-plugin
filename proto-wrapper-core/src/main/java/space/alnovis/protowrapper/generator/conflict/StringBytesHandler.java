package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;
import java.nio.charset.StandardCharsets;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for STRING_BYTES conflict fields.
 *
 * <p>This handler processes fields where one version uses String type and another
 * uses bytes/ByteString. It generates both String and byte[] accessor methods
 * with UTF-8 conversion.</p>
 */
public final class StringBytesHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final StringBytesHandler INSTANCE = new StringBytesHandler();

    private StringBytesHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method (returns String)
        addAbstractExtractMethod(builder, field, ClassName.get(String.class), ctx);

        // Add bytes extract method
        String bytesExtractMethodName = field.getExtractBytesMethodName();
        builder.addMethod(MethodSpec.methodBuilder(bytesExtractMethodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(ctx.protoType(), "proto")
                .build());
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
        String versionType = versionField != null ? versionField.getJavaType() : "String";
        boolean versionIsBytes = "byte[]".equals(versionType) || "ByteString".equals(versionType);
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Add extractHas for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Extract String value
        MethodSpec.Builder extractString = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ClassName.get(String.class))
                .addParameter(ctx.protoClassName(), "proto");

        if (versionIsBytes) {
            extractString.addStatement("return proto.get$L().toString($T.UTF_8)",
                    versionJavaName, StandardCharsets.class);
        } else {
            extractString.addStatement("return proto.get$L()", versionJavaName);
        }
        builder.addMethod(extractString.build());

        // Extract byte[] value
        String bytesExtractMethodName = field.getExtractBytesMethodName();
        MethodSpec.Builder extractBytes = MethodSpec.methodBuilder(bytesExtractMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(ctx.protoClassName(), "proto");

        if (versionIsBytes) {
            extractBytes.addStatement("return proto.get$L().toByteArray()", versionJavaName);
        } else {
            extractBytes.addStatement("return proto.get$L().getBytes($T.UTF_8)",
                    versionJavaName, StandardCharsets.class);
        }
        builder.addMethod(extractBytes.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Add standard getter (returns String)
        addStandardGetterImpl(builder, field, ClassName.get(String.class), ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);

        // Add bytes getter
        String bytesGetterName = "get" + ctx.capitalize(field.getJavaName()) + "Bytes";
        String bytesExtractMethodName = field.getExtractBytesMethodName();

        MethodSpec.Builder bytesGetter = MethodSpec.methodBuilder(bytesGetterName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Get the field value as a byte array.\n")
                .addJavadoc("@return Byte array value\n");

        if (field.needsHasCheck()) {
            bytesGetter.addStatement("return $L(proto) ? $L(proto) : null",
                    field.getExtractHasMethodName(), bytesExtractMethodName);
        } else {
            bytesGetter.addStatement("return $L(proto)", bytesExtractMethodName);
        }

        builder.addMethod(bytesGetter.build());
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // doSet (String)
        builder.addMethod(MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(ClassName.get(String.class), field.getJavaName())
                .build());

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
        String versionType = versionField != null ? versionField.getJavaType() : "String";
        boolean versionIsBytes = "byte[]".equals(versionType) || "ByteString".equals(versionType);
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // doSet (String)
        MethodSpec.Builder doSet = MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ClassName.get(String.class), field.getJavaName());

        if (presentInVersion) {
            if (versionIsBytes) {
                doSet.addStatement("protoBuilder.set$L($T.copyFromUtf8($L))",
                        versionJavaName, ClassName.get("com.google.protobuf", "ByteString"), field.getJavaName());
            } else {
                doSet.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
            }
        } else {
            doSet.addComment("Field not present in this version - ignored");
        }
        builder.addMethod(doSet.build());

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
        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing String extract method
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ClassName.get(String.class))
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return null")
                .build());

        // Add missing bytes extract method
        String bytesExtractMethodName = field.getExtractBytesMethodName();
        builder.addMethod(MethodSpec.methodBuilder(bytesExtractMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return null")
                .build());
    }
}
