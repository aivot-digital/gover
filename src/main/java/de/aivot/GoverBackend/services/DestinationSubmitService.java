package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.elements.models.form.input.TextField;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.models.IdentityValue;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.SubmissionMailService;
import de.aivot.GoverBackend.ozgCloud.enums.OZGCloudPostfachAdresseType;
import de.aivot.GoverBackend.ozgCloud.enums.OZGCloudServiceKontoType;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudControlData;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudPostfachAdresse;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudServiceKontoData;
import de.aivot.GoverBackend.ozgCloud.services.OZGCloudDestinationService;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.submission.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DestinationSubmitService {
    private static final Logger logger = LoggerFactory.getLogger(DestinationSubmitService.class);

    private final SubmissionMailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final PdfService pdfService;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final OZGCloudDestinationService oZGCloudDestinationService;
    private final IdentityProviderService identityProviderService;

    @Autowired
    public DestinationSubmitService(
            SubmissionMailService mailService,
            SubmissionStorageService submissionStorageService,
            PdfService pdfService,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentProviderRepository paymentProviderRepository,
            OZGCloudDestinationService oZGCloudDestinationService,
            IdentityProviderService identityProviderService) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.pdfService = pdfService;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.oZGCloudDestinationService = oZGCloudDestinationService;
        this.identityProviderService = identityProviderService;
    }

    public void testDestinationAttachmentSize(Destination destination, MultipartFile[] files) {
        // If form has no files, return because it is nothing to do
        if (files == null || files.length == 0) {
            return;
        }

        // If no destination is present, return because it is nothing to do
        if (destination == null) {
            return;
        }

        if (destination.getMaxAttachmentMegaBytes() != null) {
            long filesTotalBytes = 0;
            for (MultipartFile file : files) {
                filesTotalBytes += file.getSize();
            }
            long allowedTotalBytes = destination.getMaxAttachmentMegaBytes() * 1000 * 1000;
            if (filesTotalBytes > allowedTotalBytes) {
                throw new ConflictException("Exceeded max allowed file size of destination");
            }
        }
    }

    public void handleSubmit(Destination destination, Form form, Submission submission, Collection<SubmissionAttachment> attachments) throws ResponseException {
        // Send to destination
        DestinationResponse response;
        try {
            response = switch (destination.getType()) {
                case Mail -> {
                    try {
                        mailService.sendToDestination(form, submission, destination, attachments);
                        yield new DestinationResponse(true, null, null, null);
                    } catch (MailException | MessagingException | IOException e) {
                        yield new DestinationResponse(false, "Die E-Mail konnte nicht an das Ziel gesendet werden. Fehler: " + e.getMessage(), null, null);
                    }
                }
                case HTTP -> sendHttp(destination, form, submission, attachments);
                case OZGCloud -> sendOzg(destination, form, submission, attachments);
            };
        } catch (Exception e) {
            response = new DestinationResponse(false, "Die Übermittlung an das Ziel konnte nicht durchgeführt werden. Fehler: " + e.getMessage(), null, null);
        }

        // Update submission with destination response

        submission.setDestinationTimestamp(LocalDateTime.now());
        submission.setDestinationSuccess(response.ok());

        if (submission.getDestinationSuccess()) {
            submission.setDestinationResult(response.message());
            submission.setFileNumber(response.fileNumber());
            submission.setArchived(LocalDateTime.now());
            submission.setStatus(SubmissionStatus.Archived);

            if (response.attachments() != null) {
                for (var responseAttachment : response.attachments()) {
                    var attachment = new SubmissionAttachment();
                    attachment.setId(UUID.randomUUID().toString());
                    attachment.setSubmissionId(submission.getId());
                    attachment.setFilename(responseAttachment.getFilename());
                    attachment.setContentType(responseAttachment.getContentType());
                    attachment.setType("destination");

                    var bytes = org.apache.commons.codec.binary.Base64.decodeBase64(responseAttachment.getBase64Data());

                    submissionStorageService.saveAttachment(submission, attachment, bytes);

                    // TODO: This methode does not return anything. We are prone to errors here. When the submission is not saved afterwards we have an invalid state where attachments were received but the main submission was not saved successfully.
                    submissionAttachmentRepository.save(attachment);
                }
            }
        } else {
            submission.setDestinationResult(response.message());
            submission.setFileNumber(null);
            submission.setArchived(null);
            submission.setStatus(SubmissionStatus.HasDestinationError);
        }
    }

    private DestinationResponse sendHttp(Destination destination, Form form, Submission submission, Collection<SubmissionAttachment> attachments) throws ResponseException {
        URL url;
        try {
            url = new URL(destination.getApiAddress());
        } catch (MalformedURLException e) {
            return new DestinationResponse(false, "Die URL des Ziels ist ungültig", null, null);
        }
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return new DestinationResponse(false, "Die Verbindung zum Ziel konnte nicht hergestellt werden", null, null);
        }

        try {
            con.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        if (destination.getAuthorizationHeader() != null) {
            con.setRequestProperty("Authorization", destination.getAuthorizationHeader());
        }

        con.setDoOutput(true);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, FormPdfScope.Staff);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ResponseException e) {
            throw new RuntimeException(e);
        }

        Map<String, byte[]> attachmentBytes = new HashMap<>();

        for (var attachment : attachments) {
            var bytes = submissionStorageService.getAttachmentData(submission, attachment);
            attachmentBytes.put(attachment.getFilename(), bytes);
        }

        var paymentTransaction = submission.getPaymentTransactionKey() == null ?
                null :
                paymentTransactionRepository.findById(submission.getPaymentTransactionKey()).orElse(null);

        var paymentProvider = paymentTransaction == null ?
                null :
                paymentProviderRepository.findById(paymentTransaction.getPaymentProviderKey()).orElse(null);

        Map<String, Object> destinationData = DestinationDataFormatter
                .create(form, submission, paymentTransaction, paymentProvider, pdfBytes, attachmentBytes)
                .format();

        ObjectMapper mapper = new ObjectMapper();
        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(destinationData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        OutputStream os;
        try {
            os = con.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] input = jsonResult.getBytes(StandardCharsets.UTF_8);
        try {
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int statusCode;
        try {
            statusCode = con.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        InputStream inputStream;
        try {
            inputStream = con.getInputStream();
        } catch (IOException e) {
            inputStream = con.getErrorStream();
        }

        if (inputStream == null) {
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: Rückgabe ist leer", null, null);
        }

        String response;
        try {
            response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: " + e.getMessage(), null, null);
        }

        var resultMapper = new ObjectMapper();
        resultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DestinationResponseDto responseDto;
        try {
            responseDto = resultMapper.readValue(response, DestinationResponseDto.class);
        } catch (IOException e) {
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: " + e.getMessage(), null, null);
        }

        if (statusCode < 200 || statusCode >= 400) {
            if (StringUtils.isNotNullOrEmpty(responseDto.message)) {
                return new DestinationResponse(false, responseDto.message, null, null);
            } else {
                return new DestinationResponse(false, "Das Ziel hat einen unerwarteten Statuscode zurückgegeben: " + statusCode + ". Es sind nur die Codes 200 bis 399 erlaubt. Antwort: " + response, null, null);
            }
        }

        return new DestinationResponse(true, responseDto.getMessage(), responseDto.getFileNumber(), responseDto.attachments);
    }

    private static final String INTERNAL_NAME_ZUSTAENDIGE_STELLE = "OZG_CLOUD_ZUSTAENDIGE_STELLE";

    private DestinationResponse sendOzg(Destination destination, Form form, Submission submission, Collection<SubmissionAttachment> attachments) throws ResponseException {
        var zustandigeStelle = form
                .getRoot()
                .findChild((c) -> (
                        INTERNAL_NAME_ZUSTAENDIGE_STELLE.equals(c.getName()) && c instanceof TextField
                ))
                .map((c) -> {
                    if (c instanceof TextField textField) {
                        return textField.getLabel();
                    } else {
                        return null;
                    }
                })
                .orElse(null);

        if (zustandigeStelle == null) {
            return new DestinationResponse(false,
                    "Das Formular enthält kein Textfeld mit dem Namen '" + INTERNAL_NAME_ZUSTAENDIGE_STELLE + "'. Dieses wird jedoch benötigt, um die zuständige Stelle für die Übermittlung an OZG Cloud zu bestimmen.",
                    null,
                    null);
        }

        var idDataObj = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);

        OZGCloudServiceKontoData serviceKontoData = null;

        if (idDataObj instanceof Map<?, ?> idData) {
            IdentityValue idValue;
            try {
                idValue = IdentityValue.fromMap(idData);
            } catch (Exception e) {
                logger.warn("Die Identitätsdaten konnten nicht verarbeitet werden. Es wird versucht, ohne diese Daten an OZG Cloud zu übermitteln. Fehler: " + e.getMessage(), e);
                idValue = null;
            }

            if (idValue != null) {
                var providerType = identityProviderService
                        .retrieve(idValue.identityProviderKey())
                        .map(IdentityProviderEntity::getType)
                        .orElse(IdentityProviderType.Custom);

                if (providerType == IdentityProviderType.ShId) {
                    serviceKontoData = new OZGCloudServiceKontoData(
                            OZGCloudServiceKontoType.OSI,
                            idValue.userInfo().getOrDefault("trust_level_authentication", "Keine Angaben"),
                            new OZGCloudPostfachAdresse(
                                    idValue.userInfo().getOrDefault("dataport_inbox_id", "Keine Angaben"),
                                    OZGCloudPostfachAdresseType.Citizen
                            )
                    );
                } else if (providerType == IdentityProviderType.BayernId) {
                    serviceKontoData = new OZGCloudServiceKontoData(
                            OZGCloudServiceKontoType.BAYERN_ID,
                            idValue.userInfo().getOrDefault("trust_level_authentication", "Keine Angaben"),
                            new OZGCloudPostfachAdresse(
                                    idValue.userInfo().getOrDefault("legacy_postkorb_handle", "Keine Angaben"),
                                    OZGCloudPostfachAdresseType.Citizen
                            )
                    );
                }
            }
        }

        var control = new OZGCloudControlData(
                submission.getId(),
                zustandigeStelle,
                List.of(),
                form.getId().toString(),
                form.getFormTitle(),
                serviceKontoData
        );

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateCustomerSummary(form, submission, FormPdfScope.Staff);
        } catch (IOException | InterruptedException | URISyntaxException | ResponseException e) {
            throw new RuntimeException(e);
        }

        var pdfRes = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return form
                        .getFormTitle()
                        .replaceAll("\\W+", "_") + ".pdf";
            }
        };

        List<Resource> attRes = new LinkedList<>();
        for (var attachment : attachments) {
            var bytes = submissionStorageService.getAttachmentData(submission, attachment);
            var res = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    var name = attachment
                            .getFilename()
                            .split("\\.");

                    var baseName = String.join(".", Arrays.copyOf(name, name.length - 1));
                    var extension = name[name.length - 1];
                    return baseName.replaceAll("\\W+", "_") + "." + extension;
                }
            };
            attRes.add(res);
        }

        oZGCloudDestinationService
                .send(
                        destination.getApiAddress(),
                        control,
                        form.getRoot(),
                        submission.getCustomerInput(),
                        pdfRes,
                        attRes
                );

        return new DestinationResponse(true, null, null, List.of());
    }

    public record DestinationResponse(
            Boolean ok,
            String message,
            String fileNumber,
            List<DestinationResponseAttachment> attachments
    ) {
    }

    private static class DestinationResponseDto {
        private String message;
        private String fileNumber;
        private List<DestinationResponseAttachment> attachments;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getFileNumber() {
            return fileNumber;
        }

        public void setFileNumber(String fileNumber) {
            this.fileNumber = fileNumber;
        }

        public List<DestinationResponseAttachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<DestinationResponseAttachment> attachments) {
            this.attachments = attachments;
        }
    }

    private static class DestinationResponseAttachment {
        private String filename;
        private String contentType;
        private String base64Data;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getBase64Data() {
            return base64Data;
        }

        public void setBase64Data(String base64Data) {
            this.base64Data = base64Data;
        }
    }
}
