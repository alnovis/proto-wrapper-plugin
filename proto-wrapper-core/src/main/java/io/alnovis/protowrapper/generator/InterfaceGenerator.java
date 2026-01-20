package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedOneof;

import io.alnovis.protowrapper.generator.builder.BuilderInterfaceGenerator;
import io.alnovis.protowrapper.generator.oneof.OneofGenerator;

import static io.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates version-agnostic Java interfaces from merged schema.
 *
 * <p>This class orchestrates interface generation by delegating to specialized generators:</p>
 * <ul>
 *   <li>{@link InterfaceMethodGenerator} - field getters, has/supports methods</li>
 *   <li>{@link InterfaceCommonMethodGenerator} - common utility methods (toBytes, asVersion, etc.)</li>
 *   <li>{@link NestedTypeGenerator} - nested interfaces and enums</li>
 *   <li>{@link BuilderInterfaceGenerator} - nested Builder interface</li>
 *   <li>{@link OneofGenerator} - oneof case enums and discriminators</li>
 * </ul>
 *
 * <p>Example output:</p>
 * <pre>
 * public interface Money {
 *     long getBills();
 *     int getCoins();
 *     long toKopecks();
 * }
 * </pre>
 *
 * @see InterfaceMethodGenerator
 * @see InterfaceCommonMethodGenerator
 * @see NestedTypeGenerator
 */
public class InterfaceGenerator extends BaseGenerator<MergedMessage> {

    private final InterfaceMethodGenerator methodGenerator;
    private final InterfaceCommonMethodGenerator commonMethodGenerator;
    private final NestedTypeGenerator nestedTypeGenerator;

    /**
     * Create a new InterfaceGenerator.
     *
     * @param config the generator configuration
     */
    public InterfaceGenerator(GeneratorConfig config) {
        super(config);
        this.methodGenerator = new InterfaceMethodGenerator(config);
        this.commonMethodGenerator = new InterfaceCommonMethodGenerator(config);
        this.nestedTypeGenerator = new NestedTypeGenerator(config, methodGenerator);
    }

    /**
     * Generate interface for a merged message using context.
     *
     * @param message Merged message info
     * @param ctx Generation context
     * @return Generated JavaFile
     */
    public JavaFile generate(MergedMessage message, GenerationContext ctx) {
        TypeResolver resolver = ctx.getTypeResolver();

        // ProtoWrapper is the base interface for all wrappers
        ClassName protoWrapperType = ClassName.get(config.getApiPackage(), ProtoWrapperGenerator.INTERFACE_NAME);

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(message.getInterfaceName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(protoWrapperType)
                .addJavadoc("Version-agnostic interface for $L.\n\n", message.getName())
                .addJavadoc("<p>Supported in versions: $L</p>\n", message.getPresentInVersions());

        // Add getter methods for all fields
        for (MergedField field : message.getFieldsSorted()) {
            // Handle map fields specially
            if (field.isMap() && field.getMapInfo() != null) {
                methodGenerator.generateMapFieldMethods(interfaceBuilder, field, resolver);

                // Add supportsXxx() for version-specific fields
                if (!field.isUniversal(message.getPresentInVersions())) {
                    interfaceBuilder.addMethod(methodGenerator.generateSupportsMethod(field, message, resolver));
                }
                continue;
            }

            MethodSpec getter = methodGenerator.generateGetter(field, message, resolver);
            interfaceBuilder.addMethod(getter);

            // Add raw proto accessor for well-known type fields when enabled
            if (methodGenerator.shouldGenerateRawProtoAccessor(field)) {
                if (field.isRepeated()) {
                    interfaceBuilder.addMethod(methodGenerator.generateRepeatedRawProtoAccessor(field, resolver));
                } else {
                    interfaceBuilder.addMethod(methodGenerator.generateRawProtoAccessor(field, resolver));
                }
            }

            if (field.shouldGenerateHasMethod()) {
                interfaceBuilder.addMethod(methodGenerator.generateHasMethod(field, resolver));
            }

            // Add supportsXxx() for version-specific fields or fields with conflicts
            if (!field.isUniversal(message.getPresentInVersions()) ||
                (field.getConflictType() != null && field.getConflictType() != MergedField.ConflictType.NONE)) {
                interfaceBuilder.addMethod(methodGenerator.generateSupportsMethod(field, message, resolver));
            }

            // Add enum getter for INT_ENUM conflict fields (scalar only - repeated handled differently)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.INT_ENUM) {
                ctx.getSchema()
                        .getConflictEnum(message.getName(), field.getName())
                        .map(enumInfo -> methodGenerator.generateEnumGetter(field, enumInfo, resolver))
                        .ifPresent(interfaceBuilder::addMethod);
            }

            // Add bytes getter for STRING_BYTES conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.STRING_BYTES) {
                MethodSpec bytesGetter = methodGenerator.generateBytesGetter(field, resolver);
                interfaceBuilder.addMethod(bytesGetter);
            }

            // Add message getter for PRIMITIVE_MESSAGE conflict fields (scalar only)
            if (!field.isRepeated() && field.getConflictType() == MergedField.ConflictType.PRIMITIVE_MESSAGE) {
                MethodSpec messageGetter = methodGenerator.generateMessageGetter(field, message, resolver);
                if (messageGetter != null) {
                    interfaceBuilder.addMethod(messageGetter);
                    // Also add supportsXxxMessage() method
                    interfaceBuilder.addMethod(methodGenerator.generateSupportsMessageMethod(field, resolver));
                }
            }
        }

