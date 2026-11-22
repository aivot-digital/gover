package de.aivot.prosuna.backend.plugins.core.v1.operators.object;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeObjectGetOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "object-get";

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Objekteigenschaft lesen";
    }

    @Override
    public String getAbstract() {
        return "Liest den Wert einer benannten Eigenschaft aus einem Objekt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Lesen Sie eine einzelne Eigenschaft aus einem Objekt aus, etwa den Ort aus einer gespeicherten Adresse.

                # Welche Werte werden benötigt?
                Ein „Objekt“ und die gesuchte „Eigenschaft“. Ein festes Objekt geben Sie als JSON-Text ein, zum Beispiel `{"ort":"Berlin"}`. Den Namen der Eigenschaft geben Sie frei als Text ein, etwa `ort`; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können beide Eingaben auch aus Wertquellen oder Funktionen stammen.

                # Welches Ergebnis erhalten Sie?
                Den unter diesem Namen gespeicherten Wert. Der Wert kann beispielsweise ein Text, eine Zahl, eine Liste oder ein weiteres Objekt sein. Fehlt die Eigenschaft, erhalten Sie einen leeren Wert.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Objekteigenschaft lesen“ als Funktion für einen Wert.
                2. Wählen Sie als „Objekt“ über „Andere Wertquelle verwenden …“ die Wertquelle „Adresse“, die ein Objekt mit der Eigenschaft `ort` enthält.
                3. Tragen Sie bei „Eigenschaft“ `ort` ein.

                Steht dort „Berlin“, erhalten Sie den Text „Berlin“. Diesen können Sie beispielsweise mit „Ist gleich“ vergleichen.

                # Was ist zu beachten?
                Der Name muss einschließlich Groß- und Kleinschreibung übereinstimmen. Es wird genau eine Eigenschaft auf der obersten Ebene gelesen. `adresse.ort` wird als vollständiger Eigenschaftsname behandelt, nicht als Pfad. Für verschachtelte Objekte können Sie die Funktion mehrfach ineinander einsetzen.
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
                                NoCodeDataType.Object,
                                "Objekt",
                                "Das Objekt, dessen Eigenschaft gelesen wird."
                        ).withSentence("aus"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Eigenschaft",
                                "Der genaue Name der Eigenschaft im Objekt."
                        ).withSentence("die Eigenschaft")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Eigenschaft #1 von #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var obj = castToMap(args[0]);
        var field = castToString(args[1]);

        return new NoCodeResult(obj.getOrDefault(field, null));
    }
}
