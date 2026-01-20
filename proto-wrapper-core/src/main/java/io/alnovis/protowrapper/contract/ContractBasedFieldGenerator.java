package io.alnovis.protowrapper.contract;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates field methods based on {@link FieldContract}.
 *
 * <p>This is the <b>contract-driven generator</b> that produces method specs
 * purely based on the field contract, without any conditional logic scattered
 * across multiple handlers.</p>
 *
 * <h2>Generated Methods</h2>
 * <ul>
 *   <li><b>Abstract class:</b> getters, has-methods (delegating to extract methods)</li>
 *   <li><b>Implementation class:</b> extract methods (version-specific)</li>
 *   <li><b>Builder interface:</b> setters, adders, clearers</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldContract contract = FieldContract.from(fieldInfo, syntax);
 * FieldMethodNames names = FieldMethodNames.from(fieldInfo.getJavaName());
 * TypeName returnType = resolver.resolveType(fieldInfo);
 *
 * ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
 *     contract, names, returnType, protoTypeName);
 *
 * // Generate getter for abstract class
 * MethodSpec getter = generator.generateGetter();
 *
 * // Generate has method (if applicable)
 * generator.generateHasMethod().ifPresent(builder::addMethod);
 * }</pre>
 *
 * @see FieldContract
 * @see FieldMethodNames
 */
public final class ContractBasedFieldGenerator {

    private final FieldContract contract;
    private final FieldMethodNames names;
    private final TypeName returnType;
    private final TypeName protoTypeName;

    // For repeated fields
    private final TypeName elementType;

    /**
     * Creates a generator for a singular field.
     *
     * @param contract the field contract
     * @param names the method names
     * @param returnType the return type for getters
     * @param protoTypeName the proto message type name (for extract method parameter)
     */
    public ContractBasedFieldGenerator(
            FieldContract contract,
            FieldMethodNames names,
            TypeName returnType,
            TypeName protoTypeName) {
        this(contract, names, returnType, protoTypeName, null);
    }

    /**
     * Creates a generator for a field (singular or repeated).
     *
     * @param contract the field contract
     * @param names the method names
     * @param returnType the return type for getters (List<T> for repeated)
     * @param protoTypeName the proto message type name
     * @param elementType the element type for repeated fields (null for singular)
     */
    public ContractBasedFieldGenerator(
            FieldContract contract,
            FieldMethodNames names,
            TypeName returnType,
            TypeName protoTypeName,
            TypeName elementType) {
        this.contract = Objects.requireNonNull(contract, "contract must not be null");
        this.names = Objects.requireNonNull(names, "names must not be null");
        this.returnType = Objects.requireNonNull(returnType, "returnType must not be null");
        this.protoTypeName = Objects.requireNonNull(protoTypeName, "protoTypeName must not be null");
        this.elementType = elementType;
    }

    // ==================== Abstract Class Methods ====================

    /**
     * Generates the getter method for the abstract wrapper class.
     *
     * <p>The generated method delegates to extract methods:</p>
     * <ul>
     *   <li>If {@code getterUsesHasCheck}: {@code return extractHas(proto) ? extract(proto) : null}</li>
     *   <li>Otherwise: {@code return extract(proto)}</li>
     * </ul>
     *
     * @return the getter method spec
     */
    public MethodSpec generateGetter() {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(names.getterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);

        if (contract.getterUsesHasCheck()) {
            getter.addStatement("return $L(proto) ? $L(proto) : null",
                    names.extractHasMethodName(), names.extractMethodName());
        } else {
            getter.addStatement("return $L(proto)", names.extractMethodName());
        }

        return getter.build();
    }

