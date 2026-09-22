package de.aivot.prosuna.backend.communication.converters;

import de.aivot.prosuna.backend.core.converters.JsonObjectConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import java.util.Map;

/** SQL NULL marks an empty outbox; JSON null would still match the pending-work query. */
@Converter
@Component
public class CommunicationWorkConverter extends JsonObjectConverter {
    public CommunicationWorkConverter(@Nonnull JsonMapper mapper) {
        super(mapper);
    }

    @Nullable
    @Override
    public String convertToDatabaseColumn(@Nullable Map<String, Object> work) {
        return work == null ? null : super.convertToDatabaseColumn(work);
    }

    @Nullable
    @Override
    public Map<String, Object> convertToEntityAttribute(@Nullable String work) {
        return work == null ? null : super.convertToEntityAttribute(work);
    }
}