        // Add oneof Case enums and discriminator methods
        OneofGenerator oneofGenerator = new OneofGenerator(config);
        for (MergedOneof oneof : message.getOneofGroups()) {
            // Generate Case enum as nested type
            interfaceBuilder.addType(oneofGenerator.generateCaseEnum(oneof));

            // Add discriminator method getXxxCase()
            interfaceBuilder.addMethod(oneofGenerator.generateInterfaceCaseGetter(oneof, message));

            // Note: hasXxx() methods for oneof fields are generated by normal field processing
            // since oneof fields are regular fields with isInOneof() flag
        }

        // Add nested enums first
        message.getNestedEnums().stream()
                .map(nestedTypeGenerator::generateNestedEnum)
                .forEach(interfaceBuilder::addType);

        // Add nested interfaces for nested messages
        message.getNestedMessages().stream()
                .map(nested -> nestedTypeGenerator.generateNestedInterface(nested, ctx))
                .forEach(interfaceBuilder::addType);

        // Add common utility methods
        commonMethodGenerator.addCommonMethods(interfaceBuilder, message);

        // Common types used for static methods
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");
        ClassName interfaceType = ClassName.get(config.getApiPackage(), message.getInterfaceName());

        // Add Builder interface if enabled
        if (config.isGenerateBuilders()) {
            interfaceBuilder.addMethod(MethodSpec.methodBuilder("toBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "Builder"))
                    .addJavadoc("Create a builder initialized with this instance's values.\n")
                    .addJavadoc("@return Builder for creating modified copies\n")
                    .build());

            BuilderInterfaceGenerator builderGen = new BuilderInterfaceGenerator(config);
            TypeSpec builderInterface = builderGen.generate(message, resolver, ctx);
            interfaceBuilder.addType(builderInterface);

            // Add static newBuilder(VersionContext ctx) method
            ClassName builderType = interfaceType.nestedClass("Builder");
            String builderMethodName = "new" + message.getName() + "Builder";

            interfaceBuilder.addMethod(MethodSpec.methodBuilder("newBuilder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(versionContextType, "ctx")
                    .returns(builderType)
                    .addJavadoc("Create a new builder for $L using the specified version context.\n", message.getName())
                    .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L()}.</p>\n", builderMethodName)
                    .addJavadoc("@param ctx Version context to use for builder creation\n")
                    .addJavadoc("@return Empty builder for $L\n", message.getName())
                    .addStatement("return ctx.$L()", builderMethodName)
                    .build());
        }

        // Add static parseFromBytes(VersionContext ctx, byte[] bytes) method
        ClassName invalidProtocolBufferException = ClassName.get("com.google.protobuf", "InvalidProtocolBufferException");
        String parseMethodName = "parse" + message.getName() + "FromBytes";

        interfaceBuilder.addMethod(MethodSpec.methodBuilder("parseFromBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(versionContextType, "ctx")
                .addParameter(ArrayTypeName.of(TypeName.BYTE), "bytes")
                .returns(interfaceType)
                .addException(invalidProtocolBufferException)
                .addJavadoc("Parse bytes into a $L using the specified version context.\n", message.getName())
                .addJavadoc("<p>This is a convenience method equivalent to {@code ctx.$L(bytes)}.</p>\n", parseMethodName)
                .addJavadoc("@param ctx Version context determining the protocol version\n")
                .addJavadoc("@param bytes Serialized protobuf data\n")
                .addJavadoc("@return Parsed $L instance\n", message.getName())
                .addJavadoc("@throws InvalidProtocolBufferException if the bytes are not valid protobuf data\n")
                .addStatement("return ctx.$L(bytes)", parseMethodName)
                .build());

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(ctx.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate and write interface using context.
     *
     * @param message the merged message
     * @param ctx the generation context
     * @return the path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite(MergedMessage message, GenerationContext ctx) throws IOException {
        JavaFile javaFile = generate(message, ctx);
        writeToFile(javaFile);

        String relativePath = ctx.getApiPackage().replace('.', '/')
                + "/" + message.getInterfaceName() + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

}
