package de.aivot.prosuna.backend.communication.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.util.regex.Pattern;

public final class EmailAddressUtils {
    public static final String EMAIL_PATTERN_VALUE = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_PATTERN_VALUE);

    private EmailAddressUtils() {
    }

    @Nonnull
    public static String normalizeSingleAddress(@Nullable String rawAddress) {
        if (rawAddress == null) {
            throw new IllegalArgumentException("Die E-Mail-Adresse fehlt.");
        }

        var address = rawAddress.trim();
        if (address.isEmpty() || address.length() > 254 || !EMAIL_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        }

        try {
            var parsed = InternetAddress.parse(address, true);
            if (parsed.length != 1 || !address.equals(parsed[0].getAddress())) {
                throw new IllegalArgumentException("Es muss genau eine E-Mail-Adresse angegeben werden.");
            }
            parsed[0].validate();
        } catch (AddressException e) {
            throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.", e);
        }

        return address;
    }

    public static boolean isValidSingleAddress(@Nullable String rawAddress) {
        try {
            normalizeSingleAddress(rawAddress);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
