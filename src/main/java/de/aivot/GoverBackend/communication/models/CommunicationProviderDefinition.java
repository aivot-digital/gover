package de.aivot.GoverBackend.communication.models;

import de.aivot.GoverBackend.plugin.enums.PluginComponentType;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
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
