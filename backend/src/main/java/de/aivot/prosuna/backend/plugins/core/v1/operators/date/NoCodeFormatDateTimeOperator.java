package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;

public class NoCodeFormatDateTimeOperator extends NoCodeFormatDateOperator {
    public NoCodeFormatDateTimeOperator(BusinessTime businessTime) {
        super(businessTime);
    }

    @Override
    public String getIdentifier() {
        return "format-datetime";
    }

    @Override
    public String getLabel() {
        return "Zeitpunkt formatieren";
    }

    @Override
    public String getAbstract() {
        return "Formatiert einen absoluten Zeitpunkt in der Zeitzone der Anwendung.";
    }

    @Override
    public String getDescription() {
        return """
                Formatiert einen absoluten Zeitpunkt in der Zeitzone der Anwendung.
                Der numerische Offset wird aus der Zone und dem jeweiligen Zeitpunkt abgeleitet.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.DateTime,
                                "Zeitpunkt",
                                "Der zu formatierende absolute Zeitpunkt."
                        ).withSentence("den Zeitpunkt"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Format",
                                "Das Ausgabeformat als DateTimeFormatter-Muster.",
                                new NoCodeParameterOption("Datum (TT.MM.JJJJ)", "dd.MM.yyyy"),
                                new NoCodeParameterOption("Datum mit Zeit", "dd.MM.yyyy HH:mm"),
                                new NoCodeParameterOption("ISO Datum/Zeit", "yyyy-MM-dd'T'HH:mm:ssXXX")
                        ).withSentence("im Format")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        // Do not inherit format-date's legacy DateTime fallback in reverse. New
        // expressions should enforce the dedicated operator's DateTime contract.
        return formatDateTimeValue(
                args[0],
                requireFormatter(args[1]),
                "Ungültiger Zeitpunkt: " + castToString(args[0])
        );
    }
}
