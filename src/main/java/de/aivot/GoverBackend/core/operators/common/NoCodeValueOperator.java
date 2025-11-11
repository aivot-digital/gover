package de.aivot.GoverBackend.core.operators.common;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

public class NoCodeValueOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "value";
    }

    @Override
    public String getLabel() {
        return "Wert zurückgeben";
    }

    @Override
    public String getAbstract() {
        return "Gibt einen Wert zurück";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Wert zurückgeben“** gibt einen angegebenen Wert unverändert zurück.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten einen statischen Wert oder eine Benutzereingabe direkt weiterverarbeiten.
                
                Mit dem Operator **„Wert zurückgeben“** wird diese Logik so formuliert: \\s
                `Eingabewert WIRD ZURÜCKGEGEBEN`
                
                **Ergebnis:**
                - Der eingegebene Wert wird unverändert zurückgegeben.
                
                # Wann verwenden Sie den Operator „Wert zurückgeben“?
                Verwenden Sie **„Wert zurückgeben“**, wenn Sie einen Wert ohne Änderungen weiterleiten oder ausgeben möchten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Ziel",
                                "Die ID des Elementes, dessen Wert zurückgegeben werden soll."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(args[0]);
    }
}
