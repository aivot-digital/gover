package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class RichText extends BaseFormElement {
    @Nullable
    private String content;

    public RichText() {
        super(ElementType.Richtext);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RichText richText = (RichText) o;
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

    public RichText setContent(@Nullable String content) {
        this.content = content;
        return this;
    }
}
