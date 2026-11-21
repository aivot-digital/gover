package de.aivot.prosuna.backend.plugins.core.v1.operators;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsDefinedOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsInvisibleOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsUndefinedOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeIsVisibleOperator;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeNotEqualsOperator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

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

    @Test
    void detailsShouldContainTheSentenceLead() {
        var equals = NoCodeOperatorDetailsDTO.fromSPI(provider)
                .filter(details -> details.identifier().equals(new NoCodeEqualsOperator().getIdentifier()))
                .findFirst()
                .orElseThrow();

        assertEquals("ergibt wahr", equals.sentenceLead());
        assertEquals("gleich", equals.signatures()[0].parameters()[1].sentencePhrase());
    }

    @Test
    void negatedOperatorsShouldNegateTheirSentence() {
        assertEquals("ungleich", new NoCodeNotEqualsOperator().getSignatures()[0].parameters()[1].sentencePhrase());
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
