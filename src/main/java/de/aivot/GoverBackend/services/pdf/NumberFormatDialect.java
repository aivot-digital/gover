package de.aivot.GoverBackend.services.pdf;

import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class NumberFormatDialect extends AbstractDialect implements IExpressionObjectDialect {

    public NumberFormatDialect() {
        super("numberformat");
    }

    public String format(Object value, int decimalPlaces) {
        Locale locale = Locale.GERMAN;
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(decimalPlaces);
        formatter.setMaximumFractionDigits(decimalPlaces);
        return formatter.format(value);
    }

    public String formatISOTimestamp(String timestamp, String format) {
        var dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        return dateTime.format(DateTimeFormatter.ofPattern(format));
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
