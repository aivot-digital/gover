package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.user.repositories.UserDeputyRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserDeputyService implements EntityService<UserDeputyEntity, Integer> {
    private final UserDeputyRepository repository;

    public UserDeputyService(UserDeputyRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public UserDeputyEntity create(@Nonnull UserDeputyEntity entity) throws ResponseException {
        validateDateRange(entity);
        entity.setId(null);
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
        // No fields to update
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull UserDeputyEntity entity) throws ResponseException {
        repository.delete(entity);
    }

    private void validateDateRange(@Nonnull UserDeputyEntity entity) throws ResponseException {
        var untilTimestamp = entity.getUntilTimestamp();
        if (untilTimestamp == null) {
            return;
        }

        LocalDateTime fromTimestamp = entity.getFromTimestamp();
        if (!fromTimestamp.isBefore(untilTimestamp)) {
            throw ResponseException.badRequest("Das Ende der Vertretung muss nach dem Start der Vertretung liegen.");
        }
    }
}
