package de.aivot.GoverBackend.redis;

import de.aivot.GoverBackend.models.entities.FormLock;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.CrudRepository;

public interface FormLockRepository extends KeyValueRepository<FormLock, Integer> {
}
