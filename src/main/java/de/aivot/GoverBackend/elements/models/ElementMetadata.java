package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElementMetadata {
    /**
     * @deprecated use {@link #identityMappings} instead. This needs to be present for compatability reasons.
     */
    private String bundIdMapping;
    /**
      @deprecated use {@link #identityMappings} instead. This needs to be present for compatability reasons.
     */
    private String bayernIdMapping;
    /**
      @deprecated use {@link #identityMappings} instead. This needs to be present for compatability reasons.
     */
    private String shIdMapping;
    /**
      @deprecated use {@link #identityMappings} instead. This needs to be present for compatability reasons.
     */
    private String mukMapping;
    private Map<String, String> identityMappings;
    private String userInfoIdentifier;

    public ElementMetadata(Map<String, Object> values) {
        this.bundIdMapping = MapUtils.getString(values, "bundIdMapping", null);
        this.bayernIdMapping = MapUtils.getString(values, "bayernIdMapping", null);
        this.shIdMapping = MapUtils.getString(values, "shIdMapping", null);
        this.mukMapping = MapUtils.getString(values, "mukMapping", null);
        this.identityMappings = (Map<String, String>) MapUtils.get(values, "identityMappings", Map.class);
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
        if (!Objects.equals(identityMappings, that.identityMappings)) return false;
        return Objects.equals(userInfoIdentifier, that.userInfoIdentifier);
    }

    @Override
    public int hashCode() {
        int result = bundIdMapping != null ? bundIdMapping.hashCode() : 0;
        result = 31 * result + (bayernIdMapping != null ? bayernIdMapping.hashCode() : 0);
        result = 31 * result + (shIdMapping != null ? shIdMapping.hashCode() : 0);
        result = 31 * result + (mukMapping != null ? mukMapping.hashCode() : 0);
        result = 31 * result + (identityMappings != null ? identityMappings.hashCode() : 0);
        result = 31 * result + (userInfoIdentifier != null ? userInfoIdentifier.hashCode() : 0);
        return result;
    }

    /**
     ** @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @return
     */
    public String getBundIdMapping() {
        return bundIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @param bundIdMapping
     */
    public void setBundIdMapping(String bundIdMapping) {
        this.bundIdMapping = bundIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @return
     */
    public String getBayernIdMapping() {
        return bayernIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @param bayernIdMapping
     */
    public void setBayernIdMapping(String bayernIdMapping) {
        this.bayernIdMapping = bayernIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @return
     */
    public String getShIdMapping() {
        return shIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @param shIdMapping
     */
    public void setShIdMapping(String shIdMapping) {
        this.shIdMapping = shIdMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @return
     */
    public String getMukMapping() {
        return mukMapping;
    }

    /**
      @deprecated use {@link #identityMappings} instead. these methods needs to be here for compatability reasons with older forms
     * @param mukMapping
     */
    public void setMukMapping(String mukMapping) {
        this.mukMapping = mukMapping;
    }

    public String getUserInfoIdentifier() {
        return userInfoIdentifier;
    }

    public void setUserInfoIdentifier(String userInfoIdentifier) {
        this.userInfoIdentifier = userInfoIdentifier;
    }

    public Map<String, String> getIdentityMappings() {
        var mapping = new HashMap<String, String>();

        // Normalize the mappings to use the default metadata identifiers
        if (bayernIdMapping != null) {
            mapping.put(IdentityProviderType.BayernId.getDefaultMetadataIdentifier(), bayernIdMapping);
        }
        if (bundIdMapping != null) {
            mapping.put(IdentityProviderType.BundId.getDefaultMetadataIdentifier(), bundIdMapping);
        }
        if (shIdMapping != null) {
            mapping.put(IdentityProviderType.ShId.getDefaultMetadataIdentifier(), shIdMapping);
        }
        if (mukMapping != null) {
            mapping.put(IdentityProviderType.MUK.getDefaultMetadataIdentifier(), mukMapping);
        }
        // Add any additional identity mappings
        if (identityMappings != null) {
            mapping.putAll(identityMappings);
        }

        return mapping;
    }

    public ElementMetadata setIdentityMappings(Map<String, String> identityMappings) {
        this.identityMappings = identityMappings;
        return this;
    }
}
