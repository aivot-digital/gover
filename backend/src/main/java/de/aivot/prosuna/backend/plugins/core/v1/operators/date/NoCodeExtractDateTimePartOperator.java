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

public class NoCodeExtractDateTimePartOperator extends NoCodeOperator {
    public enum Part {
        DATE,
        TIME,
    }

    private final Part part;
    private final BusinessTime businessTime;

    public NoCodeExtractDateTimePartOperator(Part part, BusinessTime businessTime) {
        this.part = part;
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return switch (part) {
            case DATE -> "extract-date-from-datetime";
            case TIME -> "extract-time-from-datetime";
        };
    }

    @Override
    public String getLabel() {
        return switch (part) {
            case DATE -> "Datum aus Zeitpunkt";
            case TIME -> "Uhrzeit aus Zeitpunkt";
        };
    }

    @Override
    public String getAbstract() {
        return switch (part) {
            case DATE -> "Ermittelt das Kalenderdatum eines Zeitpunkts in der Zeitzone der Anwendung.";
            case TIME -> "Ermittelt die lokale Uhrzeit eines Zeitpunkts in der Zeitzone der Anwendung.";
        };
    }

    @Override
    public String getDescription() {
        return switch (part) {
            case DATE -> """
                    # Wofür verwenden Sie diese Funktion?

                    Ermitteln Sie das Datum eines Zeitpunkts, zum Beispiel den Kalendertag eines Termins.

                    # Welche Werte werden benötigt?

                    - **Zeitpunkt:** Den Zeitpunkt können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Für eine selbst festgelegte Angabe verwenden Sie das Datums- und Uhrzeitfeld; eine feste Auswahlliste gibt es nicht.

                    # Welches Ergebnis erhalten Sie?

                    Ein vollständiges Datum mit Tag, Monat und Jahr in der Zeitzone der Anwendung. Die Uhrzeit ist nicht Teil des Ergebnisses.

                    # Beispiel im No-Code-Editor

                    1. Wählen Sie im Eingabefeld **„Datum“** von **„Datumswerte vergleichen“** die Funktion **„Datum aus Zeitpunkt“**.
                    2. Wählen Sie als **„Zeitpunkt“** die Wertquelle **„Termin“**, die dem `22.11.2026, 09:30:00 Uhr` in der Zeitzone `Europe/Berlin` entspricht.
                    3. Vergleichen Sie das ermittelte Datum beispielsweise mit **„Heutiges Datum“**.

                    In dieser Zeitzone erhalten Sie `22.11.2026`. Zugeklappt lesen Sie: `Datum von „Termin“`.

                    # Was ist zu beachten?

                    Maßgeblich ist die Zeitzone der Anwendung. Derselbe Zeitpunkt kann in einer anderen Zeitzone auf einen anderen Kalendertag fallen. Ein Datum ohne Uhrzeit ist selbst kein Zeitpunkt.
                    """;
            case TIME -> """
                    # Wofür verwenden Sie diese Funktion?

                    Ermitteln Sie die Uhrzeit eines Zeitpunkts, zum Beispiel um einen Termin mit den Öffnungszeiten zu vergleichen.

                    # Welche Werte werden benötigt?

                    - **Zeitpunkt:** Den Zeitpunkt können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Für eine selbst festgelegte Angabe verwenden Sie das Datums- und Uhrzeitfeld; eine feste Auswahlliste gibt es nicht.

                    # Welches Ergebnis erhalten Sie?

                    Eine Uhrzeit auf die Sekunde genau in der Zeitzone der Anwendung. Datum und Sekundenbruchteile sind nicht Teil des Ergebnisses.

                    # Beispiel im No-Code-Editor

                    1. Wählen Sie im Eingabefeld **„Uhrzeit“** von **„Uhrzeiten vergleichen“** die Funktion **„Uhrzeit aus Zeitpunkt“**.
                    2. Wählen Sie als **„Zeitpunkt“** die Wertquelle **„Termin“**, die dem `22.11.2026, 09:30:00 Uhr` in der Zeitzone `Europe/Berlin` entspricht.
                    3. Vergleichen Sie die ermittelte Uhrzeit beispielsweise mit `17:00:00 Uhr`.

                    In dieser Zeitzone erhalten Sie `09:30:00 Uhr`. Zugeklappt lesen Sie: `Uhrzeit von „Termin“`.

                    # Was ist zu beachten?

                    Maßgeblich ist die Zeitzone der Anwendung. Das Ergebnis enthält weder das Datum noch eine Zeitzone und unterscheidet deshalb nicht zwischen zwei Vorkommen derselben Uhrzeit bei einer Zeitumstellung. Sekundenbruchteile werden weggelassen, nicht gerundet.
                    """;
        };
    }

    @Override
    public String getSentenceLead() {
        return part == Part.DATE ? "ergibt das Datum" : "ergibt die Uhrzeit";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        part == Part.DATE ? NoCodeDataType.Date : NoCodeDataType.Time,
                        new NoCodeParameter(
                                NoCodeDataType.DateTime,
                                "Zeitpunkt",
                                "Der Zeitpunkt, dessen Datum oder Uhrzeit Sie benötigen."
                        ).withSentence("aus")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return switch (part) {
            case DATE -> "Datum von #0";
            case TIME -> "Uhrzeit von #0";
        };
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var instant = requireDateTime(
                args[0],
                "Ungültiger Zeitpunkt: " + castToString(args[0])
        ).toInstant();
        var applicationDateTime = instant.atZone(businessTime.zoneId());

        return new NoCodeResult(
                part == Part.DATE
                        ? applicationDateTime.toLocalDate()
                        // The public Time contract has second precision even when the
                        // source Instant contains a fractional second.
                        : applicationDateTime.toLocalTime().withNano(0)
        );
    }
}
