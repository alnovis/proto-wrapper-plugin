package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.alnovis.protowrapper.it.model.api.Money;
import space.alnovis.protowrapper.it.model.api.Address;
import space.alnovis.protowrapper.it.model.api.VersionContext;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for equals() and hashCode() implementation in generated wrapper classes.
 */
@DisplayName("Wrapper equals/hashCode")
public class EqualsHashCodeTest {

    @Nested
    @DisplayName("Top-level wrapper equality")
    class TopLevelEquality {

        @Test
        @DisplayName("Same version, same content should be equal")
        void sameVersionSameContent() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money money1 = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money money2 = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertEquals(money1, money2, "Same content should be equal");
            assertEquals(money1.hashCode(), money2.hashCode(), "Hash codes should match for equal objects");
        }

        @Test
        @DisplayName("Same version, different content should not be equal")
        void sameVersionDifferentContent() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money money1 = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money money2 = ctx.newMoneyBuilder()
                    .setAmount(2000)
                    .setCurrency("EUR")
                    .build();

            assertNotEquals(money1, money2, "Different content should not be equal");
        }

        @Test
        @DisplayName("Different version, same content should not be equal")
        void differentVersionSameContent() {
            Money money1 = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money money2 = VersionContext.forVersion(2).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertNotEquals(money1, money2, "Different versions should not be equal even with same content");
        }

        @Test
        @DisplayName("Comparison with null should return false")
        void comparisonWithNull() {
            Money money = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertNotEquals(null, money);
            assertFalse(money.equals(null));
        }

        @Test
        @DisplayName("Comparison with different type should return false")
        void comparisonWithDifferentType() {
            Money money = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertNotEquals("string", money);
            assertFalse(money.equals(123));
        }

        @Test
        @DisplayName("Self-equality should return true")
        void selfEquality() {
            Money money = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            assertEquals(money, money, "Object should be equal to itself");
        }

        @Test
        @DisplayName("HashSet should work correctly with wrappers")
        void hashSetBehavior() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money money1 = ctx.newMoneyBuilder().setAmount(100).setCurrency("USD").build();
            Money money2 = ctx.newMoneyBuilder().setAmount(200).setCurrency("EUR").build();
            Money money1Copy = ctx.newMoneyBuilder().setAmount(100).setCurrency("USD").build();

            Set<Money> set = new HashSet<>();
            set.add(money1);
            set.add(money2);
            set.add(money1Copy); // Should not add duplicate

            assertEquals(2, set.size(), "HashSet should contain 2 unique money objects");
            assertTrue(set.contains(money1));
            assertTrue(set.contains(money2));
            assertTrue(set.contains(money1Copy), "HashSet should find equal object");
        }
    }

    @Nested
    @DisplayName("toBuilder equality")
    class ToBuilderEquality {

        @Test
        @DisplayName("toBuilder().build() should create equal object")
        void toBuilderCreateEqualObject() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money original = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money copy = original.toBuilder().build();

            assertEquals(original, copy, "toBuilder().build() should create equal object");
            assertEquals(original.hashCode(), copy.hashCode());
        }

        @Test
        @DisplayName("Modified copy should not be equal to original")
        void modifiedCopyNotEqual() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money original = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Money modified = original.toBuilder()
                    .setAmount(2000)
                    .build();

            assertNotEquals(original, modified, "Modified copy should not be equal");
        }
    }

    @Nested
    @DisplayName("Hash code consistency")
    class HashCodeConsistency {

        @Test
        @DisplayName("hashCode should be consistent across multiple calls")
        void hashCodeConsistency() {
            Money money = VersionContext.forVersion(1).newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            int hash1 = money.hashCode();
            int hash2 = money.hashCode();
            int hash3 = money.hashCode();

            assertEquals(hash1, hash2);
            assertEquals(hash2, hash3);
        }

        @Test
        @DisplayName("Equal objects should have same hashCode")
        void equalObjectsSameHashCode() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money money1 = ctx.newMoneyBuilder().setAmount(100).setCurrency("Test").build();
            Money money2 = ctx.newMoneyBuilder().setAmount(100).setCurrency("Test").build();

            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode(),
                    "Equal objects must have the same hashCode (hashCode contract)");
        }
    }

    @Nested
    @DisplayName("Wrapped from proto equality")
    class WrappedFromProtoEquality {

        @Test
        @DisplayName("Wrapper from same proto should be equal")
        void wrapperFromSameProto() {
            space.alnovis.protowrapper.it.proto.v1.Common.Money proto =
                    space.alnovis.protowrapper.it.proto.v1.Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build();

            VersionContext ctx = VersionContext.forVersion(1);
            Money money1 = ctx.wrapMoney(proto);
            Money money2 = ctx.wrapMoney(proto);

            assertEquals(money1, money2, "Wrappers from same proto should be equal");
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("Wrapper from equal proto should be equal")
        void wrapperFromEqualProto() {
            space.alnovis.protowrapper.it.proto.v1.Common.Money proto1 =
                    space.alnovis.protowrapper.it.proto.v1.Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build();

            space.alnovis.protowrapper.it.proto.v1.Common.Money proto2 =
                    space.alnovis.protowrapper.it.proto.v1.Common.Money.newBuilder()
                            .setAmount(1000)
                            .setCurrency("USD")
                            .build();

            VersionContext ctx = VersionContext.forVersion(1);
            Money money1 = ctx.wrapMoney(proto1);
            Money money2 = ctx.wrapMoney(proto2);

            assertEquals(money1, money2, "Wrappers from equal protos should be equal");
            assertEquals(money1.hashCode(), money2.hashCode());
        }
    }

    @Nested
    @DisplayName("Different message types")
    class DifferentMessageTypes {

        @Test
        @DisplayName("Different message types should not be equal")
        void differentMessageTypesNotEqual() {
            VersionContext ctx = VersionContext.forVersion(1);

            Money money = ctx.newMoneyBuilder()
                    .setAmount(1000)
                    .setCurrency("USD")
                    .build();

            Address address = ctx.newAddressBuilder()
                    .setStreet("123 Main St")
                    .setCity("NYC")
                    .setCountry("USA")
                    .build();

            // Different types should not be equal even if they're both wrappers
            assertNotEquals(money, address);
        }
    }
}
