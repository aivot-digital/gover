package de.aivot.prosuna.backend.ai.repositories;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.core.converters.JsonArrayConverter;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Exercise the shared string converter without H2's different JSON string coercion rules.
        // PostgreSQL's JSONB storage and migration must be verified separately on PostgreSQL.
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:ai-chat-sessions;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS CHARACTER VARYING",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = AiChatSessionRepositoryTest.JpaTestConfiguration.class)
class AiChatSessionRepositoryTest {
    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String OTHER_USER_ID = "00000000-0000-0000-0000-000000000002";
    private static final String SESSION_ID = "a".repeat(32);

    @Autowired
    private AiChatSessionRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsEmptySessionWithTimestampsAndVersion() {
        var before = Instant.now().minusSeconds(1);
        var session = repository.saveAndFlush(session(USER_ID, SESSION_ID));
        entityManager.clear();

        var loaded = repository.findById(session.getId()).orElseThrow();
        assertEquals(USER_ID, loaded.getUserId());
        assertEquals(SESSION_ID, loaded.getSessionId());
        assertTrue(loaded.getMessages().isEmpty());
        assertTrue(loaded.getChatMessages().isEmpty());
        assertNotNull(loaded.getVersion());
        assertNotNull(loaded.getCreated());
        assertEquals(loaded.getCreated(), loaded.getUpdated());
        assertFalse(loaded.getCreated().isBefore(before));
        assertFalse(loaded.getCreated().isAfter(Instant.now()));
    }

    @Test
    void preservesCompleteOrderedMessageHistory() {
        List<Map<String, Object>> messages = List.of(
                Map.of("messageType", "system", "text", "Hilf bei der Formularerstellung."),
                Map.of("messageType", "user", "text", "Ergänze ein Feld für die Größe.",
                        "metadata", Map.of("source", Map.of("name", "Entwurf", "pages", List.of(1, 2))),
                        "media", List.of(Map.of("mimeType", "image/png", "url", "https://example.org/form.png"))),
                Map.of("messageType", "assistant", "text", "",
                        "toolCalls", List.of(Map.of("id", "call-1", "type", "function", "name", "addField",
                                "arguments", "{\"label\":\"Größe\"}"))),
                Map.of("messageType", "tool", "responses", List.of(Map.of(
                        "id", "call-1", "name", "addField", "responseData", "{\"success\":true}"))),
                Map.of("messageType", "assistant", "text", "Das Feld wurde ergänzt.",
                        "metadata", Map.of("usage", Map.of("totalTokens", 42)))
        );
        List<Map<String, Object>> chatMessages = List.of(
                Map.of("role", "user", "content", "Ergänze ein Feld für die Größe."),
                Map.of("role", "assistant", "content", "Das Feld wurde ergänzt."));
        var saved = repository.saveAndFlush(session(USER_ID, SESSION_ID)
                .setMessages(messages)
                .setChatMessages(chatMessages));
        entityManager.clear();

        var loaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(messages, loaded.getMessages());
        assertEquals(chatMessages, loaded.getChatMessages());
    }

    @Test
    void scopesSessionLookupToUser() {
        var first = repository.saveAndFlush(session(USER_ID, SESSION_ID));
        assertTrue(repository.findByUserIdAndSessionId(OTHER_USER_ID, SESSION_ID).isEmpty());

        var second = repository.saveAndFlush(session(OTHER_USER_ID, SESSION_ID));
        entityManager.clear();

        assertEquals(first.getId(), repository.findByUserIdAndSessionId(USER_ID, SESSION_ID).orElseThrow().getId());
        assertEquals(second.getId(), repository.findByUserIdAndSessionId(OTHER_USER_ID, SESSION_ID).orElseThrow().getId());
        assertTrue(repository.findByUserIdAndSessionId(USER_ID, "unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateUserAndSession() {
        repository.saveAndFlush(session(USER_ID, SESSION_ID));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(session(USER_ID, SESSION_ID)));
    }

    @Test
    void listsOnlyUserSessionsWithStableNewestFirstPagination() {
        var oldest = repository.saveAndFlush(session(USER_ID, "a".repeat(32)));
        var newer = repository.saveAndFlush(session(USER_ID, "b".repeat(32)));
        var newest = repository.saveAndFlush(session(USER_ID, "c".repeat(32)));
        repository.saveAndFlush(session(OTHER_USER_ID, SESSION_ID));
        setUpdated(oldest, Instant.parse("2026-01-01T00:00:00Z"));
        setUpdated(newer, Instant.parse("2026-01-02T00:00:00Z"));
        setUpdated(newest, Instant.parse("2026-01-02T00:00:00Z"));
        entityManager.clear();

        var firstPage = repository.findAllByUserIdOrderByUpdatedDescIdDesc(USER_ID, PageRequest.of(0, 2));
        var secondPage = repository.findAllByUserIdOrderByUpdatedDescIdDesc(USER_ID, PageRequest.of(1, 2));

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(List.of(newest.getId(), newer.getId()), firstPage.map(AiChatSessionEntity::getId).getContent());
        assertEquals(List.of(oldest.getId()), secondPage.map(AiChatSessionEntity::getId).getContent());
    }

    @Test
    void detectsInPlaceMessageChangesAndUpdatesTimestampAndVersion() {
        var saved = repository.saveAndFlush(session(USER_ID, SESSION_ID));
        var earlier = Instant.parse("2026-01-01T00:00:00Z");
        setUpdated(saved, earlier);
        entityManager.clear();
        var loaded = repository.findById(saved.getId()).orElseThrow();
        var created = loaded.getCreated();
        var version = loaded.getVersion();

        loaded.getMessages().add(Map.of("messageType", "user", "text", "Hallo"));
        repository.flush();
        entityManager.clear();

        var updated = repository.findById(saved.getId()).orElseThrow();
        assertEquals(loaded.getMessages(), updated.getMessages());
        assertEquals(created, updated.getCreated());
        assertTrue(updated.getUpdated().isAfter(earlier));
        assertEquals(version + 1, updated.getVersion());
    }

    @Test
    void rejectsStaleHistoryUpdates() {
        var stale = repository.saveAndFlush(session(USER_ID, SESSION_ID));
        entityManager.clear();
        var current = repository.findById(stale.getId()).orElseThrow();
        current.setMessages(List.of(Map.of("messageType", "user", "text", "Erste Änderung")));
        repository.flush();
        entityManager.clear();

        stale.setMessages(List.of(Map.of("messageType", "user", "text", "Veraltete Änderung")));
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> repository.saveAndFlush(stale));
    }

    private AiChatSessionEntity session(String userId, String sessionId) {
        return new AiChatSessionEntity().setUserId(userId).setSessionId(sessionId);
    }

    private void setUpdated(AiChatSessionEntity session, Instant updated) {
        entityManager.createNativeQuery("update ai_chat_sessions set updated = :updated where id = :id")
                .setParameter("updated", updated)
                .setParameter("id", session.getId())
                .executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = AiChatSessionEntity.class)
    @EnableJpaRepositories(basePackageClasses = AiChatSessionRepository.class)
    static class JpaTestConfiguration {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapperTestUtils.createMapper();
        }

        @Bean
        JsonArrayConverter jsonArrayConverter(JsonMapper jsonMapper) {
            return new JsonArrayConverter(jsonMapper);
        }
    }
}
