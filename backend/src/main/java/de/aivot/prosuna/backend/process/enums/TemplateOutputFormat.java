package de.aivot.prosuna.backend.process.enums;

/**
 * The kind of text a template renders into. It decides how {@code {{ ... }}} and {@code {! ... !}} insert values, so
 * process data cannot change the structure or formatting of the text around them.
 */
public enum TemplateOutputFormat {
    /**
     * HTML that may also contain scripts, for example PDF templates. {@code {{ ... }}} escapes values for HTML and
     * JavaScript, {@code {! ... !}} inserts them unchanged.
     */
    Html,
    /**
     * Markdown that is rendered by {@code MarkdownRenderer} or the frontend renderer, for example mail texts.
     * {@code {{ ... }}} inserts values as literal text, {@code {! ... !}} inserts them as Markdown with visible link
     * targets.
     */
    Markdown,
    /** Text without markup, for example AI prompts. Both tags insert values unchanged. */
    PlainText,
    /** A single line of text, for example a mail subject. Both tags replace line breaks in values with spaces. */
    SingleLineText
}
