package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class HeadlineContentElement extends BaseFormElement {
    @Nullable
    private String content;
    @Nullable
    private Boolean small;
    @Nullable
    private Boolean uppercase;

    public HeadlineContentElement() {
        super(ElementType.Headline);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        HeadlineContentElement headline = (HeadlineContentElement) o;
        return Objects.equals(content, headline.content) && Objects.equals(small, headline.small) && Objects.equals(uppercase, headline.uppercase);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(content);
        result = 31 * result + Objects.hashCode(small);
        result = 31 * result + Objects.hashCode(uppercase);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getContent() {
        return content;
    }

    @Nonnull
    public String getResolvedContent() {
        if (StringUtils.isNotNullOrEmpty(content)) {
            return content;
        }
        return "Überschrift";
    }

    public HeadlineContentElement setContent(@Nullable String content) {
        this.content = content;
        return this;
    }

    @Nullable
    public Boolean getSmall() {
        return small;
    }

    public HeadlineContentElement setSmall(@Nullable Boolean small) {
        this.small = small;
        return this;
    }

    @Nullable
    public Boolean getUppercase() {
        return uppercase;
    }

    public HeadlineContentElement setUppercase(@Nullable Boolean uppercase) {
        this.uppercase = uppercase;
        return this;
    }

    // endregion
}
