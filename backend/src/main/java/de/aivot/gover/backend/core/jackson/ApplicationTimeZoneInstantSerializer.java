package de.aivot.gover.backend.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import de.aivot.gover.backend.utils.IsoTimestampUtils;

import java.io.IOException;
import java.time.Instant;

/**
 * Serializes absolute instants with the offset that applies in Gover's application timezone.
 *
 * <p>The zone is resolved for every value instead of being captured in the constructor.
 * Jackson can instantiate this serializer before Spring has applied {@code GoverConfig},
 * and the same serializer is also used by static, non-Spring object mappers.</p>
 */
public final class ApplicationTimeZoneInstantSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(
            Instant value,
            JsonGenerator generator,
            SerializerProvider serializers
    ) throws IOException {
        generator.writeString(
                IsoTimestampUtils.toOffsetString(value, ApplicationTimeZone.getZoneId())
        );
    }
}
