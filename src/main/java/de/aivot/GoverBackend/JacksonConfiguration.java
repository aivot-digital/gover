package de.aivot.GoverBackend;

import de.aivot.GoverBackend.core.jackson.FallbackZoneInstantDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class JacksonConfiguration {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer fallbackZoneInstantDeserializerCustomizer() {
        return builder -> builder.deserializerByType(Instant.class, new FallbackZoneInstantDeserializer());
    }
}
