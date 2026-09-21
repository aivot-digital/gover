package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiChatControllerMultipartTest {
    private final AiChatController controller = mock(AiChatController.class);
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("owner").build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        // Isolate HTTP binding from model execution, document parsing and persistence.
        when(controller.send(any(Jwt.class), anyString(), anyString(), nullable(ElementType.class),
                nullable(BaseElement.class), nullable(Integer.class), nullable(Integer.class),
                nullable(MultipartFile[].class))).thenReturn(Flux.just("Hello"));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter(JsonMapperTestUtils.createMapper()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsRequiredFieldsWithoutOptionalPartsAndStreamsSse() throws Exception {
        var result = mvc.perform(multipart("/api/ai/chat/send/")
                        .param("chatSessionId", "session")
                        .param("userInput", "Hello")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string("data:Hello\n\n"));
        verify(controller).send(jwt, "session", "Hello", null, null, null, null, null);
    }

    @Test
    void bindsAllMultipartFieldsIncludingNumericEnumZeroAndMultipleFiles() throws Exception {
        var first = new MockMultipartFile("attachments", "first.txt", "text/plain", "First".getBytes(StandardCharsets.UTF_8));
        var second = new MockMultipartFile("attachments", "second.txt", "text/plain", "Second".getBytes(StandardCharsets.UTF_8));

        var result = mvc.perform(multipart("/api/ai/chat/send/")
                        .file(jsonPart("targetRootType", "0"))
                        .file(jsonPart("currentState", "{\"type\":15,\"id\":\"field\",\"name\":\"Straße\"}"))
                        .file(first)
                        .file(second)
                        .param("chatSessionId", "session")
                        .param("userInput", "Edit")
                        .param("processId", "42")
                        .param("processVersion", "0")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk());

        var state = ArgumentCaptor.forClass(BaseElement.class);
        var attachments = ArgumentCaptor.forClass(MultipartFile[].class);
        verify(controller).send(eq(jwt), eq("session"), eq("Edit"), eq(ElementType.FormLayout),
                state.capture(), eq(42), eq(0), attachments.capture());
        assertInstanceOf(TextInputElement.class, state.getValue());
        assertEquals("field", state.getValue().getId());
        assertEquals("Straße", state.getValue().getName());
        assertArrayEquals(new MultipartFile[]{first, second}, attachments.getValue());
    }

    @ParameterizedTest
    @CsvSource({
            "targetRootType, -1",
            "targetRootType, invalid-json",
            "currentState, invalid-json",
            "currentState, {\"type\":-1}",
    })
    void rejectsInvalidJsonAndUnknownElementTypes(String name, String json) throws Exception {
        mvc.perform(multipart("/api/ai/chat/send/")
                        .file(jsonPart(name, json))
                        .param("chatSessionId", "session")
                        .param("userInput", "Edit")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(controller);
    }

    private static MockMultipartFile jsonPart(String name, String json) {
        return new MockMultipartFile(name, "blob", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }
}
