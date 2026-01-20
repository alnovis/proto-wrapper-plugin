package io.alnovis.protowrapper.diff.formatter;

import io.alnovis.protowrapper.diff.SchemaDiff;

/**
 * Interface for formatting schema diff results.
 * Implementations provide different output formats (text, JSON, markdown, etc.).
 */
public interface DiffFormatter {

    /**
     * Formats the complete schema diff.
     *
     * @param diff The schema diff to format
     * @return Formatted output string
     */
    String format(SchemaDiff diff);

    /**
     * Formats only the breaking changes from the diff.
     *
     * @param diff The schema diff to format
     * @return Formatted output string containing only breaking changes
     */
    default String formatBreakingOnly(SchemaDiff diff) {
        return format(diff); // Default implementation returns full format
    }
}
