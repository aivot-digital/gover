package de.aivot.GoverBackend.mail;

import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component
public class CustomerMailService {
    private final MailService mailService;
    private final DepartmentRepository departmentRepository;
    private final FormRepository formRepository;
    private final SubmissionStorageService submissionStorageService;

    @Autowired
    public CustomerMailService(MailService mailService, DepartmentRepository departmentRepository, FormRepository formRepository, SubmissionStorageService submissionStorageService) {
        this.mailService = mailService;
        this.departmentRepository = departmentRepository;
        this.formRepository = formRepository;
        this.submissionStorageService = submissionStorageService;
    }

    public void sendSubmissionCopy(String to, Submission submission) throws MessagingException, IOException {
        Form form = formRepository
                .findById(submission.getFormId())
                .orElseThrow(() -> new RuntimeException("No form " + submission.getFormId() + " found for submission " + submission.getId()));

        Department department;
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

        Path pdfPath = submissionStorageService.getSubmissionPdfPath(submission.getId());

        mailService.sendMail(
                to,
                Optional.empty(),
                Optional.empty(),
                title,
                MailTemplate.CustomerSubmissionCopy,
                context,
                Optional.of(List.of(pdfPath))
        );
    }
}