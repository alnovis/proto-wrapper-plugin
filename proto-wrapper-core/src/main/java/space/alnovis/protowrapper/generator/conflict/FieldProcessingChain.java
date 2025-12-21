package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.TypeSpec;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;

import java.util.List;

/**
 * Orchestrates field processing using the chain of handlers.
 *
 * <p>This class implements the Chain of Responsibility pattern, delegating
 * field processing to the appropriate handler based on field characteristics.</p>
 *
 * <p>The chain is ordered from most specific to least specific:</p>
 * <ol>
 *   <li>IntEnumHandler - scalar INT_ENUM conflicts</li>
 *   <li>StringBytesHandler - scalar STRING_BYTES conflicts</li>
 *   <li>WideningHandler - scalar WIDENING conflicts</li>
 *   <li>PrimitiveMessageHandler - scalar PRIMITIVE_MESSAGE conflicts</li>
 *   <li>RepeatedConflictHandler - repeated fields with any type conflict</li>
 *   <li>DefaultHandler - all other fields (fallback)</li>
 * </ol>
 */
public final class FieldProcessingChain {

    private static final List<ConflictHandler> HANDLERS = List.of(
            IntEnumHandler.INSTANCE,
            StringBytesHandler.INSTANCE,
            WideningHandler.INSTANCE,
            PrimitiveMessageHandler.INSTANCE,
            RepeatedConflictHandler.INSTANCE,
            DefaultHandler.INSTANCE  // Fallback - must be last
    );

    private static final FieldProcessingChain INSTANCE = new FieldProcessingChain();

    private FieldProcessingChain() {
        // Singleton
    }

    public static FieldProcessingChain getInstance() {
        return INSTANCE;
    }

    /**
     * Find the appropriate handler for a field.
     *
     * @param field The field to process
     * @param ctx Processing context
     * @return The handler that should process this field
     */
    public ConflictHandler findHandler(MergedField field, ProcessingContext ctx) {
        for (ConflictHandler handler : HANDLERS) {
            if (handler.handles(field, ctx)) {
                return handler;
            }
        }
        // Should never reach here since DefaultHandler.handles() returns true
        return DefaultHandler.INSTANCE;
    }

    /**
     * Add abstract extract methods for all fields in a message.
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param message The message containing the fields
     * @param ctx Processing context
     */
    public void addAbstractExtractMethods(TypeSpec.Builder builder, MergedMessage message, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            ConflictHandler handler = findHandler(field, ctx);
            handler.addAbstractExtractMethods(builder, field, ctx);
        }
    }

    /**
     * Add concrete extract method implementations for all fields in a message.
     *
     * @param builder The TypeSpec builder for the implementation class
     * @param message The message containing the fields
     * @param version Current version string
     * @param ctx Processing context
     */
    public void addExtractImplementations(TypeSpec.Builder builder, MergedMessage message,
                                           String version, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            boolean presentInVersion = field.getPresentInVersions().contains(version);
            boolean hasIncompatibleConflict = hasIncompatibleTypeConflict(field, version);

            ConflictHandler handler = findHandler(field, ctx);

            // For incompatible conflicts, treat as not present
            if (hasIncompatibleConflict) {
                handler.addExtractImplementation(builder, field, false, ctx);
            } else {
                handler.addExtractImplementation(builder, field, presentInVersion, ctx);
            }
        }
    }

    /**
     * Add getter implementations for all fields in a message.
     *
     * @param builder The TypeSpec builder for the abstract class
     * @param message The message containing the fields
     * @param ctx Processing context
     */
    public void addGetterImplementations(TypeSpec.Builder builder, MergedMessage message, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            ConflictHandler handler = findHandler(field, ctx);
            handler.addGetterImplementation(builder, field, ctx);
        }
    }

    /**
     * Add abstract builder methods for all fields in a message.
     *
     * @param builder The TypeSpec builder for the abstract builder
     * @param message The message containing the fields
     * @param ctx Processing context
     */
    public void addAbstractBuilderMethods(TypeSpec.Builder builder, MergedMessage message, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            ConflictHandler handler = findHandler(field, ctx);
            handler.addAbstractBuilderMethods(builder, field, ctx);
        }
    }

    /**
     * Add concrete builder method implementations for all fields in a message.
     *
     * @param builder The TypeSpec builder for the builder implementation
     * @param message The message containing the fields
     * @param version Current version string
     * @param ctx Processing context
     */
    public void addBuilderImplMethods(TypeSpec.Builder builder, MergedMessage message,
                                       String version, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            boolean presentInVersion = field.getPresentInVersions().contains(version);
            ConflictHandler handler = findHandler(field, ctx);
            handler.addBuilderImplMethods(builder, field, presentInVersion, ctx);
        }
    }

    /**
     * Check if a field has a type conflict that makes it incompatible for this version.
     * Fields with INCOMPATIBLE conflict type or PRIMITIVE_MESSAGE (where current version
     * has incompatible type) are treated as "not present".
     */
    private boolean hasIncompatibleTypeConflict(MergedField field, String version) {
        if (field.getConflictType() == MergedField.ConflictType.INCOMPATIBLE) {
            return true;
        }
        // For other conflict types, the field is compatible
        return false;
    }
}
