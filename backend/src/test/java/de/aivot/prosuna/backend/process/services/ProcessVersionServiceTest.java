package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.services.CaseNumberGeneratorService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVersionServiceTest {
    @Test
    void create_ValidatesCaseNumberTemplateBeforeSaving() throws ResponseException {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.maxVersionForProcessDefinition(12)).thenReturn(Optional.of(4));
        when(repository.save(any(ProcessVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var caseNumberGeneratorService = mock(CaseNumberGeneratorService.class);
        var service = new ProcessVersionService(
                repository,
                mock(ProcessNodeService.class),
                mock(ProcessNodeDefinitionService.class),
                caseNumberGeneratorService
        );

        var entity = new ProcessVersionEntity()
                .setProcessId(12)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Bauantrag")
                .setCaseNumberTemplate("AZ-%YYY-%I(4)");

        var result = service.create(entity);

        verify(caseNumberGeneratorService).validateCaseNumberTemplate("AZ-%YYY-%I(4)");
        verify(repository).save(entity);
        assertEquals(5, result.getProcessVersion());
    }

    @Test
    void performUpdate_ValidatesAndPersistsCaseNumberTemplate() throws ResponseException {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.save(any(ProcessVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var caseNumberGeneratorService = mock(CaseNumberGeneratorService.class);
        var service = new ProcessVersionService(
                repository,
                mock(ProcessNodeService.class),
                mock(ProcessNodeDefinitionService.class),
                caseNumberGeneratorService
        );

        var existingEntity = new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(5)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Alt")
                .setCaseNumberTemplate(null)
                .setNotes("Alte Notizen")
                .setLegalSupportDepartmentId(1)
                .setTechnicalSupportDepartmentId(2)
                .setImprintDepartmentId(3)
                .setPrivacyDepartmentId(4)
                .setAccessibilityDepartmentId(5)
                .setProcessSpecificPrivacyStatement("Alter Datenschutz")
                .setProcessSpecificAccessibilityStatement("Alte Barrierefreiheit");
        var updatedEntity = new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(5)
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Neu")
                .setCaseNumberTemplate("AZ-%YYY-%M-%I(4)")
                .setNotes("Neue Notizen")
                .setLegalSupportDepartmentId(11)
                .setTechnicalSupportDepartmentId(12)
                .setImprintDepartmentId(13)
                .setPrivacyDepartmentId(14)
                .setAccessibilityDepartmentId(15)
                .setProcessSpecificPrivacyStatement("Neuer Datenschutz")
                .setProcessSpecificAccessibilityStatement("Neue Barrierefreiheit");

        var result = service.performUpdate(ProcessVersionEntityId.of(12, 5), updatedEntity, existingEntity);

        verify(caseNumberGeneratorService).validateCaseNumberTemplate("AZ-%YYY-%M-%I(4)");
        verify(repository).save(existingEntity);
        assertEquals(ProcessVersionStatus.Published, result.getStatus());
        assertEquals("Neu", result.getPublicTitle());
        assertEquals("AZ-%YYY-%M-%I(4)", result.getCaseNumberTemplate());
        assertEquals("Neue Notizen", result.getNotes());
        assertEquals(11, result.getLegalSupportDepartmentId());
        assertEquals(12, result.getTechnicalSupportDepartmentId());
        assertEquals(13, result.getImprintDepartmentId());
        assertEquals(14, result.getPrivacyDepartmentId());
        assertEquals(15, result.getAccessibilityDepartmentId());
        assertEquals("Neuer Datenschutz", result.getProcessSpecificPrivacyStatement());
        assertEquals("Neue Barrierefreiheit", result.getProcessSpecificAccessibilityStatement());
    }

    @Test
    void validate_ShouldReportMissingVersionLegalSettings() throws ResponseException {
        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.findAllByProcessIdAndProcessVersion(12, 5)).thenReturn(List.of());

        var service = new ProcessVersionService(
                mock(ProcessVersionRepository.class),
                processNodeService,
                mock(ProcessNodeDefinitionService.class),
                mock(CaseNumberGeneratorService.class)
        );

        var result = service.validate(new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(5));

        assertEquals(List.of(
                "Der fachliche Support muss eingerichtet sein.",
                "Der technische Support muss eingerichtet sein.",
                "Das Impressum muss eingerichtet sein.",
                "Die Datenschutzerklärung muss eingerichtet sein.",
                "Die Barrierefreiheitserklärung muss eingerichtet sein."
        ), result.versionProblems());
        assertEquals(List.of(), result.nodeProblems());
    }

    @Test
    void validate_ShouldAcceptCompleteVersionLegalSettingsWithoutNodes() throws ResponseException {
        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.findAllByProcessIdAndProcessVersion(12, 5)).thenReturn(List.of());

        var service = new ProcessVersionService(
                mock(ProcessVersionRepository.class),
                processNodeService,
                mock(ProcessNodeDefinitionService.class),
                mock(CaseNumberGeneratorService.class)
        );

        var result = service.validate(new ProcessVersionEntity()
                .setProcessId(12)
                .setProcessVersion(5)
                .setLegalSupportDepartmentId(1)
                .setTechnicalSupportDepartmentId(2)
                .setImprintDepartmentId(3)
                .setPrivacyDepartmentId(4)
                .setAccessibilityDepartmentId(5));

        assertEquals(List.of(), result.versionProblems());
        assertEquals(List.of(), result.nodeProblems());
        assertFalse(result.hasAnyProblems());
    }
}
