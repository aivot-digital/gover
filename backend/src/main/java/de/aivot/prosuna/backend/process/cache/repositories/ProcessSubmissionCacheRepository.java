package de.aivot.prosuna.backend.process.cache.repositories;

import de.aivot.prosuna.backend.process.cache.entities.ProcessSubmissionCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface ProcessSubmissionCacheRepository extends KeyValueRepository<ProcessSubmissionCacheEntity, String> {
}
