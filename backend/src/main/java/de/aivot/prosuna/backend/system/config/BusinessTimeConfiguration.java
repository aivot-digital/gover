package de.aivot.prosuna.backend.system.config;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BusinessTimeConfiguration {
    @Bean
    public Clock businessClock() {
        // A Clock provides absolute instants; the business timezone is applied separately
        // by BusinessTime. Exposing it as a bean also makes "now" deterministic in tests.
        return Clock.systemUTC();
    }

    @Bean
    public BusinessTime businessTime(ProsunaConfig prosunaConfig, Clock businessClock) {
        return new BusinessTime(prosunaConfig.getZoneId(), businessClock);
    }
}
