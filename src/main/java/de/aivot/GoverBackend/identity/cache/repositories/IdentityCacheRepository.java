package de.aivot.GoverBackend.identity.cache.repositories;

import de.aivot.GoverBackend.identity.cache.entities.IdentityCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.util.UUID;

public interface IdentityCacheRepository extends KeyValueRepository<IdentityCacheEntity, UUID> {
}
