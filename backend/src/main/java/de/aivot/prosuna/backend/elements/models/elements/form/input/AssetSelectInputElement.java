package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Selects an asset and persists only its stable key as the element value. */
public class AssetSelectInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    @Nullable
    private String dialogTitle;

    @Nullable
    private List<String> allowedMimeTypes;

    @Nonnull
    private AssetVisibility assetVisibility = AssetVisibility.All;

    public AssetSelectInputElement() {
        super(ElementType.AssetSelectInput);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        return StringUtils.toNullableTrimmedString(value);
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        if (value == null) {
            return;
        }

        try {
            var assetKey = UUID.fromString(value);
            if (!assetKey.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Non-canonical UUID");
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(this, "Bitte wählen Sie eine gültige Datei aus.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        return StringUtils.isNullOrEmpty(value) ? "Keine Angabe" : value;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssetSelectInputElement that = (AssetSelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder)
                && Objects.equals(dialogTitle, that.dialogTitle)
                && Objects.equals(allowedMimeTypes, that.allowedMimeTypes)
                && assetVisibility == that.assetVisibility;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeholder, dialogTitle, allowedMimeTypes, assetVisibility);
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public AssetSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getDialogTitle() {
        return dialogTitle;
    }

    public AssetSelectInputElement setDialogTitle(@Nullable String dialogTitle) {
        this.dialogTitle = dialogTitle;
        return this;
    }

    @Nullable
    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public AssetSelectInputElement setAllowedMimeTypes(@Nullable List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
        return this;
    }

    @Nonnull
    public AssetVisibility getAssetVisibility() {
        return assetVisibility;
    }

    public AssetSelectInputElement setAssetVisibility(@Nullable AssetVisibility assetVisibility) {
        this.assetVisibility = Objects.requireNonNullElse(assetVisibility, AssetVisibility.All);
        return this;
    }
}
