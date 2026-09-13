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
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
            recipient = EmailAddressUtils.normalizeSingleAddress(rawRecipient);
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
        if (message.htmlBody() == null || message.htmlBody().isBlank()) {
            throw new CommunicationException("Der HTML-Inhalt der E-Mail darf nicht leer sein.");
        }

        var callToActions = validateAndNormalizeCallToActions(message.callToActions());

        if (!mailService.isSendingConfigured()) {
            throw new CommunicationException("Der E-Mail-Versand ist nicht konfiguriert.");
        }

        try {
            var document = Parser.builder().build().parse(message.htmlBody());
            var templateContext = new HashMap<String, Object>();
            templateContext.put("title", message.subject());
            templateContext.put("messageText", message.body());
            templateContext.put("messageHtml", HtmlRenderer.builder().build().render(document));
            templateContext.put("callToActions", callToActions);

            var theme = systemService.retrieveDefaultTheme();
            var includeDefaultMailSignature = false;
            if (message.sendingDepartment() != null) {
                var department = resolveDepartment(message.sendingDepartment());
                templateContext.put("department", department);
                includeDefaultMailSignature = true;

                var themeId = resolveThemeId(department);
                if (themeId != null) {
                    theme = themeService.retrieve(themeId).orElse(theme);
                }
            }

            var attachments = readAttachments(message);
            var mailOptions = new MailSendOptions(
                    includeDefaultMailSignature,
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
            throw new CommunicationException(
                    "Die E-Mail an %s konnte nicht versendet werden.".formatted(recipient),
                    e
            );
        }
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
                    ? "Anhang"
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
