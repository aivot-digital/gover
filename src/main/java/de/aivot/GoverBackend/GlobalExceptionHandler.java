package de.aivot.GoverBackend;

import de.aivot.GoverBackend.services.SystemMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final SystemMailService systemMailService;

    @Autowired
    public GlobalExceptionHandler(SystemMailService systemMailService) {
        this.systemMailService = systemMailService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) throws Exception {
        if (!(ex instanceof ResourceNotFoundException || ex instanceof ResponseStatusException)) {
            systemMailService.sendExceptionMail(ex);
        }
        return super.handleException(ex, request);
    }
}
