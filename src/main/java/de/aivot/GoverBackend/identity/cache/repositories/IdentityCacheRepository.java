package de.aivot.GoverBackend.identity.cache.repositories;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.util.List;

public interface IdentityCacheRepository extends KeyValueRepository<IdentityCacheEntity, String> {
    List<IdentityCacheEntity> findAllBySessionId(String sessionId);

    List<IdentityCacheEntity> findAllBySessionIdAndRelatedProcessNodeId(String sessionId, Integer relatedProcessNodeId);

    void deleteAllBySessionId(String sessionId);

    void deleteAllBySessionIdAndRelatedProcessNodeId(String sessionId, Integer relatedProcessNodeId);

    boolean existsBySessionId(String sessionId);
}
