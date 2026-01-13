package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;
import static space.alnovis.protowrapper.generator.TypeUtils.*;

/**
 * Handler for scalar WIDENING type conflict fields.
 *
 * <p>This handler processes scalar fields where different proto versions use
 * different numeric types that can be safely widened. The unified type is
 * the widest type across all versions.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Product { int32 price = 1; }
 * // v2: message Product { int64 price = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Uses the widest type as the unified type:</p>
 * <ul>
 *   <li>int32 + int64 → long</li>
 *   <li>float + double → double</li>
 *   <li>int32 + float → not supported (INCOMPATIBLE)</li>
 * </ul>
 *
 * <h2>Widening Hierarchy</h2>
 * <pre>
 * int32 → int64 → (unified as long)
 * float → double → (unified as double)
 * </pre>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code long getPrice()}, {@code Builder.setPrice(long)}</li>
 *   <li><b>Abstract:</b> {@code extractPrice(proto)} - delegates to DefaultHandler</li>
 *   <li><b>Impl:</b> Automatic widening cast when reading from narrower versions</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder setter methods include range validation for narrower versions:</p>
 * <ul>
 *   <li>When setting to v1 (int32): validates value fits in int range</li>
 *   <li>When setting to v2 (int64): no validation needed</li>
 *   <li>Throws {@code IllegalArgumentException} if value exceeds target range</li>
 * </ul>
 *
 * <h2>Implementation Note</h2>
 * <p>Extract methods delegate to {@link DefaultHandler} since the widening cast
 * is already handled by {@code generateProtoGetterCall}. This handler only provides
 * special builder methods with narrowing validation.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see DefaultHandler
 */
public final class WideningHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final WideningHandler INSTANCE = new WideningHandler();

    /** Private constructor for singleton. */
    private WideningHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.WIDENING;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.WIDENING;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // WIDENING extract methods are the same as default - delegate to parent
        TypeName returnType = ctx.parseFieldType(field);
        addAbstractHasMethod(builder, field, ctx);
        addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        // WIDENING extract implementation uses the DefaultHandler's logic
        // The widening cast is handled in generateProtoGetterCall
        DefaultHandler.INSTANCE.addExtractImplementation(builder, field, presentInVersion, ctx);
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // WIDENING getter implementation is the same as default
        DefaultHandler.INSTANCE.addGetterImplementation(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());
        // Use template method for scalar abstract builder methods
        addAbstractScalarBuilderMethods(builder, field, widerType, ctx);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        TypeName widerType = getWiderPrimitiveType(field.getJavaType());
        // Use template method for scalar concrete builder methods
        addScalarConcreteBuilderMethods(builder, field, widerType, builderReturnType, ctx);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        String widerType = field.getJavaType();
        TypeName widerTypeName = getWiderPrimitiveType(widerType);

        FieldInfo versionField = field.getVersionFields().get(version);
        String versionType = versionField != null ? versionField.getJavaType() : widerType;
        boolean needsNarrowing = !versionType.equals(widerType) &&
                !versionType.equals("Long") && !versionType.equals("Double");

        // doSet - use template method
        buildDoSetImpl(builder, "doSet" + capitalizedName, widerTypeName, field.getJavaName(),
                presentInVersion, m -> {
                    if (needsNarrowing) {
                        addNarrowingSetterBody(m, field.getJavaName(), widerType, versionType, versionJavaName, version);
                    } else {
                        m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                    }
                });

        // doClear - use template method
        if (field.shouldGenerateHasMethod()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    private void addNarrowingSetterBody(MethodSpec.Builder doSet, String fieldName,
                                         String widerType, String versionType,
                                         String versionJavaName, String version) {
        switch (widerType) {
            case "long", "Long", "double", "Double" -> {
                // Narrowing to int with range check
                doSet.beginControlFlow("if ($L < $T.MIN_VALUE || $L > $T.MAX_VALUE)",
                        fieldName, Integer.class, fieldName, Integer.class);
                doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds int range for $L\")",
                        IllegalArgumentException.class, fieldName, version);
                doSet.endControlFlow();
                doSet.addStatement("protoBuilder.set$L((int) $L)", versionJavaName, fieldName);
            }
            default -> // Unknown narrowing - just cast
                    doSet.addStatement("protoBuilder.set$L(($L) $L)", versionJavaName, versionType, fieldName);
        }
    }

}
