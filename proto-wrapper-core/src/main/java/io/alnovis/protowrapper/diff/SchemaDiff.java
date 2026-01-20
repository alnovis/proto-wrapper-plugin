package io.alnovis.protowrapper.diff;

import io.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.diff.formatter.DiffFormatter;
import io.alnovis.protowrapper.diff.formatter.JsonDiffFormatter;
import io.alnovis.protowrapper.diff.formatter.MarkdownDiffFormatter;
import io.alnovis.protowrapper.diff.formatter.TextDiffFormatter;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.merger.VersionMerger;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.MergedSchema;
import io.alnovis.protowrapper.model.MessageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Main result class for schema comparison.
 * Provides access to all differences between two schema versions.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Compare two schemas
 * SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);
 *
 * // Query differences
 * List<MessageInfo> added = diff.getAddedMessages();
 * List<MessageInfo> removed = diff.getRemovedMessages();
 *
 * // Check breaking changes
 * if (diff.hasBreakingChanges()) {
 *     for (BreakingChange bc : diff.getBreakingChanges()) {
 *         System.err.println(bc.severity() + ": " + bc.description());
 *     }
 * }
 *
 * // Export to different formats
 * String textReport = diff.toText();
 * String jsonReport = diff.toJson();
 * String markdownReport = diff.toMarkdown();
 * }</pre>
 */
public class SchemaDiff {

    private final String v1Name;
    private final String v2Name;
    private final List<MessageDiff> messageDiffs;
    private final List<EnumDiff> enumDiffs;
    private final List<BreakingChange> breakingChanges;
    private final DiffSummary summary;

    /**
     * Creates a new SchemaDiff result.
     *
     * @param v1Name the source version name
     * @param v2Name the target version name
     * @param messageDiffs the list of message differences
     * @param enumDiffs the list of enum differences
     * @param breakingChanges the list of breaking changes
     */
    public SchemaDiff(
            String v1Name,
            String v2Name,
            List<MessageDiff> messageDiffs,
            List<EnumDiff> enumDiffs,
            List<BreakingChange> breakingChanges) {
        this.v1Name = v1Name;
        this.v2Name = v2Name;
        this.messageDiffs = List.copyOf(messageDiffs);
        this.enumDiffs = List.copyOf(enumDiffs);
        this.breakingChanges = List.copyOf(breakingChanges);
        this.summary = calculateSummary();
    }

    // ========== Factory Methods ==========

    /**
     * Compare two version schemas.
     *
     * <p>Uses the VersionMerger infrastructure for consistent conflict detection
     * across the plugin.</p>
     *
     * @param v1 Source version schema
     * @param v2 Target version schema
     * @return SchemaDiff containing all differences
     */
    public static SchemaDiff compare(VersionSchema v1, VersionSchema v2) {
        return compareViaMerger(v1, v2);
    }

    /**
     * Compare two proto directories.
     *
     * @param v1Dir Directory containing source version proto files
     * @param v2Dir Directory containing target version proto files
     * @return SchemaDiff containing all differences
     * @throws IOException if directories cannot be read
     */
    public static SchemaDiff compare(Path v1Dir, Path v2Dir) throws IOException {
        return compare(v1Dir, v2Dir, "v1", "v2");
    }

    /**
     * Compare two proto directories with custom version names.
     *
     * @param v1Dir   Directory containing source version proto files
     * @param v2Dir   Directory containing target version proto files
     * @param v1Name  Name for source version
     * @param v2Name  Name for target version
     * @return SchemaDiff containing all differences
     * @throws IOException if directories cannot be read
     */
    public static SchemaDiff compare(Path v1Dir, Path v2Dir, String v1Name, String v2Name) throws IOException {
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema v1 = analyzer.analyze(v1Dir, v1Name);
        VersionSchema v2 = analyzer.analyze(v2Dir, v2Name);
        return compare(v1, v2);
    }

