package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PaymentConfigElement extends BaseInputElement<PaymentConfigElementValue> {
    public PaymentConfigElement() {
        super(ElementType.PaymentConfig);
    }

    @Override
    public void performValidation(@Nullable PaymentConfigElementValue value) throws ValidationException {
        if (value == null) {
            if (getRequired()) {
                throw new ValidationException(this, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.", Map.of());
            }
            return;
        }

        var errorDetails = new HashMap<String, Object>();

        if (value.paymentProviderKey() == null) {
            errorDetails.put("paymentProviderKey", "Es muss ein Zahlungsanbieter ausgewählt werden.");
        }

        if (StringUtils.isNullOrEmpty(value.purpose())) {
            errorDetails.put("purpose", "Es muss ein Buchungszweck angegeben werden.");
        }

        if (StringUtils.isNullOrEmpty(value.description())) {
            errorDetails.put("description", "Es muss eine Beschreibung angegeben werden.");
        }

        if (Boolean.TRUE.equals(value.mapRequestor())) {
            if (value.requestorMapping() == null) {
                errorDetails.put("requestorMapping", "Es muss eine Zuweisung des Antragstellers angegeben werden.");
            } else {
                var err = value.requestorMapping().performValidation();
                if (!err.isEmpty()) {
                    errorDetails.put("requestorMapping", err);
                }
            }
        }

        if (value.items() == null || value.items().isEmpty()) {
            errorDetails.put("items", "Es muss mindestens ein Zahlungsposten angegeben werden.");
        } else {
            var itemErrors = new ArrayList<Object>();
            var hasItemErrors = false;
            for (PaymentConfigElementValueItem item : value.items()) {
                var err = item.performValidation();
                itemErrors.add(err.isEmpty() ? null : err);
                hasItemErrors = hasItemErrors || !err.isEmpty();
            }
            if (hasItemErrors) {
                errorDetails.put("items", itemErrors);
            }
        }

        if (!errorDetails.isEmpty()) {
            throw new ValidationException(this, "Konfiguration fehlerhaft.", errorDetails);
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
