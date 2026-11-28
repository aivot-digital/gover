package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.communication.models.MailCommunicationSendOptions;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import org.springframework.http.MediaType;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/** Sends a raw communication message through Prosuna's configured default mail transport. */
@Service
public class DefaultMailCommunicationService {
    private final MailService mailService;
    private final SystemService systemService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final ThemeService themeService;

    public DefaultMailCommunicationService(MailService mailService,
                                           SystemService systemService,
                                           VDepartmentShadowedService vDepartmentShadowedService,
                                           ThemeService themeService) {
        this.mailService = mailService;
        this.systemService = systemService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.themeService = themeService;
    }

    public void sendMessage(@Nonnull String rawRecipient, @Nonnull CommunicationMessage message) throws CommunicationException {
        sendMessage(rawRecipient, message, MailCommunicationSendOptions.defaults());
    }

    public void sendMessage(@Nonnull String rawRecipient,
                            @Nonnull CommunicationMessage message,
                            @Nonnull MailCommunicationSendOptions options) throws CommunicationException {
        final String recipient;
        try {
            recipient = EmailAddressUtils.normalizeTransportAddress(rawRecipient);
        } catch (IllegalArgumentException e) {
            throw new CommunicationException("Die E-Mail-Adresse der Identität ist ungültig.", e);
        }

        final String senderName;
        final String senderAddress;
        if (!options.useDefaultSender()) {
            senderName = trimToNull(options.senderName());
            if (senderName == null) {
                throw new CommunicationException("Der konfigurierte Absendername darf nicht leer sein.");
            }
            try {
                senderAddress = EmailAddressUtils.normalizeSingleAddress(options.senderAddress());
            } catch (IllegalArgumentException e) {
                throw new CommunicationException("Die konfigurierte Absenderadresse ist ungültig.", e);
            }
        } else {
            senderName = null;
            senderAddress = null;
        }

        final String replyToAddress;
        var rawReplyToAddress = trimToNull(options.replyToAddress());
        if (rawReplyToAddress == null) {
            replyToAddress = null;
        } else {
            try {
                replyToAddress = EmailAddressUtils.normalizeSingleAddress(rawReplyToAddress);
            } catch (IllegalArgumentException e) {
                throw new CommunicationException("Die konfigurierte Reply-To-Adresse ist ungültig.", e);
            }
        }

        if (message.subject() == null || message.subject().isBlank()) {
            throw new CommunicationException("Der Betreff der E-Mail darf nicht leer sein.");
        }
        if (message.body() == null || message.body().isBlank()) {
            throw new CommunicationException("Der Inhalt der E-Mail darf nicht leer sein.");
        }
        if (message.markdownBody() == null || message.markdownBody().isBlank()) {
            throw new CommunicationException("Der HTML-Inhalt der E-Mail darf nicht leer sein.");
        }

        var callToActions = validateAndNormalizeCallToActions(message.callToActions());

        if (!mailService.isSendingConfigured()) {
            throw new CommunicationException("Der E-Mail-Versand ist nicht konfiguriert.");
        }

        try {
            var templateContext = new HashMap<String, Object>();
            templateContext.put("title", message.subject());
            templateContext.put("messageText", message.body());
            templateContext.put("messageHtml", MarkdownRenderer.renderHtml(message.markdownBody()));
            templateContext.put("callToActions", callToActions);

            var theme = systemService.retrieveDefaultTheme();
            if (message.sendingDepartment() != null) {
                var department = resolveDepartment(message.sendingDepartment());
                templateContext.put("department", department);

                var themeId = resolveThemeId(department);
                if (themeId != null) {
                    theme = themeService.retrieve(themeId).orElse(theme);
                }
            }
            if (message.signatureDepartment() != null) {
                templateContext.put("signatureDepartment", message.signatureDepartment());
            }

            var attachments = readAttachments(message);
            var mailOptions = new MailSendOptions(
                    message.signatureDepartment() != null,
                    senderName,
                    senderAddress,
                    replyToAddress
            );
            mailService.sendMail(
                    theme,
                    recipient,
                    Optional.empty(),
                    Optional.empty(),
                    message.subject(),
                    MailTemplate.GenericEmailMessage,
                    templateContext,
                    Optional.empty(),
                    attachments.isEmpty() ? Optional.empty() : Optional.of(attachments),
                    mailOptions
            );
        } catch (MessagingException | MailException | IOException | ResponseException | IllegalArgumentException e) {
            var messageText = "Die E-Mail an %s konnte nicht versendet werden.".formatted(recipient);
            var reason = mailFailureReason(e);
            throw new CommunicationException(reason == null ? messageText : messageText + " " + reason, e);
        }
    }

