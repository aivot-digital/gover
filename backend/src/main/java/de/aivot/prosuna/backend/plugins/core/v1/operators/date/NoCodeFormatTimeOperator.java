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
        return "Formatiert eine lokale Uhrzeit.";
    }

    @Override
    public String getDescription() {
        return "Formatiert eine lokale Uhrzeit wahlweise mit oder ohne Sekunden.";
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
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Format",
                                "Das Ausgabeformat.",
                                new NoCodeParameterOption("Stunde und Minute", MINUTE_PATTERN),
                                new NoCodeParameterOption("Mit Sekunden", SECOND_PATTERN)
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "formatiere Uhrzeit „#0“ als „#1“";
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
