package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiChatElementServiceTest {
    private static final String USER_ID = "owner";
    private static final String SESSION_ID = "session";
    private final PermissionService permissions = mock(PermissionService.class);
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final AiUiElementChatSessionCacheRepository cache = mock(AiUiElementChatSessionCacheRepository.class);
    private final JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final AiChatElementService service = new AiChatElementService(permissions, sessions, cache, mapper);
    private final AiChatSessionEntity session = new AiChatSessionEntity().setUserId(USER_ID).setSessionId(SESSION_ID);
    private final Map<String, AiUiElementChatSessionCacheEntity> stored = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(sessions.findByUserIdAndSessionId(USER_ID, SESSION_ID)).thenReturn(Optional.of(session));
        when(cache.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(cache.save(any())).thenAnswer(invocation -> {
            AiUiElementChatSessionCacheEntity entity = invocation.getArgument(0);
            stored.put(entity.getId(), entity);
            return entity;
        });
    }

    @Test
    void returnsTypedTreeAndSubsequentToolUpdatesWithoutWritingOnRead() throws Exception {
        var root = new GroupLayoutElement();
        var field = new TextInputElement();
        field.setName("Before");
        root.addChild(field);
        service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.GroupLayout, root);
        assertEquals(ElementType.GroupLayout, stored.get(SESSION_ID).getTargetRootType());
        assertEquals(mapper.valueToTree(root), mapper.valueToTree(service.getCurrentElement(USER_ID, SESSION_ID)));

        var tools = new AiChatElementTools(mapper, cache);
        var context = new ToolContext(new ChatContextModel(SESSION_ID, ElementType.GroupLayout, null, null).toMap());
        tools.updatePropertiesOfElement("/children/0", Map.of("name", "After"), context);
        clearInvocations(cache, sessions);

        var result = assertInstanceOf(GroupLayoutElement.class, service.getCurrentElement(USER_ID, SESSION_ID));
        var child = assertInstanceOf(TextInputElement.class, result.getChildren().getFirst());
        assertEquals("After", child.getName());
        assertEquals(field.getId(), child.getId());
        verify(cache).findById(SESSION_ID);
        verifyNoMoreInteractions(cache);
        verify(sessions, never()).save(any());
    }

    @Test
    void replacesStateUnderTheSameSessionId() throws Exception {
        service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.Text, new TextInputElement());
        var replacement = new GroupLayoutElement();
        service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.GroupLayout, replacement);

        assertEquals(1, stored.size());
        assertEquals(replacement, service.getCurrentElement(USER_ID, SESSION_ID));
        assertEquals(ElementType.GroupLayout, stored.get(SESSION_ID).getTargetRootType());
        verify(sessions, never()).save(any());
    }

    @Test
    void leavesCacheUnchangedWhenStateIsOmitted() throws Exception {
        var previous = new AiUiElementChatSessionCacheEntity().setId(SESSION_ID).setCurrentElementJson("{}");
        stored.put(SESSION_ID, previous);

        assertNull(service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.FormLayout, null));

        assertSame(previous, stored.get(SESSION_ID));
        verify(permissions).requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);
        verify(sessions).findByUserIdAndSessionId(USER_ID, SESSION_ID);
        verifyNoInteractions(cache);
    }

    @Test
    void infersRootTypeFromTheSuppliedDraft() throws Exception {
        var draft = new GroupLayoutElement();

        assertEquals(ElementType.GroupLayout, service.cacheCurrentElement(USER_ID, SESSION_ID, null, draft));

        assertEquals(ElementType.GroupLayout, stored.get(SESSION_ID).getTargetRootType());
        assertEquals(draft, service.getCurrentElement(USER_ID, SESSION_ID));
    }

    @Test
    void preservesNullPropertiesWhenCaching() throws Exception {
        var element = new TextInputElement();
        element.setName(null);

        service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.Text, element);

        var data = mapper.readTree(stored.get(SESSION_ID).getCurrentElementJson());
        assertTrue(data.has("name"));
        assertTrue(data.get("name").isNull());
        assertNull(service.getCurrentElement(USER_ID, SESSION_ID).getName());
    }

    @Test
    void deniesReadsAndWritesWithoutPermissionBeforeLookingUpSession() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissions)
                .requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseException.class,
                () -> service.getCurrentElement(USER_ID, SESSION_ID)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseException.class,
                () -> service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.Text, new TextInputElement())).getStatus());

        verifyNoInteractions(sessions, cache);
    }

    @Test
    void hidesOtherUsersSessionsForReadsAndWrites() {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseException.class,
                () -> service.getCurrentElement("other", SESSION_ID)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseException.class,
                () -> service.cacheCurrentElement("other", SESSION_ID, ElementType.Text, new TextInputElement())).getStatus());

        verifyNoInteractions(cache);
        verify(sessions, never()).save(any());
    }

    @Test
    void isolatesDifferentSessions() throws Exception {
        var otherSessionId = "other-session";
        var otherSession = new AiChatSessionEntity().setUserId("other").setSessionId(otherSessionId);
        when(sessions.findByUserIdAndSessionId("other", otherSessionId)).thenReturn(Optional.of(otherSession));
        var first = new TextInputElement();
        var second = new TextInputElement();
        service.cacheCurrentElement(USER_ID, SESSION_ID, ElementType.Text, first);
        service.cacheCurrentElement("other", otherSessionId, ElementType.Text, second);

        assertEquals(first.getId(), service.getCurrentElement(USER_ID, SESSION_ID).getId());
        assertEquals(second.getId(), service.getCurrentElement("other", otherSessionId).getId());
    }

    @Test
    void returnsSameNotFoundForUnknownSessionExpiredCacheAndMissingJson() {
        var unknown = assertThrows(ResponseException.class, () -> service.getCurrentElement(USER_ID, "unknown"));
        verifyNoInteractions(cache);
        var expired = assertThrows(ResponseException.class, () -> service.getCurrentElement(USER_ID, SESSION_ID));
        stored.put(SESSION_ID, new AiUiElementChatSessionCacheEntity().setId(SESSION_ID));
        var missingJson = assertThrows(ResponseException.class, () -> service.getCurrentElement(USER_ID, SESSION_ID));

        for (var exception : new ResponseException[]{unknown, expired, missingJson}) {
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals(unknown.getTitle(), exception.getTitle());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "42", "", "{\"type\":-1,\"internal\":\"private details\"}", "{\"type\":15} {}"})
    void returnsControlledErrorForInvalidCachedElement(String json) {
        stored.put(SESSION_ID, new AiUiElementChatSessionCacheEntity().setId(SESSION_ID)
                .setCurrentElementJson(json));

        var exception = assertThrows(ResponseException.class, () -> service.getCurrentElement(USER_ID, SESSION_ID));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Der zwischengespeicherte Formularentwurf konnte nicht geladen werden.", exception.getTitle());
        assertNull(exception.getDetails());
        assertNotNull(exception.getCause());
        verify(cache, never()).save(any());
    }

    @Test
    void typedResponseDropsUnknownPropertiesWithoutChangingCache() throws Exception {
        stored.put(SESSION_ID, new AiUiElementChatSessionCacheEntity().setId(SESSION_ID)
                .setCurrentElementJson(mapper.writeValueAsString(Map.of("type", ElementType.Text.getKey(), "id", "field", "extension", "value"))));

        var result = service.getCurrentElement(USER_ID, SESSION_ID);

        assertInstanceOf(TextInputElement.class, result);
        assertFalse(mapper.valueToTree(result).has("extension"));
        assertEquals("value", mapper.readTree(stored.get(SESSION_ID).getCurrentElementJson()).get("extension").asString());
    }
}
