package de.aivot.prosuna.backend.mail.services;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        var service = new ProcessTaskMailService(mailService, processService, departmentService, mock(UserDeputyService.class));
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
        var service = new ProcessTaskMailService(mailService, processService, departmentService, mock(UserDeputyService.class));
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

    @Test
    @SuppressWarnings("unchecked")
    void sendsAssignmentToActiveDeputiesWordedForThemAndSkipsTheTriggeringDeputy() throws Exception {
        var mailService = mock(MailService.class);
        var processService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        var deputies = mock(UserDeputyService.class);
        var service = new ProcessTaskMailService(mailService, processService, departmentService, deputies);
        var process = new ProcessEntity().setId(17).setDepartmentId(3);
        var department = new DepartmentEntity().setId(3);
        when(processService.retrieve(17)).thenReturn(Optional.of(process));
        when(departmentService.retrieve(3)).thenReturn(Optional.of(department));
        var assignee = new UserEntity().setId("philipp").setFullName("Philipp Schröder");
        var deputy = new UserEntity().setId("daniel").setFullName("Daniel Müller");
        var triggeringDeputy = new UserEntity().setId("jana").setFullName("Jana Beispiel");
        when(deputies.findActiveDeputies(java.util.Set.of("philipp")))
                .thenReturn(Map.of("philipp", List.of(deputy, triggeringDeputy)));
        doThrow(new jakarta.mail.MessagingException("SMTP unavailable")).when(mailService)
                .sendMailToUser(eq(null), eq(deputy), any(), any(), any());

        service.sendAssigned(triggeringDeputy, assignee, new ProcessInstanceEntity().setId(5L).setAssignedFileNumbers(List.of()),
                new ProcessInstanceTaskEntity().setId(8L).setProcessId(17).setStarted(java.time.Instant.now()),
                new ProcessNodeEntity().setName("Prüfung"), mock(ProcessNodeDefinition.class), false);

        verify(mailService).sendMailToUser(eq(null), eq("philipp"), eq("[Prosuna] Ihnen wurde eine Aufgabe zugewiesen"),
                eq(MailTemplate.ProcessTaskAssigned), any());
        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(eq(null), eq(deputy), eq("[Prosuna] In Vertretung: Aufgabe zugewiesen"),
                eq(MailTemplate.ProcessTaskAssigned), captor.capture());
        Map<String, Object> context = captor.getValue();
        assertEquals("Philipp Schröder wurde in Prosuna eine Aufgabe zur Bearbeitung zugewiesen. "
                + "Sie erhalten diese Benachrichtigung, weil Sie Philipp Schröder derzeit vertreten.", context.get("assignmentIntro"));
        verify(mailService, never()).sendMailToUser(any(), eq(triggeringDeputy), any(), any(), any());
    }

    @Test
    void stillSendsTheDeputyCopiesWhenTheMailToTheAssigneeFails() throws Exception {
        var mailService = mock(MailService.class);
        var processService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        var deputies = mock(UserDeputyService.class);
        var service = new ProcessTaskMailService(mailService, processService, departmentService, deputies);
        var department = new DepartmentEntity().setId(3);
        when(processService.retrieve(17)).thenReturn(Optional.of(new ProcessEntity().setId(17).setDepartmentId(3)));
        when(departmentService.retrieve(3)).thenReturn(Optional.of(department));
        var assignee = new UserEntity().setId("philipp").setFullName("Philipp Schröder");
        var deputy = new UserEntity().setId("daniel").setFullName("Daniel Müller");
        when(deputies.findActiveDeputies(java.util.Set.of("philipp"))).thenReturn(Map.of("philipp", List.of(deputy)));
        doThrow(new jakarta.mail.MessagingException("SMTP unavailable")).when(mailService)
                .sendMailToUser(eq(null), eq("philipp"), any(), any(), any());

        assertThrows(jakarta.mail.MessagingException.class, () -> service.sendAssigned(null, assignee,
                new ProcessInstanceEntity().setId(5L).setAssignedFileNumbers(List.of()),
                new ProcessInstanceTaskEntity().setId(8L).setProcessId(17).setStarted(java.time.Instant.now()),
                new ProcessNodeEntity().setName("Prüfung"), mock(ProcessNodeDefinition.class), false));

        verify(mailService).sendMailToUser(eq(null), eq(deputy), eq("[Prosuna] In Vertretung: Aufgabe zugewiesen"),
                eq(MailTemplate.ProcessTaskAssigned), any());
    }
}
