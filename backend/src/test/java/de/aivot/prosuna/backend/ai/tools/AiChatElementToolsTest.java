package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatElementToolsTest {
    private final JsonMapper jsonMapper = JsonMapperTestUtils.createMapper();
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(jsonMapper, repository);
    private final Map<String, AiUiElementChatSessionCacheEntity> stored = new HashMap<>();

    @BeforeEach
    void setUpRepository() {
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            AiUiElementChatSessionCacheEntity entity = invocation.getArgument(0);
            stored.put(entity.getId(), entity);
            return entity;
        });
    }

    @Test
    void shouldReturnTreePathsAndIndividualElements() {
        var context = createContext();
        var state = state(context);

        assertTrue(tools.getElementsTree(context).contains("Pfad=\"/children/0/children/0\""));
        var expectedRoot = state.deepCopy();
        expectedRoot.remove("children");
        assertEquals(expectedRoot, jsonMapper.readTree(tools.getElementByPath("", context)));
        assertEquals(state.at("/children/0/children/0"),
                jsonMapper.readTree(tools.getElementByPath("/children/0/children/0", context)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"children/0", "/", "/children", "/children/-1", "/children/01",
            "/children/100", "/children/0/name", "/children/0/children/0/children/0"})
    void shouldRejectInvalidOrMissingElementPaths(String path) {
        var context = createContext();
        var before = state(context).deepCopy();

        assertTrue(tools.getElementByPath(path, context).startsWith("Das Formularelement konnte nicht abgerufen werden:"));
        assertTrue(updateProperty(path, "name", "Changed", context).startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
    }

    @Test
    void shouldUpdateRootAndNestedPropertiesAcrossToolContexts() {
        var context = createContext();
        var sharedState = state(context);
        var nextContext = new ToolContext(new HashMap<>(context.getContext()));

        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                updateProperty("", "name", "Changed root", context));
        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                updateProperty("/children/0/children/0", "name", "Changed field", nextContext));

        var result = state(context);
        assertEquals("Changed root", result.path("name").asString());
        assertEquals("Changed field", result.at("/children/0/children/0/name").asString());
        assertEquals("Root", sharedState.path("name").asString());
    }

    @Test
    void shouldCreatePropertiesAndPreserveJsonValueTypes() {
        var context = createContext();
        var value = Map.of("enabled", true, "count", 3, "items", List.of("one", "two"));

        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                updateProperty("", "extension", value, context));
        assertEquals(jsonMapper.valueToTree(value), state(context).path("extension"));
        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                updateProperty("", "name", null, context));
        assertTrue(state(context).path("name").isNull());
        assertEquals(jsonMapper.valueToTree(value), state(context).path("extension"));
    }

    @Test
    void shouldReplaceChildrenWithValidElements() {
        var context = createContext();
        var children = List.of(Map.of("type", ElementType.Text.getKey(), "id", "replacement", "name", "New field"));

        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                updateProperty("", "children", children, context));
        assertEquals("replacement", jsonMapper.readTree(tools.getElementByPath("/children/0", context)).path("id").asString());
        assertEquals(1, state(context).path("children").size());
    }

    @Test
    void shouldLeaveStateUnchangedWhenUpdatesFailValidation() {
        var context = createContext();
        var before = state(context).deepCopy();

        assertTrue(updateProperty("/children/0", "type", -1, context).startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
        assertTrue(updateProperty("/children/0", "weight", Map.of("invalid", true), context)
                .startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
        assertTrue(updateProperty("", "children", List.of(Map.of("type", 0)), context)
                .startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
        assertTrue(updateProperty("", "children", Arrays.asList((Object) null), context)
                .startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n"})
    void shouldRejectBlankPropertyNames(String property) {
        var context = createContext();
        var before = state(context).deepCopy();

        assertTrue(updateProperty("", property, "value", context).startsWith("Die Aktualisierung ist fehlgeschlagen:"));
        assertEquals(before, state(context));
    }

    @Test
    void shouldKeepRequestsIsolated() {
        var first = createContext();
        var second = createContext();
        var secondBefore = state(second).deepCopy();

        updateProperty("", "name", "Changed root", first);

        assertEquals(secondBefore, state(second));
    }

    @Test
    void shouldRejectGeneralChatAndMissingCache() {
        var generalContext = new ToolContext(new ChatContextModel("general", null, null, null).toMap());
        var missingCache = new ToolContext(new ChatContextModel("missing", ElementType.GroupLayout, null, null).toMap());

        for (var context : List.of(generalContext, missingCache)) {
            assertThrows(IllegalStateException.class, () -> tools.getElementsTree(context));
            assertThrows(IllegalStateException.class, () -> tools.getElementByPath("", context));
            assertThrows(IllegalStateException.class, () -> updateProperty("", "name", "Changed", context));
        }
    }

    @Test
    void shouldValidateNestedTree() {
        assertEquals("Die JSON-Struktur ist gültig.", tools.verifyElementStructure(jsonMapper.writeValueAsString(state(createContext()))));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"null", "[]", "42", "{}", "{", "{\"type\":-1}", "{\"type\":15} {}",
            "{\"type\":3,\"children\":[null]}", "{\"type\":3,\"children\":[{}]}",
            "{\"type\":0,\"children\":[{\"type\":15}]}", "{\"type\":15,\"weight\":{}}"})
    void shouldRejectInvalidStructures(String json) {
        assertTrue(tools.verifyElementStructure(json).startsWith("Die JSON-Struktur ist ungültig:"));
    }

    @Test
    void shouldInjectToolContextAndUpdateStateThroughSpringAiCallback() {
        var context = createContext();
        var callbacks = ToolCallbacks.from(tools);
        var callback = Arrays.stream(callbacks)
                .filter(candidate -> candidate.getToolDefinition().name().equals("aktualisiere-element-eigenschaften"))
                .findFirst().orElseThrow();
        var schema = jsonMapper.readTree(callback.getToolDefinition().inputSchema());

        assertFalse(schema.path("properties").has("toolContext"));
        assertTrue(schema.path("properties").has("path"));
        assertEquals("object", schema.at("/properties/properties/type").asString());

        callback.call("{\"path\":\"/children/0/children/0\",\"properties\":{\"name\":\"Changed via callback\"}}", context);

        assertEquals("Changed via callback", state(context).at("/children/0/children/0/name").asString());
        callback.call("{\"path\":\"\",\"properties\":{\"name\":null}}", context);
        assertTrue(state(context).path("name").isNull());
    }

    private String updateProperty(String path, String property, Object value, ToolContext context) {
        var properties = new HashMap<String, Object>();
        properties.put(property, value);
        return tools.updatePropertiesOfElement(path, properties, context);
    }

    private ToolContext createContext() {
        var field = new TextInputElement();
        field.setName("Field");
        var group = new GroupLayoutElement();
        group.addChild(field);
        var root = new GroupLayoutElement();
        root.setName("Root");
        root.addChild(group);
        var sessionId = UUID.randomUUID().toString();
        stored.put(sessionId, new AiUiElementChatSessionCacheEntity().setId(sessionId)
                .setTargetRootType(ElementType.GroupLayout)
                .setCurrentElementJson(jsonMapper.writeValueAsString(root)));
        return new ToolContext(new ChatContextModel(sessionId, ElementType.GroupLayout, null, null).toMap());
    }

    private ObjectNode state(ToolContext context) {
        return (ObjectNode) jsonMapper.readTree(stored.get(ChatContextModel.fromToolContext(context).sessionId()).getCurrentElementJson());
    }
}
