package de.aivot.GoverBackend;

import de.aivot.GoverBackend.exceptions.UserFriendlyResponseStatusException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.ApiErrorDto;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final ExceptionMailService mailService;
    private final GoverConfig goverConfig;

    @Autowired
    public GlobalExceptionHandler(ExceptionMailService mailService, GoverConfig goverConfig) {
        this.mailService = mailService;
        this.goverConfig = goverConfig;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) throws Exception {
        if (!(ex instanceof ResourceNotFoundException || ex instanceof ResponseStatusException || ex instanceof AccessDeniedException)) {
            if (goverConfig.getSentryServer() != null && !goverConfig.getSentryServer().isEmpty()) {
                Sentry.captureException(ex);
            } else {
                mailService.send(ex);
            }
        }

        return super.handleException(ex, request);
    }

    @ExceptionHandler(UserFriendlyResponseStatusException.class)
    public ResponseEntity<String> handleUserFriendlyResponseException(UserFriendlyResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .contentType(MediaType.TEXT_HTML)
                .body("<html><head><meta charset=\"UTF-8\"><title>Fehler beim Aufruf der Seite</title></head><body>Leider ist beim Aufruf der Seite ein Fehler aufgetreten. Die Fehlermeldung lautet:<br/><br/>" +  e.getReason() + "</body></html>");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        final List<String> errors = new ArrayList<>();

        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }

        final ApiErrorDto apiError = new ApiErrorDto(HttpStatus.BAD_REQUEST, "bad request", errors);

        return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
    }
}
