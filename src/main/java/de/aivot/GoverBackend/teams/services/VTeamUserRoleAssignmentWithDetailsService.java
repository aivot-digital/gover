package de.aivot.GoverBackend.teams.services;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.teams.entities.VTeamUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.teams.repositories.VTeamUserRoleAssignmentWithDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class VTeamUserRoleAssignmentWithDetailsService implements ReadEntityService<VTeamUserRoleAssignmentWithDetailsEntity, Integer> {
    private final VTeamUserRoleAssignmentWithDetailsRepository vTeamMembershipWithDetailsRepository;

    @Autowired
    public VTeamUserRoleAssignmentWithDetailsService(VTeamUserRoleAssignmentWithDetailsRepository vTeamMembershipWithDetailsRepository) {
        this.vTeamMembershipWithDetailsRepository = vTeamMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VTeamUserRoleAssignmentWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VTeamUserRoleAssignmentWithDetailsEntity> specification,
            Filter<VTeamUserRoleAssignmentWithDetailsEntity> filter) {
        return vTeamMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VTeamUserRoleAssignmentWithDetailsEntity> retrieve(@Nonnull Integer id) {
        return vTeamMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VTeamUserRoleAssignmentWithDetailsEntity> retrieve(
            @Nonnull Specification<VTeamUserRoleAssignmentWithDetailsEntity> specification
    ) {
        return vTeamMembershipWithDetailsRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vTeamMembershipWithDetailsRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VTeamUserRoleAssignmentWithDetailsEntity> specification
    ) {
        return vTeamMembershipWithDetailsRepository
                .exists(specification);
    }
}
