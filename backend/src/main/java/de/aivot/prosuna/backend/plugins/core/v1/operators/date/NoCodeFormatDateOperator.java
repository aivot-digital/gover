package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDateInput;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NoCodeFormatDateOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeFormatDateOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "format-date";
    }

    @Override
    public String getLabel() {
        return "Datum formatieren";
    }

    @Override
    public String getAbstract() {
        return "Gibt ein Datum im gewählten Format als Text aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Geben Sie ein Datum als Text aus, zum Beispiel für eine Nachricht oder eine Zusammenfassung.

                # Welche Werte werden benötigt?

                - **Datum:** Der Datumswert, den Sie ausgeben möchten.
                - **Ausgabeformat:** Die gewünschte Darstellung des Datums.

                Das Datum können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Für ein selbst festgelegtes Datum wählen Sie unter **„Datumsgenauigkeit“** Tag, Monat und Jahr, Monat und Jahr oder nur das Jahr. Für das selbst festgelegte Ausgabeformat wählen Sie eine der vier Vorgaben: Tag, Monat und Jahr; Monat und Jahr; Jahr; oder die Reihenfolge Jahr-Monat-Tag. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich.

                Beide Eingaben können auch aus einer anderen Wertquelle oder einer Funktion stammen. Ein darüber geliefertes Format muss ein gültiges Formatmuster sein, etwa `dd.MM.yyyy`.

                # Welches Ergebnis erhalten Sie?

                Einen Text im gewählten Format. Der ursprüngliche Datumswert bleibt unverändert.

                # Beispiel im No-Code-Editor

                1. Wählen Sie für einen Textwert **„Datum formatieren“**.
                2. Legen Sie als **„Datum“** den `22.11.2026` fest oder wählen Sie ein entsprechendes Datumsfeld als Wertquelle.
                3. Wählen Sie als **„Ausgabeformat“** **„Tag, Monat und Jahr“**.

                Das Ergebnis ist der Text `22.11.2026`. Zugeklappt lesen Sie: `22.11.2026 als Text im Format „Tag, Monat und Jahr“`.

                # Was ist zu beachten?

                Ein Datumswert kann auch nur aus Monat und Jahr oder nur aus einem Jahr bestehen. Wählen Sie dann ein Format, das keine fehlenden Bestandteile verlangt. Aus `November 2026` kann beispielsweise `11.2026`, aber kein vollständiges Tagesdatum entstehen.

                Für die Textdarstellung eines Zeitpunkts mit Uhrzeit verwenden Sie **„Zeitpunkt formatieren“**. Das Ergebnis dieser Funktion ist Text und kein neuer Datumswert.
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
                                NoCodeDataType.Date,
                                "Datum",
                                "Das zu formatierende Datum."
                        ).withSentence("das Datum").withDateInput(NoCodeDateInput.CalendarOrDateTime),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Ausgabeformat",
                                "Wählen Sie die gewünschte Darstellung. Ein Format aus einer anderen Wertquelle muss ein gültiges Formatmuster sein.",
                                new NoCodeParameterOption("Tag, Monat und Jahr", "dd.MM.yyyy"),
                                new NoCodeParameterOption("Monat und Jahr", "MM.yyyy"),
                                new NoCodeParameterOption("Jahr", "yyyy"),
                                new NoCodeParameterOption("Jahr-Monat-Tag", "yyyy-MM-dd")
                        ).withSentence("im Format")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 als Text im Format #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var formatter = requireFormatter(args[1]);

        try {
            var calendarValue = tryCastToCalendarValue(args[0]);
            if (calendarValue != null) {
                // Formatting the original type preserves Year and YearMonth precision.
                // Incompatible fields (for example "dd" on YearMonth) fail explicitly.
                return new NoCodeResult(formatter.format(calendarValue));
            }

            // Existing format-date expressions historically accepted DateTime values at
            // runtime despite declaring a Date parameter. Preserve that behavior while
            // the dedicated format-datetime operator provides the correct editor contract.
            return formatDateTimeValue(
                    args[0],
                    formatter,
                    "Ungültiger Datumswert: " + castToString(args[0])
            );
        } catch (DateTimeException exception) {
            throw incompatibleFormat();
        }
    }

    protected final DateTimeFormatter requireFormatter(Object patternValue) throws NoCodeException {
        var formatPattern = castToString(patternValue).trim();

        if (formatPattern.isEmpty()) {
            throw new NoCodeException("Das Datumsformat darf nicht leer sein.");
        }

        try {
            return DateTimeFormatter.ofPattern(formatPattern, Locale.GERMAN);
        } catch (IllegalArgumentException exception) {
            throw new NoCodeException("Ungültiges Datumsformat: " + formatPattern);
        }
    }

    protected final NoCodeResult formatDateTimeValue(
            Object value,
            DateTimeFormatter formatter,
            String invalidValueMessage
    ) throws NoCodeException {
        var instant = requireDateTime(value, invalidValueMessage).toInstant();

        try {
            return new NoCodeResult(formatter.format(instant.atZone(businessTime.zoneId())));
        } catch (DateTimeException exception) {
            throw incompatibleFormat();
        }
    }

    private NoCodeException incompatibleFormat() {
        return new NoCodeException(
                "Das gewählte Format ist für den übergebenen Datumswert nicht anwendbar."
        );
    }
}
