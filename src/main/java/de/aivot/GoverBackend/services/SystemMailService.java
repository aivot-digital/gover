package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.GoverConfig;
import de.aivot.GoverBackend.models.User;
import de.aivot.GoverBackend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class SystemMailService {
    private static final Logger logger = LoggerFactory.getLogger(SystemMailService.class);
    @Autowired
    MailService mailService;
    @Autowired
    GoverConfig goverConfig;

    public void sendInfoMail(String title, String message) {
        sendInfoMail(title, message, goverConfig.getReportMail());
    }

    public void sendInfoMail(String title, String message, String to) {
        Map<String, Object> mailData = new HashMap<>();
        mailData.put("title", title);
        mailData.put("message", message);

        String html = mailService.loadTemplate("system-mail-info.html", mailData);
        String text = mailService.loadTemplate("system-mail-info.txt", mailData);

        try {
            mailService.sendMail(to, "[Gover] Informationen", text, html);
        } catch (MessagingException e) {
            logger.error("Failed to send info admin mail", e);
        }
    }

    public void sendExceptionMail(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString();

        Map<String, Object> mailData = new HashMap<>();
        mailData.put("message", exception.getMessage());
        mailData.put("stackTrace", sStackTrace);

        String html = mailService.loadTemplate("system-mail-exception.html", mailData);
        String text = mailService.loadTemplate("system-mail-exception.txt", mailData);

        try {
            mailService.sendMail(goverConfig.getReportMail(), "[Gover] Fehler im Betrieb", text, html);
        } catch (MessagingException e) {
            logger.error("Failed to send exception admin mail", e);
        }
    }
}
