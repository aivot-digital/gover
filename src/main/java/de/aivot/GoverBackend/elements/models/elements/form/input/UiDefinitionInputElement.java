package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class UiDefinitionInputElement extends BaseInputElement<BaseElement> implements PrintableElement<BaseElement> {
    @Nullable
    private ElementType elementType;

    public UiDefinitionInputElement() {
        super(ElementType.UiDefinitionInput);
    }

    @Override
    public BaseElement formatValue(Object value) {
        if (value == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, BaseElement.class);
    }

    @Override
    public void performValidation(BaseElement value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
            return;
        }

        if (elementType != null && value.getType() != elementType) {
            throw new ValidationException(this, "Die UI-Definition muss vom Typ " + elementType.name() + " sein.");
        }
    }

    @Override
    public String toDisplayValue(@Nullable BaseElement value) {
        if (!isFilled(value)) {
            return "Keine Angabe";
        }

        return countElements(value) + " Elemente enthalten";
    }

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

    private boolean isFilled(@Nullable BaseElement value) {
        return value != null;
    }

    private int countElements(@Nullable BaseElement element) {
        if (element == null) {
            return 0;
        }

        if (element instanceof LayoutElement<?> layoutElement && layoutElement.getChildren() != null) {
            var total = 1;
            for (var child : layoutElement.getChildren()) {
                total += countElements(child);
            }
            return total;
        }

        return 1;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UiDefinitionInputElement that = (UiDefinitionInputElement) o;
        return elementType == that.elementType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elementType);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public ElementType getElementType() {
        return elementType;
    }

    public UiDefinitionInputElement setElementType(@Nullable ElementType elementType) {
        this.elementType = elementType;
        return this;
    }

    // endregion
}
