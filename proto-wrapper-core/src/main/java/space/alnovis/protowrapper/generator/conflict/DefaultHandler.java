package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;
import static space.alnovis.protowrapper.generator.TypeUtils.*;

/**
 * Default handler for fields without type conflicts.
 *
 * <p>This is the fallback handler in the {@link FieldProcessingChain} that processes
 * all fields not matched by specialized conflict handlers. It handles:</p>
 * <ul>
 *   <li>Fields with consistent types across all versions</li>
 *   <li>Fields present in only some versions (with defaults for missing versions)</li>
 *   <li>Both scalar and repeated fields</li>
 *   <li>Message and enum field types</li>
 * </ul>
 *
 * <h2>Processing Priority</h2>
 * <p>The {@code DefaultHandler} is always last in the handler chain. It returns
 * {@code true} from {@code handles()} unconditionally, serving as a catch-all.</p>
 *
 * <h2>Generated Code - Scalar Fields</h2>
 * <p>For scalar fields like {@code string name = 1}:</p>
 * <ul>
 *   <li><b>Interface:</b> {@code String getName()}, {@code boolean hasName()} (if optional),
 *       {@code Builder.setName(String)}, {@code Builder.clearName()}</li>
 *   <li><b>Abstract:</b> {@code extractName(proto)}, {@code extractHasName(proto)}</li>
 *   <li><b>Impl:</b> {@code extractName} calls {@code proto.getName()}</li>
 * </ul>
 *
 * <h2>Generated Code - Repeated Fields</h2>
 * <p>For repeated fields like {@code repeated string tags = 2}:</p>
 * <ul>
 *   <li><b>Interface:</b> {@code List<String> getTags()},
 *       {@code Builder.addTags(String)}, {@code Builder.addAllTags(List)},
 *       {@code Builder.setTags(List)}, {@code Builder.clearTags()}</li>
 *   <li><b>Abstract:</b> {@code extractTags(proto)}</li>
 *   <li><b>Impl:</b> {@code extractTags} calls {@code proto.getTagsList()}</li>
 * </ul>
 *
 * <h2>Missing Field Behavior</h2>
 * <p>When a field is not present in a specific version:</p>
 * <ul>
 *   <li>Scalar primitives return default value (0, false, etc.)</li>
 *   <li>Scalar objects return null</li>
 *   <li>Repeated fields return empty list</li>
 *   <li>{@code hasXxx()} returns false</li>
 *   <li>Builder setters throw {@code UnsupportedOperationException}</li>
 * </ul>
 *
 * <h2>Message Fields</h2>
 * <p>For message-type fields, the handler wraps the proto message in the
 * corresponding version-specific wrapper class.</p>
 *
 * <h2>Enum Fields</h2>
 * <p>For enum-type fields, the handler includes validation in builder setters
 * to ensure the enum value is valid for the target proto version.</p>
 *
 * @see ConflictHandler
 * @see FieldProcessingChain
 * @see AbstractConflictHandler
 */
public final class DefaultHandler extends AbstractConflictHandler implements ConflictHandler {

    public static final DefaultHandler INSTANCE = new DefaultHandler();

    private DefaultHandler() {
        // Singleton
    }

    @Override
    public HandlerType getHandlerType() {
        return HandlerType.DEFAULT;
    }

    @Override
    public boolean handles(MergedField field, ProcessingContext ctx) {
        // Default handler handles fields that don't match any specific conflict handler
        // It's always used as the fallback
        return true;
    }

    @Override
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add extractHas for optional fields
        addAbstractHasMethod(builder, field, ctx);

