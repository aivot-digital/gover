package de.aivot.GoverBackend.storage;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.storage.services.StorageProviderConfigurationService;
import de.aivot.GoverBackend.storage.services.StorageProviderDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("storage")
@ConditionalOnEnabledHealthIndicator("storage")
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageProviderConfigurationService storageProviderConfigurationService;

    @Autowired
    public StorageHealthIndicator(StorageProviderRepository storageProviderRepository,
                                  StorageProviderDefinitionService storageProviderDefinitionService,
                                  StorageProviderConfigurationService storageProviderConfigurationService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageProviderConfigurationService = storageProviderConfigurationService;
    }

    @Override
    public Health health() {
        var providers = storageProviderRepository
                .findAll();

        if (providers.isEmpty()) {
            return Health
                    .unknown()
                    .withDetail("hint", "Es sind keine Speicheranbieter konfiguriert.")
                    .build();
        }

        for (var provider : providers) {
            var def = storageProviderDefinitionService
                    .retrieveProviderDefinition(
                            provider.getStorageProviderDefinitionKey(),
                            provider.getStorageProviderDefinitionVersion()
                    )
                    .orElse(null);

            if (def == null) {
                return Health
                        .down()
                        .withDetail("hint", "Der Speicheranbieter '" + provider.getName() + "' referenziert eine nicht vorhandene Anbieterdefinition.")
                        .build();
            }

            try {
                testConnection(provider, def);
            } catch (Exception e) {
                return Health
                        .down()
                        .withDetail("error", "Verbindungstest zum Speicheranbieter '" + provider.getName() + "' ist fehlgeschlagen. Fehlermeldung: " + e.getMessage())
                        .build();
            }
        }

        return Health
                .up()
                .build();
    }

    private <T> void testConnection(StorageProviderEntity provider, StorageProviderDefinition<T> definition) throws StorageException {
        T config;
        try {
            config = storageProviderConfigurationService
                    .mapToConfig(provider, definition);
        } catch (ResponseException e) {
            throw new StorageException(e, "Fehler beim Konvertieren der Speicheranbieter-Konfiguration.");
        }

        // Do not test write, because this may include the writing of test files (S3) and we do not want to write unnecessary files
        definition.testConnection(config, false);
    }
}
