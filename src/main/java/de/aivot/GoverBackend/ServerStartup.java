package de.aivot.GoverBackend;

import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import io.sentry.Sentry;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
public class ServerStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerStartup.class);
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;

    @Autowired
    public ServerStartup(
            GoverConfig goverConfig,
            StorageConfig storageConfig
    ) {
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        initializeSentry();
        initializeStorages();
    }

    private void initializeSentry() {
        if (!goverConfig.getSentryServer().isBlank()) {
            logger
                    .atInfo()
                    .setMessage("Starting server with Sentry.")
                    .addKeyValue("sentryEnvironment", goverConfig.getEnvironment())
                    .addKeyValue("sentryServerDSN", goverConfig.getSentryServer())
                    .addKeyValue("sentryWebAppDSN", goverConfig.getSentryWebApp())
                    .log();

            Sentry.init(options -> {
                options.setDsn(goverConfig.getSentryServer());
                options.setEnvironment(goverConfig.getEnvironment());
                options.setTracesSampleRate(0.1);
            });
        } else {
            logger
                    .atWarn()
                    .setMessage("Starting server without Sentry.")
                    .log();
        }
    }

    private void initializeStorages() {
        if (storageConfig.localStorageEnabled()) {
            logger
                    .atInfo()
                    .setMessage("Using local storage.")
                    .addKeyValue("localStoragePath", storageConfig.getLocalStoragePath())
                    .log();
        } else {
            logger
                    .atInfo()
                    .setMessage("Using remote storage.")
                    .addKeyValue("remoteEndpoint", storageConfig.getRemoteEndpoint())
                    .addKeyValue("remoteBucket", storageConfig.getRemoteBucket())
                    .log();
        }
    }
}
