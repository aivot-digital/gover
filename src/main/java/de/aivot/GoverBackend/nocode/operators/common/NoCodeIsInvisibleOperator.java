package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeIsInvisibleOperator extends NoCodeIsVisibleOperator {
    @Override
    public String getIdentifier() {
        return "is-invisible";
    }

    @Override
    public String getLabel() {
        return "Ist nicht sichtbar";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein bestimmtes Element auf der Benutzeroberfläche aktuell nicht sichtbar ist.";
    }

    @Override
    public String getDescription() {
        return """                
                ### **Operator: "Ist Nicht Sichtbar"**
                
                # Beschreibung:
                Der Operator **„Ist Nicht Sichtbar“** prüft, ob ein bestimmtes Element auf der Benutzeroberfläche aktuell **nicht sichtbar** ist. \s
                Dazu nimmt der Operator die **ID des Elements** als Eingabe und gibt **wahr** (true) zurück, wenn das Element ausgeblendet ist, oder **falsch** (false), wenn es sichtbar ist.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob ein Hinweisfeld mit der ID `"hinweis"` derzeit ausgeblendet ist.
                
                Mit dem Operator **„Ist Nicht Sichtbar“** formulieren Sie: \s
                ```text
                Ist Nicht Sichtbar("hinweis")
                ```
                
                **Ergebnis:**
                - **wahr:** Wenn das Element mit der ID `"hinweis"` ausgeblendet ist (z. B. per CSS `display: none` oder durch eine Bedingung). \s
                - **falsch:** Wenn das Element sichtbar ist.
                
                **Weitere Beispiele:**
                - **Ist Nicht Sichtbar("button_submit"):** → **falsch**, wenn der Button sichtbar ist. \s
                - **Ist Nicht Sichtbar("error_message"):** → **wahr**, wenn die Fehlermeldung ausgeblendet ist.
                
                # Wann verwenden Sie den Operator „Ist Nicht Sichtbar“?
                Verwenden Sie **„Ist Nicht Sichtbar“**, wenn:
                - Sie prüfen möchten, ob ein bestimmtes Element in der Benutzeroberfläche nicht angezeigt wird. \s
                - Sie auf unsichtbare Elemente reagieren möchten (z. B. um bestimmte Aktionen erst dann auszuführen, wenn ein Element versteckt ist). \s
                - Sie dynamische Interfaces verwalten, bei denen Elemente je nach Benutzeraktion ausgeblendet werden.
                
                Dieser Operator ist besonders hilfreich, um Workflows, Formulare oder Benutzeroberflächen mit dynamischen Anzeigeelementen effizient zu steuern.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        return new NoCodeResult(NoCodeDataType.Boolean, super.performEvaluation(data, args).getValueAsBoolean());
    }
}
