package io.alnovis.protowrapper.generator.conflict;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static io.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.getVersionSpecificJavaName;

/**
 * Handler for scalar SIGNED_UNSIGNED type conflict fields.
 *
 * <p>This handler processes scalar fields where different proto versions use
 * different signedness variants of integer types. The unified type is always
 * {@code long}, which can safely represent all 32-bit and 64-bit values.</p>
 *
 * <h2>Conflict Examples</h2>
 * <pre>
 * // v1: message Order { int32 quantity = 1; }
 * // v2: message Order { uint32 quantity = 1; }
 *
 * // v1: message Record { sint32 offset = 2; }
 * // v2: message Record { int32 offset = 2; }
 * </pre>
 *
 * <h2>Protobuf Integer Types</h2>
 * <table>
 *   <caption>Integer Type Variants</caption>
 *   <tr><th>Type</th><th>Wire Format</th><th>Signedness</th><th>Java Type</th></tr>
 *   <tr><td>int32</td><td>varint</td><td>signed</td><td>int</td></tr>
 *   <tr><td>uint32</td><td>varint</td><td>unsigned</td><td>int*</td></tr>
 *   <tr><td>sint32</td><td>ZigZag varint</td><td>signed</td><td>int</td></tr>
 *   <tr><td>fixed32</td><td>fixed 4 bytes</td><td>unsigned</td><td>int*</td></tr>
 *   <tr><td>sfixed32</td><td>fixed 4 bytes</td><td>signed</td><td>int</td></tr>
 * </table>
 * <p>* uint32/fixed32 values &gt; Integer.MAX_VALUE appear as negative in Java.</p>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Uses {@code long} as the unified type:</p>
 * <ul>
 *   <li>Reading from unsigned version: extends to long with proper unsigned conversion</li>
 *   <li>Writing to signed version: validates value fits in signed range</li>
 *   <li>Writing to unsigned version: validates value is non-negative</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code long getQuantity()}, {@code Builder.setQuantity(long)}</li>
 *   <li><b>Getter:</b> Converts to long with proper sign handling</li>
 *   <li><b>Setter:</b> Range validation based on target version type</li>
 * </ul>
 *
 * <h2>Range Validation</h2>
 * <ul>
 *   <li>For signed 32-bit: Integer.MIN_VALUE to Integer.MAX_VALUE</li>
 *   <li>For unsigned 32-bit: 0 to 4294967295 (0xFFFFFFFFL)</li>
 *   <li>For signed 64-bit: Long.MIN_VALUE to Long.MAX_VALUE (no validation)</li>
 *   <li>For unsigned 64-bit: 0 to Long.MAX_VALUE (partial range)</li>
 * </ul>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see MergedField.ConflictType#SIGNED_UNSIGNED
 */
