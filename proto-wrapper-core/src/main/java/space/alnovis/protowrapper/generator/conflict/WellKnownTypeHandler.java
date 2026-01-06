package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for scalar well-known type fields.
 *
 * <p>This handler processes fields that use Google Well-Known Types and converts
 * them to idiomatic Java types when {@code convertWellKnownTypes} is enabled.</p>
 *
 * <h2>Supported Conversions</h2>
 * <table>
 *   <caption>Well-Known Type Conversions</caption>
 *   <tr><th>Proto Type</th><th>Java Type</th></tr>
 *   <tr><td>google.protobuf.Timestamp</td><td>java.time.Instant</td></tr>
 *   <tr><td>google.protobuf.Duration</td><td>java.time.Duration</td></tr>
 *   <tr><td>google.protobuf.StringValue</td><td>String</td></tr>
 *   <tr><td>google.protobuf.Int32Value</td><td>Integer</td></tr>
 *   <tr><td>google.protobuf.Int64Value</td><td>Long</td></tr>
 *   <tr><td>google.protobuf.BoolValue</td><td>Boolean</td></tr>
 *   <tr><td>google.protobuf.FloatValue</td><td>Float</td></tr>
 *   <tr><td>google.protobuf.DoubleValue</td><td>Double</td></tr>
 *   <tr><td>google.protobuf.BytesValue</td><td>byte[]</td></tr>
 * </table>
 *
 * <h2>Generated Code</h2>
 * <ul>
 *   <li><b>Interface:</b> {@code Instant getCreatedAt()}</li>
 *   <li><b>Abstract:</b> {@code extractCreatedAt(proto)}</li>
 *   <li><b>Impl:</b> Inline conversion code</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * // Proto: google.protobuf.Timestamp created_at = 1;
 * // Generated extraction:
 * proto.hasCreatedAt()
 *     ? java.time.Instant.ofEpochSecond(proto.getCreatedAt().getSeconds(),
 *                                        proto.getCreatedAt().getNanos())
 *     : null
 * </pre>
 *
 * @since 1.3.0
 * @see WellKnownTypeInfo
 * @see ConflictHandler
 */
public final class WellKnownTypeHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final WellKnownTypeHandler INSTANCE = new WellKnownTypeHandler();

    /** Private constructor for singleton. */
    private WellKnownTypeHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.WELL_KNOWN_TYPE;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated()
                && !field.isMap()
                && field.isWellKnownType()
                && ctx.config().isConvertWellKnownTypes();
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method returning the Java type
        addAbstractExtractMethod(builder, field, javaType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, ctx);
            return;
        }

        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Add extractHas for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Add extraction method with inline conversion
        String protoGetter = "proto.get" + versionJavaName + "()";
        String hasCheck = "proto.has" + versionJavaName + "()";
        String extractionCode = wkt.getExtractionCode(protoGetter, hasCheck, ctx.apiPackage());

        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(javaType)
                .addParameter(ctx.protoClassName(), "proto")
                .addStatement("return $L", extractionCode)
                .build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();

        // Add standard getter
        addStandardGetterImpl(builder, field, javaType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();

        // doSet
        addAbstractDoSet(builder, field.getDoSetMethodName(), javaType, field.getJavaName());

        // doClear for optional
        if (field.isOptional()) {
            addAbstractDoClear(builder, field.getDoClearMethodName());
        }
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // doSet implementation
        buildDoSetImpl(builder, field.getDoSetMethodName(), javaType, field.getJavaName(),
                presentInVersion, m -> {
                    // Generate inline building code with FQN for StructConverter
                    String setterCode = wkt.getBuilderSetterCode("protoBuilder", versionJavaName, field.getJavaName(), ctx.apiPackage());
                    m.addCode(setterCode + "\n");
                });

        // doClear for optional
        if (field.isOptional()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();

        // setXxx
        addConcreteSetMethod(builder, field.getJavaName(), javaType, builderReturnType, ctx);

        // clearXxx for optional
        if (field.isOptional()) {
            addConcreteClearMethod(builder, field.getJavaName(), builderReturnType, ctx);
        }
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName javaType = wkt.getJavaTypeName();

        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing extract method
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(javaType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return null")
                .build());
    }
}
