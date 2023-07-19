package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.dtos.*;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.*;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;

@RestController
public class SubmissionController {
    private final ApplicationRepository applicationRepository;
    private final AccessibleDepartmentRepository accessibleDepartmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final UserRepository userRepository;
    private final SubmissionStorageService submissionStorageService;
    private final DestinationSubmitService destinationSubmitService;

    public SubmissionController(
            ApplicationRepository applicationRepository,
            AccessibleDepartmentRepository accessibleDepartmentRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            UserRepository userRepository, SubmissionStorageService submissionStorageService, DestinationSubmitService destinationSubmitService) {
        this.applicationRepository = applicationRepository;
        this.accessibleDepartmentRepository = accessibleDepartmentRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.userRepository = userRepository;
        this.submissionStorageService = submissionStorageService;
        this.destinationSubmitService = destinationSubmitService;
    }


    @GetMapping("/api/submissions/{applicationId}")
    public Collection<SubmissionListDto> list(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeArchived,
            @RequestParam(required = false, defaultValue = "false") Boolean includeTest,
            @RequestParam(required = false) Integer assignee
    ) {
        testPermission(authentication, applicationId);

        Collection<Submission> submissions;

        if (assignee != null) {
            if (includeArchived && includeTest) {
                submissions = submissionRepository
                        .findAllByApplicationIdAndAssigneeIdOrderByCreatedDesc(applicationId, assignee);
            }

            else if (includeArchived) {
                submissions = submissionRepository
                        .findAllByApplicationIdAndAssigneeIdAndIsTestSubmissionFalseOrderByCreatedDesc(applicationId, assignee);
            }

            else if (includeTest) {
                submissions = submissionRepository
                        .findAllByApplicationIdAndAssigneeIdAndArchivedIsNullOrderByCreatedDesc(applicationId, assignee);
            }

            else {
                submissions = submissionRepository
                        .findAllByApplicationIdAndAssigneeIdAndArchivedIsNullAndIsTestSubmissionFalseOrderByCreatedDesc(applicationId, assignee);
            }
        } else {
            if (includeArchived && includeTest) {
                submissions = submissionRepository
                        .findAllByApplicationIdOrderByCreatedDesc(applicationId);
            }

            else if (includeArchived) {
                submissions = submissionRepository
                        .findAllByApplicationIdAndIsTestSubmissionFalseOrderByCreatedDesc(applicationId);
            }

            else if (includeTest) {
                submissions = submissionRepository
                        .findAllByApplicationIdAndArchivedIsNullOrderByCreatedDesc(applicationId);
            }

            else {
                submissions = submissionRepository
                        .findAllByApplicationIdAndArchivedIsNullAndIsTestSubmissionFalseOrderByCreatedDesc(applicationId);
            }
        }

        return submissions
                .stream()
                .map(SubmissionListDto::new)
                .toList();
    }

    @GetMapping("/api/submissions/{applicationId}/{id}")
    public SubmissionDetailsDto retrieve(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String id
    ) {
        testPermission(authentication, applicationId);

        return submissionRepository
                .findByIdAndApplicationId(id, applicationId)
                .map(SubmissionDetailsDto::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/api/submissions/{applicationId}/{id}/attachments")
    public Collection<SubmissionAttachmentListDto> listAttachments(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String id
    ) {
        testPermission(authentication, applicationId);

        return submissionAttachmentRepository
                .findAllBySubmissionId(id)
                .stream()
                .map(SubmissionAttachmentListDto::valueOf)
                .toList();
    }

    @PatchMapping("/api/submissions/{applicationId}/{id}")
    public SubmissionDetailsDto update(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String id,
            @RequestBody SubmissionListDto update
    ) {
        testPermission(authentication, applicationId);

        var sub = submissionRepository
                .findByIdAndApplicationId(id, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (sub.getArchived() != null && sub.getArchived().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (update.getAssignee() != null) {
            var assignee = userRepository
                    .findById(update.getAssignee())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            sub.setAssignee(assignee);
        }
        sub.setFileNumber(update.getFileNumber());
        sub.setArchived(update.getArchived());
        submissionRepository.save(sub);

        return new SubmissionDetailsDto(sub);
    }

    @PostMapping("/api/submissions/{applicationId}/{id}/resend")
    public SubmissionDetailsDto update(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String id
    ) {
        testPermission(authentication, applicationId);

        var sub = submissionRepository
                .findByIdAndApplicationId(id, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (sub.getDestination() == null || sub.getDestinationSuccess()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (sub.getArchived() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        var attachments = submissionAttachmentRepository
                .findAllBySubmissionId(sub.getId());

        try {
            destinationSubmitService.handleSubmit(
                    sub,
                    attachments
            );
            sub.setDestinationSuccess(true);
            sub.setArchived(LocalDateTime.now());
            submissionRepository.save(sub);
        } catch (IOException | InterruptedException | MessagingException e) {
            throw new RuntimeException(e);
        }

        return new SubmissionDetailsDto(sub);
    }

    @GetMapping("/api/submissions/{applicationId}/{id}/print")
    public ResponseEntity<Resource> getPrint(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String id
    ) {
        testPermission(authentication, applicationId);

        var sub = submissionRepository
                .findByIdAndApplicationId(id, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Get the path to the generated pdf
        Path path = submissionStorageService.getSubmissionPdfPath(sub.getId());

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
                .filename(sub.getApplication().getTitle() + ".pdf")
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

    @GetMapping("/api/submissions/{applicationId}/{submissionId}/attachments/{id}")
    public ResponseEntity<Resource> getAttachment(
            Authentication authentication,
            @PathVariable Integer applicationId,
            @PathVariable String submissionId,
            @PathVariable String id
    ) {
        testPermission(authentication, applicationId);

        var sub = submissionRepository
                .findByIdAndApplicationId(submissionId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var att = submissionAttachmentRepository
                .findByIdAndSubmissionId(id, submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Get the path to the attachment
        Path path = submissionStorageService.getSubmissionAttachmentPath(sub.getId(), att.getId());

        // Get pointer to the attachment resource
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
                .filename(att.getFilename())
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

    private void testPermission(Authentication authentication, Integer applicationId) {
        var requester = (User) authentication.getPrincipal();

        if (requester.isAdmin()) {
            return;
        }

        var optApp = applicationRepository.findById(applicationId);
        if (optApp.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var app = optApp.get();

        boolean canAccess = app.getManagingDepartment() != null && accessibleDepartmentRepository
                .existsByDepartmentIdAndUserId(app.getManagingDepartment().getId(), requester.getId());
        if (canAccess) {
            return;
        }

        canAccess = app.getResponsibleDepartment() != null && accessibleDepartmentRepository
                .existsByDepartmentIdAndUserId(app.getResponsibleDepartment().getId(), requester.getId());
        if (canAccess) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
