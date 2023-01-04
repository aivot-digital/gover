package de.aivot.GoverBackend.services;

import com.oracle.truffle.js.runtime.Strings;
import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.models.Department;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerMailService {
    private static final String SUBJECT_TEMPLATE = "Unterlagen für: %s";
    private final MailService mailService;

    @Autowired
    public CustomerMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendApplicationCopyMail(String to, Application application, @Nullable Department department, String pdfLink) throws MessagingException, MalformedURLException, MailException {
        URL pdfUrl = new URL(pdfLink);

        String title = (String) application.getRoot().get("title");

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);
        mailData.put("department", department != null ? department.getName() : "Ihre Dienststelle");

        String html = mailService.loadTemplate("customer-mail.html", mailData);
        String text = mailService.loadTemplate("customer-mail.txt", mailData);
        String subject = Strings.format(SUBJECT_TEMPLATE, title).toString();

        mailService.sendMail(to, subject, text, html, pdfUrl);
    }
}
