package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NoCodeRegexFindOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "regex-find";
    }

    @Override
    public String getLabel() {
        return "Text enthält Muster";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein Teil eines Textes zu einem regulären Ausdruck passt.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Text enthält Muster“** prüft, ob irgendein Teil eines Textes zu einem regulären Ausdruck passt. \s
                Anders als bei **„Regex-Übereinstimmung“** muss nicht der gesamte Text dem Muster entsprechen.

                # Beispiele:
                - **Text** „Aktenzeichen AZ-2026-17“, **Regulärer Ausdruck** „AZ-\\d{4}“ → **wahr**
                - **Text** „Aktenzeichen fehlt“, **Regulärer Ausdruck** „AZ-\\d{4}“ → **falsch**

                Ein ungültiger regulärer Ausdruck führt zu einem Fehler. \s
                Um das Gegenteil zu prüfen, kombinieren Sie den Operator mit **„Nicht“**.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der zu durchsuchende Text."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Regulärer Ausdruck",
                                "Das Muster, nach dem im Text gesucht werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ enthält das Muster „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var input = castToString(args[0]);
        var regex = castToString(args[1]);

        try {
            return new NoCodeResult(Pattern.compile(regex).matcher(input).find());
        } catch (PatternSyntaxException e) {
            throw new NoCodeException("Ungültiger regulärer Ausdruck: " + regex);
        }
    }
}
