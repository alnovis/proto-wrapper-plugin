package space.alnovis.protowrapper.generator.conflict;

import space.alnovis.protowrapper.model.MergedField;

/**
 * Sealed interface for handling field conflicts during code generation.
 *
 * <p>This is a composite interface that combines {@link FieldExtractionHandler} and
 * {@link FieldBuilderHandler} for handlers that need both extraction and builder capabilities.</p>
 *
 * <h2>Interface Segregation</h2>
 * <p>Following the Interface Segregation Principle (ISP), responsibilities are split:</p>
 * <ul>
 *   <li>{@link FieldExtractionHandler} - generates extraction and getter methods</li>
 *   <li>{@link FieldBuilderHandler} - generates builder methods</li>
 *   <li>{@link ConflictHandler} - composite interface for full capability (this interface)</li>
 * </ul>
 *
 * <p>Clients should depend on the narrowest interface that meets their needs:</p>
 * <ul>
 *   <li>Use {@code FieldExtractionHandler} when only generating read methods</li>
 *   <li>Use {@code FieldBuilderHandler} when only generating builder methods</li>
 *   <li>Use {@code ConflictHandler} when generating complete wrapper classes</li>
 * </ul>
 *
 * <h2>Strategy Pattern</h2>
 * <p>Each implementation handles a specific type of field conflict (INT_ENUM, STRING_BYTES, etc.)
 * and knows how to generate the appropriate code for both abstract classes and implementations.</p>
 *
 * <h2>Sealed Hierarchy</h2>
 * <p>This is a sealed interface, which provides:</p>
 * <ul>
 *   <li>Compile-time guarantee of exhaustive handling when pattern matching</li>
 *   <li>Documentation of the complete set of implementations</li>
 *   <li>Prevention of accidental external extensions</li>
 * </ul>
 *
 * <h2>Handler Selection</h2>
 * <p>Use {@link FieldProcessingChain} to find the appropriate handler for a field:</p>
 *
 * <pre>{@code
 * ConflictHandler handler = FieldProcessingChain.findHandler(field, ctx);
 * handler.addAbstractExtractMethods(builder, field, ctx);
 * handler.addGetterImplementation(builder, field, ctx);
 * // ... more generation
 * }</pre>
 *
 * @since 1.0.0
 * @see FieldExtractionHandler
 * @see FieldBuilderHandler
 * @see FieldProcessingChain
 */
public sealed interface ConflictHandler extends FieldExtractionHandler, FieldBuilderHandler permits
        IntEnumHandler, EnumEnumHandler, StringBytesHandler, WideningHandler, FloatDoubleHandler,
        SignedUnsignedHandler, RepeatedSingleHandler, PrimitiveMessageHandler,
        RepeatedConflictHandler, MapFieldHandler, WellKnownTypeHandler, RepeatedWellKnownTypeHandler,
        DefaultHandler {

    /**
     * {@inheritDoc}
     *
     * <p>Used for logging, debugging, and identifying which handler processed a field.
     * Each handler returns a unique {@link HandlerType} constant.</p>
     */
    @Override
    HandlerType getHandlerType();

    /**
     * {@inheritDoc}
     *
     * <p>The {@link FieldProcessingChain} iterates through handlers in priority order,
     * calling this method to find the first handler that can process the field.</p>
     */
    @Override
    boolean handles(MergedField field, ProcessingContext ctx);
}
