package de.aivot.gover.backend.system.config;

import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class ApplicationTimeZoneConfiguration {
    private final GoverConfig goverConfig;

    public ApplicationTimeZoneConfiguration(GoverConfig goverConfig) {
        this.goverConfig = goverConfig;
    }

    @PostConstruct
    public void configureApplicationTimeZone() {
        var zoneId = goverConfig.getZoneId();

        // Keep the JVM default timezone aligned with the configured business timezone so
        // legacy code paths and third-party libraries that rely on systemDefault() stay consistent.
        ApplicationTimeZone.configure(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        System.setProperty("user.timezone", zoneId.getId());
    }
}
