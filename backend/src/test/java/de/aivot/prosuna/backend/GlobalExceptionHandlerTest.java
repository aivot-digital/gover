package de.aivot.prosuna.backend;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(body.getDisplayableToUser());
        verifyNoInteractions(mailService);
    }
}
