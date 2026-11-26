package de.aivot.prosuna.backend.plugins.core.v1.operators.bool;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeNotOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "not";

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Nicht";
    }

    @Override
    public String getAbstract() {
        return "Kehrt das Ergebnis einer Bedingung um.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit **„Nicht“** kehren Sie das Ergebnis einer Bedingung um: Aus `wahr` wird `falsch`, aus `falsch` wird `wahr`. Nutzen Sie die Funktion, wenn Sie prüfen möchten, ob eine Bedingung nicht erfüllt ist.

                # Welche Werte werden benötigt?

                - **Bedingung:** Die Bedingung, deren Ergebnis Sie umkehren möchten.

                Für einen festgelegten Wert wählen Sie **„Wahr“** oder **„Falsch“**. Zusätzliche eigene Eingaben sind in dieser Auswahl nicht möglich.

                Über **„Andere Wertquelle verwenden …“** können Sie stattdessen einen verfügbaren Wert übernehmen oder mit einer Funktion ermitteln. Die verwendete Wertquelle sollte einen Wahrheitswert liefern.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten einen Wahrheitswert:

                - `Wahr`, wenn die eingesetzte Bedingung `falsch` ergibt.
                - `Falsch`, wenn die eingesetzte Bedingung `wahr` ergibt.

                # Beispiel im No-Code-Editor

                Sie möchten die Bedingung `Geburtsort ist gleich Berlin` umkehren.

                1. Wählen Sie die Funktion **„Nicht“**.
                2. Wählen Sie für **„Bedingung“** zunächst **„Andere Wertquelle verwenden …“**, dann **„Mit Funktion ermitteln“** und die Funktion **„Ist gleich“**.
                3. Wählen Sie innerhalb von **„Ist gleich“** für **„Wert 1“** die Option **„Aus Formularelement übernehmen“** und anschließend **„Geburtsort“**.
                4. Wählen Sie für **„Wert 2“** die Option **„Wert selbst festlegen“** und geben Sie `Berlin` ein.

                Der fertige Ausdruck liest sich so:

                `nicht (Wert von „Geburtsort“ ist gleich „Berlin“)`

                Bei `Berlin` ergibt der innere Vergleich `wahr`. **„Nicht“** kehrt das Ergebnis zu `falsch` um. Bei `Hamburg` ergibt der innere Vergleich `falsch` und der gesamte Ausdruck `wahr`.

                Für diesen einzelnen Vergleich können Sie auch direkt **„Ist nicht gleich“** verwenden. **„Nicht“** ist besonders hilfreich, wenn Sie das Ergebnis einer zusammengesetzten Bedingung umkehren möchten.

                # Was ist zu beachten?

                - **„Nicht“** kehrt das Ergebnis der gesamten eingesetzten Bedingung um.
                - Liefert die verwendete Wertquelle keinen Wert, wird dieser wie `falsch` behandelt. **„Nicht“** ergibt dann `wahr`.
                - Leerer Text gilt ebenfalls als `falsch`. Die Texte `wahr`, `falsch`, `true` und `false` werden unabhängig von Groß- und Kleinschreibung erkannt. Andere Werte, etwa Zahlen, Listen oder der Text `Nein`, führen zu einer Fehlermeldung. Verwenden Sie deshalb möglichst eine Wertquelle, die tatsächlich einen Wahrheitswert liefert.
                - Zweimaliges Umkehren stellt den ursprünglichen Wahrheitswert wieder her. Achten Sie bei verschachtelten Verneinungen darauf, welches Ergebnis Sie benötigen.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Boolean,
                                "Bedingung",
                                "Die Bedingung, deren Ergebnis Sie umkehren möchten.",
                                new NoCodeParameterOption("Wahr", "true"),
                                new NoCodeParameterOption("Falsch", "false")
                        ).withSentence("wenn", "falsch ist")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg = castToBoolean(args[0]);

        return new NoCodeResult(!arg);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "nicht „#0“";
    }
}
