package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component that generates abstract instance methods for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>getVersionId() - method to get version identifier</li>
 * </ul>
 */
public class InstanceMethodsComponent implements InterfaceComponent {

    private final MergedSchema schema;

    /**
     * Create a new InstanceMethodsComponent.
     *
     * @param config generator configuration (unused, kept for API compatibility)
     * @param schema merged schema
     */
    public InstanceMethodsComponent(GeneratorConfig config, MergedSchema schema) {
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
    }
}
