package de.aivot.gover.backend.storage;

import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.exceptions.StorageException;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.services.StorageProviderConfigurationService;
import de.aivot.gover.backend.storage.services.StorageProviderDefinitionService;
import de.aivot.gover.backend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("storage")
@ConditionalOnEnabledHealthIndicator("storage")
public class StorageHealthIndicator implements HealthIndicator {
    private static final String PROVIDERS_DETAILS_KEY = "providers";

    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageProviderConfigurationService storageProviderConfigurationService;
    private final SystemConfigService systemConfigService;

    @Autowired
    public StorageHealthIndicator(StorageProviderRepository storageProviderRepository,
                                  StorageProviderDefinitionService storageProviderDefinitionService,
                                  StorageProviderConfigurationService storageProviderConfigurationService,
                                  SystemConfigService systemConfigService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageProviderConfigurationService = storageProviderConfigurationService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public Health health() {
        // Fetch the default attachment storage id to check if the default attachment storage is down, which would be critical.
        String defaultAttachmentStorage;
        try {
            var val = systemConfigService
                    .getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY);

            if (val != null) {
                defaultAttachmentStorage = val.toString();
            } else {
                defaultAttachmentStorage = null;
            }
        } catch (ResponseException e) {
            defaultAttachmentStorage = null;
        }

        var providers = storageProviderRepository
                .findAll();

        // If no storage providers are configured they are per definition up.
        if (providers.isEmpty()) {
            return Health
                    .up()
                    .withDetail(PROVIDERS_DETAILS_KEY, new ArrayList<>())
                    .build();
        }

        boolean hasErrors = defaultAttachmentStorage == null;
        boolean hasHints = false;

        List<Map<String, Object>> providerDetails = new ArrayList<>();

        // Check for all storage providers if they are reachable.
        for (var provider : providers) {
            var providerDetail = new HashMap<String, Object>();
            providerDetail.put("name", provider.getName());
            providerDetail.put("definitionKey", provider.getStorageProviderDefinitionKey());
            providerDetail.put("definitionVersion", String.valueOf(provider.getStorageProviderDefinitionVersion()));
            providerDetail.put("isDefaultAttachmentStorage", defaultAttachmentStorage != null && defaultAttachmentStorage.equals(provider.getId().toString()));

            var def = storageProviderDefinitionService
                    .retrieveProviderDefinition(
                            provider.getStorageProviderDefinitionKey(),
                            provider.getStorageProviderDefinitionVersion()
                    )
                    .orElse(null);

            // If a storage provider definition is missing, the health check should be down, because this is a misconfiguration and the sysadmin needs to add the corresponding plugin again.
            if (def == null) {
                var msg = String.format(
                        "Der Speicheranbieter %s referenziert eine nicht vorhandene Anbieterdefinition %s Version %d. Bitte stellen Sie sicher, dass das entsprechende Plugin wieder installiert wird.",
                        StringUtils.quote(provider.getName()),
                        StringUtils.quote(provider.getStorageProviderDefinitionKey()),
                        provider.getStorageProviderDefinitionVersion()
                );


                providerDetail.put("error", msg);
                hasErrors = true;
                providerDetails.add(providerDetail);
                continue;
            }

            providerDetail.put("definitionName", def.getName());

            // Check the connection to each storage provider.
            try {
                testConnection(provider, def);
            } catch (Exception e) {
                var msg = String.format(
                        "Verbindungstest zum Speicheranbieter %s ist fehlgeschlagen. Fehlermeldung: %s",
                        StringUtils.quote(provider.getName()),
                        e.getMessage()
                );

                // Check if the currently checked storage provider is the default attachment storage.
                // If so, the health check should fail because this is a critical component and the system cannot function properly if the process attachments cannot be stored.
                // For all non default attachment storages just drop a hint
                if (defaultAttachmentStorage != null && defaultAttachmentStorage.equals(provider.getId().toString())) {
                    msg += " Dieser Speicheranbieter ist als Standard-Speicheranbieter für Anhänge von Vorgängen konfiguriert. Bitte stellen Sie sicher, dass der Speicheranbieter erreichbar ist.";

                    providerDetail.put("error", msg);
                    hasErrors = true;
                } else {
                    providerDetail.put("hint", msg);
                    hasHints = true;
                }
            }

            providerDetails.add(providerDetail);
        }

        // Create the builder for the health check result based on the errors and hints collected during the checks.
        // If errors exist, the component is down. If hints exist the component is afflicted. If no errors and no hints exist, the component is simply up.
        Health.Builder builder;
        if (hasErrors) {
            builder = Health.down();
        } else if (hasHints) {
            builder = Health.unknown();
        } else {
            builder = Health.up();
        }

        if (defaultAttachmentStorage == null) {
            builder.withDetail("error", "Der Standard-Speicheranbieter für Anhänge von Vorgängen ist nicht konfiguriert. Bitte konfigurieren Sie einen Speicheranbieter als Standard-Speicheranbieter für Anhänge von Vorgängen.");
        }

        return builder
                .withDetail(PROVIDERS_DETAILS_KEY, providerDetails)
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
