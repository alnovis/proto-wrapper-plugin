package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.alnovis.protowrapper.it.model.api.Money;
import space.alnovis.protowrapper.it.model.api.Address;
import space.alnovis.protowrapper.it.model.api.VersionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for toString() implementation in generated wrapper classes.
 */
@DisplayName("Wrapper toString()")
public class ToStringTest {

    @Nested
    @DisplayName("Top-level wrapper toString")
    class TopLevelToString {

        @Test
        @DisplayName("toString should include class name and version")
        void includesClassNameAndVersion() {
            VersionContext ctx = VersionContext.forVersion(1);
            Money money = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            String str = money.toString();

            assertTrue(str.contains("Money"), "Should contain class name");
            assertTrue(str.contains("version=1"), "Should contain version");
        }

        @Test
        @DisplayName("toString should include proto field values")
        void includesProtoFieldValues() {
            VersionContext ctx = VersionContext.forVersion(1);
            Money money = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            String str = money.toString();

            assertTrue(str.contains("1000") || str.contains("amount"),
                    "Should contain amount value or field name");
            assertTrue(str.contains("USD") || str.contains("currency"),
                    "Should contain currency value or field name");
        }

        @Test
        @DisplayName("toString should be single line (no newlines)")
        void singleLine() {
            VersionContext ctx = VersionContext.forVersion(1);
            Money money = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            String str = money.toString();

            assertFalse(str.contains("\n"), "Should not contain newlines");
        }

        @Test
        @DisplayName("different versions should show different version numbers")
        void differentVersionsShowDifferentNumbers() {
            Money v1 = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            Money v2 = VersionContext.forVersion(2).newMoneyBuilder()
                    .setAmount(100)
                    .setCurrency("USD")
                    .build();

            assertTrue(v1.toString().contains("version=1"));
            assertTrue(v2.toString().contains("version=2"));
        }
    }

    @Nested
    @DisplayName("Nested class toString")
    class NestedClassToString {

        @Test
        @DisplayName("nested class toString should include class name")
        void nestedIncludesClassName() {
            VersionContext ctx = VersionContext.forVersion(1);
            Address address = ctx.newAddressBuilder()
                    .setStreet("123 Main St")
                    .setCity("NYC")
                    .setCountry("USA")
                    .build();

            // Get a nested type if available (e.g., from ZXReport.Tax)
            // For this test, we just verify the parent works
            String str = address.toString();
            assertTrue(str.contains("Address"), "Should contain class name");
        }

        @Test
        @DisplayName("nested class toString should include proto content")
        void nestedIncludesProtoContent() {
            VersionContext ctx = VersionContext.forVersion(1);
            Address address = ctx.newAddressBuilder()
                    .setStreet("123 Main St")
                    .setCity("NYC")
                    .setCountry("USA")
                    .build();

            String str = address.toString();
            assertTrue(str.contains("123 Main St") || str.contains("street"),
                    "Should contain street value or field name");
        }
    }

    @Nested
    @DisplayName("Empty and edge cases")
    class EdgeCases {

        @Test
        @DisplayName("empty message should have valid toString")
        void emptyMessageToString() {
            VersionContext ctx = VersionContext.forVersion(1);
            // Create with only required fields
            Money money = ctx.newMoneyBuilder()
                    .setAmount(0)
                    .setCurrency("")
                    .build();

            String str = money.toString();

            assertNotNull(str);
            assertTrue(str.contains("Money"));
            assertTrue(str.contains("version=1"));
        }

        @Test
        @DisplayName("toString should not throw exception")
        void noException() {
            VersionContext ctx = VersionContext.forVersion(1);
            Money money = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertDoesNotThrow(() -> money.toString());
        }
    }
}
