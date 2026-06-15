package de.aivot.GoverBackend.core.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import de.aivot.GoverBackend.utils.IsoTimestampUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class FallbackZoneInstantDeserializer extends JsonDeserializer<Instant> {
    private static final JsonDeserializer<Instant> DEFAULT_DESERIALIZER = InstantDeserializer.INSTANT;

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            var value = parser.getText().trim();

            if (!value.isEmpty()) {
                try {
                    return IsoTimestampUtils.parseIsoTimestamp(value, ApplicationTimeZone.getZoneId());
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        return DEFAULT_DESERIALIZER.deserialize(parser, context);
    }
}
