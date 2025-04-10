package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeIsVisibleOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "is-visible";
    }

    @Override
    public String getLabel() {
        return "Ist sichtbar";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein bestimmtes Element auf der Benutzeroberfläche aktuell sichtbar ist.";
    }

    @Override
    public String getDescription() {
        return """                
                # Beschreibung:
                Der Operator **„Ist Sichtbar“** prüft, ob ein bestimmtes Element auf der Benutzeroberfläche aktuell **sichtbar** ist. \s
                Dazu nimmt der Operator die **ID des Elements** als Eingabe und gibt **wahr** (true) zurück, wenn das Element sichtbar ist, oder **falsch** (false), wenn es ausgeblendet ist.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob ein Hinweisfeld mit der ID `"hinweis"` angezeigt wird.
                
                Mit dem Operator **„Ist Sichtbar“** formulieren Sie: \s
                ```text
                Ist Sichtbar("hinweis")
                ```
                
                **Ergebnis:**
                - **wahr:** Wenn das Element mit der ID `"hinweis"` auf der Oberfläche angezeigt wird. \s
                - **falsch:** Wenn das Element ausgeblendet ist (z. B. per CSS `display: none` oder durch eine Bedingung).
                
                **Weitere Beispiele:**
                - **Ist Sichtbar("button_submit"):** → **wahr**, wenn der Button sichtbar ist. \s
                - **Ist Sichtbar("error_message"):** → **falsch**, wenn die Fehlermeldung ausgeblendet ist.
                
                # Wann verwenden Sie den Operator „Ist Sichtbar“?
                Verwenden Sie **„Ist Sichtbar“**, wenn:
                - Sie prüfen möchten, ob ein bestimmtes Element in der Benutzeroberfläche angezeigt wird. \s
                - Sie auf sichtbare oder ausgeblendete Elemente reagieren möchten (z. B. um Bedingungen oder Aktionen zu steuern). \s
                - Sie dynamische Interfaces verwalten, bei denen Elemente je nach Benutzeraktion ein- oder ausgeblendet werden.
                
                Dieser Operator ist besonders hilfreich für interaktive Workflows, dynamische Formulare oder benutzerfreundliche Oberflächen mit einblendbaren Elementen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Wert"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Boolean;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var arg = castToString(args[0]);
        var isVisible = data.getVisibilities().getOrDefault(arg, true);
        return new NoCodeResult(NoCodeDataType.Boolean, isVisible);
    }
}
