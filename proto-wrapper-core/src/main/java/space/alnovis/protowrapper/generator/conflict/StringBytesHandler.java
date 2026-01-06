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
 * Handler for STRING_BYTES type conflict fields.
 *
 * <p>This handler processes fields where one proto version uses {@code string} type
 * and another version uses {@code bytes} type for the same field number.
 * This is common when binary data needs to be stored as text or vice versa.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Document { string content = 1; }
 * // v2: message Document { bytes content = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The handler generates dual accessor methods with UTF-8 conversion:</p>
 * <ul>
 *   <li>{@code getContent()} - returns String (primary accessor)</li>
 *   <li>{@code getContentBytes()} - returns byte[] (binary accessor)</li>
 * </ul>
 *
 * <p>Conversion is handled transparently:</p>
 * <ul>
 *   <li>bytes to String: {@code ByteString.toString(StandardCharsets.UTF_8)}</li>
 *   <li>String to bytes: {@code String.getBytes(StandardCharsets.UTF_8)}</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code String getContent()}, {@code byte[] getContentBytes()},
 *       {@code Builder.setContent(String)}, {@code Builder.setContentBytes(byte[])}</li>
 *   <li><b>Abstract:</b> {@code extractContent(proto)}, {@code extractContentBytes(proto)}</li>
 *   <li><b>Impl:</b> Version-specific extraction with UTF-8 conversion</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder provides both String and byte[] setters:</p>
 * <ul>
 *   <li>{@code setContent(String)} - converts to ByteString for bytes versions</li>
 *   <li>{@code setContentBytes(byte[])} - converts to String for string versions</li>
 * </ul>
 *
 * <h2>Encoding</h2>
 * <p>All conversions use UTF-8 encoding. Non-UTF-8 binary data may be corrupted
 * when using String accessors on bytes fields. Use byte[] accessors for binary data.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 */
public final class StringBytesHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final StringBytesHandler INSTANCE = new StringBytesHandler();

    /** Private constructor for singleton. */
    private StringBytesHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.STRING_BYTES;
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
        addAbstractDoSet(builder, field.getDoSetMethodName(), ClassName.get(String.class), field.getJavaName());

        // doSetBytes (byte[])
        addAbstractDoSet(builder, field.getDoSetMethodName() + "Bytes", ArrayTypeName.of(TypeName.BYTE), field.getJavaName());

        // doClear for optional
        if (field.isOptional()) {
            addAbstractDoClear(builder, field.getDoClearMethodName());
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
        buildDoSetImpl(builder, field.getDoSetMethodName(), ClassName.get(String.class), field.getJavaName(),
                presentInVersion, m -> {
                    if (versionIsBytes) {
                        m.addStatement("protoBuilder.set$L($T.copyFromUtf8($L))",
                                versionJavaName, ClassName.get("com.google.protobuf", "ByteString"), field.getJavaName());
                    } else {
                        m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                    }
                });

        // doSetBytes (byte[])
        buildDoSetImpl(builder, field.getDoSetMethodName() + "Bytes", ArrayTypeName.of(TypeName.BYTE), field.getJavaName(),
                presentInVersion, m -> {
                    if (versionIsBytes) {
                        m.addStatement("protoBuilder.set$L($T.copyFrom($L))",
                                versionJavaName, ClassName.get("com.google.protobuf", "ByteString"), field.getJavaName());
                    } else {
                        m.addStatement("protoBuilder.set$L(new $T($L, $T.UTF_8))",
                                versionJavaName, String.class, field.getJavaName(), StandardCharsets.class);
                    }
                });

        // doClear for optional
        if (field.isOptional()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // setXxx(String)
        addConcreteSetMethod(builder, field.getJavaName(), ClassName.get(String.class), builderReturnType, ctx);

        // setXxxBytes(byte[])
        String capName = ctx.capitalize(field.getJavaName());
        builder.addMethod(MethodSpec.methodBuilder("set" + capName + "Bytes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(ArrayTypeName.of(TypeName.BYTE), field.getJavaName())
                .returns(builderReturnType)
                .addStatement("$LBytes($L)", field.getDoSetMethodName(), field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx() for optional
        if (field.isOptional()) {
            addConcreteClearMethod(builder, field.getJavaName(), builderReturnType, ctx);
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
