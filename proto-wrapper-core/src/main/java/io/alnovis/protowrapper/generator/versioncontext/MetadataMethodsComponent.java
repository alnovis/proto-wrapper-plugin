package io.alnovis.protowrapper.generator.versioncontext;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.runtime.SchemaInfo;
import io.alnovis.protowrapper.runtime.VersionSchemaDiff;

import javax.lang.model.element.Modifier;
import java.util.Optional;

/**
 * Component that generates schema metadata methods for VersionContext interface.
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>getSchemaInfo() - method to get schema metadata (enums, messages)</li>
 *   <li>getDiffFrom(String) - method to get diff from another version</li>
 * </ul>
 *
 * <p>These methods are only added when generateSchemaMetadata is enabled.</p>
 *
 * @since 2.3.0
 */
public class MetadataMethodsComponent implements InterfaceComponent {

    private final GeneratorConfig config;

    /**
     * Create a new MetadataMethodsComponent.
     *
     * @param config generator configuration
     */
    public MetadataMethodsComponent(GeneratorConfig config) {
        this.config = config;
    }

    @Override
    public void addTo(TypeSpec.Builder builder) {
        if (!config.isGenerateSchemaMetadata()) {
            return;
        }

        // getSchemaInfo() method
        builder.addMethod(MethodSpec.methodBuilder("getSchemaInfo")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(SchemaInfo.class)
                .addJavadoc("Get schema metadata for this version.\n\n")
                .addJavadoc("<p>Provides runtime access to enum values and message field information.</p>\n\n")
                .addJavadoc("<p>Example usage:</p>\n")
                .addJavadoc("<pre>{@code\n")
                .addJavadoc("SchemaInfo schema = ctx.getSchemaInfo();\n")
                .addJavadoc("schema.getEnum(\"TaxTypeEnum\").ifPresent(e -> {\n")
                .addJavadoc("    for (SchemaInfo.EnumValue v : e.getValues()) {\n")
                .addJavadoc("        System.out.println(v.name() + \" = \" + v.number());\n")
                .addJavadoc("    }\n")
                .addJavadoc("});\n")
                .addJavadoc("}</pre>\n\n")
                .addJavadoc("@return schema metadata for this version\n")
                .addJavadoc("@since 2.3.0\n")
                .build());

        // getDiffFrom(String previousVersion) method
        ParameterizedTypeName optionalDiffType = ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                ClassName.get(VersionSchemaDiff.class));

        builder.addMethod(MethodSpec.methodBuilder("getDiffFrom")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(optionalDiffType)
                .addParameter(String.class, "previousVersion")
                .addJavadoc("Get schema diff from a previous version to this version.\n\n")
                .addJavadoc("<p>Returns information about field and enum changes between versions,\n")
                .addJavadoc("including migration hints.</p>\n\n")
                .addJavadoc("<p>Example usage:</p>\n")
                .addJavadoc("<pre>{@code\n")
                .addJavadoc("VersionContext ctx = VersionContext.forVersionId(ProtocolVersions.V203);\n")
                .addJavadoc("ctx.getDiffFrom(ProtocolVersions.V202).ifPresent(diff -> {\n")
                .addJavadoc("    diff.findFieldChange(\"Tax\", \"type\").ifPresent(fc -> {\n")
                .addJavadoc("        System.out.println(\"Migration hint: \" + fc.migrationHint());\n")
                .addJavadoc("    });\n")
                .addJavadoc("});\n")
                .addJavadoc("}</pre>\n\n")
                .addJavadoc("@param previousVersion the version to diff from (e.g., \"v202\")\n")
                .addJavadoc("@return optional containing diff if available, empty if no diff exists\n")
                .addJavadoc("@since 2.3.0\n")
                .build());
    }
}
