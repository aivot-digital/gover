package de.aivot.GoverBackend.services.mail;

import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
public class DepartmentMembershipMailService {
    private final MailService mailService;

    @Autowired
    public DepartmentMembershipMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendAdded(KeyCloakDetailsUser inviter, KeyCloakDetailsUser user, Department department, DepartmentMembership membership) throws MessagingException, IOException, InvalidUserEMailException {
        send(inviter, user, department, membership, MailTemplate.DepartmentMembershipAdded);
    }

    public void sendRemoved(KeyCloakDetailsUser remover, KeyCloakDetailsUser user, Department department, DepartmentMembership membership) throws MessagingException, IOException, InvalidUserEMailException {
        send(remover, user, department, membership, MailTemplate.DepartmentMembershipRemoved);
    }

    private void send(KeyCloakDetailsUser admin, KeyCloakDetailsUser user, Department department, DepartmentMembership membership, MailTemplate template) throws MessagingException, IOException, InvalidUserEMailException {
        if (user.getEmail() == null) {
            // Ignore users without email
            return;
        }

        var title = "Ihre Mitgliedschaft im Fachbereich \"" + department.getName() + "\"";
        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("user", user);
        context.put("admin", admin);
        context.put("department", department);
        context.put("membership", membership);

        mailService.sendMailToUser(
                user,
                "[Gover] " + title,
                template,
                context
        );
    }
}