package de.aivot.GoverBackend.nocode.operators.object;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeObjectGetOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "object-get";
    }

    @Override
    public String getLabel() {
        return "Objektzugriff";
    }

    @Override
    public String getAbstract() {
        return "Gibt ein Element eines Objekts zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Objektzugriff“** gibt ein Element eines Objekts zurück. \s
                Sollte der angegebene Schlüssel nicht im Objekt vorhanden sein, wird **null** zurückgegeben.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten die Postleitzahl einer Stadt aus einem Objekt extrahieren. \s
                
                Mit dem Operator **„Objektzugriff“** wird diese Logik so formuliert: \s
                `Objektzugriff Stadt Postleitzahl`
                
                Beispielwerte: \s
                - **Objekt:** {"name": "Marburg", "postleitzahl": "35039", "ambiente": "Schön"} \s
                - **Schlüssel:** "postleitzahl"
                
                **Ergebnis:**
                - **Rückgabewert:** "35039"
                
                # Weitere Beispiele:
                - `Objektzugriff {"name": "Marburg", "postleitzahl": "35039", "ambiente": "Schön"} postleitzahl` \s
                  **Ergebnis:** "35039"
                
                # Wann verwenden Sie den Operator „Objektzugriff“?
                Verwenden Sie **„Objektzugriff“**, um ein bestimmtes Element aus einem Objekt zu extrahieren. \s
                Dieser Operator ist besonders hilfreich, wenn Sie gezielt auf Elemente innerhalb eines Objekts zugreifen möchten, z. B. um spezifische Daten zu extrahieren oder zu verarbeiten.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Object,
                        "Objekt"
                ),
                new NoCodeParameter(
                        NoCodeDataType.String,
                        "Feldbezeichner"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Any;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var obj = castToMap(args[0]);
        var field = castToString(args[1]);

        return new NoCodeResult(
                NoCodeDataType.Any,
                obj.getOrDefault(field, null)
        );
    }
}