        // Add main extract method
        addAbstractExtractMethod(builder, field, returnType, ctx);
    }

    @Override
    public void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                          boolean presentInVersion, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        if (!presentInVersion) {
            addMissingFieldImplementation(builder, field, returnType, ctx);
            return;
        }

        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        // Add has method for optional fields
        addHasMethodImpl(builder, field, versionJavaName, ctx);

        // Add extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto");

        String getterCall = generateProtoGetterCall(field, ctx);
        extract.addStatement("return $L", getterCall);

        builder.addMethod(extract.build());
    }

    @Override
    public void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);

        // Add standard getter
        addStandardGetterImpl(builder, field, returnType, ctx);

        // Add has method for optional fields
        addHasMethodToAbstract(builder, field, ctx);
    }

    @Override
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.shouldSkipBuilderSetter()) {
            return;
        }

        TypeName fieldType = ctx.parseFieldType(field);

        if (field.isRepeated()) {
            TypeName singleElementType = extractListElementType(fieldType);
            addAbstractRepeatedBuilderMethods(builder, field, singleElementType, fieldType, ctx);
        } else {
            addAbstractScalarBuilderMethods(builder, field, fieldType, ctx);
        }
    }

    @Override
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                       boolean presentInVersion, ProcessingContext ctx) {
        // Skip fields with type conflicts that should be skipped
        if (field.shouldSkipBuilderSetter()) {
            return;
        }

        TypeName fieldType = ctx.parseFieldType(field);
        String capitalizedName = ctx.capitalize(field.getJavaName());
        String versionJavaName = CodeGenerationHelper.getVersionSpecificJavaName(field, ctx);

        if (field.isRepeated()) {
            TypeName singleElementType = extractListElementType(fieldType);
            addRepeatedFieldBuilderMethods(builder, field, singleElementType, fieldType,
                    presentInVersion, capitalizedName, versionJavaName, ctx);
        } else {
            addSingleFieldBuilderMethods(builder, field, fieldType, presentInVersion,
                    capitalizedName, versionJavaName, ctx);
        }
    }

    @Override
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        // Skip fields with type conflicts that should be skipped
        if (field.shouldSkipBuilderSetter()) {
            return;
        }

        TypeName fieldType = ctx.parseFieldType(field);

        if (field.isRepeated()) {
            TypeName singleElementType = extractListElementType(fieldType);
            addRepeatedConcreteBuilderMethods(builder, field, singleElementType, fieldType, builderReturnType, ctx);
        } else {
            addScalarConcreteBuilderMethods(builder, field, fieldType, builderReturnType, ctx);
        }
    }

    private void addMissingFieldImplementation(TypeSpec.Builder builder, MergedField field,
                                                TypeName returnType, ProcessingContext ctx) {
        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing extract method
        MethodSpec.Builder extract = MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto")
                .addJavadoc("Field not present in this version.\n");

        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        extract.addStatement("return $L", defaultValue);

        builder.addMethod(extract.build());
    }

    private void addSingleFieldBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                               TypeName fieldType, boolean presentInVersion,
                                               String capitalizedName, String versionJavaName,
                                               ProcessingContext ctx) {
        // doSet - use template method that throws when field not present
        buildDoSetImplOrThrow(builder, "doSet" + capitalizedName, fieldType, field.getJavaName(),
                presentInVersion, field, m -> {
                    if (field.isEnum()) {
                        CodeGenerationHelper.addEnumSetterWithValidation(m, field, versionJavaName,
                                field.getJavaName(), ctx);
                    } else {
                        String setterCall = generateProtoSetterCall(field, versionJavaName, ctx);
                        m.addStatement(setterCall, field.getJavaName());
                    }
                });

        // doClear for optional - use template method
        if (field.isOptional()) {
            buildDoClearImplForField(builder, field, presentInVersion, versionJavaName);
        }
    }

    private void addRepeatedFieldBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                 TypeName singleElementType, TypeName listType,
                                                 boolean presentInVersion,
                                                 String capitalizedName, String versionJavaName,
                                                 ProcessingContext ctx) {
        // doAdd
        MethodSpec.Builder doAdd = MethodSpec.methodBuilder("doAdd" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(singleElementType, field.getJavaName());

        addVersionConditionalOrThrow(doAdd, presentInVersion, field, m ->
                ADD_SINGLE_DISPATCHER.dispatch(m, field, versionJavaName, ctx));
        builder.addMethod(doAdd.build());

        // doAddAll
        MethodSpec.Builder doAddAll = MethodSpec.methodBuilder("doAddAll" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        addVersionConditionalOrThrow(doAddAll, presentInVersion, field, m ->
                ADD_ALL_DISPATCHER.dispatch(m, field, versionJavaName, ctx));
        builder.addMethod(doAddAll.build());

        // doSet (replace all)
        MethodSpec.Builder doSetAll = MethodSpec.methodBuilder("doSet" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(listType, field.getJavaName());

        addVersionConditionalOrThrow(doSetAll, presentInVersion, field, m -> {
            m.addStatement("protoBuilder.clear$L()", versionJavaName);
            ADD_ALL_DISPATCHER.dispatch(m, field, versionJavaName, ctx);
        });
        builder.addMethod(doSetAll.build());

        // doClear
        MethodSpec.Builder doClear = MethodSpec.methodBuilder("doClear" + capitalizedName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        addVersionConditionalClear(doClear, presentInVersion, versionJavaName);
        builder.addMethod(doClear.build());
    }

}
