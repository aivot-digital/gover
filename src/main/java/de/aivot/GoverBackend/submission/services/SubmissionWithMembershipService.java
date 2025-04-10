package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembershipId;
import de.aivot.GoverBackend.submission.repositories.SubmissionWithMembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class SubmissionWithMembershipService implements ReadEntityService<SubmissionWithMembership, SubmissionWithMembershipId> {
    private final SubmissionWithMembershipRepository repository;

    @Autowired
    public SubmissionWithMembershipService(
            SubmissionWithMembershipRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<SubmissionWithMembership> performList(@Nonnull Pageable pageable, @Nullable Specification<SubmissionWithMembership> specification, Filter<SubmissionWithMembership> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<SubmissionWithMembership> retrieve(@Nonnull SubmissionWithMembershipId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<SubmissionWithMembership> retrieve(@Nonnull Specification<SubmissionWithMembership> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull SubmissionWithMembershipId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<SubmissionWithMembership> specification) {
        return repository.exists(specification);
    }
}
