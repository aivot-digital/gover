package de.aivot.GoverBackend.plugins.core.v1.operators.common;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

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
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Wert",
                                "Die ID des zu überprüfenden Elements."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var arg = castToString(args[0]);
        var dataObject = data.get(arg);
        var isVisible = dataObject != null && dataObject.getIsVisible();
        return new NoCodeResult(isVisible);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "das Feld „#0“ ist sichtbar";
    }
}
