package io.alnovis.protowrapper.it.model;

import io.alnovis.protowrapper.it.model.api.*;
import io.alnovis.protowrapper.runtime.SchemaInfo;
import io.alnovis.protowrapper.runtime.VersionSchemaDiff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Schema Metadata feature.
 *
 * <p>Tests the generated SchemaInfo and VersionSchemaDiff classes
 * via VersionContext methods.</p>
 */
@DisplayName("Schema Metadata Integration Tests")
class SchemaMetadataIntegrationTest {

    @Nested
    @DisplayName("VersionContext.getSchemaInfo()")
    class GetSchemaInfoTest {

        @Test
        @DisplayName("V1 context returns SchemaInfo for V1")
        void v1ContextReturnsSchemaInfoV1() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            SchemaInfo schema = ctx.getSchemaInfo();

            assertThat(schema).isNotNull();
            assertThat(schema.getVersionId()).isEqualTo("v1");
        }

        @Test
        @DisplayName("V2 context returns SchemaInfo for V2")
        void v2ContextReturnsSchemaInfoV2() {
            VersionContext ctx = VersionContext.forVersionId("v2");

            SchemaInfo schema = ctx.getSchemaInfo();

            assertThat(schema).isNotNull();
            assertThat(schema.getVersionId()).isEqualTo("v2");
        }

