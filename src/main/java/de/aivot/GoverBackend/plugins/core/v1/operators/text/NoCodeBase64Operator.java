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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class NoCodeBase64Operator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "base64";
    }

    @Override
    public String getLabel() {
        return "Konvertiere zu Base64";
    }

    @Override
    public String getAbstract() {
        return "Konvertiert einen Text in eine Base64-kodierte Zeichenfolge.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Konvertiere zu Base64“** konvertiert einen Text in eine Base64-kodierte Zeichenfolge. \s
                Das Ergebnis ist ein Text, der sicher in textbasierten Formaten übertragen oder gespeichert werden kann.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten einen Text für die Übertragung in einem textbasierten Format kodieren.
                
                Mit dem Operator **„Konvertiere zu Base64“** wird diese Logik so formuliert: \s
                `Konvertiere zu Base64 Text`
                
                Beispielwerte: \s
                - **Text:** "Hallo Welt"
                
                **Ergebnis:**
                - **Rückgabewert:** "SGFsbG8gV2VsdA=="
                
                # Wahrheitswerte für den „Konvertiere zu Base64“-Operator
                - **Text "Hallo"** → **"SGFsbG8="** \s
                - **Text "a,b,c"** → **"YSxiLGM="** \s
                - **Text ""** → **""**
                
                # Wann verwenden Sie den Operator „Konvertiere zu Base64“?
                Verwenden Sie **„Konvertiere zu Base64“**, wenn Text in Base64-kodierter Form benötigt wird. \s
                Dieser Operator ist besonders nützlich, wenn Textwerte in Schnittstellen, URLs oder anderen textbasierten Formaten weitergegeben werden.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der Text, der in Base64 konvertiert werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "konvertiere „#0“ zu Base64";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        var input = castToString(args[0]);
        var encoded = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));

        return new NoCodeResult(encoded);
    }
}
