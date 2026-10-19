package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.GlobalExceptionHandler;
import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentMetadata;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.services.AiChatAttachmentService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.ai.tools.AiChatProcessTools;
import de.aivot.prosuna.backend.ai.tools.AiChatElementSchemaTools;
import de.aivot.prosuna.backend.ai.tools.AiChatAttachmentTools;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.ai.services.AiInputValueSchemaService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ExceptionMailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiChatControllerTest {
    private static final String USER_ID = "owner";
    private static final String SESSION_ID = "session";
    private final PermissionService permissions = mock(PermissionService.class);
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final AiUiElementChatSessionCacheRepository cache = mock(AiUiElementChatSessionCacheRepository.class);
    private final ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final AiChatAttachmentService attachmentService = mock(AiChatAttachmentService.class);
    private final AiChatTraceService traceService = mock(AiChatTraceService.class);
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID).build();
    private final AiChatSessionEntity session = new AiChatSessionEntity().setUserId(USER_ID).setSessionId(SESSION_ID);
    private AiChatController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var mapper = JsonMapperTestUtils.createMapper();
        var builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        when(traceService.startTurn(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new AiChatTraceContext(USER_ID, SESSION_ID, "turn"));
        var service = new AiChatElementService(permissions, sessions, cache, mapper);
        controller = new AiChatController(builder, new AiChatElementTools(mapper, cache),
                permissions, sessions, service, new AiChatSharedTools(), new AiChatElementSchemaTools(mapper, new AiInputValueSchemaService(mapper)), new AiChatProcessTools(mock(AiChatProcessService.class)), mock(AiChatProcessService.class),
                attachmentService, new AiChatAttachmentTools(),
                traceService, ToolCallingManager.builder().build(), chatMemory,
                Duration.ofMinutes(10));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new ResourceHttpMessageConverter(), new JacksonJsonHttpMessageConverter(mapper))
                .setControllerAdvice(new GlobalExceptionHandler(mock(ExceptionMailService.class), mock(ProsunaConfig.class)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(sessions.findByUserIdAndSessionId(USER_ID, SESSION_ID)).thenReturn(Optional.of(session));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void retrievesElementAsJsonWithoutEnvelope() throws Exception {
        when(cache.findById(SESSION_ID)).thenReturn(Optional.of(new AiUiElementChatSessionCacheEntity().setId(SESSION_ID)
                .setCurrentElementJson("{\"type\":15,\"id\":\"field\",\"name\":\"Cached field\"}")));

        mvc.perform(get("/api/ai/chat/element/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("field"))
                .andExpect(jsonPath("$.type").value(ElementType.Text.getKey()))
                .andExpect(jsonPath("$.name").value("Cached field"));

        verify(permissions).requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);
        verify(sessions).findByUserIdAndSessionId(USER_ID, SESSION_ID);
        verifyNoInteractions(client);
    }

    @Test
    void retrievesVisibleMessagesForOwnedSession() throws Exception {
        chatMemory.add("owner:session", List.of(
                UserMessage.builder().text("Mein Hund heißt Bello.").metadata(Map.of(
                        AiChatAttachmentContext.ATTACHMENTS_METADATA_KEY,
                        List.of(new AiChatAttachmentMetadata("formular.pdf", 1234, "application/pdf"))
                )).build(),
                new AssistantMessage("Ich habe den Namen übernommen.")));

        mvc.perform(get("/api/ai/chat/messages/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Mein Hund heißt Bello."))
                .andExpect(jsonPath("$[0].attachments[0].name").value("formular.pdf"))
                .andExpect(jsonPath("$[0].attachments[0].size").value(1234))
                .andExpect(jsonPath("$[0].attachments[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$[1].role").value("assistant"))
                .andExpect(jsonPath("$[1].content").value("Ich habe den Namen übernommen."))
                .andExpect(jsonPath("$[1].attachments").isEmpty());

        verify(permissions).requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);
        verify(sessions).findByUserIdAndSessionId(USER_ID, SESSION_ID);
    }

    @Test
    void returnsEmptyMessageListForNewSession() throws Exception {
        mvc.perform(get("/api/ai/chat/messages/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void rejectsMissingOrUnownedMessageSession() throws Exception {
        mvc.perform(get("/api/ai/chat/messages/"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ai/chat/messages/").param("chatSessionId", "unowned"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresSessionIdQueryParameter() throws Exception {
        mvc.perform(get("/api/ai/chat/element/")).andExpect(status().isBadRequest());

        verifyNoInteractions(permissions, sessions, cache, client);
    }

    @Test
    void returnsForbiddenWithoutPermission() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissions)
                .requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);

        mvc.perform(get("/api/ai/chat/element/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sessions, cache, client);
    }

    @Test
    void returnsNotFoundForUnavailableElementOrUnownedSession() throws Exception {
        mvc.perform(get("/api/ai/chat/element/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/ai/chat/element/").param("chatSessionId", "unowned"))
                .andExpect(status().isNotFound());

        verify(cache).findById(SESSION_ID);
        verifyNoInteractions(client);
    }

    @Test
    void returnsControlledServerErrorForInvalidCache() throws Exception {
        when(cache.findById(SESSION_ID)).thenReturn(Optional.of(new AiUiElementChatSessionCacheEntity().setId(SESSION_ID)
                .setCurrentElementJson("{\"type\":-1}")));

        mvc.perform(get("/api/ai/chat/element/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void downloadsTheOwnedSessionTraceAsJsonAttachment() throws Exception {
        var content = "{\"schemaVersion\":1}".getBytes(StandardCharsets.UTF_8);
        when(traceService.exportTrace(USER_ID, SESSION_ID))
                .thenReturn(new AiChatTraceService.TraceExport(content, "prosuna-ai-chat-trace-session.json"));

        mvc.perform(get("/api/ai/chat/trace/").param("chatSessionId", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(
                                "attachment; filename=\"prosuna-ai-chat-trace-session.json\"")))
                .andExpect(content().bytes(content));

        verify(traceService).exportTrace(USER_ID, SESSION_ID);
        verifyNoInteractions(client);
    }

    @Test
    void cachesSuppliedStateBeforeStartingChatAndPassesSessionToTools() throws Exception {
        var request = client.prompt();
        when(client.prompt()).thenAnswer(invocation -> {
            verify(cache).save(argThat(entity -> SESSION_ID.equals(entity.getId()) && entity.getCurrentElementJson() != null));
            return request;
        });

        controller.send(jwt, SESSION_ID, "Edit", ElementType.Text, new TextInputElement(), null, null, null);

        ArgumentCaptor<Map<String, Object>> context = ArgumentCaptor.captor();
        verify(request).toolContext(context.capture());
        assertEquals(ElementType.Text, context.getValue().get("targetRootType"));
        assertEquals(SESSION_ID, context.getValue().get("sessionId"));
        verify(cache).save(any());
    }

    @Test
    void chatWithoutStateDoesNotWriteCacheOrEnableElementEditingMode() throws Exception {
        controller.send(jwt, SESSION_ID, "Hello", ElementType.FormLayout, null, null, null, null);

        ArgumentCaptor<Map<String, Object>> context = ArgumentCaptor.captor();
        verify(client.prompt()).toolContext(context.capture());
        assertNull(context.getValue().get("targetRootType"));
        verify(sessions, never()).save(any());
        verifyNoInteractions(cache);
    }

    @Test
    void deniesUnownedSessionBeforeProcessingAttachmentsOrCallingModel() {
        var attachment = new MockMultipartFile("attachments", "private.txt", "text/plain", new byte[]{1});

        var exception = assertThrows(ResponseException.class,
                () -> controller.send(jwt, "unowned", "Edit", ElementType.FormLayout,
                        new TextInputElement(), null, null, new MockMultipartFile[]{attachment}));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verifyNoInteractions(cache, client, attachmentService);
    }

    @Test
    void deniesChatWithoutPermissionEvenWithoutDraft() throws Exception {
        doThrow(ResponseException.forbidden()).when(permissions)
                .requireSystemPermission(USER_ID, AiChatPermissionProvider.AI_CHAT_USE);

        var exception = assertThrows(ResponseException.class,
                () -> controller.send(jwt, SESSION_ID, "Hello", null, null, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(sessions, cache, client, attachmentService);
    }
}
