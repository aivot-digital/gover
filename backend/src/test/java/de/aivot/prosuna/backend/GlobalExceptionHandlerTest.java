package de.aivot.prosuna.backend;

import de.aivot.prosuna.backend.exceptions.UserFriendlyResponseStatusException;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.aivot.prosuna.backend.mail.dtos.TestMailRequestDTO;
import de.aivot.prosuna.backend.mail.services.ExceptionMailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.models.dtos.ApiErrorDto;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalExceptionHandlerTest {
    @Test
    void validationMessageOmitsTechnicalFieldPathsWhileDetailsPreserveThem() throws Exception {
        var errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new ObjectError("request", "Die Angaben sind ungültig."));
        errors.addError(new FieldError("request", "targetMail", "Geben Sie eine gültige E-Mail-Adresse ein."));
        errors.addError(new FieldError("request", "name", "Geben Sie einen Namen ein."));

        var body = validationErrorBody(errors);

        assertEquals("Geben Sie eine gültige E-Mail-Adresse ein.", body.getMessage());
        assertEquals(List.of(
                "targetMail: Geben Sie eine gültige E-Mail-Adresse ein.",
                "name: Geben Sie einen Namen ein.",
                "request: Die Angaben sind ungültig."
        ), body.getDetails());
    }

    @Test
    void globalValidationMessageOmitsTheTechnicalObjectName() throws Exception {
        var errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new ObjectError("request", "Die Angaben sind ungültig."));

        var body = validationErrorBody(errors);

        assertEquals("Die Angaben sind ungültig.", body.getMessage());
        assertEquals(List.of("request: Die Angaben sind ungültig."), body.getDetails());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void validationErrorsWithoutAMessageUseAReadableFallback(String message) throws Exception {
        var errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "targetMail", message));

        assertEquals("Die Anfrage enthält ungültige Daten.", validationErrorBody(errors).getMessage());
    }

    @Test
    void emptyValidationErrorsUseAReadableFallback() throws Exception {
        var errors = new BeanPropertyBindingResult(new Object(), "request");

        var body = validationErrorBody(errors);

        assertEquals("Die Anfrage enthält ungültige Daten.", body.getMessage());
        assertEquals(List.of(), body.getDetails());
    }

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

    private ApiErrorDto validationErrorBody(BeanPropertyBindingResult errors) throws Exception {
        var parameter = new MethodParameter(TestMailRequestDTO.class.getDeclaredConstructor(String.class), 0);
        var exception = new MethodArgumentNotValidException(parameter, errors);
        var handler = new GlobalExceptionHandler(mock(ExceptionMailService.class), mock(ProsunaConfig.class));

        var response = handler.handleMethodArgumentNotValid(
                exception,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest())
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var body = assertInstanceOf(ApiErrorDto.class, response.getBody());
        assertTrue(body.getDisplayableToUser());
        return body;
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
