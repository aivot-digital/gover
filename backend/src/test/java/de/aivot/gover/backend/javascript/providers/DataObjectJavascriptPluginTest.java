package de.aivot.gover.backend.javascript.providers;

import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntity;
import de.aivot.gover.backend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.gover.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.gover.backend.dataObject.filters.DataObjectItemFilter;
import de.aivot.gover.backend.dataObject.services.DataObjectItemService;
import de.aivot.gover.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.plugins.core.v1.javascript.DataObjectJavascriptV1;
import de.aivot.gover.backend.utils.IsoTimestampUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataObjectJavascriptPluginTest {
    private static final Instant CREATED = Instant.parse("2024-05-01T10:15:30Z");
    private static final Instant UPDATED = Instant.parse("2024-05-02T11:16:31Z");

    private DataObjectSchemaService dataObjectSchemaService;
    private DataObjectItemService dataObjectItemService;

    @BeforeEach
    void setUp() {
        dataObjectSchemaService = mock(DataObjectSchemaService.class);
        dataObjectItemService = mock(DataObjectItemService.class);
    }

    @Test
    void getSchema() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            when(dataObjectSchemaService.retrieve("contacts"))
                    .thenReturn(Optional.of(schemaEntity()));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.getSchema('contacts').id;"));

            assertEquals("contact-schema", result.asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void list() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            when(dataObjectItemService.list(any(DataObjectItemFilter.class)))
                    .thenReturn(new PageImpl<>(List.of(item("contacts", "item-1", mapOf("name", "Ada", "status", "active")))));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    _data_objects_v1.list('contacts', [
                        {path: 'status', operator: 'eq', value: 'active'}
                    ]);
                    """));
            var values = assertInstanceOf(List.class, result.asObject());
            var item = assertInstanceOf(Map.class, values.getFirst());
            var filterCaptor = ArgumentCaptor.forClass(DataObjectItemFilter.class);

            assertEquals("Ada", item.get("name"));
            assertEquals("active", item.get("status"));
            assertEquals("item-1", item.get("$id"));
            assertEquals(instantWithApplicationOffset(CREATED), item.get("$created"));
            assertEquals(instantWithApplicationOffset(UPDATED), item.get("$updated"));

            verify(dataObjectItemService).list(filterCaptor.capture());
            assertEquals("contacts", filterCaptor.getValue().getSchemaKey());
            assertEquals(1, filterCaptor.getValue().getDataFields().size());
            assertEquals("status", filterCaptor.getValue().getDataFields().getFirst().getPath());
            assertEquals("eq", filterCaptor.getValue().getDataFields().getFirst().getOperator());
            assertEquals("active", filterCaptor.getValue().getDataFields().getFirst().getValue());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void create() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            when(dataObjectSchemaService.retrieve("contacts"))
                    .thenReturn(Optional.of(schemaEntity()));
            when(dataObjectItemService.create(any(DataObjectItemEntity.class)))
                    .thenAnswer(invocation -> {
                        var entity = invocation.getArgument(0, DataObjectItemEntity.class);
                        return item(entity.getSchemaKey(), "created-item", entity.getData());
                    });

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.create('contacts', {name: 'Ada', status: 'active'});"));
            var created = assertInstanceOf(Map.class, result.asObject());
            var entityCaptor = ArgumentCaptor.forClass(DataObjectItemEntity.class);

            assertEquals("Ada", created.get("name"));
            assertEquals("active", created.get("status"));
            assertEquals("created-item", created.get("$id"));

            verify(dataObjectItemService).create(entityCaptor.capture());
            assertEquals("contacts", entityCaptor.getValue().getSchemaKey());
            assertEquals("Ada", entityCaptor.getValue().getData().get("name"));
            assertEquals("active", entityCaptor.getValue().getData().get("status"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void retrieve() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            when(dataObjectItemService.retrieve(new DataObjectItemEntityId("contacts", "item-1")))
                    .thenReturn(Optional.of(item("contacts", "item-1", mapOf("name", "Ada"))));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.retrieve('contacts', 'item-1');"));
            var item = assertInstanceOf(Map.class, result.asObject());

            assertEquals("Ada", item.get("name"));
            assertEquals("item-1", item.get("$id"));
            assertEquals(instantWithApplicationOffset(CREATED), item.get("$created"));
            assertEquals(instantWithApplicationOffset(UPDATED), item.get("$updated"));
        } catch (Exception e) {
            fail(e);
        }
    }

    private static String instantWithApplicationOffset(Instant value) {
        return IsoTimestampUtils.toOffsetString(value);
    }

    @Test
    void update() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            var id = new DataObjectItemEntityId("contacts", "item-1");

            when(dataObjectSchemaService.retrieve("contacts"))
                    .thenReturn(Optional.of(schemaEntity()));
            when(dataObjectItemService.update(eq(id), any(DataObjectItemEntity.class)))
                    .thenReturn(item("contacts", "item-1", mapOf("name", "Grace")));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.update('contacts', 'item-1', {name: 'Grace'});"));
            var updated = assertInstanceOf(Map.class, result.asObject());
            var entityCaptor = ArgumentCaptor.forClass(DataObjectItemEntity.class);

            assertEquals("Grace", updated.get("name"));
            assertEquals("item-1", updated.get("$id"));

            verify(dataObjectItemService).update(eq(id), entityCaptor.capture());
            assertEquals("contacts", entityCaptor.getValue().getSchemaKey());
            assertEquals("Grace", entityCaptor.getValue().getData().get("name"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsNullOrEmptyForMissingInput() {
        try (var jsService = new JavascriptEngine(new DataObjectJavascriptV1(dataObjectSchemaService, dataObjectItemService))) {
            var nullResult = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.retrieve(null, 'item-1');"));
            var listResult = jsService.evaluateCode(new JavascriptCode().setCode("_data_objects_v1.list(null, null);"));

            assertTrue(nullResult.isNull());
            assertEquals(List.of(), listResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }

    private static DataObjectSchemaEntity schemaEntity() {
        return new DataObjectSchemaEntity()
                .setKey("contacts")
                .setName("Contacts")
                .setDescription("Contact data")
                .setIdGen(DataObjectItemService.ID_GEN_UUID)
                .setSchema(schemaElement())
                .setCreated(CREATED)
                .setUpdated(UPDATED)
                .setDisplayFields(List.of("name"));
    }

    private static GroupLayoutElement schemaElement() {
        var schema = new GroupLayoutElement();
        schema.setId("contact-schema");
        return schema;
    }

    private static DataObjectItemEntity item(String schemaKey, String id, Map<String, Object> data) {
        return new DataObjectItemEntity()
                .setSchemaKey(schemaKey)
                .setId(id)
                .setData(data)
                .setCreated(CREATED)
                .setUpdated(UPDATED);
    }

    private static Map<String, Object> mapOf(String keyA, Object valueA) {
        var map = new LinkedHashMap<String, Object>();
        map.put(keyA, valueA);
        return map;
    }

    private static Map<String, Object> mapOf(String keyA, Object valueA, String keyB, Object valueB) {
        var map = mapOf(keyA, valueA);
        map.put(keyB, valueB);
        return map;
    }
}
