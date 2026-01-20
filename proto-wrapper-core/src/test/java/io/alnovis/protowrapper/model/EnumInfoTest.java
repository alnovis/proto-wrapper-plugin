package io.alnovis.protowrapper.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class EnumInfoTest {

    @Test
    void shouldBeEquivalentWhenSameNameAndValues() {
        EnumInfo enum1 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100),
                new EnumInfo.EnumValue("NO_TAX", 0)
        ));

        EnumInfo enum2 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100),
                new EnumInfo.EnumValue("NO_TAX", 0)
        ));

        assertThat(enum1.isEquivalentTo(enum2)).isTrue();
        assertThat(enum2.isEquivalentTo(enum1)).isTrue();
    }

    @Test
    void shouldBeEquivalentWhenValuesInDifferentOrder() {
        EnumInfo enum1 = new EnumInfo("Status", Arrays.asList(
                new EnumInfo.EnumValue("ACTIVE", 1),
                new EnumInfo.EnumValue("INACTIVE", 2),
                new EnumInfo.EnumValue("PENDING", 3)
        ));

        EnumInfo enum2 = new EnumInfo("Status", Arrays.asList(
                new EnumInfo.EnumValue("PENDING", 3),
                new EnumInfo.EnumValue("ACTIVE", 1),
                new EnumInfo.EnumValue("INACTIVE", 2)
        ));

        assertThat(enum1.isEquivalentTo(enum2)).isTrue();
    }

    @Test
    void shouldNotBeEquivalentWhenDifferentNames() {
        EnumInfo enum1 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100)
        ));

        EnumInfo enum2 = new EnumInfo("TaxationTyp", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100)
        ));

        assertThat(enum1.isEquivalentTo(enum2)).isFalse();
    }

    @Test
    void shouldNotBeEquivalentWhenDifferentValueCount() {
        EnumInfo enum1 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100),
                new EnumInfo.EnumValue("NO_TAX", 0)
        ));

        EnumInfo enum2 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100)
        ));

        assertThat(enum1.isEquivalentTo(enum2)).isFalse();
    }

    @Test
    void shouldNotBeEquivalentWhenDifferentValueNumbers() {
        EnumInfo enum1 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100)
        ));

        EnumInfo enum2 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 200)
        ));

        assertThat(enum1.isEquivalentTo(enum2)).isFalse();
    }

    @Test
    void shouldNotBeEquivalentWhenOtherIsNull() {
        EnumInfo enum1 = new EnumInfo("TaxType", Arrays.asList(
                new EnumInfo.EnumValue("VAT", 100)
        ));

        assertThat(enum1.isEquivalentTo(null)).isFalse();
    }

    @Test
    void shouldBeEquivalentForEmptyEnums() {
        EnumInfo enum1 = new EnumInfo("Empty", Collections.emptyList());
        EnumInfo enum2 = new EnumInfo("Empty", Collections.emptyList());

        assertThat(enum1.isEquivalentTo(enum2)).isTrue();
    }

    @Test
    void shouldGetJavaNameRemovingPrefix() {
        EnumInfo.EnumValue value = new EnumInfo.EnumValue("OPERATION_BUY", 1);
        assertThat(value.getJavaName()).isEqualTo("BUY");
    }

    @Test
    void shouldGetJavaNameWithoutPrefixUnchanged() {
        EnumInfo.EnumValue value = new EnumInfo.EnumValue("BUY", 1);
        assertThat(value.getJavaName()).isEqualTo("BUY");
    }

    @Test
    void shouldReturnValueNames() {
        EnumInfo enumInfo = new EnumInfo("Status", Arrays.asList(
                new EnumInfo.EnumValue("ACTIVE", 1),
                new EnumInfo.EnumValue("INACTIVE", 2)
        ));

        assertThat(enumInfo.getValueNames()).containsExactly("ACTIVE", "INACTIVE");
    }
}
