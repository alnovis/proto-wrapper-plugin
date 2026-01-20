package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Component that generates static factory methods for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>forVersionId(String) - primary factory method</li>
 *   <li>find(String) - returns Optional</li>
 *   <li>getDefault() - returns default (latest) version</li>
 *   <li>supportedVersions() - returns list of versions</li>
 *   <li>defaultVersion() - returns default version string</li>
 *   <li>isSupported(String) - checks if version is supported</li>
 * </ul>
 */
public class StaticMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new StaticMethodsComponent.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public StaticMethodsComponent(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        List<String> versions = schema.getVersions();
        String defaultVersion = versions.get(versions.size() - 1);

        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");

        ParameterizedTypeName listType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(String.class));

        String versionIdExamples = versions.stream()
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(", "));

        // forVersionId(String) - primary factory method
        builder.addMethod(MethodSpec.methodBuilder("forVersionId")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(versionContextType)
                .addParameter(String.class, "versionId")
                .addJavadoc("Get VersionContext for a specific version identifier.\n\n")
                .addJavadoc("@param versionId Version identifier (e.g., $L)\n", versionIdExamples)
                .addJavadoc("@return VersionContext for the specified version\n")
                .addJavadoc("@throws IllegalArgumentException if versionId is null or not supported\n")
                .addStatement("$T ctx = CONTEXTS.get(versionId)", versionContextType)
                .beginControlFlow("if (ctx == null)")
                .addStatement("throw new $T($S + versionId + $S + SUPPORTED_VERSIONS)",
                        IllegalArgumentException.class,
                        "Unsupported version: '",
                        "'. Supported: ")
                .endControlFlow()
                .addStatement("return ctx")
                .build());

        // find(String) - returns Optional
        ParameterizedTypeName optionalType = ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                versionContextType);

        builder.addMethod(MethodSpec.methodBuilder("find")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(optionalType)
                .addParameter(String.class, "versionId")
                .addJavadoc("Find VersionContext for the specified version.\n\n")
                .addJavadoc("@param versionId Version identifier (e.g., $L)\n", versionIdExamples)
                .addJavadoc("@return Optional containing VersionContext, or empty if not supported\n")
                .addStatement("return $T.ofNullable(CONTEXTS.get(versionId))", Optional.class)
                .build());

        // getDefault() - returns default version
        builder.addMethod(MethodSpec.methodBuilder("getDefault")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(versionContextType)
                .addJavadoc("Get the default VersionContext (latest version).\n\n")
                .addJavadoc("@return Default VersionContext ($L)\n", defaultVersion)
                .addStatement("return CONTEXTS.get(DEFAULT_VERSION)")
                .build());

        // supportedVersions() - returns list
        builder.addMethod(MethodSpec.methodBuilder("supportedVersions")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(listType)
                .addJavadoc("Get list of supported version identifiers.\n\n")
                .addJavadoc("@return Immutable list of supported versions (e.g., [$L])\n", versionIdExamples)
                .addStatement("return SUPPORTED_VERSIONS")
                .build());

        // defaultVersion() - returns default version string
        builder.addMethod(MethodSpec.methodBuilder("defaultVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addJavadoc("Get the default version identifier.\n\n")
                .addJavadoc("@return Default version identifier ($S)\n", defaultVersion)
                .addStatement("return DEFAULT_VERSION")
                .build());

        // isSupported(String) - checks if version is supported
        builder.addMethod(MethodSpec.methodBuilder("isSupported")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(String.class, "versionId")
                .addJavadoc("Check if a version is supported.\n\n")
                .addJavadoc("@param versionId Version identifier to check\n")
                .addJavadoc("@return true if version is supported\n")
                .addStatement("return CONTEXTS.containsKey(versionId)")
                .build());
    }
}
