package de.aivot.gover.backend.elements.models.elements.steps;

import de.aivot.gover.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.gover.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntroductionStepElementTest {
    @Test
    void performValidationShouldExposePrivacyConsentLabel() {
        var introductionStepElement = new IntroductionStepElement();

        var exception = assertThrows(ValidationException.class, () -> introductionStepElement.performValidation(false));

        assertEquals("Bitte akzeptieren Sie die Hinweise zum Datenschutz.", exception.getMessage());
        assertEquals(Map.of("label", "Datenschutzrechtliche Einwilligung"), exception.getErrorDetails());
    }
}
