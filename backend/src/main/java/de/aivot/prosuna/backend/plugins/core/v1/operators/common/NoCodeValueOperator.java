package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeValueOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "value";
    }

    @Override
    public String getLabel() {
        return "Wert übernehmen";
    }

    @Override
    public String getAbstract() {
        return "Übernimmt einen Wert unverändert als Ergebnis.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Übernehmen Sie einen Wert unverändert, etwa einen Text aus einer Wertquelle als Ergebnis eines Ausdrucks.

                # Welche Werte werden benötigt?
                Einen „Wert“. Einen festen Text können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie den Wert aus einer Wertquelle oder einer Funktion übernehmen.

                # Welches Ergebnis erhalten Sie?
                Genau den übergebenen Wert, ohne ihn zu verändern. Wird ein Text übergeben, erhalten Sie denselben Text; Werte anderer Typen behalten ihren Typ.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Wert übernehmen“ als Funktion für einen Textwert.
                2. Wählen Sie für „Wert“ über „Andere Wertquelle verwenden …“ die Wertquelle „E-Mail-Adresse“.
                3. Verwenden Sie den Ausdruck beispielsweise als berechneten Text.

                Enthält die Wertquelle „person@example.org“, erhalten Sie genau diesen Text.

                # Was ist zu beachten?
                Ein selbst eingegebener Text wird wörtlich übernommen. Die Eingabe `email` sucht kein Formularelement mit dieser Kennung. Um einen vorhandenen Wert zu übernehmen, wählen Sie die entsprechende Wertquelle.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert",
                                "Der Wert, der unverändert als Ergebnis übernommen wird."
                        ).withSentence("")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(args[0]);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0";
    }
}
