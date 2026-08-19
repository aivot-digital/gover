package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.utils.RandomUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessInstanceService implements EntityService<ProcessInstanceEntity, Long> {
    private static final int MAX_CASE_NUMBER_GENERATION_ATTEMPTS = 5;

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;
    private final ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessVersionService processVersionService;
    private final CaseNumberGeneratorService caseNumberGeneratorService;

    @Autowired
    public ProcessInstanceService(ProcessInstanceRepository processInstanceRepository,
                                  ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                  ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository,
                                  ProcessInstanceAttachmentService processInstanceAttachmentService,
                                  ProcessVersionService processVersionService,
                                  CaseNumberGeneratorService caseNumberGeneratorService) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.processInstanceAttachmentSetRepository = processInstanceAttachmentSetRepository;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processVersionService = processVersionService;
        this.caseNumberGeneratorService = caseNumberGeneratorService;
    }

    @Nonnull
    @Override
    public ProcessInstanceEntity create(@Nonnull ProcessInstanceEntity entity) throws ResponseException {
        entity.setId(null);
        entity.setAccessKey(RandomUtils.generateRandomString(ProcessInstanceEntity.ACCESS_KEY_LENGTH));

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(entity.getProcessId(), entity.getInitialProcessVersion()))
                .orElseThrow(ResponseException::badRequest);

        return createWithUniqueCaseNumber(entity, processVersion.getCaseNumberTemplate());
    }

    @Nullable
    @Override
    public Page<ProcessInstanceEntity> performList(@Nonnull Pageable pageable,
                                                   @Nullable Specification<ProcessInstanceEntity> specification,
                                                   @Nullable Filter<ProcessInstanceEntity> filter) throws ResponseException {
        return processInstanceRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processInstanceRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEntity> retrieve(@Nonnull Specification<ProcessInstanceEntity> specification) throws ResponseException {
        return processInstanceRepository.findOne(specification);
    }

    @Nonnull
    public Optional<ProcessInstanceEntity> retrieveByAccessKey(@Nonnull String accessKey) {
        return processInstanceRepository.findByAccessKey(accessKey);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processInstanceRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceEntity> specification) {
        return processInstanceRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceEntity performUpdate(@Nonnull Long id,
                                               @Nonnull ProcessInstanceEntity entity,
                                               @Nonnull ProcessInstanceEntity existingEntity) throws ResponseException {
        existingEntity.setStatus(entity.getStatus());
        existingEntity.setStatusOverride(entity.getStatusOverride());
        existingEntity.setAssignedUserId(entity.getAssignedUserId());
        existingEntity.setAssignedFileNumbers(entity.getAssignedFileNumbers());
        existingEntity.setIdentities(entity.getIdentities());
        existingEntity.setKeepUntil(entity.getKeepUntil());
        return processInstanceRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceEntity entity) throws ResponseException {
        var allAttachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceId(entity.getId());

        for (var attachment : allAttachments) {
            processInstanceAttachmentService
                    .deleteEntity(attachment);
        }

        processInstanceAttachmentSetRepository.deleteAll(
                processInstanceAttachmentSetRepository.findAllByProcessInstanceId(entity.getId())
        );

        processInstanceRepository.delete(entity);
    }

    @Nonnull
    public ProcessInstanceEntity save(@Nonnull ProcessInstanceEntity entity) {
        return processInstanceRepository.save(entity);
    }

    /**
     * The generator reads the current maximum increment before persisting, but the database unique constraint is still the last line of defense under concurrent instance creation.
     * Retrying here keeps the sequencing logic simple in the generator while still handling the race at the boundary where it actually happens.
     */
    @Nonnull
    private ProcessInstanceEntity createWithUniqueCaseNumber(@Nonnull ProcessInstanceEntity entity,
                                                             @Nullable String caseNumberTemplate) throws ResponseException {
        for (int attempt = 1; attempt <= MAX_CASE_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            entity.setId(null);
            entity.setCaseNumber(caseNumberGeneratorService.generateCaseNumber(caseNumberTemplate));

            try {
                return processInstanceRepository.saveAndFlush(entity);
            } catch (DataIntegrityViolationException e) {
                if (processInstanceRepository.existsByCaseNumber(entity.getCaseNumber())) {
                    if (attempt == MAX_CASE_NUMBER_GENERATION_ATTEMPTS) {
                        throw ResponseException.conflict("Es konnte kein eindeutiger Vorgangsschlüssel erzeugt werden. Bitte versuchen Sie es erneut.");
                    }
                    continue;
                }

                throw ResponseException.internalServerError("Die Prozessinstanz konnte nicht gespeichert werden.", e);
            }
        }

        throw ResponseException.conflict("Es konnte kein eindeutiger Vorgangsschlüssel erzeugt werden. Bitte versuchen Sie es erneut.");
    }
}
