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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatCreateElementTest {
    private static final String SESSION_ID = "session";
    private static final String TREE = """
            {"id":"root","type":3,"extension":{"keep":[true,1,null]},"children":[
              {"id":"group","type":3,"children":[{"id":"field","type":15,"name":"Field"}]},
              {"id":"sibling","type":15,"name":"Sibling"}
            ]}
            """;
    private final JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(mapper, repository);
    private final Map<String, AiUiElementChatSessionCacheEntity> stored = new HashMap<>();
    private final ToolContext context = new ToolContext(new ChatContextModel(SESSION_ID, ElementType.GroupLayout, null, null).toMap());

    @BeforeEach
    void setUp() {
        stored.put(SESSION_ID, new AiUiElementChatSessionCacheEntity().setId(SESSION_ID)
                .setTargetRootType(ElementType.GroupLayout).setCurrentElementJson(TREE));
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.save(any(AiUiElementChatSessionCacheEntity.class))).thenAnswer(invocation -> {
            AiUiElementChatSessionCacheEntity entity = invocation.getArgument(0);
            stored.put(entity.getId(), entity);
            return entity;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/children/0"})
    void appendsWithServerGeneratedIdAndPreservesExistingTree(String parentPath) {
        var original = stored.get(SESSION_ID);
        var before = state();
        var index = before.at(parentPath).path("children").size();

        var result = mapper.readTree(tools.createElement(parentPath, ElementType.Text.getKey(), context));

        var path = parentPath + "/children/" + index;
        assertEquals(3, result.size());
        assertEquals(path, result.path("path").asString());
        assertEquals(ElementType.Text.getKey().intValue(), result.path("type").asInt());
        assertFalse(result.path("id").asString().isBlank());
        var created = state().at(path);
        assertEquals(result.path("id"), created.path("id"));
        assertEquals(result.path("type"), created.path("type"));
        assertTrue(created.path("name").isNull());
        assertEquals(created, mapper.readTree(tools.getElementByPath(path, context)));
        assertTrue(tools.getElementsTree(context).contains("Pfad=\"" + path + "\""));
        var withoutNewElement = state();
        ((ArrayNode) withoutNewElement.at(parentPath).get("children")).remove(index);
        assertEquals(before, withoutNewElement);
        assertNotSame(original, stored.get(SESSION_ID));
        assertEquals(TREE, original.getCurrentElementJson());
        assertEquals(ElementType.GroupLayout, stored.get(SESSION_ID).getTargetRootType());
        verify(repository).save(stored.get(SESSION_ID));
    }

    @Test
    void supportsSequentialCreationAndConfigurationUsingReturnedPaths() {
        var group = mapper.readTree(tools.createElement("", ElementType.GroupLayout.getKey(), context));
        var groupPath = group.path("path").asString();
        var first = mapper.readTree(tools.createElement(groupPath, ElementType.Text.getKey(), context));
        var second = mapper.readTree(tools.createElement(groupPath, ElementType.Text.getKey(), context));

        assertEquals("/children/2", groupPath);
        assertEquals(groupPath + "/children/0", first.path("path").asString());
        assertEquals(groupPath + "/children/1", second.path("path").asString());
        assertNotEquals(first.path("id"), second.path("id"));
        assertNotEquals(group.path("id"), first.path("id"));
        assertEquals("Die Eigenschaften des Formularelements wurden erfolgreich aktualisiert.",
                tools.updatePropertiesOfElement(first.path("path").asString(), Map.of("name", "New field"), context));
        assertEquals("New field", state().at(first.path("path").asString()).path("name").asString());
        verify(repository, times(4)).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"id\":\"root\",\"type\":3}",
            "{\"id\":\"root\",\"type\":3,\"children\":null}",
            "{\"id\":\"root\",\"type\":3,\"children\":[]}"})
    void initializesEmptyChildrenForLayouts(String json) {
        stored.get(SESSION_ID).setCurrentElementJson(json);

        var result = mapper.readTree(tools.createElement("", ElementType.Text.getKey(), context));

        assertEquals("/children/0", result.path("path").asString());
        assertEquals(1, state().path("children").size());
    }

    @Test
    void acceptsStepUnderFormButRejectsIncompatibleChildTypes() {
        stored.get(SESSION_ID).setCurrentElementJson("{\"id\":\"form\",\"type\":0,\"children\":[]}");
        assertRejected("", ElementType.Text.getKey());

        var result = mapper.readTree(tools.createElement("", ElementType.Step.getKey(), context));

        assertEquals("/children/0", result.path("path").asString());
        assertEquals(ElementType.Step.getKey().intValue(), state().at("/children/0/type").asInt());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 999, 21})
    void rejectsUnknownAndRetiredTypes(@Nullable Integer type) {
        assertRejected("", type);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "children/0", "/", "/children", "/children/-1", "/children/01",
            "/children/999", "/children/2147483648", "/children/0/name", "/children/1", "/children/0/children/0"})
    void rejectsInvalidPathsAndLeafParents(@Nullable String path) {
        assertRejected(path, ElementType.Text.getKey());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{", "null", "[]", "42", "", "{} {}",
            "{\"type\":3,\"children\":{}}", "{\"type\":3,\"children\":[null]}",
            "{\"type\":3,\"children\":[{\"type\":-1}]}",
            "{\"type\":15,\"children\":[]}"})
    void rejectsMissingOrInvalidTreeWithoutReplacingIt(@Nullable String json) {
        stored.get(SESSION_ID).setCurrentElementJson(json);
        assertRejected("", ElementType.Text.getKey());
    }

    @Test
    void validatesUnchangedSiblingsBeforeSaving() {
        stored.get(SESSION_ID).setCurrentElementJson("""
                {"type":3,"children":[{"type":3,"children":[]},{"type":-1}]}
                """);

        assertRejected("/children/0", ElementType.Text.getKey());
    }

    @Test
    void preservesContextAndMissingCacheExceptions() {
        var generalContext = new ToolContext(new ChatContextModel(SESSION_ID, null, null, null).toMap());
        assertThrows(IllegalStateException.class, () -> tools.createElement("", 15, generalContext));
        verifyNoInteractions(repository);
        stored.clear();

        assertThrows(IllegalStateException.class, () -> tools.createElement("", 15, context));
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotMutateLoadedEntityOrReportSuccessWhenSavingFails() {
        var original = stored.get(SESSION_ID);
        var failure = new DataAccessResourceFailureException("Storage unavailable");
        when(repository.save(any())).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> tools.createElement("", 15, context)));

        assertSame(original, stored.get(SESSION_ID));
        assertEquals(TREE, original.getCurrentElementJson());
    }

    @Test
    void registersExactToolNameAndCreatesThroughSpringAiCallback() {
        var callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("erstelle-element"))
                .findFirst().orElseThrow();
        var schema = mapper.readTree(callback.getToolDefinition().inputSchema());
        assertEquals(2, schema.path("properties").size());
        assertTrue(schema.path("properties").has("parentPath"));
        assertEquals("integer", schema.at("/properties/type/type").asString());
        assertFalse(schema.path("properties").has("toolContext"));

        callback.call("{\"parentPath\":\"/children/0\",\"type\":15}", context);

        assertEquals(2, state().at("/children/0/children").size());
        assertEquals(15, state().at("/children/0/children/1/type").asInt());
        verify(repository).save(any());
    }

    private void assertRejected(@Nullable String path, @Nullable Integer type) {
        var original = stored.get(SESSION_ID);
        var json = original.getCurrentElementJson();

        assertTrue(tools.createElement(path, type, context).startsWith("Das Formularelement konnte nicht erstellt werden:"));

        assertSame(original, stored.get(SESSION_ID));
        assertEquals(json, original.getCurrentElementJson());
        verify(repository, never()).save(any());
    }

    @Nonnull
    private JsonNode state() {
        return mapper.readTree(stored.get(SESSION_ID).getCurrentElementJson());
    }
}
