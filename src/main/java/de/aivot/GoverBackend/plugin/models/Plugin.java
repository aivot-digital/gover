package de.aivot.GoverBackend.plugin.models;

public interface Plugin {
    String getKey();
    String getName();
    String getDescription();
    String getBuildDate();
    String getVersion();
    String getVendorName();
}

