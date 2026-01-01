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
 * Handler for PRIMITIVE_MESSAGE type conflict fields.
 *
 * <p>This handler processes fields where one proto version uses a primitive type
 * (int32, string, etc.) and another version uses a message type for the same
 * field number. This is a rare but valid schema evolution scenario.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Order { int64 total = 1; }
 * // v2: message Order { Money total = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The handler generates dual accessor methods:</p>
 * <ul>
 *   <li>{@code getTotal()} - returns primitive value (for primitive versions)</li>
 *   <li>{@code getTotalMessage()} - returns message wrapper (for message versions)</li>
 * </ul>
 *
 * <p>Behavior depends on the current version:</p>
 * <ul>
 *   <li>Primitive version: {@code getTotal()} returns value, {@code getTotalMessage()} returns null</li>
 *   <li>Message version: {@code getTotal()} returns default, {@code getTotalMessage()} returns wrapper</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code long getTotal()}, {@code Money getTotalMessage()}</li>
 *   <li><b>Abstract:</b> {@code extractTotal(proto)}, {@code extractTotalMessage(proto)}</li>
 *   <li><b>Impl:</b> Version-specific extraction returning value or null</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder methods are <b>not generated</b> for PRIMITIVE_MESSAGE conflict fields.
 * The type incompatibility makes it impossible to provide a safe unified setter.
 * Application code must use version-specific builders when modifying such fields.</p>
 *
 * <h2>Use Case</h2>
 * <p>This conflict type is read-only. It allows applications to read data from
 * multiple versions but not modify the field through the unified API.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
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
        // Treat String and bytes as "primitive-like" for this purpose
        // because they can be accessed directly without needing message wrapper
        boolean versionIsPrimitive = versionField != null && isPrimitiveOrPrimitiveLike(versionField);
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
        // PRIMITIVE_MESSAGE conflict fields don't have builder methods in the interface
        // (InterfaceGenerator skips them via shouldSkipBuilderSetter)
        // And ImplClassGenerator also skips them
        // So we don't generate abstract builder methods either to maintain consistency
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        // PRIMITIVE_MESSAGE conflict fields don't have builder methods
        // ImplClassGenerator skips them via shouldSkipBuilderSetter
        // So we don't generate impl methods either to maintain consistency
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // PRIMITIVE_MESSAGE conflict fields don't have builder methods in the interface
        // (InterfaceGenerator skips them via shouldSkipBuilderSetter)
        // So we don't generate concrete implementations either
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

    /**
     * Check if field is primitive or "primitive-like" (String, bytes).
     * String and bytes are treated as primitive-like because they can be accessed
     * directly without needing message wrappers, unlike nested message types.
     */
    private boolean isPrimitiveOrPrimitiveLike(FieldInfo field) {
        if (field.isPrimitive()) {
            return true;
        }
        // String and bytes are "primitive-like" - they're not nested messages
        String javaType = field.getJavaType();
        return "String".equals(javaType) || "byte[]".equals(javaType) || "ByteString".equals(javaType);
    }
}
