package de.aivot.GoverBackend.submission.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.exceptions.InvalidUserEMailException;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.SubmissionMailService;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.payment.services.PaymentTransactionService;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.submission.dtos.SubmissionDetailsResponseDTO;
import de.aivot.GoverBackend.submission.dtos.SubmissionListResponseDTO;
import de.aivot.GoverBackend.submission.dtos.SubmissionRequestDTO;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.filters.SubmissionAttachmentFilter;
import de.aivot.GoverBackend.submission.filters.SubmissionWithMembershipFilter;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.submission.services.SubmissionAttachmentService;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import de.aivot.GoverBackend.submission.services.SubmissionWithMembershipService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/submissions/")
public class SubmissionController {
    private final ScopedAuditService auditService;
    private final SubmissionService submissionService;

    private final SubmissionWithMembershipService submissionWithMembershipService;
    private final DestinationSubmitService destinationSubmitService;
    private final SubmissionMailService submissionMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormService formService;
    private final DestinationService destinationService;
    private final SubmissionAttachmentService submissionAttachmentService;
    private final SubmissionRepository submissionRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final UserService userService;
    private final FormPaymentService formPaymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public SubmissionController(
            AuditService auditService,
            SubmissionService submissionService,
            SubmissionWithMembershipService submissionWithMembershipService,
            DestinationSubmitService destinationSubmitService,
            SubmissionMailService submissionMailService,
            ExceptionMailService exceptionMailService,
            FormService formService,
            DestinationService destinationService,
            SubmissionAttachmentService submissionAttachmentService,
            SubmissionRepository submissionRepository,
            PaymentTransactionService paymentTransactionService,
            UserService userService,
            FormPaymentService formPaymentService,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.auditService = auditService.createScopedAuditService(SubmissionController.class);
        this.submissionService = submissionService;
        this.submissionWithMembershipService = submissionWithMembershipService;
        this.destinationSubmitService = destinationSubmitService;
        this.submissionMailService = submissionMailService;
        this.exceptionMailService = exceptionMailService;
        this.formService = formService;
        this.destinationService = destinationService;
        this.submissionAttachmentService = submissionAttachmentService;
        this.submissionRepository = submissionRepository;
        this.paymentTransactionService = paymentTransactionService;
        this.userService = userService;
        this.formPaymentService = formPaymentService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @GetMapping("")
    public Page<SubmissionListResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid SubmissionWithMembershipFilter filter
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        filter.setUserId(user.getId());

        return submissionWithMembershipService
                .list(pageable, filter)
                .map(SubmissionListResponseDTO::fromEntity);
    }

    @GetMapping("{id}/")
    public SubmissionDetailsResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var specification = SubmissionWithMembershipFilter
                .create()
                .setId(id)
                .setUserId(user.getId())
                .build();

        return submissionWithMembershipService
                .retrieve(specification)
                .map(SubmissionDetailsResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public SubmissionDetailsResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id,
            @Nonnull @RequestBody @Valid SubmissionRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var submission = getSubmissionForUserOrThrowException(id, user);

        var previousArchived = submission
                .getArchived();

        var previousAssigneeId = submission
                .getAssigneeId();

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(ResponseException::notFound);

        var updatedSubmission = submissionService
                .update(id, requestDTO.toEntity());

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        Submission.class,
                        Map.of(
                                "submissionId", updatedSubmission.getId()
                        )
                );

