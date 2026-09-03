package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultMailCommunicationServiceTest {
    private final ProsunaConfig config = mock(ProsunaConfig.class);
    private final JavaMailSenderImpl mailSender = mock(JavaMailSenderImpl.class);
    private final DefaultMailCommunicationService service = new DefaultMailCommunicationService(config, mailSender);

    @Test
    void sendsMarkdownMessageThroughTheConfiguredDefaultMailTransport() throws Exception {
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(config.getFromMail()).thenReturn("service@example.test");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendMessage(" customer@example.test ", new CommunicationMessage(
                "Status update",
                "Hello **customer**",
                "Hello **customer**",
                Instant.now(),
                List.of()
        ));

        verify(mailSender).send(same(mimeMessage));
        assertEquals("Status update", mimeMessage.getSubject());
        assertEquals("customer@example.test", mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("service@example.test", mimeMessage.getFrom()[0].toString());
        assertTrue(flattenContent(mimeMessage.getContent()).contains("<strong>customer</strong>"));
    }

    @Test
    void rejectsMultipleRecipientsBeforeCreatingAMessage() {
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "first@example.test,second@example.test",
                new CommunicationMessage("Subject", "Body", "Body", Instant.now(), List.of())
        ));

        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void rejectsAnEmptySubjectBeforeSending() {
        assertThrows(CommunicationException.class, () -> service.sendMessage(
                "customer@example.test",
                new CommunicationMessage(" ", "Body", "Body", Instant.now(), List.of())
        ));

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    private static String flattenContent(Object content) throws Exception {
        if (!(content instanceof Multipart multipart)) {
            return String.valueOf(content);
        }

        var result = new StringBuilder();
        for (int index = 0; index < multipart.getCount(); index++) {
            result.append(flattenContent(multipart.getBodyPart(index).getContent()));
        }
        return result.toString();
    }
}
