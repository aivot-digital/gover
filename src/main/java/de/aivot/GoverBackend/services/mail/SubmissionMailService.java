package de.aivot.GoverBackend.services.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.DestinationDataFormatter;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

@Component
public class SubmissionMailService {
    private final MailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final UserRepository userRepository;
    private final PdfService pdfService;

    @Autowired
    public SubmissionMailService(
            MailService mailService,
            SubmissionStorageService submissionStorageService,
            UserRepository userRepository,
            PdfService pdfService
    ) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.userRepository = userRepository;
        this.pdfService = pdfService;
    }

    public void sendToDestination(Form form, Submission submission, Destination destination, Collection<SubmissionAttachment> attachments) throws MessagingException, IOException {
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission);
        } catch (InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

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

        List<MailAttachmentBytes> attachmentsData = new LinkedList<>();
        Map<String, byte[]> attachmentBytes = new HashMap<>();

        for (var att : attachments) {
            var bytes = submissionStorageService.getAttachmentData(submission, att);
            attachmentBytes.put(att.getFilename(), bytes);

            var mailBytes = new MailAttachmentBytes(att.getFilename(), att.getMediaType(), bytes);
            attachmentsData.add(mailBytes);
        }

        attachmentsData.add(new MailAttachmentBytes("Antrag.pdf", MediaType.APPLICATION_PDF, pdfBytes));

        var destinationData = DestinationDataFormatter.create(form, submission, pdfBytes, attachmentBytes).format();
        var destinationDataBytes = new ObjectMapper().writeValueAsBytes(destinationData);
        attachmentsData.add(new MailAttachmentBytes("Antrag.json", MediaType.APPLICATION_JSON, destinationDataBytes));

        mailService.sendMail(
                to,
                cc,
                bcc,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionDestination,
                mailData,
                Optional.empty(),
                Optional.of(attachmentsData)
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
                mailData,
                new HashSet<>()
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
                mailData,
                new HashSet<>()
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

        var userIdsToIgnore = new HashSet<String>();
        userIdsToIgnore.add(triggeringUser.getId());

        mailService.sendMailToDepartment(
                departmentToNotify,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionArchived,
                mailData,
                userIdsToIgnore
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