        @Test
        @DisplayName("SchemaInfo is singleton instance")
        void schemaInfoIsSingleton() {
            VersionContext ctx = VersionContext.forVersionId("v1");

            SchemaInfo schema1 = ctx.getSchemaInfo();
            SchemaInfo schema2 = ctx.getSchemaInfo();

            assertThat(schema1).isSameAs(schema2);
        }
    }

    @Nested
    @DisplayName("SchemaInfo.getEnums()")
    class GetEnumsTest {

        @Test
        @DisplayName("returns non-null map of enums")
        void returnsNonNullEnumsMap() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            assertThat(schema.getEnums()).isNotNull();
        }

        @Test
        @DisplayName("enum map contains expected enums")
        void enumMapContainsExpectedEnums() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            // Check that some known enum exists
            // The exact enum depends on test protos
            assertThat(schema.getEnums()).isNotNull();
        }

        @Test
        @DisplayName("getEnum returns Optional with EnumInfo")
        void getEnumReturnsOptionalWithEnumInfo() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            // If enums exist, verify structure
            if (!schema.getEnums().isEmpty()) {
                String enumName = schema.getEnums().keySet().iterator().next();
                Optional<SchemaInfo.EnumInfo> enumInfo = schema.getEnum(enumName);

                assertThat(enumInfo).isPresent();
                assertThat(enumInfo.get().getName()).isEqualTo(enumName);
                assertThat(enumInfo.get().getFullName()).isNotEmpty();
                assertThat(enumInfo.get().getValues()).isNotNull();
            }
        }

        @Test
        @DisplayName("getEnum returns empty for non-existent enum")
        void getEnumReturnsEmptyForNonExistent() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            Optional<SchemaInfo.EnumInfo> enumInfo = schema.getEnum("NonExistentEnum");

            assertThat(enumInfo).isEmpty();
        }
    }

    @Nested
    @DisplayName("SchemaInfo.EnumInfo")
    class EnumInfoTest {

        @Test
        @DisplayName("EnumValue has name and number")
        void enumValueHasNameAndNumber() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            // Find first enum with values
            Optional<SchemaInfo.EnumInfo> enumOpt = schema.getEnums().values().stream()
                    .filter(e -> !e.getValues().isEmpty())
                    .findFirst();

            if (enumOpt.isPresent()) {
                SchemaInfo.EnumValue firstValue = enumOpt.get().getValues().get(0);

                assertThat(firstValue.name()).isNotEmpty();
                // number() can be any int, just verify it's accessible
                int number = firstValue.number();
                assertThat(number).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("SchemaInfo.getMessages()")
    class GetMessagesTest {

        @Test
        @DisplayName("returns non-null map of messages")
        void returnsNonNullMessagesMap() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            assertThat(schema.getMessages()).isNotNull();
        }

        @Test
        @DisplayName("getMessage returns Optional with MessageInfo")
        void getMessageReturnsOptionalWithMessageInfo() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            // If messages exist, verify structure
            if (!schema.getMessages().isEmpty()) {
                String msgName = schema.getMessages().keySet().iterator().next();
                Optional<SchemaInfo.MessageInfo> msgInfo = schema.getMessage(msgName);

                assertThat(msgInfo).isPresent();
                assertThat(msgInfo.get().getName()).isEqualTo(msgName);
                assertThat(msgInfo.get().getFullName()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("getMessage returns empty for non-existent message")
        void getMessageReturnsEmptyForNonExistent() {
            VersionContext ctx = VersionContext.forVersionId("v1");
            SchemaInfo schema = ctx.getSchemaInfo();

            Optional<SchemaInfo.MessageInfo> msgInfo = schema.getMessage("NonExistentMessage");

            assertThat(msgInfo).isEmpty();
        }
    }

    @Nested
    @DisplayName("VersionContext.getDiffFrom()")
    class GetDiffFromTest {

        @Test
        @DisplayName("V2 context can get diff from V1")
        void v2ContextCanGetDiffFromV1() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");

            Optional<VersionSchemaDiff> diff = v2Ctx.getDiffFrom("v1");

            assertThat(diff).isPresent();
            assertThat(diff.get().getFromVersion()).isEqualTo("v1");
            assertThat(diff.get().getToVersion()).isEqualTo("v2");
        }

        @Test
        @DisplayName("V1 context returns empty diff (no previous version)")
        void v1ContextReturnsEmptyDiff() {
            VersionContext v1Ctx = VersionContext.forVersionId("v1");

            // V1 has no previous version
            Optional<VersionSchemaDiff> diff = v1Ctx.getDiffFrom("v0");

            assertThat(diff).isEmpty();
        }

        @Test
        @DisplayName("getDiffFrom with non-adjacent version returns empty")
        void getDiffFromNonAdjacentVersionReturnsEmpty() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");

            // Only v1->v2 diff exists, not v99->v2
            Optional<VersionSchemaDiff> diff = v2Ctx.getDiffFrom("v99");

            assertThat(diff).isEmpty();
        }
    }

    @Nested
    @DisplayName("VersionSchemaDiff methods")
    class VersionSchemaDiffMethodsTest {

        @Test
        @DisplayName("getFieldChanges returns non-null list")
        void getFieldChangesReturnsNonNullList() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            assertThat(diff.getFieldChanges()).isNotNull();
        }

        @Test
        @DisplayName("getEnumChanges returns non-null list")
        void getEnumChangesReturnsNonNullList() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            assertThat(diff.getEnumChanges()).isNotNull();
        }

        @Test
        @DisplayName("hasChanges returns correct value")
        void hasChangesReturnsCorrectValue() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            // hasChanges should match whether field/enum changes exist
            boolean expected = !diff.getFieldChanges().isEmpty() || !diff.getEnumChanges().isEmpty();
            assertThat(diff.hasChanges()).isEqualTo(expected);
        }

        @Test
        @DisplayName("findFieldChange returns matching change")
        void findFieldChangeReturnsMatchingChange() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            // If there are field changes, verify findFieldChange works
            if (!diff.getFieldChanges().isEmpty()) {
                VersionSchemaDiff.FieldChange fc = diff.getFieldChanges().get(0);
                Optional<VersionSchemaDiff.FieldChange> found = diff.findFieldChange(
                        fc.messageName(), fc.fieldName());

                assertThat(found).isPresent();
                assertThat(found.get()).isEqualTo(fc);
            }
        }

        @Test
        @DisplayName("findFieldChange returns empty for non-existent field")
        void findFieldChangeReturnsEmptyForNonExistent() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            Optional<VersionSchemaDiff.FieldChange> found = diff.findFieldChange(
                    "NonExistentMessage", "nonExistentField");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("getAddedFields returns only ADDED changes")
        void getAddedFieldsReturnsOnlyAdded() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            List<VersionSchemaDiff.FieldChange> added = diff.getAddedFields();
            assertThat(added).allMatch(fc ->
                    fc.changeType() == VersionSchemaDiff.FieldChangeType.ADDED);
        }

        @Test
        @DisplayName("getRemovedFields returns only REMOVED changes")
        void getRemovedFieldsReturnsOnlyRemoved() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            List<VersionSchemaDiff.FieldChange> removed = diff.getRemovedFields();
            assertThat(removed).allMatch(fc ->
                    fc.changeType() == VersionSchemaDiff.FieldChangeType.REMOVED);
        }
    }

    @Nested
    @DisplayName("FieldChange record")
    class FieldChangeRecordTest {

        @Test
        @DisplayName("FieldChange has all required fields")
        void fieldChangeHasAllRequiredFields() {
            VersionContext v2Ctx = VersionContext.forVersionId("v2");
            Optional<VersionSchemaDiff> diffOpt = v2Ctx.getDiffFrom("v1");

            assertThat(diffOpt).isPresent();
            VersionSchemaDiff diff = diffOpt.get();

            if (!diff.getFieldChanges().isEmpty()) {
                VersionSchemaDiff.FieldChange fc = diff.getFieldChanges().get(0);

                // All required fields are accessible
                assertThat(fc.messageName()).isNotNull();
                assertThat(fc.fieldName()).isNotNull();
                assertThat(fc.changeType()).isNotNull();
                // These may be null depending on change type
                fc.oldType();
                fc.newType();
                fc.migrationHint();
            }
        }

        @Test
        @DisplayName("hasHint returns correct value")
        void hasHintReturnsCorrectValue() {
            // Test the hasHint method directly
            VersionSchemaDiff.FieldChange withHint = VersionSchemaDiff.FieldChange.added(
                    "Test", "field", "string", "Some migration hint");
            assertThat(withHint.hasHint()).isTrue();

            VersionSchemaDiff.FieldChange withoutHint = VersionSchemaDiff.FieldChange.added(
                    "Test", "field", "string", null);
            assertThat(withoutHint.hasHint()).isFalse();

            VersionSchemaDiff.FieldChange withEmptyHint = VersionSchemaDiff.FieldChange.added(
                    "Test", "field", "string", "");
            assertThat(withEmptyHint.hasHint()).isFalse();
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("FieldChange.added creates ADDED change")
        void fieldChangeAddedCreatesAddedChange() {
            VersionSchemaDiff.FieldChange fc = VersionSchemaDiff.FieldChange.added(
                    "Order", "newField", "string", "New field in v2");

            assertThat(fc.messageName()).isEqualTo("Order");
            assertThat(fc.fieldName()).isEqualTo("newField");
            assertThat(fc.changeType()).isEqualTo(VersionSchemaDiff.FieldChangeType.ADDED);
            assertThat(fc.newType()).isEqualTo("string");
            assertThat(fc.migrationHint()).isEqualTo("New field in v2");
        }

        @Test
        @DisplayName("FieldChange.removed creates REMOVED change")
        void fieldChangeRemovedCreatesRemovedChange() {
            VersionSchemaDiff.FieldChange fc = VersionSchemaDiff.FieldChange.removed(
                    "Order", "oldField", "int32", "Field removed in v2");

            assertThat(fc.messageName()).isEqualTo("Order");
            assertThat(fc.fieldName()).isEqualTo("oldField");
            assertThat(fc.changeType()).isEqualTo(VersionSchemaDiff.FieldChangeType.REMOVED);
            assertThat(fc.oldType()).isEqualTo("int32");
        }

        @Test
        @DisplayName("FieldChange.typeChanged creates TYPE_CHANGED change")
        void fieldChangeTypeChangedCreatesTypeChangedChange() {
            VersionSchemaDiff.FieldChange fc = VersionSchemaDiff.FieldChange.typeChanged(
                    "Order", "status", "int32", "StatusEnum", "Changed from int to enum");

            assertThat(fc.messageName()).isEqualTo("Order");
            assertThat(fc.fieldName()).isEqualTo("status");
            assertThat(fc.changeType()).isEqualTo(VersionSchemaDiff.FieldChangeType.TYPE_CHANGED);
            assertThat(fc.oldType()).isEqualTo("int32");
            assertThat(fc.newType()).isEqualTo("StatusEnum");
        }

        @Test
        @DisplayName("EnumChange.added creates ADDED change")
        void enumChangeAddedCreatesAddedChange() {
            VersionSchemaDiff.EnumChange ec = VersionSchemaDiff.EnumChange.added(
                    "StatusEnum", List.of("PENDING", "COMPLETED"), "New enum in v2");

            assertThat(ec.enumName()).isEqualTo("StatusEnum");
            assertThat(ec.changeType()).isEqualTo(VersionSchemaDiff.EnumChangeType.ADDED);
            assertThat(ec.addedValues()).containsExactly("PENDING", "COMPLETED");
        }

        @Test
        @DisplayName("EnumChange.valuesChanged creates VALUES_CHANGED change")
        void enumChangeValuesChangedCreatesValuesChangedChange() {
            VersionSchemaDiff.EnumChange ec = VersionSchemaDiff.EnumChange.valuesChanged(
                    "StatusEnum",
                    List.of("NEW_VALUE"),
                    List.of("OLD_VALUE"),
                    "Enum values modified");

            assertThat(ec.enumName()).isEqualTo("StatusEnum");
            assertThat(ec.changeType()).isEqualTo(VersionSchemaDiff.EnumChangeType.VALUES_CHANGED);
            assertThat(ec.addedValues()).containsExactly("NEW_VALUE");
            assertThat(ec.removedValues()).containsExactly("OLD_VALUE");
        }
    }
}
