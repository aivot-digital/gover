package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

public abstract class ContactInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable private String placeholder;
    @Nullable private String autocomplete;

    protected ContactInputElement(@Nonnull ElementType type) { super(type); }

    @Override
    public void validate(@Nullable Object value) throws ValidationException {
        if (value != null && !(value instanceof String)) throw new ValidationException(this, invalidMessage());
        super.validate(value);
    }

    @Override
    @Nullable
    public String formatValue(@Nullable Object value) {
        if (value == null) return null;
        if (!(value instanceof String text)) throw new IllegalArgumentException(invalidMessage());
        if (text.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0xFEFF)) return "";
        // Preserve invalid authored or derived values so validation can explain the error.
        var normalized = normalize(text);
        return normalized == null ? text : normalized;
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        if (value != null && !value.isBlank() && normalize(value) == null) throw new ValidationException(this, invalidMessage());
    }

    @Nullable protected abstract String normalize(@Nonnull String value);
    @Nonnull protected abstract String invalidMessage();

    @Override
    @Nonnull
    public String toDisplayValue(@Nullable String value) { return value == null || value.isBlank() ? "Keine Angabe" : value; }

    @Override
    @Nonnull
    public Boolean evaluate(@Nonnull ConditionOperator operator, @Nullable Object a, @Nullable Object b) {
        return new TextInputElement().evaluate(operator, a, b);
    }

    @Nullable public String getPlaceholder() { return placeholder; }
    @Nonnull public ContactInputElement setPlaceholder(@Nullable String value) { placeholder = value; return this; }
    @Nullable public String getAutocomplete() { return autocomplete; }
    @Nonnull public ContactInputElement setAutocomplete(@Nullable String value) { autocomplete = value; return this; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ContactInputElement that && super.equals(other)
                && Objects.equals(placeholder, that.placeholder) && Objects.equals(autocomplete, that.autocomplete);
    }
    @Override public int hashCode() { return Objects.hash(super.hashCode(), placeholder, autocomplete); }
}
