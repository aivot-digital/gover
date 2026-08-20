package de.aivot.prosuna.backend;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import de.aivot.prosuna.backend.core.jackson.ApplicationTimeZoneInstantSerializer;
import de.aivot.prosuna.backend.core.jackson.DurationToMillisecondsSerializer;
import de.aivot.prosuna.backend.core.jackson.StrictInstantDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfiguration {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJacksonSerializers() {
        // Keep this HTTP mapper aligned with ObjectMapperFactory, which is used by
        // persistence and destination-payload code outside Spring MVC.
        return builder -> builder
                .deserializerByType(Instant.class, new StrictInstantDeserializer())
                .serializerByType(Instant.class, new ApplicationTimeZoneInstantSerializer())
                .serializerByType(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .serializerByType(Duration.class, new DurationToMillisecondsSerializer());
    }
}
