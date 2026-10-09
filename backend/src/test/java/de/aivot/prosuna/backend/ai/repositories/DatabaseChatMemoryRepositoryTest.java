package de.aivot.prosuna.backend.ai.repositories;

import de.aivot.prosuna.backend.ai.configuration.AiChatMemoryConfiguration;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseChatMemoryRepositoryTest {
    private final AiChatSessionRepository sessions = mock(AiChatSessionRepository.class);
    private final Map<String, AiChatSessionEntity> stored = new HashMap<>();
    private final DatabaseChatMemoryRepository repository = new DatabaseChatMemoryRepository(sessions);

    @BeforeEach
    void setUp() {
        when(sessions.findByUserIdAndSessionId(anyString(), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(key(invocation.getArgument(0), invocation.getArgument(1)))));
        when(sessions.findByUserIdAndSessionIdForUpdate(anyString(), anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(key(invocation.getArgument(0), invocation.getArgument(1)))));
        when(sessions.findAll()).thenAnswer(invocation -> List.copyOf(stored.values()));
        when(sessions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesOnlyConversationTextAndRestoresItFromAnotherRepositoryInstance() {
        addSession("owner", "session");
        var user = new UserMessage("Größe des Hundes?\nBitte in cm.");
        var assistant = AssistantMessage.builder().content("Antwort\nmit Umlauten: äöüß")
                .properties(Map.of("reasoningContent", "private reasoning")).build();

        repository.saveAll("owner:session", List.of(new SystemMessage("System"), user,
                AssistantMessage.builder().toolCalls(List.of(
                        new AssistantMessage.ToolCall("id", "function", "read", "{}"))).build(),
                ToolResponseMessage.builder().responses(List.of(
                        new ToolResponseMessage.ToolResponse("id", "read", "tool data"))).build(),
                assistant));

        var restored = new DatabaseChatMemoryRepository(sessions).findByConversationId("owner:session");
        assertThat(restored).containsExactly(user, new AssistantMessage(assistant.getText()));
        assertThat(stored.get("owner:session").getChatMessages()).containsExactly(
                Map.of("role", "user", "content", user.getText()),
                Map.of("role", "assistant", "content", assistant.getText()));
        assertThat(repository.findConversationIds()).containsExactly("owner:session");
    }

    @Test
    void replacesClearsAndIsolatesConversations() {
        addSession("owner", "a");
        addSession("owner", "b");
        assertThat(repository.findByConversationId("owner:missing")).isEmpty();
        repository.saveAll("owner:a", List.of(new UserMessage("Old")));
        repository.saveAll("owner:b", List.of(new UserMessage("Other")));
        repository.saveAll("owner:a", List.of(new UserMessage("New")));
        assertThat(repository.findByConversationId("owner:a")).containsExactly(new UserMessage("New"));
        repository.deleteByConversationId("owner:a");
        assertThat(repository.findByConversationId("owner:a")).isEmpty();
        assertThat(repository.findByConversationId("owner:b")).containsExactly(new UserMessage("Other"));
        repository.saveAll("owner:b", List.of());
        assertThat(repository.findConversationIds()).isEmpty();
    }

    @Test
    void rejectsMissingSessionsInvalidIdsAndCorruptMessages() {
        assertThatThrownBy(() -> repository.saveAll("owner:missing", List.of(new UserMessage("Text"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> repository.findByConversationId("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        addSession("owner", "broken").setChatMessages(List.of(Map.of("role", "tool", "content", "Data")));
        assertThatThrownBy(() -> repository.findByConversationId("owner:broken"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void configuredWindowKeepsTwentyMessagesAndCanResumeFromDatabase() {
        addSession("owner", "session");
        var configuration = new AiChatMemoryConfiguration();
        var memory = configuration.chatMemory(repository);
        for (int i = 0; i < 12; i++) {
            memory.add("owner:session", List.of(
                    new UserMessage("Question " + i), new AssistantMessage("Answer " + i)));
        }

        var resumed = configuration.chatMemory(new DatabaseChatMemoryRepository(sessions));
        assertThat(resumed.get("owner:session")).hasSize(20)
                .first().isEqualTo(new UserMessage("Question 2"));
        assertThat(resumed.get("owner:session")).last().isEqualTo(new AssistantMessage("Answer 11"));
    }

    @Test
    void restoresAttachmentMetadataWithoutPersistingFileContent() {
        addSession("owner", "session").setChatMessages(List.of(Map.of(
                "role", "user",
                "content", "Bauen Sie das Formular nach.",
                "attachments", List.of(Map.of(
                        "name", "formular.pdf",
                        "size", 1234,
                        "contentType", "application/pdf"
                ))
        )));

        var message = repository.findByConversationId("owner:session").getFirst();

        assertThat(message.getText()).isEqualTo("Bauen Sie das Formular nach.");
        assertThat(message.getMetadata().get("attachments")).isEqualTo(List.of(Map.of(
                "name", "formular.pdf",
                "size", 1234,
                "contentType", "application/pdf"
        )));
        assertThat(stored.get("owner:session").getChatMessages().toString()).doesNotContain("Dateiinhalt");
    }

    private AiChatSessionEntity addSession(String userId, String sessionId) {
        var session = new AiChatSessionEntity().setUserId(userId).setSessionId(sessionId);
        stored.put(key(userId, sessionId), session);
        return session;
    }

    private static String key(String userId, String sessionId) {
        return DatabaseChatMemoryRepository.conversationId(userId, sessionId);
    }
}
