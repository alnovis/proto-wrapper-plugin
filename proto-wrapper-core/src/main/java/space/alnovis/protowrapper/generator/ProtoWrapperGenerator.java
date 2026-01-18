package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;

import static space.alnovis.protowrapper.generator.ProtobufConstants.*;

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
 *     int getWrapperVersion();
 *     byte[] toBytes();
 * }
 * </pre>
 *
 * <p>All generated message interfaces extend this interface, enabling type-safe
 * access to wrapped protos without reflection:</p>
 * <pre>{@code
 * if (wrapper instanceof ProtoWrapper pw) {
 *     Message proto = pw.getTypedProto();
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
        // Extract numeric version examples from version identifiers (e.g., "v1" -> "1", "v2" -> "2")
        String versionExamples = versions.stream()
                .map(v -> v.replaceAll("[^0-9]", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        if (versionExamples.isEmpty()) {
            versionExamples = "1, 2"; // fallback
        }

        // Build version package examples (e.g., "v1.Order, v2.Order")
        String versionPackageExamples = versions.stream()
                .limit(2) // take first two versions for examples
                .map(v -> v + ".Order")
                .collect(Collectors.joining(" or "));
        if (versionPackageExamples.isEmpty()) {
            versionPackageExamples = "v1.Order or v2.Order"; // fallback
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
                .addJavadoc("    int version = pw.getWrapperVersion();\n")
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

        // getWrapperVersion() - version accessor
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the protocol version this wrapper was created from.\n\n")
                .addJavadoc("@return version number (e.g., $L)\n", versionExamples)
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
