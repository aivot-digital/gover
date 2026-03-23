package de.aivot.GoverBackend.teams.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.repositories.TeamRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeamService implements EntityService<TeamEntity, Integer> {

    private final TeamRepository teamRepository;

    @Autowired
    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Nonnull
    @Override
    public TeamEntity create(@Nonnull TeamEntity entity) throws ResponseException {
        entity.setId(null);

        return teamRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<TeamEntity> performList(@Nonnull Pageable pageable,
                                        @Nullable Specification<TeamEntity> specification,
                                        @Nullable Filter<TeamEntity> filter) throws ResponseException {
        return teamRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<TeamEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return teamRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<TeamEntity> retrieve(@Nonnull Specification<TeamEntity> specification) throws ResponseException {
        return teamRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return teamRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<TeamEntity> specification) {
        return teamRepository.exists(specification);
    }

    @Nonnull
    @Override
    public TeamEntity performUpdate(@Nonnull Integer id,
                                    @Nonnull TeamEntity entity,
                                    @Nonnull TeamEntity existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());

        return teamRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull TeamEntity entity) throws ResponseException {
        teamRepository.delete(entity);
    }
}
