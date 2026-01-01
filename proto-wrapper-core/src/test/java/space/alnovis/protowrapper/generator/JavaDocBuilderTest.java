package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.CodeBlock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JavaDocBuilder}.
 */
class JavaDocBuilderTest {

    @Nested
    class BasicContentTests {

        @Test
        void description_addsDescription() {
            String result = JavaDocBuilder.create()
                    .description("This is a test method.")
                    .buildString();

            assertThat(result).contains("This is a test method.");
        }

        @Test
        void paragraph_addsParagraphTags() {
            String result = JavaDocBuilder.create()
                    .paragraph("Some paragraph text.")
                    .buildString();

            assertThat(result).contains("<p>Some paragraph text.</p>");
        }

        @Test
        void bold_addsBoldTags() {
            String result = JavaDocBuilder.create()
                    .bold("Important")
                    .buildString();

            assertThat(result).contains("<b>Important</b>");
        }

        @Test
        void boldParagraph_addsBoldPrefixInParagraph() {
            String result = JavaDocBuilder.create()
                    .boldParagraph("Note:", "This is important.")
                    .buildString();

            assertThat(result).contains("<p><b>Note:</b> This is important.</p>");
        }

        @Test
        void code_addsCodeTags() {
            String result = JavaDocBuilder.create()
                    .code("getValue()")
                    .buildString();

            assertThat(result).contains("{@code getValue()}");
        }

        @Test
        void codeBlock_addsPreTags() {
            String result = JavaDocBuilder.create()
                    .codeBlock("int x = 1;")
                    .buildString();

            assertThat(result).contains("<pre>");
            assertThat(result).contains("int x = 1;");
            assertThat(result).contains("</pre>");
        }
    }

    @Nested
    class TagTests {

        @Test
        void param_addsParamTag() {
            String result = JavaDocBuilder.create()
                    .param("value", "the input value")
                    .buildString();

            assertThat(result).contains("@param value the input value");
        }

        @Test
        void returns_addsReturnTag() {
            String result = JavaDocBuilder.create()
                    .returns("the computed result")
                    .buildString();

            assertThat(result).contains("@return the computed result");
        }

        @Test
        void throws_addsThrowsTag() {
            String result = JavaDocBuilder.create()
                    .throws_("IllegalArgumentException", "if value is null")
                    .buildString();

            assertThat(result).contains("@throws IllegalArgumentException if value is null");
        }

        @Test
        void see_addsSeeTag() {
            String result = JavaDocBuilder.create()
                    .see("Money#getAmount()")
                    .buildString();

            assertThat(result).contains("@see Money#getAmount()");
        }

        @Test
        void apiNote_addsApiNoteTag() {
            String result = JavaDocBuilder.create()
                    .apiNote("This is an implementation detail.")
                    .buildString();

            assertThat(result).contains("@apiNote This is an implementation detail.");
        }

        @Test
        void since_addsSinceTag() {
            String result = JavaDocBuilder.create()
                    .since("1.2.0")
                    .buildString();

            assertThat(result).contains("@since 1.2.0");
        }

        @Test
        void deprecated_addsDeprecatedTag() {
            String result = JavaDocBuilder.create()
                    .deprecated("Use newMethod() instead.")
                    .buildString();

            assertThat(result).contains("@deprecated Use newMethod() instead.");
        }
    }

    @Nested
    class SpecializedContentTests {

        @Test
        void typeConflict_addsFormattedConflictInfo() {
            String result = JavaDocBuilder.create()
                    .typeConflict("WIDENING", "v1=int, v2=long")
                    .buildString();

            assertThat(result).contains("<p><b>Type conflict [WIDENING]:</b> v1=int, v2=long</p>");
        }

        @Test
        void presentInVersions_addsVersionInfo() {
            String result = JavaDocBuilder.create()
                    .presentInVersions("[v1, v2]")
                    .buildString();

            assertThat(result).contains("Present in versions: [v1, v2]");
        }
    }

    @Nested
    class CompleteJavaDocTests {

        @Test
        void fullJavaDoc_containsAllElements() {
            String result = JavaDocBuilder.create()
                    .description("Get the order amount.")
                    .paragraph("This method returns the total amount.")
                    .typeConflict("WIDENING", "v1=int, v2=long")
                    .apiNote("Value is automatically widened.")
                    .param("currency", "The currency code")
                    .returns("The amount")
                    .see("Money#getValue()")
                    .since("1.0.0")
                    .buildString();

            assertThat(result)
                    .contains("Get the order amount.")
                    .contains("<p>This method returns the total amount.</p>")
                    .contains("Type conflict [WIDENING]")
                    .contains("@apiNote Value is automatically widened.")
                    .contains("@param currency The currency code")
                    .contains("@return The amount")
                    .contains("@see Money#getValue()")
                    .contains("@since 1.0.0");
        }