        if (previousArchived == null && updatedSubmission.getArchived() != null) {
            try {
                submissionMailService.sendArchived(user, form, submission);
            } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException | InvalidUserEMailException e) {
                exceptionMailService.send(e);
            }
        }

        if (updatedSubmission.getAssigneeId() != null
            && !Objects.equals(previousAssigneeId, updatedSubmission.getAssigneeId())) {

            boolean isReassignment = previousAssigneeId != null;

            var newAssignee = userService
                    .retrieve(updatedSubmission.getAssigneeId())
                    .orElseThrow(() -> ResponseException.notFound("Neue zugewiesene Mitarbeiter:in nicht gefunden."));

            UserEntity previousAssignee;
            if (isReassignment) {
                previousAssignee = userService
                        .retrieve(previousAssigneeId)
                        .orElse(null);
            } else {
                previousAssignee = null;
            }

            try {
                submissionMailService.sendAssigned(user, newAssignee, form, submission, isReassignment, previousAssignee);
            } catch (MessagingException | IOException | InvalidUserEMailException e) {
                exceptionMailService.send(e);
            }
        }

        return SubmissionDetailsResponseDTO
                .fromEntity(updatedSubmission);
    }

    @PostMapping("{id}/resend/")
    public SubmissionDetailsResponseDTO resend(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var submission = getSubmissionForUserOrThrowException(id, user);

        if (submission.getDestinationId() == null) {
            throw ResponseException.conflict("Für den Antrag ist keine Schnittstelle hinterlegt.");
        }

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(ResponseException::notFound);

        var attachmentsFilter = SubmissionAttachmentFilter
                .create()
                .setSubmissionId(submission.getId());

        var attachments = submissionAttachmentService
                .list(null, attachmentsFilter);

        var destination = destinationService
                .retrieve(submission.getDestinationId())
                .orElseThrow(ResponseException::notFound);

        destinationSubmitService.handleSubmit(
                destination,
                form,
                submission,
                attachments.toList()
        );

        // TODO: Refactor SubmissionService update method to update destination information as well
        var updatedSubmission = submissionRepository
                .save(submission);

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        Submission.class,
                        Map.of(
                                "resend", true,
                                "destinationId", destination.getId(),
                                "submissionId", submission.getId()
                        )
                );

        return SubmissionDetailsResponseDTO
                .fromEntity(updatedSubmission);
    }

    @PostMapping("{id}/cancel-payment/")
    public SubmissionDetailsResponseDTO cancelPayment(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var submission = getSubmissionForUserOrThrowException(id, user);

        if (submission.getPaymentTransactionKey() == null) {
            throw ResponseException.conflict("Für den Antrag ist keine Zahlung hinterlegt.");
        }

        var transaction = paymentTransactionService
                .retrieve(submission.getPaymentTransactionKey())
                .orElseThrow(() -> ResponseException.conflict("Die Zahlung konnte nicht gefunden werden."));

        if (transaction.getStatus() == XBezahldienstStatus.PAYED) {
            throw ResponseException.conflict("Die Zahlung wurde bereits abgeschlossen.");
        }

        submission
                .setPaymentTransactionKey(null);

        Destination destination = null;

        if (submission.getDestinationId() != null) {
            destination = destinationService
                    .retrieve(submission.getDestinationId())
                    .orElse(null);
        }

        if (destination == null) {
            submission.setStatus(SubmissionStatus.OpenForManualWork);
            submissionRepository.save(submission);
        } else {
            var form = formService
                    .retrieve(submission.getFormId())
                    .orElseThrow(() -> new RuntimeException("Form not found"));

            var attachmentFilter = SubmissionAttachmentFilter
                    .create()
                    .setSubmissionId(submission.getId());

            var attachments = submissionAttachmentService
                    .list(attachmentFilter)
                    .getContent();

            destinationSubmitService
                    .handleSubmit(destination, form, submission, attachments);

            if (!submission.getDestinationSuccess()) {
                try {
                    submissionMailService.sendDestinationFailed(form, submission, destination);
                } catch (Exception e) {
                    exceptionMailService.send(e);
                }
            } else {
                submission.setStatus(SubmissionStatus.Archived);
            }

            submissionRepository.save(submission);
        }

        var updatedSubmission = submissionRepository.save(submission);

        paymentTransactionService
                .delete(transaction.getKey());

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        Submission.class,
                        Map.of(
                                "cancelPayment", true,
                                "transactionKey", transaction.getKey(),
                                "submissionId", submission.getId()
                        )
                );

        return SubmissionDetailsResponseDTO.fromEntity(updatedSubmission);
    }

    @PostMapping("{id}/restart-payment/")
    public SubmissionDetailsResponseDTO restartPayment(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var submission = getSubmissionForUserOrThrowException(id, user);

        if (submission.getPaymentTransactionKey() == null) {
            throw ResponseException.conflict("Für den Antrag ist keine Zahlung hinterlegt.");
        }

        var oldTransaction = paymentTransactionService
                .retrieve(submission.getPaymentTransactionKey())
                .orElseThrow(() -> ResponseException.conflict("Die Zahlung konnte nicht gefunden werden."));

        if (oldTransaction.getStatus() == XBezahldienstStatus.PAYED) {
            throw ResponseException.conflict("Die Zahlung wurde bereits abgeschlossen.");
        }

        if (oldTransaction.getStatus() == XBezahldienstStatus.INITIAL) {
            throw ResponseException.conflict("Die Zahlung ist noch nicht fehlgeschlagen.");
        }

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(() -> ResponseException.conflict("Das Formular konnte nicht gefunden werden."));

        PaymentTransactionEntity newTranscation;
        try {
            newTranscation = formPaymentService
                    .createTransaction(form, submission.getId(), submission.getCustomerInput())
                    .orElse(null);
        } catch (PaymentException e) {
            throw ResponseException.internalServerError("Die Zahlung konnte nicht neu gestartet werden.", e);
        }

        if (newTranscation == null) {
            throw ResponseException.conflict("Für das Formular " + form.getTitle() + " ergibt sich keine Zahlungsanforderung mehr. Dies kann passieren, wenn die Zahlungsmethode im Formular geändert wurde.");
        }

        submission.setPaymentTransactionKey(newTranscation.getKey());
        submission.setStatus(SubmissionStatus.Pending);
        submission = submissionRepository.save(submission);

        paymentTransactionRepository
                .delete(oldTransaction);

        auditService
                .logAction(
                        user,
                        AuditAction.Update,
                        Submission.class,
                        Map.of(
                                "restartPayment", true,
                                "oldTransactionKey", oldTransaction.getKey(),
                                "newTransactionKey", newTranscation.getKey(),
                                "submissionId", submission.getId()
                        )
                );

        return SubmissionDetailsResponseDTO
                .fromEntity(submission);
    }

    private Submission getSubmissionForUserOrThrowException(@Nonnull String id, UserEntity user) throws ResponseException {
        var submissionSpec = SubmissionWithMembershipFilter
                .create()
                .setId(id)
                .setUserId(user.getId())
                .build();

        var submission = submissionWithMembershipService
                .retrieve(submissionSpec)
                .orElseThrow(ResponseException::forbidden);

        return submission.asSubmission();
    }
}
