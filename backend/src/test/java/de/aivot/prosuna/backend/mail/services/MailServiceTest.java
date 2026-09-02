package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.config.services.UserConfigService;
import de.aivot.prosuna.backend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.DepartmentMembershipService;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MailServiceTest {
    @Test
    void sendMailEmbedsTheSenderLogoAndKeepsTextAndHtmlAlternatives() throws Exception {
        var prosunaConfig = mock(ProsunaConfig.class);
        when(prosunaConfig.getFromMail()).thenReturn("noreply@example.org");
        var mailSender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        var systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.retrieve(ProviderNameSystemConfigDefinition.KEY)).thenReturn(
                new SystemConfigEntity()
                        .setKey(ProviderNameSystemConfigDefinition.KEY)
                        .setValue("Musterstadt")
                        .setPublicConfig(true)
        );
        var mailLogoService = mock(MailLogoService.class);
        var logoKey = UUID.randomUUID();
        when(mailLogoService.createSenderLogo(logoKey)).thenReturn(Optional.of(
                new MailLogoService.SenderLogo(
                        new byte[]{1, 2, 3},
                        "image/png",
                        "sender-logo.png"
                )
        ));
        var service = new MailService(
                prosunaConfig,
                mailSender,
                systemConfigService,
                mock(DepartmentService.class),
                mock(VDepartmentShadowedService.class),
                mock(DepartmentMembershipService.class),
                mailLogoService,
                mock(UserService.class),
                mock(UserConfigService.class)
        );
        ReflectionTestUtils.setField(service, "mailHost", "smtp.example.org");
        var theme = new ThemeEntity().setLogoKey(logoKey);
        var context = new HashMap<String, Object>();
        context.put("title", "Testversand");

        service.sendMail(
                theme,
                "recipient@example.org",
                Optional.empty(),
                Optional.empty(),
                "Test",
                MailTemplate.SmtpTest,
                context,
                Optional.empty()
        );

        verify(mailSender).send(message);
        message.saveChanges();
        var parts = collectParts(message.getContent());
        assertTrue(parts.stream().anyMatch(part -> contentTypeStartsWith(part, "text/plain")));
        assertTrue(parts.stream().anyMatch(part -> contentTypeStartsWith(part, "text/html")));
        assertTrue(parts.stream().anyMatch(part -> {
            try {
                var contentIds = part.getHeader("Content-ID");
                return contentIds != null
                        && Arrays.asList(contentIds).contains("<sender-logo>")
                        && contentTypeStartsWith(part, "image/png");
            } catch (Exception exception) {
                return false;
            }
        }));
    }

    @Test
    void sendMailIncludesTheInheritedDepartmentSignatureByDefault() throws Exception {
        var message = sendMailWithDepartmentSignature(MailSendOptions.defaults());

        var textParts = collectParts(message.getContent())
                .stream()
                .filter(part -> contentTypeStartsWith(part, "text/plain") || contentTypeStartsWith(part, "text/html"))
                .map(this::readContent)
                .toList();

        assertFalse(textParts.isEmpty());
        assertTrue(textParts.stream().allMatch(content -> content.contains("Amt für Beispielangelegenheiten")));
        assertTrue(textParts.stream().noneMatch(content -> content.contains("Mit freundlichen Grüßen")));
    }

    @Test
    void sendMailCanExcludeTheDepartmentSignature() throws Exception {
        var message = sendMailWithDepartmentSignature(new MailSendOptions(false));

        var textParts = collectParts(message.getContent())
                .stream()
                .filter(part -> contentTypeStartsWith(part, "text/plain") || contentTypeStartsWith(part, "text/html"))
                .map(this::readContent)
                .toList();

        assertFalse(textParts.isEmpty());
        assertTrue(textParts.stream().noneMatch(content -> content.contains("Amt für Beispielangelegenheiten")));
    }

    private MimeMessage sendMailWithDepartmentSignature(MailSendOptions options) throws Exception {
        var prosunaConfig = mock(ProsunaConfig.class);
        when(prosunaConfig.getFromMail()).thenReturn("noreply@example.org");
        var mailSender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        var systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.retrieve(ProviderNameSystemConfigDefinition.KEY)).thenReturn(
                new SystemConfigEntity()
                        .setKey(ProviderNameSystemConfigDefinition.KEY)
                        .setValue("Musterstadt")
                        .setPublicConfig(true)
        );
        var shadowedDepartmentService = mock(VDepartmentShadowedService.class);
        when(shadowedDepartmentService.retrieve(42)).thenReturn(Optional.of(
                new VDepartmentShadowedEntity()
                        .setId(42)
                        .setDefaultMailSignature("Amt für Beispielangelegenheiten")
        ));
        var service = new MailService(
                prosunaConfig,
                mailSender,
                systemConfigService,
                mock(DepartmentService.class),
                shadowedDepartmentService,
                mock(DepartmentMembershipService.class),
                mock(MailLogoService.class),
                mock(UserService.class),
                mock(UserConfigService.class)
        );
        ReflectionTestUtils.setField(service, "mailHost", "smtp.example.org");
        var context = new HashMap<String, Object>();
        context.put("title", "Testversand");
        context.put("department", new DepartmentEntity().setId(42));

        service.sendMail(
                new ThemeEntity(),
                "recipient@example.org",
                Optional.empty(),
                Optional.empty(),
                "Test",
                MailTemplate.SmtpTest,
                context,
                Optional.empty(),
                options
        );

        verify(mailSender).send(message);
        message.saveChanges();
        return message;
    }

    private List<BodyPart> collectParts(Object content) throws Exception {
        var parts = new ArrayList<BodyPart>();
        if (!(content instanceof Multipart multipart)) {
            return parts;
        }

        for (var index = 0; index < multipart.getCount(); index++) {
            var part = multipart.getBodyPart(index);
            parts.add(part);
            parts.addAll(collectParts(part.getContent()));
        }
        return parts;
    }

    private boolean contentTypeStartsWith(BodyPart part, String expected) {
        try {
            return part.getContentType().toLowerCase().startsWith(expected);
        } catch (Exception exception) {
            return false;
        }
    }

    private String readContent(BodyPart part) {
        try {
            return part.getContent().toString();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
