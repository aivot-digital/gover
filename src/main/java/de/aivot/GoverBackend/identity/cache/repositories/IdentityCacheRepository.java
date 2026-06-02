package de.aivot.GoverBackend.identity.cache.repositories;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.util.List;

public interface IdentityCacheRepository extends KeyValueRepository<IdentityCacheEntity, String> {
    List<IdentityCacheEntity> findAllBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);
}
