package de.aivot.prosuna.backend.services.pdf;

import jakarta.annotation.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;

public class NumberFormatDialect extends AbstractDialect implements IExpressionObjectDialect {
    public NumberFormatDialect() {
        super("numberformat");
    }

    public String format(@Nullable Object value, @Nullable Integer decimalPlaces) {
        if (value == null) {
            return "";
        }

        if (value instanceof Number nValue) {
            Locale locale = Locale.GERMAN;
            NumberFormat formatter = NumberFormat.getNumberInstance(locale);
            formatter.setMinimumFractionDigits(decimalPlaces == null ? 0 : decimalPlaces);
            formatter.setMaximumFractionDigits(decimalPlaces == null ? 0 : decimalPlaces);
            return formatter.format(nValue);
        } else if (value instanceof String s) {
            return s;
        } else {
            return "";
        }
    }

    public String formatISOTimestamp(String timestamp, String format) {
        return formatInstant(timestamp, format);
    }

    public String formatInstant(@Nullable Object value, String format) {
        if (value == null) {
            return "";
        }

        try {
            var displayZone = ApplicationTimeZone.getZoneId();
            var instant = switch (value) {
                case Instant instantValue -> instantValue;
                case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
                case ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
                case String stringValue -> IsoTimestampUtils.parseIsoInstant(stringValue);
                default -> throw new DateTimeException("Unsupported instant value: " + value.getClass().getName());
            };
            return instant
                    .atZone(displayZone)
                    .format(DateTimeFormatter.ofPattern(format));
        } catch (DateTimeException ex) {
            return value.toString();
        }
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {

            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Collections.singleton("numberformat");
            }

            @Override
            public Object buildObject(IExpressionContext context,
                                      String expressionObjectName) {
                return new NumberFormatDialect();
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}
