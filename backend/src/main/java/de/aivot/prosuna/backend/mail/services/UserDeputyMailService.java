package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Component
public class UserDeputyMailService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final MailService mailService;
    private final UserService userService;
    private final SystemService systemService;

    public UserDeputyMailService(MailService mailService, UserService userService, SystemService systemService) {
        this.mailService = mailService;
        this.userService = userService;
        this.systemService = systemService;
    }

    /**
     * Tells the deputy that they were named. No consent is required for the relation to take effect, so this is
     * information only. A user who names themselves is not notified.
     */
    public void sendAssigned(@Nonnull UserEntity admin,
                             @Nonnull UserDeputyEntity relation) throws MessagingException, IOException, ResponseException {
        if (admin.getId().equals(relation.getDeputyUserId())) {
            return;
        }

        var deputy = userService.retrieve(relation.getDeputyUserId()).orElse(null);
        var original = userService.retrieve(relation.getOriginalUserId()).orElse(null);
        if (deputy == null || original == null) {
            return;
        }

        var title = "Sie wurden als Stellvertreter:in eingetragen";
        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("admin", admin);
        context.put("originalUser", original);
        context.put("periodLabel", relation.getUntilDate() == null
                ? "ab " + DATE_FORMATTER.format(relation.getFromDate()) + ", unbefristet"
                : "vom " + DATE_FORMATTER.format(relation.getFromDate()) + " bis " + DATE_FORMATTER.format(relation.getUntilDate()));
        context.put("deputiesPath", "/staff/account/deputies");

        mailService.sendMailToUser(systemService.retrieveDefaultTheme(), deputy, "[Prosuna] " + title,
                MailTemplate.DeputyAssigned, context);
    }
}
