package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDateInput;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class NoCodeTemporalCompareOperator extends NoCodeOperator {
    public enum TemporalType {
        DATE(NoCodeDataType.Date, "date", "Datumswerte"),
        TIME(NoCodeDataType.Time, "time", "Uhrzeiten"),
        DATETIME(NoCodeDataType.DateTime, "datetime", "Zeitpunkte");

        private final NoCodeDataType dataType;
        private final String identifierPart;
        private final String label;

        TemporalType(NoCodeDataType dataType, String identifierPart, String label) {
            this.dataType = dataType;
            this.identifierPart = identifierPart;
            this.label = label;
        }
    }

    private static final String EQUAL = "gleich";
    private static final String NOT_EQUAL = "ungleich";
    private static final String BEFORE = "vor";
    private static final String BEFORE_OR_EQUAL = "vor-oder-gleich";
    private static final String AFTER = "nach";
    private static final String AFTER_OR_EQUAL = "nach-oder-gleich";

    private final TemporalType temporalType;

    public NoCodeTemporalCompareOperator(TemporalType temporalType) {
        this.temporalType = temporalType;
    }

    @Override
    public String getIdentifier() {
        return "compare-" + temporalType.identifierPart;
    }

    @Override
    public String getLabel() {
        return temporalType.label + " vergleichen";
    }

    @Override
    public String getAbstract() {
        return switch (temporalType) {
            case DATE -> "Vergleicht zwei Datumsangaben ohne Uhrzeit.";
            case TIME -> "Vergleicht zwei Uhrzeiten.";
            case DATETIME -> "Vergleicht zwei Zeitpunkte mit Datum und Uhrzeit.";
        };
    }

    @Override
    public String getDescription() {
        return switch (temporalType) {
            case DATE -> """
                    # Wofür verwenden Sie diese Funktion?

                    Mit **„Datumswerte vergleichen“** prüfen Sie beispielsweise, ob ein Datum spätestens an einem bestimmten Stichtag liegt.

                    # Welche Werte werden benötigt?

                    - **Datum:** Die erste Angabe für den Vergleich.
                    - **Vergleichsart:** Der Vergleich, der geprüft werden soll.
                    - **Vergleichsdatum:** Die Angabe, mit der verglichen wird.

                    Beide Datumsangaben können Sie selbst festlegen, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Die Funktion gibt dafür keine Auswahlwerte vor.

                    Für **„Vergleichsart“** stehen sechs Auswahlwerte bereit. Zusätzliche eigene Einträge sind nicht möglich. Über **„Andere Wertquelle verwenden …“** können Sie die Vergleichsart auch übernehmen oder mit einer Funktion ermitteln.

                    # Welches Ergebnis erhalten Sie?

                    Sie erhalten `Wahr`, wenn der gewählte Vergleich zutrifft, andernfalls `Falsch`.

                    # Beispiel im No-Code-Editor

                    Sie möchten prüfen, ob ein Termin heute stattfindet oder bereits vergangen ist.

                    1. Wählen Sie **„Datumswerte vergleichen“**.
                    2. Übernehmen Sie für **„Datum“** den Wert eines Datumsfeldes, beispielsweise **„Termin“**.
                    3. Wählen Sie für **„Vergleichsart“** die Option **„früher als oder gleich“**.
                    4. Wählen Sie für **„Vergleichsdatum“** die Option **„Mit Funktion ermitteln“** und anschließend **„Heutiges Datum“**.

                    Der Ausdruck liest sich so:

                    `Wert von „Termin“ ist früher als oder gleich (heutiges Datum)`

                    Bei einer Auswertung am `22.11.2026` ergibt ein Termin am `22.11.2026` `Wahr`, ein Termin am `23.11.2026` `Falsch`.

                    # Was ist zu beachten?

                    Beide Angaben müssen dieselben Bestandteile enthalten: entweder Tag, Monat und Jahr, nur Monat und Jahr oder nur das Jahr. Das gilt für alle Vergleichsarten.

                    Für selbst festgelegte Werte wählen Sie unter **„Datumsgenauigkeit“** die benötigten Bestandteile.
                    """;
            case TIME -> """
                    # Wofür verwenden Sie diese Funktion?

                    Mit **„Uhrzeiten vergleichen“** prüfen Sie beispielsweise, ob eine eingetragene Beginnzeit vor einer bestimmten Uhrzeit liegt.

                    # Welche Werte werden benötigt?

                    - **Uhrzeit:** Die erste Angabe für den Vergleich.
                    - **Vergleichsart:** Der Vergleich, der geprüft werden soll.
                    - **Vergleichsuhrzeit:** Die Angabe, mit der verglichen wird.

                    Beide Uhrzeiten können Sie selbst festlegen, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Die Funktion gibt dafür keine Auswahlwerte vor.

                    Für **„Vergleichsart“** stehen sechs Auswahlwerte bereit. Zusätzliche eigene Einträge sind nicht möglich. Über **„Andere Wertquelle verwenden …“** können Sie die Vergleichsart auch übernehmen oder mit einer Funktion ermitteln.

                    # Welches Ergebnis erhalten Sie?

                    Sie erhalten `Wahr`, wenn der gewählte Vergleich zutrifft, andernfalls `Falsch`.

                    # Beispiel im No-Code-Editor

                    Sie möchten prüfen, ob eine Beginnzeit vor `17 Uhr` liegt.

                    1. Wählen Sie **„Uhrzeiten vergleichen“**.
                    2. Übernehmen Sie für **„Uhrzeit“** den Wert eines Uhrzeitfeldes, beispielsweise **„Beginn“**.
                    3. Wählen Sie für **„Vergleichsart“** die Option **„früher als“**.
                    4. Wählen Sie für **„Vergleichsuhrzeit“** die Option **„Wert selbst festlegen“** und geben Sie `17:00:00 Uhr` ein.

                    Der Ausdruck liest sich so:

                    `Wert von „Beginn“ ist früher als 17:00:00 Uhr`

                    Eine Beginnzeit von `09:00 Uhr` ergibt `Wahr`, eine Beginnzeit von `17:00 Uhr` `Falsch`.

                    # Was ist zu beachten?

                    Verglichen werden Stunden, Minuten und Sekunden innerhalb eines Tages. `01:00 Uhr` gilt als früher als `23:00 Uhr`.

                    Für eine zeitliche Reihenfolge über mehrere Tage verwenden Sie **„Zeitpunkte vergleichen“**. Liefert eine Wertquelle einen Zeitpunkt, wird dessen Uhrzeit in der für Prosuna eingestellten Zeitzone verglichen. Das gilt auch für gespeicherten Text wie `2026-11-22T12:30:00Z`, der in der Zeitzone Berlin als `13:30:00` Uhr gilt.
                    """;
            case DATETIME -> """
                    # Wofür verwenden Sie diese Funktion?

                    Mit **„Zeitpunkte vergleichen“** prüfen Sie beispielsweise, ob ein Termin bereits erreicht oder überschritten ist.

                    # Welche Werte werden benötigt?

                    - **Zeitpunkt:** Die erste Angabe für den Vergleich.
                    - **Vergleichsart:** Der Vergleich, der geprüft werden soll.
                    - **Vergleichszeitpunkt:** Die Angabe, mit der verglichen wird.

                    Beide Zeitpunkte können Sie selbst festlegen, aus einer verfügbaren Wertquelle übernehmen oder mit einer Funktion ermitteln. Die Funktion gibt dafür keine Auswahlwerte vor.

                    Für **„Vergleichsart“** stehen sechs Auswahlwerte bereit. Zusätzliche eigene Einträge sind nicht möglich. Über **„Andere Wertquelle verwenden …“** können Sie die Vergleichsart auch übernehmen oder mit einer Funktion ermitteln.

                    # Welches Ergebnis erhalten Sie?

                    Sie erhalten `Wahr`, wenn der gewählte Vergleich zutrifft, andernfalls `Falsch`.

                    # Beispiel im No-Code-Editor

                    Sie möchten prüfen, ob ein Termin bereits erreicht ist.

                    1. Wählen Sie **„Zeitpunkte vergleichen“**.
                    2. Übernehmen Sie für **„Zeitpunkt“** den Wert eines Datums- und Zeitfeldes, beispielsweise **„Termin“**.
                    3. Wählen Sie für **„Vergleichsart“** die Option **„früher als oder gleich“**.
                    4. Wählen Sie für **„Vergleichszeitpunkt“** die Option **„Mit Funktion ermitteln“** und anschließend **„Aktueller Zeitpunkt“**.

                    Der Ausdruck liest sich so:

                    `Wert von „Termin“ ist früher als oder gleich (aktueller Zeitpunkt)`

                    Bei einer Auswertung am `22.11.2026 um 13:30 Uhr` ergibt ein Termin desselben Tages um `13:00 Uhr` `Wahr`, ein Termin um `14:00 Uhr` `Falsch`.

                    # Was ist zu beachten?

                    Verglichen wird der tatsächliche Zeitpunkt. Angaben aus verschiedenen Zeitzonen können denselben Zeitpunkt bezeichnen. Auch Sekundenbruchteile werden berücksichtigt.
                    """;
        };
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        var dateInput = temporalType == TemporalType.DATE ? NoCodeDateInput.Calendar : null;
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                temporalType.dataType,
                                getValueLabel(true),
                                getValueDescription(true)
                        ).withSentence("wenn").withDateInput(dateInput),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Vergleichsart",
                                "Wählen Sie die gewünschte Vergleichsart.",
                                new NoCodeParameterOption("gleich", EQUAL),
                                new NoCodeParameterOption("nicht gleich", NOT_EQUAL),
                                new NoCodeParameterOption("früher als", BEFORE),
                                new NoCodeParameterOption("früher als oder gleich", BEFORE_OR_EQUAL),
                                new NoCodeParameterOption("später als", AFTER),
                                new NoCodeParameterOption("später als oder gleich", AFTER_OR_EQUAL)
                        ).withSentence("").withOptionsAsSentencePart(),
                        new NoCodeParameter(
                                temporalType.dataType,
                                getValueLabel(false),
                                getValueDescription(false)
                        ).withSentence("", "ist").withDateInput(dateInput)
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist #1 #2";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var operation = castToString(args[1]).trim().toLowerCase(Locale.ROOT);
        if (!isSupportedOperation(operation)) {
            throw new NoCodeException(invalidComparison());
        }

        var comparison = switch (temporalType) {
            case DATE -> compareCalendarValues(
                    requireCalendarValue(args[0], invalidValue(true)),
                    requireCalendarValue(args[2], invalidValue(false))
            );
            case TIME -> requireTime(args[0], invalidValue(true))
                    .compareTo(requireTime(args[2], invalidValue(false)));
            case DATETIME -> requireDateTime(args[0], invalidValue(true)).toInstant()
                    .compareTo(requireDateTime(args[2], invalidValue(false)).toInstant());
        };

        var result = switch (operation) {
            case EQUAL -> comparison == 0;
            case NOT_EQUAL -> comparison != 0;
            case BEFORE -> comparison < 0;
            case BEFORE_OR_EQUAL -> comparison <= 0;
            case AFTER -> comparison > 0;
            case AFTER_OR_EQUAL -> comparison >= 0;
            default -> throw new NoCodeException(invalidComparison());
        };

        return new NoCodeResult(result);
    }

    private boolean isSupportedOperation(String operation) {
        return switch (operation) {
            case EQUAL, NOT_EQUAL, BEFORE, BEFORE_OR_EQUAL, AFTER, AFTER_OR_EQUAL -> true;
            default -> false;
        };
    }

    private int compareCalendarValues(
            TemporalAccessor left,
            TemporalAccessor right
    ) throws NoCodeException {
        // Cross-precision ordering is ambiguous: treating YearMonth or Year as their
        // first day would add calendar information that is absent from the source value.
        if (!left.getClass().equals(right.getClass())) {
            throw new NoCodeException(
                    "Verwenden Sie zwei vollständige Datumsangaben, zwei Angaben mit Monat und Jahr oder zwei Jahresangaben."
            );
        }

        return switch (left) {
            case LocalDate localDate -> localDate.compareTo((LocalDate) right);
            case YearMonth yearMonth -> yearMonth.compareTo((YearMonth) right);
            case Year year -> year.compareTo((Year) right);
            default -> throw new NoCodeException("Nicht unterstützter Datumswert.");
        };
    }

    @Nonnull
    private String getValueLabel(boolean first) {
        return switch (temporalType) {
            case DATE -> first ? "Datum" : "Vergleichsdatum";
            case TIME -> first ? "Uhrzeit" : "Vergleichsuhrzeit";
            case DATETIME -> first ? "Zeitpunkt" : "Vergleichszeitpunkt";
        };
    }

    @Nonnull
    private String getValueDescription(boolean first) {
        return switch (temporalType) {
            case DATE -> first
                    ? "Die Datumsangabe, die Sie prüfen möchten."
                    : "Die Datumsangabe, mit der Sie den ersten Wert vergleichen möchten.";
            case TIME -> first
                    ? "Die Uhrzeit, die Sie prüfen möchten."
                    : "Die Uhrzeit, mit der Sie den ersten Wert vergleichen möchten.";
            case DATETIME -> first
                    ? "Das Datum mit Uhrzeit, das Sie prüfen möchten."
                    : "Das Datum mit Uhrzeit, mit dem Sie den ersten Wert vergleichen möchten.";
        };
    }

    @Nonnull
    private String invalidValue(boolean first) {
        return switch (temporalType) {
            case DATE -> "Prüfen Sie die Datumsangabe für „%s“.".formatted(getValueLabel(first));
            case TIME -> "Prüfen Sie die Uhrzeit für „%s“.".formatted(getValueLabel(first));
            case DATETIME -> "Prüfen Sie das Datum und die Uhrzeit für „%s“.".formatted(getValueLabel(first));
        };
    }

    @Nonnull
    private String invalidComparison() {
        return "Die Vergleichsart ist ungültig. Wählen Sie eine der angebotenen Vergleichsarten oder prüfen Sie die verwendete Wertquelle.";
    }
}
