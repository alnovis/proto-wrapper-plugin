/**
 * Proto Wrapper Plugin - Version-agnostic protobuf wrapper generator.
 *
 * <p>This library generates Java wrapper classes that provide a unified API
 * for working with multiple versions of protobuf schemas. Application code
 * can work with any protocol version through type-safe interfaces.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Version-agnostic interfaces for all message types</li>
 *   <li>Automatic type conflict resolution (int/enum, string/bytes, widening)</li>
 *   <li>Immutable wrapper instances with fluent builders</li>
 *   <li>Seamless version conversion</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.analyzer} - Proto file parsing</li>
 *   <li>{@link space.alnovis.protowrapper.merger} - Version schema merging</li>
 *   <li>{@link space.alnovis.protowrapper.model} - Domain model classes</li>
 *   <li>{@link space.alnovis.protowrapper.generator} - Code generation</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>The library is typically used through Maven or Gradle plugins that invoke
 * the generation pipeline automatically during build.</p>
 *
 * <h2>Entry Points</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.generator.GenerationOrchestrator} - Main generation coordinator</li>
 *   <li>{@link space.alnovis.protowrapper.merger.VersionMerger} - Schema merging</li>
 *   <li>{@link space.alnovis.protowrapper.analyzer.ProtoAnalyzer} - Proto parsing</li>
 * </ul>
 *
 * @see space.alnovis.protowrapper.generator.GenerationOrchestrator
 */
package space.alnovis.protowrapper;
