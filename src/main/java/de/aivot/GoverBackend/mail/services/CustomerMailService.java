package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.services.PdfService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class CustomerMailService {
    private final MailService mailService;
    private final DepartmentRepository departmentRepository;
    private final FormRepository formRepository;
    private final PdfService pdfService;

    @Autowired
    public CustomerMailService(
            MailService mailService,
            DepartmentRepository departmentRepository,
            FormRepository formRepository,
            PdfService pdfService
    ) {
        this.mailService = mailService;
        this.departmentRepository = departmentRepository;
        this.formRepository = formRepository;
        this.pdfService = pdfService;
    }

    public void sendSubmissionCopy(String to, Submission submission) throws MessagingException, IOException, ResponseException {
        Form form = formRepository
                .findById(submission.getFormId())
                .orElseThrow(() -> new RuntimeException("No form " + submission.getFormId() + " found for submission " + submission.getId()));

        DepartmentEntity department;
        if (form.getManagingDepartmentId() != null) {
            department = departmentRepository
                    .findById(form.getManagingDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No managing department " + form.getManagingDepartmentId() + " found for form " + form.getId()));
        } else if (form.getResponsibleDepartmentId() != null) {
            department = departmentRepository
                    .findById(form.getResponsibleDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No responsible department " + form.getResponsibleDepartmentId() + " found for form " + form.getId()));
        } else {
            department = departmentRepository
                    .findById(form.getDevelopingDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No developing department " + form.getDevelopingDepartmentId() + " found for form " + form.getId()));
        }

        String title = "Ihre eingereichten Daten für das Formular \"" + form.getApplicationTitle() + "\"";

        var context = new HashMap<String, Object>();
        context.put("title", title);
        context.put("department", department);
        context.put("form", form);
        context.put("submission", submission);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, FormPdfScope.Citizen);
        } catch (InterruptedException | URISyntaxException | ResponseException e) {
            throw new RuntimeException(e);
        }
        MailAttachmentBytes pdfAttachment = new MailAttachmentBytes("Antrag.pdf", MediaType.APPLICATION_PDF, pdfBytes);
        List<MailAttachmentBytes> attachments = new LinkedList<>();
        attachments.add(pdfAttachment);

        mailService.sendMail(
                to,
                Optional.empty(),
                Optional.empty(),
                title,
                MailTemplate.CustomerSubmissionCopy,
                context,
                Optional.empty(),
                Optional.of(attachments)
        );
    }
}