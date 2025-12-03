package de.aivot.GoverBackend.teams.services;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.teams.repositories.VTeamMembershipWithDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

@Service
public class VTeamMembershipWithDetailsService implements ReadEntityService<VTeamMembershipWithDetailsEntity, Integer> {
    private final VTeamMembershipWithDetailsRepository vTeamMembershipWithDetailsRepository;

    @Autowired
    public VTeamMembershipWithDetailsService(VTeamMembershipWithDetailsRepository vTeamMembershipWithDetailsRepository) {
        this.vTeamMembershipWithDetailsRepository = vTeamMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VTeamMembershipWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VTeamMembershipWithDetailsEntity> specification,
            Filter<VTeamMembershipWithDetailsEntity> filter) {
        return vTeamMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VTeamMembershipWithDetailsEntity> retrieve(@Nonnull Integer id) {
        return vTeamMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VTeamMembershipWithDetailsEntity> retrieve(
            @Nonnull Specification<VTeamMembershipWithDetailsEntity> specification
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
            @Nonnull Specification<VTeamMembershipWithDetailsEntity> specification
    ) {
        return vTeamMembershipWithDetailsRepository
                .exists(specification);
    }
}
