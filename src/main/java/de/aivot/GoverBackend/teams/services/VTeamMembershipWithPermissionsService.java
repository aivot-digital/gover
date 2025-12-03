package de.aivot.GoverBackend.teams.services;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.teams.repositories.VTeamMembershipWithPermissionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

@Service
public class VTeamMembershipWithPermissionsService implements ReadEntityService<VTeamMembershipWithPermissionsEntity, Integer> {
    private final VTeamMembershipWithPermissionsRepository VTeamMembershipWithDetailsRepository;

    @Autowired
    public VTeamMembershipWithPermissionsService(VTeamMembershipWithPermissionsRepository VTeamMembershipWithDetailsRepository) {
        this.VTeamMembershipWithDetailsRepository = VTeamMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VTeamMembershipWithPermissionsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VTeamMembershipWithPermissionsEntity> specification,
            Filter<VTeamMembershipWithPermissionsEntity> filter) {
        return VTeamMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VTeamMembershipWithPermissionsEntity> retrieve(@Nonnull Integer id) {
        return VTeamMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VTeamMembershipWithPermissionsEntity> retrieve(
            @Nonnull Specification<VTeamMembershipWithPermissionsEntity> specification
    ) {
        return VTeamMembershipWithDetailsRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return VTeamMembershipWithDetailsRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VTeamMembershipWithPermissionsEntity> specification
    ) {
        return VTeamMembershipWithDetailsRepository
                .exists(specification);
    }
}
