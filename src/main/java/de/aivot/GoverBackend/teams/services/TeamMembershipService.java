package de.aivot.GoverBackend.teams.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import de.aivot.GoverBackend.teams.repositories.TeamMembershipRepository;
import de.aivot.GoverBackend.teams.repositories.TeamRepository;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TeamMembershipService implements EntityService<TeamMembershipEntity, Integer> {

    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Autowired
    public TeamMembershipService(TeamMembershipRepository teamMembershipRepository, TeamRepository teamRepository, UserRepository userRepository) {
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Nonnull
    @Override
    public TeamMembershipEntity create(@Nonnull TeamMembershipEntity entity) throws ResponseException {
        entity.setId(null);

        // Check if the team exists
        if (!teamRepository.existsById(entity.getTeamId()))  {
            throw ResponseException.badRequest("Das angegebene Team existiert nicht.");
        }

        // Check if user exists
        if (!userRepository.existsById(entity.getUserId()))  {
            throw ResponseException.badRequest("Der angegebene Benutzer existiert nicht.");
        }

        return teamMembershipRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<TeamMembershipEntity> performList(@Nonnull Pageable pageable,
                                                  @Nullable Specification<TeamMembershipEntity> specification,
                                                  @Nullable Filter<TeamMembershipEntity> filter) throws ResponseException {
        return teamMembershipRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<TeamMembershipEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return teamMembershipRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<TeamMembershipEntity> retrieve(@Nonnull Specification<TeamMembershipEntity> specification) throws ResponseException {
        return teamMembershipRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return teamMembershipRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<TeamMembershipEntity> specification) {
        return teamMembershipRepository.exists(specification);
    }

    @Nonnull
    @Override
    public TeamMembershipEntity performUpdate(@Nonnull Integer id,
                                              @Nonnull TeamMembershipEntity entity,
                                              @Nonnull TeamMembershipEntity existingEntity) throws ResponseException {
        return existingEntity;
    }

    @Override
    public void performDelete(@Nonnull TeamMembershipEntity entity) throws ResponseException {
        teamMembershipRepository.delete(entity);
    }
}
