package de.aivot.prosuna.backend.dataObject.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.dataObject.dtos.DataObjectItemResponseDTO;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.dataObject.repositories.DataObjectItemRepository;
import de.aivot.prosuna.backend.dataObject.repositories.DataObjectSchemaRepository;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.services.InputVariableResolver;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataObjectItemServiceTest {
    private final DataObjectItemRepository items = mock(DataObjectItemRepository.class);
    private final DataObjectSchemaRepository schemas = mock(DataObjectSchemaRepository.class);
    private final AuthoredInputValueService authoredValues = new AuthoredInputValueService(JsonMapperTestUtils.createMapper());
    private final ElementDerivationService derivation = new ElementDerivationService(
            new JavascriptEngineFactoryService(List.of()), new NoCodeEvaluationService(List.of()),
            new ElementDataTransformService(), new CodeListElementOptionsService(null, null),
            authoredValues, new InputVariableResolver()
    );
    private final DataObjectItemService service = new DataObjectItemService(items, schemas, derivation, authoredValues);

    @BeforeEach
    void setUp() {
        var detail = text("detail");
        detail.setRequired(true);
        var details = new ReplicatingContainerLayoutElement();
        details.setId("details");
        details.setChildren(List.of(detail));
        var rows = new ReplicatingContainerLayoutElement();
        rows.setId("rows");
        rows.setChildren(List.of(text("name"), details));
        var group = new GroupLayoutElement();
        group.setId("root");
        group.setChildren(List.of(text("$id"), rows));
        when(schemas.findById("contacts")).thenReturn(Optional.of(new DataObjectSchemaEntity()
                .setKey("contacts").setIdGen(DataObjectItemService.ID_GEN_CUSTOM).setSchema(group)));
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateAndUpdateNestedPlainValuesAndPreserveRowIds() throws Exception {
        var created = service.create(entity(data("  Initial  ")));
        assertEquals("contact-1", created.getId());
        assertEquals("Initial", nestedDetail(created));
        assertEquals("outer", json(created).get("rows").get(0).get("id").asString());
        assertEquals("inner", json(created).get("rows").get(0).get("values").get("details").get(0).get("id").asString());

        // Re-submit the response shape, just as a client would after loading the saved item.
        var incomingData = JsonMapperTestUtils.createMapper().convertValue(DataObjectItemResponseDTO.fromEntity(created).data(), Map.class);
        var savedJson = json(created);
        service.performUpdate(new DataObjectItemEntityId("contacts", "contact-1"), entity(incomingData), created);
        assertEquals(savedJson, json(created));
        var updatedData = new LinkedHashMap<String, Object>(incomingData);
        updatedData.putAll(data("Changed"));
        var updated = service.performUpdate(new DataObjectItemEntityId("contacts", "contact-1"), entity(updatedData), created);
        assertEquals("Changed", nestedDetail(updated));
        assertEquals("contact-1", updated.getData().get("$id"));
        assertEquals("outer", json(updated).get("rows").get(0).get("id").asString());
        verify(items, times(3)).save(any());
    }

    @Test
    void shouldRejectInvalidNestedValuesBeforePersistence() {
        var error = assertThrows(ResponseException.class, () -> service.create(entity(data(""))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(items, never()).save(any());
    }

    @Test
    void shouldLeaveTheExistingDataUntouchedWhenAnUpdateFailsValidation() {
        var existing = entity(data("Original"));
        var original = json(existing);
        var error = assertThrows(ResponseException.class, () -> service.performUpdate(
                new DataObjectItemEntityId("contacts", "contact-1"), entity(data("")), existing
        ));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(original, json(existing));
        verify(items, never()).save(any());
    }

    @Test
    void shouldTreatIncomingModeObjectsAsDataRatherThanExecutableValues() {
        var error = assertThrows(ResponseException.class, () -> service.create(entity(data(
                Map.of("type", "LowCode", "code", "return 'not a literal';")
        ))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(items, never()).save(any());
    }

    @Test
    void shouldReturnBadRequestForMalformedRowStructures() {
        var malformed = Map.<String, Object>of("$id", "contact-1", "rows", List.of(Map.of("values", 42)));
        var error = assertThrows(ResponseException.class, () -> service.create(entity(malformed)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(items, never()).save(any());
    }

    private static TextInputElement text(String id) {
        var field = new TextInputElement();
        field.setId(id);
        return field;
    }

    private static Map<String, Object> data(Object detail) {
        return Map.of("$id", "contact-1", "rows", List.of(Map.of(
                "id", "outer", "values", Map.of("name", "Ada", "details", List.of(Map.of(
                        "id", "inner", "values", Map.of("detail", detail)
                )))
        )));
    }

    private static DataObjectItemEntity entity(Map<String, Object> data) {
        return new DataObjectItemEntity().setId("contact-1").setSchemaKey("contacts").setData(data)
                .setCreated(Instant.EPOCH).setUpdated(Instant.EPOCH);
    }

    private static tools.jackson.databind.JsonNode json(DataObjectItemEntity entity) {
        return JsonMapperTestUtils.createMapper().valueToTree(entity.getData());
    }

    private static String nestedDetail(DataObjectItemEntity entity) {
        return json(entity).get("rows").get(0).get("values").get("details").get(0).get("values").get("detail").asString();
    }
}
