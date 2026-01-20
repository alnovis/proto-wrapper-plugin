package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.alnovis.protowrapper.model.MergedField;

import javax.lang.model.element.Modifier;

/**
 * Factory for creating pre-configured MethodSpec builders.
 *
 * <p>This factory eliminates repetitive boilerplate when creating method specifications
 * by providing pre-configured builders for common method patterns used in code generation.</p>
 *
 * <h2>Supported Method Patterns</h2>
 * <table>
 *   <caption>Method Patterns</caption>
 *   <tr><th>Pattern</th><th>Modifiers</th><th>Annotation</th><th>Example</th></tr>
 *   <tr><td>Protected Extract</td><td>protected</td><td>@Override</td><td>extractName(proto)</td></tr>
 *   <tr><td>Protected Abstract</td><td>protected abstract</td><td>-</td><td>extractName(proto)</td></tr>
 *   <tr><td>Public Final Getter</td><td>public final</td><td>@Override</td><td>getName()</td></tr>
 *   <tr><td>Public Final Builder</td><td>public final</td><td>@Override</td><td>setName(value)</td></tr>
 *   <tr><td>Protected Impl</td><td>protected</td><td>@Override</td><td>doSetName(value)</td></tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Instead of:
 * MethodSpec.Builder method = MethodSpec.methodBuilder(field.getExtractMethodName())
 *     .addAnnotation(Override.class)
 *     .addModifiers(Modifier.PROTECTED)
 *     .returns(returnType)
 *     .addParameter(ctx.protoClassName(), "proto");
 *
 * // Use:
 * MethodSpec.Builder method = MethodSpecFactory.protectedExtract(field, returnType, ctx);
 * }</pre>
 *
 * @since 1.6.5
 * @see ExtractMethodGenerator
 * @see BuilderMethodGenerator
 */
public final class MethodSpecFactory {

    private MethodSpecFactory() {
        // Utility class - no instantiation
    }

    // ========== Extract Method Patterns ==========

