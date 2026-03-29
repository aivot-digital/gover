package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.captcha.services.AltchaService;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.SpringContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmitStepElementTest {
    @Test
    void performValidationShouldPreserveExpiredMessage() {
        var submitStepElement = new SubmitStepElement();

        var exception = assertThrows(ValidationException.class, () -> submitStepElement.performValidation(Map.of(
                "payload", "payload",
                "expiresAt", Instant.now().minusSeconds(1).getEpochSecond()
        )));

        assertEquals("Die Captcha-Bestätigung ist abgelaufen. Bitte erneut bestätigen.", exception.getMessage());
    }

    @Test
    void performValidationShouldPreserveVerificationFailedMessage() throws Exception {
        var altchaService = mock(AltchaService.class);
        when(altchaService.verify("payload")).thenReturn(false);

        var submitStepElement = new SubmitStepElement();
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(AltchaService.class)).thenReturn(altchaService);

        var previousContext = setSpringContext(applicationContext);
        try {
            var exception = assertThrows(ValidationException.class, () -> submitStepElement.performValidation(Map.of(
                    "payload", "payload"
            )));

            assertEquals("Captcha-Verifizierung fehlgeschlagen. Bitte erneut bestätigen.", exception.getMessage());
        } finally {
            setSpringContext(previousContext);
        }
    }

    private static ApplicationContext setSpringContext(ApplicationContext applicationContext) throws Exception {
        Field field = SpringContext.class.getDeclaredField("context");
        field.setAccessible(true);
        var previousContext = (ApplicationContext) field.get(null);
        field.set(null, applicationContext);
        return previousContext;
    }
}
