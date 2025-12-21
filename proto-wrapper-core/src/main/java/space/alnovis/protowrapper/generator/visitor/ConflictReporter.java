package space.alnovis.protowrapper.generator.visitor;

import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates reports about type conflicts in a schema using the visitor pattern.
 *
 * <p>This utility helps developers understand the type conflicts detected
 * during schema merging and how they will be handled in the generated code.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ConflictReport report = ConflictReporter.analyze(schema);
 * System.out.println(report.toDetailedString());
 * }</pre>
 */
public final class ConflictReporter {

    private ConflictReporter() {
        // Utility class
    }

    /**
     * A detailed report of type conflicts in a schema.
     */
    public record ConflictReport(
            int totalFields,
            int conflictFields,
            Map<MergedField.ConflictType, List<ConflictDetail>> conflictsByType
    ) {
        /**
         * Get a summary string.
         */
        public String toSummary() {
            if (conflictFields == 0) {
                return String.format("No type conflicts detected among %d fields.", totalFields);
            }
            return String.format("%d type conflicts detected among %d fields:\n%s",
                    conflictFields, totalFields,
                    conflictsByType.entrySet().stream()
                            .filter(e -> !e.getValue().isEmpty())
                            .map(e -> String.format("  - %s: %d", e.getKey(), e.getValue().size()))
                            .collect(Collectors.joining("\n")));
        }

        /**
         * Get a detailed string with all conflict information.
         */
        public String toDetailedString() {
            if (conflictFields == 0) {
                return toSummary();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(toSummary()).append("\n\nDetails:\n");

            conflictsByType.forEach((type, details) -> {
                if (!details.isEmpty()) {
                    sb.append("\n").append(type).append(":\n");
                    details.forEach(detail -> sb.append("  ").append(detail.toDetailedString()).append("\n"));
                }
            });

            return sb.toString();
        }
    }

    /**
     * Details about a single type conflict.
     */
    public record ConflictDetail(
            String messagePath,
            String fieldName,
            MergedField.ConflictType conflictType,
            String unifiedType,
            Map<String, String> versionTypes
    ) {
        @Override
        public String toString() {
            return String.format("%s.%s: %s", messagePath, fieldName, conflictType);
        }

        public String toDetailedString() {
            String versions = versionTypes.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
            return String.format("%s.%s [%s -> %s] (%s)",
                    messagePath, fieldName, versions, unifiedType, conflictType);
        }
    }

    /**
     * Analyze a schema and generate a conflict report.
     *
     * @param schema The schema to analyze
     * @return A detailed conflict report
     */
    public static ConflictReport analyze(MergedSchema schema) {
        ReportCollector collector = new ReportCollector();
        MessageTraverser.traverse(schema, collector);
        return collector.build();
    }

    /**
     * Analyze a single message and generate a conflict report.
     *
     * @param message The message to analyze
     * @return A detailed conflict report
     */
    public static ConflictReport analyze(MergedMessage message) {
        ReportCollector collector = new ReportCollector();
        MessageTraverser.traverse(message, collector);
        return collector.build();
    }

    /**
     * Check if a schema has any type conflicts.
     *
     * @param schema The schema to check
     * @return true if there are type conflicts
     */
    public static boolean hasConflicts(MergedSchema schema) {
        return !FieldCollector.collectConflicts(schema).isEmpty();
    }

    /**
     * Internal visitor for collecting conflict information.
     */
    private static class ReportCollector implements MessageVisitor {
        private int totalFields = 0;
        private int conflictFields = 0;
        private final Map<MergedField.ConflictType, List<ConflictDetail>> conflictsByType =
                new EnumMap<>(MergedField.ConflictType.class);
        private final Deque<String> messagePathStack = new ArrayDeque<>();

        ReportCollector() {
            // Initialize empty lists for all conflict types
            for (MergedField.ConflictType type : MergedField.ConflictType.values()) {
                conflictsByType.put(type, new ArrayList<>());
            }
        }

        @Override
        public void enterMessage(MergedMessage message) {
            messagePathStack.push(message.getName());
        }

        @Override
        public void exitMessage(MergedMessage message) {
            messagePathStack.pop();
        }

        @Override
        public void visitField(MergedMessage message, MergedField field) {
            totalFields++;

            if (field.hasTypeConflict()) {
                conflictFields++;

                String messagePath = buildMessagePath();
                Map<String, String> versionTypes = field.getVersionFields().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getJavaType(),
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));

                ConflictDetail detail = new ConflictDetail(
                        messagePath,
                        field.getName(),
                        field.getConflictType(),
                        field.getGetterType(),
                        versionTypes
                );

                conflictsByType.get(field.getConflictType()).add(detail);
            }
        }

        private String buildMessagePath() {
            List<String> path = new ArrayList<>(messagePathStack);
            Collections.reverse(path);
            return String.join(".", path);
        }

        ConflictReport build() {
            // Remove empty conflict type entries
            Map<MergedField.ConflictType, List<ConflictDetail>> nonEmpty = conflictsByType.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> List.copyOf(e.getValue()),
                            (a, b) -> a,
                            () -> new EnumMap<>(MergedField.ConflictType.class)
                    ));

            return new ConflictReport(totalFields, conflictFields, nonEmpty);
        }
    }
}
