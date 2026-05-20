package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public class ProcessDataKeyInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    private static final String PROCESS_DATA_KEY_REGEX = "[a-zA-Z0-9\\.\\*_]+";
    private static final Pattern PROCESS_DATA_KEY_PATTERN = Pattern.compile(PROCESS_DATA_KEY_REGEX);

    private Boolean disableWildCards;

    public ProcessDataKeyInputElement() {
        super(ElementType.ProcessDataKeyInput);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        return StringUtils.toNullableTrimmedString(value);
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }

            return;
        }

        if (!PROCESS_DATA_KEY_PATTERN.matcher(value).matches()) {
            throw new ValidationException(this, "Der Prozessdaten-Schlüssel darf nur Buchstaben (A-Z), Zahlen, Punkte, Unterstriche und Sternchen enthalten.");
        }

        if (Boolean.TRUE.equals(disableWildCards)) {
            if (value.contains("*")) {
                throw new ValidationException(this, "Der Prozessdaten-Schlüssel darf keine Sternchen enthalten, da Wildcards deaktiviert sind.");
            }
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return "Keine Angabe";
        }

        return value;
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = formatValue(referencedValue);
        var valueB = formatValue(comparedValue);

        return switch (operator) {
            case Equals -> Objects.equals(valueA, valueB);
            case NotEquals -> !Objects.equals(valueA, valueB);
            case Empty -> StringUtils.isNullOrEmpty(valueA);
            case NotEmpty -> StringUtils.isNotNullOrEmpty(valueA);
            default -> false;
        };
    }

    public Boolean getDisableWildCards() {
        return disableWildCards;
    }

    public ProcessDataKeyInputElement setDisableWildCards(Boolean disableWildCards) {
        this.disableWildCards = disableWildCards;
        return this;
    }
}
