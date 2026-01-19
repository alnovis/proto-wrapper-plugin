package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component that generates abstract instance methods for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>getVersionId() - primary method to get version identifier</li>
 *   <li>getVersion() - deprecated method for numeric version</li>
 * </ul>
 */
public class InstanceMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new InstanceMethodsComponent.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public InstanceMethodsComponent(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        List<String> versions = schema.getVersions();

        String versionIdExamples = versions.stream()
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(", "));

        // getVersionId() - primary method
        builder.addMethod(MethodSpec.methodBuilder("getVersionId")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(String.class)
                .addJavadoc("Get the version identifier for this context.\n\n")
                .addJavadoc("@return Version identifier (e.g., $L)\n", versionIdExamples)
                .build());

        // getVersion() - deprecated
        // Java 8 doesn't support @Deprecated(since, forRemoval), use simple @Deprecated
        AnnotationSpec.Builder deprecatedBuilder = AnnotationSpec.builder(Deprecated.class);
        if (!config.isJava8Compatible()) {
            deprecatedBuilder.addMember("since", "$S", "1.6.7")
                    .addMember("forRemoval", "$L", true);
        }
        AnnotationSpec deprecatedAnnotation = deprecatedBuilder.build();

        builder.addMethod(MethodSpec.methodBuilder("getVersion")
                .addAnnotation(deprecatedAnnotation)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the numeric protocol version.\n\n")
                .addJavadoc("@return Protocol version number (extracted from version identifier)\n")
                .addJavadoc("@deprecated since 1.6.7, for removal. Use {@link #getVersionId()} instead. ")
                .addJavadoc("This method returns 0 for non-numeric version identifiers.\n")
                .build());
    }
}
