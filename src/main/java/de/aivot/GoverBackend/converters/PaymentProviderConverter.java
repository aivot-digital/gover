package de.aivot.GoverBackend.converters;

import de.aivot.GoverBackend.enums.PaymentProvider;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class PaymentProviderConverter implements AttributeConverter<PaymentProvider, String> {
    @Override
    public String convertToDatabaseColumn(PaymentProvider status) {
        if (status == null) {
            return null;
        }
        return status.getKey();
    }

    @Override
    public PaymentProvider convertToEntityAttribute(String status) {
        if (status == null) {
            return null;
        }

        return Stream.of(PaymentProvider.values())
                .filter(c -> c.matches(status))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
