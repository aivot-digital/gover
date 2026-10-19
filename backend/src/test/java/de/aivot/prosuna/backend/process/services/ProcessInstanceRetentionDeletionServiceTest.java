package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.audit.entities.AuditLogEntity;
import de.aivot.prosuna.backend.audit.repositories.AuditLogRepository;
import de.aivot.prosuna.backend.audit.services.AuditLogService;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessInstanceRetentionDeletionServiceTest {
    private AuditLogRepository auditLogRepository;
    private ProcessInstanceService processInstanceService;
    private PlatformTransactionManager transactionManager;
    private ProcessInstanceRetentionDeletionService service;
    private ProcessInstanceEntity processInstance;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        processInstanceService = mock(ProcessInstanceService.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        var auditService = new AuditService(new AuditLogService(auditLogRepository, mock(UserRepository.class)));
        var target = new ProcessInstanceRetentionDeletionService(processInstanceService, auditService);
        var proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource()));
        service = (ProcessInstanceRetentionDeletionService) proxyFactory.getProxy();

        processInstance = new ProcessInstanceEntity()
                .setId(42L)
                .setProcessId(7)
                .setKeepUntil(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void deleteDueProcessInstance_AuditsSystemAndRetentionReasonBeforeDeletion() throws ResponseException {
        service.deleteDueProcessInstance(processInstance);

        var auditLog = ArgumentCaptor.forClass(AuditLogEntity.class);
        var order = inOrder(auditLogRepository, processInstanceService);
        order.verify(auditLogRepository).save(auditLog.capture());
        order.verify(auditLogRepository).flush();
        order.verify(processInstanceService).deleteEntity(processInstance);

        assertEquals("System", auditLog.getValue().getActorType());
        assertNull(auditLog.getValue().getActorId());
        assertEquals("Delete", auditLog.getValue().getTriggerType());
        assertEquals("ProcessInstanceEntity", auditLog.getValue().getEntityType());
        assertEquals("42", auditLog.getValue().getEntityRef());
        assertEquals("id", auditLog.getValue().getEntityRefType());
        assertEquals("Prozesse", auditLog.getValue().getModule());
        assertEquals("Der Vorgang mit der ID „42“ für den Prozess „7“ wurde nach Ablauf der Aufbewahrungsfrist durch das System gelöscht.",
                auditLog.getValue().getMessage());
        assertEquals(42L, auditLog.getValue().getMetadata().get("id"));
        assertEquals(7, auditLog.getValue().getMetadata().get("processDefinitionId"));
        assertEquals("2026-10-01T10:00:00Z", auditLog.getValue().getMetadata().get("keepUntil"));
        verify(transactionManager).commit(any(TransactionStatus.class));
    }

    @Test
    void deleteDueProcessInstance_DoesNotDeleteWhenAuditFlushFails() throws ResponseException {
        doThrow(new DataAccessResourceFailureException("audit unavailable"))
                .when(auditLogRepository).flush();

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.deleteDueProcessInstance(processInstance));

        verify(processInstanceService, never()).deleteEntity(any(ProcessInstanceEntity.class));
        verify(transactionManager).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any(TransactionStatus.class));
    }

    @Test
    void deleteDueProcessInstance_RollsBackAuditWhenDeletionFails() throws ResponseException {
        doThrow(ResponseException.internalServerError("Löschung fehlgeschlagen"))
                .when(processInstanceService).deleteEntity(processInstance);

        assertThrows(ResponseException.class,
                () -> service.deleteDueProcessInstance(processInstance));

        verify(auditLogRepository).flush();
        verify(transactionManager).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any(TransactionStatus.class));
    }
}
