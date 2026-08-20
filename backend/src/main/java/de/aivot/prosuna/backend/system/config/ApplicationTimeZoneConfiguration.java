package de.aivot.prosuna.backend.system.config;

import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class ApplicationTimeZoneConfiguration {
    private final ProsunaConfig prosunaConfig;

    public ApplicationTimeZoneConfiguration(ProsunaConfig prosunaConfig) {
        this.prosunaConfig = prosunaConfig;
    }

    @PostConstruct
    public void configureApplicationTimeZone() {
        var zoneId = prosunaConfig.getZoneId();

        // Keep the JVM default timezone aligned with the configured business timezone so
        // legacy code paths and third-party libraries that rely on systemDefault() stay consistent.
        ApplicationTimeZone.configure(zoneId);
        // Setting user.timezone alone does not reliably invalidate the JDK's cached default.
        // TimeZone.setDefault is therefore the effective runtime update.
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        System.setProperty("user.timezone", zoneId.getId());
    }
}
