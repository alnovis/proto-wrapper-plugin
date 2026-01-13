package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

/**
 * Interface for generating field extraction and getter methods.
 *
 * <p>This interface is part of the ISP-compliant design where responsibilities are split:</p>
 * <ul>
 *   <li>{@link FieldExtractionHandler} - extraction and getter generation (this interface)</li>
 *   <li>{@link FieldBuilderHandler} - builder method generation</li>
 *   <li>{@link ConflictHandler} - composite interface for full handler capability</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <p>Implementations of this interface generate code for:</p>
 * <ul>
 *   <li><b>Abstract extract methods:</b> {@code extractXxx(proto)} declarations in abstract class</li>
 *   <li><b>Extract implementations:</b> Version-specific {@code extractXxx} method bodies</li>
 *   <li><b>Getter methods:</b> Public {@code getXxx()} that delegates to extract methods</li>
 * </ul>
 *
 * <h2>Method Hierarchy</h2>
 * <pre>
 * Interface:   String getName()
 *                   ↓
 * Abstract:    protected abstract String extractName(P proto)
 *              public final String getName() { return extractName(proto); }
 *                   ↓
 * Impl (V1):   protected String extractName(V1Proto proto) { return proto.getName(); }
 * Impl (V2):   protected String extractName(V2Proto proto) { return proto.getTitle(); }
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>Clients that only need to generate read-only wrapper code can depend on this
 * interface instead of the full {@link ConflictHandler}:</p>
 *
 * <pre>{@code
 * // When only extraction is needed:
 * void generateExtractMethods(FieldExtractionHandler handler, MergedField field, ProcessingContext ctx) {
 *     handler.addAbstractExtractMethods(abstractClassBuilder, field, ctx);
 *     handler.addExtractImplementation(implClassBuilder, field, presentInVersion, ctx);
 *     handler.addGetterImplementation(abstractClassBuilder, field, ctx);
 * }
 * }</pre>
 *
 * @since 1.6.5
 * @see FieldBuilderHandler
 * @see ConflictHandler
 */
public interface FieldExtractionHandler {

    /**
     * Get the type identifier for this handler.
     *
     * <p>Used for logging, debugging, and identifying which handler processed a field.</p>
     *
     * @return The handler type
     */
    HandlerType getHandlerType();

    /**
     * Determines if this handler should process the given field.
     *
     * @param field The field to check
     * @param ctx Processing context
     * @return true if this handler should handle the field
     */
    boolean handles(MergedField field, ProcessingContext ctx);

    /**
     * Add abstract extract method declarations to the abstract class.
     *
     * <p>This generates the abstract method signatures that subclasses must implement.
     * For example:</p>
     *
     * <pre>{@code
     * protected abstract String extractName(P proto);
     * protected abstract boolean extractHasName(P proto);  // for optional fields
     * }</pre>
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);

    /**
     * Add concrete extract method implementations to the implementation class.
     *
     * <p>This generates the actual method bodies that extract values from the proto.
     * For example:</p>
     *
     * <pre>{@code
     * @Override
     * protected String extractName(V1Proto proto) {
     *     return proto.getName();
     * }
     * }</pre>
     *
     * @param builder The TypeSpec builder for the implementation class
     * @param field The field being processed
     * @param presentInVersion Whether the field is present in the current version
     * @param ctx Processing context
     */
    void addExtractImplementation(TypeSpec.Builder builder, MergedField field,
                                   boolean presentInVersion, ProcessingContext ctx);

    /**
     * Add getter implementation to the abstract class.
     *
     * <p>This generates the final getter methods that delegate to extract methods.
     * For example:</p>
     *
     * <pre>{@code
     * @Override
     * public final String getName() {
     *     return extractName(proto);
     * }
     *
     * @Override
     * public final boolean hasName() {
     *     return extractHasName(proto);
     * }
     * }</pre>
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);
}
