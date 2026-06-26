package de.aivot.gover.backend.communication.models;

import de.aivot.gover.backend.plugin.enums.PluginComponentType;
import de.aivot.gover.backend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface CommunicationProviderDefinition<T extends CommunicationMessage> extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.CommunicationProviderDefinition;
    }

    void sendMessage(T message);
    List<T> receiveMessages();
}
