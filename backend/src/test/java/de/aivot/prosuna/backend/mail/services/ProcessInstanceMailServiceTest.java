package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.mail.config.ProcessNotificationInstanceAssignmentChangedUserConfigDefinition;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceMailServiceTest {
    private MailService mailService;
    private ProcessInstanceMailService service;
    private ThemeEntity theme;
    private ProcessInstanceEntity instance;
    private UserEntity recipient;

    @BeforeEach
    void setUp() throws Exception {
        mailService = mock(MailService.class);
        var processService = mock(ProcessService.class);
        var departmentService = mock(DepartmentService.class);
        service = new ProcessInstanceMailService(mailService, processService, departmentService);

        var process = new ProcessEntity().setId(17).setInternalTitle("Beispielprozess").setDepartmentId(3);
        var department = new DepartmentEntity().setId(3);
        theme = new ThemeEntity();
        when(processService.retrieve(17)).thenReturn(Optional.of(process));
        when(departmentService.retrieve(3)).thenReturn(Optional.of(department));
        when(departmentService.getDepartmentTheme(department)).thenReturn(theme);

        instance = new ProcessInstanceEntity()
                .setId(42L)
                .setProcessId(17)
                .setInitialProcessVersion(2)
                .setCaseNumber("V-42")
                .setAssignedFileNumbers(List.of("AZ-1", "AZ-2"));
        recipient = new UserEntity().setId("recipient").setFullName("Empfangende Person");
    }

    @Test
    void sendsFirstAssignmentWithInstanceLinkAndSeparatePreference() throws Exception {
        var actor = new UserEntity().setId("actor").setFullName("Alex Beispiel");

        service.sendAssigned(actor, recipient, instance, false);

        var context = sentContext("[Prosuna] Vorgang zugewiesen", MailTemplate.ProcessInstanceAssigned);
        assertEquals("Ihnen wurde in Prosuna ein Vorgang zugewiesen.", context.get("intro"));
        assertEquals("Alex Beispiel", context.get("changeSource"));
        assertEquals("AZ-1, AZ-2", context.get("fileNumbersDisplay"));
        assertEquals("/staff/processes/17/versions/2?instanceId=42", context.get("instancePath"));
        assertEquals(ProcessNotificationInstanceAssignmentChangedUserConfigDefinition.KEY,
                MailTemplate.ProcessInstanceAssigned.getUserConfigKey());
    }

    @Test
    void sendsReassignmentToNewRecipient() throws Exception {
        service.sendAssigned(null, recipient, instance, true);

        var context = sentContext("[Prosuna] Vorgang neu zugewiesen", MailTemplate.ProcessInstanceAssigned);
        assertEquals("Ihnen wurde in Prosuna ein Vorgang neu zugewiesen.", context.get("intro"));
        assertEquals("System", context.get("changeSource"));
        assertTrue(context.containsKey("instancePath"));
    }

    @Test
    void sendsRemovalWithoutTaskDetailsOrLink() throws Exception {
        instance.setAssignedFileNumbers(List.of("", " "));

        service.sendUnassigned(null, recipient, instance, false);

        var context = sentContext("[Prosuna] Zuweisung zum Vorgang aufgehoben", MailTemplate.ProcessInstanceUnassigned);
        assertEquals("Die Zuweisung dieses Vorgangs an Sie wurde aufgehoben.", context.get("intro"));
        assertEquals("System", context.get("changeSource"));
        assertNull(context.get("fileNumbersDisplay"));
        assertFalse(context.containsKey("instancePath"));
        assertFalse(context.containsKey("taskPath"));
        assertEquals(ProcessNotificationInstanceAssignmentChangedUserConfigDefinition.KEY,
                MailTemplate.ProcessInstanceUnassigned.getUserConfigKey());
    }

    @Test
    void informsFormerRecipientOfTransferWithoutLink() throws Exception {
        var actor = new UserEntity().setId("actor").setFullName("Alex Beispiel");

        service.sendUnassigned(actor, recipient, instance, true);

        var context = sentContext("[Prosuna] Zuweisung zum Vorgang aufgehoben", MailTemplate.ProcessInstanceUnassigned);
        assertEquals("Der Ihnen zugewiesene Vorgang wurde einer anderen Person zugewiesen.", context.get("intro"));
        assertEquals("Alex Beispiel", context.get("changeSource"));
        assertFalse(context.containsKey("instancePath"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentContext(String subject, MailTemplate template) throws Exception {
        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(eq(theme), eq(recipient.getId()), eq(subject), eq(template), captor.capture());
        return captor.getValue();
    }
}
