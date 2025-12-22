package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class RichTextInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String content;

    public RichTextInputElement() {
        super(ElementType.RichTextInput);
    }

    @Override
    public String formatValue(Object value) {
        return switch (value) {
            case String sValue -> sValue;
            case null, default -> null;
        };
    }

    @Override
    public void performValidation(String value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable String value) {
        return value != null ? value : "Keine Angabe";
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RichTextInputElement that = (RichTextInputElement) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getContent() {
        return content;
    }

    public RichTextInputElement setContent(@Nullable String content) {
        this.content = content;
        return this;
    }

    // endregion
}
