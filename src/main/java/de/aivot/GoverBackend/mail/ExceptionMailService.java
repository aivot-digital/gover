package de.aivot.GoverBackend.mail;

import de.aivot.GoverBackend.models.config.GoverConfig;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Optional;

@Component
public class ExceptionMailService {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMailService.class);

    private final MailService mailService;
    private final GoverConfig goverConfig;

    @Autowired
    public ExceptionMailService(MailService mailService, GoverConfig goverConfig) {
        this.mailService = mailService;
        this.goverConfig = goverConfig;
    }

    public void send(Exception exception) {
        String title = "Es ist ein Fehler aufgetreten";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString();

        var context = new HashMap<String, Object>();
        context.put("message", exception.getMessage());
        context.put("stackTrace", sStackTrace);

        var mailReached = false;
        for (String mail : goverConfig.getReportMail()) {
            try {
                mailService.sendMail(
                        mail,
                        Optional.empty(),
                        Optional.empty(),
                        "[Gover] " + title,
                        MailTemplate.UnhandledSystemException,
                        context,
                        Optional.empty()
                );
                mailReached = true;
            } catch (MessagingException | IOException e) {
                logger.error("Error sending exception mail to " + mail, e);
            }
        }
        if (!mailReached) {
            throw new RuntimeException("Exception occurred and no admin mail reached", exception);
        }
    }
}