package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.MembershipWithUserEntity;
import de.aivot.GoverBackend.department.entities.MembershipWithUserEntityId;
import de.aivot.GoverBackend.department.repositories.MembershipWithUserRepository;
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
public class MembershipWithUserService implements ReadEntityService<MembershipWithUserEntity, MembershipWithUserEntityId> {
    private final MembershipWithUserRepository repository;

    @Autowired
    public MembershipWithUserService(MembershipWithUserRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<MembershipWithUserEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<MembershipWithUserEntity> specification,
            Filter<MembershipWithUserEntity> filter) {
        return repository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<MembershipWithUserEntity> retrieve(@Nonnull MembershipWithUserEntityId id) {
        return repository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<MembershipWithUserEntity> retrieve(
            @Nonnull Specification<MembershipWithUserEntity> specification
    ) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull MembershipWithUserEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<MembershipWithUserEntity> specification
    ) {
        return repository.exists(specification);
    }
}
