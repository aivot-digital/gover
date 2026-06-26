package de.aivot.GoverBackend.plugins.core.v1.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "storage.local.v1")
public class LocalDiskStorageProviderDefinitionPropertiesV1 {
    private List<String> allowedLocalRoots;

    public List<String> getAllowedLocalRoots() {
        return allowedLocalRoots;
    }

    public LocalDiskStorageProviderDefinitionPropertiesV1 setAllowedLocalRoots(List<String> allowedLocalRoots) {
        this.allowedLocalRoots = allowedLocalRoots;
        return this;
    }
}
