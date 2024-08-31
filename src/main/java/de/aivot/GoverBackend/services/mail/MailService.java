package de.aivot.GoverBackend.services.mail;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.Asset;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.repositories.*;
import de.aivot.GoverBackend.services.TemplateLoaderService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Component
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final GoverConfig goverConfig;
    private final JavaMailSender mailSender;
    private final SystemConfigRepository systemConfigRepository;

    private final AssetRepository assetRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMembershipRepository departmentMembershipRepository;
    private final UserRepository userRepository;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Autowired
    public MailService(
            GoverConfig goverConfig,
            JavaMailSender mailSender,
            SystemConfigRepository systemConfigRepository,
            AssetRepository assetRepository,
            DepartmentRepository departmentRepository,
            DepartmentMembershipRepository departmentMembershipRepository,
            UserRepository userRepository
    ) {
        this.goverConfig = goverConfig;
        this.mailSender = mailSender;
        this.systemConfigRepository = systemConfigRepository;
        this.assetRepository = assetRepository;
        this.departmentRepository = departmentRepository;
        this.departmentMembershipRepository = departmentMembershipRepository;
        this.userRepository = userRepository;
    }

    public void sendMailToDepartment(
            Integer departmentId,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        var department = departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new MessagingException("Department with ID " + departmentId + " not found"));
        sendMailToDepartment(department, subject, template, context, userIdsToIgnore);
    }

    public void sendMailToDepartment(
            Department department,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        context.put("department", department);

        if (StringUtils.isNotNullOrEmpty(department.getDepartmentMail())) {
            String[] mails = department.getDepartmentMail().split(",");
            for (String mail : mails) {
                if (StringUtils.isNotNullOrEmpty(mail)) {
                    sendMail(
                            mail.trim(),
                            Optional.empty(),
                            Optional.empty(),
                            subject,
                            template,
                            context,
                            Optional.empty()
                    );
                }
            }
        } else {
            var memberships = departmentMembershipRepository
                    .findAllByDepartmentId(department.getId());

            var hasSent = false;

            for (var membership : memberships) {
                if (userIdsToIgnore != null && userIdsToIgnore.contains(membership.getUserId())) {
                    hasSent = true;
                    continue;
                }

                context.put("membership", membership);
                try {
                    sendMailToUser(membership.getUserId(), subject, template, context);
                    hasSent = true;
                } catch (InvalidUserEMailException e) {
                    logger.error("Failed to send mail to " + membership.getUserId(), e);
                }
            }

            if (!hasSent) {
                throw new NoValidUserEMailsInDepartmentException("No valid email addresses (user/team) found in department " + department.getId());
            }
        }
    }

    public void sendMailToUser(
            String userId,
            String subject,
            MailTemplate template,
            Map<String, Object> context
    ) throws MessagingException, IOException, InvalidUserEMailException {
        var user = userRepository
                .getUserAsServer(userId)
                .orElseThrow(() -> new MessagingException("User not found"));
        sendMailToUser(user, subject, template, context);
    }

    public void sendMailToUser(
            KeyCloakDetailsUser user,
            String subject,
            MailTemplate template,
            Map<String, Object> context
    ) throws MessagingException, IOException, InvalidUserEMailException {
        if (StringUtils.isNullOrEmpty(user.getEmail())) {
            throw new InvalidUserEMailException("User " + user.getId() + " has no email address");
        }
        context.put("user", user);
        sendMail(
                user.getEmail(),
                Optional.empty(),
                Optional.empty(),
                subject,
                template,
                context,
                Optional.empty()
        );
    }

    public void sendMail(
            String to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Optional<Collection<Path>> attachmentPaths
    ) throws MessagingException, MailException, IOException {
        sendMail(to, cc, bcc, subject, template, context, attachmentPaths, Optional.empty());
    }

    public void sendMail(
            String to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Optional<Collection<Path>> attachmentPaths,
            Optional<Collection<MailAttachmentBytes>> attachmentBytes
    ) throws MessagingException, MailException {
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

        context.put("base", createBaseContext());

        String textMessage = loadTemplate(template.getKey() + ".txt", context, TemplateMode.TEXT);
        String htmlMessage = loadTemplate(template.getKey() + ".html", context, TemplateMode.HTML);

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

        if (attachmentBytes.isPresent()) {
            for (var entry : attachmentBytes.get()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new ByteArrayDataSource(entry.bytes(), entry.contentType().toString());
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setDisposition(Part.ATTACHMENT);
                attachmentPart.setFileName(entry.filename());
                multipart.addBodyPart(attachmentPart);
            }
        }

        message.setContent(multipart);

        if (!mailHost.isEmpty()) {
            mailSender.send(message);
        }
    }

    private String loadTemplate(String template, Map<String, Object> data, TemplateMode mode) {
        return new TemplateLoaderService()
                .processTemplate("mail/" + template, data, mode);
    }

    // TODO: This was copied to the pdf service. Needs unification!
    private HashMap<String, Object> createBaseContext() {
        var context = new HashMap<String, Object>();

        context.put("providerName", systemConfigRepository
                .findById(SystemConfigKey.PROVIDER__NAME.getKey())
                .map(SystemConfig::getValue)
                .orElse("")
        );

        context.put("logoAssetKey", systemConfigRepository
                .findById(SystemConfigKey.SYSTEM__LOGO.getKey())
                .map(SystemConfig::getValue)
                .orElse("")
        );

        var logoAssetKey = context.get("logoAssetKey").toString();
        try {
            var assetKeyUUID = UUID.fromString(logoAssetKey);
            context.put("logoAssetName", assetRepository
                    .findById(assetKeyUUID.toString())
                    .map(Asset::getFilename)
                    .orElse("")
            );
        } catch (Exception e) {
            context.put("logoAssetName", "");
        }

        context.put("config", goverConfig);

        return context;
    }
}