public final class SignedUnsignedHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final SignedUnsignedHandler INSTANCE = new SignedUnsignedHandler();

    /** Private constructor for singleton. */
    private SignedUnsignedHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.SIGNED_UNSIGNED;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.SIGNED_UNSIGNED;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // SIGNED_UNSIGNED extract methods use long as return type
        addAbstractHasMethod(builder, field, ctx);
        addAbstractExtractMethod(builder, field, TypeName.LONG, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            // Field not present in this version - return 0L
            addMissingFieldExtract(builder, field, TypeName.LONG, "0L", ctx);
            return;
        }

        String version = ctx.requireVersion();
        FieldInfo versionField = field.getVersionFields().get(version);
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Determine if this version uses unsigned type
        boolean isUnsigned = isUnsignedType(versionField);
        boolean is32Bit = is32BitType(versionField);

        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.LONG)
                .addParameter(ctx.protoClassName(), "proto");

        if (isUnsigned && is32Bit) {
            // uint32/fixed32: need to convert to long with unsigned semantics
            // Java's int is signed, so large unsigned values appear negative
            // Use Integer.toUnsignedLong() for proper conversion
            extract.addStatement("return $T.toUnsignedLong(proto.get$L())",
                    Integer.class, versionJavaName);
        } else if (!is32Bit) {
            // 64-bit type: already returns long, no conversion needed
            extract.addStatement("return proto.get$L()", versionJavaName);
        } else {
            // Signed 32-bit (int32, sint32, sfixed32): simple widening cast
            extract.addStatement("return (long) proto.get$L()", versionJavaName);
        }

        builder.addMethod(extract.build());

        // Add has method for fields that support has*()
        if (field.shouldGenerateHasMethod()) {
            addHasExtractImpl(builder, field, versionJavaName, ctx);
        }
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Getter implementation is the same as default (uses extract methods)
        DefaultHandler.INSTANCE.addGetterImplementation(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Use template method for scalar abstract builder methods with long type
        addAbstractScalarBuilderMethods(builder, field, TypeName.LONG, ctx);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // Use template method for scalar concrete builder methods with long type
        addScalarConcreteBuilderMethods(builder, field, TypeName.LONG, builderReturnType, ctx);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        FieldInfo versionField = field.getVersionFields().get(version);
        boolean isUnsigned = isUnsignedType(versionField);
        boolean is32Bit = is32BitType(versionField);

        // doSet - with range validation based on target type
        buildDoSetImpl(builder, "doSet" + capitalizedName, TypeName.LONG, field.getJavaName(),
                presentInVersion, m -> {
                    if (is32Bit) {
                        if (isUnsigned) {
                            addUnsigned32SetterBody(m, field.getJavaName(), versionJavaName, version);
                        } else {
                            addSigned32SetterBody(m, field.getJavaName(), versionJavaName, version);
                        }
                    } else {
                        if (isUnsigned) {
                            addUnsigned64SetterBody(m, field.getJavaName(), versionJavaName, version);
                        } else {
                            // Signed 64-bit: no range check needed, direct set
                            m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                        }
                    }
                });

        // doClear - use template method (only for fields with has*())
        if (field.shouldGenerateHasMethod()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    /**
     * Adds setter body for unsigned 32-bit types (uint32, fixed32).
     * Validates value is in range [0, 0xFFFFFFFFL].
     */
    private void addUnsigned32SetterBody(MethodSpec.Builder doSet, String fieldName,
                                          String versionJavaName, String version) {
        // Check for valid unsigned 32-bit range
        doSet.beginControlFlow("if ($L < 0 || $L > 0xFFFFFFFFL)", fieldName, fieldName);
        doSet.addStatement("throw new $T(\"Value \" + $L + \" is outside unsigned 32-bit range [0, 4294967295] for $L\")",
                IllegalArgumentException.class, fieldName, version);
        doSet.endControlFlow();

        // Cast to int (Java will interpret high values as negative, which is correct for unsigned)
        doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, fieldName);
    }

    /**
     * Adds setter body for signed 32-bit types (int32, sint32, sfixed32).
     * Validates value fits in int range.
     */
    private void addSigned32SetterBody(MethodSpec.Builder doSet, String fieldName,
                                        String versionJavaName, String version) {
        // Check for valid signed 32-bit range
        doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                fieldName, Integer.class, fieldName, Integer.class);
        doSet.addStatement("throw new $T(\"Value \" + $L + \" is outside int32 range for $L\")",
                IllegalArgumentException.class, fieldName, version);
        doSet.endControlFlow();

        // Safe cast to int
        doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, fieldName);
    }

    /**
     * Adds setter body for unsigned 64-bit types (uint64, fixed64).
     * Validates value is non-negative (we can only represent half the unsigned range).
     */
    private void addUnsigned64SetterBody(MethodSpec.Builder doSet, String fieldName,
                                          String versionJavaName, String version) {
        // Check for non-negative (Java long can't represent values > Long.MAX_VALUE)
        doSet.beginControlFlow("if ($L < 0)", fieldName);
        doSet.addStatement("throw new $T(\"Value \" + $L + \" is negative, cannot represent as unsigned 64-bit for $L\")",
                IllegalArgumentException.class, fieldName, version);
        doSet.endControlFlow();

        doSet.addStatement("protoBuilder.set$L($L)", versionJavaName, fieldName);
    }

    /**
     * Checks if the field type is unsigned (uint32, uint64, fixed32, fixed64).
     *
     * @param field the field to check
     * @return true if the field type is unsigned
     */
    private boolean isUnsignedType(FieldInfo field) {
        if (field == null) return false;
        Type type = field.getType();
        return type == Type.TYPE_UINT32 || type == Type.TYPE_UINT64 ||
               type == Type.TYPE_FIXED32 || type == Type.TYPE_FIXED64;
    }

    /**
     * Checks if the field type is 32-bit (int32, uint32, sint32, fixed32, sfixed32).
     *
     * @param field the field to check
     * @return true if the field type is 32-bit
     */
    private boolean is32BitType(FieldInfo field) {
        if (field == null) return true; // default to 32-bit if unknown
        Type type = field.getType();
        return type == Type.TYPE_INT32 || type == Type.TYPE_UINT32 ||
               type == Type.TYPE_SINT32 || type == Type.TYPE_FIXED32 ||
               type == Type.TYPE_SFIXED32;
    }

    /**
     * Adds has extract implementation for optional fields.
     * Called only when field is present in version.
     */
    private void addHasExtractImpl(TypeSpec.Builder builder, MergedField field,
                                    String versionJavaName, ProcessingContext ctx) {
        MethodSpec.Builder hasMethod = MethodSpec.methodBuilder(field.getExtractHasMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addParameter(ctx.protoClassName(), "proto");

        hasMethod.addStatement("return proto.has$L()", versionJavaName);

        builder.addMethod(hasMethod.build());
    }
}
