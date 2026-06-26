package de.aivot.gover.backend.form.cache.repositories;

import de.aivot.gover.backend.form.cache.entities.FormLockCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface FormLockCacheRepository extends KeyValueRepository<FormLockCacheEntity, Integer> {
}
