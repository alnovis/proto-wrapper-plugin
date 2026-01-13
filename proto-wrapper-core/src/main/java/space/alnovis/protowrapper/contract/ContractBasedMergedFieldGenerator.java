package space.alnovis.protowrapper.contract;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.*;

/**
 * Generates field methods for multi-version merged fields based on {@link MergedFieldContract}.
 *
 * <p>This generator produces method specs for the unified wrapper API, taking into account
 * all versions of the field and their contracts.</p>
 *
 * <h2>Key Differences from ContractBasedFieldGenerator</h2>
 * <ul>
 *   <li>Works with {@link MergedFieldContract} instead of single {@link FieldContract}</li>
 *   <li>Uses unified contract for public API methods</li>
 *   <li>Generates version-aware abstract methods</li>
 *   <li>Handles version-specific presence (field may not exist in all versions)</li>
 * </ul>
 *
 * <h2>Generated Methods Structure</h2>
 * <pre>
 * Public Wrapper API (from unified contract):
 *   - getXxx()  - public final, delegates to extractXxx
 *   - hasXxx()  - public final, delegates to extractHasXxx (if hasMethodExists)
 *
 * Abstract Extract Methods (per version):
 *   - extractXxx(proto)     - protected abstract
 *   - extractHasXxx(proto)  - protected abstract (if hasMethodExists)
 *
 * Builder Methods (from unified contract):
 *   - setXxx(), clearXxx(), addXxx(), addAllXxx() - public final
 *   - doSetXxx(), doClearXxx(), etc. - protected abstract
 * </pre>
 *
 * @see MergedFieldContract
 * @see ContractBasedFieldGenerator
 */
public final class ContractBasedMergedFieldGenerator {

    private final MergedFieldContract contract;
    private final FieldMethodNames names;
    private final TypeName returnType;
    private final TypeName protoTypeName;
    private final TypeName elementType; // For repeated fields

    /**
     * Creates a generator for a merged field.
     *
     * @param contract the merged field contract
     * @param names the method names
     * @param returnType the return type for getters
     * @param protoTypeName the proto message type name
     */
    public ContractBasedMergedFieldGenerator(
            MergedFieldContract contract,
            FieldMethodNames names,
            TypeName returnType,
            TypeName protoTypeName) {
        this(contract, names, returnType, protoTypeName, null);
    }

    /**
     * Creates a generator for a merged field (singular or repeated).
     *
     * @param contract the merged field contract
     * @param names the method names
     * @param returnType the return type for getters
     * @param protoTypeName the proto message type name
     * @param elementType the element type for repeated fields (null for singular)
     */
    public ContractBasedMergedFieldGenerator(
            MergedFieldContract contract,
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

    // ==================== Public Wrapper Methods ====================

    /**
     * Generates the getter method for the abstract wrapper class.
     *
     * @return the getter method spec
     */
    public MethodSpec generateGetter() {
        FieldContract unified = contract.unified();

        MethodSpec.Builder getter = MethodSpec.methodBuilder(names.getterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(returnType);

        if (unified.getterUsesHasCheck()) {
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
     * @return the has method spec, or empty if not applicable
     */
    public Optional<MethodSpec> generateHasMethod() {
        FieldContract unified = contract.unified();

        if (!unified.hasMethodExists()) {
            return Optional.empty();
        }

        MethodSpec has = MethodSpec.methodBuilder(names.hasMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(TypeName.BOOLEAN)
                .addStatement("return $L(proto)", names.extractHasMethodName())
                .build();

        return Optional.of(has);
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
     * @return the abstract extractHas method spec, or empty if not applicable
     */
    public Optional<MethodSpec> generateAbstractExtractHasMethod() {
        FieldContract unified = contract.unified();

        if (!unified.hasMethodExists()) {
            return Optional.empty();
        }

        MethodSpec extractHas = MethodSpec.methodBuilder(names.extractHasMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addParameter(protoTypeName, "proto")
                .build();

        return Optional.of(extractHas);
    }

    // ==================== Builder Interface Methods ====================

    /**
     * Generates all builder methods for this field.
     *
     * <p>Returns empty list if builder setters should be skipped due to conflict type.</p>
     *
     * @param builderReturnType the return type for fluent builder pattern
     * @return list of builder method specs (may be empty)
     */
    public List<MethodSpec> generateBuilderMethods(TypeName builderReturnType) {
        // Skip builder setters for certain conflict types
        if (contract.shouldSkipBuilderSetter()) {
            return Collections.emptyList();
        }

        FieldContract unified = contract.unified();
        List<MethodSpec> methods = new ArrayList<>();

        if (unified.isRepeated() || unified.isMap()) {
            methods.addAll(generateRepeatedBuilderMethods(builderReturnType));
        } else {
            methods.addAll(generateSingularBuilderMethods(builderReturnType));
        }

        return methods;
    }

    private List<MethodSpec> generateSingularBuilderMethods(TypeName builderReturnType) {
        FieldContract unified = contract.unified();
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
        if (unified.nullable()) {
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

    // ==================== Abstract Builder Methods ====================

    /**
     * Generates abstract builder method declarations.
     *
     * <p>Returns empty list if builder setters should be skipped due to conflict type.</p>
     *
     * @return list of abstract method specs (may be empty)
     */
    public List<MethodSpec> generateAbstractBuilderMethods() {
        if (contract.shouldSkipBuilderSetter()) {
            return Collections.emptyList();
        }

        FieldContract unified = contract.unified();
        List<MethodSpec> methods = new ArrayList<>();

        if (unified.isRepeated() || unified.isMap()) {
            methods.addAll(generateAbstractRepeatedBuilderMethods());
        } else {
            methods.addAll(generateAbstractSingularBuilderMethods());
        }

        return methods;
    }

    private List<MethodSpec> generateAbstractSingularBuilderMethods() {
        FieldContract unified = contract.unified();
        List<MethodSpec> methods = new ArrayList<>();

        // doSet
        methods.add(MethodSpec.methodBuilder(names.doSetMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .addParameter(returnType, names.javaName())
                .build());

        // doClear (for nullable fields)
        if (unified.nullable()) {
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

    // ==================== Version-Specific Support ====================

    /**
     * Check if field is present in a specific version.
     *
     * @param version the version to check
     * @return true if field exists in that version
     */
    public boolean isPresentIn(String version) {
        return contract.isPresentIn(version);
    }

    /**
     * Check if has*() method is available in a specific version.
     *
     * <p>This is useful for generating version-specific extract implementations
     * where some versions may have has*() and others may not.</p>
     *
     * @param version the version to check
     * @return true if has*() is available in that version
     */
    public boolean hasMethodAvailableIn(String version) {
        return contract.hasMethodAvailableIn(version);
    }

    /**
     * Get the set of versions where this field is present.
     *
     * @return set of version identifiers
     */
    public Set<String> presentInVersions() {
        return contract.presentInVersions();
    }

    // ==================== Utilities ====================

    private static TypeName extractElementType(TypeName listType) {
        if (listType instanceof ParameterizedTypeName paramType) {
            if (!paramType.typeArguments.isEmpty()) {
                return paramType.typeArguments.get(0);
            }
        }
        return ClassName.get(Object.class);
    }

    /**
     * @return the merged field contract
     */
    public MergedFieldContract contract() {
        return contract;
    }

    /**
     * @return the unified contract (shortcut for contract().unified())
     */
    public FieldContract unifiedContract() {
        return contract.unified();
    }

    /**
     * @return the method names
     */
    public FieldMethodNames names() {
        return names;
    }
}
