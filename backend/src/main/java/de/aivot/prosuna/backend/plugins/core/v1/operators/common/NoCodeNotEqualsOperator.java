package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import jakarta.annotation.Nullable;

public class NoCodeNotEqualsOperator extends NoCodeEqualsOperator {
    @Override
    public String getIdentifier() {
        return "not-equals";
    }

    @Override
    public String getLabel() {
        return "Ist nicht gleich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob zwei Werte nicht gleich sind.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Ist nicht gleich“ prüfen Sie, ob zwei Werte voneinander abweichen. Sie können beispielsweise prüfen, ob der Wert eines Formularelements von einem festgelegten Vergleichswert abweicht.

                # Welche Werte werden benötigt?

                - **Wert 1:** Der Wert, den Sie prüfen möchten.
                - **Wert 2:** Der Wert, mit dem Sie den ersten Wert vergleichen möchten.

                Sie können beide Werte selbst festlegen, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln.

                Die Funktion gibt keine festen Auswahlwerte vor. Stellen verwendete Formularelemente oder Vorgangsdaten passende Auswahlwerte bereit, werden diese vorgeschlagen. Sie können einen Vorschlag auswählen oder einen eigenen Wert eingeben. Ohne Vorschläge geben Sie den Wert selbst ein.

                Selbst eingegebene Werte werden als Text gespeichert. Bei vorgeschlagenen Auswahlwerten wird der zur Auswahl gehörende Wert übernommen.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten einen Wahrheitswert:

                - **Wahr**, wenn die Werte nach den Vergleichsregeln nicht übereinstimmen.
                - **Falsch**, wenn sie übereinstimmen.

                # Beispiel im No-Code-Editor

                Sie möchten prüfen, ob im Formularelement „Geburtsort“ ein anderer Text als „Berlin“ eingegeben wurde.

                1. Wählen Sie die Funktion „Ist nicht gleich“.
                2. Wählen Sie für „Wert 1“ die Option „Aus Formularelement übernehmen“ und anschließend „Geburtsort“.
                3. Wählen Sie für „Wert 2“ die Option „Wert selbst festlegen“ und geben Sie `Berlin` ein.

                Der fertige Ausdruck liest sich so:

                **Wert von „Geburtsort“ ist nicht gleich „Berlin“**

                Bei der Eingabe „Hamburg“ ergibt der Vergleich **wahr**, bei „Berlin“ ergibt er **falsch**.

                # Was ist zu beachten?

                - Beim Vergleich zweier Texte werden Groß- und Kleinschreibung sowie Leerzeichen berücksichtigt. „Berlin“ und „berlin“ sind unterschiedlich.
                - Zahlen werden nach ihrem Zahlenwert verglichen. Eine Zahl stimmt nur mit einer Zahl oder einem als Zahl geschriebenen Text überein, auch mit Dezimalkomma: Die Zahl `5` und die Texte `"5"` und `"5,0"` ergeben bei dieser Funktion **falsch**. Zwei Textwerte bleiben dagegen Texte: `"5.0"` und `"5"` ergeben **wahr**.
                - Verwenden Sie möglichst Werte derselben Art. Bei unterschiedlichen Wertarten wird der zweite Wert in der Regel an die Art des ersten Werts angepasst. Dadurch kann die Reihenfolge das Ergebnis beeinflussen. Lässt sich ein Wert nicht anpassen, etwa der Text „abc“ an die Zahl `3` oder der Text „kein Datum“ an ein Datum, ergibt der Vergleich **wahr**.
                - Liefern beide verwendeten Wertquellen keinen Wert, ergibt der Vergleich **falsch**. Liefert nur eine Quelle keinen Wert, ergibt er **wahr**. Ein fehlender Wert stimmt deshalb beispielsweise nicht mit „Berlin“ überein.
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
        return "„#0“ ist nicht gleich „#1“";
    }

    @Override
    protected String getComparisonPhrase() {
        return "nicht gleich";
    }
}
