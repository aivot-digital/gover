package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;

public class NoCodeFormatTimeOperator extends NoCodeOperator {
    private static final String MINUTE_PATTERN = "HH:mm";
    private static final String SECOND_PATTERN = "HH:mm:ss";
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern(MINUTE_PATTERN);
    private static final DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern(SECOND_PATTERN);

    @Override
    public String getIdentifier() {
        return "format-time";
    }

    @Override
    public String getLabel() {
        return "Uhrzeit formatieren";
    }

    @Override
    public String getAbstract() {
        return "Gibt eine Uhrzeit wahlweise mit oder ohne Sekunden als Text aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Geben Sie eine Uhrzeit als Text aus, zum Beispiel eine Öffnungszeit mit oder ohne Sekunden.

                # Welche Werte werden benötigt?
                - **Uhrzeit:** Die Uhrzeit, die Sie ausgeben möchten.
                - **Ausgabeformat:** Wählen Sie **„Stunde und Minute“** oder **„Stunde, Minute und Sekunde“**.

                Die Uhrzeit können Sie selbst festlegen, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Das selbst festgelegte Ausgabeformat ist eine feste Auswahl; zusätzliche eigene Einträge sind nicht möglich. Auch das Format kann aus einer anderen Wertquelle oder einer Funktion stammen. Dabei werden die Formatwerte **HH:mm** oder **HH:mm:ss** erwartet.

                # Welches Ergebnis erhalten Sie?
                Einen Text im gewählten Format. Die ursprüngliche Uhrzeit bleibt unverändert.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für einen Textwert **„Uhrzeit formatieren“**.
                2. Legen Sie als **„Uhrzeit“** **09:30:45 Uhr** fest oder wählen Sie ein entsprechendes Uhrzeitfeld als Wertquelle.
                3. Wählen Sie als **„Ausgabeformat“** **„Stunde und Minute“**.

                Das Ergebnis ist der Text **„09:30“**. Zugeklappt lesen Sie: **„09:30:45 Uhr als Text im Format „Stunde und Minute““**.

                # Was ist zu beachten?
                Bei der Darstellung ohne Sekunden werden die Sekunden weggelassen; es wird nicht auf die nächste Minute gerundet. Die Funktion ergänzt weder ein Datum noch eine Zeitzone. Das Ergebnis ist Text und keine neue Uhrzeit.
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
                                NoCodeDataType.Time,
                                "Uhrzeit",
                                "Die zu formatierende lokale Uhrzeit."
                        ).withSentence("die Uhrzeit"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Ausgabeformat",
                                "Wählen Sie, ob die Sekunden im Text erscheinen sollen.",
                                new NoCodeParameterOption("Stunde und Minute", MINUTE_PATTERN),
                                new NoCodeParameterOption("Stunde, Minute und Sekunde", SECOND_PATTERN)
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
        var time = requireTime(args[0], "Ungültige Uhrzeit: " + castToString(args[0]));
        var pattern = castToString(args[1]).trim();

        var formatter = switch (pattern) {
            case MINUTE_PATTERN -> MINUTE_FORMATTER;
            case SECOND_PATTERN -> SECOND_FORMATTER;
            default -> throw new NoCodeException("Ungültiges Uhrzeitformat: " + pattern);
        };

        return new NoCodeResult(time.format(formatter));
    }
}
