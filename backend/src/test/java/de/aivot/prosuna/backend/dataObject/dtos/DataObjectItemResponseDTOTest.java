package de.aivot.prosuna.backend.dataObject.dtos;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataObjectItemResponseDTOTest {
    @Test
    void shouldWrapEffectiveReplicatingRowsForAuthoring() {
        var name = new TextInputElement();
        name.setId("name");
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(name));
        var schemaElement = new GroupLayoutElement();
        schemaElement.setId("root");
        schemaElement.setChildren(List.of(rows));

        var schema = new DataObjectSchemaEntity()
                .setKey("contacts")
                .setSchema(schemaElement);
        var entity = new DataObjectItemEntity()
                .setSchemaKey("contacts")
                .setId("1")
                .setData(Map.of(
                        "rows", List.of(Map.of(
                                "id", "row-1",
                                "values", Map.of("name", "Ada")
                        ))
                ))
                .setCreated(Instant.EPOCH)
                .setUpdated(Instant.EPOCH);

        var response = DataObjectItemResponseDTO.fromEntity(
                entity,
                schema,
                new AuthoredInputValueService(JsonMapper.builder().build())
        );

        var authoredRows = assertInstanceOf(List.class, response.data().getLiteral("rows"));
        var authoredRow = assertInstanceOf(ReplicatingContainerLayoutElementValue.class, authoredRows.getFirst());
        assertEquals("Ada", authoredRow.getValues().getLiteral("name"));
    }
}
