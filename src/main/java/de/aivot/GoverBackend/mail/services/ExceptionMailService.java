package de.aivot.GoverBackend.mail.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.enums.MailTemplate;
import de.aivot.GoverBackend.models.config.GoverConfig;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

@Component
public class ExceptionMailService {
    private final ScopedAuditService auditService;

    private final MailService mailService;
    private final GoverConfig goverConfig;

    @Autowired
    public ExceptionMailService(
            AuditService auditService,
            MailService mailService,
            GoverConfig goverConfig
    ) {
        this.auditService = auditService.createScopedAuditService(ExceptionMailService.class);
        this.mailService = mailService;
        this.goverConfig = goverConfig;
    }

    public void send(Exception exception) {
        send(exception, null);
    }

    public void send(Exception exception, LinkedHashMap<String, String> additionalContext) {
        String title = "Es ist ein Fehler aufgetreten";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString();

        var context = new HashMap<String, Object>();
        context.put("message", exception.getMessage());
        context.put("stackTrace", sStackTrace);

        LinkedHashMap<String, String> internalAdditionalContext = additionalContext != null ? new LinkedHashMap<>(additionalContext) : new LinkedHashMap<>();
        var request = getCurrentHttpRequest();
        if (request != null) {
            internalAdditionalContext.put("Request URL", request.getRequestURL().toString());
            internalAdditionalContext.put("Request Method", request.getMethod());
            internalAdditionalContext.put("Request UserAgent", request.getHeader("User-Agent"));
        }
        context.put("additionalContext", internalAdditionalContext);

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
                auditService.logException("Error sending exception mail to " + mail, e);
            } catch (ResponseException e) {
                throw new RuntimeException(e);
            }
        }
        if (!mailReached) {
            throw new RuntimeException("Exception occurred and no admin mail reached", exception);
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}