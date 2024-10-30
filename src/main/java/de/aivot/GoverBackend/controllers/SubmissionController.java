package de.aivot.GoverBackend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.enums.PaymentProvider;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import de.aivot.GoverBackend.models.entities.SubmissionListProjection;
import de.aivot.GoverBackend.models.giropay.GiroPayCallbackResponse;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.services.DestinationDataFormatter;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.services.PaymentService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import de.aivot.GoverBackend.services.mail.SubmissionMailService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import static de.aivot.GoverBackend.models.giropay.GiroPayCallbackResponse.RESULT_PAYMENT_SUCCESS;

@RestController
public class SubmissionController {
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final SubmissionStorageService submissionStorageService;
    private final DestinationSubmitService destinationSubmitService;
    private final SubmissionMailService submissionMailService;
    private final ExceptionMailService exceptionMailService;
    private final PdfService pdfService;
    private final PaymentService paymentService;
    private final GoverConfig goverConfig;
    private final DestinationRepository destinationRepository;

    public SubmissionController(
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            SubmissionStorageService submissionStorageService,
            DestinationSubmitService destinationSubmitService,
            SubmissionMailService submissionMailService,
            ExceptionMailService exceptionMailService,
            PdfService pdfService,
            PaymentService paymentService,
            GoverConfig goverConfig,
            DestinationRepository destinationRepository
    ) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.submissionStorageService = submissionStorageService;
        this.destinationSubmitService = destinationSubmitService;
        this.submissionMailService = submissionMailService;
        this.exceptionMailService = exceptionMailService;
        this.pdfService = pdfService;
        this.paymentService = paymentService;
        this.goverConfig = goverConfig;
        this.destinationRepository = destinationRepository;
    }


    @GetMapping("/api/submissions")
    public Collection<SubmissionListProjection> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, name = "formId") Integer formId,
            @RequestParam(required = false, name = "formVersion") String formVersion,
            @RequestParam(required = false, name = "includeStatus") List<Integer> includeStatus,
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

        if (includeStatus != null && !includeStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("status").in(includeStatus)));
        } else {
            spec = spec
                    .and((root, query, cb) -> cb.notEqual(root.get("status"), SubmissionStatus.PaymentPending.getKey()))
                    .and((root, query, cb) -> cb.notEqual(root.get("status"), SubmissionStatus.DestinationCallbackPending.getKey()))
                    .and((root, query, cb) -> cb.notEqual(root.get("status"), SubmissionStatus.Archived.getKey()));
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
        if (sub.getAssigneeId() != null) {
            sub.setStatus(SubmissionStatus.ManualWorkingOn);
        }

        sub.setFileNumber(update.getFileNumber());
        if (sub.getFileNumber() != null && sub.getFileNumber().isBlank()) {
            sub.setFileNumber(null);
        }

        sub.setArchived(update.getArchived() != null ? LocalDateTime.now() : null);

        if (update.getDestinationId() == null && sub.getDestinationId() != null) {
            if (sub.getStatus() == SubmissionStatus.DestinationCallbackPending || sub.getStatus() == SubmissionStatus.HasDestinationError) {
                sub.setStatus(SubmissionStatus.OpenForManualWork);
            }
            sub.setDestinationId(null);
            sub.setDestinationSuccess(null);
            sub.setDestinationResult(null);
        }

        if (sub.getArchived() != null) {
            sub.setStatus(SubmissionStatus.Archived);
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

    @GetMapping("/api/public/submissions/{id}/xbezahldienste/payment-callback")
    public void paymentCallbackXBezahldienste(
            @PathVariable String id,
            HttpServletResponse response
    ) throws IOException {
        var sub = submissionRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        if (sub.getStatus() != SubmissionStatus.PaymentPending) {
            throw new BadRequestException();
        }

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        sub = paymentService.pollSinglePayment(sub);

        var url = goverConfig.createUrl("/" + form.getSlug() + "/" + form.getVersion() + "?submissionId=" + sub.getId());

        if (sub.hasExternalAccessExpired(form)) {
            url += "&externalAccessExpired=true";
        }

        if (sub.getPaymentInformation().getStatus() == XBezahldienstStatus.PAYED) {
            response.sendRedirect(url);
        } else {
            response.sendRedirect(url + "&paymentLink=" + sub.getPaymentInformation().getTransactionRedirectUrl());
        }
    }

    @GetMapping("/api/public/submissions/{id}/giropay/payment-callback")
    public void paymentCallbackGiroPay(
            @PathVariable String id,
            @RequestParam Map<String, String> params,
            HttpServletResponse response
    ) throws IOException {
        var sub = submissionRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        if (sub.getStatus() != SubmissionStatus.PaymentPending) {
            throw new BadRequestException();
        }

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        ObjectMapper objectMapper = new ObjectMapper();
        var callbackResponse = objectMapper
                .convertValue(params, GiroPayCallbackResponse.class);

        var paymentProvider = goverConfig.getPaymentProvider().get(PaymentProvider.giroPay.getKey());
        var projectPassword = paymentProvider.get("projectPassword");

        String desiredHash;
        try {
            desiredHash = GiroPayCallbackResponse.generateHash(callbackResponse, projectPassword);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        if (!desiredHash.equals(callbackResponse.getGcHash())) {
            throw new BadRequestException();
        }

        var url = goverConfig.createUrl("/" + form.getSlug() + "/" + form.getVersion() + "?submissionId=" + sub.getId());

        if (sub.hasExternalAccessExpired(form)) {
            url += "&externalAccessExpired=true";
        }

        if (RESULT_PAYMENT_SUCCESS.equals(callbackResponse.getGcResultPayment())) {
            var sub2 = paymentService
                    .storePaymentInfoForSubmission(
                            sub,
                            callbackResponse.toXBezahldienstePaymentInformation(sub.getPaymentInformation()),
                            form
                    );
            submissionRepository.save(sub2);
            response.sendRedirect(url);
        } else {
            response.sendRedirect(url + "&paymentLink=" + sub.getPaymentInformation().getTransactionRedirectUrl());
        }
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


        var destination = destinationRepository
                .findById(sub.getDestinationId())
                .orElseThrow(NotFoundException::new);

        destinationSubmitService.handleSubmit(
                destination,
                form,
                sub,
                attachments
        );

        return submissionRepository.save(sub);
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

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        // Get the path to the generated pdf
        byte[] pdfBytes = null;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, sub);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Get pointer to the pdf file resource
        Resource resource = new ByteArrayResource(pdfBytes);

        // Check resource exists and is readable
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException();
        }

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

        var bytes = submissionStorageService.getAttachmentData(sub, att);

        // Get pointer to the attachment resource
        Resource resource = new ByteArrayResource(bytes);

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

    @GetMapping("/api/submissions/{id}/destination-data")
    public Map<String, Object> getDestinationData(
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

        var form = formRepository
                .findById(sub.getFormId())
                .orElseThrow(NotFoundException::new);

        var attachments = submissionAttachmentRepository
                .findAllBySubmissionId(sub.getId());

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, sub);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Map<String, byte[]> attachmentBytes = new HashMap<>();
        for (var attachment : attachments) {
            var bytes = submissionStorageService.getAttachmentData(sub, attachment);
            attachmentBytes.put(attachment.getFilename(), bytes);
        }

        return DestinationDataFormatter
                .create(form, sub, pdfBytes, attachmentBytes)
                .format();
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
