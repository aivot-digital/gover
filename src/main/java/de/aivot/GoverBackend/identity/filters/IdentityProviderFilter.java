package de.aivot.GoverBackend.identity.filters;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class IdentityProviderFilter implements Filter<IdentityProviderEntity> {
    private String key;
    private List<String> keys;
    private String name;
    private String iconAssetKey;
    private String clientSecretKey;
    private IdentityProviderType type;
    private Boolean isEnabled;
    private Boolean isTestProvider;

    public static IdentityProviderFilter create() {
        return new IdentityProviderFilter();
    }

    @Override
    public Specification<IdentityProviderEntity> build() {
        return SpecificationBuilder
                .create(IdentityProviderEntity.class)
                .withEquals("key", key)
                .withInList("key", keys)
                .withContains("name", name)
                .withEquals("iconAssetKey", iconAssetKey)
                .withEquals("clientSecretKey", clientSecretKey)
                .withEquals("type", type)
                .withEquals("isEnabled", isEnabled)
                .withEquals("isTestProvider", isTestProvider)
                .build();
    }

    public String getKey() {
        return key;
    }

    public IdentityProviderFilter setKey(String key) {
        this.key = key;
        return this;
    }

    public List<String> getKeys() {
        return keys;
    }

    public IdentityProviderFilter setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }

    public String getName() {
        return name;
    }

    public IdentityProviderFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getIconAssetKey() {
        return iconAssetKey;
    }

    public IdentityProviderFilter setIconAssetKey(String iconAssetKey) {
        this.iconAssetKey = iconAssetKey;
        return this;
    }

    public String getClientSecretKey() {
        return clientSecretKey;
    }

    public IdentityProviderFilter setClientSecretKey(String clientSecretKey) {
        this.clientSecretKey = clientSecretKey;
        return this;
    }

    public IdentityProviderType getType() {
        return type;
    }

    public IdentityProviderFilter setType(IdentityProviderType type) {
        this.type = type;
        return this;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public IdentityProviderFilter setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    public Boolean getIsTestProvider() {
        return isTestProvider;
    }

    public IdentityProviderFilter setIsTestProvider(Boolean testProvider) {
        isTestProvider = testProvider;
        return this;
    }
}