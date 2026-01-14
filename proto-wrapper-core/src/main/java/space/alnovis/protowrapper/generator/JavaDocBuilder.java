package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.CodeBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static space.alnovis.protowrapper.generator.GeneratorUtils.isNullOrEmpty;

/**
 * Fluent builder for constructing JavaDoc comments.
 *
 * <p>This builder simplifies the creation of well-formatted JavaDoc comments
 * for generated code. It handles proper formatting, line breaks, and common
 * JavaDoc tags.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * CodeBlock javadoc = JavaDocBuilder.create()
 *         .description("Get the order amount.")
 *         .paragraph("This method returns the total amount including taxes.")
 *         .typeConflict("WIDENING", "v1=int, v2=long")
 *         .apiNote("Value is automatically widened to the larger type.")
 *         .param("currency", "The currency code")
 *         .returns("The order amount in specified currency")
 *         .see("Money#getAmount()")
 *         .build();
 * </pre>
 *
 * @since 1.2.0
 */
public final class JavaDocBuilder {

    private final List<String> lines = new ArrayList<>();
    private final List<String> params = new ArrayList<>();
    private final List<String> sees = new ArrayList<>();
    private String returns;
    private String throws_;
    private String apiNote;
    private String since;
    private String deprecatedMessage;

    private JavaDocBuilder() {
    }

    /**
     * Create a new JavaDocBuilder.
     *
     * @return New builder instance
     */
    public static JavaDocBuilder create() {
        return new JavaDocBuilder();
    }

    // ==================== Content Methods ====================

    /**
     * Add the main description (first sentence/paragraph).
     *
     * @param text the description text
     * @return this builder
     */
    public JavaDocBuilder description(String text) {
        return addLineAt(0, text, s -> s + "\n");
    }

    /**
     * Add a paragraph wrapped in {@code <p>} tags.
     *
     * @param text the paragraph text
     * @return this builder
     */
    public JavaDocBuilder paragraph(String text) {
        return addLine(text, s -> "<p>" + s + "</p>\n");
    }

    /**
     * Add raw text (no formatting).
     *
     * @param text the raw text
     * @return this builder
     */
    public JavaDocBuilder text(String text) {
        return addLine(text, s -> s + "\n");
    }

    /**
     * Add bold text.
     *
     * @param text the text to make bold
     * @return this builder
     */
    public JavaDocBuilder bold(String text) {
        return addLine(text, s -> "<b>" + s + "</b>\n");
    }

    /**
     * Add a paragraph with bold prefix.
     *
     * @param prefix the bold prefix
     * @param text the paragraph text
     * @return this builder
     */
    public JavaDocBuilder boldParagraph(String prefix, String text) {
        return Optional.of(prefix)
                .filter(p -> !isNullOrEmpty(p) && !isNullOrEmpty(text))
                .map(p -> {
                    lines.add("<p><b>" + p + "</b> " + text + "</p>\n");
                    return this;
                })
                .orElse(this);
    }

    /**
     * Add type conflict documentation.
     *
     * @param conflictType the conflict type name
     * @param typeInfo the type information
     * @return this builder
     */
    public JavaDocBuilder typeConflict(String conflictType, String typeInfo) {
        return Optional.of(conflictType)
                .filter(c -> !isNullOrEmpty(c))
                .map(c -> {
                    lines.add("<p><b>Type conflict [" + c + "]:</b> " +
                              GeneratorUtils.nullToEmpty(typeInfo) + "</p>\n");
                    return this;
                })
                .orElse(this);
    }

    /**
     * Add version availability note.
     *
     * @param versions the versions string
     * @return this builder
     */
    public JavaDocBuilder presentInVersions(String versions) {
        return paragraph("Present in versions: " + GeneratorUtils.nullToEmpty(versions));
    }

    /**
     * Add code example.
     *
     * @param code the code to add
     * @return this builder
     */
    public JavaDocBuilder code(String code) {
        return addLine(code, s -> "{@code " + s + "}\n");
    }

    /**
     * Add a code block (pre-formatted).
     *
     * @param code the code block
     * @return this builder
     */
    public JavaDocBuilder codeBlock(String code) {
        return addLine(code, s -> "<pre>\n" + s + "\n</pre>\n");
    }

    // ==================== Tag Methods ====================

    /**
     * Add {@code @param} tag.
     *
     * @param name the parameter name
     * @param description the parameter description
     * @return this builder
     */
    public JavaDocBuilder param(String name, String description) {
        Optional.ofNullable(name)
                .filter(n -> !isNullOrEmpty(n) && !isNullOrEmpty(description))
                .ifPresent(n -> params.add("@param " + n + " " + description + "\n"));
        return this;
    }

    /**
     * Add {@code @return} tag.
     *
     * @param description the return description
     * @return this builder
     */
    public JavaDocBuilder returns(String description) {
        this.returns = Optional.ofNullable(description)
                .filter(d -> !d.isEmpty())
                .map(d -> "@return " + d + "\n")
                .orElse(null);
        return this;
    }

    /**
     * Add {@code @throws} tag.
     *
     * @param exceptionType the exception type
     * @param description the exception description
     * @return this builder
     */
    public JavaDocBuilder throws_(String exceptionType, String description) {
        this.throws_ = Optional.of(exceptionType)
                .filter(e -> !isNullOrEmpty(e) && !isNullOrEmpty(description))
                .map(e -> "@throws " + e + " " + description + "\n")
                .orElse(null);
        return this;
    }

