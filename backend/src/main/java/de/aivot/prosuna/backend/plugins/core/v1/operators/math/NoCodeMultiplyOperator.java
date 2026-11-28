package de.aivot.prosuna.backend.plugins.core.v1.operators.math;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;

public class NoCodeMultiplyOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "multiply";
    }

    @Override
    public String getLabel() {
        return "Multiplizieren";
    }

    @Override
    public String getAbstract() {
        return "Multipliziert zwei oder mehr Zahlen miteinander.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Berechnen Sie ein Produkt, etwa das Volumen eines Quaders aus Länge, Breite und Höhe.

                # Welche Werte werden benötigt?

                - **Zahl 1:** Die erste Zahl.
                - **Zahl 2:** Die zweite Zahl.

                Sie benötigen mindestens zwei Zahlen. Jede Zahl können Sie frei eingeben oder über **„Andere Wertquelle verwenden …“** aus einer Wertquelle oder Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?

                Das Produkt aller angegebenen Zahlen.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Multiplizieren“** als Funktion für einen Zahlenwert.
                2. Tragen Sie bei **„Zahl 1“** `5` und bei **„Zahl 2“** `3` ein.
                3. Wählen Sie **„Weitere Zahl hinzufügen“** und geben Sie bei **„Zahl 3“** `2` ein.

                Der Ausdruck `5 mal 3 mal 2` ergibt `30`. Sind die Maße in Metern angegeben, entspricht das einem Volumen von `30` Kubikmetern.

                # Was ist zu beachten?

                Verwenden Sie Zahlen. Zahlen als Text werden übernommen, mit Dezimalpunkt oder Dezimalkomma wie `1.5` oder `1,5`; Tausendertrennzeichen werden nicht unterstützt. Fehlende Werte und leerer Text werden wie die Zahl `0` behandelt. Andere Werte, etwa sonstige Texte, Wahrheitswerte oder Datumswerte, führen zu einer Fehlermeldung.
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
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl 1",
                                "Die erste Zahl für das Produkt."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl 2",
                                "Die Zahl, mit der das bisherige Ergebnis multipliziert wird."
                        ).withSentence("mal")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 mal #1";
    }

    @Override
    protected boolean supportsVariableArgumentCount() {
        return true;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        BigDecimal result = BigDecimal.ONE;

        for (Object rawArg : args) {
            var arg = castToNumber(rawArg);
            result = result.multiply(arg);
        }

        return new NoCodeResult(result);
    }
}
