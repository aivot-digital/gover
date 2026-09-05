package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.MailCommunicationSendOptions;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import jakarta.annotation.Nonnull;
import jakarta.mail.MessagingException;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Sends a raw communication message through Prosuna's configured default mail transport. */
@Service
public class DefaultMailCommunicationService {
    private final ProsunaConfig prosunaConfig;
    private final JavaMailSenderImpl mailSender;

    public DefaultMailCommunicationService(ProsunaConfig prosunaConfig, JavaMailSenderImpl mailSender) {
        this.prosunaConfig = prosunaConfig;
        this.mailSender = mailSender;
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

        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (senderAddress == null) {
                helper.setFrom(prosunaConfig.getFromMail());
            } else {
                helper.setFrom(senderAddress, senderName);
            }
            if (replyToAddress != null) {
                helper.setReplyTo(replyToAddress);
            }
            helper.setTo(recipient);
            helper.setSubject(message.subject());

            var document = Parser.builder().build().parse(message.htmlBody());
            helper.setText(HtmlRenderer.builder().build().render(document), true);

            if (message.attachments() != null) {
                for (var attachment : message.attachments()) {
                    if (attachment == null) {
                        continue;
                    }
                    var attachmentContent = attachment.getContent();
                    if (attachmentContent == null) continue;
                    var name = attachment.getName() == null || attachment.getName().isBlank()
                            ? "Anhang"
                            : attachment.getName();
                    try (var content = attachmentContent) {
                        var resource = new ByteArrayResource(content.readAllBytes());
                        if (attachment.getContentType() == null || attachment.getContentType().isBlank()) {
                            helper.addAttachment(name, resource);
                        } else {
                            helper.addAttachment(name, resource, attachment.getContentType());
                        }
                    }
                }
            }

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException | IOException | IllegalArgumentException e) {
            throw new CommunicationException(
                    "Die E-Mail an %s konnte nicht versendet werden.".formatted(recipient),
                    e
            );
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
