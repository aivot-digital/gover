package de.aivot.GoverBackend.nocode.operators.date;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.operators.common.*;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the common no code operators for Gover.
 */
@Component
public class NoCodeDateOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeAddToDateOperator(),
                new NoCodeCreateDateOperator(),
                new NoCodeCreateTimeOperator(),
                new NoCodeCreateTodayOperator(),
                new NoCodeSubtractFromDateOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.date";
    }

    @Override
    public String getLabel() {
        return "Datums und Zeit Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Operatoren für die Arbeit mit Datums- und Zeitwerten.";
    }
}
