package de.aivot.prosuna.backend.ai.repositories;

import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class DatabaseChatMemoryRepository implements ChatMemoryRepository {
    private static final String USER_ROLE = "user";
    private static final String ASSISTANT_ROLE = "assistant";

    private final AiChatSessionRepository repository;

    public DatabaseChatMemoryRepository(@Nonnull AiChatSessionRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public static String conversationId(@Nonnull String userId, @Nonnull String sessionId) {
        return userId + ":" + sessionId;
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public List<String> findConversationIds() {
        return repository.findAll().stream()
                .filter(session -> !session.getChatMessages().isEmpty())
                .map(session -> conversationId(session.getUserId(), session.getSessionId()))
                .toList();
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public List<Message> findByConversationId(@Nonnull String conversationId) {
        var id = parseConversationId(conversationId);
        return repository.findByUserIdAndSessionId(id.userId(), id.sessionId())
                .map(session -> session.getChatMessages().stream()
                        .map(DatabaseChatMemoryRepository::toMessage)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public void saveAll(@Nonnull String conversationId, @Nonnull List<Message> messages) {
        var id = parseConversationId(conversationId);
        var session = repository.findByUserIdAndSessionIdForUpdate(id.userId(), id.sessionId())
                .orElseThrow(() -> new IllegalStateException("Chat session does not exist"));
        var storedMessages = messages.stream()
                .filter(message -> message instanceof UserMessage
                        || message instanceof AssistantMessage assistant
                        && !assistant.hasToolCalls()
                        && !Objects.requireNonNullElse(assistant.getText(), "").isBlank())
                .map(DatabaseChatMemoryRepository::toStoredMessage)
                .toList();
        session.setChatMessages(storedMessages);
        repository.saveAndFlush(session);
    }

    @Override
    @Transactional
    public void deleteByConversationId(@Nonnull String conversationId) {
        var id = parseConversationId(conversationId);
        repository.findByUserIdAndSessionIdForUpdate(id.userId(), id.sessionId()).ifPresent(session -> {
            session.setChatMessages(List.of());
            repository.saveAndFlush(session);
        });
    }

    @Nonnull
    private static Map<String, Object> toStoredMessage(@Nonnull Message message) {
        var role = message instanceof UserMessage ? USER_ROLE : ASSISTANT_ROLE;
        return Map.of("role", role, "content", Objects.requireNonNullElse(message.getText(), ""));
    }

    @Nonnull
    private static Message toMessage(@Nonnull Map<String, Object> storedMessage) {
        var role = storedMessage.get("role");
        var content = storedMessage.get("content");
        if (!(role instanceof String storedRole) || !(content instanceof String text)) {
            throw new IllegalStateException("Stored chat message is invalid");
        }
        return switch (storedRole) {
            case USER_ROLE -> new UserMessage(text);
            case ASSISTANT_ROLE -> new AssistantMessage(text);
            default -> throw new IllegalStateException("Unsupported stored chat message role: " + storedRole);
        };
    }

    @Nonnull
    private static ConversationId parseConversationId(@Nonnull String conversationId) {
        var separator = conversationId.lastIndexOf(':');
        if (separator <= 0 || separator == conversationId.length() - 1) {
            throw new IllegalArgumentException("Invalid conversation ID");
        }
        return new ConversationId(conversationId.substring(0, separator), conversationId.substring(separator + 1));
    }

    private record ConversationId(@Nonnull String userId, @Nonnull String sessionId) {
    }
}
