package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeEqualsOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "equals";

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Ist gleich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob zwei Werte gleich sind.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Ist gleich“ prüfen Sie, ob zwei Werte gleich sind. Sie können beispielsweise prüfen, ob ein Formularelement einen bestimmten Text enthält.

                # Welche Werte werden benötigt?

                - **Wert 1:** Der Wert, den Sie prüfen möchten.
                - **Wert 2:** Der Wert, mit dem Sie den ersten Wert vergleichen möchten.

                Sie können beide Werte selbst festlegen, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln.

                Die Funktion gibt keine festen Auswahlwerte vor. Stellen verwendete Formularelemente oder Vorgangsdaten passende Auswahlwerte bereit, werden diese vorgeschlagen. Sie können einen Vorschlag auswählen oder einen eigenen Wert eingeben. Ohne Vorschläge geben Sie den Wert selbst ein.

                Selbst eingegebene Werte werden als Text gespeichert. Bei vorgeschlagenen Auswahlwerten wird der zur Auswahl gehörende Wert übernommen.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten einen Wahrheitswert:

                - **Wahr**, wenn die Werte nach den Vergleichsregeln übereinstimmen.
                - **Falsch**, wenn sie nicht übereinstimmen.

                # Beispiel im No-Code-Editor

                Sie möchten prüfen, ob im Formularelement „Geburtsort“ der Text „Berlin“ eingegeben wurde.

                1. Wählen Sie die Funktion „Ist gleich“.
                2. Wählen Sie für „Wert 1“ die Option „Aus Formularelement übernehmen“ und anschließend „Geburtsort“.
                3. Wählen Sie für „Wert 2“ die Option „Wert selbst festlegen“ und geben Sie `Berlin` ein.

                Der fertige Ausdruck liest sich so:

                **Wert von „Geburtsort“ ist gleich „Berlin“**

                Bei der Eingabe „Berlin“ ergibt der Vergleich **wahr**, bei „Hamburg“ ergibt er **falsch**.

                # Was ist zu beachten?

                - Beim Vergleich zweier Texte werden Groß- und Kleinschreibung sowie Leerzeichen berücksichtigt. „Berlin“ und „berlin“ sind unterschiedlich.
                - Zahlen werden nach ihrem Zahlenwert verglichen. Ist einer der Werte eine Zahl, kann auch ein als Zahl geschriebener Text übereinstimmen: Die Zahl `5` und der Text `"5"` gelten als gleich. Zwei Textwerte bleiben dagegen Texte: `"5.0"` und `"5"` sind unterschiedlich.
                - Verwenden Sie möglichst Werte derselben Art. Bei unterschiedlichen Wertarten wird der zweite Wert in der Regel an die Art des ersten Werts angepasst. Dadurch kann die Reihenfolge das Ergebnis beeinflussen.
                - Liefern beide verwendeten Wertquellen keinen Wert, ergibt der Vergleich **wahr**. Liefert nur eine Quelle keinen Wert, ergibt er **falsch**.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    /** The phrase before the second value, so subclasses keep the sentence of this operator. */
    protected String getComparisonPhrase() {
        return "gleich";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert 1",
                                "Der Wert, den Sie prüfen möchten."
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert 2",
                                "Der Wert, mit dem Sie den ersten Wert vergleichen möchten."
                        ).withSentence(getComparisonPhrase(), "ist")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg0 = args[0];
        var arg1 = args[1];

        if (arg0 == null && arg1 == null) {
            return new NoCodeResult(true);
        }

        if (arg0 == null || arg1 == null) {
            return new NoCodeResult(false);
        }

        return new NoCodeResult(isEqualToReference(arg0, arg1));
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ ist gleich „#1“";
    }
}
