package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.CaseNumberType;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessInstanceServiceTest {
    private ProcessInstanceRepository repository;
    private CaseNumberGeneratorService generator;
    private PlatformTransactionManager transactionManager;
    private ProcessInstanceService service;
    private ProcessVersionEntity version;
    private ProcessInstanceEntity instance;

    @BeforeEach
    void setUp() throws ResponseException {
        repository = mock(ProcessInstanceRepository.class);
        generator = mock(CaseNumberGeneratorService.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            return new SimpleTransactionStatus();
        });
        var versions = mock(ProcessVersionService.class);
        version = new ProcessVersionEntity()
                .setProcessId(7)
                .setProcessVersion(1)
                .setCaseNumberType(CaseNumberType.CROCKFORD_BASE32);
        when(versions.retrieve(any(ProcessVersionEntityId.class))).thenReturn(Optional.of(version));
        service = new ProcessInstanceService(repository,
                mock(ProcessInstanceAttachmentRepository.class), mock(ProcessInstanceAttachmentSetRepository.class),
                mock(ProcessInstanceAttachmentService.class), versions, generator, transactionManager);
        instance = new ProcessInstanceEntity().setProcessId(7).setInitialProcessVersion(1);
        when(generator.generateCaseNumber(CaseNumberType.CROCKFORD_BASE32, null)).thenReturn("7K0M-9X1Q-4R8T");
    }

    @ParameterizedTest
    @EnumSource(CaseNumberType.class)
    void create_RetriesWhenTheGeneratedCaseNumberCollides(CaseNumberType type) throws ResponseException {
        var template = type == CaseNumberType.TEMPLATE ? "AZ-%YYY-%I(4)" : null;
        version.setCaseNumberType(type).setCaseNumberTemplate(template);
        when(generator.generateCaseNumber(type, template)).thenReturn("AZ-2026-0001", "AZ-2026-0002");
        when(repository.saveAndFlush(instance))
                .thenThrow(uniqueViolation("process_instances_case_number_key"))
                .thenReturn(instance);

        var result = service.create(instance);

        verify(generator, times(2)).generateCaseNumber(type, template);
        verify(repository, times(2)).saveAndFlush(instance);
        verify(repository, never()).existsByCaseNumber(any());
        assertEquals("AZ-2026-0002", result.getCaseNumber());
        assertNotNull(result.getAccessKey());
        assertRollbackPrecedesRetry();
    }

    @Test
    void create_RetriesWithANewAccessKeyAndResetsTheFailedInsertId() throws ResponseException {
        var attemptedKeys = new ArrayList<String>();
        when(repository.saveAndFlush(instance)).thenAnswer(invocation -> {
            assertNull(instance.getId());
            attemptedKeys.add(instance.getAccessKey());
            instance.setId(42L);
            if (attemptedKeys.size() == 1) {
                throw uniqueViolation("process_instances_access_key_key");
            }
            return instance;
        });

        var result = service.create(instance);

        assertSame(instance, result);
        assertEquals(2, attemptedKeys.size());
        assertNotEquals(attemptedKeys.getFirst(), attemptedKeys.getLast());
        assertRollbackPrecedesRetry();
    }

    @ParameterizedTest
    @ValueSource(strings = {"process_instances_case_number_key", "process_instances_access_key_key"})
    void create_StopsAfterRepeatedIdentifierCollisions(String constraintName) {
        when(repository.saveAndFlush(instance)).thenThrow(uniqueViolation(constraintName));

        var failure = assertThrows(ResponseException.class, () -> service.create(instance));

        if (constraintName.equals("process_instances_case_number_key")) {
            assertEquals(HttpStatus.CONFLICT, failure.getStatus());
            assertEquals("Es konnte keine eindeutige Vorgangskennung erzeugt werden. Bitte versuchen Sie es erneut.", failure.getTitle());
        } else {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatus());
            assertEquals("Der Vorgang konnte nicht erstellt werden.", failure.getTitle());
            assertEquals("Bitte versuchen Sie es erneut.", failure.getDetails());
        }
        verify(repository, times(5)).saveAndFlush(instance);
        verify(transactionManager, times(5)).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void create_RejectsADuplicateInboundReferenceWithoutRetry() {
        instance.setInboundReference("external-reference");
        when(repository.saveAndFlush(instance)).thenThrow(uniqueViolation("process_instances_inbound_reference_unique"));

        var failure = assertThrows(ResponseException.class, () -> service.create(instance));

        assertEquals(HttpStatus.CONFLICT, failure.getStatus());
        assertEquals("Für diese externe Eingangsreferenz existiert bereits ein Vorgang.", failure.getTitle());
        verify(repository).saveAndFlush(instance);
        verify(repository, never()).existsByInboundReference(any());
        verify(transactionManager).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void create_DoesNotRetryOrExposeOtherDatabaseFailures() {
        var databaseFailure = new DataIntegrityViolationException("private database details",
                new ConstraintViolationException("foreign key", new SQLException("foreign key", "23503"),
                        "process_instances_process_id_fkey"));
        when(repository.saveAndFlush(instance)).thenThrow(databaseFailure);

        var failure = assertThrows(ResponseException.class, () -> service.create(instance));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatus());
        assertEquals("Der Vorgang konnte nicht gespeichert werden.", failure.getTitle());
        assertEquals("Beim Speichern ist ein technischer Fehler aufgetreten.", failure.getDetails());
        assertSame(databaseFailure, failure.getCause());
        verify(repository).saveAndFlush(instance);
        verify(transactionManager).rollback(any(TransactionStatus.class));
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void create_UsesTheExplicitUuidSetting() throws ResponseException {
        version.setCaseNumberType(CaseNumberType.UUID_V4);
        when(generator.generateCaseNumber(CaseNumberType.UUID_V4, null)).thenReturn("generated-uuid");
        when(repository.saveAndFlush(instance)).thenReturn(instance);

        var result = service.create(instance);

        verify(generator).generateCaseNumber(CaseNumberType.UUID_V4, null);
        verify(repository).saveAndFlush(instance);
        assertEquals("generated-uuid", result.getCaseNumber());
    }

    private void assertRollbackPrecedesRetry() {
        var order = inOrder(repository, transactionManager);
        order.verify(repository).saveAndFlush(instance);
        order.verify(transactionManager).rollback(any(TransactionStatus.class));
        order.verify(repository).saveAndFlush(instance);
        order.verify(transactionManager).commit(any(TransactionStatus.class));
    }

    private static DataIntegrityViolationException uniqueViolation(String constraintName) {
        return new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), constraintName));
    }
}
