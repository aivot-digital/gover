package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.models.SmtpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.activation.DataHandler;
import javax.annotation.Nullable;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

@Component
public class MailService {
    @Autowired
    SmtpConfig smtpConfig;

    public void sendMail(String to, String subject, String textMessage, String htmlMessage, URL... attachmentURLs) throws MessagingException {
        sendMail(to, null, null, subject, textMessage, htmlMessage, attachmentURLs);
    }

    public void sendMail(String to, @Nullable String cc, @Nullable String bcc, String subject, String textMessage, String htmlMessage, URL... attachmentURLs) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", String.valueOf(smtpConfig.getUseTls()));
        prop.put("mail.smtp.host", smtpConfig.getHost());
        prop.put("mail.smtp.port", smtpConfig.getPort());
        prop.put("mail.smtp.ssl.trust", smtpConfig.getHost());
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.timeout", "1000");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpConfig.getUsername(), smtpConfig.getPassword());
            }
        });

        InternetAddress[] mailToList = InternetAddress.parse(to);
        InternetAddress[] mailCcList = cc != null ? InternetAddress.parse(cc) : null;
        InternetAddress[] mailBccList = bcc != null ? InternetAddress.parse(bcc) : null;

        Message message = new MimeMessage(session);

        message.setRecipients(
                Message.RecipientType.TO,
                mailToList
        );

        if (mailCcList != null) {
            message.setRecipients(
                    Message.RecipientType.CC,
                    mailCcList
            );
        }
        if (mailBccList != null) {
            message.setRecipients(
                    Message.RecipientType.BCC,
                    mailBccList
            );
        }

        message.setSubject(subject);
        message.setText(textMessage);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(htmlMessage, "text/html;charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        for (URL url : attachmentURLs) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(url));
            attachmentPart.setDisposition(Part.ATTACHMENT);
            attachmentPart.setFileName(url.getFile());
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);

        Transport.send(message);
    }

    public String loadTemplate(String templateName, Map<String, Object> data) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/mail/");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariables(data);

        return templateEngine.process(templateName, context);
    }
}
