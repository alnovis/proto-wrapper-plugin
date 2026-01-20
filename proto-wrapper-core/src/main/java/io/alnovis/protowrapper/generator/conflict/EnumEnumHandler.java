package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static io.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

/**
 * Handler for ENUM_ENUM type conflict fields.
 *
 * <p>This handler processes fields where different proto versions use different
 * enum types for the same field number. Since the enum types are incompatible
 * at the Java level, we provide only an integer accessor.</p>
 *
 * <h2>Conflict Example</h2>
 * <pre>
 * // v1: message EnumValueConflicts { SharedEnumV1 shared_type = 1; }
 * // v2: message EnumValueConflicts { SharedEnumV2 shared_type = 1; }
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The handler generates an integer accessor since different enum types
 * share the same wire format (varint):</p>
 * <ul>
 *   <li>{@code int getSharedType()} - returns the raw enum number value</li>
 * </ul>
 *
 * <h2>Wire Compatibility</h2>
 * <p>All protobuf enum types use varint wire format, so cross-version
 * conversion works at the binary level. The integer accessor provides
 * a safe unified API for accessing the raw value.</p>
 *
 * <h2>Builder Behavior</h2>
 * <p>Builder setter methods accept int values and set them directly:</p>
 * <ul>
 *   <li>{@code setSharedType(int)} - sets the raw enum value</li>
 * </ul>
 */
public final class EnumEnumHandler extends AbstractConflictHandler implements ConflictHandler {

    /** Singleton instance of the enum-enum handler. */
    public static final EnumEnumHandler INSTANCE = new EnumEnumHandler();

    private EnumEnumHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.ENUM_ENUM;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        return !field.isRepeated() && field.getConflictType() == MergedField.ConflictType.ENUM_ENUM;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // For ENUM_ENUM, the unified type is int (raw enum value)
        TypeName returnType = TypeName.INT;

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method (returns int)
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(ctx.protoType(), "proto")
                .build());
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        if (!presentInVersion) {
            addMissingFieldExtract(builder, field, TypeName.INT, "0", ctx);
            return;
        }

        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // Add extractHas for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Main extract - returns int (enum.getNumber())
        MethodSpec.Builder extractInt = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.INT)
                .addParameter(ctx.protoClassName(), "proto");

        // All versions have enum, so always call getNumber()
        extractInt.addStatement("return proto.get$L().getNumber()", versionJavaName);
        builder.addMethod(extractInt.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // Add standard getter (returns int)
        addStandardGetterImpl(builder, field, TypeName.INT, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        // doSet (int)
        addAbstractDoSet(builder, field.getDoSetMethodName(), TypeName.INT, field.getJavaName());

        // doClear
        addAbstractDoClear(builder, field.getDoClearMethodName());
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        String versionJavaName = getVersionSpecificJavaName(field, ctx);

        // doSet (int) - convert int to enum using forNumber
        buildDoSetImpl(builder, field.getDoSetMethodName(), TypeName.INT, field.getJavaName(),
                presentInVersion, m -> {
                    String protoEnumType = getProtoEnumTypeForField(field, ctx, null);
                    String enumMethod = getEnumFromIntMethod(ctx.config());
                    m.addStatement("protoBuilder.set$L($L.$L($L))",
                            versionJavaName, protoEnumType, enumMethod, field.getJavaName());
                });

        // doClear
        buildDoClearImpl(builder, field.getDoClearMethodName(), presentInVersion, versionJavaName);
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // setXxx(int)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.INT, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", field.getDoSetMethodName(), field.getJavaName())
                .addStatement("return this")
                .build());

        // clearXxx()
        addConcreteClearMethod(builder, field.getJavaName(), builderReturnType, ctx);
    }

}
