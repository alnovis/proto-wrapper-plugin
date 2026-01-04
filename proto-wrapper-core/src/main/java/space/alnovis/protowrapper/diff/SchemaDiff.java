package space.alnovis.protowrapper.diff;

import space.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.diff.formatter.DiffFormatter;
import space.alnovis.protowrapper.diff.formatter.JsonDiffFormatter;
import space.alnovis.protowrapper.diff.formatter.MarkdownDiffFormatter;
import space.alnovis.protowrapper.diff.formatter.TextDiffFormatter;
import space.alnovis.protowrapper.diff.model.*;
import space.alnovis.protowrapper.model.EnumInfo;
import space.alnovis.protowrapper.model.MessageInfo;

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
     * @param v1 Source version schema
     * @param v2 Target version schema
     * @return SchemaDiff containing all differences
     */
    public static SchemaDiff compare(VersionSchema v1, VersionSchema v2) {
        return new SchemaDiffEngine().compare(v1, v2);
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

    // ========== Query Methods ==========

    /**
     * Returns the name of the source version.
     */
    public String getV1Name() {
        return v1Name;
    }

    /**
     * Returns the name of the target version.
     */
    public String getV2Name() {
        return v2Name;
    }

    /**
     * Returns all message diffs.
     */
    public List<MessageDiff> getMessageDiffs() {
        return messageDiffs;
    }

    /**
     * Returns all enum diffs.
     */
    public List<EnumDiff> getEnumDiffs() {
        return enumDiffs;
    }

    /**
     * Returns messages that were added in the target version.
     */
    public List<MessageInfo> getAddedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.ADDED)
            .map(MessageDiff::v2Message)
            .toList();
    }

    /**
     * Returns messages that were removed in the target version.
     */
    public List<MessageInfo> getRemovedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.REMOVED)
            .map(MessageDiff::v1Message)
            .toList();
    }

    /**
     * Returns messages that were modified between versions.
     */
    public List<MessageDiff> getModifiedMessages() {
        return messageDiffs.stream()
            .filter(md -> md.changeType() == ChangeType.MODIFIED)
            .toList();
    }

    /**
     * Returns enums that were added in the target version.
     */
    public List<EnumInfo> getAddedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.ADDED)
            .map(EnumDiff::v2Enum)
            .toList();
    }

    /**
     * Returns enums that were removed in the target version.
     */
    public List<EnumInfo> getRemovedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.REMOVED)
            .map(EnumDiff::v1Enum)
            .toList();
    }

    /**
     * Returns enums that were modified between versions.
     */
    public List<EnumDiff> getModifiedEnums() {
        return enumDiffs.stream()
            .filter(ed -> ed.changeType() == ChangeType.MODIFIED)
            .toList();
    }

    // ========== Breaking Changes ==========

    /**
     * Returns true if there are any breaking changes.
     */
    public boolean hasBreakingChanges() {
        return !breakingChanges.isEmpty();
    }

    /**
     * Returns all breaking changes.
     */
    public List<BreakingChange> getBreakingChanges() {
        return Collections.unmodifiableList(breakingChanges);
    }

    /**
     * Returns breaking changes of ERROR severity only.
     */
    public List<BreakingChange> getErrors() {
        return breakingChanges.stream()
            .filter(BreakingChange::isError)
            .toList();
    }

    /**
     * Returns breaking changes of WARNING severity only.
     */
    public List<BreakingChange> getWarnings() {
        return breakingChanges.stream()
            .filter(BreakingChange::isWarning)
            .toList();
    }

    // ========== Summary ==========

    /**
     * Returns the diff summary.
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

        return new DiffSummary(
            addedMessages, removedMessages, modifiedMessages,
            addedEnums, removedEnums, modifiedEnums,
            errors, warnings
        );
    }

    // ========== Output Methods ==========

    /**
     * Formats the diff as plain text.
     */
    public String toText() {
        return new TextDiffFormatter().format(this);
    }

    /**
     * Formats the diff as JSON.
     */
    public String toJson() {
        return new JsonDiffFormatter().format(this);
    }

    /**
     * Formats the diff as Markdown.
     */
    public String toMarkdown() {
        return new MarkdownDiffFormatter().format(this);
    }

    /**
     * Formats the diff using a custom formatter.
     */
    public String format(DiffFormatter formatter) {
        return formatter.format(this);
    }

    // ========== Summary Record ==========

    /**
     * Summary statistics for the schema diff.
     */
    public record DiffSummary(
        int addedMessages,
        int removedMessages,
        int modifiedMessages,
        int addedEnums,
        int removedEnums,
        int modifiedEnums,
        int errorCount,
        int warningCount
    ) {
        /**
         * Returns true if there are any differences.
         */
        public boolean hasDifferences() {
            return addedMessages > 0 || removedMessages > 0 || modifiedMessages > 0 ||
                   addedEnums > 0 || removedEnums > 0 || modifiedEnums > 0;
        }

        /**
         * Returns true if there are breaking changes.
         */
        public boolean hasBreakingChanges() {
            return errorCount > 0;
        }

        /**
         * Returns total number of entity changes.
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
