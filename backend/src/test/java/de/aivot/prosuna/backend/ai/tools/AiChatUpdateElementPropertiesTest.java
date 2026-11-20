package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatUpdateElementPropertiesTest {
    private static final String SUCCESS = "Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.";
    private static final String TREE = """
            {"id":"root","type":3,"name":"Root","extension":{"keep":[true,1,null]},"children":[
              {"id":"group","type":3,"children":[{"id":"field","type":15,"name":"Field"}]},
              {"id":"sibling","type":15,"name":"Sibling"}
            ]}
            """;

    private final JsonMapper jsonMapper = JsonMapperTestUtils.createMapper();
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(jsonMapper, repository);
    private final Map<String, AiUiElementChatSessionCacheEntity> stored = new HashMap<>();
    private final String cacheId = UUID.randomUUID().toString();
    private final ToolContext context = context(cacheId);

    @BeforeEach
    void setUpRepository() {
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.save(any(AiUiElementChatSessionCacheEntity.class))).thenAnswer(invocation -> {
            AiUiElementChatSessionCacheEntity entity = invocation.getArgument(0);
            stored.put(entity.getId(), entity);
            return entity;
        });
        stored.put(cacheId, new AiUiElementChatSessionCacheEntity()
                .setId(cacheId)
                .setTargetRootType(ElementType.GroupLayout)
                .setCurrentElementJson(TREE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/children/0", "/children/0/children/0"})
    void shouldSaveUpdatedTreeWithoutMutatingLoadedEntity(String path) {
        var original = stored.get(cacheId);
        var before = jsonMapper.readTree(original.getCurrentElementJson());

        assertEquals(SUCCESS, updateProperty(path, "name", "Changed", context));

        var saved = stored.get(cacheId);
        assertNotSame(original, saved);
        assertEquals(before, jsonMapper.readTree(original.getCurrentElementJson()));
        assertEquals(cacheId, saved.getId());
        assertEquals(original.getTargetRootType(), saved.getTargetRootType());
        var expected = before.deepCopy();
        ((tools.jackson.databind.node.ObjectNode) expected.at(path)).put("name", "Changed");
        assertEquals(expected, state());
        verify(repository).findById(cacheId);
        verify(repository).save(saved);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldUpdateMultiplePropertiesWithOneSave() {
        var properties = new HashMap<String, Object>();
        properties.put("name", "Changed");
        properties.put("custom", Map.of("enabled", true));
        properties.put("nullable", null);

        assertEquals(SUCCESS, tools.updatePropertiesOfElement("/children/0", properties, context));

        var element = state().at("/children/0");
        assertEquals("Changed", element.path("name").asString());
        assertEquals(jsonMapper.valueToTree(Map.of("enabled", true)), element.path("custom"));
        assertTrue(element.has("nullable"));
        assertTrue(element.path("nullable").isNull());
        verify(repository).save(any());
    }

    @Test
    void shouldExposeSequentialUpdatesToOtherContextsAndKeepCachesIsolated() {
        var secondId = UUID.randomUUID().toString();
        stored.put(secondId, new AiUiElementChatSessionCacheEntity().setId(secondId)
                .setCurrentElementJson(TREE));
        var secondBefore = jsonMapper.readTree(stored.get(secondId).getCurrentElementJson());

        assertEquals(SUCCESS, updateProperty("", "name", "Changed root", context));
        assertEquals(SUCCESS, updateProperty("/children/0/children/0", "name", "Changed field", context(cacheId)));

        var result = state();
        assertEquals("Changed root", result.path("name").asString());
        assertEquals("Changed field", result.at("/children/0/children/0/name").asString());
        assertEquals(secondBefore, jsonMapper.readTree(stored.get(secondId).getCurrentElementJson()));
        verify(repository, times(2)).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "true", "42", "1.5", "\"text\"", "[1,\"two\",null]", "{\"enabled\":true,\"items\":[1,null]}"})
    void shouldCreateLiteralPropertiesAndPreserveJsonValueTypes(String json) {
        var expectedValue = jsonMapper.readTree(json);
        var value = jsonMapper.readValue(json, Object.class);

        assertEquals(SUCCESS, updateProperty("", "custom.property/path", value, context));

        assertEquals(expectedValue, state().get("custom.property/path"));
        assertEquals(jsonMapper.readTree(TREE).get("extension"), state().get("extension"));
        assertFalse(state().has("custom"));
    }

    @Test
    void shouldSetNullRatherThanRemoveProperty() {
        assertEquals(SUCCESS, updateProperty("", "name", null, context));

        assertTrue(state().has("name"));
        assertTrue(state().get("name").isNull());
    }

    @Test
    void shouldReplaceChildrenAndExposeThemToReadTools() {
        var children = List.of(Map.of("type", 15, "id", "replacement", "name", "New field"));

        assertEquals(SUCCESS, updateProperty("", "children", children, context));

        assertEquals(jsonMapper.valueToTree(children), state().path("children"));
        assertEquals("replacement", jsonMapper.readTree(tools.getElementByPath("/children/0", context)).path("id").asString());
        assertTrue(tools.getElementsTree(context)
                .contains("Element-ID=\"replacement\", Typ-Schlüssel=15, Pfad=\"/children/0\""));
    }

    @Test
    void shouldAllowValidRootTypeChangeRegardlessOfTargetRootType() {
        stored.get(cacheId).setCurrentElementJson(jsonMapper.writeValueAsString(Map.of("id", "root", "type", ElementType.Text.getKey())));

        assertEquals(SUCCESS, tools.updatePropertiesOfElement("", Map.of(
                "id", "replacement",
                "type", ElementType.Number.getKey(),
                "name", "Changed"
        ), context));

        assertEquals("replacement", state().path("id").asString());
        assertEquals(ElementType.Number.getKey().intValue(), state().path("type").asInt());
        assertEquals("Changed", state().path("name").asString());
        assertEquals(ElementType.GroupLayout, stored.get(cacheId).getTargetRootType());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "children/0", "/", "/children", "/children/-1", "/children/01", "/children/0/name",
            "/children/100", "/children/2147483648", "/children/99999999999999999999999", "/children/1/children/0"})
    void shouldRejectInvalidOrMissingPaths(@Nullable String path) {
        assertRejected(path, "name", "Changed");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n"})
    void shouldRejectBlankPropertyNames(@Nullable String property) {
        assertRejected("", property, "Changed");
    }

    @Test
    void shouldRejectMissingOrEmptyProperties() {
        assertRejected("", (Map<String, Object>) null);
        assertRejected("", Map.of());
    }

    @Test
    void shouldRejectInvalidTypesPropertiesAndChildren() {
        assertRejected("/children/0", "type", -1);
        assertRejected("/children/0", "weight", Map.of("invalid", true));
        assertRejected("", "children", List.of(Map.of("type", 0)));
        assertRejected("", "children", Arrays.asList((Object) null));
        assertRejected("", "children", List.of(Map.of("id", "missing-type")));
    }

    @Test
    void shouldRejectTheEntireBatchWhenOnePropertyIsInvalid() {
        assertRejected("/children/0", Map.of("name", "Changed", "weight", Map.of("invalid", true)));
    }

    @Test
    void shouldValidateTheEntireTreeIncludingUnchangedSiblings() {
        stored.get(cacheId).setCurrentElementJson(jsonMapper.writeValueAsString(Map.of("id", "root", "type", 3,
                "children", List.of(Map.of("id", "invalid", "type", -1)))));

        assertRejected("", "name", "Changed");
    }

    @Test
    void shouldRejectMissingTree() {
        stored.get(cacheId).setCurrentElementJson(null);

        assertRejected("", "name", "Changed");
        assertNull(stored.get(cacheId).getCurrentElementJson());
    }

    @Test
    void shouldKeepCacheAndModeExceptions() {
        assertThrows(IllegalStateException.class, () -> updateProperty("", "name", "Changed", context(null)));
        verifyNoInteractions(repository);
        stored.clear();

        assertThrows(IllegalStateException.class, () -> updateProperty("", "name", "Changed", context));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "42", "", "{} {}"})
    void shouldRejectInvalidCachedJsonWithoutChangingIt(String json) {
        var original = stored.get(cacheId).setCurrentElementJson(json);

        assertEquals("Die Aktualisierung ist fehlgeschlagen: Der zwischengespeicherte Formularentwurf ist kein gültiges JSON-Objekt.",
                updateProperty("", "name", "Changed", context));
        assertEquals("Das Formularelement konnte nicht abgerufen werden: Der zwischengespeicherte Formularentwurf ist kein gültiges JSON-Objekt.",
                tools.getElementByPath("", context));
        assertEquals("Die Formularstruktur konnte nicht abgerufen werden: Der zwischengespeicherte Formularentwurf ist kein gültiges JSON-Objekt.",
                tools.getElementsTree(context));

        assertSame(original, stored.get(cacheId));
        assertEquals(json, original.getCurrentElementJson());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPropagateStorageFailureWithoutMutatingLoadedEntity() {
        var original = stored.get(cacheId);
        var before = state();
        var failure = new DataAccessResourceFailureException("Storage unavailable");
        when(repository.save(any())).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> updateProperty("", "name", "Changed", context)));

        assertSame(original, stored.get(cacheId));
        assertEquals(before, state());
    }

    @Test
    void shouldUpdateThroughSpringAiCallback() {
        var callbacks = ToolCallbacks.from(tools);
        var callback = Arrays.stream(callbacks)
                .filter(candidate -> candidate.getToolDefinition().name().equals("aktualisiere-element-eigenschaften"))
                .findFirst().orElseThrow();
        var schema = jsonMapper.readTree(callback.getToolDefinition().inputSchema());
        assertFalse(schema.path("properties").has("toolContext"));
        assertTrue(schema.path("properties").has("path"));
        assertEquals("object", schema.at("/properties/properties/type").asString());
        assertTrue(Arrays.stream(callbacks)
                .noneMatch(candidate -> candidate.getToolDefinition().name().equals("aktualisiere-element-eigenschaft")));

        callback.call("{\"path\":\"\",\"properties\":{\"name\":\"Changed\",\"custom\":[1,null]}}", context);

        assertEquals("Changed", state().path("name").asString());
        assertEquals(jsonMapper.readTree("[1,null]"), state().path("custom"));
        verify(repository).save(any());
    }

    private void assertRejected(@Nullable String path, @Nullable String property, @Nullable Object value) {
        var properties = new HashMap<String, Object>();
        properties.put(property, value);
        assertRejected(path, properties);
    }

    private void assertRejected(@Nullable String path, @Nullable Map<String, Object> properties) {
        var original = stored.get(cacheId);
        var before = state();

        assertTrue(tools.updatePropertiesOfElement(path, properties, context).startsWith("Die Aktualisierung ist fehlgeschlagen:"));

        assertSame(original, stored.get(cacheId));
        assertEquals(before, state());
        verify(repository, never()).save(any());
    }

    private String updateProperty(@Nullable String path,
                                  @Nullable String property,
                                  @Nullable Object value,
                                  @Nonnull ToolContext toolContext) {
        var properties = new HashMap<String, Object>();
        properties.put(property, value);
        return tools.updatePropertiesOfElement(path, properties, toolContext);
    }

    @Nonnull
    private JsonNode state() {
        var json = stored.get(cacheId).getCurrentElementJson();
        return json == null ? jsonMapper.nullNode() : jsonMapper.readTree(json);
    }

    @Nonnull
    private static ToolContext context(@Nullable String id) {
        return new ToolContext(new ChatContextModel(id == null ? "session" : id, id == null ? null : ElementType.GroupLayout, null, null).toMap());
    }
}
