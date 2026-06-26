package de.aivot.GoverBackend.plugins.core.v1.operators.text;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoCodeRegexExtractOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "regex-extract";
    }

    @Override
    public String getLabel() {
        return "Regex-Extraktion";
    }

    @Override
    public String getAbstract() {
        return "Extrahiert alle übereinstimmenden Teile eines Textes, die einem regulären Ausdruck entsprechen.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Regex-Extraktion“** extrahiert alle übereinstimmenden Teile eines Textes, die einem regulären Ausdruck entsprechen. \s
                Das Ergebnis ist eine Liste der übereinstimmenden Teile.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten alle E-Mail-Adressen aus einem Text extrahieren.
                
                Mit dem Operator **„Regex-Extraktion“** wird diese Logik so formuliert: \s
                `Regex-Extraktion Text Regulärer Ausdruck`
                
                Beispielwerte: \s
                - **Text:** "Kontakt: test@example.com, info@example.org" \s
                - **Regulärer Ausdruck:** "[\\w.-]+@[\\w.-]+\\.\\w+"
                
                **Ergebnis:**
                - **Rückgabewert:** ["test@example.com", "info@example.org"]
                
                # Wahrheitswerte für den „Regex-Extraktion“-Operator
                - **Text "abc123 def456" Regulärer Ausdruck "\\d+"** → **["123", "456"]** \s
                - **Text "keine Übereinstimmung" Regulärer Ausdruck "\\d+"** → **[]**
                
                # Wann verwenden Sie den Operator „Regex-Extraktion“?
                Verwenden Sie **„Regex-Extraktion“**, um alle übereinstimmenden Teile eines Textes zu extrahieren, die einem bestimmten Muster entsprechen. \s
                Dieser Operator ist besonders hilfreich bei der Extraktion von Daten wie E-Mail-Adressen, Telefonnummern oder anderen formatierten Daten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der Text, aus dem extrahiert werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Regulärer Ausdruck",
                                "Der reguläre Ausdruck, der die zu extrahierenden Muster definiert."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "extrahiere aus „#0“ alle Treffer für „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        String input = castToString(args[0]);
        String regex = castToString(args[1]);

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception e) {
            throw new NoCodeException("Ungültiger regulärer Ausdruck: " + regex);
        }
        Matcher matcher = pattern.matcher(input);

        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return new NoCodeResult(matches);
    }
}
