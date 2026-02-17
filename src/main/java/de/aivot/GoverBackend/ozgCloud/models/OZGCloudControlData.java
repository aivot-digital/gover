package de.aivot.GoverBackend.ozgCloud.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public record OZGCloudControlData(
        @Nonnull
        String transactionId,
        @Nonnull
        String zustaendigeStelle,
        @Nonnull
        List<String> leikaIds,
        @Nonnull
        String formId,
        @Nonnull
        String name,
        @Nullable
        OZGCloudServiceKontoData serviceKonto
) {
}
