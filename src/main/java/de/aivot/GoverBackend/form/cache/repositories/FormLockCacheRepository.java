package de.aivot.GoverBackend.form.cache.repositories;

import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface FormLockCacheRepository extends KeyValueRepository<FormLockCacheEntity, Integer> {
}
