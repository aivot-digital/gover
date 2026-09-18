package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ExceptionMailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.models.dtos.ApiErrorDto;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
}
