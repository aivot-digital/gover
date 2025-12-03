package de.aivot.GoverBackend.mail.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.services.DestinationDataFormatter;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

@Component
public class SubmissionMailService {
    private final MailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final UserService userService;
    private final PdfService pdfService;
    private final PaymentTransactionRepository paymentTransactionService;
    private final PaymentProviderRepository paymentProviderService;
    private final FormVersionService formVersionService;

    @Autowired
    public SubmissionMailService(
            MailService mailService,
            SubmissionStorageService submissionStorageService,
            UserService userService,
            PdfService pdfService,
            PaymentTransactionRepository paymentTransactionService,
            PaymentProviderRepository paymentProviderService, FormVersionService formVersionService) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.userService = userService;
        this.pdfService = pdfService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderService = paymentProviderService;
        this.formVersionService = formVersionService;
    }

    public void sendToDestination(VFormVersionWithDetailsEntity form, Submission submission, Destination destination, Collection<SubmissionAttachment> attachments) throws MessagingException, IOException, ResponseException {
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, FormPdfScope.Staff);
        } catch (InterruptedException | URISyntaxException | ResponseException e) {
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

        var paymentTransaction = submission.getPaymentTransactionKey() != null ?
                paymentTransactionService.findById(submission.getPaymentTransactionKey()).orElse(null) :
                null;

        var paymentProvider = paymentTransaction != null ?
                paymentProviderService.findById(paymentTransaction.getPaymentProviderKey()).orElse(null) :
                null;

        var destinationData = DestinationDataFormatter.createDataWithoutFiles(form, submission, paymentTransaction, paymentProvider).format();
        var destinationDataBytes = new ObjectMapper().writeValueAsBytes(destinationData);
        attachmentsData.add(new MailAttachmentBytes("Antrag.json", MediaType.APPLICATION_JSON, destinationDataBytes));

        var departmentTheme = formVersionService
                .getFormThemesInOrderOfImportance(form.getFormId(), form.getVersion());

        mailService.sendMail(
                departmentTheme.getFirst(),
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

    public void sendDestinationFailed(VFormVersionWithDetailsEntity form, Submission submission, Destination destination) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        var title = "Übertragung an Schnittstelle fehlgeschlagen";
        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("destination", destination);

        mailService.sendMailToDepartment(
                form.getDevelopingDepartmentId(),
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionDestinationFailed,
                mailData,
                new HashSet<>()
        );
    }

    public void sendPaymentFailed(VFormVersionWithDetailsEntity form, Submission submission, PaymentTransactionEntity paymentTransactionEntity, PaymentProviderEntity paymentProviderEntity) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
        Integer departmentToNotify;
        if (form.getManagingDepartmentId() != null) {
            departmentToNotify = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentToNotify = form.getResponsibleDepartmentId();
        } else {
            departmentToNotify = form.getDevelopingDepartmentId();
        }

        var title = "Abwicklung einer Zahlung fehlgeschlagen";
        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("paymentTransaction", paymentTransactionEntity);
        mailData.put("paymentProvider", paymentProviderEntity);

        mailService.sendMailToDepartment(
                departmentToNotify,
                "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                MailTemplate.SubmissionPaymentFailed,
                mailData,
                new HashSet<>()
        );
    }

    public void sendReceived(VFormVersionWithDetailsEntity form, Submission submission) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException {
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

    public void sendArchived(UserEntity triggeringUser, VFormVersionWithDetailsEntity form, Submission submission) throws MessagingException, IOException, NoValidUserEMailsInDepartmentException, ResponseException, InvalidUserEMailException {
        String title = "Ein Antrag wurde abgeschlossen";
        String fileNumber = getFileNumber(submission);

        UserEntity assignee = null;
        if (submission.getAssigneeId() != null) {
            if (submission.getAssigneeId().equals(triggeringUser.getId())) {
                return;
            }

            assignee = userService
                    .retrieve(submission.getAssigneeId())
                    .orElse(null);
        }

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("fileNumber", fileNumber);
        mailData.put("triggeringUser", triggeringUser);
        mailData.put("assignee", assignee);

        var formTheme = formVersionService
                .getFormThemesInOrderOfImportance(form.getFormId(), form.getVersion())
                .getFirst();

        if (assignee != null) {
            mailService.sendMailToUser(
                    formTheme,
                    assignee.getId(),
                    "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                    MailTemplate.SubmissionArchived,
                    mailData
            );
        } else {
            Integer departmentToNotify;
            if (form.getManagingDepartmentId() != null) {
                departmentToNotify = form.getManagingDepartmentId();
            } else if (form.getResponsibleDepartmentId() != null) {
                departmentToNotify = form.getResponsibleDepartmentId();
            } else {
                departmentToNotify = form.getDevelopingDepartmentId();
            }

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
    }

    public void sendAssigned(
            UserEntity triggeringUser,
            UserEntity newAssignee,
            VFormVersionWithDetailsEntity form,
            Submission submission,
            boolean isReassignment,
            @Nullable UserEntity previousAssignee
    ) throws MessagingException, IOException, InvalidUserEMailException, ResponseException {
        String title = isReassignment
                ? "Ein Antrag wurde neu zugewiesen"
                : "Ihnen wurde ein Antrag zugewiesen";
        String fileNumber = getFileNumber(submission);

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("form", form);
        mailData.put("submission", submission);
        mailData.put("fileNumber", fileNumber);
        mailData.put("triggeringUser", triggeringUser);
        mailData.put("newAssignee", newAssignee);

        if (isReassignment && previousAssignee != null) {
            mailData.put("previousAssignee", previousAssignee);
        }

        List<UserEntity> recipients = new ArrayList<>();
        if (!newAssignee.getId().equals(triggeringUser.getId())) {
            recipients.add(newAssignee);
        }
        if (isReassignment && previousAssignee != null && !previousAssignee.getId().equals(triggeringUser.getId())) {
            recipients.add(previousAssignee);
        }

        var formTheme = formVersionService
                .getFormThemesInOrderOfImportance(form.getFormId(), form.getVersion())
                .getFirst();

        for (UserEntity recipient : recipients) {
            mailService.sendMailToUser(
                    formTheme,
                    recipient.getId(),
                    "[Gover] " + (submission.getIsTestSubmission() ? "[Test] " : "") + title,
                    isReassignment ? MailTemplate.SubmissionReassigned : MailTemplate.SubmissionAssigned,
                    mailData
            );
        }
    }

    private String getFileNumber(Submission submission) {
        if (StringUtils.isNotNullOrEmpty(submission.getFileNumber())) {
            return submission.getFileNumber();
        } else {
            return "Kein Aktenzeichen vergeben";
        }
    }
}