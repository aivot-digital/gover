package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HtmlTemplateInputElement extends BaseInputElement<HtmlTemplateInputElementValue> implements PrintableElement<HtmlTemplateInputElementValue> {
    public HtmlTemplateInputElement() {
        super(ElementType.HtmlTemplateInput);
    }

    @Override
    public HtmlTemplateInputElementValue formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        return JsonMapperFactory
                .getInstance()
                .convertValue(value, HtmlTemplateInputElementValue.class);
    }

    @Override
    public void performValidation(@Nullable HtmlTemplateInputElementValue value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !isFilled(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable HtmlTemplateInputElementValue value) {
        if (!isFilled(value)) {
            return "Keine Angabe";
        }

        var slotCount = value.getSlots() == null ? 0 : value
                .getSlots()
                .values()
                .stream()
                .filter(StringUtils::isNotNullOrEmpty)
                .count();
        return slotCount + " Slot" + (slotCount == 1 ? "" : "s") + " ausgefüllt";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var value = formatValue(referencedValue);
        var isEmpty = !isFilled(value);

        return switch (operator) {
            case Empty -> isEmpty;
            case NotEmpty -> !isEmpty;
            default -> false;
        };
    }

    private boolean isFilled(@Nullable HtmlTemplateInputElementValue value) {
        return value != null && StringUtils.isNotNullOrEmpty(value.getAssetKey());
    }
}
