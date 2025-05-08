package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.SubmissionMailService;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.models.PaymentTransactionChangeListener;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.submission.filters.SubmissionAttachmentFilter;
import de.aivot.GoverBackend.submission.filters.SubmissionFilter;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Component
public class SubmissionTransactionChangeListener implements PaymentTransactionChangeListener {
    private final SubmissionRepository submissionRepository;
    private final DestinationSubmitService destinationSubmitService;
    private final FormService formService;
    private final DestinationService destinationService;
    private final SubmissionAttachmentService submissionAttachmentService;
    private final SubmissionMailService submissionMailService;
    private final ExceptionMailService exceptionMailService;
    private final PaymentProviderRepository paymentProviderRepository;

    public SubmissionTransactionChangeListener(
            SubmissionRepository submissionRepository,
            DestinationSubmitService destinationSubmitService,
            FormService formService,
            DestinationService destinationService,
            SubmissionAttachmentService submissionAttachmentService,
            SubmissionMailService submissionMailService,
            ExceptionMailService exceptionMailService,
            PaymentProviderRepository paymentProviderRepository) {
        this.submissionRepository = submissionRepository;
        this.destinationSubmitService = destinationSubmitService;
        this.formService = formService;
        this.destinationService = destinationService;
        this.submissionAttachmentService = submissionAttachmentService;
        this.submissionMailService = submissionMailService;
        this.exceptionMailService = exceptionMailService;
        this.paymentProviderRepository = paymentProviderRepository;
    }

    @Override
    public void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        var submissionSpec = SubmissionFilter
                .create()
                .setPaymentTransactionKey(paymentTransactionEntity.getKey())
                .build();

        var submission = submissionRepository
                .findOne(submissionSpec)
                .orElse(null);

        if (submission == null) {
            return;
        }

        var status = paymentTransactionEntity.getPaymentInformation().getStatus();

        var form = formService
                .retrieve(submission.getFormId())
                .orElseThrow(() -> ResponseException.internalServerError("Formular mit der ID " + submission.getFormId() + " konnte nicht gefunden werden."));

        switch (status) {
            case PAYED -> {
                // Reset the copy status so the user can send the submission again after the payment
                submission.setCopySent(false);
                submission.setCopyTries(0);

                Destination destination = null;
                if (submission.getDestinationId() != null) {
                    destination = destinationService
                            .retrieve(submission.getDestinationId())
                            .orElse(null);
                }

                if (destination == null) {
                    submission.setStatus(SubmissionStatus.OpenForManualWork);
                    submissionRepository.save(submission);

                    try {
                        submissionMailService
                                .sendReceived(form, submission);
                    } catch (MessagingException | NoValidUserEMailsInDepartmentException | IOException e) {
                        throw ResponseException.internalServerError("E-Mail für Antragseingang konnte nicht versendet werden.", e);
                    }
                } else {
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
            }
            case CANCELED, FAILED -> {
                submission.setStatus(SubmissionStatus.HasPaymentError);
                submission.setCopySent(false);
                submission.setCopyTries(0);
                submissionRepository.save(submission);

                var paymentProvider = paymentProviderRepository
                        .findById(paymentTransactionEntity.getPaymentProviderKey())
                        .orElseThrow(() -> ResponseException.internalServerError("Zahlungsanbieter mit der ID " + paymentTransactionEntity.getPaymentProviderKey() + " konnte nicht gefunden werden."));

                try {
                    submissionMailService.sendPaymentFailed(form, submission, paymentTransactionEntity, paymentProvider);
                } catch (MessagingException | NoValidUserEMailsInDepartmentException | IOException e) {
                    throw ResponseException.internalServerError("E-Mail für Zahlungsfehler konnte nicht versendet werden.", e);
                }
            }
        }
    }
}
