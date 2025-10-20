package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.FormWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormWithMembershipEntityId;
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
public class FormWithMembershipService implements ReadEntityService<FormWithMembershipEntity, FormWithMembershipEntityId> {
    private final FormWithMembershipRepository repository;

    @Autowired
    public FormWithMembershipService(
            FormWithMembershipRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<FormWithMembershipEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormWithMembershipEntity> specification,
            @Nullable Filter<FormWithMembershipEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<FormWithMembershipEntity> retrieve(@Nonnull FormWithMembershipEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormWithMembershipEntity> retrieve(@Nonnull Specification<FormWithMembershipEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormWithMembershipEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormWithMembershipEntity> specification) {
        return repository.exists(specification);
    }
}
