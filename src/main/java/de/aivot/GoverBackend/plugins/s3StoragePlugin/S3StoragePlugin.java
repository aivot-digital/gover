package de.aivot.GoverBackend.plugins.s3StoragePlugin;

import de.aivot.GoverBackend.plugin.models.Plugin;
import org.springframework.stereotype.Component;

@Component
public class S3StoragePlugin implements Plugin {
    @Override
    public String getKey() {
        return "s3StoragePlugin";
    }

    @Override
    public String getName() {
        return "S3 Speicher Plugin";
    }

    @Override
    public String getDescription() {
        return "Dieses Plugin stellt funktionen zum Speichern von Dateien in einem S3-kompatiblen Speicher bereit.";
    }

    @Override
    public String getBuildDate() {
        return "2024-06-11";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getVendorName() {
        return "Aivot";
    }
}
