package de.aivot.GoverBackend.submission.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.services.FormDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.data.SpecialCustomerInputKeys;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.services.FormDerivationServiceFactory;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.CustomerSubmissionCopyRequestDto;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.destination.repositories.DestinationRepository;
import de.aivot.GoverBackend.services.AVService;
import de.aivot.GoverBackend.services.DestinationSubmitService;
import de.aivot.GoverBackend.user.services.KeyCloakApiService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.mail.services.CustomerMailService;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.SubmissionMailService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.dtos.SubmissionStatusResponseDTO;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.submission.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.utils.ElementUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class SubmitController {
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final AVService avService;
    private final PdfService pdfService;
    private final DestinationSubmitService destinationSubmitService;
    private final SubmissionStorageService submissionStorageService;
    private final GoverConfig goverConfig;
    private final DestinationRepository destinationRepository;
    private final KeyCloakApiService keyCloakApiService;
    private final CustomerMailService customerMailService;
    private final SubmissionMailService submissionMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormDerivationServiceFactory formDerivationServiceFactory;
    private final FormPaymentService paymentService;
    private final PaymentProviderService paymentProviderService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderRepository paymentProviderRepository;

    @Autowired
    public SubmitController(
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            AVService avService,
            PdfService pdfService,
            DestinationSubmitService destinationSubmitService,
            SubmissionStorageService submissionStorageService,
            GoverConfig goverConfig,
            DestinationRepository destinationRepository,
            KeyCloakApiService keyCloakApiService,
            CustomerMailService customerMailService,
            SubmissionMailService submissionMailService,
            ExceptionMailService exceptionMailService,
            FormDerivationServiceFactory formDerivationServiceFactory,
            FormPaymentService paymentService,
            PaymentProviderService paymentProviderService,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentProviderRepository paymentProviderRepository
    ) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.avService = avService;
        this.pdfService = pdfService;
        this.destinationSubmitService = destinationSubmitService;
        this.submissionStorageService = submissionStorageService;
        this.goverConfig = goverConfig;
        this.destinationRepository = destinationRepository;
        this.keyCloakApiService = keyCloakApiService;
        this.customerMailService = customerMailService;
        this.submissionMailService = submissionMailService;
        this.exceptionMailService = exceptionMailService;
        this.formDerivationServiceFactory = formDerivationServiceFactory;
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProviderRepository = paymentProviderRepository;
    }

    @PostMapping("/api/public/submit/{applicationId}/")
    public Submission submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer applicationId,
            @RequestParam(value = "inputs", required = true) String inputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt);

        // Fetch form
        var form = formRepository
                .findById(applicationId)
                .orElseThrow(ResponseException::notFound);

        Destination destination = null;
        if (form.getDestinationId() != null) {
            destination = destinationRepository
                    .findById(form.getDestinationId())
                    .orElseThrow(() -> new RuntimeException("Destination with id " + form.getDestinationId() + " for form with id " + form.getId() + " not found"));
        }

        // Test form published or user authenticated
        if (form.getStatus() != FormStatus.Published && (jwt == null)) {
            throw ResponseException.forbidden();
        }

        // Get customer input
        var customerInput = new JSONObject(inputs).toMap();
        var optionalAccessToken = extractAccessToken(customerInput);
        var optionalIdp = extractIdp(customerInput);

        // Check if any authorization with an idp is required
        checkIfAnyAuthorizationIsRequiredButNotMet(form, optionalAccessToken, optionalIdp);

        // Hydrate the customer input with the data from an idp
        hydrateCustomerInputWithIdpData(form, optionalAccessToken, optionalIdp, customerInput);

        // Test files for viruses
        avService.testMultipartFiles(files);

        // Test destination attachment size
        destinationSubmitService.testDestinationAttachmentSize(destination, files);

        // Validate customer input
        var derivationResult = formDerivationServiceFactory
                .create(form, List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER))
                .derive(form.getRoot(), customerInput);
        if (derivationResult.hasErrors()) {
            throw new BadRequestException("Validation failed"); // TODO: Extend error message
        }

        // Transfer derived values to customer input
        customerInput = derivationResult.getCombinedValues();

        // Prepare submission id
        var submissionId = UUID
                .randomUUID()
                .toString();


        // Create submission
        Submission submission = new Submission();

        submission.setId(submissionId);
        submission.setFormId(form.getId());
        submission.setStatus(SubmissionStatus.Pending);
        submission.setCreated(LocalDateTime.now());
        submission.setUpdated(LocalDateTime.now());
        submission.setAssigneeId(null);
        submission.setCustomerInput(customerInput);
        submission.setIsTestSubmission(form.getStatus() != FormStatus.Published || jwt != null);
        submission.setCopySent(false);
        submission.setCopyTries(0);
        submission.setDestinationId(destination != null ? destination.getId() : null);

        submissionRepository.save(submission);

        // Save attachments to submission folder
        List<SubmissionAttachment> attachments = new LinkedList<>();
        if (files != null) {
            for (var file : files) {
                // Skip empty files
                if (file.isEmpty()) {
                    continue;
                }

                var attachment = new SubmissionAttachment();
                attachment.setId(UUID.randomUUID().toString());
                attachment.setSubmissionId(submission.getId());
                attachment.setContentType(file.getContentType());
                attachment.setType("customer");

                if (file.getOriginalFilename() != null) {
                    if (file.getOriginalFilename().length() > 255) {
                        attachment.setFilename(file.getOriginalFilename().substring(0, 255));
                    } else {
                        attachment.setFilename(file.getOriginalFilename());
                    }
                } else {
                    attachment.setFilename(submissionId);
                }

                try {
                    submissionStorageService
                            .saveAttachment(submission, attachment, file.getBytes());
                } catch (Exception e) {
                    submissionRepository.delete(submission);
                    throw new RuntimeException(e);
                }

                attachments.add(submissionAttachmentRepository.save(attachment));
            }
        }

        Optional<PaymentTransactionEntity> paymentRequest = Optional.empty();
        // If a payment provider is set, create a transaction
        if (form.getPaymentProvider() != null) {
            try {
                paymentRequest = paymentService
                        .createTransaction(form, submissionId, customerInput);
            } catch (PaymentException e) {
                // If the payment creation failed, delete the submission and throw an exception
                submissionRepository.delete(submission);
                throw ResponseException.internalServerError("Der Antrag konnte nicht eingereicht werden, da es ein Problem mit der Zahlung gab.", e);
            }
        }

        if (paymentRequest.isPresent()) {
            submission.setPaymentTransactionKey(paymentRequest.get().getKey());
            submission.setStatus(SubmissionStatus.Pending);
        } else {
            if (destination != null) {
                submission.setStatus(SubmissionStatus.Pending);

                destinationSubmitService.handleSubmit(
                        destination,
                        form,
                        submission,
                        attachments
                );

                if (!submission.getDestinationSuccess()) {
                    try {
                        submissionMailService.sendDestinationFailed(form, submission, destination);
                    } catch (Exception e) {
                        exceptionMailService.send(e);
                    }
                } else {
                    submission.setStatus(SubmissionStatus.Archived);
                }
            } else {
                submission.setStatus(SubmissionStatus.OpenForManualWork);
                try {
                    submissionMailService.sendReceived(form, submission);
                } catch (Exception e) {
                    exceptionMailService.send(e);
                }
            }
        }

        submissionRepository.save(submission);

        var submissionCopy = new Submission();
        submissionCopy.setId(submission.getId());

        return submissionCopy;
    }

    private void hydrateCustomerInputWithIdpData(Form form, Optional<String> optionalAccessToken, Optional<Idp> optionalIdp, Map<String, Object> customerInput) {
        if (optionalAccessToken.isEmpty() || optionalIdp.isEmpty()) {
            return;
        }

        RSAPublicKey publicKey;
        try {
            publicKey = keyCloakApiService.getCustomerRealmPublicKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var algo = Algorithm.RSA256(publicKey);
        var verifier = JWT
                .require(algo)
                .acceptLeeway(60 * 60 * 4) // 4h valid
                .build();
        DecodedJWT decodedJWT;
        try {
            decodedJWT = verifier.verify(optionalAccessToken.get());
        } catch (Exception e) {
            throw new BadRequestException("Access token is not valid");
        }

        var claims = decodedJWT.getClaims();
        var flatElements = ElementUtils.flattenElements(form.getRoot());
        for (var element : flatElements) {
            var metadata = element.getMetadata();
            if (metadata != null) {
                Object idMappingRaw = optionalIdp
                        .get()
                        .extractFromMetadata(metadata);

                if (idMappingRaw instanceof String idMapping) {
                    var idClaim = claims.get(idMapping);
                    if (idClaim != null && !idClaim.isNull()) {
                        var idValue = idClaim.asString();
                        if (StringUtils.isNotNullOrEmpty(idValue)) {
                            // TODO: Check idp type and format date for birthdays
                            customerInput.put(element.getId(), idValue);
                        }
                    }
                }
            }
        }

        var rawIdData = customerInput.get(SpecialCustomerInputKeys.IdCustomerInputKey);
        if (rawIdData instanceof HashMap<?, ?> idData) {
            var rawUserInfo = idData.get(SpecialCustomerInputKeys.UserInfoKey);
            if (rawUserInfo instanceof HashMap<?, ?> userInfo) {
                var typedUserInfo = (HashMap<String, Object>) userInfo;
                for (var claim : claims.entrySet()) {
                    typedUserInfo.put(claim.getKey(), claim.getValue().asString());
                }
            }
        }
    }

    private static void checkIfAnyAuthorizationIsRequiredButNotMet(Form form, Optional<String> optionalAccessToken, Optional<Idp> optionalIdp) {
        var isBayernIdRequired = form.getBayernIdEnabled() && BayernIdAccessLevel.OPTIONAL.isLowerThan(form.getBayernIdLevel());
        var isBundIdRequired = form.getBundIdEnabled() && BundIdAccessLevel.OPTIONAL.isLowerThan(form.getBundIdLevel());
        var isMukRequired = form.getMukEnabled() && MukAccessLevel.OPTIONAL.isLowerThan(form.getMukLevel());
        var isShIdRequired = form.getShIdEnabled() && SchleswigHolsteinIdAccessLevel.OPTIONAL.isLowerThan(form.getShIdLevel());

        if ((isBayernIdRequired || isBundIdRequired || isMukRequired || isShIdRequired) && (optionalAccessToken.isEmpty() || optionalIdp.isEmpty())) {
            throw new BadRequestException("A service account is required");
        }
    }

    @PostMapping("/api/public/send-copy/{submissionId}/")
    public ResponseEntity<HttpStatus> sendCopy(@PathVariable String submissionId, @RequestBody CustomerSubmissionCopyRequestDto customerSubmissionCopyRequestDto) throws ResponseException {
        var submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(ResourceNotFoundException::new);

        var submissionExpired = testSubmissionExpired(submission);
        if (submissionExpired) {
            throw ResponseException.forbidden();
        }

        if (submission.getCopySent()) {
            throw new NotAcceptableException();
        }

        if (submission.getCopyTries() >= goverConfig.getMaxSubmissionCopyRetryCount()) {
            throw new ConflictException();
        }

        try {
            customerMailService.sendSubmissionCopy(customerSubmissionCopyRequestDto.getEmail(), submission);
            submission.setCopySent(true);
            submissionRepository.save(submission);
        } catch (MailException | MessagingException e) {
            submission.setCopyTries(submission.getCopyTries() + 1);
            submissionRepository.save(submission);
            throw new BadRequestException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/api/public/rate/{submissionId}/")
    public ResponseEntity<HttpStatus> rate(@PathVariable String submissionId, @RequestParam(required = true) Integer score) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setReviewScore(score);
            submissionRepository.save(submission);
        });

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/api/public/status/{submissionId}/")
    public SubmissionStatusResponseDTO status(
            @PathVariable String submissionId
    ) {
        var sub = submissionRepository
                .findById(submissionId)
                .orElseThrow(ResourceNotFoundException::new);

        Optional<PaymentTransactionEntity> paymentTransaction = Optional.empty();
        if (sub.getPaymentTransactionKey() != null) {
            paymentTransaction = paymentTransactionRepository
                    .findById(sub.getPaymentTransactionKey());
        }

        Optional<PaymentProviderEntity> paymentProvider = Optional.empty();
        if (paymentTransaction.isPresent()) {
            paymentProvider = paymentProviderRepository
                    .findById(paymentTransaction.get().getPaymentProviderKey());
        }

        Optional<PaymentProviderDefinition> paymentProviderDefinition = Optional.empty();
        if (paymentProvider.isPresent()) {
            paymentProviderDefinition = paymentProviderService
                    .getProviderDefinition(paymentProvider.get().getProviderKey());
        }

        return new SubmissionStatusResponseDTO(
                sub.getId(),
                paymentProviderDefinition.map(PaymentProviderDefinition::getProviderName).orElse(null),
                paymentTransaction.map(tx -> tx.getPaymentInformation().getTransactionRedirectUrl()).orElse(null),
                paymentTransaction.map(tx -> tx.getPaymentInformation().getStatus() == XBezahldienstStatus.PAYED).orElse(false),
                paymentTransaction.map(tx -> tx.getPaymentInformation().getStatus() != XBezahldienstStatus.INITIAL && tx.getPaymentInformation().getStatus() != XBezahldienstStatus.PAYED).orElse(false),
                testSubmissionExpired(sub),
                sub.getCopySent() != null ? sub.getCopySent() : false
        );
    }

    @GetMapping("/api/public/prints/{uuid}")
    public ResponseEntity<Resource> getPrint(@PathVariable String uuid) {
        var submission = submissionRepository
                .findById(uuid)
                .orElseThrow(() -> new UserFriendlyResponseStatusException(HttpStatus.NOT_FOUND, "Der Antrag mit der ID " + uuid + " konnte nicht gefunden werden."));

        var submissionExpired = testSubmissionExpired(submission);
        if (submissionExpired) {
            throw new UserFriendlyResponseStatusException(HttpStatus.FORBIDDEN, "Die Zugriffsfrist für den Antrag ist abgelaufen. Bitte wenden Sie sich an die zuständige Dienststelle. Zur eindeutigen Identifizierung Ihrer Einreichung geben Sie bitte folgende Kennung an: " + uuid);
        }

        var form = formRepository
                .findById(submission.getFormId())
                .orElseThrow(() -> new UserFriendlyResponseStatusException(HttpStatus.NOT_FOUND, "Das Formular mit der ID " + submission.getFormId() + " konnte nicht gefunden werden."));

        // Get the path to the generated pdf
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService
                    .generateCustomerSummary(form, submission, FormPdfScope.Citizen);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new UserFriendlyResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Der Antrag konnte nicht gedruckt werden. Bitte versuchen Sie es später erneut.");
        } catch (ResponseException e) {
            throw new RuntimeException(e);
        }

        // Get pointer to the pdf file resource
        var resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename("Antrag.pdf")
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private boolean testSubmissionExpired(Submission submission) {
        var form = formRepository
                .findById(submission.getFormId());

        return form.map(submission::hasExternalAccessExpired).orElse(true);
    }

    private Optional<String> extractAccessToken(Map<String, Object> customerInput) {
        var authData = getAuthData(customerInput);
        if (authData.isEmpty()) {
            return Optional.empty();
        }

        var accessTokenRaw = authData.get().get("access_token");
        if (accessTokenRaw instanceof String accessToken) {
            if (StringUtils.isNotNullOrEmpty(accessToken)) {
                return Optional.of(accessToken);
            }
        }

        return Optional.empty();
    }

    private Optional<Map<?, ?>> getAuthData(Map<String, Object> customerInput) {
        var idRaw = customerInput.get(SpecialCustomerInputKeys.IdCustomerInputKey);
        if (idRaw instanceof Map<?, ?> id) {
            var authDataRaw = id.get("authData");
            if (authDataRaw instanceof Map<?, ?> authData) {
                return Optional.of(authData);
            }
        }

        return Optional.empty();
    }

    private Optional<Idp> extractIdp(Map<String, Object> customerInput) {
        var idRaw = customerInput.get(SpecialCustomerInputKeys.IdCustomerInputKey);
        if (idRaw instanceof Map<?, ?> id) {
            var idpRaw = id.get("idp");
            if (idpRaw instanceof String sIdp) {
                return Idp.fromString(sIdp);
            }
        }

        return Optional.empty();
    }
}
