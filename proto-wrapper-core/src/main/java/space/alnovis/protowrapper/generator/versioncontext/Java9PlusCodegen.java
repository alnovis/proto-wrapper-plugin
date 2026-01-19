package space.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import space.alnovis.protowrapper.generator.GeneratorConfig;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Java 9+ code generation strategy.
 *
 * <p>Uses modern Java features:</p>
 * <ul>
 *   <li>Private static methods in interfaces</li>
 *   <li>List.of() for immutable lists</li>
 * </ul>
 */
public class Java9PlusCodegen implements JavaVersionCodegen {

    /** Singleton instance. */
    public static final Java9PlusCodegen INSTANCE = new Java9PlusCodegen();

    private Java9PlusCodegen() {
    }

    @Override
    public FieldSpec createContextsField(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config) {

        return FieldSpec.builder(mapType, "CONTEXTS",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("createContexts()")
                .build();
    }

    @Override
    public FieldSpec createSupportedVersionsField(
            ParameterizedTypeName listType,
            String versionsJoined) {

        return FieldSpec.builder(listType, "SUPPORTED_VERSIONS",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.of($L)", List.class, versionsJoined)
                .build();
    }

    @Override
    public Optional<MethodSpec> createContextsMethod(
            ParameterizedTypeName mapType,
            ClassName versionContextType,
            List<String> versions,
            GeneratorConfig config) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("createContexts")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(mapType)
                .addStatement("$T<$T, $T> map = new $T<>()",
                        Map.class, String.class, versionContextType,
                        LinkedHashMap.class);

        for (String version : versions) {
            String implPackage = config.getImplPackage(version);
            String contextClass = "VersionContext" + version.substring(0, 1).toUpperCase() + version.substring(1);
            ClassName contextClassName = ClassName.get(implPackage, contextClass);
            methodBuilder.addStatement("map.put($S, $T.INSTANCE)", version, contextClassName);
        }

        methodBuilder.addStatement("return $T.unmodifiableMap(map)", Collections.class);

        return Optional.of(methodBuilder.build());
    }

    @Override
    public boolean requiresHelperClass() {
        return false;
    }
}
