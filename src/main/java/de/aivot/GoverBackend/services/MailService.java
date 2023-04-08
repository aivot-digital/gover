package de.aivot.GoverBackend.services;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.config.GoverConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Component
public class MailService {
    private final GoverConfig goverConfig;
    private final JavaMailSender mailSender;

    @Autowired
    public MailService(GoverConfig goverConfig, JavaMailSender mailSender) {
        this.goverConfig = goverConfig;
        this.mailSender = mailSender;
    }

    public void sendMail(String to, String subject, String textMessage, String htmlMessage) throws MessagingException, MailException, IOException {
        sendMail(to, null, null, subject, textMessage, htmlMessage, new Path[]{}, new MultipartFile[]{});
    }

    public void sendMail(String to, String subject, String textMessage, String htmlMessage, Path... attachmentPaths) throws MessagingException, MailException, IOException {
        sendMail(to, null, null, subject, textMessage, htmlMessage, attachmentPaths, new MultipartFile[]{});
    }

    public void sendMail(String to, @Nullable String cc, @Nullable String bcc, String subject, String textMessage, String htmlMessage, Path[] attachmentPaths, MultipartFile[] attachmentFiles) throws MessagingException, MailException, IOException {

        InternetAddress[] mailToList = InternetAddress.parse(to);
        InternetAddress[] mailCcList = cc != null ? InternetAddress.parse(cc) : null;
        InternetAddress[] mailBccList = bcc != null ? InternetAddress.parse(bcc) : null;

        MimeMessage message = mailSender.createMimeMessage();

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

        message.setFrom(goverConfig.getFromMail());
        message.setSubject(subject);
        message.setText(textMessage);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(htmlMessage, "text/html;charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        for (Path path : attachmentPaths) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new FileDataSource(path.toFile());
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setDisposition(Part.ATTACHMENT);
            attachmentPart.setFileName(path.getFileName().toString());
            multipart.addBodyPart(attachmentPart);
        }

        for (MultipartFile file : attachmentFiles) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(file.getInputStream(), file.getContentType());
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(file.getOriginalFilename());
            attachmentPart.setDisposition(Part.ATTACHMENT);
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);

        mailSender.send(message);
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
