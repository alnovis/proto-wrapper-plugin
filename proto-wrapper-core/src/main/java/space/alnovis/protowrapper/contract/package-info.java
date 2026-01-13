/**
 * Contract Matrix for field behavior definition.
 *
 * <p>This package contains the <b>single source of truth</b> for how protobuf fields
 * should behave in the generated wrapper API. The contract is defined as a matrix of:
 *
 * <ul>
 *   <li><b>Input dimensions:</b> cardinality, type category, presence, oneof membership</li>
 *   <li><b>Derived behavior:</b> has method existence, nullability, default values</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.contract.FieldContract} - The main contract record</li>
 *   <li>{@link space.alnovis.protowrapper.contract.FieldCardinality} - Singular vs repeated vs map</li>
 *   <li>{@link space.alnovis.protowrapper.contract.FieldTypeCategory} - Scalar, message, or enum</li>
 *   <li>{@link space.alnovis.protowrapper.contract.FieldPresence} - Proto2/proto3 presence semantics</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ol>
 *   <li><b>Single Source of Truth:</b> All behavior defined in one place</li>
 *   <li><b>Exhaustive Coverage:</b> Every combination has defined behavior</li>
 *   <li><b>Testability:</b> Each matrix cell = one test case</li>
 *   <li><b>Documentation:</b> Contract IS the documentation</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FieldContract contract = FieldContract.from(fieldInfo, ProtoSyntax.PROTO3);
 *
 * // All decisions derived from contract
 * if (contract.hasMethodExists()) {
 *     generateHasMethod();
 * }
 *
 * if (contract.getterUsesHasCheck()) {
 *     generateGetter("return has ? value : null");
 * } else {
 *     generateGetter("return value");
 * }
 * }</pre>
 *
 * @see space.alnovis.protowrapper.contract.FieldContract
 */
package space.alnovis.protowrapper.contract;
