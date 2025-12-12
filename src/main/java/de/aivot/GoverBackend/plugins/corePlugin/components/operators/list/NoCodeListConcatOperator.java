package de.aivot.GoverBackend.plugins.corePlugin.components.operators.list;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

import java.util.ArrayList;

public class NoCodeListConcatOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-concat";
    }

    @Override
    public String getLabel() {
        return "Listen zusammenfügen";
    }

    @Override
    public String getAbstract() {
        return "Kombiniert zwei Listen zu einer einzigen Liste.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Listen zusammenfügen“** dient dazu, zwei Listen zu einer einzigen Liste zu kombinieren. \s
                Das Ergebnis ist eine neue Liste, die alle Elemente der beiden ursprünglichen Listen enthält.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben zwei Listen: `["A", "B", "C"]` und `["D", "E", "F"]`. \s
                Mit dem Operator **„Listen zusammenfügen“** können Sie diese Listen kombinieren:
                
                ```text
                Listen zusammenfügen (["A", "B", "C"], ["D", "E", "F"])
                ```
                
                **Ergebnis:** \s
                Die zurückgegebene Liste enthält: `["A", "B", "C", "D", "E", "F"]`.
                
                # Wann verwenden Sie den Operator „Listen zusammenfügen“?
                Verwenden Sie **„Listen zusammenfügen“**, wenn Sie:
                - Zwei Listen zu einer einzigen Liste kombinieren möchten. \s
                - Daten aus mehreren Quellen zusammenführen möchten. \s
                - Eine vollständige Liste aus mehreren Teilmengen erstellen möchten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 1",
                                "Die erste Liste, die zusammengefügt werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 2",
                                "Die zweite Liste, die zusammengefügt werden soll."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var list1 = castToList(args[0]);
        var list2 = castToList(args[1]);

        var result = new ArrayList<>(list1);
        result.addAll(list2);

        return new NoCodeResult(result);
    }
}