package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component that generates static fields for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>CONTEXTS - Map of version ID to VersionContext instance</li>
 *   <li>SUPPORTED_VERSIONS - List of supported version IDs</li>
 *   <li>DEFAULT_VERSION - Default version ID (latest)</li>
 * </ul>
 */
public class StaticFieldsComponent implements InterfaceComponent {

    private final JavaVersionCodegen codegen;
    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new StaticFieldsComponent.
     *
     * @param codegen Java version-specific code generator
     * @param config generator configuration
     * @param schema merged schema
     */
    public StaticFieldsComponent(JavaVersionCodegen codegen, GeneratorConfig config, MergedSchema schema) {
        this.codegen = codegen;
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        List<String> versions = schema.getVersions();
        String defaultVersion = versions.get(versions.size() - 1);

        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");

        // Map<String, VersionContext>
        ParameterizedTypeName mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                versionContextType);

        // List<String>
        ParameterizedTypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(String.class));

        String versionsJoined = versions.stream()
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(", "));

        // Add createContexts() method if needed (Java 9+)
        codegen.createContextsMethod(mapType, versionContextType, versions, config)
                .ifPresent(builder::addMethod);

        // Add CONTEXTS field
        builder.addField(codegen.createContextsField(mapType, versionContextType, versions, config));

        // Add SUPPORTED_VERSIONS field
        builder.addField(codegen.createSupportedVersionsField(listType, versionsJoined));

        // Add DEFAULT_VERSION field
        builder.addField(FieldSpec.builder(String.class, "DEFAULT_VERSION",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", defaultVersion)
                .build());
    }
}
