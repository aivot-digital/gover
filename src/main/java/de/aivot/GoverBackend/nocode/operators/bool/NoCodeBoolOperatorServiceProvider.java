package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the boolean no code operators for Gover.
 */
@Component
public class NoCodeBoolOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeAndOperator(),
                new NoCodeNotOperator(),
                new NoCodeOrOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.bool";
    }

    @Override
    public String getLabel() {
        return "Logische NoCode-Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält logische Operatoren für NoCode-Ausdrücke.";
    }
}
