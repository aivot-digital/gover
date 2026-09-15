package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.YearSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JsonMapperConfiguration {
    @Bean
    public JsonMapperBuilderCustomizer customJacksonSerializers() {
        // Keep this HTTP mapper aligned with ObjectMapperFactory, which is used by
        // persistence and destination-payload code outside Spring MVC.
        return builder -> builder.addModule(new SimpleModule("prosuna-temporal")
                .addDeserializer(Instant.class, new StrictInstantValueDeserializer())
                .addSerializer(Instant.class, new ApplicationTimeZoneInstantValueSerializer())
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .addSerializer(Year.class, new YearSerializer(DateTimeFormatter.ofPattern("uuuu")))
                .addSerializer(Duration.class, new DurationToMillisecondsValueSerializer()));
    }

    private static final class StrictInstantValueDeserializer extends ValueDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }

            var value = parser.getText().trim();
            if (value.isEmpty()) {
                return null;
            }

            try {
                return IsoTimestampUtils.parseIsoInstant(value);
            } catch (DateTimeParseException exception) {
                return (Instant) context.handleWeirdStringValue(
                        Instant.class,
                        value,
                        "Expected an ISO-8601 instant with Z or an explicit numeric offset"
                );
            }
        }
    }

    private static final class ApplicationTimeZoneInstantValueSerializer extends ValueSerializer<Instant> {
        @Override
        public void serialize(
                Instant value,
                JsonGenerator generator,
                SerializationContext context
        ) throws JacksonException {
            generator.writeString(IsoTimestampUtils.toOffsetString(value));
        }
    }

    private static final class DurationToMillisecondsValueSerializer extends ValueSerializer<Duration> {
        @Override
        public void serialize(
                Duration value,
                JsonGenerator generator,
                SerializationContext context
        ) throws JacksonException {
            generator.writeNumber(value.toMillis());
        }
    }
}
