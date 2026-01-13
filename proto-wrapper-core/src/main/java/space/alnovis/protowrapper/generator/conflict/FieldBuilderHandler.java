package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

/**
 * Interface for generating builder methods for a field.
 *
 * <p>This interface is part of the ISP-compliant design where responsibilities are split:</p>
 * <ul>
 *   <li>{@link FieldExtractionHandler} - extraction and getter generation</li>
 *   <li>{@link FieldBuilderHandler} - builder method generation (this interface)</li>
 *   <li>{@link ConflictHandler} - composite interface for full handler capability</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <p>Implementations of this interface generate code for:</p>
 * <ul>
 *   <li><b>Abstract builder methods:</b> {@code doSetXxx}, {@code doClearXxx} declarations</li>
 *   <li><b>Builder impl methods:</b> Version-specific {@code doSetXxx} implementations</li>
 *   <li><b>Concrete builder methods:</b> Public {@code setXxx()}, {@code clearXxx()} that delegate</li>
 * </ul>
 *
 * <h2>Method Hierarchy</h2>
 * <pre>
 * Builder Interface:   Builder setName(String name)
 *                           ↓
 * Abstract Builder:    protected abstract void doSetName(String name)
 *                      public final Builder setName(String name) {
 *                          doSetName(name);
 *                          return this;
 *                      }
 *                           ↓
 * Impl Builder (V1):   protected void doSetName(String name) { protoBuilder.setName(name); }
 * Impl Builder (V2):   protected void doSetName(String name) { protoBuilder.setTitle(name); }
 * </pre>
 *
 * <h2>Scalar vs Repeated Fields</h2>
 * <p>Different method patterns are generated based on field type:</p>
 *
 * <h3>Scalar fields generate:</h3>
 * <ul>
 *   <li>{@code doSetXxx(value)} / {@code setXxx(value)}</li>
 *   <li>{@code doClearXxx()} / {@code clearXxx()} - for optional fields</li>
 * </ul>
 *
 * <h3>Repeated fields generate:</h3>
 * <ul>
 *   <li>{@code doAddXxx(element)} / {@code addXxx(element)}</li>
 *   <li>{@code doAddAllXxx(list)} / {@code addAllXxx(list)}</li>
 *   <li>{@code doSetXxx(list)} / {@code setXxx(list)} - replace all</li>
 *   <li>{@code doClearXxx()} / {@code clearXxx()}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Clients that need to generate builder code can use this interface:</p>
 *
 * <pre>{@code
 * void generateBuilderMethods(FieldBuilderHandler handler, MergedField field,
 *                              TypeName builderType, ProcessingContext ctx) {
 *     handler.addAbstractBuilderMethods(abstractBuilderBuilder, field, ctx);
 *     handler.addBuilderImplMethods(implBuilderBuilder, field, presentInVersion, ctx);
 *     handler.addConcreteBuilderMethods(abstractBuilderBuilder, field, builderType, ctx);
 * }
 * }</pre>
 *
 * @since 1.6.5
 * @see FieldExtractionHandler
 * @see ConflictHandler
 */
public interface FieldBuilderHandler {

    /**
     * Add abstract builder method declarations to the abstract builder class.
     *
     * <p>This generates the abstract method signatures for builder operations.
     * These are the internal methods that version-specific builders must implement:</p>
     *
     * <pre>{@code
     * // For scalar fields:
     * protected abstract void doSetName(String name);
     * protected abstract void doClearName();
     *
     * // For repeated fields:
     * protected abstract void doAddItem(Item item);
     * protected abstract void doAddAllItem(List<Item> items);
     * protected abstract void doSetItem(List<Item> items);
     * protected abstract void doClearItem();
     * }</pre>
     *
     * @param builder The TypeSpec builder for the abstract builder
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);

    /**
     * Add concrete builder method implementations to the builder implementation class.
     *
     * <p>This generates the override implementations of the abstract doSetXxx methods
     * that actually call the proto builder:</p>
     *
     * <pre>{@code
     * @Override
     * protected void doSetName(String name) {
     *     protoBuilder.setName(name);  // or setTitle() for v2
     * }
     *
     * @Override
     * protected void doClearName() {
     *     protoBuilder.clearName();
     * }
     * }</pre>
     *
     * <p>For fields not present in a version, the implementation may either:</p>
     * <ul>
     *   <li>Silently ignore the operation (with a comment)</li>
     *   <li>Throw {@code UnsupportedOperationException}</li>
     * </ul>
     *
     * @param builder The TypeSpec builder for the builder implementation
     * @param field The field being processed
     * @param presentInVersion Whether the field is present in the current version
     * @param ctx Processing context
     */
    void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                boolean presentInVersion, ProcessingContext ctx);

    /**
     * Add concrete builder interface methods to the abstract builder class.
     *
     * <p>This generates the public final methods (setXxx, clearXxx, addXxx) that
     * implement the Builder interface and delegate to the abstract doXxx methods:</p>
     *
     * <pre>{@code
     * @Override
     * public final Builder setName(String name) {
     *     doSetName(name);
     *     return this;
     * }
     *
     * @Override
     * public final Builder clearName() {
     *     doClearName();
     *     return this;
     * }
     * }</pre>
     *
     * @param builder The TypeSpec builder for the abstract builder
     * @param field The field being processed
     * @param builderReturnType The return type for fluent builder pattern (Builder interface type)
     * @param ctx Processing context
     */
    void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedField field,
                                    TypeName builderReturnType, ProcessingContext ctx);
}
