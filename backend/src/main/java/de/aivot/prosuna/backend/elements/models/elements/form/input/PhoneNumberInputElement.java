package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.PhoneNumberValidation;
import de.aivot.prosuna.backend.utils.PhoneNumberUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class PhoneNumberInputElement extends ContactInputElement {
    @Nullable private String defaultCountry;
    @Nullable private List<String> preferredCountries;
    @Nullable private PhoneNumberValidation validationMode;

    public PhoneNumberInputElement() { super(ElementType.PhoneNumber); }

    @Override @Nullable
    protected String normalize(@Nonnull String value) {
        return validationMode == PhoneNumberValidation.Strict
                ? PhoneNumberUtils.normalizeValidPhoneNumberToE164(value)
                : PhoneNumberUtils.normalizePossiblePhoneNumberToE164(value);
    }
    @Override @Nonnull
    protected String invalidMessage() { return "Bitte geben Sie eine gültige Telefonnummer mit Ländervorwahl ein."; }

    @Override @Nonnull
    public String toDisplayValue(@Nullable String value) {
        var formatted = PhoneNumberUtils.formatPhoneNumber(value, false);
        return formatted == null ? super.toDisplayValue(value) : formatted;
    }

    @Nullable public String getDefaultCountry() { return defaultCountry; }
    @Nonnull public PhoneNumberInputElement setDefaultCountry(@Nullable String value) { defaultCountry = value; return this; }
    @Nullable public List<String> getPreferredCountries() { return preferredCountries; }
    @Nonnull public PhoneNumberInputElement setPreferredCountries(@Nullable List<String> value) { preferredCountries = value; return this; }
    @Nullable public PhoneNumberValidation getValidationMode() { return validationMode; }
    @Nonnull public PhoneNumberInputElement setValidationMode(@Nullable PhoneNumberValidation value) { validationMode = value; return this; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof PhoneNumberInputElement that && super.equals(other)
                && Objects.equals(defaultCountry, that.defaultCountry) && Objects.equals(preferredCountries, that.preferredCountries)
                && validationMode == that.validationMode;
    }
    @Override public int hashCode() { return Objects.hash(super.hashCode(), defaultCountry, preferredCountries, validationMode); }
}
