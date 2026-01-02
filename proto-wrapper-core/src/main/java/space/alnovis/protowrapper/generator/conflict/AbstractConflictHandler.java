package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;

import static space.alnovis.protowrapper.generator.TypeUtils.*;
import static space.alnovis.protowrapper.generator.conflict.CodeGenerationHelper.*;

import javax.lang.model.element.Modifier;
import java.util.function.Consumer;

/**
 * Sealed base class providing common functionality for conflict handlers.
 *
 * <p>Contains utility methods for generating common method patterns
 * and extracting field information.</p>
 *
 * <p>This is a sealed class that permits only the known handler implementations,
 * ensuring type safety and preventing accidental external extensions.</p>
 */
public abstract sealed class AbstractConflictHandler permits
        IntEnumHandler, EnumEnumHandler, StringBytesHandler, WideningHandler, FloatDoubleHandler,
        SignedUnsignedHandler, RepeatedSingleHandler, PrimitiveMessageHandler,
        RepeatedConflictHandler, MapFieldHandler, WellKnownTypeHandler, RepeatedWellKnownTypeHandler,
        DefaultHandler {

    /**
     * Add an abstract extractHas method for optional fields.
     */
    protected void addAbstractHasMethod(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoType(), "proto")
                    .build());
        }
    }

    /**
     * Add an abstract extract method.
     */
    protected void addAbstractExtractMethod(TypeSpec.Builder builder, MergedField field,
                                             TypeName returnType, ProcessingContext ctx) {
        builder.addMethod(MethodSpec.methodBuilder(field.getExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(ctx.protoType(), "proto")
                .build());
    }

    /**
     * Add a concrete has method implementation for present fields.
     */
    protected void addHasMethodImpl(TypeSpec.Builder builder, MergedField field,
                                     String versionJavaName, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addStatement("return proto.has$L()", versionJavaName)
                    .build());
        }
    }

    /**
     * Add a concrete has method implementation returning false (field not present).
     */
    protected void addMissingHasMethodImpl(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            builder.addMethod(MethodSpec.methodBuilder(field.getExtractHasMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(TypeName.BOOLEAN)
                    .addParameter(ctx.protoClassName(), "proto")
                    .addStatement("return false")
                    .addJavadoc("Field not present in this version.\n")
                    .build());
        }
    }

    /**
     * Add a getter implementation that delegates to extract methods.
     */
    protected void addStandardGetterImpl(TypeSpec.Builder builder, MergedField field,
                                          TypeName returnType, ProcessingContext ctx) {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);

        if (field.needsHasCheck()) {
            getter.addStatement("return $L(proto) ? $L(proto) : null",
                    field.getExtractHasMethodName(), field.getExtractMethodName());
        } else {
            getter.addStatement("return $L(proto)", field.getExtractMethodName());
        }

        builder.addMethod(getter.build());
    }

    /**
     * Add has method implementation to abstract class.
     */
    protected void addHasMethodToAbstract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.isOptional() && !field.isRepeated()) {
            MethodSpec has = MethodSpec.methodBuilder("has" + ctx.capitalize(field.getJavaName()))
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.BOOLEAN)
                    .addStatement("return $L(proto)", field.getExtractHasMethodName())
                    .build();
            builder.addMethod(has);
        }
    }

    /**
     * Get the field type for the current version.
     */
    protected FieldInfo getVersionField(MergedField field, ProcessingContext ctx) {
        String version = ctx.version();
        return version != null ? field.getVersionFields().get(version) : null;
    }

    // ========== Concrete Builder Methods (setXxx delegating to doSetXxx) ==========

    /**
     * Add standard concrete builder methods for a repeated field.
     * Generates: addXxx, addAllXxx, setXxx (replace all), clearXxx
     */
    protected void addRepeatedConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                      TypeName singleElementType, TypeName listType,
                                                      TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // add single
        builder.addMethod(MethodSpec.methodBuilder("add" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(singleElementType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doAdd$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // addAll
        builder.addMethod(MethodSpec.methodBuilder("addAll" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(listType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doAddAll$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // set (replace all)
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(listType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // clear
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderReturnType)
                .addStatement("doClear$L()", capName)
                .addStatement("return this")
                .build());
    }

    /**
     * Add standard concrete builder methods for a scalar field.
     * Generates: setXxx, clearXxx (if optional)
     */
    protected void addScalarConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                    TypeName fieldType, TypeName builderReturnType,
                                                    ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // set
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(fieldType, field.getJavaName())
                .returns(builderReturnType)
                .addStatement("doSet$L($L)", capName, field.getJavaName())
                .addStatement("return this")
                .build());

        // clear for optional
        if (field.isOptional()) {
            builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(builderReturnType)
                    .addStatement("doClear$L()", capName)
                    .addStatement("return this")
                    .build());
        }
    }

    // ========== Template Methods for Builder Impl (doXxx) ==========

    /**
     * Build a doSet implementation method with version-conditional body.
     * Uses silent ignore when field not present (adds comment).
     *
     * @param builder TypeSpec builder to add method to
     * @param methodName Full method name (e.g., "doSetName")
     * @param paramType Parameter type
     * @param paramName Parameter name
     * @param presentInVersion Whether field is present in current version
     * @param bodyBuilder Consumer that builds the method body when field is present
     */
    protected void buildDoSetImpl(TypeSpec.Builder builder, String methodName,
                                   TypeName paramType, String paramName,
                                   boolean presentInVersion,
                                   Consumer<MethodSpec.Builder> bodyBuilder) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(paramType, paramName);

        addVersionConditional(method, presentInVersion, bodyBuilder);
        builder.addMethod(method.build());
    }

    /**
     * Build a doSet implementation method that throws when field not present.
     * Use this for setter operations where silently ignoring would be confusing.
     */
    protected void buildDoSetImplOrThrow(TypeSpec.Builder builder, String methodName,
                                          TypeName paramType, String paramName,
                                          boolean presentInVersion, MergedField field,
                                          Consumer<MethodSpec.Builder> bodyBuilder) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(paramType, paramName);

        addVersionConditionalOrThrow(method, presentInVersion, field, bodyBuilder);
        builder.addMethod(method.build());
    }

    /**
     * Build a doSet implementation with simple protoBuilder.setXxx call.
     */
    protected void buildSimpleDoSetImpl(TypeSpec.Builder builder, String methodName,
                                         TypeName paramType, String paramName,
                                         boolean presentInVersion, String versionJavaName) {
        buildDoSetImpl(builder, methodName, paramType, paramName, presentInVersion,
                m -> m.addStatement("protoBuilder.set$L($L)", versionJavaName, paramName));
    }

    /**
     * Build a doClear implementation method with version-conditional clear.
     */
    protected void buildDoClearImpl(TypeSpec.Builder builder, String methodName,
                                     boolean presentInVersion, String versionJavaName) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        addVersionConditionalClear(method, presentInVersion, versionJavaName);
        builder.addMethod(method.build());
    }

    /**
     * Build a doClear implementation for a field using field's method name.
     */
    protected void buildDoClearImplForField(TypeSpec.Builder builder, MergedField field,
                                             boolean presentInVersion, String versionJavaName) {
        buildDoClearImpl(builder, field.getDoClearMethodName(), presentInVersion, versionJavaName);
    }

    // ========== Template Methods for Abstract Builder Methods ==========

    /**
     * Add an abstract doSet method to the builder.
     */
    protected void addAbstractDoSet(TypeSpec.Builder builder, String methodName, TypeName paramType, String paramName) {
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(paramType, paramName)
                .build());
    }

    /**
     * Add an abstract doClear method to the builder.
     */
    protected void addAbstractDoClear(TypeSpec.Builder builder, String methodName) {
        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build());
    }

    /**
     * Add abstract doSet and optionally doClear for a scalar field.
     */
    protected void addAbstractScalarBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                    TypeName fieldType, ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // doSet
        addAbstractDoSet(builder, "doSet" + capName, fieldType, field.getJavaName());

        // doClear for optional
        if (field.isOptional()) {
            addAbstractDoClear(builder, "doClear" + capName);
        }
    }

    /**
     * Add abstract doAdd, doAddAll, doSet, doClear for a repeated field.
     */
    protected void addAbstractRepeatedBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                                      TypeName singleElementType, TypeName listType,
                                                      ProcessingContext ctx) {
        String capName = ctx.capitalize(field.getJavaName());

        // doAdd
        addAbstractDoSet(builder, "doAdd" + capName, singleElementType, field.getJavaName());

        // doAddAll
        addAbstractDoSet(builder, "doAddAll" + capName, listType, field.getJavaName());

        // doSet (replace all)
        addAbstractDoSet(builder, "doSet" + capName, listType, field.getJavaName());

        // doClear
        addAbstractDoClear(builder, "doClear" + capName);
    }

    // ========== Template Methods for Concrete Builder Methods ==========

    /**
     * Build a concrete setXxx method that delegates to doSetXxx.
     */
    protected void addConcreteSetMethod(TypeSpec.Builder builder, String fieldName,
                                         TypeName paramType, TypeName builderReturnType,
                                         ProcessingContext ctx) {
        String capName = ctx.capitalize(fieldName);
        builder.addMethod(MethodSpec.methodBuilder("set" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(paramType, fieldName)
                .returns(builderReturnType)
                .addStatement("doSet$L($L)", capName, fieldName)
                .addStatement("return this")
                .build());
    }

    /**
     * Build a concrete clearXxx method that delegates to doClearXxx.
     */
    protected void addConcreteClearMethod(TypeSpec.Builder builder, String fieldName,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        String capName = ctx.capitalize(fieldName);
        builder.addMethod(MethodSpec.methodBuilder("clear" + capName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderReturnType)
                .addStatement("doClear$L()", capName)
                .addStatement("return this")
                .build());
    }
}
