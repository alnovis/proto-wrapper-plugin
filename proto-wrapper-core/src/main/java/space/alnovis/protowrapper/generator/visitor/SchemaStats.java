package space.alnovis.protowrapper.generator.visitor;

import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects statistics about a schema using the visitor pattern.
 *
 * <p>Provides counts of messages, fields, nested types, and conflict types.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SchemaStats stats = SchemaStats.collect(schema);
 * System.out.println("Messages: " + stats.messageCount());
 * System.out.println("Fields: " + stats.fieldCount());
 * System.out.println("Max nesting depth: " + stats.maxNestingDepth());
 * }</pre>
 */
public record SchemaStats(
        int messageCount,
        int nestedMessageCount,
        int fieldCount,
        int repeatedFieldCount,
        int optionalFieldCount,
        int enumCount,
        int maxNestingDepth,
        Map<MergedField.ConflictType, Integer> conflictCounts
) {

    /**
     * Collect statistics from a schema.
     *
     * @param schema The schema to analyze
     * @return Statistics about the schema
     */
    public static SchemaStats collect(MergedSchema schema) {
        StatsCollector collector = new StatsCollector();
        MessageTraverser.traverse(schema, collector);
        return collector.build();
    }

    /**
     * Collect statistics from a single message (including nested).
     *
     * @param message The message to analyze
     * @return Statistics about the message
     */
    public static SchemaStats collect(MergedMessage message) {
        StatsCollector collector = new StatsCollector();
        MessageTraverser.traverse(message, collector);
        return collector.build();
    }

    /**
     * Get count of fields with conflicts (any type except NONE).
     *
     * @return count of fields with conflicts
     */
    public int conflictFieldCount() {
        return conflictCounts.entrySet().stream()
                .filter(e -> e.getKey() != MergedField.ConflictType.NONE)
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * Get count of fields with a specific conflict type.
     *
     * @param type the conflict type to count
     * @return count of fields with the specified conflict type
     */
    public int getConflictCount(MergedField.ConflictType type) {
        return conflictCounts.getOrDefault(type, 0);
    }

    @Override
    public String toString() {
        return String.format(
                "SchemaStats[messages=%d (nested=%d), fields=%d (repeated=%d, optional=%d), " +
                "enums=%d, maxDepth=%d, conflicts=%d]",
                messageCount, nestedMessageCount, fieldCount, repeatedFieldCount, optionalFieldCount,
                enumCount, maxNestingDepth, conflictFieldCount());
    }

    /**
     * Internal visitor that collects statistics.
     */
    private static class StatsCollector implements MessageVisitor {
        private int messageCount = 0;
        private int nestedMessageCount = 0;
        private int fieldCount = 0;
        private int repeatedFieldCount = 0;
        private int optionalFieldCount = 0;
        private int enumCount = 0;
        private int currentDepth = 0;
        private int maxDepth = 0;
        private final Map<MergedField.ConflictType, Integer> conflictCounts = new HashMap<>();

        @Override
        public void enterMessage(MergedMessage message) {
            messageCount++;
            currentDepth++;
            maxDepth = Math.max(maxDepth, currentDepth);
        }

        @Override
        public void exitMessage(MergedMessage message) {
            currentDepth--;
        }

        @Override
        public void enterNestedMessage(MergedMessage parent, MergedMessage nested) {
            nestedMessageCount++;
        }

        @Override
        public void visitField(MergedMessage message, MergedField field) {
            fieldCount++;
            if (field.isRepeated()) repeatedFieldCount++;
            if (field.isOptional()) optionalFieldCount++;

            MergedField.ConflictType conflictType = field.getConflictType();
            conflictCounts.merge(conflictType, 1, Integer::sum);
        }

        @Override
        public void visitNestedEnum(MergedMessage message, MergedEnum nestedEnum) {
            enumCount++;
        }

        SchemaStats build() {
            return new SchemaStats(
                    messageCount,
                    nestedMessageCount,
                    fieldCount,
                    repeatedFieldCount,
                    optionalFieldCount,
                    enumCount,
                    maxDepth,
                    Map.copyOf(conflictCounts)
            );
        }
    }
}
