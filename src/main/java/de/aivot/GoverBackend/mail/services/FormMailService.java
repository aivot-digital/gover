package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: Implement user notification settings and check them before sending mails

@Component
public class FormMailService {
    private final MailService mailService;
    private final DepartmentService departmentService;

    @Autowired
    public FormMailService(MailService mailService, DepartmentService departmentService) {
        this.mailService = mailService;
        this.departmentService = departmentService;
    }

    public void sendAdded(UserEntity triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        departmentsToNotify.add(form.getDevelopingDepartmentId());
        var title = "Ein neues Formular wurde erstellt";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormAdded);
    }

    public void sendPublished(UserEntity triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        if (form.getManagingDepartmentId() != null) {
            departmentsToNotify.add(form.getManagingDepartmentId());
        }
        if (form.getResponsibleDepartmentId() != null) {
            departmentsToNotify.add(form.getResponsibleDepartmentId());
        }
        departmentsToNotify.add(form.getDevelopingDepartmentId());

        var title = "Ein Formular wurde veröffentlicht";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormPublished);
    }

    public void sendRevoked(UserEntity triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        if (form.getManagingDepartmentId() != null) {
            departmentsToNotify.add(form.getManagingDepartmentId());
        }
        if (form.getResponsibleDepartmentId() != null) {
            departmentsToNotify.add(form.getResponsibleDepartmentId());
        }
        departmentsToNotify.add(form.getDevelopingDepartmentId());

        var title = "Ein Formular wurde zurückgezogen";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormRevoked);
    }

    public void sendDeleted(UserEntity triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        departmentsToNotify.add(form.getDevelopingDepartmentId());
        var title = "Ein bestehendes Formular wurde gelöscht";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormDeleted);
    }

    private void send(UserEntity triggeringUser, String title, Set<Integer> departmentIds, Form form, MailTemplate template) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("triggeringUser", triggeringUser);
        context.put("form", form);

        addDepartmentsToContext(form, context);

        var userIdsToIgnore = new HashSet<String>();
        userIdsToIgnore.add(triggeringUser.getId());

        mailService.sendMailToDepartmentsById(
                departmentIds,
                "[Gover] " + title,
                template,
                context,
                userIdsToIgnore
        );
    }

    private void addDepartmentsToContext(Form form, Map<String, Object> context) {
        if (form.getDevelopingDepartmentId() != null) {
            departmentService.retrieve(form.getDevelopingDepartmentId())
                    .ifPresent(dept -> context.put("developingDepartment", dept));
        }
        if (form.getResponsibleDepartmentId() != null) {
            departmentService.retrieve(form.getResponsibleDepartmentId())
                    .ifPresent(dept -> context.put("responsibleDepartment", dept));
        }
        if (form.getManagingDepartmentId() != null) {
            departmentService.retrieve(form.getManagingDepartmentId())
                    .ifPresent(dept -> context.put("managingDepartment", dept));
        }
    }

}