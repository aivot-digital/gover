package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeIfOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "if";
    }

    @Override
    public String getLabel() {
        return "Wenn, dann, sonst";
    }

    @Override
    public String getAbstract() {
        return "Wählt abhängig von einer Bedingung einen von zwei Werten aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Wählen Sie abhängig von einer Bedingung zwischen zwei Ergebnissen, etwa zwischen zwei Hinweistexten.

                # Welche Werte werden benötigt?
                Eine „Bedingung“ und die beiden Werte „Ergebnis bei Wahr“ und „Ergebnis bei Falsch“. Für eine feste Bedingung wählen Sie „Wahr“ oder „Falsch“; eigene Auswahlwerte sind nicht möglich. Feste Ergebnisse können Sie frei als Text eingeben. Über „Andere Wertquelle verwenden …“ können alle drei Eingaben aus Wertquellen oder Funktionen stammen; die Ergebnisse können dabei auch andere Werttypen haben.

                # Welches Ergebnis erhalten Sie?
                Bei einer wahren Bedingung den Wert aus „Ergebnis bei Wahr“, andernfalls den Wert aus „Ergebnis bei Falsch“. Der ausgewählte Wert wird unverändert übernommen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Wenn, dann, sonst“ als Funktion für einen Textwert.
                2. Wählen Sie als „Bedingung“ die Wertquelle „Unterlagen vollständig“.
                3. Tragen Sie bei „Ergebnis bei Wahr“ `Prüfung möglich` und bei „Ergebnis bei Falsch“ `Unterlagen fehlen` ein.

                Sind die Unterlagen vollständig, erhalten Sie „Prüfung möglich“, andernfalls „Unterlagen fehlen“.

                # Was ist zu beachten?
                Verwenden Sie für die Bedingung einen Wahrheitswert. Andere Werttypen werden in Wahrheitswerte umgewandelt. Verschachtelte Funktionen in beiden Ergebniszweigen werden vor der Auswahl ausgewertet; auch ein nicht ausgewählter Zweig muss daher auswertbar sein.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.Boolean,
                                "Bedingung",
                                "Die Bedingung, die das Ergebnis bestimmt.",
                                new NoCodeParameterOption("Wahr", "true"),
                                new NoCodeParameterOption("Falsch", "false")
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Ergebnis bei Wahr",
                                "Der Wert, der bei einer wahren Bedingung übernommen wird."
                        ).withSentence("dann"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Ergebnis bei Falsch",
                                "Der Wert, der bei einer falschen Bedingung übernommen wird."
                        ).withSentence("sonst")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg0 = castToBoolean(args[0]);
        var arg1 = args[1];
        var arg2 = args[2];

        return new NoCodeResult(arg0 ? arg1 : arg2);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "wenn #0, dann #1, sonst #2";
    }
}
