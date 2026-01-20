package io.alnovis.protowrapper.generator.oneof;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.generator.TypeResolver;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedOneof;

import javax.lang.model.element.Modifier;

/**
 * Consolidated generator for all oneof-related code.
 *
 * <p>This class handles generation of:
 * <ul>
 *   <li>Case enum (nested in interface)</li>
 *   <li>Interface methods (getCaseEnumName, hasXxx for oneof fields)</li>
 *   <li>Abstract class methods (extractXxxCase, getXxxCase, doClearXxx, clearXxx)</li>
 *   <li>Impl class methods (extractXxxCase implementation, doClearXxx implementation)</li>
 * </ul>
 */
public class OneofGenerator {

    private final GeneratorConfig config;

    /**
     * Create a new OneofGenerator.
     *
     * @param config the generator configuration
     */
    public OneofGenerator(GeneratorConfig config) {
        this.config = config;
    }

    // ==================== Case Enum Generation ====================

    /**
     * Generate a Case enum TypeSpec to be nested inside an interface.
     *
     * <p>Example output for oneof "payment_method":</p>
     * <pre>
     * public enum PaymentMethodCase {
     *     CREDIT_CARD(2),
     *     BANK_TRANSFER(3),
     *     PAYMENT_METHOD_NOT_SET(0);
     *
     *     private final int number;
     *     PaymentMethodCase(int number) { this.number = number; }
     *     public int getNumber() { return number; }
     *     public static PaymentMethodCase forNumber(int number) { ... }
     * }
     * </pre>
     *
     * @param oneof the merged oneof group
     * @return the generated enum TypeSpec
     */
    public TypeSpec generateCaseEnum(MergedOneof oneof) {
        String enumName = oneof.getCaseEnumName();

        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Case enum for oneof '$L'.\n\n", oneof.getProtoName())
                .addJavadoc("<p>Indicates which field in the oneof is currently set.</p>\n");

        // Add number field
        enumBuilder.addField(TypeName.INT, "number", Modifier.PRIVATE, Modifier.FINAL);

        // Add constructor
        enumBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "number")
                .addStatement("this.number = number")
                .build());

        // Add getNumber() method
        enumBuilder.addMethod(MethodSpec.methodBuilder("getNumber")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return number")
                .build());

        // Add enum constants for each field in the oneof
        for (MergedOneof.CaseConstant constant : oneof.getCaseConstants()) {
            TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder("$L", constant.fieldNumber());

            if (!constant.isNotSet()) {
                MergedField field = constant.field();
                if (field != null && !field.getPresentInVersions().containsAll(oneof.getPresentInVersions())) {
                    constantBuilder.addJavadoc("Field '$L' - present in versions: $L\n",
                            field.getName(), field.getPresentInVersions());
                }
            }

            enumBuilder.addEnumConstant(constant.name(), constantBuilder.build());
        }

        // Add static forNumber() method
        ClassName enumClassName = ClassName.get("", enumName);
        enumBuilder.addMethod(MethodSpec.methodBuilder("forNumber")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(TypeName.INT, "number")
                .beginControlFlow("for ($T c : values())", enumClassName)
                .beginControlFlow("if (c.number == number)")
                .addStatement("return c")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $L", oneof.getNotSetConstantName())
                .addJavadoc("Get the case constant for a field number.\n")
                .addJavadoc("@param number the proto field number\n")
                .addJavadoc("@return the corresponding case, or $L if not found\n", oneof.getNotSetConstantName())
                .build());

        return enumBuilder.build();
    }

    // ==================== Interface Methods ====================

    /**
     * Generate discriminator method for interface.
     * E.g., PaymentMethodCase getPaymentMethodCase()
     *
     * @param oneof the merged oneof group
     * @param message the merged message containing the oneof
     * @return the generated method spec
     */
    public MethodSpec generateInterfaceCaseGetter(MergedOneof oneof, MergedMessage message) {
        ClassName caseEnumType = ClassName.get("", oneof.getCaseEnumName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(oneof.getCaseGetterName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(caseEnumType)
                .addJavadoc("Get which field in the '$L' oneof is currently set.\n", oneof.getProtoName());

        // Add version info if oneof is not universal
        if (!oneof.isUniversal(message.getPresentInVersions())) {
            builder.addJavadoc("<p>This oneof is present in versions: $L</p>\n", oneof.getPresentInVersions());
            builder.addJavadoc("<p>Returns $L for versions without this oneof.</p>\n", oneof.getNotSetConstantName());
        }

        builder.addJavadoc("@return The case enum indicating which field is set, or $L if none\n",
                oneof.getNotSetConstantName());

        return builder.build();
    }

    /**
     * Generate hasXxx() method for a oneof field in interface.
     *
     * @param field the merged field
     * @param resolver the type resolver
     * @return the generated method spec
     */
    public MethodSpec generateInterfaceFieldHasMethod(MergedField field, TypeResolver resolver) {
        String methodName = "has" + resolver.capitalize(field.getJavaName());

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if '$L' is the currently set field in its oneof group.\n", field.getName())
                .addJavadoc("@return true if this field is set in the oneof\n")
                .build();
    }

    // ==================== Abstract Class Methods ====================

    /**
     * Add abstract extractXxxCase() method to abstract class.
     *
     * @param oneof the merged oneof group
     * @param message the merged message
     * @param protoType the proto type variable name
     * @return the generated method spec
     */
    public MethodSpec generateAbstractExtractCaseMethod(MergedOneof oneof, MergedMessage message,
                                                          TypeVariableName protoType) {
        ClassName interfaceType = ClassName.get(config.getApiPackage(), message.getInterfaceName());
        ClassName caseEnumType = interfaceType.nestedClass(oneof.getCaseEnumName());

        return MethodSpec.methodBuilder(oneof.getExtractCaseMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(caseEnumType)
                .addParameter(protoType, "proto")
                .build();
    }

    /**
     * Add concrete getXxxCase() method that delegates to extractXxxCase.
     *
     * @param oneof the merged oneof group
     * @param message the merged message
     * @return the generated method spec
     */
    public MethodSpec generateAbstractCaseGetter(MergedOneof oneof, MergedMessage message) {
        ClassName interfaceType = ClassName.get(config.getApiPackage(), message.getInterfaceName());
        ClassName caseEnumType = interfaceType.nestedClass(oneof.getCaseEnumName());

        return MethodSpec.methodBuilder(oneof.getCaseGetterName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(caseEnumType)
                .addStatement("return $L(proto)", oneof.getExtractCaseMethodName())
                .build();
    }

    /**
     * Add abstract doClearXxx() method to AbstractBuilder.
     *
     * @param oneof the merged oneof group
     * @return the generated method spec
     */
    public MethodSpec generateAbstractBuilderDoClear(MergedOneof oneof) {
        return MethodSpec.methodBuilder(oneof.getDoClearMethodName())
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .build();
    }

    /**
     * Add concrete clearXxx() method to AbstractBuilder.
     *
     * @param oneof the merged oneof group
     * @param builderInterfaceType the builder interface class name
     * @return the generated method spec
     */
    public MethodSpec generateAbstractBuilderClear(MergedOneof oneof, ClassName builderInterfaceType) {
        return MethodSpec.methodBuilder(oneof.getClearMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builderInterfaceType)
                .addStatement("$L()", oneof.getDoClearMethodName())
                .addStatement("return this")
                .build();
    }

    // ==================== Impl Class Methods ====================

    /**
     * Generate extractXxxCase() implementation for version-specific impl class.
     *
     * @param oneof the merged oneof group
     * @param message the merged message
     * @param protoType the proto class name
     * @param version the version identifier
     * @param apiPackage the API package name
     * @return the generated method spec
     */
    public MethodSpec generateImplExtractCaseMethod(MergedOneof oneof, MergedMessage message,
                                                      ClassName protoType, String version, String apiPackage) {
        boolean oneofPresentInVersion = oneof.getPresentInVersions().contains(version);

        ClassName interfaceType = ClassName.get(apiPackage, message.getInterfaceName());
        ClassName caseEnumType = interfaceType.nestedClass(oneof.getCaseEnumName());

        MethodSpec.Builder extractCase = MethodSpec.methodBuilder(oneof.getExtractCaseMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(caseEnumType)
                .addParameter(protoType, "proto");

        if (oneofPresentInVersion) {
            // Get proto's case and map to our enum
            String protoGetCaseMethod = "get" + oneof.getJavaName() + "Case";
            extractCase.addStatement("$T protoCase = proto.$L()", Object.class, protoGetCaseMethod);

            // Switch on proto case and return our enum
            extractCase.beginControlFlow("return switch (protoCase.toString())");

            for (MergedField field : oneof.getFields()) {
                if (field.getPresentInVersions().contains(version)) {
                    String constantName = toScreamingSnakeCase(field.getName());
                    extractCase.addStatement("case $S -> $T.$L", constantName, caseEnumType, constantName);
                }
            }

            // Default case - not set
            extractCase.addStatement("default -> $T.$L", caseEnumType, oneof.getNotSetConstantName());
            extractCase.endControlFlow("");
        } else {
            // Oneof not in this version - always return NOT_SET
            extractCase.addStatement("return $T.$L", caseEnumType, oneof.getNotSetConstantName());
        }

        return extractCase.build();
    }

    /**
     * Generate doClearXxx() implementation for version-specific BuilderImpl.
     *
     * @param oneof the merged oneof group
     * @param version the version identifier
     * @return the generated method spec
     */
    public MethodSpec generateImplBuilderDoClear(MergedOneof oneof, String version) {
        boolean oneofPresentInVersion = oneof.getPresentInVersions().contains(version);

        MethodSpec.Builder doClear = MethodSpec.methodBuilder(oneof.getDoClearMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED);

        if (oneofPresentInVersion) {
            // Protobuf builders have clearXxx() for the oneof group
            doClear.addStatement("protoBuilder.clear$L()", oneof.getJavaName());
        } else {
            // Oneof not in this version - no-op
            doClear.addComment("Oneof not present in this version - clear is no-op");
        }

        return doClear.build();
    }

    // ==================== Utility Methods ====================

    /**
     * Convert a name to SCREAMING_SNAKE_CASE.
     *
     * @param name the name to convert
     * @return the SCREAMING_SNAKE_CASE version
     */
    private static String toScreamingSnakeCase(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }
        return result.toString();
    }
}
