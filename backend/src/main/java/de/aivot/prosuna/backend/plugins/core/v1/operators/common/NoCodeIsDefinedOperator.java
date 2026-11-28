package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Map;

public class NoCodeIsDefinedOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "is-defined";
    }

    @Override
    public String getLabel() {
        return "Ist nicht leer";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein Wert nicht leer ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Prüfen Sie, ob ein Wert nicht leer ist, etwa ob eine E-Mail-Adresse angegeben wurde.

                # Welche Werte werden benötigt?

                - **Wert:** Der Wert, den Sie prüfen möchten. Einen festen Text können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über **„Andere Wertquelle verwenden …“** können Sie auch Werte anderer Typen aus Wertquellen oder Funktionen prüfen.

                # Welches Ergebnis erhalten Sie?

                Ein Wahrheitswert: `Wahr`, wenn der Wert nicht leer ist, andernfalls `Falsch`. Als leer gelten ein fehlender Wert, ein leerer Text, ein Text nur aus Leerraum sowie eine Liste oder ein Objekt ohne Einträge.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Ist nicht leer“** als Funktion für eine Bedingung.
                2. Wählen Sie für **„Wert“** über **„Andere Wertquelle verwenden …“** die Wertquelle **„E-Mail-Adresse“**.
                3. Verwenden Sie das Ergebnis beispielsweise als Bedingung für einen Hinweis.

                Bei `person@example.org` ergibt der Ausdruck `Wahr`. Ohne E-Mail-Adresse ergibt er `Falsch`.

                # Was ist zu beachten?

                Die Zahl `0` und der Wahrheitswert `Falsch` zählen als vorhandene, nicht leere Werte. Bei Listen und Objekten wird nur geprüft, ob Einträge vorhanden sind; leere Inhalte innerhalb eines Eintrags werden nicht einzeln geprüft. Ein als Text eingegebenes `[]` ist ein vorhandener Text und keine leere Liste.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    /** The phrase after the value, so subclasses keep the sentence of this operator. */
    protected String getEmptinessSuffix() {
        return "nicht leer ist";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert",
                                "Der zu überprüfende Wert."
                        ).withSentence("wenn", getEmptinessSuffix())
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args[0] == null) {
            return new NoCodeResult(false);
        }

        return switch (args[0]) {
            case String value -> new NoCodeResult(!value.isBlank());
            case Collection<?> value -> new NoCodeResult(!value.isEmpty());
            case Map<?, ?> value -> new NoCodeResult(!value.isEmpty());
            default -> new NoCodeResult(true);
        };
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist nicht leer";
    }
}
