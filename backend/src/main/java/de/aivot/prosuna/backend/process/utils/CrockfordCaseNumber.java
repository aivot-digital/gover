package de.aivot.prosuna.backend.process.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Human-readable case numbers using <a href="https://www.crockford.com/base32.html">Crockford's Base32</a>.
 * Its 32-character alphabet encodes five bits per character and omits I, L, O and U. Twelve random characters
 * provide 60 random bits while remaining practical to read aloud, especially when grouped in blocks of four.
 * Search accepts either letter case and treats O as 0 and I/L as 1 to tolerate common transcription mistakes.
 * The database constraint, rather than randomness alone, guarantees uniqueness.
 */
public final class CrockfordCaseNumber {
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern INPUT = Pattern.compile("[" + ALPHABET + "]{4,12}");

    private CrockfordCaseNumber() {
    }

    @Nonnull
    public static String generate() {
        var value = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return format(value.toString());
    }

    @Nullable
    public static String normalizeSearch(@Nonnull String input) {
        var normalized = input.toUpperCase(Locale.ROOT).replaceAll("[\\s\\p{Zs}-]", "")
                .replace('O', '0').replace('I', '1').replace('L', '1');
        return INPUT.matcher(normalized).matches() ? normalized : null;
    }

    @Nonnull
    public static String format(@Nonnull String value) {
        if (value.length() != LENGTH) throw new IllegalArgumentException("Expected twelve characters");
        return value.substring(0, 4) + "-" + value.substring(4, 8) + "-" + value.substring(8);
    }
}
