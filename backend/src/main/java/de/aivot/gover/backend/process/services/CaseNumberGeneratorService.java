package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates process case numbers from a process-local template contract.
 *
 * <p>The process module keeps this logic in its own service on purpose. Process case numbers are part of the
 * workflow domain and carry their own placeholder semantics, persistence query, and uniqueness guarantees. Reusing
 * another module's identifier generator would couple two separate concepts that may evolve independently.</p>
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *     <li>{@code %YYY} - four-digit year</li>
 *     <li>{@code %Y} - two-digit year</li>
 *     <li>{@code %M} - two-digit month</li>
 *     <li>{@code %D} - two-digit day of month</li>
 *     <li>{@code %h} - two-digit hour</li>
 *     <li>{@code %m} - two-digit minute</li>
 *     <li>{@code %I(n)} - increment with zero padding of {@code n} digits</li>
 * </ul>
 */
@Service
public class CaseNumberGeneratorService {
    private static final int MAX_CASE_NUMBER_LENGTH = 36;
    private static final int CASE_NUMBER_PADDING_MIN = 4;
    private static final int CASE_NUMBER_PADDING_MAX = 12;

    private static final String CASE_NUMBER_YEAR_PLACEHOLDER_FULL = "%YYY";
    private static final String CASE_NUMBER_YEAR_PLACEHOLDER_SHORT = "%Y";
    private static final String CASE_NUMBER_MONTH_PLACEHOLDER = "%M";
    private static final String CASE_NUMBER_DAY_PLACEHOLDER = "%D";
    private static final String CASE_NUMBER_HOUR_PLACEHOLDER = "%h";
    private static final String CASE_NUMBER_MIN_PLACEHOLDER = "%m";
    private static final Pattern CASE_NUMBER_INCREMENT_PATTERN = Pattern.compile("%I\\((\\d{1,2})\\)");
    private static final String SUPPORTED_PLACEHOLDERS = "%YYY, %Y, %M, %D, %h, %m und %I(n)";

    private final ProcessInstanceRepository processInstanceRepository;

