package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import jakarta.annotation.Nullable;

public class NoCodeIsUndefinedOperator extends NoCodeIsDefinedOperator {
    @Override
    public String getIdentifier() {
        return "is-undefined";
    }

    @Override
    public String getLabel() {
        return "Ist leer";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein Wert leer ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob ein Wert leer ist, etwa ob eine E-Mail-Adresse fehlt.

                # Welche Werte werden benötigt?
                Einen „Wert“. Einen festen Text können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie auch Werte anderer Typen aus Wertquellen oder Funktionen prüfen.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn der Wert leer ist, andernfalls „Falsch“. Als leer gelten ein fehlender Wert, ein leerer Text, ein Text nur aus Leerraum sowie eine Liste oder ein Objekt ohne Einträge.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Ist leer“ als Funktion für eine Bedingung.
                2. Wählen Sie für „Wert“ über „Andere Wertquelle verwenden …“ die Wertquelle „E-Mail-Adresse“.
                3. Verwenden Sie das Ergebnis beispielsweise als Bedingung für einen Hinweis.

                Ohne E-Mail-Adresse ergibt der Ausdruck „Wahr“. Bei „person@example.org“ ergibt er „Falsch“.

                # Was ist zu beachten?
                Die Zahl `0` und der Wahrheitswert „Falsch“ zählen als vorhandene, nicht leere Werte. Bei Listen und Objekten wird nur geprüft, ob Einträge vorhanden sind; leere Inhalte innerhalb eines Eintrags werden nicht einzeln geprüft. Ein als Text eingegebenes `[]` ist ein vorhandener Text und keine leere Liste.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var res = super.performEvaluation(data, args);
        return new NoCodeResult(!res.getValueAsBoolean());
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist leer";
    }

    @Override
    protected String getEmptinessSuffix() {
        return "leer ist";
    }
}
