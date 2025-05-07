package de.aivot.GoverBackend.identity.cache.repositories;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface IdentityCacheRepository extends KeyValueRepository<IdentityCacheEntity, String> {
}
