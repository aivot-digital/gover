package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class ElementMetadata {
    private String bundIdMapping;
    private String bayernIdMapping;
    private String shIdMapping;
    private String mukMapping;
    private String userInfoIdentifier;

    public ElementMetadata(Map<String, Object> values) {
        this.bundIdMapping = MapUtils.getString(values, "bundIdMapping", null);
        this.bayernIdMapping = MapUtils.getString(values, "bayernIdMapping", null);
        this.shIdMapping = MapUtils.getString(values, "shIdMapping", null);
        this.mukMapping = MapUtils.getString(values, "mukMapping", null);
        this.userInfoIdentifier = MapUtils.getString(values, "userInfoIdentifier", null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElementMetadata that = (ElementMetadata) o;

        if (!Objects.equals(bundIdMapping, that.bundIdMapping)) return false;
        if (!Objects.equals(bayernIdMapping, that.bayernIdMapping)) return false;
        if (!Objects.equals(shIdMapping, that.shIdMapping)) return false;
        if (!Objects.equals(mukMapping, that.mukMapping)) return false;
        return Objects.equals(userInfoIdentifier, that.userInfoIdentifier);
    }

    @Override
    public int hashCode() {
        int result = bundIdMapping != null ? bundIdMapping.hashCode() : 0;
        result = 31 * result + (bayernIdMapping != null ? bayernIdMapping.hashCode() : 0);
        result = 31 * result + (shIdMapping != null ? shIdMapping.hashCode() : 0);
        result = 31 * result + (mukMapping != null ? mukMapping.hashCode() : 0);
        result = 31 * result + (userInfoIdentifier != null ? userInfoIdentifier.hashCode() : 0);
        return result;
    }

    public String getBundIdMapping() {
        return bundIdMapping;
    }

    public void setBundIdMapping(String bundIdMapping) {
        this.bundIdMapping = bundIdMapping;
    }

    public String getBayernIdMapping() {
        return bayernIdMapping;
    }

    public void setBayernIdMapping(String bayernIdMapping) {
        this.bayernIdMapping = bayernIdMapping;
    }

    public String getShIdMapping() {
        return shIdMapping;
    }

    public void setShIdMapping(String shIdMapping) {
        this.shIdMapping = shIdMapping;
    }

    public String getMukMapping() {
        return mukMapping;
    }

    public void setMukMapping(String mukMapping) {
        this.mukMapping = mukMapping;
    }

    public String getUserInfoIdentifier() {
        return userInfoIdentifier;
    }

    public void setUserInfoIdentifier(String userInfoIdentifier) {
        this.userInfoIdentifier = userInfoIdentifier;
    }
}
