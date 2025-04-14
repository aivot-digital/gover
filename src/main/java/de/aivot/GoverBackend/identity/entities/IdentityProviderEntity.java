package de.aivot.GoverBackend.identity.entities;

import de.aivot.GoverBackend.identity.converters.IdentityAdditionalParametersConverter;
import de.aivot.GoverBackend.identity.converters.IdentityAttributesConverter;
import de.aivot.GoverBackend.identity.converters.IdentityScopesConverter;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityAdditionalParameter;
import de.aivot.GoverBackend.identity.models.IdentityAttributeMapping;
import jakarta.persistence.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Represents an identity provider in the Gover system, used for authenticating citizens.
 * This entity contains all the necessary information to connect to an OpenID Connect (OIDC)
 * authentication system and to process the data received from the OIDC API.
 *
 * <p>Key features of this entity include:</p>
 * <ul>
 *     <li>Connection details such as endpoints (authorization, token, userinfo, etc.) and client credentials.</li>
 *     <li>Configuration for mapping attributes from the OIDC API to the Gover system.</li>
 *     <li>Default scopes and additional parameters for customizing the OIDC connection.</li>
 *     <li>Metadata and flags to manage the identity provider, such as enabling/disabling it or marking it as a test provider.</li>
 * </ul>
 *
 * <p>This entity is mapped to the database table `identity_providers` and uses JPA annotations
 * for persistence.</p>
 */
@Entity
@Table(name = "identity_providers")
public class IdentityProviderEntity {
    @Id
    @Nonnull
    @Column(length = 36, columnDefinition = "uuid")
    private String key;

    @Nonnull
    @Column(length = 64)
    private String metadataIdentifier;

    @Nonnull
    @Column(columnDefinition = "smallint")
    private IdentityProviderType type;

    @Nonnull
    @Column(length = 64)
    private String name;

    @Nonnull
    @Column(length = 255)
    private String description;

    @Nullable
    @Column(length = 36, columnDefinition = "uuid")
    private String iconAssetKey;

    @Nonnull
    @Column(length = 255)
    private String authorizationEndpoint;

    @Nonnull
    @Column(length = 255)
    private String tokenEndpoint;

    @Nullable
    @Column(length = 255)
    private String userinfoEndpoint;

    @Nullable
    @Column(length = 255)
    private String endSessionEndpoint;

    @Nonnull
    @Column(length = 128)
    private String clientId;

    @Nullable
    @Column(length = 36, columnDefinition = "uuid")
    private String clientSecretKey;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityAttributesConverter.class)
    private List<IdentityAttributeMapping> attributes;

    @Nonnull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityScopesConverter.class)
    private List<String> defaultScopes;

    @Nonnull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = IdentityAdditionalParametersConverter.class)
    private List<IdentityAdditionalParameter> additionalParams;

    @Nonnull
    private Boolean isEnabled;

    @Nonnull
    private Boolean isTestProvider;

    // Equals and HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        IdentityProviderEntity provider = (IdentityProviderEntity) object;
        return key.equals(provider.key) && metadataIdentifier.equals(provider.metadataIdentifier) && type == provider.type && name.equals(provider.name) && description.equals(provider.description) && Objects.equals(iconAssetKey, provider.iconAssetKey) && authorizationEndpoint.equals(provider.authorizationEndpoint) && tokenEndpoint.equals(provider.tokenEndpoint) && Objects.equals(userinfoEndpoint, provider.userinfoEndpoint) && Objects.equals(endSessionEndpoint, provider.endSessionEndpoint) && clientId.equals(provider.clientId) && Objects.equals(clientSecretKey, provider.clientSecretKey) && Objects.equals(attributes, provider.attributes) && defaultScopes.equals(provider.defaultScopes) && additionalParams.equals(provider.additionalParams) && isEnabled.equals(provider.isEnabled) && isTestProvider.equals(provider.isTestProvider);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + metadataIdentifier.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Objects.hashCode(iconAssetKey);
        result = 31 * result + authorizationEndpoint.hashCode();
        result = 31 * result + tokenEndpoint.hashCode();
        result = 31 * result + Objects.hashCode(userinfoEndpoint);
        result = 31 * result + Objects.hashCode(endSessionEndpoint);
        result = 31 * result + clientId.hashCode();
        result = 31 * result + Objects.hashCode(clientSecretKey);
        result = 31 * result + Objects.hashCode(attributes);
        result = 31 * result + defaultScopes.hashCode();
        result = 31 * result + additionalParams.hashCode();
        result = 31 * result + isEnabled.hashCode();
        result = 31 * result + isTestProvider.hashCode();
        return result;
    }

    // endregion

    // Getters & Setters

    @Nonnull
    public String getKey() {
        return key;
    }

    public IdentityProviderEntity setKey(@Nonnull String key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getMetadataIdentifier() {
        return metadataIdentifier;
    }

    public IdentityProviderEntity setMetadataIdentifier(@Nonnull String metadataIdentifier) {
        this.metadataIdentifier = metadataIdentifier;
        return this;
    }

    @Nonnull
    public IdentityProviderType getType() {
        return type;
    }

    public IdentityProviderEntity setType(@Nonnull IdentityProviderType type) {
        this.type = type;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public IdentityProviderEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public IdentityProviderEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nullable
    public String getIconAssetKey() {
        return iconAssetKey;
    }

    public IdentityProviderEntity setIconAssetKey(@Nullable String iconAssetKey) {
        this.iconAssetKey = iconAssetKey;
        return this;
    }

    @Nonnull
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public IdentityProviderEntity setAuthorizationEndpoint(@Nonnull String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    @Nonnull
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public IdentityProviderEntity setTokenEndpoint(@Nonnull String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    @Nullable
    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public IdentityProviderEntity setUserinfoEndpoint(@Nullable String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
        return this;
    }

    @Nullable
    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public IdentityProviderEntity setEndSessionEndpoint(@Nullable String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
        return this;
    }

    @Nonnull
    public String getClientId() {
        return clientId;
    }

    public IdentityProviderEntity setClientId(@Nonnull String clientId) {
        this.clientId = clientId;
        return this;
    }

    @Nullable
    public String getClientSecretKey() {
        return clientSecretKey;
    }

    public IdentityProviderEntity setClientSecretKey(@Nullable String clientSecretKey) {
        this.clientSecretKey = clientSecretKey;
        return this;
    }

    public List<IdentityAttributeMapping> getAttributes() {
        return attributes;
    }

    public IdentityProviderEntity setAttributes(List<IdentityAttributeMapping> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Nonnull
    public List<String> getDefaultScopes() {
        return defaultScopes;
    }

    public IdentityProviderEntity setDefaultScopes(@Nonnull List<String> defaultScopes) {
        this.defaultScopes = defaultScopes;
        return this;
    }

    @Nonnull
    public List<IdentityAdditionalParameter> getAdditionalParams() {
        return additionalParams;
    }

    public IdentityProviderEntity setAdditionalParams(@Nonnull List<IdentityAdditionalParameter> additionalParams) {
        this.additionalParams = additionalParams;
        return this;
    }

    @Nonnull
    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public IdentityProviderEntity setIsEnabled(@Nonnull Boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    @Nonnull
    public Boolean getIsTestProvider() {
        return isTestProvider;
    }

    public IdentityProviderEntity setIsTestProvider(@Nonnull Boolean testProvider) {
        isTestProvider = testProvider;
        return this;
    }

    // endregion
}
