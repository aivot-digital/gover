package de.aivot.GoverBackend.nocode.operators.object;

import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.stereotype.Component;

/**
 * This class provides the NoCodeOperators for the object operators.
 */
@Component
public class NoCodeObjectOperatorServiceProvider implements NoCodeOperatorServiceProvider {
    @Override
    public NoCodeOperator[] getOperators() {
        return new NoCodeOperator[]{
                new NoCodeObjectGetOperator(),
        };
    }

    @Override
    public String getPackageName() {
        return "aivot.object";
    }

    @Override
    public String getLabel() {
        return "Objekt-Operatoren";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Operatoren, welche den Zugriff auf Objekte ermöglichen.";
    }
}
