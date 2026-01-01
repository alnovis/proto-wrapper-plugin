package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for scalar FLOAT_DOUBLE type conflict fields.
 *
 * <p>This handler processes scalar fields where different proto versions use
 * float in some versions and double in others. The unified type is always
 * double, which can represent all float values without loss.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message Sensor { float temperature = 1; }
 * // v2: message Sensor { double temperature = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>Uses double as the unified type:</p>
 * <ul>
 *   <li>Reading from float version: automatic widening cast (float to double)</li>
 *   <li>Writing to float version: range validation + narrowing cast (double to float)</li>
 * </ul>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code double getTemperature()}, {@code Builder.setTemperature(double)}</li>
 *   <li><b>Getter:</b> Automatic widening when reading from float version</li>
 *   <li><b>Setter:</b> Range validation when writing to float version</li>
 * </ul>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder setter methods include range validation for float versions:</p>
 * <ul>
 *   <li>When setting to v1 (float): validates value fits in float range</li>
 *   <li>When setting to v2 (double): no validation needed</li>
 *   <li>Throws {@code IllegalArgumentException} if value exceeds float range</li>
 * </ul>
 *
 * <h2>Range Validation</h2>
 * <p>Float range: approximately -3.4028235E38 to 3.4028235E38</p>
 * <p>Special cases: NaN and Infinity are preserved without validation.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see MergedField.ConflictType#FLOAT_DOUBLE
 */
public final class FloatDoubleHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final FloatDoubleHandler INSTANCE = new FloatDoubleHandler();

    private FloatDoubleHandler() {
        // Singleton
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.FLOAT_DOUBLE;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // FLOAT_DOUBLE extract methods use double as return type
        addAbstractHasMethod(builder, field, ctx);
        addAbstractExtractMethod(builder, field, TypeName.DOUBLE, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        // Extract implementation uses DefaultHandler's logic
        // The widening cast from float to double is automatic in Java
        DefaultHandler.INSTANCE.addExtractImplementation(builder, field, presentInVersion, ctx);
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Getter implementation is the same as default
        DefaultHandler.INSTANCE.addGetterImplementation(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Use template method for scalar abstract builder methods with double type
        addAbstractScalarBuilderMethods(builder, field, TypeName.DOUBLE, ctx);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // Use template method for scalar concrete builder methods with double type
        addScalarConcreteBuilderMethods(builder, field, TypeName.DOUBLE, builderReturnType, ctx);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String version = ctx.requireVersion();
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        FieldInfo versionField = field.getVersionFields().get(version);
        String versionType = versionField != null ? versionField.getJavaType() : "double";
        boolean needsNarrowing = "float".equals(versionType) || "Float".equals(versionType);

        // doSet - with narrowing validation for float versions
        buildDoSetImpl(builder, "doSet" + capitalizedName, TypeName.DOUBLE, field.getJavaName(),
                presentInVersion, m -> {
                    if (needsNarrowing) {
                        addNarrowingSetterBody(m, field.getJavaName(), versionJavaName, version);
                    } else {
                        m.addStatement("protoBuilder.set$L($L)", versionJavaName, field.getJavaName());
                    }
                });

        // doClear - use template method
        if (field.isOptional()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    /**
     * Adds narrowing setter body with float range validation.
     *
     * <p>Generates code that validates the double value fits in float range
     * before narrowing cast. Special values (NaN, Infinity) are preserved.</p>
     *
     * @param doSet method builder
     * @param fieldName the field name for the parameter
     * @param versionJavaName version-specific proto field name
     * @param version version string for error messages
     */
    private void addNarrowingSetterBody(MethodSpec.Builder doSet, String fieldName,
                                         String versionJavaName, String version) {
        // Check for special values first (NaN and Infinity are preserved)
        doSet.beginControlFlow("if (!$T.isNaN($L) && !$T.isInfinite($L))",
                Double.class, fieldName, Double.class, fieldName);

        // Range check for finite values
        doSet.beginControlFlow("if ($L < -$T.MAX_VALUE || $L > $T.MAX_VALUE)",
                fieldName, Float.class, fieldName, Float.class);
        doSet.addStatement("throw new $T(\"Value \" + $L + \" exceeds float range for $L\")",
                IllegalArgumentException.class, fieldName, version);
        doSet.endControlFlow();

        doSet.endControlFlow();

        // Perform the narrowing cast
        doSet.addStatement("protoBuilder.set$L((float) $L)", versionJavaName, fieldName);
    }
}