    public CaseNumberGeneratorService(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    /**
     * Validates the template before it is stored on a process version.
     *
     * <p>Validation happens up front so invalid templates fail at configuration time instead of later during instance
     * creation, where the resulting error would be harder to recover from.</p>
     */
    public void validateCaseNumberTemplate(@Nullable String caseNumberTemplate) throws ResponseException {
        if (caseNumberTemplate == null) {
            return;
        }

        parseTemplate(caseNumberTemplate);
    }

    /**
     * Generates the next case number for the provided template.
     *
     * <p>The database remains the final uniqueness guard, but the generator still reads the current maximum increment
     * for the rendered prefix/suffix pair so numbering stays monotonic for the active time bucket.</p>
     */
    @Nonnull
    public String generateCaseNumber(@Nullable String caseNumberTemplate) throws ResponseException {
        if (caseNumberTemplate == null) {
            return UUID.randomUUID().toString();
        }

        return generateCaseNumber(caseNumberTemplate, ZonedDateTime.now(ApplicationTimeZone.getZoneId()));
    }

    @Nonnull
    String generateCaseNumber(@Nonnull String caseNumberTemplate, @Nonnull ZonedDateTime now) throws ResponseException {
        var parsedTemplate = parseTemplate(caseNumberTemplate);
        var prefix = renderTemplateSegment(parsedTemplate.prefixTemplate(), now);
        var suffix = renderTemplateSegment(parsedTemplate.suffixTemplate(), now);

        var prefixLength = prefix.codePointCount(0, prefix.length());
        var suffixLength = suffix.codePointCount(0, suffix.length());
        var currentMaxIncrement = processInstanceRepository.getMaxCaseNumberIncrement(
                prefix,
                suffix,
                prefixLength,
                suffixLength,
                prefixLength + 1,
                parsedTemplate.padding(),
                prefixLength + parsedTemplate.padding() + suffixLength
        );

        var nextIncrement = currentMaxIncrement == null ? 1 : currentMaxIncrement + 1;
        var incrementValue = String.format(Locale.ROOT, "%0" + parsedTemplate.padding() + "d", nextIncrement);
        var caseNumber = prefix + incrementValue + suffix;

        if (caseNumber.codePointCount(0, caseNumber.length()) > MAX_CASE_NUMBER_LENGTH) {
            throw ResponseException.internalServerError(
                    "Der erzeugte Vorgangsschlüssel überschreitet unerwartet das zulässige Limit von %d Zeichen.",
                    MAX_CASE_NUMBER_LENGTH
            );
        }

        return caseNumber;
    }

    @Nonnull
    private ParsedCaseNumberTemplate parseTemplate(@Nonnull String caseNumberTemplate) throws ResponseException {
        if (caseNumberTemplate.isBlank()) {
            throw ResponseException.badRequest("Die Vorgangsschlüssel-Formatvorlage darf nicht leer sein.");
        }

        var incrementMatcher = CASE_NUMBER_INCREMENT_PATTERN.matcher(caseNumberTemplate);
        int incrementMatchCount = 0;
        int incrementStart = -1;
        int incrementEnd = -1;
        int incrementPadding = -1;
        int renderedLength = 0;

        for (int index = 0; index < caseNumberTemplate.length(); ) {
            if (caseNumberTemplate.startsWith(CASE_NUMBER_YEAR_PLACEHOLDER_FULL, index)) {
                renderedLength += 4;
                index += CASE_NUMBER_YEAR_PLACEHOLDER_FULL.length();
                continue;
            }
            if (caseNumberTemplate.startsWith(CASE_NUMBER_YEAR_PLACEHOLDER_SHORT, index)) {
                renderedLength += 2;
                index += CASE_NUMBER_YEAR_PLACEHOLDER_SHORT.length();
                continue;
            }
            if (caseNumberTemplate.startsWith(CASE_NUMBER_MONTH_PLACEHOLDER, index)
                    || caseNumberTemplate.startsWith(CASE_NUMBER_DAY_PLACEHOLDER, index)
                    || caseNumberTemplate.startsWith(CASE_NUMBER_HOUR_PLACEHOLDER, index)
                    || caseNumberTemplate.startsWith(CASE_NUMBER_MIN_PLACEHOLDER, index)) {
                renderedLength += 2;
                index += 2;
                continue;
            }

            incrementMatcher.region(index, caseNumberTemplate.length());
            if (incrementMatcher.lookingAt()) {
                incrementMatchCount++;
                incrementStart = incrementMatcher.start();
                incrementEnd = incrementMatcher.end();
                incrementPadding = parsePadding(incrementMatcher);
                renderedLength += incrementPadding;
                index = incrementMatcher.end();
                continue;
            }

            int codePoint = caseNumberTemplate.codePointAt(index);
            if (codePoint == '%') {
                throw ResponseException.badRequest(
                        "Die Vorgangsschlüssel-Formatvorlage enthält einen unbekannten Platzhalter an Position %d. Unterstützt werden %s.",
                        index + 1,
                        SUPPORTED_PLACEHOLDERS
                );
            }

            renderedLength++;
            index += Character.charCount(codePoint);
        }

        if (incrementMatchCount != 1) {
            throw ResponseException.badRequest(
                    "Die Vorgangsschlüssel-Formatvorlage muss genau einen Inkrement-Platzhalter im Format %I(n) enthalten."
            );
        }

        if (renderedLength > MAX_CASE_NUMBER_LENGTH) {
            throw ResponseException.badRequest(
                    "Der erzeugte Vorgangsschlüssel würde das Limit von %d Zeichen überschreiten.",
                    MAX_CASE_NUMBER_LENGTH
            );
        }

        return new ParsedCaseNumberTemplate(
                caseNumberTemplate,
                incrementStart,
                incrementEnd,
                incrementPadding
        );
    }

    private int parsePadding(@Nonnull Matcher incrementMatcher) throws ResponseException {
        var padding = Integer.parseInt(incrementMatcher.group(1));
        if (padding < CASE_NUMBER_PADDING_MIN || padding > CASE_NUMBER_PADDING_MAX) {
            throw ResponseException.badRequest(
                    "Die Inkrement-Breite in der Vorgangsschlüssel-Formatvorlage muss zwischen %d und %d Stellen liegen.",
                    CASE_NUMBER_PADDING_MIN,
                    CASE_NUMBER_PADDING_MAX
            );
        }

        return padding;
    }

    @Nonnull
    private String renderTemplateSegment(@Nonnull String templateSegment, @Nonnull ZonedDateTime now) {
        return templateSegment
                .replace(CASE_NUMBER_YEAR_PLACEHOLDER_FULL, String.format(Locale.ROOT, "%04d", now.getYear()))
                .replace(CASE_NUMBER_YEAR_PLACEHOLDER_SHORT, String.format(Locale.ROOT, "%02d", now.getYear() % 100))
                .replace(CASE_NUMBER_MONTH_PLACEHOLDER, String.format(Locale.ROOT, "%02d", now.getMonthValue()))
                .replace(CASE_NUMBER_DAY_PLACEHOLDER, String.format(Locale.ROOT, "%02d", now.getDayOfMonth()))
                .replace(CASE_NUMBER_HOUR_PLACEHOLDER, String.format(Locale.ROOT, "%02d", now.getHour()))
                .replace(CASE_NUMBER_MIN_PLACEHOLDER, String.format(Locale.ROOT, "%02d", now.getMinute()));
    }

    /**
     * Stores the increment position inside the original template so generation can render the static parts around it.
     * Keeping the raw bounds avoids reparsing or making assumptions about where the increment placeholder lives.
     */
    private record ParsedCaseNumberTemplate(
            @Nonnull String template,
            int incrementStart,
            int incrementEnd,
            int padding
    ) {
        @Nonnull
        private String prefixTemplate() {
            return template.substring(0, incrementStart);
        }

        @Nonnull
        private String suffixTemplate() {
            return template.substring(incrementEnd);
        }
    }
}
