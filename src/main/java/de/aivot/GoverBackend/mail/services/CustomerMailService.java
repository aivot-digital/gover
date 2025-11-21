package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitRepository;
import de.aivot.GoverBackend.department.services.OrganizationalUnitService;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.models.lib.MailAttachmentBytes;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.submission.entities.Submission;
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
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final PdfService pdfService;
    private final FormVersionWithDetailsRepository formVersionWithDetailsRepository;
    private final OrganizationalUnitService organizationalUnitService;

    @Autowired
    public CustomerMailService(MailService mailService,
                               OrganizationalUnitRepository organizationalUnitRepository,
                               PdfService pdfService,
                               FormVersionWithDetailsRepository formVersionWithDetailsRepository, OrganizationalUnitService organizationalUnitService) {
        this.mailService = mailService;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.pdfService = pdfService;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
        this.organizationalUnitService = organizationalUnitService;
    }

    public void sendSubmissionCopy(String to, Submission submission) throws MessagingException, IOException, ResponseException {
        var id = FormVersionWithDetailsEntityId
                .of(submission.getFormId(), submission.getFormVersion());

        FormVersionWithDetailsEntity form = formVersionWithDetailsRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("No form " + submission.getFormId() + " found for submission " + submission.getId()));

        OrganizationalUnitEntity department;
        if (form.getManagingDepartmentId() != null) {
            department = organizationalUnitRepository
                    .findById(form.getManagingDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No managing department " + form.getManagingDepartmentId() + " found for form " + form.getId()));
        } else if (form.getResponsibleDepartmentId() != null) {
            department = organizationalUnitRepository
                    .findById(form.getResponsibleDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No responsible department " + form.getResponsibleDepartmentId() + " found for form " + form.getId()));
        } else {
            department = organizationalUnitRepository
                    .findById(form.getDevelopingDepartmentId())
                    .orElseThrow(() -> new RuntimeException("No developing department " + form.getDevelopingDepartmentId() + " found for form " + form.getId()));
        }

        var departmentTheme = organizationalUnitService
                .getDepartmentTheme(department);

        String title = "Ihre eingereichten Daten für das Formular \"" + form.getPublicTitle() + "\"";

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
                departmentTheme,
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