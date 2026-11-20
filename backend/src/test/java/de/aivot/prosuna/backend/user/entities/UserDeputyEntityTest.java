package de.aivot.prosuna.backend.user.entities;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDeputyEntityTest {
    @ParameterizedTest
    @CsvSource({
            "originalUserId,,Die vertretene Person fehlt.",
            "originalUserId,invalid,Die Angabe zur vertretenen Person ist ungültig.",
            "deputyUserId,,Wählen Sie eine Person als Stellvertreter:in aus.",
            "deputyUserId,invalid,Die Auswahl im Feld „Stellvertreter:in“ ist ungültig.",
            "fromDate,,Geben Sie den Beginn der Stellvertretung an."
    })
    void namesTheInvalidFormField(String field, String value, String message) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validateValue(UserDeputyEntity.class, field, value);

            assertEquals(1, violations.size());
            assertEquals(message, violations.iterator().next().getMessage());
        }
    }
}
