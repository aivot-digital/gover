package de.aivot.GoverBackend.user.cache.repositories;

import de.aivot.GoverBackend.user.cache.entities.UserCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface UserCacheRepository extends KeyValueRepository<UserCacheEntity, String> {
}
