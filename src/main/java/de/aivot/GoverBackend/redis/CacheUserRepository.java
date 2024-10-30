package de.aivot.GoverBackend.redis;

import de.aivot.GoverBackend.models.entities.CacheUser;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface CacheUserRepository extends KeyValueRepository<CacheUser, String> {
}
