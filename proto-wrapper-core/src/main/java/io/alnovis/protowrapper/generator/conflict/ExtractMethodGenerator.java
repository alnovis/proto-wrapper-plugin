package io.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.VersionFieldSnapshot;

/**
 * Generates extract, has, and getter methods for conflict handlers.
 *
 * <p>This class extracts method generation logic from AbstractConflictHandler
 * to improve separation of concerns and reduce class size.</p>
 *
 * <h2>Generated Method Categories</h2>
 * <ul>
 *   <li><b>Abstract extract:</b> extractXxx(proto) - declared in abstract class</li>
 *   <li><b>Abstract extractHas:</b> extractHasXxx(proto) - for optional fields</li>
 *   <li><b>Impl extract:</b> extractXxx implementations per version</li>
 *   <li><b>Getter:</b> getXxx() in abstract class - delegates to extractXxx</li>
 *   <li><b>Has:</b> hasXxx() in abstract class - delegates to extractHasXxx</li>
 * </ul>
 *
 * <h2>Method Hierarchy</h2>
 * <pre>
 * Interface:     String getName();           boolean hasName();
 *                    ↓                             ↓
 * Abstract:      extractName(proto)          extractHasName(proto)
 *                    ↓                             ↓
 * Impl (V1):     proto.getName()             proto.hasName()
 * Impl (V2):     proto.getTitle()            proto.hasTitle()
 * </pre>
 *
 * @since 1.6.5
 * @see AbstractConflictHandler
 * @see ConflictHandler
 */
public final class ExtractMethodGenerator {

    private ExtractMethodGenerator() {
        // Utility class - no instantiation
    }

    // ========== Abstract Method Declarations ==========

    /**
     * Add an abstract extractHas method for optional fields.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    public static void addAbstractHasMethod(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.shouldGenerateHasMethod()) {
            builder.addMethod(MethodSpecFactory.protectedAbstractExtractHas(field, ctx).build());
        }
    }

    /**
     * Add an abstract extract method.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param ctx the processing context
     */
    public static void addAbstractExtractMethod(TypeSpec.Builder builder, MergedField field,
                                                 TypeName returnType, ProcessingContext ctx) {
        builder.addMethod(MethodSpecFactory.protectedAbstractExtract(field, returnType, ctx).build());
    }

    // ========== Impl Method Implementations ==========

    /**
     * Add a concrete has method implementation for present fields.
     * In proto3, scalar fields without 'optional' modifier do not have has*() methods,
     * so we return false for those fields.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param versionJavaName the Java name for this version
     * @param ctx the processing context
     */
    public static void addHasMethodImpl(TypeSpec.Builder builder, MergedField field,
                                         String versionJavaName, ProcessingContext ctx) {
        if (field.shouldGenerateHasMethod()) {
            VersionFieldSnapshot snapshot = ctx.versionSnapshot(field);

            MethodSpec.Builder method = MethodSpecFactory.protectedExtractHas(field, ctx);

            if (snapshot.supportsHasMethod()) {
                method.addStatement("return proto.has$L()", versionJavaName);
            } else {
                // Proto3 scalar field without 'optional' - no has*() method, always return false
                method.addJavadoc("Proto3 scalar field without 'optional' modifier - has*() not available.\n");
                method.addStatement("return false");
            }

            builder.addMethod(method.build());
        }
    }

    /**
     * Add a concrete has method implementation returning false (field not present).
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    public static void addMissingHasMethodImpl(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.shouldGenerateHasMethod()) {
            builder.addMethod(MethodSpecFactory.protectedExtractHas(field, ctx)
                    .addStatement("return false")
                    .addJavadoc("Field not present in this version.\n")
                    .build());
        }
    }

    // ========== Getter Methods (in Abstract Class) ==========

    /**
     * Add a getter implementation that delegates to extract methods.
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param returnType the return type for the getter
     * @param ctx the processing context
     */
    public static void addStandardGetterImpl(TypeSpec.Builder builder, MergedField field,
                                              TypeName returnType, ProcessingContext ctx) {
        MethodSpec.Builder getter = MethodSpecFactory.publicFinalGetter(field, returnType);

        if (field.needsHasCheck()) {
            getter.addStatement("return $L(proto) ? $L(proto) : null",
                    field.getExtractHasMethodName(), field.getExtractMethodName());
        } else {
            getter.addStatement("return $L(proto)", field.getExtractMethodName());
        }

        builder.addMethod(getter.build());
    }

