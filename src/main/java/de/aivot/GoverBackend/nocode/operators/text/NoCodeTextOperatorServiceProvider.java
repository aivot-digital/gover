package de.aivot.GoverBackend.nocode.operators.text;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the arithmetic no code operators for Gover.
 */
@Component
public class NoCodeTextOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeRegexExtractOperator(),
                new NoCodeRegexMatchOperator(),
                new NoCodeSplitOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.text";
    }

    @Override
    public String getLabel() {
        return "Textoperatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Textoperatoren.";
    }
}
