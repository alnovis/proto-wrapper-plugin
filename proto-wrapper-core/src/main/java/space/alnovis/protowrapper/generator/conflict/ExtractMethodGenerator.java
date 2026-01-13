package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.VersionFieldSnapshot;

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

    // ========== Helper Methods ==========

    /**
     * Get the FieldInfo for the current version.
     *
     * @param field the merged field
     * @param ctx the processing context containing version info
     * @return the FieldInfo for this version, or null if not present
     * @deprecated Use {@link ProcessingContext#versionSnapshot(MergedField)} instead
     */
    @Deprecated
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
     * @deprecated Use {@link MethodSpecFactory#protectedExtract(MergedField, TypeName, ProcessingContext)} instead
     */
    @Deprecated
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
     * @deprecated Use {@link MethodSpecFactory#protectedExtractHas(MergedField, ProcessingContext)} instead
     */
    @Deprecated
    public static MethodSpec.Builder createExtractHasMethodBuilder(MergedField field,
                                                                    ProcessingContext ctx) {
        return MethodSpecFactory.protectedExtractHas(field, ctx);
    }
}
