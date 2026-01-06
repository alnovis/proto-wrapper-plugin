package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;
import space.alnovis.protowrapper.model.MergedMessage;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;

/**
 * Generates common utility methods for wrapper interfaces.
 *
 * <p>This class is responsible for generating:</p>
 * <ul>
 *   <li>{@code getWrapperVersion()} - protocol version accessor</li>
 *   <li>{@code toBytes()} - serialization method</li>
 *   <li>{@code asVersion()} / {@code asVersionStrict()} - version conversion</li>
 *   <li>{@code getContext()} - VersionContext accessor</li>
 *   <li>{@code emptyBuilder()} / {@code toBuilder()} - builder methods</li>
 *   <li>{@code getFieldsInaccessibleInVersion()} - compatibility check</li>
 *   <li>{@code canConvertLosslesslyTo()} - lossless conversion check</li>
 * </ul>
 *
 * <p>Extracted from {@link InterfaceGenerator} to improve maintainability
 * and separation of concerns.</p>
 *
 * @since 1.2.0
 * @see InterfaceGenerator
 * @see InterfaceMethodGenerator
 */
public final class InterfaceCommonMethodGenerator {

    private final GeneratorConfig config;

    /**
     * Create a new InterfaceCommonMethodGenerator.
     *
     * @param config the generator configuration
     */
    public InterfaceCommonMethodGenerator(GeneratorConfig config) {
        this.config = config;
    }

    /**
     * Add all common utility methods to the interface builder.
     *
     * @param builder Interface builder to add methods to
     * @param message Message being generated
     */
    public void addCommonMethods(TypeSpec.Builder builder, MergedMessage message) {
        addGetWrapperVersion(builder, message);
        addToBytes(builder);
        addAsVersion(builder, message);
        addAsVersionStrict(builder, message);
        addGetContext(builder);
        addFieldsInaccessibleInVersion(builder);
        addCanConvertLosslesslyTo(builder);

        if (config.isGenerateBuilders()) {
            addEmptyBuilder(builder);
        }
    }

    // ==================== Version Methods ====================

    private void addGetWrapperVersion(TypeSpec.Builder builder, MergedMessage message) {
        String versionExamples = message.getPresentInVersions().stream()
                .map(v -> v.replaceAll("[^0-9]", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));

        builder.addMethod(MethodSpec.methodBuilder("getWrapperVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.INT)
                .addJavadoc("Get the wrapper protocol version this instance was created from.\n")
                .addJavadoc("@return Protocol version (e.g., $L)\n", versionExamples)
                .build());
    }

    // ==================== Serialization Methods ====================

    private void addToBytes(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("toBytes")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addJavadoc("Serialize to protobuf bytes.\n")
                .addJavadoc("@return Protobuf-encoded bytes\n")
                .build());
    }

    // ==================== Version Conversion Methods ====================

    private void addAsVersion(TypeSpec.Builder builder, MergedMessage message) {
        TypeVariableName typeVar = TypeVariableName.get("T",
                ClassName.get(config.getApiPackage(), message.getInterfaceName()));

        builder.addMethod(MethodSpec.methodBuilder("asVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Convert to a specific version implementation.\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                .build());
    }

    private void addAsVersionStrict(TypeSpec.Builder builder, MergedMessage message) {
        TypeVariableName typeVar = TypeVariableName.get("T",
                ClassName.get(config.getApiPackage(), message.getInterfaceName()));

        builder.addMethod(MethodSpec.methodBuilder("asVersionStrict")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(typeVar)
                .returns(typeVar)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), "versionClass")
                .addJavadoc("Convert to a specific version with strict data accessibility check.\n")
                .addJavadoc("<p>Unlike {@link #asVersion(Class)}, this method throws an exception if any\n")
                .addJavadoc("populated fields would become inaccessible in the target version.</p>\n")
                .addJavadoc("<p><b>Note:</b> Data is NOT physically lost (protobuf preserves unknown fields),\n")
                .addJavadoc("but it cannot be accessed through the target version's API.</p>\n")
                .addJavadoc("@param versionClass Target version class\n")
                .addJavadoc("@return Instance of the specified version\n")
                .addJavadoc("@throws IllegalStateException if any populated fields would become inaccessible\n")
                .build());
    }

    // ==================== Context Methods ====================

    private void addGetContext(TypeSpec.Builder builder) {
        ClassName versionContextType = ClassName.get(config.getApiPackage(), "VersionContext");

        builder.addMethod(MethodSpec.methodBuilder("getContext")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(versionContextType)
                .addJavadoc("Get the VersionContext for this wrapper's version.\n")
                .addJavadoc("<p>The context provides factory methods for creating other wrapper types\n")
                .addJavadoc("of the same protocol version.</p>\n")
                .addJavadoc("@return VersionContext for this version\n")
                .build());
    }

    // ==================== Compatibility Check Methods ====================

    private void addFieldsInaccessibleInVersion(TypeSpec.Builder builder) {
        TypeName listOfString = ParameterizedTypeName.get(
                ClassName.get(java.util.List.class),
                ClassName.get(String.class));

        builder.addMethod(MethodSpec.methodBuilder("getFieldsInaccessibleInVersion")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(listOfString)
                .addParameter(TypeName.INT, "targetVersion")
                .addJavadoc("Get fields that have values but will be inaccessible in the target version.\n")
                .addJavadoc("<p>These fields exist in the current version but not in the target version.\n")
                .addJavadoc("The data is NOT lost (protobuf preserves unknown fields), but it cannot be\n")
                .addJavadoc("accessed through the target version's API.</p>\n")
                .addJavadoc("@param targetVersion Target protocol version to check\n")
                .addJavadoc("@return List of field names that will become inaccessible\n")
                .build());
    }

    private void addCanConvertLosslesslyTo(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("canConvertLosslesslyTo")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.BOOLEAN)
                .addParameter(TypeName.INT, "targetVersion")
                .addJavadoc("Check if conversion to target version will keep all data accessible.\n")
                .addJavadoc("<p>Returns true if no populated fields will become inaccessible.</p>\n")
                .addJavadoc("@param targetVersion Target protocol version to check\n")
                .addJavadoc("@return true if all populated fields will remain accessible\n")
                .addStatement("return getFieldsInaccessibleInVersion(targetVersion).isEmpty()")
                .build());
    }

    // ==================== Builder Methods ====================

    private void addEmptyBuilder(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("emptyBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("", "Builder"))
                .addJavadoc("Create a new empty builder of the same version as this instance.\n")
                .addJavadoc("<p>Unlike {@link #toBuilder()}, this creates an empty builder without\n")
                .addJavadoc("copying any values from this instance.</p>\n")
                .addJavadoc("@return Empty builder for creating new instances\n")
                .build());
    }
}
