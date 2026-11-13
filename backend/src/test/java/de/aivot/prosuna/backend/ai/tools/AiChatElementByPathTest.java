package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatElementByPathTest {
    private static final String TREE = """
            {"id":"root","type":3,"name":"Root name","children":[
              {"id":"group","type":3,"children":[
                {"id":"field","type":15,"label":"Field label","required":true,
                 "extension":{"items":[1,"two",null],"enabled":false}}
              ]},
              {"id":"sibling","type":15,"name":null,"weight":1.5}
            ]}
            """;

    private final JsonMapper jsonMapper = JsonMapperTestUtils.createMapper();
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(jsonMapper, repository);
    private final String cacheId = UUID.randomUUID().toString();
    private final ToolContext context = new ToolContext(new ChatContextModel(cacheId, ElementType.GroupLayout, null, null).toMap());

    @ParameterizedTest
    @ValueSource(strings = {"", "/children/0", "/children/0/children/0", "/children/1"})
    void shouldReturnElementWithoutChildrenWithoutChangingCache(String path) {
        var cache = cacheTree(TREE);
        var original = cache.getCurrentElementJson();
        var before = jsonMapper.readTree(original);

        var result = jsonMapper.readTree(tools.getElementByPath(path, context));

        var expected = (tools.jackson.databind.node.ObjectNode) before.at(path).deepCopy();
        expected.remove("children");
        assertEquals(expected, result);
        assertFalse(result.has("children"));
        assertSame(original, cache.getCurrentElementJson());
        assertEquals(before, jsonMapper.readTree(cache.getCurrentElementJson()));
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "children/0", "/", "/children", "/children/", "/children/-1",
            "/children/01", "/children/+1", "/children/1.0", "/children/-", "/children/0/",
            "/children/0/name", "/children/0/children", "/name", "/children~1/0", "/children/0\n"})
    void shouldRejectInvalidPathsWithoutChangingCache(@Nullable String path) {
        var cache = cacheTree(TREE);
        var before = jsonMapper.readTree(cache.getCurrentElementJson());

        assertTrue(tools.getElementByPath(path, context).startsWith("Das Formularelement konnte nicht abgerufen werden: Verwende einen leeren Pfad"));

        assertEquals(before, jsonMapper.readTree(cache.getCurrentElementJson()));
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/children/2", "/children/100", "/children/2147483648",
            "/children/999999999999999999999999999999999999999",
            "/children/0/children/1", "/children/0/children/0/children/0"})
    void shouldRejectMissingElementsWithoutChangingCache(String path) {
        var cache = cacheTree(TREE);
        var before = jsonMapper.readTree(cache.getCurrentElementJson());

        assertEquals("Das Formularelement konnte nicht abgerufen werden: Unter dem angegebenen Pfad existiert kein Formularelement.",
                tools.getElementByPath(path, context));

        assertEquals(before, jsonMapper.readTree(cache.getCurrentElementJson()));
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"children\":{\"0\":{\"id\":\"field\",\"type\":15}}}",
            "{\"children\":null}", "{\"children\":[]}",
            "{\"children\":[null]}", "{\"children\":[42]}", "{\"children\":[[]]}"
    })
    void shouldRequireChildrenArraysAndElementObjects(String json) {
        var cache = cacheTree(json);
        var before = jsonMapper.readTree(cache.getCurrentElementJson());

        assertEquals("Das Formularelement konnte nicht abgerufen werden: Unter dem angegebenen Pfad existiert kein Formularelement.",
                tools.getElementByPath("/children/0", context));

        assertEquals(before, jsonMapper.readTree(cache.getCurrentElementJson()));
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDescribeMissingTreeWithoutCreatingOne() {
        var cache = cacheTree(null);

        assertEquals("Das Formularelement konnte nicht abgerufen werden: Es ist noch kein Formularentwurf verfügbar.",
                tools.getElementByPath("", context));
        assertNull(cache.getCurrentElementJson());
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldRejectContextOutsideUiElementEditingMode() {
        var generalContext = new ToolContext(new ChatContextModel("session", null, null, null).toMap());

        var exception = assertThrows(IllegalStateException.class, () -> tools.getElementByPath("", generalContext));

        assertEquals("Die aktuelle Chatsitzung befindet sich nicht im Formularbearbeitungsmodus; es ist kein Formularentwurf verfügbar.",
                exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingCacheEntry() {
        when(repository.findById(cacheId)).thenReturn(Optional.empty());

        var exception = assertThrows(IllegalStateException.class, () -> tools.getElementByPath("", context));

        assertEquals("Die aktuelle Chatsitzung befindet sich im Formularbearbeitungsmodus, aber der Formularentwurf fehlt im Cache.",
                exception.getMessage());
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @Nonnull
    private AiUiElementChatSessionCacheEntity cacheTree(@Nullable String json) {
        var cache = new AiUiElementChatSessionCacheEntity()
                .setId(cacheId)
                .setCurrentElementJson(json);
        when(repository.findById(cacheId)).thenReturn(Optional.of(cache));
        return cache;
    }
}
