package de.aivot.GoverBackend.xdf.v2.utils;

import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.xdf.v2.models.XdfIdentifikation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class XdfStringUtils {
    @Nullable
    public static String cleanString(@Nullable String str) {
        if (StringUtils.isNullOrEmpty(str)) {
            return null;
        }
        return str
                .replace("\n", "")
                .replace("\r", "")
                .trim()
                .replaceAll(" +", " ");
    }

    @Nonnull
    public static String idfToName(@Nullable XdfIdentifikation identifikation) {
        if (identifikation == null) {
            return "feld";
        }

        var id = identifikation.getId();
        if (StringUtils.isNullOrEmpty(id)) {
            return "feld";
        }

        var res = XdfStringUtils.cleanString(
                id
                        .trim()
                        .toLowerCase()
        );

        if (res == null) {
            return "feld";
        }

        return res;
    }
}
