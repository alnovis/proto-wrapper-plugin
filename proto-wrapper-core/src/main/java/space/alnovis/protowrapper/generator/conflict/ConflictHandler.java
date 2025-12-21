package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;

/**
 * Sealed interface for handling field conflicts during code generation.
 *
 * <p>Each implementation handles a specific type of field conflict (INT_ENUM, STRING_BYTES, etc.)
 * and knows how to generate the appropriate code for both abstract classes and implementations.</p>
 *
 * <p>This follows the Strategy pattern, allowing the field processing logic to be
 * cleanly separated from the generators.</p>
 *
 * <p>This is a sealed interface, which provides:</p>
 * <ul>
 *   <li>Compile-time guarantee of exhaustive handling when pattern matching</li>
 *   <li>Documentation of the complete set of implementations</li>
 *   <li>Prevention of accidental external extensions</li>
 * </ul>
 *
 * @see FieldProcessingChain
 */
public sealed interface ConflictHandler permits
        IntEnumHandler, StringBytesHandler, WideningHandler,
        PrimitiveMessageHandler, RepeatedConflictHandler, DefaultHandler {

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
     * <p>This generates the abstract method signatures that subclasses must implement.</p>
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addAbstractExtractMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);

    /**
     * Add concrete extract method implementations to the implementation class.
     *
     * <p>This generates the actual method bodies that extract values from the proto.</p>
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
     * <p>This generates the final getter methods that delegate to extract methods.</p>
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addGetterImplementation(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);

    /**
     * Add abstract builder method declarations to the abstract builder class.
     *
     * @param builder The TypeSpec builder for the abstract builder
     * @param field The field being processed
     * @param ctx Processing context
     */
    void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedField field, ProcessingContext ctx);

    /**
     * Add concrete builder method implementations to the builder implementation class.
     *
     * @param builder The TypeSpec builder for the builder implementation
     * @param field The field being processed
     * @param presentInVersion Whether the field is present in the current version
     * @param ctx Processing context
     */
    void addBuilderImplMethods(TypeSpec.Builder builder, MergedField field,
                                boolean presentInVersion, ProcessingContext ctx);
}
