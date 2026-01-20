package io.alnovis.protowrapper.generator.visitor;

import io.alnovis.protowrapper.model.MergedEnum;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;

/**
 * Visitor interface for traversing message structures.
 *
 * <p>Implements the Visitor pattern for processing merged messages and their
 * nested components. All methods have default empty implementations, allowing
 * implementors to override only the methods they need.</p>
 *
 * <p>The traversal order when using {@link MessageTraverser} is:</p>
 * <ol>
 *   <li>{@link #enterMessage(MergedMessage)} - called before processing message content</li>
 *   <li>{@link #visitField(MergedMessage, MergedField)} - called for each field</li>
 *   <li>{@link #visitNestedEnum(MergedMessage, MergedEnum)} - called for each nested enum</li>
 *   <li>Recursive traversal of nested messages</li>
 *   <li>{@link #exitMessage(MergedMessage)} - called after processing message content</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MessageVisitor fieldCounter = new MessageVisitor() {
 *     private int count = 0;
 *
 *     @Override
 *     public void visitField(MergedMessage message, MergedField field) {
 *         count++;
 *     }
 *
 *     public int getCount() { return count; }
 * };
 *
 * MessageTraverser.traverse(schema, fieldCounter);
 * System.out.println("Total fields: " + fieldCounter.getCount());
 * }</pre>
 */
public interface MessageVisitor {

    /**
     * Called when entering a message (before processing its contents).
     *
     * @param message The message being entered
     */
    default void enterMessage(MergedMessage message) {
        // Default: do nothing
    }

    /**
     * Called when exiting a message (after processing its contents).
     *
     * @param message The message being exited
     */
    default void exitMessage(MergedMessage message) {
        // Default: do nothing
    }

    /**
     * Called for each field in a message.
     *
     * @param message The parent message containing the field
     * @param field The field being visited
     */
    default void visitField(MergedMessage message, MergedField field) {
        // Default: do nothing
    }

    /**
     * Called for each nested enum in a message.
     *
     * @param message The parent message containing the enum
     * @param nestedEnum The nested enum being visited
     */
    default void visitNestedEnum(MergedMessage message, MergedEnum nestedEnum) {
        // Default: do nothing
    }

    /**
     * Called when entering a nested message.
     * This is called in addition to {@link #enterMessage(MergedMessage)}.
     *
     * @param parent The parent message
     * @param nested The nested message being entered
     */
    default void enterNestedMessage(MergedMessage parent, MergedMessage nested) {
        // Default: do nothing
    }

    /**
     * Called when exiting a nested message.
     * This is called in addition to {@link #exitMessage(MergedMessage)}.
     *
     * @param parent The parent message
     * @param nested The nested message being exited
     */
    default void exitNestedMessage(MergedMessage parent, MergedMessage nested) {
        // Default: do nothing
    }
}
