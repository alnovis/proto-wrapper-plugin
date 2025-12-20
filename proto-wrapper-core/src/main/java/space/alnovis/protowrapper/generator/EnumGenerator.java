package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedEnumValue;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates version-agnostic Java enums from merged schema.
 *
 * <p>Example output:</p>
 * <pre>
 * public enum ResultTypeEnum {
 *     RESULT_TYPE_OK(0),
 *     RESULT_TYPE_INVALID_LOGIN_PASSWORD(1);
 *
 *     private final int value;
 *
 *     ResultTypeEnum(int value) { this.value = value; }
 *     public int getValue() { return value; }
 *
 *     public static ResultTypeEnum fromProtoValue(int value) {
 *         for (ResultTypeEnum e : values()) {
 *             if (e.value == value) return e;
 *         }
 *         return null;
 *     }
 * }
 * </pre>
 */
public class EnumGenerator extends BaseGenerator<MergedEnum> {

    public EnumGenerator(GeneratorConfig config) {
        super(config);
    }

    /**
     * Generate enum for a merged enum definition.
     *
     * @param enumInfo Merged enum info
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedEnum enumInfo) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumInfo.getName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Version-agnostic enum for $L.\n\n", enumInfo.getName())
                .addJavadoc("<p>Supported in versions: $L</p>\n", enumInfo.getPresentInVersions());

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
                .build());

        // Add enum constants with version info
        for (MergedEnumValue value : enumInfo.getValues()) {
            TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder("$L", value.getNumber());

            // Add version info to javadoc if not present in all versions
            if (!value.getPresentInVersions().containsAll(enumInfo.getPresentInVersions())) {
                constantBuilder.addJavadoc("Present only in versions: $L\n", value.getPresentInVersions());
            }

            enumBuilder.addEnumConstant(value.getJavaName(), constantBuilder.build());
        }

        // Add static fromProtoValue method
        ClassName enumClassName = ClassName.get(config.getApiPackage(), enumInfo.getName());
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

        TypeSpec enumSpec = enumBuilder.build();

        return JavaFile.builder(config.getApiPackage(), enumSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate and write enum.
     *
     * @param enumInfo Enum to generate
     * @return Path to generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite(MergedEnum enumInfo) throws IOException {
        JavaFile javaFile = generate(enumInfo);
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + enumInfo.getName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
