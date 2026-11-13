package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class PaymentConfigElement extends BaseInputElement<PaymentConfigElementValue> {
    private static final Pattern DISALLOWED_PURPOSE_CHARACTER = Pattern.compile(XBezahldienstePaymentRequest.DISALLOWED_PURPOSE_CHARACTERS);
    private static final Pattern DISALLOWED_DESCRIPTION_CHARACTER = Pattern.compile(XBezahldienstePaymentRequest.DISALLOWED_DESCRIPTION_CHARACTERS);

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

        // Overlong texts stay valid: inserted values can make them shorter, and the provider request truncates them.
        if (StringUtils.isNullOrEmpty(value.purpose())) {
            errorDetails.put("purpose", "Geben Sie einen Verwendungszweck an.");
        } else {
            var disallowed = describeDisallowedCharacters(value.purpose(), DISALLOWED_PURPOSE_CHARACTER);
            if (disallowed != null) {
                errorDetails.put("purpose", "Der Buchungstext enthält Zeichen, die nicht übertragen werden: %s. Erlaubt sind Buchstaben ohne Umlaute, ß und Akzente, Ziffern, Leerzeichen, Binde- und Unterstriche.".formatted(disallowed));
            }
        }

        if (StringUtils.isNullOrEmpty(value.description())) {
            errorDetails.put("description", "Es muss eine Beschreibung angegeben werden.");
        } else {
            var disallowed = describeDisallowedCharacters(value.description(), DISALLOWED_DESCRIPTION_CHARACTER);
            if (disallowed != null) {
                errorDetails.put("description", "Die Beschreibung enthält Zeichen, die nicht übertragen werden: %s. Erlaubt sind Buchstaben, auch mit Umlaut oder Akzent, ß, Ziffern, Leerzeichen, Binde- und Unterstriche, Kommas und Punkte.".formatted(disallowed));
            }
        }

        if (Boolean.TRUE.equals(value.mapRequestor())) {
            if (value.requestorMapping() == null) {
                errorDetails.put("requestorMapping", "Es muss eine Zuordnung der zahlenden Person oder Organisation angegeben werden.");
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

    /**
     * Lists the typed characters that the provider request would remove. Values of template tags are only known at
     * runtime, so they are still cleaned when sending.
     */
    @Nullable
    private static String describeDisallowedCharacters(@Nonnull String template, @Nonnull Pattern disallowedCharacter) {
        var matcher = disallowedCharacter.matcher(TemplateRenderService.extractLiteralText(template));
        var characters = new LinkedHashSet<String>();
        while (matcher.find()) {
            var codePoint = matcher.group().codePointAt(0);
            // Name invisible characters such as a non-breaking space by code point, so authors can find them.
            var invisible = Character.isSpaceChar(codePoint) || Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.FORMAT;
            characters.add(invisible ? "U+%04X".formatted(codePoint) : StringUtils.quote(matcher.group()));
        }
        return characters.isEmpty() ? null : String.join(", ", characters);
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
