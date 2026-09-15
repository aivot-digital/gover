package de.aivot.prosuna.backend.elements.models.elements.steps;

import de.aivot.prosuna.backend.captcha.services.AltchaService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.DeserializationFeature;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmitStepElementTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deserializeShouldIgnoreLegacyConfettiOption(boolean disableConfetti) {
        var mapper = JsonMapperTestUtils.createMapper().rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        var element = mapper.readValue("""
                {
                  "type": "18",
                  "textPostSubmit": "Vielen Dank für Ihre Einreichung.",
                  "disableConfetti": %s
                }
                """.formatted(disableConfetti), BaseElement.class);

        var submitStep = assertInstanceOf(SubmitStepElement.class, element);
        assertEquals("Vielen Dank für Ihre Einreichung.", submitStep.getTextPostSubmit());
        assertFalse(mapper.valueToTree(submitStep).has("disableConfetti"));
    }

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
