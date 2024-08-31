package de.aivot.GoverBackend.services.mail;

import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Form;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

@Component
public class FormMailService {
    private final MailService mailService;

    @Autowired
    public FormMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendAdded(KeyCloakDetailsUser triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        var title = "Ein neues Formular wurde erstellt";
        send(triggeringUser, title, form.getDevelopingDepartmentId(), form, MailTemplate.FormAdded);
    }

    public void sendPublished(KeyCloakDetailsUser triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        var title = "Ein Formular wurde veröffentlicht";
        send(triggeringUser, title, departmentToNotify, form, MailTemplate.FormPublished);
    }

    public void sendRevoked(KeyCloakDetailsUser triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        var title = "Ein Formular wurde zurückgezogen";
        send(triggeringUser, title, departmentToNotify, form, MailTemplate.FormRevoked);
    }

    public void sendDeleted(KeyCloakDetailsUser triggeringUser, Form form) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        var title = "Ein bestehendes Formular wurde gelöscht";
        send(triggeringUser, title, form.getDevelopingDepartmentId(), form, MailTemplate.FormDeleted);
    }

    private void send(KeyCloakDetailsUser triggeringUser, String title, Integer departmentId, Form form, MailTemplate template) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("triggeringUser", triggeringUser);
        context.put("form", form);

        var userIdsToIgnore = new HashSet<String>();
        userIdsToIgnore.add(triggeringUser.getId());

        mailService.sendMailToDepartment(
                departmentId,
                "[Gover] " + title,
                template,
                context,
                userIdsToIgnore
        );
    }
}