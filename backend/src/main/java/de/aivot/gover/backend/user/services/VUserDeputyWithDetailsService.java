package de.aivot.gover.backend.user.services;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.ReadEntityService;
import de.aivot.gover.backend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.gover.backend.user.models.DeputyDateRange;
import de.aivot.gover.backend.user.repositories.VUserDeputyWithDetailsRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VUserDeputyWithDetailsService implements ReadEntityService<VUserDeputyWithDetailsEntity, Integer> {
    private final VUserDeputyWithDetailsRepository repository;
    private final BusinessTime businessTime;

    public VUserDeputyWithDetailsService(
            VUserDeputyWithDetailsRepository repository,
            BusinessTime businessTime
    ) {
        this.repository = repository;
        this.businessTime = businessTime;
    }


    @Nullable
    @Override
    public Page<VUserDeputyWithDetailsEntity> performList(@Nonnull Pageable pageable,
                                                          @Nullable Specification<VUserDeputyWithDetailsEntity> specification,
                                                          @Nullable Filter<VUserDeputyWithDetailsEntity> filter) throws ResponseException {
        return repository
                .findAll(specification, pageable)
                .map(this::applyActiveState);
    }

    @Nonnull
    @Override
    public Optional<VUserDeputyWithDetailsEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id).map(this::applyActiveState);
    }

    @Nonnull
    @Override
    public Optional<VUserDeputyWithDetailsEntity> retrieve(@Nonnull Specification<VUserDeputyWithDetailsEntity> specification) throws ResponseException {
        return repository.findOne(specification).map(this::applyActiveState);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VUserDeputyWithDetailsEntity> specification) {
        return repository.exists(specification);
    }

    private VUserDeputyWithDetailsEntity applyActiveState(VUserDeputyWithDetailsEntity entity) {
        // The database view still calculates active for SQL-side filtering. Re-evaluate
        // the returned value through the shared inclusive range rule and BusinessTime
        // instead of exposing PostgreSQL session-time semantics at the API boundary.
        return entity.setActive(
                DeputyDateRange.isActive(
                        entity.getFromDate(),
                        entity.getUntilDate(),
                        businessTime.today()
                )
        );
    }
}
