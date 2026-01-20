package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static io.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for repeated well-known type fields.
 *
 * <p>This handler processes repeated (list) fields that use Google Well-Known Types
 * and converts them to lists of idiomatic Java types when {@code convertWellKnownTypes}
 * is enabled.</p>
 *
 * <h2>Supported Conversions</h2>
 * <table>
 *   <caption>Repeated Well-Known Type Conversions</caption>
 *   <tr><th>Proto Type</th><th>Java Type</th></tr>
 *   <tr><td>repeated google.protobuf.Timestamp</td><td>List&lt;Instant&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.Duration</td><td>List&lt;Duration&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.StringValue</td><td>List&lt;String&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.Int32Value</td><td>List&lt;Integer&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.Int64Value</td><td>List&lt;Long&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.BoolValue</td><td>List&lt;Boolean&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.FloatValue</td><td>List&lt;Float&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.DoubleValue</td><td>List&lt;Double&gt;</td></tr>
 *   <tr><td>repeated google.protobuf.BytesValue</td><td>List&lt;byte[]&gt;</td></tr>
 * </table>
 *
 * <h2>Generated Code Example</h2>
 * <pre>
 * // Proto: repeated google.protobuf.Timestamp timestamps = 1;
 * // Generated extraction:
 * return proto.getTimestampsList().stream()
 *     .map(t -> java.time.Instant.ofEpochSecond(t.getSeconds(), t.getNanos()))
 *     .collect(java.util.stream.Collectors.toList());
 * </pre>
 *
 * <h2>Performance Note</h2>
 * <p>Each getter call creates a new list via stream operations. For performance-critical
 * code accessing the same field multiple times, cache the result in a local variable.</p>
 *
 * @since 1.3.0
 * @see WellKnownTypeInfo
 * @see WellKnownTypeHandler
 * @see ConflictHandler
 */
public final class RepeatedWellKnownTypeHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance. */
    public static final RepeatedWellKnownTypeHandler INSTANCE = new RepeatedWellKnownTypeHandler();

    /** Private constructor for singleton. */
    private RepeatedWellKnownTypeHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.REPEATED_WELL_KNOWN_TYPE;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return field.isRepeated()
                && !field.isMap()
                && field.isWellKnownType()
                && ctx.config().isConvertWellKnownTypes();
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();

        // Add main extract method (returns List<T>)
        addAbstractExtractMethod(builder, field, wkt.getListJavaTypeName(), ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, ctx);
            return;
        }

        WellKnownTypeInfo wkt = field.getWellKnownType();
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Generate stream-based conversion with FQN for StructConverter
        String streamConversion = generateStreamConversion(wkt, versionJavaName, ctx.apiPackage());

        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(wkt.getListJavaTypeName())
                .addParameter(ctx.protoClassName(), "proto")
                .addStatement("return $L", streamConversion)
                .build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();

        // Add standard getter (returns List<T>)
        MethodSpec getter = MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(wkt.getListJavaTypeName())
                .addStatement("return $L(proto)", field.getExtractMethodName())
                .build();
        builder.addMethod(getter);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName elementType = wkt.getJavaTypeName();
        TypeName listType = wkt.getListJavaTypeName();
        String capName = ctx.capitalize(field.getJavaName());

        // doAdd (single element)
        addAbstractDoSet(builder, "doAdd" + capName, elementType, field.getJavaName());

        // doAddAll (list)
        addAbstractDoSet(builder, "doAddAll" + capName, listType, field.getJavaName());

        // doSet (replace all)
        addAbstractDoSet(builder, "doSet" + capName, listType, field.getJavaName());

        // doClear
        addAbstractDoClear(builder, "doClear" + capName);
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();
        TypeName elementType = wkt.getJavaTypeName();
        TypeName listType = wkt.getListJavaTypeName();
        String capName = ctx.capitalize(field.getJavaName());
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // doAdd (single element)
        buildDoSetImpl(builder, "doAdd" + capName, elementType, field.getJavaName(),
                presentInVersion, m -> {
                    String buildingCode = wkt.getBuildingCode(field.getJavaName(), ctx.apiPackage());
                    m.addStatement("protoBuilder.add$L($L)", versionJavaName, buildingCode);
                });

        // doAddAll (list)
        buildDoSetImpl(builder, "doAddAll" + capName, listType, field.getJavaName(),
                presentInVersion, m -> {
                    m.addStatement("$L.forEach(e -> protoBuilder.add$L($L))",
                            field.getJavaName(), versionJavaName, wkt.getBuildingCode("e", ctx.apiPackage()));
                });

        // doSet (replace all)
        buildDoSetImpl(builder, "doSet" + capName, listType, field.getJavaName(),
                presentInVersion, m -> {
                    m.addStatement("protoBuilder.clear$L()", versionJavaName);
                    m.addStatement("$L.forEach(e -> protoBuilder.add$L($L))",
                            field.getJavaName(), versionJavaName, wkt.getBuildingCode("e", ctx.apiPackage()));
                });

        // doClear
        buildDoClearImpl(builder, "doClear" + capName, presentInVersion, versionJavaName);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();

        // Use standard repeated field builder methods
        addRepeatedConcreteBuilderMethods(builder, field, wkt.getJavaTypeName(), wkt.getListJavaTypeName(), builderReturnType, ctx);
    }

    private String generateStreamConversion(WellKnownTypeInfo wkt, String versionJavaName, String apiPackage) {
        // Generate stream conversion code based on WKT type
        String structConverterFQN = apiPackage != null ? apiPackage + ".StructConverter" : "StructConverter";
        return switch (wkt) {
            case TIMESTAMP -> String.format(
                    "proto.get%sList().stream().map(t -> java.time.Instant.ofEpochSecond(t.getSeconds(), t.getNanos())).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case DURATION -> String.format(
                    "proto.get%sList().stream().map(d -> java.time.Duration.ofSeconds(d.getSeconds(), d.getNanos())).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case STRING_VALUE, INT32_VALUE, INT64_VALUE, UINT64_VALUE, BOOL_VALUE, FLOAT_VALUE, DOUBLE_VALUE -> String.format(
                    "proto.get%sList().stream().map(w -> w.getValue()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case UINT32_VALUE -> String.format(
                    "proto.get%sList().stream().map(w -> Integer.toUnsignedLong(w.getValue())).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case BYTES_VALUE -> String.format(
                    "proto.get%sList().stream().map(w -> w.getValue().toByteArray()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case FIELD_MASK -> String.format(
                    "proto.get%sList().stream().map(fm -> fm.getPathsList()).collect(java.util.stream.Collectors.toList())",
                    versionJavaName);
            case STRUCT -> String.format(
                    "proto.get%sList().stream().map(%s::toMap).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, structConverterFQN);
            case VALUE -> String.format(
                    "proto.get%sList().stream().map(%s::toObject).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, structConverterFQN);
            case LIST_VALUE -> String.format(
                    "proto.get%sList().stream().map(%s::toList).collect(java.util.stream.Collectors.toList())",
                    versionJavaName, structConverterFQN);
        };
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        WellKnownTypeInfo wkt = field.getWellKnownType();

        // Add missing extract method returning empty list
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(wkt.getListJavaTypeName())
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n")
                .addStatement("return java.util.Collections.emptyList()")
                .build());
    }
}
