package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.mail.config.ProcessNotificationTaskAssignedUserConfigDefinition;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessTaskMailServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void sendsRemovalToFormerRecipientWithoutTaskLink() throws Exception {
        var mailService = mock(MailService.class);
        var processService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        var service = new ProcessTaskMailService(mailService, processService, departmentService);
        var process = new ProcessEntity().setId(17).setInternalTitle("Beispielprozess").setDepartmentId(3);
        var department = new DepartmentEntity().setId(3);
        var theme = new ThemeEntity();
        when(processService.retrieve(17)).thenReturn(Optional.of(process));
        when(departmentService.retrieve(3)).thenReturn(Optional.of(department));
        when(departmentService.getDepartmentTheme(department)).thenReturn(theme);

        var formerRecipient = new UserEntity().setId("former").setFullName("Bisherige Person");
        var actor = new UserEntity().setId("actor").setFullName("Alex Beispiel");
        var instance = new ProcessInstanceEntity()
                .setAssignedFileNumbers(List.of("AZ-1", "AZ-2"))
                .setCreatedForTestClaimId(5);
        var task = new ProcessInstanceTaskEntity().setProcessId(17).setProcessVersion(2);
        var node = new ProcessNodeEntity().setName("Prüfung");

        service.sendUnassigned(actor, formerRecipient, instance, task, node, mock(ProcessNodeDefinition.class));

        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(eq(theme), eq("former"),
                eq("[Prosuna] [Test] Zuweisung zur Aufgabe aufgehoben"),
                eq(MailTemplate.ProcessTaskUnassigned), captor.capture());
        Map<String, Object> context = captor.getValue();
        assertEquals("Prüfung", context.get("taskName"));
        assertEquals("AZ-1, AZ-2", context.get("fileNumbersDisplay"));
        assertEquals("Alex Beispiel", context.get("changeSource"));
        assertFalse(context.containsKey("taskPath"));
        assertFalse(context.containsKey("startedLabel"));
        assertEquals(ProcessNotificationTaskAssignedUserConfigDefinition.KEY,
                MailTemplate.ProcessTaskUnassigned.getUserConfigKey());
    }

    @Test
    @SuppressWarnings("unchecked")
    void omitsMissingFileNumbersAndAttributesAutomaticRemovalToSystem() throws Exception {
        var mailService = mock(MailService.class);
        var processService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        var service = new ProcessTaskMailService(mailService, processService, departmentService);
        var process = new ProcessEntity().setId(17).setDepartmentId(3);
        var department = new DepartmentEntity().setId(3);
        when(processService.retrieve(17)).thenReturn(Optional.of(process));
        when(departmentService.retrieve(3)).thenReturn(Optional.of(department));

        service.sendUnassigned(null, new UserEntity().setId("former"),
                new ProcessInstanceEntity().setAssignedFileNumbers(List.of(" ")),
                new ProcessInstanceTaskEntity().setProcessId(17),
                new ProcessNodeEntity().setName("Prüfung"), mock(ProcessNodeDefinition.class));

        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(eq(null), eq("former"),
                eq("[Prosuna] Zuweisung zur Aufgabe aufgehoben"),
                eq(MailTemplate.ProcessTaskUnassigned), captor.capture());
        Map<String, Object> context = captor.getValue();
        assertNull(context.get("fileNumbersDisplay"));
        assertEquals("System", context.get("changeSource"));
    }
}
