package de.aivot.GoverBackend.identity.utils;

import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Make sure to keep this in sync with the frontend counterpart at app/src/modules/identity/utils/system-identity-provider-format-values.ts
 */
public class SystemIdentityProviderFormatter {
    private static final ZoneId germanZoneId = ZoneId.of("Europe/Berlin");
    private static final ZoneId utcZoneId = ZoneId.of("UTC");

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
                var format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    var date = format
                            .parse(value);
                    var zonedDateTime = ZonedDateTime
                            .ofInstant(date.toInstant(), germanZoneId);
                    return zonedDateTime
                            .withZoneSameInstant(utcZoneId)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
                } catch (ParseException e) {
                    return null;
                }
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
                var format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    var date = format
                            .parse(value);
                    var zonedDateTime = ZonedDateTime
                            .ofInstant(date.toInstant(), germanZoneId);
                    return zonedDateTime
                            .withZoneSameInstant(utcZoneId)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
                } catch (ParseException e) {
                    return null;
                }
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
                var format = new SimpleDateFormat("dd.MM.yyyy");
                try {
                    var date = format
                            .parse(value);
                    var zonedDateTime = ZonedDateTime
                            .ofInstant(date.toInstant(), germanZoneId);
                    return zonedDateTime
                            .withZoneSameInstant(utcZoneId)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
                } catch (ParseException e) {
                    return null;
                }
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
                var format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    var date = format
                            .parse(value);
                    var zonedDateTime = ZonedDateTime
                            .ofInstant(date.toInstant(), germanZoneId);
                    return zonedDateTime
                            .withZoneSameInstant(utcZoneId)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
                } catch (ParseException e) {
                    return null;
                }
            default:
                return value;
        }
    }
}