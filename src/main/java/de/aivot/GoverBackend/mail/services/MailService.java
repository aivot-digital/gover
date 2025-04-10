package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.config.services.UserConfigService;
import de.aivot.GoverBackend.core.configs.LogoSystemConfigDefinition;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.services.TemplateLoaderService;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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

    private final SystemConfigService systemConfigService;
    private final DepartmentService departmentService;
    private final DepartmentMembershipService departmetMembershipService;

    private final AssetRepository assetRepository;
    private final UserService userService;
    private final UserConfigService userConfigService;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Autowired
    public MailService(
            GoverConfig goverConfig,
            JavaMailSender mailSender,
            SystemConfigService systemConfigService,
            DepartmentService departmentService,
            DepartmentMembershipService departmentMembershipService,
            AssetRepository assetRepository,
            UserService userService,
            UserConfigService userConfigService
    ) {
        this.goverConfig = goverConfig;
        this.mailSender = mailSender;
        this.systemConfigService = systemConfigService;
        this.departmentService = departmentService;
        this.departmetMembershipService = departmentMembershipService;
        this.assetRepository = assetRepository;
        this.userService = userService;
        this.userConfigService = userConfigService;
    }

    public void sendMailToDepartment(
            Integer departmentId,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        var department = departmentService
                .retrieve(departmentId)
                .orElseThrow(() -> new MessagingException("Department with ID " + departmentId + " not found"));

        sendMailToDepartment(department, subject, template, context, userIdsToIgnore);
    }

    public void sendMailToDepartment(
            DepartmentEntity department,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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
            var membershipSpec = new DepartmentMembershipFilter()
                    .setDepartmentId(department.getId());

            var memberships = departmetMembershipService
                    .list(Pageable.unpaged(), membershipSpec);

            var hasSentAtLeastOnce = false;

            for (var membership : memberships) {
                if (userIdsToIgnore != null && userIdsToIgnore.contains(membership.getUserId())) {
                    hasSentAtLeastOnce = true;
                    continue;
                }

                context.put("membership", membership);
                try {
                    var hasSent = sendMailToUser(membership.getUserId(), subject, template, context);
                    if (hasSent) {
                        hasSentAtLeastOnce = true;
                    }
                } catch (ResponseException e) {
                    logger.error("Failed to send mail to " + membership.getUserId(), e);
                }
            }

            if (!hasSentAtLeastOnce) {
                throw new NoValidUserEMailsInDepartmentException("No valid email addresses (user/team) found in department " + department.getId());
            }
        }
    }

    public void sendMailToDepartmentsById(
            Collection<Integer> departmentIds,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<DepartmentEntity> departments = new HashSet<>();

        for (Integer departmentId : departmentIds) {
            DepartmentEntity department = departmentService
                    .retrieve(departmentId)
                    .orElseThrow(() -> new MessagingException("Department with ID " + departmentId + " not found"));
            departments.add(department);
        }

        sendMailToDepartments(departments, subject, template, context, userIdsToIgnore);
    }

    public void sendMailToDepartments(
            Collection<DepartmentEntity> departments,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Set<String> userIdsToIgnore
    ) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<String> alreadyNotifiedUserIds = new HashSet<>();
        boolean hasSentAtLeastOnce = false;

        for (DepartmentEntity department : departments) {
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
                        hasSentAtLeastOnce = true;
                    }
                }
            } else {
                var membershipSpec = new DepartmentMembershipFilter()
                        .setDepartmentId(department.getId());

                var memberships = departmetMembershipService
                        .list(Pageable.unpaged(), membershipSpec);

                for (var membership : memberships) {
                    var userId = membership.getUserId();

                    if ((userIdsToIgnore != null && userIdsToIgnore.contains(userId)) || alreadyNotifiedUserIds.contains(userId)) {
                        continue;
                    }

                    context.put("membership", membership);
                    try {
                        var hasSent = sendMailToUser(userId, subject, template, context);
                        if (hasSent) {
                            alreadyNotifiedUserIds.add(userId);
                            hasSentAtLeastOnce = true;
                        }
                    } catch (ResponseException e) {
                        logger.error("Failed to send mail to " + userId, e);
                    }
                }
            }
        }

        if (!hasSentAtLeastOnce) {
            throw new NoValidUserEMailsInDepartmentException("No valid recipients found in given departments");
        }
    }

    /**
     * Send a mail to a user
     * @param userId The ID of the user
     * @param subject The subject of the mail
     * @param template The mail template
     * @param context The context for the mail template
     * @return True if the mail was sent, false if the user is deleted, inactive, has no email. If the user has disabled mail notifications, the sending will be seen as successful.
     * @throws MessagingException If the mail could not be sent
     * @throws IOException If the mail template could not be loaded
     * @throws ResponseException If the user config definition is not found
     */
    public boolean sendMailToUser(
            String userId,
            String subject,
            MailTemplate template,
            Map<String, Object> context
    ) throws MessagingException, IOException, ResponseException {
        // Retrieve the user entity
        var user = userService
                .retrieve(userId)
                .orElseThrow(() -> new MessagingException("User with id \"" + userId + "\" not found"));
        return sendMailToUser(user, subject, template, context);
    }

    /**
     * Send a mail to a user
     * @param user The user entity
     * @param subject The subject of the mail
     * @param template The mail template
     * @param context The context for the mail template
     * @return True if the mail was sent, false if the user is deleted, inactive, has no email. If the user has disabled mail notifications, the sending will be seen as successful.
     * @throws MessagingException If the mail could not be sent
     * @throws IOException If the mail template could not be loaded
     * @throws ResponseException If the user config definition is not found
     */
    public boolean sendMailToUser(
            UserEntity user,
            String subject,
            MailTemplate template,
            Map<String, Object> context
    ) throws MessagingException, IOException, ResponseException {
        // Check if the user is deleted in the IDP
        if (user.getDeletedInIdp()) {
            return false;
        }

        // Check if the user is inactive
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }

        // Check if the mail of the user is not verified
        if (!Boolean.TRUE.equals(user.getVerified())) {
            return false;
        }

        // Check if the user has an email address
        if (StringUtils.isNullOrEmpty(user.getEmail())) {
            return false;
        }

        if (template.getUserConfigKey() != null) {
            var def = userConfigService
                    .getDefinition(template.getUserConfigKey())
                    .orElseThrow(() -> ResponseException.internalServerError("User config definition not found: " + template.getUserConfigKey()));

            var config = userConfigService
                    .retrieve(template.getUserConfigKey(), user.getId());

            var parsedValue = def
                    .parseValueFromDB(config.getValue());

            if (parsedValue == null || (parsedValue instanceof Collection<?> list && !list.contains("mail"))) {
                // User has disabled mail notifications
                return true;
            }
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

        return true;
    }

    public void sendMail(
            String to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            MailTemplate template,
            Map<String, Object> context,
            Optional<Collection<Path>> attachmentPaths
    ) throws MessagingException, MailException, IOException, ResponseException {
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
    ) throws MessagingException, MailException, ResponseException {
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
    private HashMap<String, Object> createBaseContext() throws ResponseException {
        var context = new HashMap<String, Object>();

        context.put("providerName", systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue()
        );

        context.put("logoAssetKey", systemConfigService
                .retrieve(LogoSystemConfigDefinition.KEY)
                .getValue()
        );

        var logoAssetKey = context.get("logoAssetKey").toString();
        try {
            var assetKeyUUID = UUID.fromString(logoAssetKey);
            context.put("logoAssetName", assetRepository
                    .findById(assetKeyUUID.toString())
                    .map(AssetEntity::getFilename)
                    .orElse("")
            );
        } catch (Exception e) {
            context.put("logoAssetName", "");
        }

        context.put("config", goverConfig);

        return context;
    }
}
