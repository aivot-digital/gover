package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.department.services.OrganizationalUnitService;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
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
    private final OrganizationalUnitService organizationalUnitService;

    @Autowired
    public FormMailService(MailService mailService, OrganizationalUnitService organizationalUnitService) {
        this.mailService = mailService;
        this.organizationalUnitService = organizationalUnitService;
    }

    public void sendAdded(UserEntity triggeringUser, FormVersionWithDetailsEntity form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        departmentsToNotify.add(form.getDevelopingDepartmentId());
        var title = "Ein neues Formular wurde erstellt";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormAdded);
    }

    public void sendPublished(UserEntity triggeringUser, FormVersionWithDetailsEntity form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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

    public void sendRevoked(UserEntity triggeringUser, FormVersionWithDetailsEntity form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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

    public void sendDeleted(UserEntity triggeringUser, FormEntity form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        departmentsToNotify.add(form.getDevelopingOrganizationalUnitId());
        var title = "Ein bestehendes Formular wurde gelöscht";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormDeletedAll);
    }

    public void sendDeleted(UserEntity triggeringUser, FormVersionWithDetailsEntity form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Set<Integer> departmentsToNotify = new HashSet<>();
        departmentsToNotify.add(form.getDevelopingDepartmentId());
        var title = "Ein bestehendes Formular wurde gelöscht";
        send(triggeringUser, title, departmentsToNotify, form, MailTemplate.FormDeleted);
    }

    private void send(UserEntity triggeringUser, String title, Set<Integer> departmentIds, FormVersionWithDetailsEntity form, MailTemplate template) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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

    private void send(UserEntity triggeringUser, String title, Set<Integer> departmentIds, FormEntity form, MailTemplate template) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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

    private void addDepartmentsToContext(FormVersionWithDetailsEntity form, Map<String, Object> context) {
        if (form.getDevelopingDepartmentId() != null) {
            organizationalUnitService.retrieve(form.getDevelopingDepartmentId())
                    .ifPresent(dept -> context.put("developingDepartment", dept));
        }
        if (form.getResponsibleDepartmentId() != null) {
            organizationalUnitService.retrieve(form.getResponsibleDepartmentId())
                    .ifPresent(dept -> context.put("responsibleDepartment", dept));
        }
        if (form.getManagingDepartmentId() != null) {
            organizationalUnitService.retrieve(form.getManagingDepartmentId())
                    .ifPresent(dept -> context.put("managingDepartment", dept));
        }
    }

    private void addDepartmentsToContext(FormEntity form, Map<String, Object> context) {
        if (form.getDevelopingOrganizationalUnitId() != null) {
            organizationalUnitService.retrieve(form.getDevelopingOrganizationalUnitId())
                    .ifPresent(dept -> context.put("developingDepartment", dept));
        }
    }

}