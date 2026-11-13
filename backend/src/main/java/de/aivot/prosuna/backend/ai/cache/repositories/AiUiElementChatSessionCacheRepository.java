package de.aivot.prosuna.backend.ai.cache.repositories;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.util.UUID;

public interface AiUiElementChatSessionCacheRepository extends KeyValueRepository<AiUiElementChatSessionCacheEntity, String> {
}
