package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.userRoles.entities.VTeamUserRoleAssignmentsWithDetailsEntity;
import de.aivot.GoverBackend.userRoles.repositories.VTeamUserRoleAssignmentsWithDetailsRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VTeamUserRoleAssignmentsWithDetailsService implements ReadEntityService<VTeamUserRoleAssignmentsWithDetailsEntity, Integer> {
    private final VTeamUserRoleAssignmentsWithDetailsRepository vOrgUserRoleAssignmentsWithDetailsRepository;

    @Autowired
    public VTeamUserRoleAssignmentsWithDetailsService(VTeamUserRoleAssignmentsWithDetailsRepository vOrgUserRoleAssignmentsWithDetailsRepository) {
        this.vOrgUserRoleAssignmentsWithDetailsRepository = vOrgUserRoleAssignmentsWithDetailsRepository;
    }

    @Nullable
    @Override
    public Page<VTeamUserRoleAssignmentsWithDetailsEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<VTeamUserRoleAssignmentsWithDetailsEntity> specification, @Nullable Filter<VTeamUserRoleAssignmentsWithDetailsEntity> filter) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VTeamUserRoleAssignmentsWithDetailsEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VTeamUserRoleAssignmentsWithDetailsEntity> retrieve(@Nonnull Specification<VTeamUserRoleAssignmentsWithDetailsEntity> specification) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vOrgUserRoleAssignmentsWithDetailsRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VTeamUserRoleAssignmentsWithDetailsEntity> specification) {
        return vOrgUserRoleAssignmentsWithDetailsRepository.count(specification) > 0;
    }
}
