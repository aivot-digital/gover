package de.aivot.prosuna.backend.core.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Accepts absolute ISO-8601 timestamps. Both {@code Z} and numeric offsets identify an
 * instant; offset-free local date-times are rejected instead of being interpreted in an
 * implicit JVM or application timezone.
 * <p>
 * Empty strings are coerced to {@code null} for compatibility with existing form payloads.
 * Required values must therefore be enforced through DTO validation.
 */
public final class StrictInstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
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
