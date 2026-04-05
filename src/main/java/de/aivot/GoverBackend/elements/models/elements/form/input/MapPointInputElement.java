package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

public class MapPointInputElement extends BaseInputElement<MapPointInputElementValue> implements PrintableElement<MapPointInputElementValue> {
    @Nullable
    private Integer zoom;
    @Nullable
    private Double centerLatitude;
    @Nullable
    private Double centerLongitude;

    public MapPointInputElement() {
        super(ElementType.MapPoint);
    }

    @Nullable
    @Override
    public MapPointInputElementValue formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable MapPointInputElementValue value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
            return;
        }

        if ((value.getLatitude() == null) != (value.getLongitude() == null)) {
            throw new ValidationException(this, "Bitte geben Sie sowohl Breitengrad als auch Längengrad an.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable MapPointInputElementValue value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        var hasCoordinates = value.getLatitude() != null && value.getLongitude() != null;
        var hasAddress = value.getAddress() != null && !value.getAddress().isBlank();

        if (hasAddress && hasCoordinates) {
            return value.getAddress() + " (" + String.format("%.6f, %.6f", value.getLatitude(), value.getLongitude()) + ")";
        }
        if (hasAddress) {
            return value.getAddress();
        }
        if (hasCoordinates) {
            return String.format("%.6f, %.6f", value.getLatitude(), value.getLongitude());
        }

        return "Keine Angabe";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var value = _formatValue(referencedValue);
        var isEmpty = value == null || value.isEmpty();
        return switch (operator) {
            case Empty -> isEmpty;
            case NotEmpty -> !isEmpty;
            default -> false;
        };
    }

    @Nullable
    public static MapPointInputElementValue _formatValue(@Nullable Object value) {
        switch (value) {
            case null:
                return null;
            case MapPointInputElementValue val:
                return val.isEmpty() ? null : val;
            case Map<?, ?> map:
                var lat = parseDouble(map.get("latitude"));
                var lon = parseDouble(map.get("longitude"));
                var address = map.get("address") == null ? null : map.get("address").toString();
                var result = new MapPointInputElementValue(lat, lon, address);
                return result.isEmpty() ? null : result;
            default:
                return null;
        }
    }

    @Nullable
    private static Double parseDouble(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MapPointInputElement that = (MapPointInputElement) o;
        return Objects.equals(zoom, that.zoom) && Objects.equals(centerLatitude, that.centerLatitude) && Objects.equals(centerLongitude, that.centerLongitude);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(zoom);
        result = 31 * result + Objects.hashCode(centerLatitude);
        result = 31 * result + Objects.hashCode(centerLongitude);
        return result;
    }

    @Nullable
    public Integer getZoom() {
        return zoom;
    }

    public MapPointInputElement setZoom(@Nullable Integer zoom) {
        this.zoom = zoom;
        return this;
    }

    @Nullable
    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public MapPointInputElement setCenterLatitude(@Nullable Double centerLatitude) {
        this.centerLatitude = centerLatitude;
        return this;
    }

    @Nullable
    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public MapPointInputElement setCenterLongitude(@Nullable Double centerLongitude) {
        this.centerLongitude = centerLongitude;
        return this;
    }
}
