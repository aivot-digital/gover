package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NoCodeCombineDateAndTimeOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeCombineDateAndTimeOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "combine-date-and-time";
    }

    @Override
    public String getLabel() {
        return "Datum und Uhrzeit verbinden";
    }

    @Override
    public String getAbstract() {
        return "Verbindet ein Datum und eine Uhrzeit zu einem Zeitpunkt in der Zeitzone der Anwendung.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Verbinden Sie ein Datum und eine Uhrzeit zu einem Zeitpunkt, zum Beispiel für den Beginn einer Veranstaltung.

                # Welche Werte werden benötigt?

                - **Datum:** Ein vollständiges Datum mit Tag, Monat und Jahr.
                - **Uhrzeit:** Die Uhrzeit an diesem Datum.

                Beide Werte können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Für selbst festgelegte Werte stehen ein Datumsfeld und ein Uhrzeitfeld bereit; eine feste Auswahlliste gibt es nicht.

                # Welches Ergebnis erhalten Sie?

                Einen Zeitpunkt. Datum und Uhrzeit werden in der Zeitzone der Anwendung zusammengeführt.

                # Beispiel im No-Code-Editor

                1. Wählen Sie für den gewünschten Zeitpunkt **„Datum und Uhrzeit verbinden“**.
                2. Legen Sie unter **„Datum“** den `22.11.2026` und unter **„Uhrzeit“** `09:30:00 Uhr` fest.
                3. Verwenden Sie das Ergebnis zum Beispiel als Ausgangszeitpunkt in **„Zum Zeitpunkt hinzufügen“**.

                In der Zeitzone `Europe/Berlin` entsteht der Zeitpunkt `22.11.2026, 09:30:00 Uhr`. Zugeklappt lesen Sie: `22.11.2026 um 09:30:00 Uhr`.

                # Was ist zu beachten?

                Die Zeitzone der Anwendung bestimmt, welcher Zeitpunkt gemeint ist. Ein Monat ohne Tag oder ein Jahr allein reicht dafür nicht aus.

                Bei einer Zeitumstellung kann eine Uhrzeit ausfallen oder zweimal vorkommen. Ausfallende Uhrzeiten werden abgelehnt; bei einer doppelt vorkommenden Uhrzeit wird das erste Vorkommen verwendet.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Zeitpunkt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.DateTime,
                        new NoCodeParameter(
                                NoCodeDataType.Date,
                                "Datum",
                                "Das Datum mit Tag, Monat und Jahr, an dem die Uhrzeit gelten soll."
                        ).withSentence("aus dem Datum"),
                        new NoCodeParameter(
                                NoCodeDataType.Time,
                                "Uhrzeit",
                                "Die Uhrzeit am angegebenen Datum in der Zeitzone der Anwendung."
                        ).withSentence("und der Uhrzeit")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 um #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var calendarValue = requireCalendarValue(
                args[0],
                "Ein vollständiges Datum im Format JJJJ-MM-TT wird benötigt."
        );
        if (!(calendarValue instanceof LocalDate date)) {
            // Resolving Year or YearMonth would invent calendar components that the
            // user never supplied, so only day-precision values can become an instant.
            throw new NoCodeException("Ein vollständiges Datum im Format JJJJ-MM-TT wird benötigt.");
        }
        var time = requireTime(args[1], "Ungültige Uhrzeit: " + castToString(args[1]));

        try {
            return new NoCodeResult(businessTime.resolve(LocalDateTime.of(date, time)));
        } catch (DateTimeException exception) {
            throw new NoCodeException(
                    "Die lokale Datums- und Uhrzeitangabe %s existiert in der Zeitzone der Anwendung (%s) nicht."
                            .formatted(LocalDateTime.of(date, time), businessTime.zoneId())
            );
        }
    }
}
