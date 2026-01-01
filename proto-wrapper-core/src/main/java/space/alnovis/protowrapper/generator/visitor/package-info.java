/**
 * Visitor pattern implementation for schema traversal.
 *
 * <p>This package provides a Visitor pattern implementation for traversing
 * merged protobuf schemas. It enables decoupled processing of schema elements
 * without modifying the model classes.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link space.alnovis.protowrapper.generator.visitor.MessageVisitor} -
 *       Visitor interface with default empty implementations</li>
 *   <li>{@link space.alnovis.protowrapper.generator.visitor.MessageTraverser} -
 *       Traverses schema and invokes visitor methods</li>
 *   <li>{@link space.alnovis.protowrapper.generator.visitor.FieldCollector} -
 *       Collects all fields from a schema</li>
 *   <li>{@link space.alnovis.protowrapper.generator.visitor.ConflictReporter} -
 *       Reports type conflicts found in schema</li>
 *   <li>{@link space.alnovis.protowrapper.generator.visitor.SchemaStats} -
 *       Collects statistics about schema</li>
 * </ul>
 *
 * <h2>Traversal Order</h2>
 * <p>The traverser visits elements in this order:</p>
 * <ol>
 *   <li>{@code enterMessage(message)} - before processing message content</li>
 *   <li>{@code visitField(message, field)} - for each field</li>
 *   <li>{@code visitNestedEnum(message, enum)} - for each nested enum</li>
 *   <li>Recursive traversal of nested messages</li>
 *   <li>{@code exitMessage(message)} - after processing message content</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Count all fields with conflicts
 * MessageVisitor conflictCounter = new MessageVisitor() {
 *     private int count = 0;
 *
 *     @Override
 *     public void visitField(MergedMessage message, MergedField field) {
 *         if (field.getConflictType() != ConflictType.NONE) {
 *             count++;
 *         }
 *     }
 *
 *     public int getCount() { return count; }
 * };
 *
 * MessageTraverser.traverse(schema, conflictCounter);
 * System.out.println("Fields with conflicts: " + conflictCounter.getCount());
 * }</pre>
 *
 * <h2>Built-in Visitors</h2>
 * <ul>
 *   <li><b>FieldCollector</b> - Collects all fields into a list</li>
 *   <li><b>ConflictReporter</b> - Generates human-readable conflict report</li>
 *   <li><b>SchemaStats</b> - Gathers statistics (message count, field count, etc.)</li>
 * </ul>
 *
 * @see space.alnovis.protowrapper.generator.visitor.MessageVisitor
 * @see space.alnovis.protowrapper.generator.visitor.MessageTraverser
 */
package space.alnovis.protowrapper.generator.visitor;
