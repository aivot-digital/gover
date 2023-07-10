package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.dtos.CustomerSubmissionCopyRequestDto;
import de.aivot.GoverBackend.models.dtos.SubmissionListDto;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import de.aivot.GoverBackend.repositories.ApplicationRepository;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.services.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.script.ScriptEngine;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class SubmitController {
    private final ApplicationRepository applicationRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final DepartmentMembershipRepository departmentMembershipRepository;
    private final AVService avService;
    private final PdfService pdfService;
    private final DestinationSubmitService destinationSubmitService;
    private final MailService mailService;
    private final SubmissionStorageService submissionStorageService;

    @Autowired
    public SubmitController(
            ApplicationRepository applicationRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            DepartmentMembershipRepository departmentMembershipRepository,
            AVService avService,
            PdfService pdfService,
            DestinationSubmitService destinationSubmitService,
            MailService mailService,
            SubmissionStorageService submissionStorageService) {
        this.applicationRepository = applicationRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.departmentMembershipRepository = departmentMembershipRepository;
        this.avService = avService;
        this.pdfService = pdfService;
        this.destinationSubmitService = destinationSubmitService;
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
    }

    @GetMapping("/api/public/prints/{uuid}")
    public ResponseEntity<Resource> getPrint(@PathVariable String uuid) {
        Optional<Submission> optSubmission = submissionRepository.findById(uuid);

        if (optSubmission.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        Submission submission = optSubmission.get();

        testSubmissionExpired(submission);

        // Get the path to the generated pdf
        Path path = submissionStorageService.getSubmissionPdfPath(submission.getId());

        // Get pointer to the pdf file resource
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Check resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException();
        }

        // Create content disposition
        ContentDisposition contentDisposition = ContentDisposition
                .builder("inline")
                .filename(submission.getApplication().getTitle() + ".pdf")
                .build();

        // Respond resource
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF);
        responseHeaders.setContentDisposition(contentDisposition);

        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(resource);
    }

    @PostMapping("/api/public/submit/{applicationId}")
    public SubmissionListDto submit(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @RequestParam(value = "inputs", required = true) String inputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        // Fetch application
        Optional<Application> fetchedApplication = applicationRepository.findById(applicationId);

        if (fetchedApplication.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No form with this version and slug"
            );
        }
        Application application = fetchedApplication.get();

        // Test application published or user authenticated
        if (application.getStatus() != ApplicationStatus.Published && (authentication == null || !authentication.isAuthenticated())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Requested form is not published"
            );
        }

        // Test files for viruses
        if (files != null) {
            for (var file : files) {
                if (!file.isEmpty()) {
                    boolean fileExtensionAndContentTypeClean = avService.testContentTypeAndExtension(file);
                    if (!fileExtensionAndContentTypeClean) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_ACCEPTABLE,
                                "Extension or ContentType not allowed " + file.getOriginalFilename()
                        );
                    }

                    try {
                        if (!avService.testFile(file)) {
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_ACCEPTABLE,
                                    "Virus detected in " + file.getOriginalFilename()
                            );
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to check file integrity", ex);
                    }
                }
            }
        }

        // Get Destination
        Destination destination = application.getDestination();

        if (files != null && files.length > 0 && destination != null && destination.getMaxAttachmentMegaBytes() != null) {
            long filesTotalBytes = 0;
            for (MultipartFile file : files) {
                filesTotalBytes += file.getSize();
            }
            long allowedTotalBytes = destination.getMaxAttachmentMegaBytes() * 1000 * 1000;
            if (filesTotalBytes > allowedTotalBytes) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Exceeded max allowed file size of destination"
                );
            }
        }

        // Initialize script engine
        ScriptEngine scriptEngine = ScriptService.getEngine();

        // Get customer input
        Map<String, Object> customerInput = new JSONObject(inputs).toMap();

        // Get root element
        RootElement root = application.getRoot();

        // Validate customer input
        try {
            root.validate(null, root, customerInput, scriptEngine);
        } catch (ValidationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Validation failed for field " + ex.getElement().getId() + ": " + ex.getMessage()
            );
        }

        // Create application pdf dto
        ApplicationPdfDto applicationDto = new ApplicationPdfDto(application, customerInput, scriptEngine);

        // Prepare submission directory
        String submissionId = UUID.randomUUID().toString();
        try {
            submissionStorageService.initSubmission(submissionId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Generate print
        try {
            pdfService.generatePdf(application, applicationDto, submissionId);
        } catch (IOException | InterruptedException e) {
            mailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        }

        // Create submission
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setCreated(LocalDateTime.now());
        submission.setApplication(application);
        submission.setAssignee(null);
        submission.setCustomerInput(customerInput);
        submission.setIsTestSubmission(application.getStatus() != ApplicationStatus.Published);
        if (application.getDestination() != null) {
            submission.setDestination(application.getDestination());
        }
        submissionRepository.save(submission);

        // Save attachments to submission folder
        List<SubmissionAttachment> attachments = new LinkedList<>();
        if (files != null) {
            for (var file : files) {
                if (!file.isEmpty()) {
                    String attachmentId = UUID.randomUUID().toString();
                    Path filePath = submissionStorageService.getSubmissionAttachmentPath(submissionId, attachmentId);
                    try {
                        file.transferTo(filePath);
                        file.getInputStream().close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    SubmissionAttachment attachment = new SubmissionAttachment();
                    attachment.setId(attachmentId);
                    attachment.setSubmission(submission);

                    if (file.getOriginalFilename() != null) {
                        if (file.getOriginalFilename().length() > 255) {
                            attachment.setFilename(file.getOriginalFilename().substring(0, 255));
                        } else {
                            attachment.setFilename(file.getOriginalFilename());
                        }
                    } else {
                        attachment.setFilename(submissionId);
                    }

                    attachments.add(submissionAttachmentRepository.save(attachment));
                }
            }
        }

        if (destination != null) {
            try {
                destinationSubmitService.handleSubmit(
                        submission,
                        attachments
                );
                submission.setDestinationSuccess(true);
                submission.setArchived(LocalDateTime.now());
                submissionRepository.save(submission);
            } catch (IOException | InterruptedException | MessagingException e) {
                submission.setDestinationSuccess(false);
                submission.setArchived(null);
                submissionRepository.save(submission);
                mailService.sendExceptionMail(e);
            }
        } else {
            Collection<DepartmentMembership> usersToNotify;

            if (application.getManagingDepartment() != null) {
                usersToNotify = departmentMembershipRepository
                        .findAllByDepartmentId(application.getManagingDepartment().getId());
            } else if (application.getResponsibleDepartment() != null) {
                usersToNotify = departmentMembershipRepository
                        .findAllByDepartmentId(application.getResponsibleDepartment().getId());
            } else {
                usersToNotify  = departmentMembershipRepository
                        .findAllByDepartmentId(application.getDevelopingDepartment().getId());
            }

            if (usersToNotify != null) {
                usersToNotify
                        .stream()
                        .map(DepartmentMembership::getUser)
                        .filter(User::isActive)
                        .forEach(user -> {
                            mailService.sendNewSubmissionMail(
                                    application,
                                    submission,
                                    user.getEmail()
                            );
                        });
            }
        }

        return new SubmissionListDto(submission);
    }

    @PostMapping("/api/public/send-copy/{submissionId}")
    public ResponseEntity<HttpStatus> sendCopy(@PathVariable String submissionId, @RequestBody CustomerSubmissionCopyRequestDto customerSubmissionCopyRequestDto) {
        Optional<Submission> optSubmission = submissionRepository.findById(submissionId);

        if (optSubmission.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        Submission submission = optSubmission.get();

        testSubmissionExpired(submission);

        try {
            mailService.sendApplicationCopyMail(customerSubmissionCopyRequestDto.getEmail(), submission);
        } catch (IOException e) {
            mailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    private void testSubmissionExpired(Submission submission) {
        Integer accessHours;
        if (submission.getApplication().getCustomerAccessHours() != null) {
            accessHours = submission.getApplication().getCustomerAccessHours();
        } else {
            accessHours = 4;
        }

        LocalDateTime expirationTimestamp = submission.getCreated().plusHours(accessHours);
        LocalDateTime currentTimestamp = LocalDateTime.now();
        if (currentTimestamp.isAfter(expirationTimestamp)) {
            throw new ResourceNotFoundException();
        }
    }
}
