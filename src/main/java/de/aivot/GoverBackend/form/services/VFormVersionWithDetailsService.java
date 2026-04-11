package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsRepository;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

@Service
public class VFormVersionWithDetailsService implements ReadEntityService<VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntityId> {
    private final VFormVersionWithDetailsRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public VFormVersionWithDetailsService(
            VFormVersionWithDetailsRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<VFormVersionWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VFormVersionWithDetailsEntity> specification,
            @Nullable Filter<VFormVersionWithDetailsEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VFormVersionWithDetailsEntity> retrieve(@Nonnull VFormVersionWithDetailsEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Transactional
    public Optional<VFormVersionWithDetailsEntity> retrieveFresh(@Nonnull VFormVersionWithDetailsEntityId id) {
        entityManager.flush();
        entityManager.clear();
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VFormVersionWithDetailsEntity> retrieve(@Nonnull Specification<VFormVersionWithDetailsEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull VFormVersionWithDetailsEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VFormVersionWithDetailsEntity> specification) {
        return repository.exists(specification);
    }

    public Optional<VFormVersionWithDetailsEntity> findBySlugAndVersion(String slug, Integer version) {
        return repository.findBySlugAndVersion(slug, version);
    }
}
