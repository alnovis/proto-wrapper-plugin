package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.ConflictEnumInfo;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates unified enums for INT_ENUM type conflict fields.
 *
 * <p>When a field has type int in one version and enum in another,
 * this generator creates a unified enum that can be used in the Builder interface.</p>
 *
 * <p>Example output:</p>
 * <pre>
 * public enum UnitType {
 *     UNIT_CELSIUS(0),
 *     UNIT_FAHRENHEIT(1),
 *     UNIT_KELVIN(2);
 *
 *     private final int value;
 *
 *     UnitType(int value) { this.value = value; }
 *     public int getValue() { return value; }
 *
 *     public static UnitType fromProtoValue(int value) {
 *         for (UnitType e : values()) {
 *             if (e.value == value) return e;
 *         }
 *         return null;
 *     }
 * }
 * </pre>
 */
public class ConflictEnumGenerator extends BaseGenerator<ConflictEnumInfo> {

    public ConflictEnumGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Generate enum for a conflict field.
     *
     * @param enumInfo Conflict enum info
     * @return Generated JavaFile
     */
    public JavaFile generate(ConflictEnumInfo enumInfo) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.getEnumName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Unified enum for INT_ENUM conflict field {@code $L.$L}.\n\n",
                        enumInfo.getMessageName(), enumInfo.getFieldName())
                .addJavadoc("<p>This enum provides type-safe access to the field value across versions\n")
                .addJavadoc("where the field type differs (int in some versions, enum in others).</p>\n\n")
                .addJavadoc("<p>Versions with enum type: $L</p>\n", enumInfo.getEnumVersions());

        // Add value field
        enumBuilder.addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL);

        // Add constructor
        enumBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value")
                .build());

        // Add getValue() method
        enumBuilder.addMethod(MethodSpec.methodBuilder("getValue")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return value")
                .addJavadoc("Get the numeric value for this enum constant.\n")
                .addJavadoc("@return The proto numeric value\n")
                .build());

        // Add enum constants
        for (ConflictEnumInfo.EnumValue value : enumInfo.getValues()) {
            TypeSpec constantSpec = TypeSpec.anonymousClassBuilder("$L", value.number())
                    .build();
            enumBuilder.addEnumConstant(value.name(), constantSpec);
        }

        // Add static fromProtoValue method
        ClassName enumClassName = ClassName.get(config.getApiPackage(), enumInfo.getEnumName());
        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProtoValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(TypeName.INT, "value")
                .beginControlFlow("for ($T e : values())", enumClassName)
                .beginControlFlow("if (e.value == value)")
                .addStatement("return e")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return null")
                .addJavadoc("Convert proto enum numeric value to this enum.\n")
                .addJavadoc("@param value Proto enum ordinal\n")
                .addJavadoc("@return Matching enum constant or null if not found\n")
                .build());

        // Add static fromProtoValue method with default value
        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProtoValueOrDefault")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(TypeName.INT, "value")
                .addParameter(enumClassName, "defaultValue")
                .addStatement("$T result = fromProtoValue(value)", enumClassName)
                .addStatement("return result != null ? result : defaultValue")
                .addJavadoc("Convert proto enum numeric value to this enum with a default.\n")
                .addJavadoc("@param value Proto enum ordinal\n")
                .addJavadoc("@param defaultValue Value to return if no match found\n")
                .addJavadoc("@return Matching enum constant or defaultValue\n")
                .build());

        // Add static fromProtoValueOrThrow method with informative error message
        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProtoValueOrThrow")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(TypeName.INT, "value")
                .addStatement("$T result = fromProtoValue(value)", enumClassName)
                .beginControlFlow("if (result == null)")
                .addStatement("throw new $T($S + value + $S + $T.toString(values()))",
                        IllegalArgumentException.class,
                        "Invalid value ",
                        " for " + enumInfo.getEnumName() + ". Valid values: ",
                        java.util.Arrays.class)
                .endControlFlow()
                .addStatement("return result")
                .addJavadoc("Convert proto enum numeric value to this enum, throwing if not found.\n")
                .addJavadoc("@param value Proto enum ordinal\n")
                .addJavadoc("@return Matching enum constant\n")
                .addJavadoc("@throws IllegalArgumentException if value is not valid for this enum\n")
                .build());

        TypeSpec enumSpec = enumBuilder.build();

        return JavaFile.builder(config.getApiPackage(), enumSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate and write enum file.
     *
     * @param enumInfo Conflict enum info
     * @return Path to generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite(ConflictEnumInfo enumInfo) throws IOException {
        JavaFile javaFile = generate(enumInfo);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + enumInfo.getEnumName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
