package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.model.MergedEnum;
import io.alnovis.protowrapper.model.MergedEnumValue;

import static io.alnovis.protowrapper.generator.ProtobufConstants.*;

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

    /**
     * Create a new EnumGenerator.
     *
     * @param config the generator configuration
     */
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

            enumBuilder.addEnumConstant(enumInfo.getStrippedValueName(value), constantBuilder.build());
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

        // Add static fromProto method for converting any proto enum
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");
        enumBuilder.addMethod(MethodSpec.methodBuilder("fromProto")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(ClassName.OBJECT, "protoEnum")
                .beginControlFlow("if (protoEnum == null)")
                .addStatement("return null")
                .endControlFlow()
                .beginControlFlow("try")
                .addStatement("$T getNumber = protoEnum.getClass().getMethod($S)", methodClass, "getNumber")
                .addStatement("int number = (int) getNumber.invoke(protoEnum)")
                .addStatement("return fromProtoValue(number)")
                .nextControlFlow("catch ($T e)", ClassName.get(ReflectiveOperationException.class))
                .addStatement("throw new $T($S + protoEnum.getClass().getName(), e)",
                        ClassName.get(IllegalArgumentException.class),
                        "Cannot convert to " + enumInfo.getName() + ": ")
                .endControlFlow()
                .addJavadoc("Convert any proto enum to this wrapper enum.\n\n")
                .addJavadoc("<p>Works with any version's proto enum by extracting numeric value via reflection.</p>\n\n")
                .addJavadoc("@param protoEnum Proto enum instance (e.g., v202.Message.CommandTypeEnum.COMMAND_INFO)\n")
                .addJavadoc("@return Corresponding wrapper enum constant, or null if value not found\n")
                .addJavadoc("@throws IllegalArgumentException if protoEnum is not a valid protobuf enum\n")
                .build());

        // Add matches method for comparing with proto enum
        enumBuilder.addMethod(MethodSpec.methodBuilder("matches")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(ClassName.OBJECT, "protoEnum")
                .beginControlFlow("if (protoEnum == null)")
                .addStatement("return false")
                .endControlFlow()
                .beginControlFlow("try")
                .addStatement("$T getNumber = protoEnum.getClass().getMethod($S)", methodClass, "getNumber")
                .addStatement("int number = (int) getNumber.invoke(protoEnum)")
                .addStatement("return this.value == number")
                .nextControlFlow("catch ($T e)", ClassName.get(ReflectiveOperationException.class))
                .addStatement("return false")
                .endControlFlow()
                .addJavadoc("Check if this wrapper enum matches a proto enum by numeric value.\n\n")
                .addJavadoc("<p>Works with any version's proto enum.</p>\n\n")
                .addJavadoc("@param protoEnum Proto enum to compare with\n")
                .addJavadoc("@return true if numeric values match, false otherwise\n")
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
