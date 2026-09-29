package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVersionServiceTest {
    @Test
    void defaultsToCompactAndPreservesExplicitGenerationSettingsAcrossSerialization() {
        var mapper = de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils.createMapper();
        assertEquals(de.aivot.prosuna.backend.process.enums.CaseNumberType.CROCKFORD_BASE32,
                mapper.readValue("{}", ProcessVersionEntity.class).getCaseNumberType());
        for (var type : de.aivot.prosuna.backend.process.enums.CaseNumberType.values()) {
            var version = new ProcessVersionEntity().setCaseNumberType(type);
            var restored = mapper.readValue(mapper.writeValueAsString(version), ProcessVersionEntity.class);
            assertEquals(type, restored.getCaseNumberType());
        }
        var retainedVersion = new ProcessVersionEntity()
                .setRetentionTimeValue(6)
                .setRetentionTimeUnit(ProcessRetentionTimeUnit.Months);
        var restored = mapper.readValue(mapper.writeValueAsString(retainedVersion), ProcessVersionEntity.class);
        assertEquals(6, restored.getRetentionTimeValue());
        assertEquals(ProcessRetentionTimeUnit.Months, restored.getRetentionTimeUnit());
    }

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
                .setCaseNumberType(de.aivot.prosuna.backend.process.enums.CaseNumberType.TEMPLATE)
                .setCaseNumberTemplate("AZ-%YYY-%I(4)");

        var result = service.create(entity);

        verify(caseNumberGeneratorService).validateConfiguration(de.aivot.prosuna.backend.process.enums.CaseNumberType.TEMPLATE, "AZ-%YYY-%I(4)");
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
                .setThemeId(6)
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
                .setCaseNumberType(de.aivot.prosuna.backend.process.enums.CaseNumberType.TEMPLATE)
                .setCaseNumberTemplate("AZ-%YYY-%M-%I(4)")
                .setRetentionTimeValue(6)
                .setRetentionTimeUnit(ProcessRetentionTimeUnit.Months)
                .setNotes("Neue Notizen")
                .setThemeId(16)
                .setLegalSupportDepartmentId(11)
                .setTechnicalSupportDepartmentId(12)
                .setImprintDepartmentId(13)
                .setPrivacyDepartmentId(14)
                .setAccessibilityDepartmentId(15)
                .setProcessSpecificPrivacyStatement("Neuer Datenschutz")
                .setProcessSpecificAccessibilityStatement("Neue Barrierefreiheit");

        var result = service.performUpdate(ProcessVersionEntityId.of(12, 5), updatedEntity, existingEntity);

        verify(caseNumberGeneratorService).validateConfiguration(de.aivot.prosuna.backend.process.enums.CaseNumberType.TEMPLATE, "AZ-%YYY-%M-%I(4)");
        verify(repository).save(existingEntity);
        assertEquals(ProcessVersionStatus.Published, result.getStatus());
        assertEquals("Neu", result.getPublicTitle());
        assertEquals("AZ-%YYY-%M-%I(4)", result.getCaseNumberTemplate());
        assertEquals("Neue Notizen", result.getNotes());
        assertEquals(16, result.getThemeId());
        assertEquals(11, result.getLegalSupportDepartmentId());
        assertEquals(12, result.getTechnicalSupportDepartmentId());
        assertEquals(13, result.getImprintDepartmentId());
        assertEquals(14, result.getPrivacyDepartmentId());
        assertEquals(15, result.getAccessibilityDepartmentId());
        assertEquals("Neuer Datenschutz", result.getProcessSpecificPrivacyStatement());
        assertEquals("Neue Barrierefreiheit", result.getProcessSpecificAccessibilityStatement());
        assertEquals(6, result.getRetentionTimeValue());
        assertEquals(ProcessRetentionTimeUnit.Months, result.getRetentionTimeUnit());
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
                "Die Aufbewahrungsfrist muss festgelegt sein.",
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
                .setRetentionTimeValue(30)
                .setRetentionTimeUnit(ProcessRetentionTimeUnit.Days)
                .setLegalSupportDepartmentId(1)
                .setTechnicalSupportDepartmentId(2)
                .setImprintDepartmentId(3)
                .setPrivacyDepartmentId(4)
                .setAccessibilityDepartmentId(5));

        assertEquals(List.of(), result.versionProblems());
        assertEquals(List.of(), result.nodeProblems());
        assertFalse(result.hasAnyProblems());
    }

    @Test
    void create_RejectsIncompleteAndNonPositiveRetention() {
        var repository = mock(ProcessVersionRepository.class);
        var service = new ProcessVersionService(repository, mock(ProcessNodeService.class),
                mock(ProcessNodeDefinitionService.class), mock(CaseNumberGeneratorService.class));
        var version = new ProcessVersionEntity().setProcessId(12).setStatus(ProcessVersionStatus.Drafted);

        assertThrows(ResponseException.class, () -> service.create(version.setRetentionTimeValue(1)));
        assertThrows(ResponseException.class, () -> service.create(version
                .setRetentionTimeValue(0)
                .setRetentionTimeUnit(ProcessRetentionTimeUnit.Days)));
    }

    @Test
    void update_RequiresRetentionForPublicationAndFreezesItAfterward() throws ResponseException {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.save(any(ProcessVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new ProcessVersionService(repository, mock(ProcessNodeService.class),
                mock(ProcessNodeDefinitionService.class), mock(CaseNumberGeneratorService.class));
        var id = ProcessVersionEntityId.of(12, 5);
        var draft = new ProcessVersionEntity().setProcessId(12).setProcessVersion(5)
                .setStatus(ProcessVersionStatus.Drafted);
        var published = new ProcessVersionEntity().setProcessId(12).setProcessVersion(5)
                .setStatus(ProcessVersionStatus.Published);

        assertThrows(ResponseException.class, () -> service.performUpdate(id, published, draft));
        published.setRetentionTimeValue(30).setRetentionTimeUnit(ProcessRetentionTimeUnit.Days);
        service.performUpdate(id, published, draft);
        assertEquals(30, draft.getRetentionTimeValue());

        var changed = new ProcessVersionEntity().setStatus(ProcessVersionStatus.Published)
                .setRetentionTimeValue(2).setRetentionTimeUnit(ProcessRetentionTimeUnit.Weeks);
        assertThrows(ResponseException.class, () -> service.performUpdate(id, changed, draft));
        assertEquals(30, draft.getRetentionTimeValue());
        draft.setStatus(ProcessVersionStatus.Revoked);
        changed.setStatus(ProcessVersionStatus.Revoked);
        assertThrows(ResponseException.class, () -> service.performUpdate(id, changed, draft));
    }
}
