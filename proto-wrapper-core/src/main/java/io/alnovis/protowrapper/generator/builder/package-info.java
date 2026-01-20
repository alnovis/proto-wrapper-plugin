/**
 * Builder interface generation for wrapper messages.
 *
 * <p>This package contains the generator for Builder interfaces that provide
 * fluent APIs for constructing wrapper instances.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.builder.BuilderInterfaceGenerator} -
 *       Generates Builder interface with setter methods for all fields</li>
 * </ul>
 *
 * <h2>Generated Builder Structure</h2>
 * <p>For each message, a nested Builder interface is generated:</p>
 * <pre>{@code
 * public interface PersonWrapper {
 *     // Getters...
 *
 *     interface Builder {
 *         Builder setName(String name);
 *         Builder setAge(int age);
 *         Builder addTags(String tag);
 *         Builder clearTags();
 *         PersonWrapper build();
 *     }
 *
 *     Builder toBuilder();
 *     static Builder builder(VersionContext ctx) { ... }
 * }
 * }</pre>
 *
 * <h2>Builder Methods by Field Type</h2>
 * <ul>
 *   <li><b>Scalar fields:</b> setXxx(value), clearXxx()</li>
 *   <li><b>Optional fields:</b> setXxx(value), clearXxx(), hasXxx()</li>
 *   <li><b>Repeated fields:</b> addXxx(item), addAllXxx(list), setXxx(list), clearXxx()</li>
 *   <li><b>Map fields:</b> putXxx(key, value), putAllXxx(map), clearXxx()</li>
 *   <li><b>Oneof fields:</b> setXxx(value), clearXxxCase()</li>
 * </ul>
 *
 * <h2>Conflict Handling in Builders</h2>
 * <p>Fields with type conflicts may have modified setter signatures or
 * additional validation. See {@link io.alnovis.protowrapper.generator.conflict}
 * for details on how each conflict type affects builder methods.</p>
 *
 * @see io.alnovis.protowrapper.generator.InterfaceGenerator
 * @see io.alnovis.protowrapper.generator.conflict.FieldProcessingChain
 */
package io.alnovis.protowrapper.generator.builder;
