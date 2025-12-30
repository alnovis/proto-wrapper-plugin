package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedOneof;

import javax.lang.model.element.Modifier;

import static space.alnovis.protowrapper.generator.ProtobufConstants.GENERATED_FILE_COMMENT;

/**
 * Generates Case enum for oneof groups.
 *
 * <p>Example output for oneof "payment_method":</p>
 * <pre>
 * public enum PaymentMethodCase {
 *     CREDIT_CARD(2),
 *     BANK_TRANSFER(3),
 *     CRYPTO(4),
 *     PAYMENT_METHOD_NOT_SET(0);
 *
 *     private final int number;
 *
 *     PaymentMethodCase(int number) { this.number = number; }
 *     public int getNumber() { return number; }
 *
 *     public static PaymentMethodCase forNumber(int number) {
 *         for (PaymentMethodCase c : values()) {
 *             if (c.number == number) return c;
 *         }
 *         return PAYMENT_METHOD_NOT_SET;
 *     }
 * }
 * </pre>
 */
public class OneofCaseEnumGenerator {

    private final GeneratorConfig config;

    public OneofCaseEnumGenerator(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Generate a Case enum TypeSpec to be nested inside an interface or class.
     *
     * @param oneof the merged oneof group
     * @return the generated enum TypeSpec
     */
    public TypeSpec generateNestedEnum(MergedOneof oneof) {
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

    /**
     * Convert a proto name (snake_case) to SCREAMING_SNAKE_CASE for enum constants.
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