    /**
     * Add {@code @see} tag.
     *
     * @param reference the reference to add
     * @return this builder
     */
    public JavaDocBuilder see(String reference) {
        Optional.ofNullable(reference)
                .filter(r -> !r.isEmpty())
                .ifPresent(r -> sees.add("@see " + r + "\n"));
        return this;
    }

    /**
     * Add {@code @apiNote} tag.
     *
     * @param note the note text
     * @return this builder
     */
    public JavaDocBuilder apiNote(String note) {
        this.apiNote = Optional.ofNullable(note)
                .filter(n -> !n.isEmpty())
                .map(n -> "@apiNote " + n + "\n")
                .orElse(null);
        return this;
    }

    /**
     * Add {@code @since} tag.
     *
     * @param version the version string
     * @return this builder
     */
    public JavaDocBuilder since(String version) {
        this.since = Optional.ofNullable(version)
                .filter(v -> !v.isEmpty())
                .map(v -> "@since " + v + "\n")
                .orElse(null);
        return this;
    }

    /**
     * Mark as deprecated.
     *
     * @param message the deprecation message
     * @return this builder
     */
    public JavaDocBuilder deprecated(String message) {
        this.deprecatedMessage = Optional.ofNullable(message)
                .filter(m -> !m.isEmpty())
                .map(m -> "@deprecated " + m + "\n")
                .orElse("@deprecated\n");
        return this;
    }

    // ==================== Build Methods ====================

    /**
     * Build the JavaDoc as a CodeBlock.
     *
     * @return the built CodeBlock
     */
    public CodeBlock build() {
        CodeBlock.Builder builder = CodeBlock.builder();

        // Content lines
        lines.forEach(builder::add);

        // Separator before tags
        if (!lines.isEmpty() && hasTags()) {
            builder.add("\n");
        }

        // Tags in standard order
        Stream.of(
                Optional.ofNullable(deprecatedMessage),
                Optional.ofNullable(apiNote)
        ).flatMap(Optional::stream).forEach(builder::add);

        params.forEach(builder::add);

        Stream.of(
                Optional.ofNullable(returns),
                Optional.ofNullable(throws_)
        ).flatMap(Optional::stream).forEach(builder::add);

        sees.forEach(builder::add);

        Optional.ofNullable(since).ifPresent(builder::add);

        return builder.build();
    }

    /**
     * Build as a formatted string (for testing).
     *
     * @return the built string
     */
    public String buildString() {
        return build().toString();
    }

    /**
     * Check if this builder has any content.
     *
     * @return true if builder is empty
     */
    public boolean isEmpty() {
        return lines.isEmpty() && !hasTags();
    }

    // ==================== Factory Methods ====================

    /**
     * Create JavaDoc for a simple getter method.
     *
     * @param fieldName the field name
     * @return new builder for getter
     */
    public static JavaDocBuilder forGetter(String fieldName) {
        return create()
                .description("Get " + fieldName + " value.")
                .returns(fieldName + " value");
    }

    /**
     * Create JavaDoc for a has-method.
     *
     * @param fieldName the field name
     * @return new builder for has-method
     */
    public static JavaDocBuilder forHasMethod(String fieldName) {
        return create()
                .description("Check if " + fieldName + " is present.")
                .returns("true if the field has a value");
    }

    /**
     * Create JavaDoc for a supports-method.
     *
     * @param fieldName the field name
     * @param versions the versions string
     * @return new builder for supports-method
     */
    public static JavaDocBuilder forSupportsMethod(String fieldName, String versions) {
        return create()
                .description("Check if " + fieldName + " is available in the current version.")
                .paragraph("This field is only present in versions: " + versions)
                .returns("true if this version supports this field");
    }

    /**
     * Create JavaDoc for a setter method.
     *
     * @param fieldName the field name
     * @return new builder for setter
     */
    public static JavaDocBuilder forSetter(String fieldName) {
        return create()
                .description("Set " + fieldName + " value.")
                .param("value", "The value to set")
                .returns("this builder for chaining");
    }

    /**
     * Create JavaDoc for a builder build method.
     *
     * @param typeName the type name
     * @return new builder for build method
     */
    public static JavaDocBuilder forBuildMethod(String typeName) {
        return create()
                .description("Build the " + typeName + " instance.")
                .returns("New " + typeName + " instance");
    }

    // ==================== Private Helpers ====================

    private boolean hasTags() {
        return !params.isEmpty() || returns != null || throws_ != null ||
               !sees.isEmpty() || apiNote != null || since != null ||
               deprecatedMessage != null;
    }

    private JavaDocBuilder addLine(String text, java.util.function.Function<String, String> formatter) {
        Optional.ofNullable(text)
                .filter(t -> !t.isEmpty())
                .map(formatter)
                .ifPresent(lines::add);
        return this;
    }

    private JavaDocBuilder addLineAt(int index, String text, java.util.function.Function<String, String> formatter) {
        Optional.ofNullable(text)
                .filter(t -> !t.isEmpty())
                .map(formatter)
                .ifPresent(formatted -> lines.add(Math.min(index, lines.size()), formatted));
        return this;
    }
}
