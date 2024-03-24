package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.mail.MailService;
import de.aivot.GoverBackend.mail.SubmissionMailService;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.mail.MessagingException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class DestinationSubmitService {
    private final SubmissionMailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final DestinationRepository destinationRepository;

    @Autowired
    public DestinationSubmitService(
            SubmissionMailService mailService,
            SubmissionStorageService submissionStorageService,
            DestinationRepository destinationRepository
    ) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.destinationRepository = destinationRepository;
    }

    public DestinationResponse handleSubmit(Form form, Submission submission, Collection<SubmissionAttachment> attachments) {
        var destinationId = submission.getDestinationId();
        if (destinationId == null) {
            throw new RuntimeException("Destination not set");
        }

        var destination = destinationRepository
                .findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destination not found"));

        return switch (destination.getType()) {
            case Mail -> {
                try {
                    mailService.sendToDestination(form, submission, destination, attachments);
                    yield new DestinationResponse(true, null, null);
                } catch (MailException | MessagingException | IOException e) {
                    yield new DestinationResponse(false, "Die E-Mail konnte nicht an das Ziel gesendet werden. Fehler: " + e.getMessage(), null);
                }
            }
            case HTTP -> sendHttp(destination, form, submission, attachments);
        };
    }

    private DestinationResponse sendHttp(Destination destination, Form form, Submission submission, Collection<SubmissionAttachment> attachments) {
        URL url;
        try {
            url = new URL(destination.getApiAddress());
        } catch (MalformedURLException e) {
            return new DestinationResponse(false, "Die URL des Ziels ist ungültig", null);
        }
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return new DestinationResponse(false, "Die Verbindung zum Ziel konnte nicht hergestellt werden", null);
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

        Path pdfPath = submissionStorageService.getSubmissionPdfPath(submission.getId());
        Map<String, Path> attachmentPaths = new HashMap<>();
        for (var attachment : attachments) {
            attachmentPaths.put(attachment.getFilename(), submissionStorageService.getSubmissionAttachmentPath(submission.getId(), attachment.getId()));
        }

        Map<String, Object> destinationData = DestinationDataFormatter
                .create()
                .formatDestinationData(
                        form,
                        form.getRoot(),
                        submission,
                        submission.getCustomerInput(),
                        pdfPath,
                        attachmentPaths
                );

        ObjectMapper mapper = new ObjectMapper();
        String jsonResult;
        try {
            jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(destinationData);
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
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: Rückgabe ist leer", null);
        }

        String response;
        try {
            response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: " + e.getMessage(), null);
        }

        var resultMapper = new ObjectMapper();
        resultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DestinationResponseDto responseDto;
        try {
            responseDto = resultMapper.readValue(response, DestinationResponseDto.class);
        } catch (IOException e) {
            return new DestinationResponse(false, "Rückgabe des Ziels konnte nicht verarbeitet werden. Fehler: " + e.getMessage(), null);
        }

        if (statusCode < 200 || statusCode >= 400) {
            if (StringUtils.isNotNullOrEmpty(responseDto.message)) {
                return new DestinationResponse(false, responseDto.message, null);
            } else {
                return new DestinationResponse(false, "Das Ziel hat einen unerwarteten Statuscode zurückgegeben: " + statusCode + ". Es sind nur die Codes 200 bis 399 erlaubt. Antwort: " + response, null);
            }
        }

        return new DestinationResponse(true, responseDto.getMessage(), responseDto.getFileNumber());
    }

    public record DestinationResponse(
            Boolean ok,
            String message,
            String fileNumber
    ) {
    }

    private static class DestinationResponseDto {
        private String message;
        private String fileNumber;

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
    }
}
