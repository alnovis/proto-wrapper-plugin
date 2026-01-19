package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
 *
 * <p>Common operations (available for all generators):</p>
 * <ul>
 *   <li>{@link #deprecatedAnnotation(String, boolean)} - create @Deprecated with version-appropriate parameters</li>
 *   <li>{@link #immutableListOf(String...)} - create immutable list expression</li>
 *   <li>{@link #immutableSetOf(String...)} - create immutable set expression</li>
 * </ul>
 *
 * @since 1.6.6
 */
public interface JavaVersionCodegen {

    // ==================== Common Operations (for all generators) ====================

    /**
     * Create @Deprecated annotation with appropriate parameters for target Java version.
     *
     * <p>For Java 9+: {@code @Deprecated(since = "1.6.9", forRemoval = true)}</p>
     * <p>For Java 8: {@code @Deprecated}</p>
     *
     * @param since the version since which the element is deprecated
     * @param forRemoval whether the element is scheduled for removal
     * @return the annotation specification
     * @since 1.6.9
     */
    AnnotationSpec deprecatedAnnotation(String since, boolean forRemoval);

    /**
     * Create immutable list expression.
     *
     * <p>For Java 9+: {@code List.of("a", "b")}</p>
     * <p>For Java 8: {@code Collections.unmodifiableList(Arrays.asList("a", "b"))}</p>
     *
     * @param elements the list elements (already formatted as code, e.g., quoted strings)
     * @return code block for creating immutable list
     * @since 1.6.9
     */
    CodeBlock immutableListOf(String... elements);

    /**
     * Create immutable set expression.
     *
     * <p>For Java 9+: {@code Set.of("a", "b")}</p>
     * <p>For Java 8: {@code Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")))}</p>
     *
     * @param elements the set elements (already formatted as code, e.g., quoted strings)
     * @return code block for creating immutable set
     * @since 1.6.9
     */
    CodeBlock immutableSetOf(String... elements);

    /**
     * Check if private interface methods are supported.
     *
     * @return true for Java 9+, false for Java 8
     * @since 1.6.9
     */
    boolean supportsPrivateInterfaceMethods();

    // ==================== VersionContext-specific Operations ====================

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
