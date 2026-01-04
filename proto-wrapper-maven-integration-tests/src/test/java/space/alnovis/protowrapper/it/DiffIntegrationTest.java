package space.alnovis.protowrapper.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import space.alnovis.protowrapper.analyzer.ProtocExecutor;
import space.alnovis.protowrapper.diff.SchemaDiff;
import space.alnovis.protowrapper.diff.model.BreakingChange;
import space.alnovis.protowrapper.diff.model.ChangeType;
import space.alnovis.protowrapper.diff.model.MessageDiff;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Schema Diff functionality using real proto files
 * from the integration-tests module.
 *
 * These tests use the existing v1 and v2 proto directories that contain
 * various schema differences.
 */
class DiffIntegrationTest {

    private Path protoRoot;
    private Path v1Dir;
    private Path v2Dir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Use the shared test-protos directory for generation scenarios
        protoRoot = Path.of("../test-protos/scenarios/generation");
        v1Dir = Path.of("../test-protos/scenarios/generation/v1");
        v2Dir = Path.of("../test-protos/scenarios/generation/v2");
    }

    static boolean isProtocAvailable() {
        try {
            Process process = new ProcessBuilder("protoc", "--version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_detectsChanges() throws Exception {
        // Analyze both versions
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        // Compare schemas
        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        // Verify basic structure
        assertNotNull(diff);
        assertNotNull(diff.getSummary());
        assertEquals("v1", diff.getV1Name());
        assertEquals("v2", diff.getV2Name());

        // The real protos should have some differences
        // We don't assert specific counts as protos may change
        System.out.println("Diff Summary:");
        System.out.println("  Added messages: " + diff.getSummary().addedMessages());
        System.out.println("  Removed messages: " + diff.getSummary().removedMessages());
        System.out.println("  Modified messages: " + diff.getSummary().modifiedMessages());
        System.out.println("  Added enums: " + diff.getSummary().addedEnums());
        System.out.println("  Removed enums: " + diff.getSummary().removedEnums());
        System.out.println("  Modified enums: " + diff.getSummary().modifiedEnums());
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_generatesTextOutput() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        String textOutput = diff.toText();

        assertNotNull(textOutput);
        assertTrue(textOutput.contains("Schema Comparison: v1 -> v2"));
        assertTrue(textOutput.contains("MESSAGES"));
        assertTrue(textOutput.contains("SUMMARY"));

        System.out.println("=== Text Output ===");
        System.out.println(textOutput);
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_generatesJsonOutput() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        String jsonOutput = diff.toJson();

        assertNotNull(jsonOutput);
        assertTrue(jsonOutput.startsWith("{"));
        assertTrue(jsonOutput.contains("\"v1\": \"v1\""));
        assertTrue(jsonOutput.contains("\"v2\": \"v2\""));
        assertTrue(jsonOutput.contains("\"summary\""));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_generatesMarkdownOutput() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        String markdownOutput = diff.toMarkdown();

        assertNotNull(markdownOutput);
        assertTrue(markdownOutput.contains("# Schema Comparison: v1 -> v2"));
        assertTrue(markdownOutput.contains("## Summary"));
        assertTrue(markdownOutput.contains("| Category | Added | Modified | Removed |"));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_detectsBreakingChanges() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        // Check breaking changes API
        List<BreakingChange> errors = diff.getErrors();
        List<BreakingChange> warnings = diff.getWarnings();

        assertNotNull(errors);
        assertNotNull(warnings);

        if (diff.hasBreakingChanges()) {
            System.out.println("Breaking changes detected:");
            for (BreakingChange bc : diff.getBreakingChanges()) {
                System.out.println("  [" + bc.severity() + "] " + bc.type() + ": " +
                    bc.entityPath() + " - " + bc.description());
            }
        }
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void compareRealSchemas_queryMethods() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

        // Test query methods
        assertNotNull(diff.getAddedMessages());
        assertNotNull(diff.getRemovedMessages());
        assertNotNull(diff.getModifiedMessages());
        assertNotNull(diff.getAddedEnums());
        assertNotNull(diff.getRemovedEnums());
        assertNotNull(diff.getModifiedEnums());

        // Check that modified messages have proper field changes
        for (MessageDiff md : diff.getModifiedMessages()) {
            assertNotNull(md.messageName());
            assertEquals(ChangeType.MODIFIED, md.changeType());
            assertNotNull(md.getAddedFields());
            assertNotNull(md.getRemovedFields());
            assertNotNull(md.getModifiedFields());
        }
    }
}
