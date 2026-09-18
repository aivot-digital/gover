package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.ByteArrayCommunicationMessageAttachment;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.communication.models.MailCommunicationSendOptions;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultMailCommunicationServiceTest {
    private final MailService mailService = mock(MailService.class);
    private final SystemService systemService = mock(SystemService.class);
    private final VDepartmentShadowedService vDepartmentShadowedService = mock(VDepartmentShadowedService.class);
    private final ThemeService themeService = mock(ThemeService.class);
    private final ThemeEntity defaultTheme = new ThemeEntity();
    private final DefaultMailCommunicationService service = new DefaultMailCommunicationService(
            mailService,
            systemService,
            vDepartmentShadowedService,
            themeService
    );

    @Test
    void delegatesGenericTemplateRenderingWithMarkdownAndCallToActions() throws Exception {
        configureSending();
        var callToActions = List.of(
                new CommunicationMessageCallToAction(" Open portal ", " https://example.test/portal "),
                new CommunicationMessageCallToAction("Show status", "https://example.test/status")
        );

        service.sendMessage(" customer@example.test ", new CommunicationMessage(
                "Status update",
                "Hello customer",
                "Hello **customer**",
                callToActions,
                Instant.now(),
                List.of(),
                null,
                null
        ));

        verify(mailService).sendMail(
                same(defaultTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("Status update"),
                eq(MailTemplate.GenericEmailMessage),
                org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(context ->
                        "Status update".equals(context.get("title"))
                                && "Hello customer".equals(context.get("messageText"))
                                && context.get("messageHtml").toString().contains("<strong>customer</strong>")
                                && List.of(
                                new CommunicationMessageCallToAction("Open portal", "https://example.test/portal"),
                                new CommunicationMessageCallToAction("Show status", "https://example.test/status")
                        ).equals(context.get("callToActions"))
                ),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(new MailSendOptions(false, null, null, null))
        );
    }

    @Test
    void delegatesCustomSenderAndReplyToAddress() throws Exception {
        configureSending();

        service.sendMessage(
                "customer@example.test",
                message(),
                MailCommunicationSendOptions.customSender(
                        " Custom Service ",
                        " custom@example.test ",
                        " replies@example.test "
                )
        );

        verify(mailService).sendMail(
                any(),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any(),
                any(),
                any(),
                eq(new MailSendOptions(
                        false,
                        "Custom Service",
                        "custom@example.test",
                        "replies@example.test"
                ))
        );
    }

    @Test
    void usesInheritedDepartmentThemeAndSignatureWithoutChangingEnvelopeHeaders() throws Exception {
        configureSending();
        var department = new DepartmentEntity()
                .setId(17)
                .setName("Fachbereich");
        var shadowedDepartment = new VDepartmentShadowedEntity()
                .setId(17)
                .setName("Fachbereich")
                .setThemeId(9)
                .setDefaultMailSignature("Viele Grüße");
        var departmentTheme = new ThemeEntity();
        when(vDepartmentShadowedService.retrieve(17)).thenReturn(Optional.of(shadowedDepartment));
        when(themeService.retrieve(9)).thenReturn(Optional.of(departmentTheme));

        service.sendMessage(
                "customer@example.test",
                message().withSendingContext(null, department),
                MailCommunicationSendOptions.customSender(
                        "Configured Sender",
                        "configured@example.test",
                        "replies@example.test"
                )
        );

        verify(mailService).sendMail(
                same(departmentTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("Subject"),
                eq(MailTemplate.GenericEmailMessage),
                org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(context ->
                        context.get("department") == shadowedDepartment
                ),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(new MailSendOptions(
                        true,
                        "Configured Sender",
                        "configured@example.test",
                        "replies@example.test"
                ))
        );
    }

    @Test
    void sendingUserAloneDoesNotChangeMailRenderingOrEnvelope() throws Exception {
        configureSending();
        var user = new UserEntity().setId("user-1").setFullName("Sender User");

        service.sendMessage(
                "customer@example.test",
                message().withSendingContext(user, null)
        );

        verify(mailService).sendMail(
                same(defaultTheme),
                eq("customer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("Subject"),
                eq(MailTemplate.GenericEmailMessage),
                org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(context ->
                        !context.containsKey("department")
                ),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq(new MailSendOptions(false, null, null, null))
        );
        verifyNoInteractions(vDepartmentShadowedService, themeService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void convertsCommunicationAttachmentsForTheCentralMailService() throws Exception {
        configureSending();
        var bytes = "attachment".getBytes(StandardCharsets.UTF_8);
        var message = CommunicationMessage.of(
                "Subject",
                "Body",
                "Body",
                List.of(),
                List.of(new ByteArrayCommunicationMessageAttachment("note.txt", "text/plain", bytes))
        );

        service.sendMessage("customer@example.test", message);

        var attachmentsCaptor = org.mockito.ArgumentCaptor.forClass(Optional.class);
        verify(mailService).sendMail(
                any(),
                anyString(),
                any(),
                any(),
                anyString(),
                any(),
                any(),
                any(),
                attachmentsCaptor.capture(),
                any()
        );
        var attachments = (Optional<Collection<MailAttachmentBytes>>) attachmentsCaptor.getValue();
        assertTrue(attachments.isPresent());
        var attachment = attachments.orElseThrow().iterator().next();
        assertEquals("note.txt", attachment.filename());
        assertEquals("text/plain", attachment.contentType().toString());
        assertArrayEquals(bytes, attachment.bytes());
    }

    @Test
    void rejectsIncompleteOrInvalidCustomSenderBeforeSending() {
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                message(),
                MailCommunicationSendOptions.customSender(null, null, null)
        ));
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                message(),
                MailCommunicationSendOptions.customSender(" ", "sender@example.test", null)
        ));
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                message(),
                MailCommunicationSendOptions.customSender("Custom Service", "invalid", null)
        ));

        verify(mailService, never()).isSendingConfigured();
    }

    @Test
    void rejectsInvalidReplyToBeforeSending() {
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                message(),
                MailCommunicationSendOptions.defaultSender("first@example.test,second@example.test")
        ));

        verify(mailService, never()).isSendingConfigured();
    }

    @Test
    void rejectsInvalidMessageAndCallToActionsBeforeSending() {
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "first@example.test,second@example.test",
                message()
        ));
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                CommunicationMessage.of(" ", "Body", "Body")
        ));
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                CommunicationMessage.of(
                        "Subject",
                        "Body",
                        "Body",
                        List.of(new CommunicationMessageCallToAction(" ", "https://example.test")),
                        List.of()
                )
        ));
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                CommunicationMessage.of(
                        "Subject",
                        "Body",
                        "Body",
                        List.of(new CommunicationMessageCallToAction("Open", null)),
                        List.of()
                )
        ));

        verify(mailService, never()).isSendingConfigured();
    }

    @Test
    void rejectsSendingWhenSmtpIsNotConfigured() throws Exception {
        when(mailService.isSendingConfigured()).thenReturn(false);

        var exception = assertThrows(
                CommunicationException.class,
                () -> service.sendMessage("customer@example.test", message())
        );

        assertEquals("Der E-Mail-Versand ist nicht konfiguriert.", exception.getMessage());
        verify(mailService, never()).sendMail(
                any(), anyString(), any(), any(), anyString(), any(), any(), any(), any(), any()
        );
    }

    private void configureSending() {
        when(mailService.isSendingConfigured()).thenReturn(true);
        when(systemService.retrieveDefaultTheme()).thenReturn(defaultTheme);
    }

    private static CommunicationMessage message() {
        return CommunicationMessage.of("Subject", "Body", "Body");
    }
}