        @Test
        void tagOrder_followsStandard() {
            String result = JavaDocBuilder.create()
                    .deprecated("Old method")
                    .apiNote("Note")
                    .param("x", "first")
                    .param("y", "second")
                    .returns("result")
                    .throws_("Exception", "on error")
                    .see("OtherClass")
                    .since("1.0")
                    .buildString();

            // Verify order: deprecated, apiNote, params, return, throws, see, since
            int deprecatedPos = result.indexOf("@deprecated");
            int apiNotePos = result.indexOf("@apiNote");
            int paramPos = result.indexOf("@param");
            int returnPos = result.indexOf("@return");
            int throwsPos = result.indexOf("@throws");
            int seePos = result.indexOf("@see");
            int sincePos = result.indexOf("@since");

            assertThat(deprecatedPos).isLessThan(apiNotePos);
            assertThat(apiNotePos).isLessThan(paramPos);
            assertThat(paramPos).isLessThan(returnPos);
            assertThat(returnPos).isLessThan(throwsPos);
            assertThat(throwsPos).isLessThan(seePos);
            assertThat(seePos).isLessThan(sincePos);
        }
    }

    @Nested
    class FactoryMethodTests {

        @Test
        void forGetter_createsGetterDoc() {
            String result = JavaDocBuilder.forGetter("amount").buildString();

            assertThat(result)
                    .contains("Get amount value.")
                    .contains("@return amount value");
        }

        @Test
        void forHasMethod_createsHasDoc() {
            String result = JavaDocBuilder.forHasMethod("amount").buildString();

            assertThat(result)
                    .contains("Check if amount is present.")
                    .contains("@return true if the field has a value");
        }

        @Test
        void forSupportsMethod_createsSupportsDoc() {
            String result = JavaDocBuilder.forSupportsMethod("amount", "[v1, v2]").buildString();

            assertThat(result)
                    .contains("Check if amount is available")
                    .contains("present in versions: [v1, v2]")
                    .contains("@return true if this version supports this field");
        }

        @Test
        void forSetter_createsSetterDoc() {
            String result = JavaDocBuilder.forSetter("amount").buildString();

            assertThat(result)
                    .contains("Set amount value.")
                    .contains("@param value The value to set")
                    .contains("@return this builder for chaining");
        }

        @Test
        void forBuildMethod_createsBuildDoc() {
            String result = JavaDocBuilder.forBuildMethod("Money").buildString();

            assertThat(result)
                    .contains("Build the Money instance.")
                    .contains("@return New Money instance");
        }
    }

    @Nested
    class EmptyAndNullHandlingTests {

        @Test
        void isEmpty_emptyBuilder_returnsTrue() {
            assertThat(JavaDocBuilder.create().isEmpty()).isTrue();
        }

        @Test
        void isEmpty_withContent_returnsFalse() {
            assertThat(JavaDocBuilder.create().description("Test").isEmpty()).isFalse();
        }

        @Test
        void isEmpty_withOnlyTags_returnsFalse() {
            assertThat(JavaDocBuilder.create().returns("value").isEmpty()).isFalse();
        }

        @Test
        void nullOrEmptyValues_areIgnored() {
            String result = JavaDocBuilder.create()
                    .description(null)
                    .paragraph("")
                    .param(null, "desc")
                    .param("name", null)
                    .returns(null)
                    .buildString();

            assertThat(result).isEmpty();
        }

        @Test
        void build_returnsCodeBlock() {
            CodeBlock block = JavaDocBuilder.create()
                    .description("Test")
                    .build();

            assertThat(block).isNotNull();
            assertThat(block.toString()).contains("Test");
        }
    }

    @Nested
    class MultipleSeeTagsTest {

        @Test
        void multipleSee_addsAllTags() {
            String result = JavaDocBuilder.create()
                    .see("Class1")
                    .see("Class2")
                    .see("Class3")
                    .buildString();

            assertThat(result)
                    .contains("@see Class1")
                    .contains("@see Class2")
                    .contains("@see Class3");
        }
    }
}
