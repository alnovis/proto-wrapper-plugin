package space.alnovis.protowrapper.generator.visitor;

import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Collects fields from a schema based on predicates using the visitor pattern.
 *
 * <p>This utility provides a convenient way to find all fields matching
 * certain criteria without manually traversing the message hierarchy.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Find all repeated fields
 * List<FieldWithContext> repeatedFields = FieldCollector.collect(schema,
 *     field -> field.isRepeated());
 *
 * // Find all fields with type conflicts
 * List<FieldWithContext> conflictFields = FieldCollector.collect(schema,
 *     field -> field.hasTypeConflict());
 *
 * // Find fields in specific messages
 * List<FieldWithContext> fields = FieldCollector.collect(schema,
 *     (message, field) -> message.getName().equals("Person") && field.isOptional());
 * }</pre>
 */
public final class FieldCollector {

    private FieldCollector() {
        // Utility class
    }

    /**
     * Field with its parent message context.
     */
    public record FieldWithContext(MergedMessage message, MergedField field) {

        /**
         * Get the fully qualified field path (e.g., "Parent.Nested.fieldName").
         */
        public String getQualifiedPath() {
            StringBuilder path = new StringBuilder();
            buildMessagePath(message, path);
            path.append(".").append(field.getName());
            return path.toString();
        }

        private void buildMessagePath(MergedMessage msg, StringBuilder path) {
            if (msg.getParent() != null) {
                buildMessagePath(msg.getParent(), path);
                path.append(".");
            }
            path.append(msg.getName());
        }

        @Override
        public String toString() {
            return getQualifiedPath();
        }
    }

    /**
     * Collect all fields matching the predicate.
     *
     * @param schema The schema to search
     * @param predicate Field filter predicate
     * @return List of matching fields with their message context
     */
    public static List<FieldWithContext> collect(MergedSchema schema, Predicate<MergedField> predicate) {
        return collect(schema, (msg, field) -> predicate.test(field));
    }

    /**
     * Collect all fields matching the predicate (with message context).
     *
     * @param schema The schema to search
     * @param predicate Filter predicate that receives both message and field
     * @return List of matching fields with their message context
     */
    public static List<FieldWithContext> collect(MergedSchema schema,
                                                   BiPredicate<MergedMessage, MergedField> predicate) {
        List<FieldWithContext> result = new ArrayList<>();
        MessageTraverser.traverse(schema, new MessageVisitor() {
            @Override
            public void visitField(MergedMessage message, MergedField field) {
                if (predicate.test(message, field)) {
                    result.add(new FieldWithContext(message, field));
                }
            }
        });
        return result;
    }

    /**
     * Collect all fields from a single message (including nested).
     *
     * @param message The message to search
     * @param predicate Field filter predicate
     * @return List of matching fields with their message context
     */
    public static List<FieldWithContext> collect(MergedMessage message, Predicate<MergedField> predicate) {
        List<FieldWithContext> result = new ArrayList<>();
        MessageTraverser.traverse(message, new MessageVisitor() {
            @Override
            public void visitField(MergedMessage msg, MergedField field) {
                if (predicate.test(field)) {
                    result.add(new FieldWithContext(msg, field));
                }
            }
        });
        return result;
    }

    /**
     * Collect all fields with type conflicts.
     *
     * @param schema The schema to search
     * @return List of fields that have type conflicts
     */
    public static List<FieldWithContext> collectConflicts(MergedSchema schema) {
        return collect(schema, MergedField::hasTypeConflict);
    }

    /**
     * Collect all fields with a specific conflict type.
     *
     * @param schema The schema to search
     * @param conflictType The conflict type to match
     * @return List of fields with the specified conflict type
     */
    public static List<FieldWithContext> collectByConflictType(MergedSchema schema,
                                                                 MergedField.ConflictType conflictType) {
        return collect(schema, field -> field.getConflictType() == conflictType);
    }

    /**
     * Collect all repeated fields.
     *
     * @param schema The schema to search
     * @return List of repeated fields
     */
    public static List<FieldWithContext> collectRepeated(MergedSchema schema) {
        return collect(schema, MergedField::isRepeated);
    }

    /**
     * Collect all message-type fields (non-primitive).
     *
     * @param schema The schema to search
     * @return List of message-type fields
     */
    public static List<FieldWithContext> collectMessageFields(MergedSchema schema) {
        return collect(schema, MergedField::isMessage);
    }

    /**
     * Collect all enum-type fields.
     *
     * @param schema The schema to search
     * @return List of enum-type fields
     */
    public static List<FieldWithContext> collectEnumFields(MergedSchema schema) {
        return collect(schema, MergedField::isEnum);
    }
}
