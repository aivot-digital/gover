package de.aivot.GoverBackend.elements.models.elements.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;

public class SubmitStepElement extends BaseInputElement<JsonNode> {
    @Nullable
    private String textPreSubmit;
    @Nullable
    private String textPostSubmit;
    @Nullable
    private String textProcessingTime;
    @Nullable
    private Collection<String> documentsToReceive;

    public SubmitStepElement() {
        super(ElementType.SubmitStep);
    }

    @Override
    public void performValidation(JsonNode value) throws ValidationException {
        if (value == null || value.isNull()) {
            throw new ValidationException(this, "Bitte bestätigen Sie, dass Sie ein Mensch sind.");
        }

        try {
            var payloadNode = value.get("payload");
            var expiresNode = value.get("expiresAt");

            if (payloadNode == null || payloadNode.isNull() || payloadNode.asText().isBlank()) {
                throw new ValidationException(this, "Bitte bestätigen Sie, dass Sie ein Mensch sind.");
            }

            // check expiration
            if (expiresNode != null && expiresNode.isNumber()) {
                long now = java.time.Instant.now().getEpochSecond();
                long expiresAt = expiresNode.asLong();

                if (expiresAt < now) {
                    throw new ValidationException(this, "Die Captcha-Bestätigung ist abgelaufen. Bitte erneut bestätigen.");
                }
            }
        } catch (Exception e) {
            throw new ValidationException(this, "Die Captcha-Daten konnten nicht gelesen werden. Bitte erneut versuchen.");
        }
    }

    @Override
    public JsonNode formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Nullable
    public static JsonNode _formatValue(@Nullable Object value) {
        if (value instanceof JsonNode jsonNode) {
            return jsonNode;
        }

        if (value instanceof String sValue) {
            try {
                var mapper = new ObjectMapper();
                return mapper.readTree(sValue);
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable JsonNode value) {
        if (value == null || value.isNull()) {
            return "Nicht bestätigt";
        }

        try {
            var payloadNode = value.get("payload");
            if (payloadNode != null && !payloadNode.isNull()) {
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
        return Objects.equals(textPreSubmit, that.textPreSubmit) && Objects.equals(textPostSubmit, that.textPostSubmit) && Objects.equals(textProcessingTime, that.textProcessingTime) && Objects.equals(documentsToReceive, that.documentsToReceive);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(textPreSubmit);
        result = 31 * result + Objects.hashCode(textPostSubmit);
        result = 31 * result + Objects.hashCode(textProcessingTime);
        result = 31 * result + Objects.hashCode(documentsToReceive);
        return result;
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

    // endregion
}
