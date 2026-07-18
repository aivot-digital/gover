package de.aivot.gover.backend.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Duration;

public class DurationToMillisecondsSerializer extends JsonSerializer<Duration> {
    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        long milliseconds = value.toMillis();
        gen.writeNumber(milliseconds);
    }
}
