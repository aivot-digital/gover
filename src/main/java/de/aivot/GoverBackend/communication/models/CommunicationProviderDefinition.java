package de.aivot.GoverBackend.communication.models;

import de.aivot.GoverBackend.plugin.models.PluginComponent;

import java.util.List;

public interface CommunicationProviderDefinition<T extends CommunicationMessage> extends PluginComponent {
    void sendMessage(T message);
    List<T> receiveMessages();
}
