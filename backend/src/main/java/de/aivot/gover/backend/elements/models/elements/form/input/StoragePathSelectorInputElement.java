package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.PrintableElement;
import de.aivot.gover.backend.enums.ConditionOperator;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.exceptions.RequiredValidationException;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class StoragePathSelectorInputElement extends BaseInputElement<StoragePathSelectorInputElementValue> implements PrintableElement<StoragePathSelectorInputElementValue> {
    @Nullable
    private String placeholder;

    @Nullable
    private List<StorageProviderType> allowedStorageProviderTypes;

    public StoragePathSelectorInputElement() {
        super(ElementType.StoragePathSelector);
    }

    @Nullable
    @Override
    public StoragePathSelectorInputElementValue formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var formattedValue = ObjectMapperFactory
                .getInstance()
                .convertValue(value, StoragePathSelectorInputElementValue.class);

        return formattedValue.setPath(StringUtils.toNullableTrimmedString(formattedValue.getPath()));
    }

    @Override
    public void performValidation(@Nullable StoragePathSelectorInputElementValue value) throws ValidationException {
        if (!isFilled(value)) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
            return;
        }

        if (value.getStorageProviderId() == null || StringUtils.isNullOrEmpty(value.getPath())) {
            throw new ValidationException(this, "Es muss ein Speicheranbieter und ein Pfad ausgewählt werden.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable StoragePathSelectorInputElementValue value) {
        return value != null && StringUtils.isNotNullOrEmpty(value.getPath())
                ? value.getPath()
                : "Keine Angabe";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = formatValue(referencedValue);
        var valueB = formatValue(comparedValue);
        var isEmpty = !isFilled(valueA);

        return switch (operator) {
            case Equals -> Objects.equals(valueA, valueB);
            case NotEquals -> !Objects.equals(valueA, valueB);
            case Empty -> isEmpty;
            case NotEmpty -> !isEmpty;
            default -> false;
        };
    }

    private boolean isFilled(@Nullable StoragePathSelectorInputElementValue value) {
        return value != null
                && (value.getStorageProviderId() != null || StringUtils.isNotNullOrEmpty(value.getPath()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StoragePathSelectorInputElement that = (StoragePathSelectorInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(allowedStorageProviderTypes, that.allowedStorageProviderTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeholder, allowedStorageProviderTypes);
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public StoragePathSelectorInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public List<StorageProviderType> getAllowedStorageProviderTypes() {
        return allowedStorageProviderTypes;
    }

    public StoragePathSelectorInputElement setAllowedStorageProviderTypes(@Nullable List<StorageProviderType> allowedStorageProviderTypes) {
        this.allowedStorageProviderTypes = allowedStorageProviderTypes;
        return this;
    }
}