    /**
     * Compare two version schemas using the VersionMerger infrastructure.
     *
     * <p>This method uses the same conflict detection logic as the code generator,
     * ensuring consistent classification of type conflicts across the plugin.</p>
     *
     * @param v1 Source version schema
     * @param v2 Target version schema
     * @return SchemaDiff containing all differences
     */
    public static SchemaDiff compareViaMerger(VersionSchema v1, VersionSchema v2) {
        // Use VersionMerger to merge both schemas - this applies consistent conflict detection
        MergedSchema merged = new VersionMerger().merge(List.of(v1, v2));

        // Use the adapter to convert MergedSchema to SchemaDiff
        return MergedSchemaDiffAdapter.toSchemaDiff(merged, v1.getVersion(), v2.getVersion());
    }

    /**
     * Compare two proto directories using the VersionMerger infrastructure.
     *
     * @param v1Dir  Directory containing source version proto files
     * @param v2Dir  Directory containing target version proto files
     * @return SchemaDiff containing all differences
     * @throws IOException if directories cannot be read
     */
    public static SchemaDiff compareViaMerger(Path v1Dir, Path v2Dir) throws IOException {
        return compareViaMerger(v1Dir, v2Dir, "v1", "v2");
    }

    /**
     * Compare two proto directories using the VersionMerger infrastructure.
     *
     * @param v1Dir   Directory containing source version proto files
     * @param v2Dir   Directory containing target version proto files
     * @param v1Name  Name for source version
     * @param v2Name  Name for target version
     * @return SchemaDiff containing all differences
     * @throws IOException if directories cannot be read
     */
    public static SchemaDiff compareViaMerger(Path v1Dir, Path v2Dir, String v1Name, String v2Name) throws IOException {
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema v1 = analyzer.analyze(v1Dir, v1Name);
        VersionSchema v2 = analyzer.analyze(v2Dir, v2Name);
        return compareViaMerger(v1, v2);
    }

    // ========== Query Methods ==========

    /**
     * Returns the name of the source version.
     *
     * @return the source version name
     */
    public String getV1Name() {
        return v1Name;
    }

    /**
     * Returns the name of the target version.
     *
     * @return the target version name
     */
    public String getV2Name() {
        return v2Name;
    }

    /**
     * Returns all message diffs.
     *
     * @return list of message differences
     */
    public List<MessageDiff> getMessageDiffs() {
        return messageDiffs;
    }

    /**
     * Returns all enum diffs.
     *
     * @return list of enum differences
     */
    public List<EnumDiff> getEnumDiffs() {
        return enumDiffs;
    }

