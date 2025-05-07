package de.aivot.GoverBackend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.servlet.HttpExchangesFilter;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {
    @Bean
    public InMemoryHttpExchangeRepository createTraceRepository() {
        var exchanges = new InMemoryHttpExchangeRepository();
        exchanges.setCapacity(100);
        return exchanges;
    }

    @Value("${spring.flyway.repairOnMigrate}")
    private Boolean repairOnMigrate;

    @Bean
    public FlywayMigrationStrategy repairFlyway() {
        return flyway -> {
            if (repairOnMigrate) {
                flyway.repair();
            }

            flyway.migrate();
        };
    }
}
