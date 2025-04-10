package de.aivot.GoverBackend.nocode.operators.list;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class NoCodeListOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeListAvgOperator(),
                new NoCodeListConcatOperator(),
                new NoCodeListContainsOperator(),
                new NoCodeListGetOperator(),
                new NoCodeListLengthOperator(),
                new NoCodeListSelectOperator(),
                new NoCodeListSumOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.list";
    }

    @Override
    public String getLabel() {
        return "Listen-Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Operatoren für die Verarbeitung von Listen.";
    }
}
