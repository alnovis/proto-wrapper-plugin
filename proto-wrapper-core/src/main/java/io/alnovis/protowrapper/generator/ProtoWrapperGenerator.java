package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;

import static io.alnovis.protowrapper.generator.ProtobufConstants.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the ProtoWrapper interface.
 *
 * <p>ProtoWrapper is a common interface for all generated wrapper classes.
 * It provides a unified way to access the underlying protobuf message
 * and common wrapper methods without reflection.</p>
 *
 * <p>Generated output:</p>
 * <pre>
 * public interface ProtoWrapper {
 *     Message getTypedProto();
 *     String getWrapperVersionId();
 *     byte[] toBytes();
 * }
 * </pre>
 *
 * <p>All generated message interfaces extend this interface, enabling type-safe
 * access to wrapped protos without reflection:</p>
 * <pre>{@code
 * if (wrapper instanceof ProtoWrapper pw) {
 *     Message proto = pw.getTypedProto();
 *     String versionId = pw.getWrapperVersionId(); // e.g., "v1", "v2"
 * }
 * }</pre>
 *
 * @since 1.6.6
 * @see InterfaceGenerator
 */
public class ProtoWrapperGenerator extends BaseGenerator<Void> {

    /**
     * The name of the generated interface.
     */
    public static final String INTERFACE_NAME = "ProtoWrapper";

    private final List<String> versions;

    /**
     * Create a new ProtoWrapperGenerator with the specified configuration.
     *
     * @param config the generator configuration
     * @param versions list of version identifiers from the schema (e.g., ["v1", "v2"])
     */
    public ProtoWrapperGenerator(GeneratorConfig config, List<String> versions) {
        super(config);
        this.versions = versions;
    }

    /**
     * Generate ProtoWrapper interface.
     *
     * @return Generated JavaFile
     */
    public JavaFile generate() {
        // Build version package examples (e.g., "v1.Order, v2.Order")
        String versionPackageExamples = versions.stream()
                .limit(2) // take first two versions for examples
                .map(v -> v + ".Order")
                .collect(Collectors.joining(" or "));
        if (versionPackageExamples.isEmpty()) {
            versionPackageExamples = "v1.Order or v2.Order"; // fallback
        }

        // Build version ID examples (e.g., "v1", "v2")
        String versionIdExamples = versions.stream()
                .limit(2)
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(", "));
        if (versionIdExamples.isEmpty()) {
            versionIdExamples = "\"v1\", \"v2\""; // fallback
        }

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(INTERFACE_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Common interface for all generated wrapper classes.\n\n")
                .addJavadoc("<p>This interface provides a unified way to access the underlying protobuf message\n")
                .addJavadoc("and common wrapper methods without reflection.</p>\n\n")
                .addJavadoc("<p>All generated wrapper interfaces extend this interface, enabling type-safe\n")
                .addJavadoc("access to wrapped protos:</p>\n")
                .addJavadoc("<pre>{@code\n")
                .addJavadoc("if (wrapper instanceof ProtoWrapper pw) {\n")
                .addJavadoc("    Message proto = pw.getTypedProto();\n")
                .addJavadoc("    String versionId = pw.getWrapperVersionId();\n")
                .addJavadoc("}\n")
                .addJavadoc("}</pre>\n\n")
                .addJavadoc("@since 1.6.6\n");

        // getTypedProto() - core method
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("getTypedProto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(MESSAGE_CLASS)
                .addJavadoc("Get the underlying protobuf message.\n\n")
                .addJavadoc("<p>The returned message is the actual protobuf object wrapped by this instance.\n")
                .addJavadoc("The concrete type depends on the protocol version (e.g., $L).</p>\n\n", versionPackageExamples)
                .addJavadoc("@return underlying protobuf Message, never null\n")
                .build());

        // getWrapperVersionId() - string version accessor
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("getWrapperVersionId")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(String.class))
                .addJavadoc("Get the version identifier this wrapper was created from.\n\n")
                .addJavadoc("@return version identifier (e.g., $L)\n", versionIdExamples)
                .build());

        // toBytes() - serialization
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Serialize to protobuf bytes.\n\n")
                .addJavadoc("@return protobuf-encoded bytes\n")
                .build());

        TypeSpec interfaceSpec = interfaceBuilder.build();

        return JavaFile.builder(config.getApiPackage(), interfaceSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Generate and write ProtoWrapper interface.
     *
     * @return the path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite() throws IOException {
        JavaFile javaFile = generate();
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/" + INTERFACE_NAME + ".java";
        return config.getOutputDirectory().resolve(relativePath);
    }

    /**
     * Get the ClassName for ProtoWrapper interface.
     *
     * @return ClassName for ProtoWrapper
     */
    public ClassName getProtoWrapperClassName() {
        return ClassName.get(config.getApiPackage(), INTERFACE_NAME);
    }
}
