package io.alnovis.protowrapper.generator.visitor;

import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Traverses message structures and invokes visitor callbacks.
 *
 * <p>This class implements the traversal logic for the Visitor pattern,
 * walking through messages, fields, nested messages, and nested enums
 * in a defined order.</p>
 *
 * <p>The traversal is depth-first, processing all content of a message
 * before moving to sibling messages.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Traverse a single message
 * MessageTraverser.traverse(message, new MessageVisitor() {
 *     @Override
 *     public void visitField(MergedMessage message, MergedField field) {
 *         System.out.println(message.getName() + "." + field.getName());
 *     }
 * });
 *
 * // Traverse all messages in a schema
 * MessageTraverser.traverse(schema, visitor);
 * }</pre>
 */
public final class MessageTraverser {

    private MessageTraverser() {
        // Utility class - prevent instantiation
    }

    /**
     * Traverse all messages in a schema.
     *
     * @param schema The schema containing messages to traverse
     * @param visitor The visitor to invoke callbacks on
     */
    public static void traverse(MergedSchema schema, MessageVisitor visitor) {
        schema.getMessages().forEach(message -> traverse(message, visitor));
    }

    /**
     * Traverse a single message and all its nested content.
     *
     * @param message The message to traverse
     * @param visitor The visitor to invoke callbacks on
     */
    public static void traverse(MergedMessage message, MessageVisitor visitor) {
        traverseInternal(message, null, visitor);
    }

    /**
     * Traverse a message without recursing into nested messages.
     * Only visits fields and nested enums of the given message.
     *
     * @param message The message to traverse (shallow)
     * @param visitor The visitor to invoke callbacks on
     */
    public static void traverseShallow(MergedMessage message, MessageVisitor visitor) {
        visitor.enterMessage(message);
        message.getFieldsSorted().forEach(field -> visitor.visitField(message, field));
        message.getNestedEnums().forEach(nestedEnum -> visitor.visitNestedEnum(message, nestedEnum));
        visitor.exitMessage(message);
    }

    /**
     * Traverse only the fields of a message (no enter/exit callbacks, no nested content).
     *
     * @param message The message whose fields to traverse
     * @param fieldVisitor Consumer for each field
     */
    public static void traverseFields(MergedMessage message,
                                       java.util.function.BiConsumer<MergedMessage, io.alnovis.protowrapper.model.MergedField> fieldVisitor) {
        message.getFieldsSorted().forEach(field -> fieldVisitor.accept(message, field));
    }

    /**
     * Traverse all messages in a schema iteratively (non-recursive).
     * Useful when stack depth is a concern for deeply nested messages.
     *
     * @param schema The schema containing messages to traverse
     * @param visitor The visitor to invoke callbacks on
     */
    public static void traverseIterative(MergedSchema schema, MessageVisitor visitor) {
        Deque<TraversalState> stack = new ArrayDeque<>();

        // Add all top-level messages to the stack
        schema.getMessages().forEach(msg -> stack.push(new TraversalState(msg, null, Phase.ENTER)));

        while (!stack.isEmpty()) {
            TraversalState state = stack.pop();

            switch (state.phase) {
                case ENTER -> {
                    if (state.parent != null) {
                        visitor.enterNestedMessage(state.parent, state.message);
                    }
                    visitor.enterMessage(state.message);

                    // Push exit state first (will be processed last)
                    stack.push(new TraversalState(state.message, state.parent, Phase.EXIT));

                    // Push nested messages (in reverse order so they're processed in order)
                    var nestedMessages = state.message.getNestedMessages();
                    for (int i = nestedMessages.size() - 1; i >= 0; i--) {
                        stack.push(new TraversalState(nestedMessages.get(i), state.message, Phase.ENTER));
                    }

                    // Process fields and enums immediately
                    state.message.getFieldsSorted().forEach(
                            field -> visitor.visitField(state.message, field));
                    state.message.getNestedEnums().forEach(
                            nestedEnum -> visitor.visitNestedEnum(state.message, nestedEnum));
                }
                case EXIT -> {
                    visitor.exitMessage(state.message);
                    if (state.parent != null) {
                        visitor.exitNestedMessage(state.parent, state.message);
                    }
                }
            }
        }
    }

    /**
     * Internal recursive traversal.
     */
    private static void traverseInternal(MergedMessage message, MergedMessage parent, MessageVisitor visitor) {
        // Notify nested message entry if this is a nested message
        if (parent != null) {
            visitor.enterNestedMessage(parent, message);
        }

        // Enter message
        visitor.enterMessage(message);

        // Visit fields
        message.getFieldsSorted().forEach(field -> visitor.visitField(message, field));

        // Visit nested enums
        message.getNestedEnums().forEach(nestedEnum -> visitor.visitNestedEnum(message, nestedEnum));

        // Recursively traverse nested messages
        message.getNestedMessages().forEach(nested -> traverseInternal(nested, message, visitor));

        // Exit message
        visitor.exitMessage(message);

        // Notify nested message exit if this is a nested message
        if (parent != null) {
            visitor.exitNestedMessage(parent, message);
        }
    }

    /**
     * Internal state for iterative traversal.
     */
    private record TraversalState(MergedMessage message, MergedMessage parent, Phase phase) {}

    private enum Phase { ENTER, EXIT }
}
