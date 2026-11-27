package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataObjectItemResponseDTOTest {
    @Test
    void shouldTransportPlainValuesInBothDirectionsIncludingNestedRows() {
        var mapper = JsonMapper.builder().build();
        var data = Map.<String, Object>of(
                "name", "Ada",
                "rows", List.of(Map.of(
                        "id", "row-1",
                        "values", Map.of("details", List.of(Map.of(
                                "id", "row-2", "values", Map.of("name", "Nested")
                        )))
                )),
                "object", Map.of("type", "Literal", "value", "Business data")
        );
        var entity = new DataObjectItemEntity()
                .setSchemaKey("contacts")
                .setId("1")
                .setData(data)
                .setCreated(Instant.EPOCH)
                .setUpdated(Instant.EPOCH);

        var response = DataObjectItemResponseDTO.fromEntity(entity);
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertEquals(mapper.valueToTree(data), json.get("data"));

        var request = mapper.readValue(mapper.writeValueAsString(Map.of("id", "1", "data", data)), DataObjectItemRequestDTO.class);
        var incoming = request.toEntity(new DataObjectSchemaEntity().setKey("contacts"));
        assertEquals(data, incoming.getData());
        assertEquals("contacts", incoming.getSchemaKey());
    }
}
