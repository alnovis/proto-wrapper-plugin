package space.alnovis.protowrapper.generator.conflict;

import com.squareup.javapoet.TypeName;
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
 *   <li>IntEnumHandler - scalar INT_ENUM conflicts (int ↔ enum)</li>
 *   <li>EnumEnumHandler - scalar ENUM_ENUM conflicts (different enum types)</li>
 *   <li>StringBytesHandler - scalar STRING_BYTES conflicts</li>
 *   <li>WideningHandler - scalar WIDENING conflicts (int to long)</li>
 *   <li>FloatDoubleHandler - scalar FLOAT_DOUBLE conflicts (float to double)</li>
 *   <li>SignedUnsignedHandler - scalar SIGNED_UNSIGNED conflicts (int32 vs uint32, etc.)</li>
 *   <li>RepeatedSingleHandler - repeated ↔ singular conflicts</li>
 *   <li>PrimitiveMessageHandler - scalar PRIMITIVE_MESSAGE conflicts</li>
 *   <li>RepeatedConflictHandler - repeated fields with any type conflict</li>
 *   <li>WellKnownTypeHandler - scalar well-known type fields (Timestamp, Duration, etc.)</li>
 *   <li>RepeatedWellKnownTypeHandler - repeated well-known type fields</li>
 *   <li>MapFieldHandler - map fields</li>
 *   <li>DefaultHandler - all other fields (fallback)</li>
 * </ol>
 */
public final class FieldProcessingChain {

    private static final List<ConflictHandler> HANDLERS = List.of(
            IntEnumHandler.INSTANCE,
            EnumEnumHandler.INSTANCE,              // enum ↔ enum conflicts (different enum types)
            StringBytesHandler.INSTANCE,
            WideningHandler.INSTANCE,
            FloatDoubleHandler.INSTANCE,           // float ↔ double conflicts
            SignedUnsignedHandler.INSTANCE,        // int32 ↔ uint32, sint32, etc.
            RepeatedSingleHandler.INSTANCE,        // repeated ↔ singular conflicts
            PrimitiveMessageHandler.INSTANCE,
            RepeatedConflictHandler.INSTANCE,
            WellKnownTypeHandler.INSTANCE,         // well-known type conversion (scalar)
            RepeatedWellKnownTypeHandler.INSTANCE, // well-known type conversion (repeated)
            MapFieldHandler.INSTANCE,              // Map fields handler
            DefaultHandler.INSTANCE                // Fallback - must be last
    );

    private static final FieldProcessingChain INSTANCE = new FieldProcessingChain();

    private FieldProcessingChain() {
        // Singleton
    }

    /**
     * Get the singleton instance.
     *
     * @return the field processing chain instance
     */
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
     * Add concrete builder interface methods for all fields in a message.
     *
     * <p>These are the public final methods (setXxx, clearXxx, addXxx) that implement
     * the Builder interface and delegate to the abstract doXxx methods.</p>
     *
     * @param builder The TypeSpec builder for the abstract builder
     * @param message The message containing the fields
     * @param builderReturnType The return type for fluent builder pattern (Builder interface type)
     * @param ctx Processing context
     */
    public void addConcreteBuilderMethods(TypeSpec.Builder builder, MergedMessage message,
                                           TypeName builderReturnType, ProcessingContext ctx) {
        for (MergedField field : message.getFieldsSorted()) {
            ConflictHandler handler = findHandler(field, ctx);
            handler.addConcreteBuilderMethods(builder, field, builderReturnType, ctx);
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
