package de.aivot.GoverBackend;

import com.beust.jcommander.Strings;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import de.aivot.GoverBackend.system.properties.CORSProperties;
import io.sentry.Sentry;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
public class ServerReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerReadyEventListener.class);
    private final BuildProperties buildProperties;
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;
    private final CORSProperties corsProperties;

    @Autowired
    public ServerReadyEventListener(
            BuildProperties buildProperties,
            GoverConfig goverConfig,
            StorageConfig storageConfig,
            CORSProperties corsProperties
    ) {
        this.buildProperties = buildProperties;
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
        this.corsProperties = corsProperties;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        logBuildInfo();
        logCorsConfiguration();

        initializeSentry();
        initializeStorages();
    }

    private void logBuildInfo() {
        var message = "Gover Version %s.%s (%s)";
        var fm = String.format(
                message,
                buildProperties.getBuildVersion(),
                buildProperties.getBuildNumber(),
                buildProperties.getBuildTimestamp()
        );

        logger
                .atInfo()
                .setMessage(fm)
                .addKeyValue("buildVersion", buildProperties.getBuildVersion())
                .addKeyValue("buildNumber", buildProperties.getBuildNumber())
                .addKeyValue("buildTime", buildProperties.getBuildTimestamp())
                .log();
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

    private void logCorsConfiguration() {
        if (corsProperties.isEnabled()) {
            var message = String.format(
                    "CORS configuration: allowed origins: %s",
                    Strings.join(", ", corsProperties.getAllowedOrigins())
            );

            logger
                    .atInfo()
                    .setMessage(message)
                    .addKeyValue("allowedOrigins", corsProperties.getAllowedOrigins())
                    .log();
        } else {
            logger
                    .atInfo()
                    .setMessage("CORS is disabled.")
                    .log();
        }
    }
}
