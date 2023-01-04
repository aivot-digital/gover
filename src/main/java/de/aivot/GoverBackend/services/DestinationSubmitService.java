package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.js.runtime.Strings;
import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.models.Destination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class DestinationSubmitService {
    @Autowired
    MailService mailService;
    @Autowired
    SystemMailService systemMailService;

    private static final String SUBJECT_TEMPLATE = "Neuer Antrag: %s";

    public void handleSubmit(Destination destination, Application application, Map<String, Object> customerData, String pdfLink) throws MessagingException, IOException, InterruptedException {
        switch (destination.getType()) {
            case Mail -> sendMail(destination, application, pdfLink);
            case HTTP -> sendHttp(destination, customerData);
        }
    }

    private void sendMail(Destination destination, Application application, String pdfLink) throws MessagingException, MalformedURLException {
        URL pdfUrl = new URL(pdfLink);

        String title = (String) application.getRoot().get("title");

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);

        String html = mailService.loadTemplate("destination-mail.html", mailData);
        String text = mailService.loadTemplate("destination-mail.txt", mailData);
        String subject = Strings.format(SUBJECT_TEMPLATE, title).toString();

        mailService.sendMail(
                destination.getMailTo(),
                destination.getMailCC(),
                destination.getMailBCC(),
                subject,
                text,
                html,
                pdfUrl
        );
    }

    private void sendHttp(Destination destination, Map<String, Object> customerData) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customerData);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(destination.getApiAddress()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonResult))
                .build();
        HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() > 299) {
            throw new IOException("Daten konnten nicht an HTTP-Schnittstelle " + destination.getName() + " übertragen werden: " + response.statusCode());
        }
    }
}
