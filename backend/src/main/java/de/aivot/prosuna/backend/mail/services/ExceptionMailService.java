package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.system.services.SystemService;
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
    private final ProsunaConfig prosunaConfig;
    private final SystemService systemService;

    @Autowired
    public ExceptionMailService(
            AuditService auditService,
            MailService mailService,
            ProsunaConfig prosunaConfig,
            SystemService systemService) {
        this.auditService = auditService.createScopedAuditService(ExceptionMailService.class, "E-Mail");
        this.mailService = mailService;
        this.prosunaConfig = prosunaConfig;
        this.systemService = systemService;
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

        var systemTheme = systemService
                .retrieveDefaultTheme();

        var mailReached = false;
        for (String mail : prosunaConfig.getReportMail()) {
            try {
                mailService.sendMail(
                        systemTheme,
                        mail,
                        Optional.empty(),
                        Optional.empty(),
                        "[Prosuna] " + title,
                        MailTemplate.UnhandledSystemException,
                        context,
                        Optional.empty()
                );
                mailReached = true;
            } catch (MessagingException | IOException e) {
                auditService.create()
                        .withException(e, this.getClass()).log();
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
