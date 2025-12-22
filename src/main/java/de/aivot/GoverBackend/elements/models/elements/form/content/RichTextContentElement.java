package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class RichTextContentElement extends BaseFormElement {
    @Nullable
    private String content;

    public RichTextContentElement() {
        super(ElementType.RichText);
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
