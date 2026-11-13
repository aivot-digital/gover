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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatElementsTreeTest {
    private final JsonMapper jsonMapper = JsonMapperTestUtils.createMapper();
    private final AiUiElementChatSessionCacheRepository repository = mock(AiUiElementChatSessionCacheRepository.class);
    private final AiChatElementTools tools = new AiChatElementTools(jsonMapper, repository);
    private final String cacheId = UUID.randomUUID().toString();
    private final ToolContext context = new ToolContext(new ChatContextModel(cacheId, ElementType.GroupLayout, null, null).toMap());

    @Test
    void shouldReturnOnlyIdsTypesAndPathsInTreeOrderWithoutChangingCache() {
        var cache = cacheTree("""
                {"id":"root","type":3,"name":"Root name","children":[
                  {"id":"group","type":3,"children":[
                    {"id":"field","type":15,"label":"Field label"}
                  ]},
                  {"id":"sibling","type":15,"metadata":{
                    "children":[{"id":"not-an-element","type":15}]
                  }}
                ]}
                """);
        var original = cache.getCurrentElementJson();
        var before = jsonMapper.readTree(original);

        assertEquals("""
                - Element-ID="root", Typ-Schlüssel=3, Pfad="", Name="Root name"
                  - Element-ID="group", Typ-Schlüssel=3, Pfad="/children/0", Name=null
                    - Element-ID="field", Typ-Schlüssel=15, Pfad="/children/0/children/0", Name=null
                  - Element-ID="sibling", Typ-Schlüssel=15, Pfad="/children/1", Name=null
                """.stripTrailing(), tools.getElementsTree(context));

        assertSame(original, cache.getCurrentElementJson());
        assertEquals(before, jsonMapper.readTree(cache.getCurrentElementJson()));
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\":\"root\",\"type\":15}",
            "{\"id\":\"root\",\"type\":3,\"children\":[]}",
            "{\"id\":\"root\",\"type\":3,\"children\":null}"
    })
    void shouldReturnSingleLineForElementsWithoutChildren(String json) {
        var cache = cacheTree(json);

        assertEquals("- Element-ID=\"root\", Typ-Schlüssel="
                        + jsonMapper.readTree(cache.getCurrentElementJson()).get("type") + ", Pfad=\"\", Name=null",
                tools.getElementsTree(context));
    }

    @Test
    void shouldEscapeIdsAsJsonStrings() {
        cacheTree("""
                {"id":"quoted\\\"\\nbackslash\\\\/ä","type":15}
                """);

        assertEquals("- Element-ID=\"quoted\\\"\\nbackslash\\\\/ä\", Typ-Schlüssel=15, Pfad=\"\", Name=null",
                tools.getElementsTree(context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"id\":null,\"type\":null}"})
    void shouldRepresentMissingIdsAndTypesAsNull(String json) {
        cacheTree(json);

        assertEquals("- Element-ID=null, Typ-Schlüssel=null, Pfad=\"\", Name=null", tools.getElementsTree(context));
    }

    @Test
    void shouldDescribeMissingTreeWithoutCreatingOne() {
        var cache = cacheTree(null);

        assertEquals("Es ist noch kein Formularentwurf verfügbar.", tools.getElementsTree(context));
        assertNull(cache.getCurrentElementJson());
        verify(repository).findById(cacheId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldRejectContextOutsideUiElementEditingMode() {
        var generalContext = new ToolContext(new ChatContextModel("session", null, null, null).toMap());

        var exception = assertThrows(IllegalStateException.class, () -> tools.getElementsTree(generalContext));

        assertEquals("Die aktuelle Chatsitzung befindet sich nicht im Formularbearbeitungsmodus; es ist kein Formularentwurf verfügbar.",
                exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectMissingCacheEntry() {
        when(repository.findById(cacheId)).thenReturn(Optional.empty());

        var exception = assertThrows(IllegalStateException.class, () -> tools.getElementsTree(context));

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
