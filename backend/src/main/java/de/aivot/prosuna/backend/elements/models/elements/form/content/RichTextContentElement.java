package de.aivot.prosuna.backend.elements.models.elements.form.content;

import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RichTextContentElement extends BaseFormElement {
    @Nullable
    private String content;

    public RichTextContentElement() {
        super(ElementType.RichText);
    }

    @Nonnull
    @Override
    public Map<String, List<String>> validateConfiguration() {
        var errors = super.validateConfiguration();
        if (isRichTextEmpty(content) && !hasOverride()) {
            addConfigurationError(errors, "content", "Geben Sie einen Text ein.");
        }
        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RichTextContentElement richText = (RichTextContentElement) o;
        return Objects.equals(content, richText.content);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(content);
        return result;
    }

    @Nullable
    public String getContent() {
        return content;
    }

    public RichTextContentElement setContent(@Nullable String content) {
        this.content = content;
        return this;
    }
}
