package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.exceptions.UserFriendlyResponseStatusException;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.mail.services.ExceptionMailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.models.dtos.ApiErrorDto;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalExceptionHandlerTest {
    @Test
    void handleBaseResponseExceptionShouldReturnDisplayableApiError() {
        var reason = "Die Kosten der Zahlungsposition 1 müssen eine Zahl ergeben.";
        var cause = new PaymentException(reason);
        var exception = ResponseException.internalServerError(
                "Die Kosten für das Formular konnten nicht berechnet werden: " + reason,
                cause
        );
        var handler = new GlobalExceptionHandler(
                mock(ExceptionMailService.class),
                mock(ProsunaConfig.class)
        );

        var response = handler.handleBaseResponseException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        var body = assertInstanceOf(ApiErrorDto.class, response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, body.getHttpStatus());
        assertEquals(exception.getTitle(), body.getMessage());
        assertEquals(reason, body.getDetails());
        assertTrue(body.getDisplayableToUser());
    }

    @Test
    void optimisticLockFailuresShouldBeHandledAsConflict() {
        var resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);
        var springException = new ObjectOptimisticLockingFailureException("TeamEntity", 17);
        var jpaException = new OptimisticLockException();

        assertEquals("handleOptimisticLockingFailure", resolver.resolveMethod(springException).getName());
        assertEquals("handleOptimisticLockingFailure", resolver.resolveMethod(jpaException).getName());
    }

    @Test
    void handleOptimisticLockingFailureShouldReturnDisplayableConflictWithoutReporting() {
        var mailService = mock(ExceptionMailService.class);
        var handler = new GlobalExceptionHandler(mailService, mock(ProsunaConfig.class));

        var response = handler.handleOptimisticLockingFailure();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        var body = assertInstanceOf(ApiErrorDto.class, response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
        assertEquals(LockVersioned.CONFLICT_MESSAGE, body.getMessage());
        assertEquals(Map.of("reason", "lock_version_conflict"), body.getDetails());
        assertTrue(body.getDisplayableToUser());
        verifyNoInteractions(mailService);
    }

    @Test
    void handleUserFriendlyResponseExceptionShouldShowTheReasonAsText() {
        var handler = new GlobalExceptionHandler(
                mock(ExceptionMailService.class),
                mock(ProsunaConfig.class)
        );
        var exception = new UserFriendlyResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Der Antrag mit der ID <img src=x onerror=alert(1)> wurde nicht gefunden & ist gelöscht."
        );

        var response = handler.handleUserFriendlyResponseException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        var body = assertInstanceOf(String.class, response.getBody());
        assertTrue(body.contains("Der Antrag mit der ID &lt;img src=x onerror=alert(1)&gt; wurde nicht gefunden &amp; ist gelöscht."));
        assertFalse(body.contains("<img"));
    }
}
