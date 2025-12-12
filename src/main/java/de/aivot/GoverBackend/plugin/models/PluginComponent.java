package de.aivot.GoverBackend.plugin.models;

public interface PluginComponent {
    String getKey();
    String getParentPluginKey();
    String getName();
    String getDescription();
}
