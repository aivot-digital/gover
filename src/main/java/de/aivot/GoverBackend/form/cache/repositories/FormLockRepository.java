package de.aivot.GoverBackend.form.cache.repositories;

import de.aivot.GoverBackend.form.cache.entities.FormLock;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface FormLockRepository extends KeyValueRepository<FormLock, Integer> {
}
