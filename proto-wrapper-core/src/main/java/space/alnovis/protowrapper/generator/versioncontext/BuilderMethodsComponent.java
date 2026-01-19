package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.generator.GeneratorConfig;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Component that generates builder factory methods for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>newXxxBuilder() - create builder for top-level message</li>
 *   <li>newXxxYyyBuilder() - create builder for nested message</li>
 * </ul>
 *
 * <p>Only generated when builders are enabled in configuration.</p>
 */
public class BuilderMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;
    private final MergedSchema schema;

    /**
     * Create a new BuilderMethodsComponent.
     *
     * @param config generator configuration
     * @param schema merged schema
     */
    public BuilderMethodsComponent(GeneratorConfig config, MergedSchema schema) {
        this.config = config;
        this.schema = schema;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        if (!config.isGenerateBuilders()) {
            return;
        }

        Set<String> allVersions = new HashSet<>(schema.getVersions());

        for (MergedMessage message : schema.getMessages()) {
            ClassName returnType = ClassName.get(config.getApiPackage(), message.getInterfaceName());
            ClassName builderType = returnType.nestedClass("Builder");

            boolean existsInAllVersions = message.getPresentInVersions().containsAll(allVersions);

            // newXxxBuilder() for top-level message
            builder.addMethod(createBuilderMethod(
                    "new" + message.getName() + "Builder",
                    message.getName(),
                    builderType,
                    existsInAllVersions,
                    message.getPresentInVersions()));

            // Recursively add nested builder methods
            addNestedBuilderMethods(builder, message, allVersions);
        }
    }

    private void addNestedBuilderMethods(TypeSpec.Builder interfaceBuilder, MergedMessage parent,
                                          Set<String> allVersions) {
        for (MergedMessage nested : parent.getNestedMessages()) {
            ClassName nestedType = buildNestedInterfaceType(nested);
            ClassName builderType = nestedType.nestedClass("Builder");

            String methodName = buildNestedBuilderMethodName(nested);
            String qualifiedName = buildNestedQualifiedName(nested);

            boolean existsInAllVersions = nested.getPresentInVersions().containsAll(allVersions);

            interfaceBuilder.addMethod(createBuilderMethod(
                    methodName,
                    qualifiedName,
                    builderType,
                    existsInAllVersions,
                    nested.getPresentInVersions()));

            // Recursively add deeply nested types
            addNestedBuilderMethods(interfaceBuilder, nested, allVersions);
        }
    }

    private MethodSpec createBuilderMethod(String methodName, String messageName, ClassName builderType,
                                            boolean existsInAllVersions, Set<String> presentInVersions) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(builderType)
                .addJavadoc("Create a new builder for $L.\n", messageName)
                .addJavadoc("@return Empty builder for $L\n", messageName);

        if (existsInAllVersions) {
            methodBuilder.addModifiers(Modifier.ABSTRACT);
        } else {
            methodBuilder.addModifiers(Modifier.DEFAULT);
            methodBuilder.addJavadoc("@apiNote Present only in versions: $L\n", presentInVersions);
            methodBuilder.addStatement("throw new $T($S + $S)",
                    UnsupportedOperationException.class,
                    messageName + " is not available in this version. Present in: ",
                    presentInVersions.toString());
        }

        return methodBuilder.build();
    }

    /**
     * Build fully qualified ClassName for a nested message interface.
     */
    private ClassName buildNestedInterfaceType(MergedMessage nested) {
        List<String> path = new ArrayList<>();
        MergedMessage current = nested;
        while (current != null) {
            path.add(current.getInterfaceName());
            current = current.getParent();
        }
        Collections.reverse(path);

        ClassName result = ClassName.get(config.getApiPackage(), path.get(0));
        for (int i = 1; i < path.size(); i++) {
            result = result.nestedClass(path.get(i));
        }
        return result;
    }

    /**
     * Build flattened method name for nested builder.
     */
    private String buildNestedBuilderMethodName(MergedMessage nested) {
        return "new" + String.join("", collectMessageHierarchyNames(nested)) + "Builder";
    }

    /**
     * Build qualified display name for nested message (for javadoc).
     */
    private String buildNestedQualifiedName(MergedMessage nested) {
        return String.join(".", collectMessageHierarchyNames(nested));
    }

    /**
     * Collect message names from root to current.
     */
    private List<String> collectMessageHierarchyNames(MergedMessage message) {
        LinkedList<String> names = new LinkedList<>();
        for (MergedMessage current = message; current != null; current = current.getParent()) {
            names.addFirst(current.getName());
        }
        return names;
    }
}
