package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nonnull;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import java.util.Locale;

public class NoCodeFormatDurationOperator extends NoCodeOperator {
    @Nonnull
    @Override
    public String getIdentifier() {
        return "format-duration";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Zeitdauer formatieren";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Formatiert eine Zeitdauer in Sekunden als Text mit Stunden, Minuten und Sekunden, optional mit Tagen.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Geben Sie eine Zeitdauer als lesbaren Text aus, zum Beispiel die Bearbeitungszeit in Stunden und Minuten.

                # Welche Werte werden benötigt?
                - **Zeitdauer in Sekunden:** Die gesamte Zeitdauer als Sekundenwert. Sie können diesen frei eingeben; es gibt keine festen Auswahlwerte.
                - **Größte Einheit:** Wählen Sie **„Stunden“** oder **„Tage“**. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich.

                Beide Eingaben können auch aus einer anderen Wertquelle oder einer Funktion stammen. Ein Zeitdauer-Feld liefert bereits einen Sekundenwert.

                # Welches Ergebnis erhalten Sie?
                Einen Text mit den benötigten Einheiten. Mit **„Stunden“** kann die Stundenzahl über **24** liegen; mit **„Tage“** werden volle Tage zusätzlich ausgewiesen. Einheiten mit dem Wert **0** werden weggelassen. Eine Zeitdauer von **0 Sekunden** ergibt den Text **„0 Sekunden“**.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für einen Textwert **„Zeitdauer formatieren“**.
                2. Legen Sie als **„Zeitdauer in Sekunden“** **99000** fest oder übernehmen Sie einen entsprechenden Wert aus einem Zeitdauer-Feld.
                3. Wählen Sie als **„Größte Einheit“** **„Tage“**.

                Das Ergebnis ist der Text **„1 Tag 3 Stunden 30 Minuten“**. Zugeklappt lesen Sie: **„99000 Sekunden als Text mit größter Einheit Tage“**.

                # Was ist zu beachten?
                Ein Tag entspricht immer **24 Stunden**; Kalenderdaten und Zeitumstellungen werden nicht berücksichtigt. Ein leerer Wert für **„Größte Einheit“** verwendet **„Stunden“**. Fehlende oder leere Sekundenwerte werden als **0** behandelt.

                Das Ergebnis ist Text. Verwenden Sie für weitere Berechnungen den ursprünglichen Sekundenwert.
                """;
    }

    @Nonnull
    @Override
    public String[] getTags() {
        return new String[]{"Zeitdauer", "Sekunden", "Minuten", "Stunden", "Tage"};
    }

    @Nonnull
    @Override
    public String getHumanReadableTemplate() {
        return "#0 Sekunden als Text mit größter Einheit #1";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        var duration = new NoCodeParameter(NoCodeDataType.Number, "Zeitdauer in Sekunden", "Die gesamte Zeitdauer als Sekundenwert, zum Beispiel aus einem Zeitdauer-Feld.").withSentence("für die Zeitdauer in Sekunden");
        return NoCodeSignatur.of(
                NoCodeSignatur.of(NoCodeDataType.String, duration,
                        new NoCodeParameter(NoCodeDataType.String, "Größte Einheit", "Wählen Sie, ob volle Tage oder die gesamte Anzahl der Stunden angezeigt werden sollen.",
                                new NoCodeParameterOption("Stunden", "hours"),
                                new NoCodeParameterOption("Tage", "days")).withSentence("mit der größten Einheit").withOptionsAsSentencePart()));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var seconds = DurationOperatorSupport.seconds(castToNumber(args[0]));
        var largestUnit = switch (castToString(args[1]).trim().toLowerCase(Locale.ROOT)) {
            case "", "hours" -> DurationLargestUnit.Hour;
            case "days" -> DurationLargestUnit.Day;
            default -> throw new NoCodeException("Bitte wählen Sie Stunden oder Tage als größte Einheit aus.");
        };
        return new NoCodeResult(DurationValueUtils.formatSeconds(seconds, false, largestUnit));
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt den Text";
    }
}
