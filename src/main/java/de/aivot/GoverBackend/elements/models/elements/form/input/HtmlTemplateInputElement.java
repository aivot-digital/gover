package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
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

        return ObjectMapperFactory
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
