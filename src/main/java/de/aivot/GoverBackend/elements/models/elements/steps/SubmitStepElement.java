package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.captcha.services.AltchaService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.ElementValidationFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.InputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.SpringContext;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class SubmitStepElement extends BaseStepElement implements InputElement<Map<String, Object>>, PrintableElement<Map<String, Object>> {
    @Nullable
    private String textPreSubmit;
    @Nullable
    private String textPostSubmit;
    @Nullable
    private String textProcessingTime;
    @Nullable
    private Collection<String> documentsToReceive;
    @Nullable
    private Boolean disableConfetti;

    public SubmitStepElement() {
        super(ElementType.SubmitStep);
    }

    @Override
    public Boolean getRequired() {
        return true;
    }

    @Override
    public void performValidation(Map<String, Object> value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException(this, "Bitte bestätigen Sie, dass Sie ein Mensch sind.");
        }

        try {
            var payloadNode = (String) value.get("payload");
            var expiresNode = (Number) value.get("expiresAt");

            if (payloadNode == null || StringUtils.isNullOrEmpty(payloadNode)) {
                throw new ValidationException(this, "Bitte bestätigen Sie, dass Sie ein Mensch sind.");
            }

            // check expiration
            if (expiresNode != null) {
                long now = Instant.now().getEpochSecond();
                long expiresAt = expiresNode.longValue();

                if (expiresAt < now) {
                    throw new ValidationException(this, "Die Captcha-Bestätigung ist abgelaufen. Bitte erneut bestätigen.");
                }
            }

            var altchaService = SpringContext
                    .getBean(AltchaService.class);

            var captchaVerificationStatus = altchaService
                    .verify(payloadNode);

            if (!captchaVerificationStatus) {
                throw new ValidationException(this, "Captcha-Verifizierung fehlgeschlagen. Bitte erneut bestätigen.");
            }

        } catch (Exception e) {
            throw new ValidationException(this, "Verifizierung des Captcha fehlgeschlagen.");
        }
    }

    @Override
    public Map<String, Object> formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Nullable
    @Override
    public ElementValueFunctions getValue() {
        return null;
    }

    @Nullable
    @Override
    public ElementValidationFunctions getValidation() {
        return null;
    }

    @Nullable
    @Override
    public Boolean getDisabled() {
        return false;
    }

    @Nullable
    public static Map<String, Object> _formatValue(@Nullable Object value) {
        var om = ObjectMapperFactory
                .getInstance();

        if (value instanceof String sValue) {
            try {
                return om.valueToTree(sValue);
            } catch (Exception e) {
                return null;
            }
        }

        try {
            return (Map<String, Object>) om.convertValue(value, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "Nicht bestätigt";
        }

        try {
            var payloadNode = value.get("payload");
            if (payloadNode != null) {
                return "Bestätigt";
            }
        } catch (Exception e) {
            // Ignore and return default
        }

        return "Nicht bestätigt";
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SubmitStepElement that = (SubmitStepElement) o;
        return Objects.equals(textPreSubmit, that.textPreSubmit) && Objects.equals(textPostSubmit, that.textPostSubmit) && Objects.equals(textProcessingTime, that.textProcessingTime) && Objects.equals(documentsToReceive, that.documentsToReceive) && Objects.equals(disableConfetti, that.disableConfetti);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), textPreSubmit, textPostSubmit, textProcessingTime, documentsToReceive, disableConfetti);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getTextPreSubmit() {
        return textPreSubmit;
    }

    public SubmitStepElement setTextPreSubmit(@Nullable String textPreSubmit) {
        this.textPreSubmit = textPreSubmit;
        return this;
    }

    @Nullable
    public String getTextPostSubmit() {
        return textPostSubmit;
    }

    public SubmitStepElement setTextPostSubmit(@Nullable String textPostSubmit) {
        this.textPostSubmit = textPostSubmit;
        return this;
    }

    @Nullable
    public String getTextProcessingTime() {
        return textProcessingTime;
    }

    public SubmitStepElement setTextProcessingTime(@Nullable String textProcessingTime) {
        this.textProcessingTime = textProcessingTime;
        return this;
    }

    @Nullable
    public Collection<String> getDocumentsToReceive() {
        return documentsToReceive;
    }

    public SubmitStepElement setDocumentsToReceive(@Nullable Collection<String> documentsToReceive) {
        this.documentsToReceive = documentsToReceive;
        return this;
    }

    @Nullable
    public Boolean getDisableConfetti() {
        return disableConfetti;
    }

    public SubmitStepElement setDisableConfetti(@Nullable Boolean disableConfetti) {
        this.disableConfetti = disableConfetti;
        return this;
    }

    // endregion
}
