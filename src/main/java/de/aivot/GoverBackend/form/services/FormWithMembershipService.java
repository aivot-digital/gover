package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.entities.FormWithMembershipId;
import de.aivot.GoverBackend.form.repositories.FormWithMembershipRepository;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class FormWithMembershipService implements ReadEntityService<FormWithMembership, FormWithMembershipId> {
    private final FormWithMembershipRepository repository;

    @Autowired
    public FormWithMembershipService(
            FormWithMembershipRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<FormWithMembership> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormWithMembership> specification,
            @Nullable Filter<FormWithMembership> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<FormWithMembership> retrieve(@Nonnull FormWithMembershipId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormWithMembership> retrieve(@Nonnull Specification<FormWithMembership> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormWithMembershipId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormWithMembership> specification) {
        return repository.exists(specification);
    }
}
