package de.aivot.GoverBackend;

import com.beust.jcommander.Strings;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import de.aivot.GoverBackend.system.properties.CORSProperties;
import io.sentry.Sentry;
import jakarta.annotation.Nonnull;
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
    private final CORSProperties corsProperties;

    @Autowired
    public ServerReadyEventListener(
            BuildProperties buildProperties,
            GoverConfig goverConfig,
            CORSProperties corsProperties
    ) {
        this.buildProperties = buildProperties;
        this.goverConfig = goverConfig;
        this.corsProperties = corsProperties;
    }

    @Override
    public void onApplicationEvent(@Nonnull @NotNull final ApplicationReadyEvent event) {
        logBuildInfo();
        logCorsConfiguration();

        initializeSentry();
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