    /**
     * Create a protected extract method builder with @Override annotation.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * protected ReturnType extractXxx(ProtoType proto) { ... }
     * }</pre>
     *
     * @param field the merged field definition
     * @param returnType the return type for the method
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedExtract(MergedField field, TypeName returnType,
                                                       ProcessingContext ctx) {
        return MethodSpec.methodBuilder(field.getExtractMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto");
    }

    /**
     * Create a protected extract method builder with custom method name.
     *
     * @param methodName the method name
     * @param returnType the return type for the method
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedExtract(String methodName, TypeName returnType,
                                                       ProcessingContext ctx) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addParameter(ctx.protoClassName(), "proto");
    }

    /**
     * Create a protected abstract extract method builder (no @Override).
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * protected abstract ReturnType extractXxx(ProtoType proto);
     * }</pre>
     *
     * @param field the merged field definition
     * @param returnType the return type for the method
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstractExtract(MergedField field, TypeName returnType,
                                                               ProcessingContext ctx) {
        return MethodSpec.methodBuilder(field.getExtractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(ctx.protoType(), "proto");
    }

    /**
     * Create a protected abstract method builder with custom name.
     *
     * @param methodName the method name
     * @param returnType the return type for the method
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstract(String methodName, TypeName returnType,
                                                        ProcessingContext ctx) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(ctx.protoType(), "proto");
    }

    /**
     * Create a protected abstract method builder without parameters.
     *
     * @param methodName the method name
     * @param returnType the return type for the method
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstractNoParam(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType);
    }

    // ========== Has Method Patterns ==========

    /**
     * Create a protected extractHas method builder.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * protected boolean extractHasXxx(ProtoType proto) { ... }
     * }</pre>
     *
     * @param field the merged field definition
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedExtractHas(MergedField field, ProcessingContext ctx) {
        return MethodSpec.methodBuilder(field.getExtractHasMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addParameter(ctx.protoClassName(), "proto");
    }

    /**
     * Create a protected abstract extractHas method builder.
     *
     * @param field the merged field definition
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstractExtractHas(MergedField field, ProcessingContext ctx) {
        return MethodSpec.methodBuilder(field.getExtractHasMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(ctx.protoType(), "proto");
    }

    // ========== Getter Method Patterns ==========

    /**
     * Create a public final getter method builder.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * public final ReturnType getXxx() { ... }
     * }</pre>
     *
     * @param field the merged field definition
     * @param returnType the return type for the method
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicFinalGetter(MergedField field, TypeName returnType) {
        return MethodSpec.methodBuilder(field.getGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);
    }

    /**
     * Create a public final getter method builder with custom name.
     *
     * @param methodName the method name
     * @param returnType the return type for the method
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicFinalGetter(String methodName, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);
    }

    /**
     * Create a public final has method builder.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * public final boolean hasXxx() { ... }
     * }</pre>
     *
     * @param methodName the method name (e.g., "hasName")
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicFinalHas(String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(TypeName.BOOLEAN);
    }

    // ========== Builder Method Patterns ==========

    /**
     * Create a public final builder setter method.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * public final Builder setXxx(ParamType value) { ... }
     * }</pre>
     *
     * @param methodName the method name (e.g., "setName")
     * @param paramType the parameter type
     * @param paramName the parameter name
     * @param builderReturnType the builder return type
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicFinalBuilderSetter(String methodName, TypeName paramType,
                                                               String paramName, TypeName builderReturnType) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(paramType, paramName)
                .returns(builderReturnType);
    }

    /**
     * Create a public final builder method without parameters.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * public final Builder clearXxx() { ... }
     * }</pre>
     *
     * @param methodName the method name (e.g., "clearName")
     * @param builderReturnType the builder return type
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicFinalBuilderNoParam(String methodName, TypeName builderReturnType) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderReturnType);
    }

    /**
     * Create a public abstract builder method.
     *
     * @param methodName the method name
     * @param paramType the parameter type
     * @param paramName the parameter name
     * @param builderReturnType the builder return type
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicAbstractBuilder(String methodName, TypeName paramType,
                                                            String paramName, TypeName builderReturnType) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(paramType, paramName)
                .returns(builderReturnType);
    }

    /**
     * Create a public abstract builder method without parameters.
     *
     * @param methodName the method name
     * @param builderReturnType the builder return type
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder publicAbstractBuilderNoParam(String methodName, TypeName builderReturnType) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(builderReturnType);
    }

    // ========== Impl (doXxx) Method Patterns ==========

    /**
     * Create a protected doXxx implementation method.
     *
     * <p>Generated pattern:</p>
     * <pre>{@code
     * @Override
     * protected void doSetXxx(ParamType value) { ... }
     * }</pre>
     *
     * @param methodName the method name (e.g., "doSetName")
     * @param paramType the parameter type
     * @param paramName the parameter name
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedDoImpl(String methodName, TypeName paramType, String paramName) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(paramType, paramName);
    }

    /**
     * Create a protected doXxx implementation method without parameters.
     *
     * @param methodName the method name (e.g., "doClearName")
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedDoImplNoParam(String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);
    }

    /**
     * Create a protected abstract doXxx method.
     *
     * @param methodName the method name
     * @param paramType the parameter type
     * @param paramName the parameter name
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstractDo(String methodName, TypeName paramType, String paramName) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(paramType, paramName);
    }

    /**
     * Create a protected abstract doXxx method without parameters.
     *
     * @param methodName the method name
     * @return configured MethodSpec.Builder
     */
    public static MethodSpec.Builder protectedAbstractDoNoParam(String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT);
    }

    // ========== Convenience Methods ==========

    /**
     * Create capitalized method name from field name.
     *
     * @param prefix the method prefix (e.g., "set", "get", "has")
     * @param fieldName the field name
     * @param ctx the processing context
     * @return the method name (e.g., "setUserName")
     */
    public static String methodName(String prefix, String fieldName, ProcessingContext ctx) {
        return prefix + ctx.capitalize(fieldName);
    }

    /**
     * Create capitalized method name from field.
     *
     * @param prefix the method prefix
     * @param field the merged field
     * @param ctx the processing context
     * @return the method name
     */
    public static String methodName(String prefix, MergedField field, ProcessingContext ctx) {
        return prefix + ctx.capitalize(field.getJavaName());
    }
}
