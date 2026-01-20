/**
 * Conflict handling using Chain of Responsibility pattern.
 *
 * <p>This package implements field-level code generation that handles type
 * conflicts between protobuf versions. Each handler in the chain specializes
 * in a specific conflict type.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.ConflictHandler} - Handler interface</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.FieldProcessingChain} - Chain coordinator</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.AbstractConflictHandler} - Base implementation</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.ProcessingContext} - Generation context</li>
 * </ul>
 *
 * <h2>Handler Chain</h2>
 * <p>Handlers are processed in order until one accepts the field:</p>
 * <ol>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.IntEnumHandler} - int/enum conflicts</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.StringBytesHandler} - string/bytes conflicts</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.WideningHandler} - int32/int64 widening</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.PrimitiveMessageHandler} - primitive/message conflicts</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.RepeatedConflictHandler} - repeated field conflicts</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.MapFieldHandler} - map field handling</li>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict.DefaultHandler} - fallback for all other fields</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FieldProcessingChain chain = new FieldProcessingChain();
 * ProcessingContext ctx = new ProcessingContext(config, resolver, ...);
 *
 * // Process a field with the appropriate handler
 * chain.addInterfaceGetter(typeBuilder, field, ctx);
 * chain.addAbstractExtractMethods(typeBuilder, field, ctx);
 * chain.addBuilderMethods(typeBuilder, field, ctx);
 * }</pre>
 *
 * <h2>Generated Code Structure</h2>
 * <p>Each handler generates code at multiple levels:</p>
 * <ul>
 *   <li><b>Interface:</b> Public getter methods (getXxx, hasXxx)</li>
 *   <li><b>Abstract class:</b> Template methods (extractXxx) and concrete getters</li>
 *   <li><b>Impl class:</b> Extract method implementations for each version</li>
 *   <li><b>Builder:</b> Setter methods (setXxx, clearXxx, addXxx for repeated)</li>
 * </ul>
 *
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li>Singleton handlers (stateless) - thread-safe, no allocation per field</li>
 *   <li>Context objects carry state - enables clean handler interfaces</li>
 *   <li>DefaultHandler as fallback - ensures all fields are processed</li>
 * </ul>
 *
 * @see io.alnovis.protowrapper.generator.conflict.FieldProcessingChain
 * @see io.alnovis.protowrapper.model.MergedField.ConflictType
 */
package io.alnovis.protowrapper.generator.conflict;
