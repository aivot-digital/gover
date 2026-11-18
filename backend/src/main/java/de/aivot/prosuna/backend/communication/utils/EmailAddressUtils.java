package de.aivot.prosuna.backend.communication.utils;

import com.ibm.icu.text.IDNA;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.util.regex.Pattern;

public final class EmailAddressUtils {
    public static final String EMAIL_PATTERN_VALUE = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_PATTERN_VALUE);

    public enum DomainPolicy {
        INTERNET,
        ALLOW_LOCAL,
    }

    // java.net.IDN uses IDNA2003, which maps ß to ss and can therefore change the recipient's domain.
    // ICU's nontransitional UTS #46 processing keeps domains such as faß.de and fass.de distinct.
    private static final IDNA IDNA_PROCESSOR = IDNA.getUTS46Instance(
            IDNA.USE_STD3_RULES | IDNA.CHECK_BIDI | IDNA.CHECK_CONTEXTJ | IDNA.NONTRANSITIONAL_TO_ASCII);
    private static final Pattern LOCAL_PART = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*");
    private static final Pattern DOMAIN_CHARACTERS = Pattern.compile("[\\p{L}\\p{M}\\p{N}.\\-]+");
    // Match browser trimming, including non-breaking spaces copied with an address.
    private static final Pattern ADDRESS_PADDING = Pattern.compile("^[\\p{Z}\\uFEFF]+|[\\p{Z}\\uFEFF]+$");

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

    /** Validates the contact-field profile, independently of whether the mail server can deliver it. */
    @Nonnull
    public static String normalizeSingleAddress(@Nullable String rawAddress, @Nonnull DomainPolicy policy) {
        if (rawAddress == null) throw new IllegalArgumentException("Die E-Mail-Adresse fehlt.");
        if (rawAddress.chars().anyMatch(c -> c < 32 || c == 127)) throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        var address = ADDRESS_PADDING.matcher(rawAddress).replaceAll("");
        var at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@') || at > 64 || !LOCAL_PART.matcher(address.substring(0, at)).matches()) {
            throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        }
        var domain = address.substring(at + 1);
        if (!DOMAIN_CHARACTERS.matcher(domain).matches()) throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        var info = new IDNA.Info();
        var asciiDomain = IDNA_PROCESSOR.nameToASCII(domain, new StringBuilder(), info).toString();
        if (info.hasErrors() || asciiDomain.endsWith(".") || (policy == DomainPolicy.INTERNET && !asciiDomain.contains("."))) {
            throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        }
        var normalized = address.substring(0, at) + "@" + asciiDomain;
        if (normalized.length() > 254) throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        return normalized;
    }

    public static boolean isValidSingleAddress(@Nullable String rawAddress, @Nonnull DomainPolicy policy) {
        try {
            normalizeSingleAddress(rawAddress, policy);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /** Keeps previously accepted Internet addresses compatible while adding local and IDN recipients. */
    @Nonnull
    public static String normalizeTransportAddress(@Nullable String rawAddress) {
        if (rawAddress != null && rawAddress.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IllegalArgumentException("Die E-Mail-Adresse ist ungültig.");
        }
        try {
            return normalizeSingleAddress(rawAddress, DomainPolicy.ALLOW_LOCAL);
        } catch (IllegalArgumentException profileException) {
            // Existing communication callers accepted quoted ASCII local parts, for example.
            var legacy = normalizeSingleAddress(rawAddress);
            if (legacy.chars().anyMatch(c -> c < 32 || c > 126)) throw profileException;
            return legacy;
        }
    }

    public static boolean isValidTransportAddress(@Nullable String rawAddress) {
        try {
            normalizeTransportAddress(rawAddress);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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
