package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.ReadEntityService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class ProcessInstanceAttachmentSetService implements ReadEntityService<ProcessInstanceAttachmentSetEntity, Integer> {
    private final ProcessInstanceAttachmentSetRepository repository;

    public ProcessInstanceAttachmentSetService(ProcessInstanceAttachmentSetRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public ProcessInstanceAttachmentSetEntity create(@Nonnull ProcessInstanceAttachmentSetEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAttachmentSetEntity> performList(@Nonnull Pageable pageable,
                                                                @Nullable Specification<ProcessInstanceAttachmentSetEntity> specification,
                                                                @Nullable Filter<ProcessInstanceAttachmentSetEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentSetEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentSetEntity> retrieve(@Nonnull Specification<ProcessInstanceAttachmentSetEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Nonnull
    public List<ProcessInstanceAttachmentSetEntity> findAllByProcessInstanceIdAndDataKey(@Nonnull Long processInstanceId,
                                                                                         @Nonnull String dataKey) {
        return repository.findAllByProcessInstanceIdAndDataKey(processInstanceId, dataKey);
    }

    @Nonnull
    public Optional<ProcessInstanceAttachmentSetEntity> retrieveLatestByProcessInstanceIdAndTaskIdAndDataKey(@Nonnull Long processInstanceId,
                                                                                                             @Nonnull Long processInstanceTaskId,
                                                                                                             @Nonnull String dataKey) {
        return repository.findFirstByProcessInstanceIdAndProcessInstanceTaskIdAndDataKeyOrderByIdDesc(
                processInstanceId,
                processInstanceTaskId,
                dataKey
        );
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAttachmentSetEntity> specification) {
        return repository.exists(specification);
    }
}
