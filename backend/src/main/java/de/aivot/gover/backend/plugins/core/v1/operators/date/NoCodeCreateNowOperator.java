package de.aivot.gover.backend.plugins.core.v1.operators.date;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeCreateNowOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeCreateNowOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "create-now";
    }

    @Override
    public String getLabel() {
        return "Aktueller Zeitpunkt";
    }

    @Override
    public String getAbstract() {
        return "Erstellt den aktuellen absoluten Zeitpunkt.";
    }

    @Override
    public String getDescription() {
        return """
                Der Operator **„Aktueller Zeitpunkt“** liefert den aktuellen absoluten Zeitpunkt.
                Bei der Ausgabe wird der für diesen Zeitpunkt gültige Offset der Zeitzone der Anwendung verwendet.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.DateTime));
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "verwende den aktuellen Zeitpunkt";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(businessTime.now());
    }
}
