package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

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

        var errorDetails = new LinkedHashMap<String, Object>();

        if (value.paymentProviderKey() == null) {
            errorDetails.put("paymentProviderKey", "Es muss ein Zahlungsanbieter ausgewählt werden.");
        }

        if (StringUtils.isNullOrEmpty(value.purpose())) {
            errorDetails.put("purpose", "Geben Sie einen Verwendungszweck an.");
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
            // Keep field paths for the editor and add readable summaries for field and node error lists.
            errorDetails.put("messages", describeErrors(errorDetails, ""));
            throw new ValidationException(this, "Konfiguration fehlerhaft.", errorDetails);
        }
    }

    @Nonnull
    private static List<String> describeErrors(@Nonnull Map<?, ?> details, @Nonnull String prefix) {
        var messages = new ArrayList<String>();
        details.forEach((key, detail) -> {
            if (detail instanceof String message) {
                messages.add(prefix + message);
            } else if (detail instanceof Map<?, ?> nested) {
                messages.addAll(describeErrors(nested, prefix));
            } else if (detail instanceof List<?> items) {
                for (int index = 0; index < items.size(); index++) {
                    if (items.get(index) instanceof Map<?, ?> item) {
                        messages.addAll(describeErrors(item, "Zahlungsposition " + (index + 1) + ": "));
                    }
                }
            }
        });
        return messages;
    }

    @Nullable
    @Override
    public PaymentConfigElementValue formatValue(@Nullable Object value) {
        return JsonMapperFactory
                .getInstance()
                .convertValue(value, PaymentConfigElementValue.class);
    }
}
