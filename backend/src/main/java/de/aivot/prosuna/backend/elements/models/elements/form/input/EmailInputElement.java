package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

public class EmailInputElement extends ContactInputElement {
    @Nullable private Boolean allowLocalDomains;

    public EmailInputElement() { super(ElementType.Email); }

    @Override @Nullable
    protected String normalize(@Nonnull String value) {
        try {
            return EmailAddressUtils.normalizeSingleAddress(value, Boolean.TRUE.equals(allowLocalDomains)
                    ? EmailAddressUtils.DomainPolicy.ALLOW_LOCAL : EmailAddressUtils.DomainPolicy.INTERNET);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
    @Override @Nonnull
    protected String invalidMessage() { return "Bitte geben Sie eine gültige E-Mail-Adresse ein."; }

    @Nullable public Boolean getAllowLocalDomains() { return allowLocalDomains; }
    @Nonnull public EmailInputElement setAllowLocalDomains(@Nullable Boolean value) { allowLocalDomains = value; return this; }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof EmailInputElement that && super.equals(other) && Objects.equals(allowLocalDomains, that.allowLocalDomains);
    }
    @Override public int hashCode() { return Objects.hash(super.hashCode(), allowLocalDomains); }
}
