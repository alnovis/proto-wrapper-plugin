package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for PRIMITIVE_MESSAGE conflict fields.
 *
 * <p>This handler processes fields where one version uses a primitive type and another
 * uses a message type. It generates both primitive and message accessor methods.</p>
 */
public final class PrimitiveMessageHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final PrimitiveMessageHandler INSTANCE = new PrimitiveMessageHandler();

    private PrimitiveMessageHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName primitiveType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method (returns primitive)
        addAbstractExtractMethod(builder, field, primitiveType, ctx);

        // Add message extract method
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            String messageExtractMethodName = field.getExtractMessageMethodName();
            builder.addMethod(MethodSpec.methodBuilder(messageExtractMethodName)
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(messageType)
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
        boolean versionIsPrimitive = versionField != null && versionField.isPrimitive();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName primitiveType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        if (field.isOptional() && !field.isRepeated()) {
            MethodSpec.Builder extractHas = MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto");

            if (versionIsPrimitive) {
                extractHas.addStatement("return proto.has$L()", versionJavaName);
            } else {
                extractHas.addJavadoc("Version has message type - primitive not available.\n");
                extractHas.addStatement("return false");
            }
            builder.addMethod(extractHas.build());
        }

        // Extract primitive value
        MethodSpec.Builder extractPrimitive = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(primitiveType)
                .addParameter(ctx.protoClassName(), "proto");

        if (versionIsPrimitive) {
            extractPrimitive.addStatement("return proto.get$L()", versionJavaName);
        } else {
            String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
            extractPrimitive.addJavadoc("Version has message type - returns default.\n");
            extractPrimitive.addStatement("return $L", defaultValue);
        }
        builder.addMethod(extractPrimitive.build());

        // Extract message value
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            String messageExtractMethodName = field.getExtractMessageMethodName();
            MethodSpec.Builder extractMessage = MethodSpec.methodBuilder(messageExtractMethodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(messageType)
                    .addParameter(ctx.protoClassName(), "proto");

            if (versionIsPrimitive) {
                extractMessage.addJavadoc("Version has primitive type - returns null.\n");
                extractMessage.addStatement("return null");
            } else {
                String wrapperClassName = getMessageWrapperClassName(field, ctx);
                extractMessage.addStatement("return new $L(proto.get$L())", wrapperClassName, versionJavaName);
            }
            builder.addMethod(extractMessage.build());
        }
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName primitiveType = ctx.parseFieldType(field);

        // Add standard getter (returns primitive)
        addStandardGetterImpl(builder, field, primitiveType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);

        // Add message getter
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            String messageGetterName = "get" + ctx.capitalize(field.getJavaName()) + "Message";
            String messageExtractMethodName = field.getExtractMessageMethodName();

            MethodSpec.Builder messageGetter = MethodSpec.methodBuilder(messageGetterName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(messageType)
                    .addJavadoc("Get the field as a message wrapper.\n")
                    .addJavadoc("@return Message wrapper, or null if this version has primitive type\n");

            messageGetter.addStatement("return $L(proto)", messageExtractMethodName);
            builder.addMethod(messageGetter.build());
        }
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName primitiveType = ctx.parseFieldType(field);

        // doSet (primitive)
        builder.addMethod(MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(primitiveType, field.getJavaName())
                .build());

        // doSet (message)
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            builder.addMethod(MethodSpec.methodBuilder(field.getDoSetMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .addParameter(messageType, field.getJavaName())
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
        boolean versionIsPrimitive = versionField != null && versionField.isPrimitive();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);
        TypeName primitiveType = ctx.parseFieldType(field);

        // doSet (primitive)
        MethodSpec.Builder doSetPrimitive = MethodSpec.methodBuilder(field.getDoSetMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(primitiveType, field.getJavaName());

        if (presentInVersion && versionIsPrimitive) {
            doSetPrimitive.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
        } else {
            doSetPrimitive.addComment("Field is message type or not present in this version - ignored");
        }
        builder.addMethod(doSetPrimitive.build());

        // doSet (message)
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            MethodSpec.Builder doSetMessage = MethodSpec.methodBuilder(field.getDoSetMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(messageType, field.getJavaName());

            if (presentInVersion && !versionIsPrimitive) {
                String protoTypeName = getProtoTypeForField(field, ctx, null);
                doSetMessage.addStatement("protoBuilder.set$L(($L) extractProto($L))",
                        versionJavaName, protoTypeName, field.getJavaName());
            } else {
                doSetMessage.addComment("Field is primitive type or not present in this version - ignored");
            }
            builder.addMethod(doSetMessage.build());
        }

        // doClear for optional
        if (field.isOptional()) {
            MethodSpec.Builder doClear = MethodSpec.methodBuilder(field.getDoClearMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED);

            addVersionConditionalClear(doClear, presentInVersion, versionJavaName);
            builder.addMethod(doClear.build());
        }
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName primitiveType = ctx.parseFieldType(field);

        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing primitive extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(primitiveType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);
        builder.addMethod(extract.build());

        // Add missing message extract method
        TypeName messageType = getMessageTypeForField(field, ctx);
        if (messageType != null) {
            String messageExtractMethodName = field.getExtractMessageMethodName();
            builder.addMethod(MethodSpec.methodBuilder(messageExtractMethodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(messageType)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addJavadoc("Field not present in this version.\n")
                    .addStatement("return null")
                    .build());
        }
    }
}
