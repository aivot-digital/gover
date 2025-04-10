package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;

@Component
public class DepartmentMembershipMailService {
    private final MailService mailService;
    private final UserService userService;
    private final DepartmentService departmentService;

    @Autowired
    public DepartmentMembershipMailService(MailService mailService, UserService userService, DepartmentService departmentService) {
        this.mailService = mailService;
        this.userService = userService;
        this.departmentService = departmentService;
    }

    public void sendAdded(
            @Nonnull UserEntity inviter,
            @Nonnull DepartmentMembershipEntity membership
    ) throws MessagingException, IOException, InvalidUserEMailException, ResponseException {
        send(inviter, membership, MailTemplate.DepartmentMembershipAdded, null, null);
    }

    public void sendRemoved(
            @Nonnull UserEntity remover,
            @Nonnull DepartmentMembershipEntity membership
    ) throws MessagingException, IOException, InvalidUserEMailException, ResponseException {
        send(remover, membership, MailTemplate.DepartmentMembershipRemoved, null, null);
    }

    public void sendRoleChanged(
            @Nonnull UserEntity updater,
            @Nonnull DepartmentMembershipEntity membership,
            @Nonnull UserRole oldRole,
            @Nonnull UserRole newRole
    ) throws MessagingException, IOException, InvalidUserEMailException, ResponseException {
        if (updater.getId().equals(membership.getUserId())) {
            return;
        }

        send(updater, membership, MailTemplate.DepartmentMembershipRoleChanged, oldRole, newRole);
    }

    private void send(
            @Nonnull UserEntity admin,
            @Nonnull DepartmentMembershipEntity membership,
            @Nonnull MailTemplate template,
            @Nullable UserRole oldRole,
            @Nullable UserRole newRole
    ) throws MessagingException, IOException, InvalidUserEMailException, ResponseException {
        var user = userService
                .retrieve(membership.getUserId())
                .orElse(null);

        if (user == null || user.getEmail() == null) {
            // Ignore users without email
            return;
        }

        var department = departmentService
                .retrieve(membership.getDepartmentId())
                .orElse(null);

        if (department == null) {
            // Ignore memberships without department
            return;
        }

        var title = switch (template) {
            case DepartmentMembershipAdded -> "Ihre Mitgliedschaft im Fachbereich \"" + department.getName() + "\"";
            case DepartmentMembershipRemoved -> "Ihre Mitgliedschaft im Fachbereich \"" + department.getName() + "\" wurde beendet";
            case DepartmentMembershipRoleChanged -> "Ihre Rolle im Fachbereich \"" + department.getName() + "\" hat sich geändert";
            default -> "Benachrichtigung zu Ihrer Mitgliedschaft im Fachbereich \"" + department.getName() + "\"";
        };

        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("user", user);
        context.put("admin", admin);
        context.put("department", department);
        context.put("membership", membership);

        if (oldRole != null && newRole != null) {
            context.put("oldRole", oldRole);
            context.put("newRole", newRole);
        }

        mailService.sendMailToUser(
                user,
                "[Gover] " + title,
                template,
                context
        );
    }
}