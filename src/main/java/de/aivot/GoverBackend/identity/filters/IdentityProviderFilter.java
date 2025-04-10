package de.aivot.GoverBackend.identity.filters;

import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class IdentityProviderFilter implements Filter<IdentityProviderEntity> {
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
                .withContains("name", name)
                .withEquals("iconAssetKey", iconAssetKey)
                .withEquals("clientSecretKey", clientSecretKey)
                .withEquals("type", type)
                .withEquals("isEnabled", isEnabled)
                .withEquals("isTestProvider", isTestProvider)
                .build();
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

    public Boolean getEnabled() {
        return isEnabled;
    }

    public IdentityProviderFilter setEnabled(Boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    public Boolean getTestProvider() {
        return isTestProvider;
    }

    public IdentityProviderFilter setTestProvider(Boolean testProvider) {
        isTestProvider = testProvider;
        return this;
    }
}