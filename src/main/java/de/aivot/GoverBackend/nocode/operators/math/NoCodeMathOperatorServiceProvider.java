package de.aivot.GoverBackend.nocode.operators.math;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the arithmetic no code operators for Gover.
 */
@Component
public class NoCodeMathOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeAddOperator(),
                new NoCodeDivideOperator(),
                new NoCodeMultiplyOperator(),
                new NoCodeRoundDownOperator(),
                new NoCodeRoundUpOperator(),
                new NoCodeSubtractOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.math";
    }

    @Override
    public String getLabel() {
        return "Arithmetische Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält arithmetische Operatoren.";
    }
}
