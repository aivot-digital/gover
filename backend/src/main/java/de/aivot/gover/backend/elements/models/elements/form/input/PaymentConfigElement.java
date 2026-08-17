package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

public class PaymentConfigElement extends BaseInputElement<PaymentConfigElementValue> {
    public PaymentConfigElement() {
        super(ElementType.PaymentConfig);
    }

    @Override
    public void performValidation(@Nullable PaymentConfigElementValue value) throws ValidationException {
        if (value == null) {
            if (getRequired()) {
                throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
            }
            return;
        }

        if (value.paymentProviderKey() == null) {
            throw new ValidationException(this, "Es muss ein Zahlungsanbieter ausgewählt werden.");
        }

        if (StringUtils.isNullOrEmpty(value.purpose())) {
            throw new ValidationException(this, "Es muss ein Buchungstext angegeben werden.");
        }

        if (StringUtils.isNullOrEmpty(value.description())) {
            throw new ValidationException(this, "Es muss eine Beschreibung angegeben werden.");
        }

        if (Boolean.TRUE.equals(value.mapRequestor())) {
            if (value.requestorMapping() == null) {
                throw new ValidationException(this, "Es muss eine Zuweisung des Antragstellers angegeben werden.");
            }

            value.requestorMapping().performValidation();
        }

        if (value.items() == null || value.items().isEmpty()) {
            throw new ValidationException(this, "Es muss mindestens ein Zahlungsposten angegeben werden.");
        }

        for (PaymentConfigElementValueItem item : value.items()) {
            item.performValidation();
        }
    }

    @Nullable
    @Override
    public PaymentConfigElementValue formatValue(@Nullable Object value) {
        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, PaymentConfigElementValue.class);
    }
}
