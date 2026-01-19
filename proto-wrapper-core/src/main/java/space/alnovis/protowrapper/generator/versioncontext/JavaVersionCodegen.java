package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import space.alnovis.protowrapper.generator.GeneratorConfig;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for Java version-specific code generation.
 *
 * <p>Implementations handle differences between Java 8 and Java 9+ code generation,
 * such as private interface methods and List.of() vs Arrays.asList().</p>
 */
public interface JavaVersionCodegen {

    /**
     * Create the CONTEXTS static field.
     *
     * @param mapType the Map type for the field
     * @param versionContextType the VersionContext interface type
     * @param versions list of version strings
     * @param config generator configuration
     * @return the field specification
     */
    FieldSpec createContextsField(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config);

    /**
     * Create the SUPPORTED_VERSIONS static field.
     *
     * @param listType the List type for the field
     * @param versionsJoined comma-separated quoted version strings
     * @return the field specification
     */
    FieldSpec createSupportedVersionsField(
            ParameterizedTypeName listType,
            String versionsJoined);

    /**
     * Create the private createContexts() method if needed.
     *
     * <p>Java 9+ uses a private static method in the interface.
     * Java 8 returns empty (uses helper class instead).</p>
     *
     * @param mapType the Map type for the return
     * @param versionContextType the VersionContext interface type
     * @param versions list of version strings
     * @param config generator configuration
     * @return optional method specification (empty for Java 8)
     */
    Optional<MethodSpec> createContextsMethod(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config);

    /**
     * Check if this codegen requires a helper class.
     *
     * @return true if helper class is needed (Java 8)
     */
    boolean requiresHelperClass();
}
