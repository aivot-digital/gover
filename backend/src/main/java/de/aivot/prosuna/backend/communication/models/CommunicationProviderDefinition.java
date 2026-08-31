package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface CommunicationProviderDefinition<C, I> extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.CommunicationProviderDefinition;
    }

    @Nonnull
    Class<C> getConfigClass();

    @Nonnull
    ConfigLayoutElement getConfigLayout() throws ResponseException;

    @Nonnull
    List<IdentityProviderType> getSupportedIdentityProviderTypes();

    default boolean supportsIdentityProvider(@Nonnull IdentityProviderEntity identityProviderEntity) {
        return getSupportedIdentityProviderTypes().contains(identityProviderEntity.getType());
    }

    @Nonnull
    Class<I> getIdentityProviderBindingConfigClass();

    @Nonnull
    ConfigLayoutElement getIdentityProviderBindingConfigLayout(@Nonnull IdentityProviderEntity identityProviderEntity) throws ResponseException;

    @Nullable
    default GroupLayoutElement getTestingLayout() throws ResponseException {
        return null;
    }

    @Nullable
    default GroupLayoutElement getCustomerLayout(@Nonnull CommunicationProviderContext<C, I> context,
                                                 @Nonnull IdentityData identityData) throws ResponseException {
        return null;
    }

    void sendMessage(@Nonnull CommunicationProviderContext<C, I> context,
                     @Nonnull IdentityData identity,
                     @Nonnull CommunicationMessage message) throws CommunicationException;
}
