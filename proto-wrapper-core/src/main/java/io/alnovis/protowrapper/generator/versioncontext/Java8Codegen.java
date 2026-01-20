package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.alnovis.protowrapper.generator.GeneratorConfig;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Java 8 compatible code generation strategy.
 *
 * <p>Java 8 interfaces cannot have private static methods, so this implementation
 * uses a helper class (VersionContextHelper) to initialize the CONTEXTS map.</p>
 *
 * <p>Also uses Collections.unmodifiableList(Arrays.asList(...)) instead of List.of().</p>
 */
public class Java8Codegen implements JavaVersionCodegen {

    /** Singleton instance. */
    public static final Java8Codegen INSTANCE = new Java8Codegen();

    private Java8Codegen() {
    }

    // ==================== Common Operations ====================

    @Override
    public AnnotationSpec deprecatedAnnotation(String since, boolean forRemoval) {
        // Java 8 does not support @Deprecated(since, forRemoval)
        return AnnotationSpec.builder(Deprecated.class).build();
    }

    @Override
    public CodeBlock immutableListOf(String... elements) {
        String joined = String.join(", ", elements);
        return CodeBlock.of("$T.unmodifiableList($T.asList($L))",
                Collections.class, Arrays.class, joined);
    }

    @Override
    public CodeBlock immutableSetOf(String... elements) {
        String joined = String.join(", ", elements);
        return CodeBlock.of("$T.unmodifiableSet(new $T<>($T.asList($L)))",
                Collections.class, HashSet.class, Arrays.class, joined);
    }

    @Override
    public boolean supportsPrivateInterfaceMethods() {
        return false;
    }

    // ==================== VersionContext-specific Operations ====================

    @Override
    public FieldSpec createContextsField(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config) {

        ClassName helperClass = ClassName.get(config.getApiPackage(), "VersionContextHelper");

        return FieldSpec.builder(mapType, "CONTEXTS",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.createContexts()", helperClass)
                .build();
    }

    @Override
    public FieldSpec createSupportedVersionsField(
            ParameterizedTypeName listType,
            String versionsJoined) {

        return FieldSpec.builder(listType, "SUPPORTED_VERSIONS",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.unmodifiableList($T.asList($L))",
                        Collections.class, Arrays.class, versionsJoined)
                .build();
    }

    @Override
    public Optional<MethodSpec> createContextsMethod(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config) {
        // Java 8 uses helper class instead of private interface method
        return Optional.empty();
    }

    @Override
    public boolean requiresHelperClass() {
        return true;
    }
}
