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

public class NoCodeConcatOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "concat";
    }

    @Override
    public String getLabel() {
        return "Text verketten";
    }

    @Override
    public String getAbstract() {
        return "Verkettet zwei oder mehr Werte zu einem Text.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Text verketten“** verbindet zwei oder mehr Werte zu einem einzigen Text. \s
                Alle Eingaben werden dabei als Text behandelt und in der angegebenen Reihenfolge zusammengefügt.
                                
                # Anwendungsbeispiel:
                Angenommen, Sie möchten aus Vorname und Nachname einen vollständigen Namen erzeugen:
                                
                ```text
                Text verketten ("Max", " ", "Mustermann")
                ```
                                
                **Ergebnis:** \s
                "Max Mustermann"
                                
                # Wann verwenden Sie den Operator „Text verketten“?
                Verwenden Sie **„Text verketten“**, wenn Sie mehrere Werte zu einer lesbaren Zeichenkette kombinieren möchten, z. B. für Ausgaben, Labels oder Meldungen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Text 1",
                                "Der erste Textbaustein."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Text 2",
                                "Der zweite Textbaustein."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "verkette „#0“ mit „#1“";
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

        var builder = new StringBuilder();
        for (var arg : args) {
            builder.append(castToString(arg));
        }

        return new NoCodeResult(builder.toString());
    }
}
