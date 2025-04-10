package de.aivot.GoverBackend.submission.controllers;

import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.exceptions.UnauthorizedException;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.payment.services.PaymentTransactionService;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.services.DestinationDataFormatter;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.dtos.SubmissionAttachmentResponseDTO;
import de.aivot.GoverBackend.submission.filters.SubmissionAttachmentFilter;
import de.aivot.GoverBackend.submission.filters.SubmissionWithMembershipFilter;
import de.aivot.GoverBackend.submission.services.SubmissionAttachmentService;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import de.aivot.GoverBackend.submission.services.SubmissionWithMembershipService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions/{submissionId}/attachments/")
public class SubmissionAttachmentsController {
    private final FormService formService;
    private final SubmissionWithMembershipService submissionWithMembershipService;
    private final SubmissionStorageService submissionStorageService;
    private final PdfService pdfService;
    private final SubmissionAttachmentService submissionAttachmentService;
    private final SubmissionService submissionService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderService paymentProviderService;

    @Autowired
    public SubmissionAttachmentsController(
            SubmissionWithMembershipService submissionWithMembershipService,
            SubmissionStorageService submissionStorageService,
            PdfService pdfService,
            SubmissionAttachmentService submissionAttachmentService,
            SubmissionService submissionService,
            FormService formService,
            PaymentTransactionService paymentTransactionService, PaymentProviderService paymentProviderService) {
        this.formService = formService;
        this.submissionWithMembershipService = submissionWithMembershipService;
        this.submissionStorageService = submissionStorageService;
        this.pdfService = pdfService;
        this.submissionAttachmentService = submissionAttachmentService;
        this.submissionService = submissionService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderService = paymentProviderService;
    }

    @GetMapping("")
    public Page<SubmissionAttachmentResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid SubmissionAttachmentFilter filter,
            @Nonnull @PathVariable String submissionId
    ) throws ResponseException {
        checkUserHasAccess(jwt, submissionId);

        filter.setSubmissionId(submissionId);

        return submissionAttachmentService
                .list(pageable, filter)
                .map(SubmissionAttachmentResponseDTO::fromEntity);
    }

    @GetMapping("{attachmentId}/")
    public ResponseEntity<Resource> downloadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String submissionId,
            @PathVariable String attachmentId
    ) throws ResponseException {
        checkUserHasAccess(jwt, submissionId);

        var submission = submissionService
                .retrieve(submissionId)
                .orElseThrow(ResponseException::notFound);

        var attachment = submissionAttachmentService
                .retrieve(attachmentId)
                .orElseThrow(ResponseException::notFound);

        var bytes = submissionStorageService
                .getAttachmentData(submission, attachment);

        // Get pointer to the attachment resource
        Resource resource = new ByteArrayResource(bytes);

        // Check resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            throw ResponseException.notFound();
        }

        // Create content disposition
        ContentDisposition contentDisposition = ContentDisposition
                .builder("inline")
                .filename(attachment.getFilename())
                .build();

        // Respond resource
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_PDF); // TODO: Parse content type of attachment
        responseHeaders.setContentDisposition(contentDisposition);

        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(resource);
    }

    @GetMapping("gover-data.json")
    public Map<String, Object> downloadGoverDataAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String submissionId
    ) throws ResponseException {
        checkUserHasAccess(jwt, submissionId);

        return submissionService
                .retrieve(submissionId)
                .orElseThrow(ResponseException::notFound)
                .getCustomerInput();
    }

    @GetMapping("destination-data.json")
    public ResponseEntity<Map<String, Object>> downloadDestinationData(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String submissionId
    ) throws ResponseException {
        checkUserHasAccess(jwt, submissionId);

        var submission = submissionService
                .retrieve(submissionId)
                .orElseThrow(ResponseException::notFound);

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(ResponseException::notFound);

        var paymentTransaction = submission.getPaymentTransactionKey() == null ?
                null :
                paymentTransactionService.retrieve(submission.getPaymentTransactionKey()).orElse(null);

        var paymentProvider = paymentTransaction == null ?
                null :
                paymentProviderService.retrieve(paymentTransaction.getPaymentProviderKey()).orElse(null);

        var attachmentFilter = SubmissionAttachmentFilter
                .create()
                .setSubmissionId(submissionId);

        var attachments = submissionAttachmentService
                .list(null, attachmentFilter);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, FormPdfScope.Staff);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Map<String, byte[]> attachmentBytes = new HashMap<>();
        for (var attachment : attachments) {
            var bytes = submissionStorageService.getAttachmentData(submission, attachment);
            attachmentBytes.put(attachment.getFilename(), bytes);
        }

        var data = DestinationDataFormatter
                .create(form, submission, paymentTransaction, paymentProvider, pdfBytes, attachmentBytes)
                .format();

        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @GetMapping("ausdruck.pdf")
    public ResponseEntity<Resource> downloadPDF(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String submissionId,
            @Nullable @RequestParam(required = false) FormPdfScope scope
    ) throws ResponseException {
        checkUserHasAccess(jwt, submissionId);

        var submission = submissionService
                .retrieve(submissionId)
                .orElseThrow(ResponseException::notFound);

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(ResponseException::notFound);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, scope != null ? scope : FormPdfScope.Staff);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Get pointer to the attachment resource
        Resource resource = new ByteArrayResource(pdfBytes);

        // Check resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            throw ResponseException.notFound();
        }

        // Create content disposition
        ContentDisposition contentDisposition = ContentDisposition
                .builder("inline")
                .filename(form.getFormTitle().replaceAll("[^\\w\\d-_]+", "") + ".pdf")
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

    private void checkUserHasAccess(
            @Nullable Jwt jwt,
            @Nonnull String submissionId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getGlobalAdmin()) {
            var submissionWithMembershipSpecification = SubmissionWithMembershipFilter
                    .create()
                    .setId(submissionId)
                    .setUserId(user.getId())
                    .build();

            if (!submissionWithMembershipService.exists(submissionWithMembershipSpecification)) {
                throw ResponseException.forbidden("Only admins or members of the related department can access this submissions attachments.");
            }
        }
    }
}