    /**
     * Returns messages that were added in the target version.
     *
     * @return list of added messages
     */
    public List<MessageInfo> getAddedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.ADDED)
            .map(MessageDiff::v2Message)
            .toList();
    }

    /**
     * Returns messages that were removed in the target version.
     *
     * @return list of removed messages
     */
    public List<MessageInfo> getRemovedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.REMOVED)
            .map(MessageDiff::v1Message)
            .toList();
    }

    /**
     * Returns messages that were modified between versions.
     *
     * @return list of modified message diffs
     */
    public List<MessageDiff> getModifiedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.MODIFIED)
            .toList();
    }

    /**
     * Returns enums that were added in the target version.
     *
     * @return list of added enums
     */
    public List<EnumInfo> getAddedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.ADDED)
            .map(EnumDiff::v2Enum)
            .toList();
    }

    /**
     * Returns enums that were removed in the target version.
     *
     * @return list of removed enums
     */
    public List<EnumInfo> getRemovedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.REMOVED)
            .map(EnumDiff::v1Enum)
            .toList();
    }

    /**
     * Returns enums that were modified between versions.
     *
     * @return list of modified enum diffs
     */
    public List<EnumDiff> getModifiedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.MODIFIED)
            .toList();
    }

    // ========== Breaking Changes ==========

    /**
     * Returns true if there are any ERROR-level breaking changes.
     * INFO-level changes (plugin-handled) and WARNING-level changes are not considered breaking.
     *
     * @return true if there are ERROR-level breaking changes
     */
    public boolean hasBreakingChanges() {
        return breakingChanges.stream().anyMatch(BreakingChange::isError);
    }

    /**
     * Returns all breaking changes.
     *
     * @return unmodifiable list of breaking changes
     */
    public List<BreakingChange> getBreakingChanges() {
        return Collections.unmodifiableList(breakingChanges);
    }

    /**
     * Returns breaking changes of ERROR severity only.
     *
     * @return list of ERROR-level breaking changes
     */
    public List<BreakingChange> getErrors() {
        return breakingChanges.stream()
            .filter(BreakingChange::isError)
            .toList();
    }

    /**
     * Returns breaking changes of WARNING severity only.
     *
     * @return list of WARNING-level breaking changes
     */
    public List<BreakingChange> getWarnings() {
        return breakingChanges.stream()
            .filter(BreakingChange::isWarning)
            .toList();
    }

    // ========== Summary ==========

    /**
     * Returns the diff summary.
     *
     * @return the diff summary
     */
    public DiffSummary getSummary() {
        return summary;
    }

    private DiffSummary calculateSummary() {
        int addedMessages = (int) messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.ADDED).count();
        int removedMessages = (int) messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.REMOVED).count();
        int modifiedMessages = (int) messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.MODIFIED).count();

        int addedEnums = (int) enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.ADDED).count();
        int removedEnums = (int) enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.REMOVED).count();
        int modifiedEnums = (int) enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.MODIFIED).count();

        int errors = (int) breakingChanges.stream()
            .filter(bc -> bc.severity() == BreakingChange.Severity.ERROR).count();
        int warnings = (int) breakingChanges.stream()
            .filter(bc -> bc.severity() == BreakingChange.Severity.WARNING).count();
        int infos = (int) breakingChanges.stream()
            .filter(bc -> bc.severity() == BreakingChange.Severity.INFO).count();

        return new DiffSummary(
            addedMessages, removedMessages, modifiedMessages,
            addedEnums, removedEnums, modifiedEnums,
            errors, warnings, infos
        );
    }

    // ========== Output Methods ==========

    /**
     * Formats the diff as plain text.
     *
     * @return plain text representation of the diff
     */
    public String toText() {
        return new TextDiffFormatter().format(this);
    }

    /**
     * Formats the diff as JSON.
     *
     * @return JSON representation of the diff
     */
    public String toJson() {
        return new JsonDiffFormatter().format(this);
    }

    /**
     * Formats the diff as Markdown.
     *
     * @return Markdown representation of the diff
     */
    public String toMarkdown() {
        return new MarkdownDiffFormatter().format(this);
    }

    /**
     * Formats the diff using a custom formatter.
     *
     * @param formatter the formatter to use
     * @return formatted representation of the diff
     */
    public String format(DiffFormatter formatter) {
        return formatter.format(this);
    }

    // ========== Summary Record ==========

    /**
     * Summary statistics for the schema diff.
     *
     * @param addedMessages count of added messages
     * @param removedMessages count of removed messages
     * @param modifiedMessages count of modified messages
     * @param addedEnums count of added enums
     * @param removedEnums count of removed enums
     * @param modifiedEnums count of modified enums
     * @param errorCount count of error-level breaking changes
     * @param warningCount count of warning-level breaking changes
     * @param infoCount count of info-level breaking changes
     */
    public record DiffSummary(
        int addedMessages,
        int removedMessages,
        int modifiedMessages,
        int addedEnums,
        int removedEnums,
        int modifiedEnums,
        int errorCount,
        int warningCount,
        int infoCount
    ) {
        /**
         * Returns true if there are any differences.
         *
         * @return true if there are differences
         */
        public boolean hasDifferences() {
            return addedMessages > 0 || removedMessages > 0 || modifiedMessages > 0 ||
                   addedEnums > 0 || removedEnums > 0 || modifiedEnums > 0;
        }

        /**
         * Returns true if there are breaking changes (errors only, not warnings or info).
         *
         * @return true if there are error-level breaking changes
         */
        public boolean hasBreakingChanges() {
            return errorCount > 0;
        }

        /**
         * Returns total number of entity changes.
         *
         * @return total count of entity changes
         */
        public int totalChanges() {
            return addedMessages + removedMessages + modifiedMessages +
                   addedEnums + removedEnums + modifiedEnums;
        }

        @Override
        public String toString() {
            return String.format(
                "Messages: +%d/-%d/~%d, Enums: +%d/-%d/~%d, Breaking: %d errors, %d warnings",
                addedMessages, removedMessages, modifiedMessages,
                addedEnums, removedEnums, modifiedEnums,
                errorCount, warningCount
            );
        }
    }

    @Override
    public String toString() {
        return String.format("SchemaDiff[%s -> %s]: %s", v1Name, v2Name, summary);
    }
}
