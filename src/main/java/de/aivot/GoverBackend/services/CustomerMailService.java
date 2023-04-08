package de.aivot.GoverBackend.services;

import com.oracle.truffle.js.runtime.Strings;
import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerMailService {
    private static final String SUBJECT_TEMPLATE = "Unterlagen für: %s";
    private final MailService mailService;
    private final BlobService blobService;

    @Autowired
    public CustomerMailService(MailService mailService, BlobService blobService) {
        this.mailService = mailService;
        this.blobService = blobService;
    }

    public void sendApplicationCopyMail(String to, Application application, @Nullable Department department, String pdfLink) throws MessagingException, IOException, MailException {
        String[] parts = pdfLink.split("/");
        String pdfUuid = parts[parts.length - 1];

        Path pdfUrl = blobService.getPrintPdfPath(pdfUuid);

        String title = application.getApplicationTitle();

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);
        mailData.put("department", department != null ? department.getName() : "Ihre Dienststelle");

        String html = mailService.loadTemplate("customer-mail.html", mailData);
        String text = mailService.loadTemplate("customer-mail.txt", mailData);
        String subject = Strings.format(SUBJECT_TEMPLATE, title).toString();

        mailService.sendMail(to, subject, text, html, pdfUrl);
    }
}
