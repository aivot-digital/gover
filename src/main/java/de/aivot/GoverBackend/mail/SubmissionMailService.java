package de.aivot.GoverBackend.mail;

import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Component
public class SubmissionMailService {
    private final MailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final UserRepository userRepository;

    @Autowired
    public SubmissionMailService(MailService mailService, SubmissionStorageService submissionStorageService, UserRepository userRepository) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.userRepository = userRepository;
    }

    public void sendToDestination(Form form, Submission submission, Destination destination, Collection<SubmissionAttachment> attachments) throws MessagingException, IOException {
        Path pdfPath = submissionStorageService.getSubmissionPdfPath(submission.getId());

        String title = "Ein neuer Antrag ist eingegangen";

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("destination", destination);
        mailData.put("hasAttachments", !attachments.isEmpty());

        String to = destination.getMailTo();
        Optional<String> cc = destination.getMailCC() != null ? Optional.of(destination.getMailCC()) : Optional.empty();
        Optional<String> bcc = destination.getMailBCC() != null ? Optional.of(destination.getMailBCC()) : Optional.empty();

        List<Path> attachmentPaths = new LinkedList<>();
        attachmentPaths.add(pdfPath);
        for (var att : attachments) {
            attachmentPaths.add(submissionStorageService.getSubmissionAttachmentPath(submission.getId(), att.getId()));
        }

        mailService.sendMail(
                to,
                cc,
                bcc,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionDestination,
                mailData,
                Optional.of(attachmentPaths)
        );
    }

    public void sendDestinationFailed(Form form, Submission submission, Destination destination) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        var title = "Übertragung an Schnittstelle fehlgeschlagen";
        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("destination", destination);

        mailService.sendMailToDepartment(
                departmentToNotify,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionDestinationFailed,
                mailData
        );
    }

    public void sendReceived(Form form, Submission submission) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        String title = "Ein neuer Antrag ist eingegangen";
        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);

        mailService.sendMailToDepartment(
                departmentToNotify,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionReceived,
                mailData
        );
    }

    public void sendArchived(KeyCloakDetailsUser triggeringUser, Form form, Submission submission) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        String title = "Ein Antrag wurde abgeschlossen";
        String fileNumber = getFileNumber(submission);

        KeyCloakDetailsUser assignee = null;
        if (submission.getAssigneeId() != null) {
            if (submission.getAssigneeId().equals(triggeringUser.getId())) {
                assignee = triggeringUser;
            } else {
                assignee = userRepository
                        .getUserAsServer(submission.getAssigneeId())
                        .orElse(null);
            }
        }

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("fileNumber", fileNumber);
        mailData.put("triggeringUser", triggeringUser);
        mailData.put("assignee", assignee);

        mailService.sendMailToDepartment(
                departmentToNotify,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionArchived,
                mailData
        );
    }

    public void sendAssigned(KeyCloakDetailsUser triggeringUser, String assigneeId, Form form, Submission submission) throws MessagingException, IOException, InvalidUserEMailException {
        String title = "Ein Antrag wurde zugewiesen";
        String fileNumber = getFileNumber(submission);

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("fileNumber", fileNumber);
        mailData.put("triggeringUser", triggeringUser);

        mailService.sendMailToUser(
                assigneeId,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionAssigned,
                mailData
        );
    }

    private String getFileNumber(Submission submission) {
        if (StringUtils.isNotNullOrEmpty(submission.getFileNumber())) {
            return submission.getFileNumber();
        } else {
            return "Kein Aktenzeichen vergeben";
        }
    }
}