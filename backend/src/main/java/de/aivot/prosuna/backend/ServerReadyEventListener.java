package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.plugin.models.Plugin;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import io.sentry.Sentry;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;


@Component
public class ServerReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerReadyEventListener.class);
    private final BuildProperties buildProperties;
    private final ProsunaConfig prosunaConfig;
    private final List<Plugin> plugins;

    @Autowired
    public ServerReadyEventListener(BuildProperties buildProperties,
                                    ProsunaConfig prosunaConfig,
                                    List<Plugin> plugins) {
        this.buildProperties = buildProperties;
        this.prosunaConfig = prosunaConfig;
        this.plugins = plugins;
    }

    @Override
    public void onApplicationEvent(@Nonnull @NotNull final ApplicationReadyEvent event) {
        logBuildInfo();
        logPlugins();
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
        if (!prosunaConfig.getSentryServer().isBlank()) {
            logger
                    .atInfo()
                    .setMessage("Starting server with Sentry.")
                    .addKeyValue("sentryEnvironment", prosunaConfig.getEnvironment())
                    .addKeyValue("sentryServerDSN", prosunaConfig.getSentryServer())
                    .addKeyValue("sentryWebAppDSN", prosunaConfig.getSentryWebApp())
                    .log();

            Sentry.init(options -> {
                options.setDsn(prosunaConfig.getSentryServer());
                options.setEnvironment(prosunaConfig.getEnvironment());
                options.setTracesSampleRate(0.1);
            });
        } else {
            logger
                    .atWarn()
                    .setMessage("Starting server without Sentry.")
                    .log();
        }
    }

    private void logPlugins() {
        var message = "Loaded %d plugins: %s";
        var pluginNames = plugins.stream()
                .map(Plugin::getName)
                .toList();
        var fm = String.format(
                message,
                plugins.size(),
                String.join(", ", pluginNames)
        );

        List<String> pluginFiles = List.of();

        var pluginsDirPath = System
                .getenv("PROSUNA_PLUGINS_DIR");
        if (pluginsDirPath != null) {
            var pluginsDir =  new File(pluginsDirPath);
            var allPluginFiles = pluginsDir.listFiles();

            if  (allPluginFiles != null) {
                pluginFiles = Stream
                        .of(allPluginFiles)
                        .filter(file -> !file.isDirectory())
                        .map(File::getName)
                        .toList();
            }
        }

        logger
                .atInfo()
                .setMessage(fm)
                .addKeyValue("registeredPlugins", pluginNames)
                .addKeyValue("pluginFiles", pluginFiles)
                .log();
    }
}
