package de.aivot.GoverBackend.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.aivot.GoverBackend.data.SpecialCustomerInputKeys;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.mail.CustomerMailService;
import de.aivot.GoverBackend.mail.ExceptionMailService;
import de.aivot.GoverBackend.mail.MailService;
import de.aivot.GoverBackend.mail.SubmissionMailService;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.CustomerSubmissionCopyRequestDto;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import de.aivot.GoverBackend.repositories.*;
import de.aivot.GoverBackend.services.*;
import de.aivot.GoverBackend.utils.ElementUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.MessagingException;

import javax.script.ScriptEngine;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
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
            ExceptionMailService exceptionMailService
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
    }

    @GetMapping("/api/public/prints/{uuid}")
    public ResponseEntity<Resource> getPrint(@PathVariable String uuid) {
        Optional<Submission> optSubmission = submissionRepository.findById(uuid);

        if (optSubmission.isEmpty()) {
            throw new NotFoundException();
        }
        Submission submission = optSubmission.get();

        testSubmissionExpired(submission);

        // Get the path to the generated pdf
        var path = submissionStorageService
                .getSubmissionPdfPath(submission.getId());

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
                .findById(submission.getFormId())
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

    @PostMapping("/api/public/submit/{applicationId}")
    public Submission submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer applicationId,
            @RequestParam(value = "inputs", required = true) String inputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        // Fetch form
        Form form = formRepository
                .findById(applicationId)
                .orElseThrow(NotFoundException::new);

        // Test form published or user authenticated
        if (form.getStatus() != ApplicationStatus.Published && (jwt == null)) {
            throw new ForbiddenException();
        }

        // Get customer input
        Map<String, Object> customerInput = new JSONObject(inputs).toMap();
        Optional<String> optionalAccessToken = extractAccessToken(customerInput);
        Optional<Idp> optionalIdp = extractIdp(customerInput);

        // Get root element
        RootElement root = form.getRoot();

        // Check if any id is required

        var isBayernIdRequired = form.getBayernIdEnabled() && BayernIdAccessLevel.OPTIONAL.isLowerThan(form.getBayernIdLevel());
        var isBundIdRequired = form.getBundIdEnabled() && BundIdAccessLevel.OPTIONAL.isLowerThan(form.getBundIdLevel());
        var isMukRequired = form.getMukEnabled() && MukAccessLevel.OPTIONAL.isLowerThan(form.getMukLevel());
        var isShIdRequired = form.getShIdEnabled() && SchleswigHolsteinIdAccessLevel.OPTIONAL.isLowerThan(form.getShIdLevel());

        if ((isBayernIdRequired || isBundIdRequired || isMukRequired || isShIdRequired) && (optionalAccessToken.isEmpty() || optionalIdp.isEmpty())) {
            throw new BadRequestException("A service account is required");
        }

        if (optionalAccessToken.isPresent() && optionalIdp.isPresent()) {
            RSAPublicKey publicKey;
            try {
                publicKey = keyCloakApiService.getRealmPublicKey();
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
            var flatElements = ElementUtils.flattenElements(root);
            for (var element : flatElements) {
                var metadata = element.getMetadata();
                if (metadata != null) {
                    Object idMappingRaw = metadata.get(optionalIdp.get().getMappingSource());

                    if (idMappingRaw instanceof String idMapping) {
                        var idClaim = claims.get(idMapping);
                        if (idClaim != null && !idClaim.isNull()) {
                            var idValue = idClaim.asString();
                            if (idValue != null && StringUtils.isNotNullOrEmpty(idValue)) {
                                // TODO: Check idp type and format date for birthdays
                                customerInput.put(element.getId(), idValue);
                            }
                        }
                    }
                }
            }

            var rawIdData = customerInput.get(SpecialCustomerInputKeys.IdCustomerInputKey);
            if (rawIdData instanceof HashMap<?,?> idData) {
                var rawUserInfo = idData.get(SpecialCustomerInputKeys.UserInfoKey);
                if (rawUserInfo instanceof HashMap<?,?> userInfo) {
                    var typedUserInfo = (HashMap<String, Object>) userInfo;
                    for (var claim : claims.entrySet()) {
                        typedUserInfo.put(claim.getKey(), claim.getValue().asString());
                    }
                }
            }
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
        Integer destinationId = form.getDestinationId();
        Destination destination = null;
        if (destinationId != null) {
            destination = destinationRepository
                    .findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Destination not found"));
        }

        if (destination != null && files != null && files.length > 0) {
            if (destination.getMaxAttachmentMegaBytes() != null) {
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
        }

        // Initialize script engine
        ScriptEngine scriptEngine = ScriptService.getEngine();

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
        ApplicationPdfDto applicationDto = new ApplicationPdfDto(form, customerInput, scriptEngine);

        // Prepare submission directory
        String submissionId = UUID.randomUUID().toString();
        try {
            submissionStorageService.initSubmission(submissionId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Generate print
        try {
            pdfService.generatePdf(form, applicationDto, submissionId);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Create submission
        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setCreated(LocalDateTime.now());
        submission.setFormId(form.getId());
        submission.setAssigneeId(null);
        submission.setCustomerInput(customerInput);
        submission.setIsTestSubmission(form.getStatus() != ApplicationStatus.Published || jwt != null);
        submission.setCopySent(false);
        submission.setCopyTries(0);
        submission.setDestinationId(form.getDestinationId());
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
                    attachment.setSubmissionId(submission.getId());

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
            submission.setDestinationTimestamp(LocalDateTime.now());
            try {
                var result = destinationSubmitService.handleSubmit(
                        form,
                        submission,
                        attachments
                );
                submission.setArchived(result.ok() ? LocalDateTime.now() : null);
                submission.setDestinationSuccess(result.ok());
                submission.setDestinationResult(result.message());
                submission.setFileNumber(result.fileNumber());
                submissionRepository.save(submission);
            } catch (Exception e) {
                submission.setArchived(null);
                submission.setDestinationSuccess(false);
                submission.setDestinationResult(e.getMessage());
                submission.setFileNumber(null);
                submissionRepository.save(submission);
            }

            if (!submission.getDestinationSuccess()) {
                try {
                    submissionMailService.sendDestinationFailed(form, submission, destination);
                } catch (Exception e) {
                    exceptionMailService.send(e);
                }
            }
        } else {
            try {
                submissionMailService.sendReceived(form, submission);
            } catch (Exception e) {
                exceptionMailService.send(e);
            }
        }

        return submission;
    }

    @PostMapping("/api/public/send-copy/{submissionId}")
    public ResponseEntity<HttpStatus> sendCopy(@PathVariable String submissionId, @RequestBody CustomerSubmissionCopyRequestDto customerSubmissionCopyRequestDto) {
        var submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(ResourceNotFoundException::new);

        testSubmissionExpired(submission);

        if (submission.getCopySent()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
        }

        if (submission.getCopyTries() >= goverConfig.getMaxSubmissionCopyRetryCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        try {
            customerMailService.sendSubmissionCopy(customerSubmissionCopyRequestDto.getEmail(), submission);
            submission.setCopySent(true);
            submissionRepository.save(submission);
        } catch (MailException | MessagingException e) {
            submission.setCopyTries(submission.getCopyTries() + 1);
            submissionRepository.save(submission);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/api/public/rate/{submissionId}")
    public ResponseEntity<HttpStatus> rate(@PathVariable String submissionId, @RequestParam(required = true) Integer score) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            submission.setReviewScore(score);
            submissionRepository.save(submission);
        });

        return ResponseEntity.ok(HttpStatus.OK);
    }

    private void testSubmissionExpired(Submission submission) {
        Integer accessHours;
        var form = formRepository
                .findById(submission.getFormId());
        if (form.isPresent() && form.get().getCustomerAccessHours() != null) {
            accessHours = form.get().getCustomerAccessHours();
        } else {
            accessHours = 4;
        }

        LocalDateTime expirationTimestamp = submission.getCreated().plusHours(accessHours);
        LocalDateTime currentTimestamp = LocalDateTime.now();
        if (currentTimestamp.isAfter(expirationTimestamp)) {
            throw new ResourceNotFoundException();
        }
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
