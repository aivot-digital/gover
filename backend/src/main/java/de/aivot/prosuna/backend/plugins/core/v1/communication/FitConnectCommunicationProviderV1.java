package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reserved v1 definition. It intentionally supports no identity-provider type so it cannot be
 * enabled in a binding before transport and addressing have been implemented.
 */
@Component
public class FitConnectCommunicationProviderV1 implements CommunicationProviderDefinition<FitConnectCommunicationProviderV1.Config, FitConnectCommunicationProviderV1.IdentityBinding> {
    @Nonnull
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigLayout() {
        var layout = new ConfigLayoutElement();
        layout.setId("fit-connect-provider-config");
        layout.setChildren(List.of());
        return layout;
    }

    @Nonnull
    @Override
    public List<IdentityProviderType> getSupportedIdentityProviderTypes() {
        return List.of();
    }

    @Nonnull
    @Override
    public Class<IdentityBinding> getIdentityProviderBindingConfigClass() {
        return IdentityBinding.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getIdentityProviderBindingConfigLayout(@Nonnull IdentityProviderEntity identityProviderEntity) {
        var layout = new ConfigLayoutElement();
        layout.setId("fit-connect-identity-provider-binding-config");
        layout.setChildren(List.of());
        return layout;
    }

    @Override
    public void sendMessage(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
                            @Nonnull IdentityData identity,
                            @Nonnull CommunicationMessage message) throws CommunicationException {
        throw new CommunicationException("FIT-Connect-Kommunikation ist in Version 1 noch nicht verfügbar.");
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "fit_connect_communication_provider";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "FIT-Connect-Kommunikation";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Vorbereitete, noch nicht aktivierbare Integration für FIT-Connect.";
    }

    @LayoutElementPOJOBinding(id = "fit-connect-provider-config", type = ElementType.ConfigLayout)
    public static class Config {
    }

    @LayoutElementPOJOBinding(id = "fit-connect-identity-provider-binding-config", type = ElementType.ConfigLayout)
    public static class IdentityBinding {
    }
}