    @Nullable
    private static String mailFailureReason(@Nonnull Exception failure) {
        if (failure instanceof MailAuthenticationException || failure instanceof AuthenticationFailedException) {
            return "Die Anmeldung am SMTP-Server ist fehlgeschlagen.";
        }
        if (failure instanceof AddressException) {
            return "Eine E-Mail-Adresse ist ungültig.";
        }
        if (failure instanceof MailSendException sendFailure) {
            if (sendFailure.contains(ConnectException.class) || sendFailure.contains(UnknownHostException.class)) {
                return "Der SMTP-Server konnte nicht erreicht werden.";
            }
            // Spring keeps per-message SMTP rejections separately from the transport exception's cause.
            for (var messageFailure : sendFailure.getMessageExceptions()) {
                if (hasRejectedRecipient(messageFailure)) {
                    return "Der SMTP-Server hat die Empfängeradresse abgelehnt.";
                }
            }
        }
        return hasRejectedRecipient(failure) ? "Der SMTP-Server hat die Empfängeradresse abgelehnt." : null;
    }

    private static boolean hasRejectedRecipient(@Nonnull Exception failure) {
        return failure instanceof SendFailedException rejected && rejected.getInvalidAddresses() != null
                && rejected.getInvalidAddresses().length > 0;
    }

    @Nonnull
    private Object resolveDepartment(@Nonnull DepartmentEntity department) {
        if (department.getId() == null) {
            return department;
        }
        var shadowedDepartment = vDepartmentShadowedService.retrieve(department.getId());
        return shadowedDepartment.isPresent() ? shadowedDepartment.get() : department;
    }

    @Nullable
    private static Integer resolveThemeId(@Nonnull Object department) {
        if (department instanceof VDepartmentShadowedEntity shadowedDepartment) {
            return shadowedDepartment.getThemeId();
        }
        if (department instanceof DepartmentEntity departmentEntity) {
            return departmentEntity.getThemeId();
        }
        return null;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Nonnull
    private static List<CommunicationMessageCallToAction> validateAndNormalizeCallToActions(
            List<CommunicationMessageCallToAction> callToActions
    ) throws CommunicationException {
        if (callToActions == null || callToActions.isEmpty()) {
            return List.of();
        }

        var normalized = new ArrayList<CommunicationMessageCallToAction>(callToActions.size());
        for (var callToAction : callToActions) {
            if (callToAction == null) {
                throw new CommunicationException("Eine Aktion der Nachricht darf nicht leer sein.");
            }
            var title = trimToNull(callToAction.title());
            if (title == null) {
                throw new CommunicationException("Der Titel einer Aktion darf nicht leer sein.");
            }
            var link = trimToNull(callToAction.link());
            if (link == null) {
                throw new CommunicationException("Der Link einer Aktion darf nicht leer sein.");
            }
            normalized.add(new CommunicationMessageCallToAction(title, link));
        }
        return normalized;
    }

    @Nonnull
    private static List<MailAttachmentBytes> readAttachments(CommunicationMessage message) throws IOException {
        var attachments = new ArrayList<MailAttachmentBytes>();
        for (var attachment : message.attachments()) {
            if (attachment == null) {
                continue;
            }
            var attachmentContent = attachment.getContent();
            if (attachmentContent == null) {
                continue;
            }
            var name = attachment.getName() == null || attachment.getName().isBlank()
                    ? "Anlage"
                    : attachment.getName();
            var contentType = attachment.getContentType() == null || attachment.getContentType().isBlank()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(attachment.getContentType());
            try (var content = attachmentContent) {
                attachments.add(new MailAttachmentBytes(name, contentType, content.readAllBytes()));
            }
        }
        return attachments;
    }
}
