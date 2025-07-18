package de.aivot.GoverBackend;

import de.aivot.GoverBackend.system.properties.CORSProperties;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ServerConfiguration implements WebMvcConfigurer {
    private final CORSProperties corsProperties;

    @Autowired
    public ServerConfiguration(CORSProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

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

    @Override
    public void addCorsMappings(@Nonnull CorsRegistry registry) {
        if (corsProperties.isEnabled()) {
            registry
                    .addMapping("/**")
                    .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                    .allowedMethods(CORSProperties.DEFAULT_ALLOWED_METHODS)
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    }
}
