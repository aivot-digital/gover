package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class MapPointInputElementValue implements Serializable {
    @Nullable
    private Double latitude;
    @Nullable
    private Double longitude;
    @Nullable
    private String address;

    public MapPointInputElementValue() {
    }

    public MapPointInputElementValue(@Nullable Double latitude, @Nullable Double longitude, @Nullable String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public boolean isEmpty() {
        return latitude == null && longitude == null && (address == null || address.isBlank());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MapPointInputElementValue that = (MapPointInputElementValue) o;
        return Objects.equals(latitude, that.latitude) && Objects.equals(longitude, that.longitude) && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, address);
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    public MapPointInputElementValue setLatitude(@Nullable Double latitude) {
        this.latitude = latitude;
        return this;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

    public MapPointInputElementValue setLongitude(@Nullable Double longitude) {
        this.longitude = longitude;
        return this;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    public MapPointInputElementValue setAddress(@Nullable String address) {
        this.address = address;
        return this;
    }
}
