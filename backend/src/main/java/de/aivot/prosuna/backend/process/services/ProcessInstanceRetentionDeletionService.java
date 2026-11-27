package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProcessInstanceRetentionDeletionService {
    private final ProcessInstanceService processInstanceService;
    private final ScopedAuditService auditService;

    public ProcessInstanceRetentionDeletionService(ProcessInstanceService processInstanceService,
                                                   AuditService auditService) {
        this.processInstanceService = processInstanceService;
        this.auditService = auditService.createScopedAuditService(ProcessInstanceRetentionDeletionService.class, "Prozesse");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDueProcessInstance(@Nonnull ProcessInstanceEntity processInstance) throws ResponseException {
        // Fail on audit persistence before deleting attachments from external storage.
        auditService.create()
                .withSystem()
                .withAuditAction(AuditAction.Delete, ProcessInstanceEntity.class, processInstance.getId(), "id", Map.of(
                        "id", processInstance.getId(),
                        "processDefinitionId", processInstance.getProcessId(),
                        "keepUntil", processInstance.getKeepUntil().toString()
                ))
                .withMessage(
                        "Der Vorgang mit der ID %s für den Prozess %s wurde nach Ablauf der Aufbewahrungsfrist durch das System gelöscht.",
                        StringUtils.quote(String.valueOf(processInstance.getId())),
                        StringUtils.quote(String.valueOf(processInstance.getProcessId()))
                )
                .logRequired();

        processInstanceService.deleteEntity(processInstance);
    }
}