    /**
     * Add has method implementation to abstract class.
     * This generates public hasXxx() that delegates to extractHasXxx().
     *
     * @param builder the TypeSpec builder to add the method to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    public static void addHasMethodToAbstract(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx) {
        if (field.shouldGenerateHasMethod()) {
            String methodName = MethodSpecFactory.methodName("has", field, ctx);
            MethodSpec has = MethodSpecFactory.publicFinalHas(methodName)
                    .addStatement("return $L(proto)", field.getExtractHasMethodName())
                    .build();
            builder.addMethod(has);
        }
    }

    // ========== Missing Field Methods ==========

    /**
     * Add extract and optionally has method implementations for a field not present in this version.
     * This is a common pattern used across multiple handlers.
     *
     * <p>Generates:</p>
     * <ul>
     *   <li>extractXxx(proto) - returns the provided default value</li>
     *   <li>extractHasXxx(proto) - returns false (if field supports has method)</li>
     * </ul>
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param defaultValueFormat the format string for the default value (e.g., "0L", "null", "$T.emptyList()")
     * @param defaultValueArgs arguments for the format string (e.g., Collections.class)
     * @param ctx the processing context
     */
    public static void addMissingFieldExtract(TypeSpec.Builder builder, MergedField field,
                                               TypeName returnType, String defaultValueFormat,
                                               Object[] defaultValueArgs, ProcessingContext ctx) {
        // Add missing has method
        addMissingHasMethodImpl(builder, field, ctx);

        // Add missing extract method
        MethodSpec.Builder extract = MethodSpecFactory.protectedExtract(field, returnType, ctx)
                .addJavadoc("Field not present in this version.\n");

        if (defaultValueArgs != null && defaultValueArgs.length > 0) {
            extract.addStatement("return " + defaultValueFormat, defaultValueArgs);
        } else {
            extract.addStatement("return " + defaultValueFormat);
        }

        builder.addMethod(extract.build());
    }

    /**
     * Add extract and optionally has method for missing field with simple default value.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param returnType the return type for the extract method
     * @param defaultValue the default value literal (e.g., "0", "0L", "null", "\"\"")
     * @param ctx the processing context
     */
    public static void addMissingFieldExtract(TypeSpec.Builder builder, MergedField field,
                                               TypeName returnType, String defaultValue,
                                               ProcessingContext ctx) {
        addMissingFieldExtract(builder, field, returnType, defaultValue, null, ctx);
    }

    /**
     * Add extract and optionally has method for missing field with auto-resolved default value.
     * Uses {@code ctx.resolver().getDefaultValue(field.getGetterType())} to determine the default.
     *
     * @param builder the TypeSpec builder to add methods to
     * @param field the merged field definition
     * @param ctx the processing context
     */
    public static void addMissingFieldExtractWithResolvedDefault(TypeSpec.Builder builder,
                                                                   MergedField field,
                                                                   ProcessingContext ctx) {
        TypeName returnType = ctx.parseFieldType(field);
        String defaultValue = ctx.resolver().getDefaultValue(field.getGetterType());
        addMissingFieldExtract(builder, field, returnType, defaultValue, ctx);
    }

    // ========== Helper Methods ==========

    /**
     * Get the FieldInfo for the current version.
     *
     * @param field the merged field
     * @param ctx the processing context containing version info
     * @return the FieldInfo for this version, or null if not present
     * @deprecated Since 2.1.0. Use {@link ProcessingContext#versionSnapshot(MergedField)} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public static FieldInfo getVersionField(MergedField field, ProcessingContext ctx) {
        String version = ctx.version();
        if (version == null) return null;
        return VersionFieldSnapshot.of(field, version).fieldInfo();
    }

    /**
     * Create a basic extract method builder with standard configuration.
     *
     * @param field the merged field definition
     * @param returnType the return type for the method
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     * @deprecated Since 2.1.0. Use {@link MethodSpecFactory#protectedExtract(MergedField, TypeName, ProcessingContext)} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public static MethodSpec.Builder createExtractMethodBuilder(MergedField field,
                                                                 TypeName returnType,
                                                                 ProcessingContext ctx) {
        return MethodSpecFactory.protectedExtract(field, returnType, ctx);
    }

    /**
     * Create a basic extractHas method builder with standard configuration.
     *
     * @param field the merged field definition
     * @param ctx the processing context
     * @return configured MethodSpec.Builder
     * @deprecated Since 2.1.0. Use {@link MethodSpecFactory#protectedExtractHas(MergedField, ProcessingContext)} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public static MethodSpec.Builder createExtractHasMethodBuilder(MergedField field,
                                                                    ProcessingContext ctx) {
        return MethodSpecFactory.protectedExtractHas(field, ctx);
    }
}
