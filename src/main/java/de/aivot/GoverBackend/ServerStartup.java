package de.aivot.GoverBackend;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import io.sentry.Sentry;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class ServerStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerStartup.class);
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;
    private final SystemConfigRepository systemConfigRepository;

    @Autowired
    public ServerStartup(
            GoverConfig goverConfig,
            StorageConfig storageConfig,
            SystemConfigRepository systemConfigRepository
    ) {
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        initializeSentry();
        initializeStorages();
        initializeConfig();
    }

    private void initializeConfig() {
        goverConfig.getSystemConfig().forEach((keyStr, value) -> {
            var key = SystemConfigKey
                    .fromString(keyStr)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown system config key: " + keyStr));

            var config = systemConfigRepository
                    .findById(key.getKey())
                    .orElseGet(() -> {
                        var newConfig = new SystemConfig();
                        newConfig.setKey(key.getKey());
                        newConfig.setCreated(LocalDateTime.now());
                        return newConfig;
                    });

            config.setPublicConfig(key.isPublic());
            config.setUpdated(LocalDateTime.now());
            config.setValue(value);

            logger.info("Setting system config: {} = {}", config.getKey(), config.getValue());

            systemConfigRepository.save(config);
        });
    }

    private void initializeSentry() {
        if (!goverConfig.getSentryServer().isBlank()) {
            logger.info("Starting server with Sentry DSN for Backend: {}.", goverConfig.getSentryServer());
            logger.info("Starting server with Sentry DSN for Frontend: {}.", goverConfig.getSentryWebApp());
            logger.info("Sentry environment is: {}.", goverConfig.getEnvironment());

            Sentry.init(options -> {
                options.setDsn(goverConfig.getSentryServer());
                options.setEnvironment(goverConfig.getEnvironment());
                options.setTracesSampleRate(0.1);
            });
        } else {
            logger.warn("Starting server without Sentry.");
        }
    }

    private void initializeStorages() {
        if (storageConfig.localStorageEnabled()) {
            logger.info("Using local storage");
            logger.info("Local storage path: {}", storageConfig.getLocalStoragePath());
        } else {
            logger.info("Using remote storage");
            logger.info("Remote storage endpoint: {}", storageConfig.getRemoteEndpoint());
            logger.info("Remote storage bucket: {}", storageConfig.getRemoteBucket());
        }
    }
}
