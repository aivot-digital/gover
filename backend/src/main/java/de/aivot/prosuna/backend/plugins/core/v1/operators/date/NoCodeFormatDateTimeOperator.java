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
        return "Gibt einen Zeitpunkt in der Zeitzone der Anwendung als Text aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Geben Sie einen Zeitpunkt als Text aus, zum Beispiel Datum und Uhrzeit eines Termins in einer Nachricht.

                # Welche Werte werden benötigt?

                - **Zeitpunkt:** Der Zeitpunkt, den Sie ausgeben möchten.
                - **Ausgabeformat:** Die gewünschte Darstellung.

                Den Zeitpunkt können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Für das selbst festgelegte Ausgabeformat wählen Sie eine von drei Vorgaben: nur das Datum, Datum und Uhrzeit oder Datum und Uhrzeit mit Zeitverschiebung. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich.

                Beide Eingaben können auch aus einer anderen Wertquelle oder einer Funktion stammen. Ein darüber geliefertes Format muss ein gültiges Formatmuster sein, etwa `dd.MM.yyyy HH:mm`.

                # Welches Ergebnis erhalten Sie?

                Einen Text. Der Zeitpunkt wird dafür in der Zeitzone der Anwendung dargestellt; der ursprüngliche Wert bleibt unverändert.

                # Beispiel im No-Code-Editor

                1. Wählen Sie für einen Textwert **„Zeitpunkt formatieren“**.
                2. Wählen Sie als **„Zeitpunkt“** einen Termin aus einer anderen Wertquelle, der dem `22.11.2026, 09:30:00 Uhr` in der Zeitzone `Europe/Berlin` entspricht.
                3. Wählen Sie als **„Ausgabeformat“** **„Datum und Uhrzeit“**.

                In dieser Zeitzone erhalten Sie den Text `22.11.2026 09:30`. Zugeklappt lesen Sie: `„Termin“ als Text im Format „Datum und Uhrzeit“`, wenn die gewählte Wertquelle **„Termin“** heißt.

                # Was ist zu beachten?

                Die Zeitzone der Anwendung kann zu einem anderen Datum oder einer anderen Uhrzeit führen als in der ursprünglichen Angabe. Das Format **„Datum und Uhrzeit mit Zeitverschiebung“** zeigt zusätzlich die zu diesem Zeitpunkt geltende Verschiebung gegenüber der Weltzeit, zum Beispiel `+01:00`.

                Das Ergebnis ist Text. Für weitere Berechnungen oder Zeitpunktvergleiche verwenden Sie den ursprünglichen Zeitpunkt.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Text für";
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
                                "Ausgabeformat",
                                "Wählen Sie die gewünschte Darstellung. Ein Format aus einer anderen Wertquelle muss ein gültiges Formatmuster sein.",
                                new NoCodeParameterOption("Nur Datum", "dd.MM.yyyy"),
                                new NoCodeParameterOption("Datum und Uhrzeit", "dd.MM.yyyy HH:mm"),
                                new NoCodeParameterOption("Datum und Uhrzeit mit Zeitverschiebung", "yyyy-MM-dd'T'HH:mm:ssXXX")
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
