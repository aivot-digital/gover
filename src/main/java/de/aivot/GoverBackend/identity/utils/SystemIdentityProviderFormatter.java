package de.aivot.GoverBackend.identity.utils;

import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Make sure to keep this in sync with the frontend counterpart at app/src/modules/identity/utils/system-identity-provider-format-values.ts
 */
public class SystemIdentityProviderFormatter {
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    public static String formatForSystemIdentityProvider(
            @Nonnull String metadataIdentifier,
            @Nonnull String keyInData,
            @Nullable String value
    ) {
        if (value == null) {
            return null;
        }

        if (IdentityProviderType.BayernId.getDefaultMetadataIdentifier().equals(metadataIdentifier)) {
            return formatForBayernId(keyInData, value);
        }

        if (IdentityProviderType.BundId.getDefaultMetadataIdentifier().equals(metadataIdentifier)) {
            return formatForBundId(keyInData, value);
        }

        if (IdentityProviderType.MUK.getDefaultMetadataIdentifier().equals(metadataIdentifier)) {
            return formatForMUK(keyInData, value);
        }

        if (IdentityProviderType.ShId.getDefaultMetadataIdentifier().equals(metadataIdentifier)) {
            return formatForShId(keyInData, value);
        }

        return value;
    }

    private static String formatForBayernId(
            @Nonnull String keyInData,
            @Nonnull String value
    ) {
        switch (keyInData) {
            case "date_of_birth":
                return formatDateOfBirth(value, "yyyy-MM-dd");
            default:
                return value;
        }
    }

    private static String formatForBundId(
            @Nonnull String keyInData,
            @Nonnull String value
    ) {
        switch (keyInData) {
            case "date_of_birth":
                return formatDateOfBirth(value, "yyyy-MM-dd");
            default:
                return value;
        }
    }

    private static String formatForMUK(
            @Nonnull String keyInData,
            @Nonnull String value
    ) {
        switch (keyInData) {
            case "date_of_birth":
                return formatDateOfBirth(value, "dd.MM.yyyy");
            default:
                return value;
        }
    }

    private static String formatForShId(
            @Nonnull String keyInData,
            @Nonnull String value
    ) {
        switch (keyInData) {
            case "date_of_birth":
                return formatDateOfBirth(value, "yyyy-MM-dd");
            default:
                return value;
        }
    }

    @Nullable
    private static String formatDateOfBirth(@Nonnull String value, @Nonnull String pattern) {
        var format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone(ApplicationTimeZone.getZoneId()));

        try {
            var date = format.parse(value);
            if (date == null) {
                return null;
            }

            return ZonedDateTime
                    .ofInstant(date.toInstant(), ApplicationTimeZone.getZoneId())
                    .withZoneSameInstant(UTC_TIME_ZONE.toZoneId())
                    .format(DateTimeFormatter.ISO_DATE_TIME);
        } catch (ParseException e) {
            return null;
        }
    }
}
