package de.aivot.prosuna.backend.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.regex.Pattern;

public final class PhoneNumberUtils {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    private static final Pattern E164_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private PhoneNumberUtils() {
    }

    public enum ValidationMode {
        VALID,
        POSSIBLE,
    }

    public static boolean isValidPhoneNumber(@Nullable String value) {
        return parsePhoneNumber(value, ValidationMode.VALID) != null;
    }

    public static boolean isPossiblePhoneNumber(@Nullable String value) {
        return parsePhoneNumber(value, ValidationMode.POSSIBLE) != null;
    }

    @Nullable
    public static String normalizeValidPhoneNumberToE164(@Nullable String value) {
        return normalizePhoneNumberToE164(value, ValidationMode.VALID);
    }

    @Nullable
    public static String normalizePossiblePhoneNumberToE164(@Nullable String value) {
        return normalizePhoneNumberToE164(value, ValidationMode.POSSIBLE);
    }

    @Nullable
    private static String normalizePhoneNumberToE164(@Nullable String value, @Nonnull ValidationMode validationMode) {
        var phoneNumber = parsePhoneNumber(value, validationMode);
        if (phoneNumber == null) {
            return null;
        }

        var normalizedValue = PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        return E164_PHONE_NUMBER_PATTERN.matcher(normalizedValue).matches()
                ? normalizedValue
                : null;
    }

    @Nullable
    private static Phonenumber.PhoneNumber parsePhoneNumber(@Nullable String value, @Nonnull ValidationMode validationMode) {
        if (value == null) {
            return null;
        }

        var trimmedValue = value.trim();
        if (trimmedValue.isEmpty() || !trimmedValue.startsWith("+")) {
            return null;
        }

        try {
            var phoneNumber = PHONE_NUMBER_UTIL.parse(trimmedValue, "ZZ");
            if (phoneNumber.hasExtension()) {
                return null;
            }

            var isAccepted = switch (validationMode) {
                case VALID -> PHONE_NUMBER_UTIL.isValidNumber(phoneNumber);
                case POSSIBLE -> PHONE_NUMBER_UTIL.isPossibleNumber(phoneNumber);
            };

            return isAccepted ? phoneNumber : null;
        } catch (NumberParseException ignored) {
            return null;
        }
    }
}
