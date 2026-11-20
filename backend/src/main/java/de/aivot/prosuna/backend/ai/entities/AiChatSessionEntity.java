package de.aivot.prosuna.backend.ai.entities;

import de.aivot.prosuna.backend.core.converters.JsonArrayConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_chat_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "ai_chat_sessions_user_session_key", columnNames = {"user_id", "session_id"})
})
public class AiChatSessionEntity {
    private static final String ID_SEQUENCE_NAME = "ai_chat_sessions_id_seq";

    @Id
    @Nullable
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, sequenceName = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Nonnull
    @Column(name = "session_id", nullable = false, length = 32)
    private String sessionId;

    @Nullable
    @Column(nullable = false, updatable = false)
    private Instant created;

    @Nullable
    @Column(nullable = false)
    private Instant updated;

    // Preserve the ordered execution trace separately from the compact chat memory.
    @Nonnull
    @Column(nullable = false, columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> messages = new ArrayList<>();

    @Nonnull
    @Column(name = "chat_messages", nullable = false, columnDefinition = "jsonb")
    @Convert(converter = JsonArrayConverter.class)
    private List<Map<String, Object>> chatMessages = new ArrayList<>();

    @Version
    @Nullable
    @Column(nullable = false)
    private Long version;

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = created;
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    @Nullable
    public Long getId() {
        return id;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    @Nonnull
    public AiChatSessionEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public String getSessionId() {
        return sessionId;
    }

    @Nonnull
    public AiChatSessionEntity setSessionId(@Nonnull String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nullable
    public Instant getCreated() {
        return created;
    }

    @Nullable
    public Instant getUpdated() {
        return updated;
    }

    @Nonnull
    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    @Nonnull
    public AiChatSessionEntity setMessages(@Nonnull List<Map<String, Object>> messages) {
        this.messages = messages;
        return this;
    }

    @Nonnull
    public List<Map<String, Object>> getChatMessages() {
        return chatMessages;
    }

    @Nonnull
    public AiChatSessionEntity setChatMessages(@Nonnull List<Map<String, Object>> chatMessages) {
        this.chatMessages = chatMessages;
        return this;
    }

    @Nullable
    public Long getVersion() {
        return version;
    }
}
