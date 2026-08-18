package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import de.aivot.gover.backend.process.services.ProcessInstanceTaskService;
import de.aivot.gover.backend.process.services.ProcessVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitizenProcessInstanceViewControllerTest {
    @Test
    void retrieveShouldSupportNonUuidProcessAccessKeys() throws ResponseException {
        var instanceAccessKey = "instance-access-key";
        var taskAccessKey = "task-access-key";
        var now = Instant.now();

        var instance = new ProcessInstanceEntity()
                .setId(42L)
                .setCaseNumber("CASE-1")
                .setAccessKey(instanceAccessKey)
                .setProcessId(7)
                .setInitialProcessVersion(3)
                .setStatus(ProcessInstanceStatus.Running);

        var processVersion = new ProcessVersionEntity()
                .setProcessId(instance.getProcessId())
                .setProcessVersion(instance.getInitialProcessVersion())
                .setStatus(ProcessVersionStatus.Published)
                .setPublicTitle("Public title");

        var task = new ProcessInstanceTaskEntity(
                9L,
                taskAccessKey,
                instance.getId(),
                instance.getProcessId(),
                instance.getInitialProcessVersion(),
                11,
                null,
                null,
                null,
                ProcessTaskStatus.Completed,
                null,
                now,
                now,
                now,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null
        );

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.retrieveByAccessKey(instanceAccessKey))
                .thenReturn(Optional.of(instance));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion())))
                .thenReturn(Optional.of(processVersion));

        var processInstanceTaskService = mock(ProcessInstanceTaskService.class);
        when(processInstanceTaskService.list(any(Pageable.class), any(ProcessInstanceTaskFilter.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        var controller = new CitizenProcessInstanceViewController(
                processInstanceService,
                processInstanceTaskService,
                processVersionService
        );

        var response = controller.retrieve(instanceAccessKey);

        assertEquals(processVersion.getPublicTitle(), response.title());
        assertEquals(ProcessInstanceStatus.Running, response.status());
        assertEquals(List.of(taskAccessKey), response.currentTasks());
        verify(processInstanceService).retrieveByAccessKey(instanceAccessKey);
        verify(processInstanceTaskService).list(any(Pageable.class), argThat((ProcessInstanceTaskFilter filter) ->
                instance.getId().equals(filter.getProcessInstanceId())
        ));
    }
}
