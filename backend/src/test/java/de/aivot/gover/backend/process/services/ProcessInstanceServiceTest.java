package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceServiceTest {
    @Test
    void create_RetriesWhenTheGeneratedCaseNumberCollides() throws ResponseException {
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        when(processInstanceRepository.saveAndFlush(any(ProcessInstanceEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate case number"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(processInstanceRepository.existsByCaseNumber("AZ-2026-0001")).thenReturn(true);

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(any(ProcessVersionEntityId.class)))
                .thenReturn(Optional.of(
                        new ProcessVersionEntity()
                                .setProcessId(7)
                                .setProcessVersion(1)
                                .setCaseNumberTemplate("AZ-%YYY-%I(4)")
                ));

        var caseNumberGeneratorService = mock(CaseNumberGeneratorService.class);
        when(caseNumberGeneratorService.generateCaseNumber("AZ-%YYY-%I(4)"))
                .thenReturn("AZ-2026-0001")
                .thenReturn("AZ-2026-0002");

        var service = new ProcessInstanceService(
                processInstanceRepository,
                mock(ProcessInstanceAttachmentRepository.class),
                mock(ProcessInstanceAttachmentSetRepository.class),
                mock(ProcessInstanceAttachmentService.class),
                processVersionService,
                caseNumberGeneratorService
        );

        var entity = new ProcessInstanceEntity()
                .setProcessId(7)
                .setInitialProcessVersion(1);

        var result = service.create(entity);

        verify(caseNumberGeneratorService, times(2)).generateCaseNumber("AZ-%YYY-%I(4)");
        verify(processInstanceRepository, times(2)).saveAndFlush(entity);
        verify(processInstanceRepository).existsByCaseNumber("AZ-2026-0001");
        assertEquals("AZ-2026-0002", result.getCaseNumber());
        assertNotNull(result.getAccessKey());
    }

    @Test
    void create_UsesTheGeneratorFallbackWhenNoTemplateExists() throws ResponseException {
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        when(processInstanceRepository.saveAndFlush(any(ProcessInstanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(any(ProcessVersionEntityId.class)))
                .thenReturn(Optional.of(
                        new ProcessVersionEntity()
                                .setProcessId(7)
                                .setProcessVersion(1)
                                .setCaseNumberTemplate(null)
                ));

        var caseNumberGeneratorService = mock(CaseNumberGeneratorService.class);
        when(caseNumberGeneratorService.generateCaseNumber(null)).thenReturn("generated-uuid");

        var service = new ProcessInstanceService(
                processInstanceRepository,
                mock(ProcessInstanceAttachmentRepository.class),
                mock(ProcessInstanceAttachmentSetRepository.class),
                mock(ProcessInstanceAttachmentService.class),
                processVersionService,
                caseNumberGeneratorService
        );

        var entity = new ProcessInstanceEntity()
                .setProcessId(7)
                .setInitialProcessVersion(1);

        var result = service.create(entity);

        verify(caseNumberGeneratorService).generateCaseNumber(null);
        verify(processInstanceRepository).saveAndFlush(entity);
        assertEquals("generated-uuid", result.getCaseNumber());
    }
}
