package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.CaseNumberType;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.utils.DatabaseConstraintUtils;
import de.aivot.prosuna.backend.utils.RandomUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessInstanceService implements EntityService<ProcessInstanceEntity, Long> {
    private static final int MAX_IDENTIFIER_GENERATION_ATTEMPTS = 5;

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;
    private final ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessVersionService processVersionService;
    private final CaseNumberGeneratorService caseNumberGeneratorService;
    private final TransactionTemplate creationTransaction;

    @Autowired
    public ProcessInstanceService(ProcessInstanceRepository processInstanceRepository,
                                  ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                  ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository,
                                  ProcessInstanceAttachmentService processInstanceAttachmentService,
                                  ProcessVersionService processVersionService,
                                  CaseNumberGeneratorService caseNumberGeneratorService,
                                  @Nonnull PlatformTransactionManager transactionManager) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.processInstanceAttachmentSetRepository = processInstanceAttachmentSetRepository;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processVersionService = processVersionService;
        this.caseNumberGeneratorService = caseNumberGeneratorService;
        this.creationTransaction = new TransactionTemplate(transactionManager);
        // A failed flush invalidates its transaction. Complete that rollback before generating new identifiers.
        this.creationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Nonnull
    @Override
    public ProcessInstanceEntity create(@Nonnull ProcessInstanceEntity entity) throws ResponseException {
        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(entity.getProcessId(), entity.getInitialProcessVersion()))
                .orElseThrow(ResponseException::badRequest);

        return createWithUniqueIdentifiers(entity, processVersion.getCaseNumberType(), processVersion.getCaseNumberTemplate());
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

    @Nonnull
    public Optional<ProcessInstanceEntity> retrieveByInboundReference(@Nonnull String inboundReference) {
        return processInstanceRepository.findByInboundReference(inboundReference);
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
     * Database constraints arbitrate collisions, including concurrent creation with incrementing case numbers.
     * Only generated identifiers may be replaced; an existing inbound reference must still reject creation.
     */
    @Nonnull
    private ProcessInstanceEntity createWithUniqueIdentifiers(@Nonnull ProcessInstanceEntity entity,
                                                             @Nonnull CaseNumberType caseNumberType,
                                                             @Nullable String caseNumberTemplate) throws ResponseException {
        for (int attempt = 1; attempt <= MAX_IDENTIFIER_GENERATION_ATTEMPTS; attempt++) {
            entity.setId(null);
            entity.setAccessKey(RandomUtils.generateRandomString(ProcessInstanceEntity.ACCESS_KEY_LENGTH));
            entity.setCaseNumber(caseNumberGeneratorService.generateCaseNumber(caseNumberType, caseNumberTemplate));

            try {
                return Objects.requireNonNull(creationTransaction.execute(
                        status -> processInstanceRepository.saveAndFlush(entity)
                ));
            } catch (DataIntegrityViolationException e) {
                if (DatabaseConstraintUtils.isUniqueViolation(e, "process_instances_inbound_reference_unique")) {
                    throw ResponseException.conflict("Für diese externe Eingangsreferenz existiert bereits ein Vorgang.");
                }

                if (DatabaseConstraintUtils.isUniqueViolation(e, "process_instances_case_number_key")) {
                    if (attempt == MAX_IDENTIFIER_GENERATION_ATTEMPTS) {
                        throw ResponseException.conflict("Es konnte keine eindeutige Vorgangskennung erzeugt werden. Bitte versuchen Sie es erneut.");
                    }
                    continue;
                }

                if (DatabaseConstraintUtils.isUniqueViolation(e, "process_instances_access_key_key")) {
                    if (attempt == MAX_IDENTIFIER_GENERATION_ATTEMPTS) {
                        throw ResponseException.internalServerErrorWithDetails(e,
                                "Der Vorgang konnte nicht erstellt werden.", "Bitte versuchen Sie es erneut.");
                    }
                    continue;
                }

                throw ResponseException.internalServerErrorWithDetails(e,
                        "Der Vorgang konnte nicht gespeichert werden.", "Beim Speichern ist ein technischer Fehler aufgetreten.");
            }
        }

        throw new IllegalStateException("Identifier generation attempts exhausted");
    }
}
