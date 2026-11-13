package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.cache.entities.AiUiElementChatSessionCacheEntity;
import de.aivot.prosuna.backend.ai.cache.repositories.AiUiElementChatSessionCacheRepository;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AiChatElementService {
    private final PermissionService permissionService;
    private final AiChatSessionRepository sessionRepository;
    private final AiUiElementChatSessionCacheRepository cacheRepository;
    private final JsonMapper jsonMapper;

    public AiChatElementService(@Nonnull PermissionService permissionService,
                                @Nonnull AiChatSessionRepository sessionRepository,
                                @Nonnull AiUiElementChatSessionCacheRepository cacheRepository,
                                @Nonnull JsonMapper jsonMapper) {
        this.permissionService = permissionService;
        this.sessionRepository = sessionRepository;
        this.cacheRepository = cacheRepository;
        this.jsonMapper = jsonMapper;
    }

    @Nullable
    public ElementType cacheCurrentElement(@Nullable String userId,
                                           @Nonnull String chatSessionId,
                                           @Nullable ElementType targetRootType,
                                           @Nullable BaseElement currentState) throws ResponseException {
        var session = requireOwnedSession(userId, chatSessionId);
        if (currentState == null) {
            // A chat request without an element must not switch into element editing mode.
            return null;
        }

        var effectiveRootType = targetRootType != null ? targetRootType : currentState.getType();

        var cache = new AiUiElementChatSessionCacheEntity()
                .setId(session.getSessionId())
                .setTargetRootType(effectiveRootType)
                .setCurrentElementJson(jsonMapper.writeValueAsString(currentState));
        cacheRepository.save(cache);
        return effectiveRootType;
    }

    @Nonnull
    public BaseElement getCurrentElement(@Nullable String userId,
                                         @Nonnull String chatSessionId) throws ResponseException {
        var session = requireOwnedSession(userId, chatSessionId);
        var cache = cacheRepository
                .findById(session.getSessionId())
                .orElseThrow(ResponseException::notFound);

        var currentElement = cache.getCurrentElementJson();
        if (currentElement == null) {
            throw ResponseException.notFound();
        }

        try {
            BaseElement element = jsonMapper.readerFor(BaseElement.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(currentElement);
            if (element == null) {
                throw new IllegalArgumentException("The cached element must be a JSON object.");
            }
            return element;
        } catch (JacksonException | IllegalArgumentException exception) {
            throw ResponseException.internalServerErrorWithDetails(
                    exception, "Der zwischengespeicherte Formularentwurf konnte nicht geladen werden.", null
            );
        }
    }

    @Nonnull
    private AiChatSessionEntity requireOwnedSession(@Nullable String userId,
                                                    @Nonnull String chatSessionId) throws ResponseException {
        permissionService.requireSystemPermission(userId, AiChatPermissionProvider.AI_CHAT_USE);
        assert userId != null : "userId must not be null after permission check";
        return sessionRepository.findByUserIdAndSessionId(userId, chatSessionId)
                .orElseThrow(ResponseException::notFound);
    }
}
