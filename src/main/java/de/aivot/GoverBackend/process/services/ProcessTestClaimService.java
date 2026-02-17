package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessTestClaimService implements EntityService<ProcessTestClaimEntity, Integer> {

    private final ProcessTestClaimRepository repository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceService processInstanceService;

    @Autowired
    public ProcessTestClaimService(ProcessTestClaimRepository repository,
                                   ProcessInstanceRepository processInstanceRepository,
                                   ProcessInstanceService processInstanceService) {
        this.repository = repository;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceService = processInstanceService;
    }

    @Nonnull
    @Override
    public ProcessTestClaimEntity create(@Nonnull ProcessTestClaimEntity entity) throws ResponseException {
        entity.setId(null);
        entity.setAccessKey(StringUtils.randomString(64));
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessTestClaimEntity> performList(@Nonnull Pageable pageable,
                                                    @Nullable Specification<ProcessTestClaimEntity> specification,
                                                    @Nullable Filter<ProcessTestClaimEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessTestClaimEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessTestClaimEntity> retrieve(@Nonnull Specification<ProcessTestClaimEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessTestClaimEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessTestClaimEntity performUpdate(@Nonnull Integer id,
                                                @Nonnull ProcessTestClaimEntity entity,
                                                @Nonnull ProcessTestClaimEntity existingEntity) throws ResponseException {
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessTestClaimEntity entity) throws ResponseException {
        var allInstances = processInstanceRepository
                .findAllByCreatedForTestClaimId(entity.getId());

        for (var instance : allInstances) {
            processInstanceService
                    .deleteEntity(instance);
        }

        repository.delete(entity);
    }
}
