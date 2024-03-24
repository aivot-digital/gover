package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.mail.ExceptionMailService;
import de.aivot.GoverBackend.mail.SubmissionMailService;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@RestController
public class SubmissionController {
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final SubmissionStorageService submissionStorageService;
    private final DestinationSubmitService destinationSubmitService;
    private final SubmissionMailService submissionMailService;
    private final ExceptionMailService exceptionMailService;

    public SubmissionController(
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            SubmissionStorageService submissionStorageService,
            DestinationSubmitService destinationSubmitService,
            SubmissionMailService submissionMailService,
            ExceptionMailService exceptionMailService
    ) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.submissionStorageService = submissionStorageService;
        this.destinationSubmitService = destinationSubmitService;
        this.submissionMailService = submissionMailService;
        this.exceptionMailService = exceptionMailService;
    }


    @GetMapping("/api/submissions")
    public Collection<SubmissionListProjection> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, name = "formId") Integer formId,
            @RequestParam(required = false, name = "formVersion") String formVersion,
            @RequestParam(required = false, name = "includeArchived") Boolean includeArchived,
            @RequestParam(required = false, name = "includeTest") Boolean includeTest,
            @RequestParam(required = false, name = "assigneeId") String assigneeId
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            return List.of();
        }

        Specification<SubmissionListProjection> spec = Specification
                .where(((root, query, cb) -> root.get("formId").in(formIds)));

        if (formId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("formId"), formId));
        }

        if (formVersion != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("formVersion"), formVersion));
        }

        if (!Boolean.TRUE.equals(includeArchived)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("archived")));
        }

        if (!Boolean.TRUE.equals(includeTest)) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("isTestSubmission")));
        }

        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId));
        }

        return submissionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "created"));
    }

    @GetMapping("/api/submissions/{id}")
    public Submission retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        return submissionRepository
                .findByIdAndFormIdIn(id, formIds)
                .orElseThrow(NotFoundException::new);
    }

    @GetMapping("/api/submissions/{id}/attachments")
    public Collection<SubmissionAttachment> listAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        var sub = submissionRepository
                .findByIdAndFormIdIn(id, formIds)
                .orElseThrow(NotFoundException::new);

        return submissionAttachmentRepository
                .findAllBySubmissionId(sub.getId());
    }

    @PutMapping("/api/submissions/{id}")
    public Submission update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody @Valid Submission update
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        var sub = submissionRepository
                .findByIdAndFormIdIn(id, formIds)
                .orElseThrow(NotFoundException::new);

        if (sub.getArchived() != null) {
            throw new ConflictException();
        }

        var previousAssigneeId = sub.getAssigneeId();

        sub.setAssigneeId(update.getAssigneeId());
        if (sub.getAssigneeId() != null && sub.getAssigneeId().isBlank()) {
            sub.setAssigneeId(null);
        }

        sub.setFileNumber(update.getFileNumber());
        if (sub.getFileNumber() != null && sub.getFileNumber().isBlank()) {
            sub.setFileNumber(null);
        }

        sub.setArchived(update.getArchived() != null ? LocalDateTime.now() : null);

        if (update.getDestinationId() == null) {
            sub.setDestinationId(null);
            sub.setDestinationSuccess(null);
            sub.setDestinationResult(null);
        }

        if (sub.getArchived() != null) {
            var form = formRepository
                    .findById(sub.getFormId());
            if (form.isPresent()) {
                try {
                    submissionMailService.sendArchived(user, form.get(), sub);
                } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
                    exceptionMailService.send(e);
                }
            }
        }

        submissionRepository.save(sub);

        if (sub.getAssigneeId() != null && !sub.getAssigneeId().equals(user.getId()) && !Objects.equals(previousAssigneeId, sub.getAssigneeId())) {
            var form = formRepository
                    .findById(sub.getFormId());
            if (form.isPresent()) {
                try {
                    submissionMailService.sendAssigned(user, sub.getAssigneeId(), form.get(), sub);
                } catch (MessagingException | IOException | InvalidUserEMailException e) {
                    exceptionMailService.send(e);
                }
            }
        }

        return sub;
    }

    @PostMapping("/api/submissions/{id}/resend")
    public Submission resend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        var sub = submissionRepository
                .findByIdAndFormIdIn(id, formIds)
                .orElseThrow(NotFoundException::new);

        if (sub.getDestinationId() == null || sub.getDestinationSuccess()) {
            throw new ConflictException();
        }

        if (sub.getArchived() != null) {
            throw new ConflictException();
        }

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        var attachments = submissionAttachmentRepository
                .findAllBySubmissionId(sub.getId());

        try {
            var result = destinationSubmitService.handleSubmit(
                    form,
                    sub,
                    attachments
            );
            sub.setArchived(result.ok() ? LocalDateTime.now() : null);
            sub.setDestinationSuccess(result.ok());
            sub.setDestinationResult(result.message());
            sub.setFileNumber(result.fileNumber());
            sub.setDestinationTimestamp(LocalDateTime.now());
            return submissionRepository.save(sub);
        } catch (Exception e) {
            sub.setArchived(null);
            sub.setDestinationSuccess(false);
            sub.setDestinationResult(e.getMessage());
            sub.setFileNumber(null);
            sub.setDestinationTimestamp(LocalDateTime.now());
            return submissionRepository.save(sub);
        }
    }

    @GetMapping("/api/submissions/{id}/print")
    public ResponseEntity<Resource> getPrint(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        var sub = submissionRepository
                .findByIdAndFormIdIn(id, formIds)
                .orElseThrow(NotFoundException::new);

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
            throw new NotFoundException();
        }

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        // Create content disposition
        ContentDisposition contentDisposition = ContentDisposition
                .builder("inline")
                .filename(form.getTitle() + ".pdf")
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

    @GetMapping("/api/submissions/{submissionId}/attachments/{id}")
    public ResponseEntity<Resource> getAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String submissionId,
            @PathVariable String id
    ) {
        var formIds = findAccessibleFormIds(jwt);
        if (formIds.isEmpty()) {
            throw new NotFoundException();
        }

        var sub = submissionRepository
                .findByIdAndFormIdIn(submissionId, formIds)
                .orElseThrow(NotFoundException::new);

        var att = submissionAttachmentRepository
                .findByIdAndSubmissionId(id, submissionId)
                .orElseThrow(NotFoundException::new);

        // Get the path to the attachment
        Path path = submissionStorageService
                .getSubmissionAttachmentPath(sub.getId(), att.getId());

        // Get pointer to the attachment resource
        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Check resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException();
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

    private Collection<Integer> findAccessibleFormIds(Jwt jwt) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);
        return formRepository
                .findAccessibleFormIds(user.getId())
                .stream()
                .toList();
    }
}
