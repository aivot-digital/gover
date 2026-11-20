package de.aivot.prosuna.backend.ai.repositories;

import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long>, JpaSpecificationExecutor<AiChatSessionEntity> {
    @Nonnull
    Optional<AiChatSessionEntity> findByUserIdAndSessionId(@Nonnull String userId, @Nonnull String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AiChatSessionEntity session where session.userId = :userId and session.sessionId = :sessionId")
    @Nonnull
    Optional<AiChatSessionEntity> findByUserIdAndSessionIdForUpdate(@Nonnull @Param("userId") String userId,
                                                                    @Nonnull @Param("sessionId") String sessionId);

    @Nonnull
    Page<AiChatSessionEntity> findAllByUserIdOrderByUpdatedDescIdDesc(@Nonnull String userId, @Nonnull Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update ai_chat_sessions
            set messages = '[]'::jsonb,
                version = version + 1
            where updated < :cutoff
              and messages <> '[]'::jsonb
            """, nativeQuery = true)
    int clearMessagesUpdatedBefore(@Nonnull @Param("cutoff") Instant cutoff);
}
