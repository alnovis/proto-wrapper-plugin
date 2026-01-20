/**
 * Code generation infrastructure for proto wrappers.
 *
 * <p>This package contains the core code generation components that produce
 * Java wrapper classes from merged protobuf schemas. It uses JavaPoet for
 * code generation and follows the Template Method pattern for extensibility.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.GenerationOrchestrator} - Main entry point
 *       that coordinates all generators</li>
 *   <li>{@link io.alnovis.protowrapper.generator.BaseGenerator} - Abstract base class
 *       providing common generator functionality</li>
 *   <li>{@link io.alnovis.protowrapper.generator.GeneratorConfig} - Configuration for
 *       generation options (output directory, packages, etc.)</li>
 *   <li>{@link io.alnovis.protowrapper.generator.TypeResolver} - Resolves protobuf types
 *       to Java types</li>
 * </ul>
 *
 * <h2>Generator Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.InterfaceGenerator} - Generates wrapper interfaces</li>
 *   <li>{@link io.alnovis.protowrapper.generator.AbstractClassGenerator} - Generates abstract wrapper classes</li>
 *   <li>{@link io.alnovis.protowrapper.generator.ImplClassGenerator} - Generates version-specific implementations</li>
 *   <li>{@link io.alnovis.protowrapper.generator.EnumGenerator} - Generates wrapper enums</li>
 *   <li>{@link io.alnovis.protowrapper.generator.ConflictEnumGenerator} - Generates enums for INT_ENUM conflicts</li>
 *   <li>{@link io.alnovis.protowrapper.generator.VersionContextGenerator} - Generates version context classes</li>
 * </ul>
 *
 * <h2>Generation Pipeline</h2>
 * <pre>
 * MergedSchema
 *     |
 *     v
 * GenerationOrchestrator
 *     |-- EnumGenerator (generates wrapper enums)
 *     |-- ConflictEnumGenerator (generates conflict resolution enums)
 *     |-- InterfaceGenerator (generates public API interfaces)
 *     |-- AbstractClassGenerator (generates abstract implementation classes)
 *     |-- ImplClassGenerator (generates version-specific implementations)
 *     +-- VersionContextGenerator (generates version context)
 *     |
 *     v
 * JavaPoet JavaFile objects
 *     |
 *     v
 * .java source files
 * </pre>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.conflict} - Conflict handling with Chain of Responsibility</li>
 *   <li>{@link io.alnovis.protowrapper.generator.builder} - Builder interface generation</li>
 *   <li>{@link io.alnovis.protowrapper.generator.oneof} - Oneof field generation</li>
 *   <li>{@link io.alnovis.protowrapper.generator.visitor} - Visitor pattern for schema traversal</li>
 * </ul>
 *
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><b>Template Method</b> - BaseGenerator defines generation skeleton</li>
 *   <li><b>Chain of Responsibility</b> - Conflict handlers (see conflict sub-package)</li>
 *   <li><b>Visitor</b> - Schema traversal (see visitor sub-package)</li>
 *   <li><b>Factory</b> - TypeResolver creates appropriate Java types</li>
 * </ul>
 *
 * @see io.alnovis.protowrapper.generator.GenerationOrchestrator
 * @see io.alnovis.protowrapper.generator.conflict.FieldProcessingChain
 */
package io.alnovis.protowrapper.generator;
