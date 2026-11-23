package de.aivot.prosuna.backend.user.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import de.aivot.prosuna.backend.user.models.DeputyDateRange;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserDeputyRepository;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserDeputyService implements EntityService<UserDeputyEntity, Integer> {
    private final UserDeputyRepository repository;
    private final UserRepository users;

    public UserDeputyService(UserDeputyRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @Nonnull
    @Override
    public UserDeputyEntity create(@Nonnull UserDeputyEntity entity) throws ResponseException {
        validateDateRange(entity);
        entity.setId(null);
        validateNoCycle(entity);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<UserDeputyEntity> performList(@Nonnull Pageable pageable,
                                              @Nullable Specification<UserDeputyEntity> specification,
                                              @Nullable Filter<UserDeputyEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<UserDeputyEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<UserDeputyEntity> retrieve(@Nonnull Specification<UserDeputyEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<UserDeputyEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public UserDeputyEntity performUpdate(@Nonnull Integer id,
                                          @Nonnull UserDeputyEntity entity,
                                          @Nonnull UserDeputyEntity existingEntity) throws ResponseException {
        // Updating a deputy relation only changes its validity period. Changing either user
        // would create a different relation.
        existingEntity
                .setFromDate(entity.getFromDate())
                .setUntilDate(entity.getUntilDate());

        validateDateRange(existingEntity);
        validateNoCycle(existingEntity);
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull UserDeputyEntity entity) throws ResponseException {
        repository.delete(entity);
    }

    /**
     * Users the deputy currently represents, directly or through further deputies.
     */
    @Nonnull
    public List<String> findActivelyRepresentedUserIds(@Nonnull String deputyUserId) {
        return repository.findActivelyRepresentedUserIds(deputyUserId);
    }

    /**
     * Users currently representing each of the given users, directly or through further deputies, sorted by name.
     * Users without an active deputy are missing from the result.
     */
    @Nonnull
    public Map<String, List<UserEntity>> findActiveDeputies(@Nonnull Collection<String> representedUserIds) {
        if (representedUserIds.isEmpty()) return Map.of();
        var relations = repository.findActiveDeputies(representedUserIds);
        var deputiesById = users.findAllById(relations.stream()
                        .map(UserDeputyRepository.ActiveDeputyProjection::getDeputyUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        var result = new HashMap<String, List<UserEntity>>();
        for (var relation : relations) {
            var deputy = deputiesById.get(relation.getDeputyUserId());
            if (deputy != null) result.computeIfAbsent(relation.getOriginalUserId(), id -> new ArrayList<>()).add(deputy);
        }
        result.values().forEach(deputies -> deputies.sort(Comparator.comparing(UserEntity::getFullName, String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    /**
     * Whether the deputy currently represents the original user, directly or through further deputies.
     */
    public boolean isActiveDeputyFor(@Nonnull String deputyUserId, @Nonnull String originalUserId) {
        return findActivelyRepresentedUserIds(deputyUserId).contains(originalUserId);
    }

    private void validateDateRange(@Nonnull UserDeputyEntity entity) throws ResponseException {
        if (!DeputyDateRange.isValid(entity.getFromDate(), entity.getUntilDate())) {
            throw ResponseException.badRequest("Das Ende der Stellvertretung darf nicht vor ihrem Beginn liegen.");
        }
    }

    private void validateNoCycle(@Nonnull UserDeputyEntity entity) throws ResponseException {
        if (repository.createsCycle(
                entity.getOriginalUserId(),
                entity.getDeputyUserId(),
                entity.getFromDate(),
                entity.getUntilDate(),
                entity.getId()
        )) {
            throw ResponseException.conflict("Im gewählten Zeitraum würden sich die beteiligten Personen direkt oder "
                    + "über weitere Stellvertretungen gegenseitig vertreten. Wählen Sie einen anderen Zeitraum.");
        }
    }
}
