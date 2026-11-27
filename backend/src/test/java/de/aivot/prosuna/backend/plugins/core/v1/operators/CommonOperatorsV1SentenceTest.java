package de.aivot.prosuna.backend.plugins.core.v1.operators;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsDefinedOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsInvisibleOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsUndefinedOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsVisibleOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeNotEqualsOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommonOperatorsV1SentenceTest {
    private final CommonOperatorsV1 provider = new CommonOperatorsV1(
            null,
            null,
            new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())
    );

    @Test
    void everyCoreParameterShouldDeclareItsSentencePhrase() {
        for (var operator : provider.getOperators()) {
            for (var signature : operator.getSignatures()) {
                for (var parameter : signature.parameters()) {
                    assertNotNull(
                            parameter.sentencePhrase(),
                            operator.getIdentifier() + " lacks a sentence phrase for " + parameter.label()
                    );
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "equals, gleich",
            "not-equals, nicht gleich"
    })
    void equalityDetailsShouldKeepCompleteSentencesAndUnrestrictedParameters(String identifier, String comparisonPhrase) {
        var details = NoCodeOperatorDetailsDTO.fromSPI(provider)
                .filter(candidate -> candidate.identifier().equals(identifier))
                .findFirst()
                .orElseThrow();
        var signature = details.signatures()[0];
        var parameters = signature.parameters();

        assertEquals("ergibt wahr", details.sentenceLead());
        assertEquals(NoCodeDataType.Boolean, signature.returnType());
        assertEquals(2, parameters.length);
        assertEquals("wenn", parameters[0].sentencePhrase());
        assertNull(parameters[0].sentenceSuffix());
        assertEquals(comparisonPhrase, parameters[1].sentencePhrase());
        assertEquals("ist", parameters[1].sentenceSuffix());
        assertEquals(NoCodeDataType.Runtime, parameters[0].type());
        assertEquals(NoCodeDataType.Runtime, parameters[1].type());
        assertEquals(0, parameters[0].options().length);
        assertEquals(0, parameters[1].options().length);
    }

    @Test
    void notDetailsShouldKeepACompleteSentenceAndFixedBooleanChoices() {
        var details = NoCodeOperatorDetailsDTO.fromSPI(provider)
                .filter(candidate -> candidate.identifier().equals("not"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, details.signatures().length);
        var signature = details.signatures()[0];
        assertEquals(NoCodeDataType.Boolean, signature.returnType());
        assertEquals(1, signature.parameters().length);
        var parameter = signature.parameters()[0];

        assertEquals("ergibt wahr", details.sentenceLead());
        assertEquals(NoCodeDataType.Boolean, parameter.type());
        assertEquals("wenn", parameter.sentencePhrase());
        assertEquals("falsch ist", parameter.sentenceSuffix());
        assertArrayEquals(new NoCodeParameterOption[]{
                new NoCodeParameterOption("Wahr", "true"),
                new NoCodeParameterOption("Falsch", "false")
        }, parameter.options());
    }

    @Test
    void negatedOperatorsShouldNegateTheirSentence() {
        assertEquals("nicht gleich", new NoCodeNotEqualsOperator().getSignatures()[0].parameters()[1].sentencePhrase());
        assertEquals("gleich", new NoCodeEqualsOperator().getSignatures()[0].parameters()[1].sentencePhrase());
        assertEquals("leer ist", new NoCodeIsUndefinedOperator().getSignatures()[0].parameters()[0].sentenceSuffix());
        assertEquals("nicht leer ist", new NoCodeIsDefinedOperator().getSignatures()[0].parameters()[0].sentenceSuffix());
        assertEquals("nicht sichtbar ist", new NoCodeIsInvisibleOperator().getSignatures()[0].parameters()[0].sentenceSuffix());
        assertEquals("sichtbar ist", new NoCodeIsVisibleOperator().getSignatures()[0].parameters()[0].sentenceSuffix());
    }

    @Test
    void parametersOfPluginsWithoutSentencePartsShouldFallBackToTheLabel() {
        var parameter = new NoCodeParameter(NoCodeDataType.String, "Text", "Ein Text.");

        assertNull(parameter.sentencePhrase());
        assertNull(parameter.sentenceSuffix());
        assertEquals(0, parameter.options().length);

        var withSentence = parameter.withSentence("wenn", "leer ist");
        assertEquals("wenn", withSentence.sentencePhrase());
        assertEquals("leer ist", withSentence.sentenceSuffix());
        assertEquals("Text", withSentence.label());
    }
}
