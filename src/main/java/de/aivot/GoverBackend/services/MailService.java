package de.aivot.GoverBackend.services;

import com.oracle.truffle.js.runtime.Strings;
import de.aivot.GoverBackend.enums.MailTemplate;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;

@Component
public class MailService {
    private final GoverConfig goverConfig;
    private final JavaMailSender mailSender;
    private final SubmissionStorageService submissionStorageService;

    @Autowired
    public MailService(GoverConfig goverConfig, JavaMailSender mailSender, SubmissionStorageService submissionStorageService) {
        this.goverConfig = goverConfig;
        this.mailSender = mailSender;
        this.submissionStorageService = submissionStorageService;
    }

    public void sendApplicationCopyMail(String to, Submission submission) throws MessagingException, IOException, MailException {
        Application application = submission.getApplication();

        String title = application.getApplicationTitle();
        String subject = Strings.format("Ihre Unterlagen für %s", title).toString();

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);

        String departmentName;
        if (application.getManagingDepartment() != null) {
            departmentName = application.getManagingDepartment().getName();
        } else if (application.getResponsibleDepartment() != null) {
            departmentName = application.getResponsibleDepartment().getName();
        } else {
            departmentName = "Ihre Dienststelle";
        }
        mailData.put("department", departmentName);

        Path pdfPath = submissionStorageService.getSubmissionPdfPath(submission.getId());

        sendMail(
                to,
                Optional.empty(),
                Optional.empty(),
                subject,
                MailTemplate.CustomerMail,
                mailData, Optional.of(List.of(pdfPath))
        );
    }

    public void sendDestinationMail(Submission submission, Collection<SubmissionAttachment> attachments) throws MessagingException, MailException, IOException {
        Application application = submission.getApplication();
        Destination destination = submission.getDestination();

        Path pdfPath = submissionStorageService.getSubmissionPdfPath(submission.getId());

        String title = application.getApplicationTitle();

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);

        String subject = Strings.format("Neuer Antrag: %s", title).toString();

        String to = destination.getMailTo();
        Optional<String> cc = destination.getMailCC() != null ? Optional.of(destination.getMailCC()) : Optional.empty();
        Optional<String> bcc = destination.getMailBCC() != null ? Optional.of(destination.getMailBCC()) : Optional.empty();

        List<Path> attachmentPaths = new LinkedList<>();
        attachmentPaths.add(pdfPath);
        for (var att : attachments) {
            attachmentPaths.add(submissionStorageService.getSubmissionAttachmentPath(submission.getId(), att.getId()));
        }

        sendMail(
                to,
                cc,
                bcc,
                subject,
                MailTemplate.DestinationMail,
                mailData,
                Optional.of(attachmentPaths)
        );
    }

    public void sendExceptionMail(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString();

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("message", exception.getMessage());
        mailData.put("stackTrace", sStackTrace);

        String subject = "Fehler im Betrieb";

        try {
            sendMail(
                    goverConfig.getReportMail(),
                    Optional.empty(),
                    Optional.empty(),
                    subject,
                    MailTemplate.SystemExceptionMail,
                    mailData,
                    Optional.empty()
            );
        } catch (MessagingException | MailException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendUserCreatedEmail(String email, String password, String to) {
        Map<String, Object> mailData = new HashMap<>();
        mailData.put("email", email);
        mailData.put("password", password);
        mailData.put("hostname", goverConfig.getHostname());

        try {
            sendMail(
                    to,
                    Optional.empty(),
                    Optional.empty(),
                    "Zugangsdaten für Ihr neues Nutzerkonto",
                    MailTemplate.UserCreatedMail,
                    mailData,
                    Optional.empty()
            );
        } catch (MessagingException | MailException | IOException e) {
            sendExceptionMail(e);
        }
    }

    public void sendNewSubmissionMail(Application application, Submission submission, String to) {

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", application.getTitle());
        mailData.put("version", application.getVersion());
        mailData.put("applicationId", application.getId());
        mailData.put("submissionId", submission.getId());
        mailData.put("hostname", goverConfig.getHostname());

        try {
            sendMail(
                    to,
                    Optional.empty(),
                    Optional.empty(),
                    submission.getIsTestSubmission() ? "[Test] Neuer Online-Antrag" : "Neuer Online-Antrag",
                    MailTemplate.NewSubmissionMail,
                    mailData,
                    Optional.empty()
            );
        } catch (MessagingException | MailException | IOException e) {
            sendExceptionMail(e);
        }
    }

    public void sendTestMail(String to) throws MessagingException, IOException {
        sendMail(
                to,
                Optional.empty(),
                Optional.empty(),
                "SMTP-Test",
                MailTemplate.SystemTestMail,
                new HashMap<>(),
                Optional.empty()
        );
    }

    // region Utils

    private void sendMail(
            String to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            MailTemplate template,
            Map<String, Object> data,
            Optional<Collection<Path>> attachmentPaths
    ) throws MessagingException, MailException, IOException {
        InternetAddress[] mailToList = InternetAddress.parse(to);

        MimeMessage message = mailSender.createMimeMessage();

        message.setRecipients(
                Message.RecipientType.TO,
                mailToList
        );

        if (cc.isPresent()) {
            message.setRecipients(
                    Message.RecipientType.CC,
                    InternetAddress.parse(cc.get())
            );
        }
        if (bcc.isPresent()) {
            message.setRecipients(
                    Message.RecipientType.BCC,
                    InternetAddress.parse(bcc.get())
            );
        }

        String textMessage = loadTemplate(template.getKey() + "/mail.txt", data);
        String htmlMessage = loadTemplate(template.getKey() + "/mail.html", data);

        message.setFrom(goverConfig.getFromMail());
        message.setSubject(subject.replaceAll("\\r?\\n", " "), "utf-8");
        message.setText(textMessage, "utf-8");

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(htmlMessage, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        if (attachmentPaths.isPresent()) {
            for (Path path : attachmentPaths.get()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(path.toFile());
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setDisposition(Part.ATTACHMENT);
                attachmentPart.setFileName(path.getFileName().toString());
                multipart.addBodyPart(attachmentPart);
            }
        }

        message.setContent(multipart);

        mailSender.send(message);
    }

    public String loadTemplate(String template, Map<String, Object> data) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/mail/");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariables(data);

        return templateEngine.process(template, context);
    }

    // endregion
}
