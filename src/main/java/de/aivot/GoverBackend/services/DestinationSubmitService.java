package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;

@Component
public class DestinationSubmitService {
    private final MailService mailService;

    @Autowired
    public DestinationSubmitService(MailService mailService) {
        this.mailService = mailService;
    }

    public void handleSubmit(Submission submission, Collection<SubmissionAttachment> attachments) throws MessagingException, MailException, IOException, InterruptedException {
        switch (submission.getDestination().getType()) {
            case Mail -> mailService.sendDestinationMail(submission, attachments);
            case HTTP -> sendHttp(submission);
        }
    }

    private void sendHttp(Submission submission) throws IOException, InterruptedException {
        Destination destination = submission.getDestination();

        ObjectMapper mapper = new ObjectMapper();
        String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(submission.getCustomerInput());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(destination.getApiAddress()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonResult));

        if (destination.getAuthorizationHeader() != null) {
            requestBuilder.header("Authorization", destination.getAuthorizationHeader());
        }

        HttpResponse<?> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IOException(
                    "Failed to submit data to the destination \"" + destination.getName() + "\". Status Code: " + response.statusCode()
            );
        }
    }
}