    /**
     * Generates the has method for the abstract wrapper class.
     *
     * <p>The has method is only generated if {@code hasMethodExists} is true
     * in the contract.</p>
     *
     * @return the has method spec, or empty if not applicable
     */
    public java.util.Optional<MethodSpec> generateHasMethod() {
        if (!contract.hasMethodExists()) {
            return java.util.Optional.empty();
        }

        MethodSpec has = MethodSpec.methodBuilder(names.hasMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L(proto)", names.extractHasMethodName())
                .build();

        return java.util.Optional.of(has);
    }

    // ==================== Abstract Extract Methods ====================

    /**
     * Generates the abstract extract method declaration.
     *
     * @return the abstract extract method spec
     */
    public MethodSpec generateAbstractExtractMethod() {
        return MethodSpec.methodBuilder(names.extractMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(protoTypeName, "proto")
                .build();
    }

    /**
     * Generates the abstract extractHas method declaration.
     *
     * <p>Only generated if {@code hasMethodExists} is true.</p>
     *
     * @return the abstract extractHas method spec, or empty if not applicable
     */
    public java.util.Optional<MethodSpec> generateAbstractExtractHasMethod() {
        if (!contract.hasMethodExists()) {
            return java.util.Optional.empty();
        }

        MethodSpec extractHas = MethodSpec.methodBuilder(names.extractHasMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(protoTypeName, "proto")
                .build();

        return java.util.Optional.of(extractHas);
    }

    // ==================== Builder Interface Methods ====================

    /**
     * Generates all builder methods for this field.
     *
     * @param builderReturnType the return type for fluent builder pattern
     * @return list of builder method specs
     */
    public List<MethodSpec> generateBuilderMethods(TypeName builderReturnType) {
        List<MethodSpec> methods = new ArrayList<>();

        if (contract.isRepeated() || contract.isMap()) {
            methods.addAll(generateRepeatedBuilderMethods(builderReturnType));
        } else {
            methods.addAll(generateSingularBuilderMethods(builderReturnType));
        }

        return methods;
    }

    private List<MethodSpec> generateSingularBuilderMethods(TypeName builderReturnType) {
        List<MethodSpec> methods = new ArrayList<>();

        // set method
        methods.add(MethodSpec.methodBuilder(names.setterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(returnType, names.javaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", names.doSetMethodName(), names.javaName())
                .addStatement("return this")
                .build());

        // clear method (for nullable fields)
        if (contract.nullable()) {
            methods.add(MethodSpec.methodBuilder(names.clearMethodName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(builderReturnType)
                    .addStatement("$L()", names.doClearMethodName())
                    .addStatement("return this")
                    .build());
        }

        return methods;
    }

    private List<MethodSpec> generateRepeatedBuilderMethods(TypeName builderReturnType) {
        List<MethodSpec> methods = new ArrayList<>();
        TypeName elemType = elementType != null ? elementType : extractElementType(returnType);

        // add single
        methods.add(MethodSpec.methodBuilder(names.addMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(elemType, names.javaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", names.doAddMethodName(), names.javaName())
                .addStatement("return this")
                .build());

        // addAll
        methods.add(MethodSpec.methodBuilder(names.addAllMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(returnType, names.javaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", names.doAddAllMethodName(), names.javaName())
                .addStatement("return this")
                .build());

        // set (replace all)
        methods.add(MethodSpec.methodBuilder(names.setterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(returnType, names.javaName())
                .returns(builderReturnType)
                .addStatement("$L($L)", names.doSetMethodName(), names.javaName())
                .addStatement("return this")
                .build());

        // clear
        methods.add(MethodSpec.methodBuilder(names.clearMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderReturnType)
                .addStatement("$L()", names.doClearMethodName())
                .addStatement("return this")
                .build());

        return methods;
    }

    // ==================== Abstract Builder Methods (doXxx) ====================

    /**
     * Generates abstract builder method declarations.
     *
     * @return list of abstract method specs
     */
    public List<MethodSpec> generateAbstractBuilderMethods() {
        List<MethodSpec> methods = new ArrayList<>();

        if (contract.isRepeated() || contract.isMap()) {
            methods.addAll(generateAbstractRepeatedBuilderMethods());
        } else {
            methods.addAll(generateAbstractSingularBuilderMethods());
        }

        return methods;
    }

    private List<MethodSpec> generateAbstractSingularBuilderMethods() {
        List<MethodSpec> methods = new ArrayList<>();

        // doSet
        methods.add(MethodSpec.methodBuilder(names.doSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(returnType, names.javaName())
                .build());

        // doClear (for nullable fields)
        if (contract.nullable()) {
            methods.add(MethodSpec.methodBuilder(names.doClearMethodName())
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .build());
        }

        return methods;
    }

    private List<MethodSpec> generateAbstractRepeatedBuilderMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        TypeName elemType = elementType != null ? elementType : extractElementType(returnType);

        // doAdd
        methods.add(MethodSpec.methodBuilder(names.doAddMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(elemType, names.javaName())
                .build());

        // doAddAll
        methods.add(MethodSpec.methodBuilder(names.doAddAllMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(returnType, names.javaName())
                .build());

        // doSet
        methods.add(MethodSpec.methodBuilder(names.doSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(returnType, names.javaName())
                .build());

        // doClear
        methods.add(MethodSpec.methodBuilder(names.doClearMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build());

        return methods;
    }

    // ==================== Utilities ====================

    private static TypeName extractElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName paramType) {
            if (!paramType.typeArguments.isEmpty()) {
                return paramType.typeArguments.get(0);
            }
        }
        // Fallback - shouldn't happen with proper usage
        return ClassName.get(Object.class);
    }

    /**
     * @return the contract used by this generator
     */
    public FieldContract contract() {
        return contract;
    }

    /**
     * @return the method names used by this generator
     */
    public FieldMethodNames names() {
        return names;
    }
}
