package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.userRoles.entities.VOrgUserRoleAssignmentsWithDetailsEntity;
import de.aivot.GoverBackend.userRoles.repositories.VOrgUserRoleAssignmentsWithDetailsRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VOrgUserRoleAssignmentsWithDetailsService implements ReadEntityService<VOrgUserRoleAssignmentsWithDetailsEntity, Integer> {
    private final VOrgUserRoleAssignmentsWithDetailsRepository vOrgUserRoleAssignmentsWithDetailsRepository;

    @Autowired
    public VOrgUserRoleAssignmentsWithDetailsService(VOrgUserRoleAssignmentsWithDetailsRepository vOrgUserRoleAssignmentsWithDetailsRepository) {
        this.vOrgUserRoleAssignmentsWithDetailsRepository = vOrgUserRoleAssignmentsWithDetailsRepository;
    }

    @Nullable
    @Override
    public Page<VOrgUserRoleAssignmentsWithDetailsEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<VOrgUserRoleAssignmentsWithDetailsEntity> specification, @Nullable Filter<VOrgUserRoleAssignmentsWithDetailsEntity> filter) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VOrgUserRoleAssignmentsWithDetailsEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VOrgUserRoleAssignmentsWithDetailsEntity> retrieve(@Nonnull Specification<VOrgUserRoleAssignmentsWithDetailsEntity> specification) throws ResponseException {
        return vOrgUserRoleAssignmentsWithDetailsRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vOrgUserRoleAssignmentsWithDetailsRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VOrgUserRoleAssignmentsWithDetailsEntity> specification) {
        return vOrgUserRoleAssignmentsWithDetailsRepository.count(specification) > 0;
    }
}
