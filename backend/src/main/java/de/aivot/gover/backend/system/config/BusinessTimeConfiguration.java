package de.aivot.gover.backend.system.config;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.models.config.GoverConfig;
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
    public BusinessTime businessTime(GoverConfig goverConfig, Clock businessClock) {
        return new BusinessTime(goverConfig.getZoneId(), businessClock);
    }
}
