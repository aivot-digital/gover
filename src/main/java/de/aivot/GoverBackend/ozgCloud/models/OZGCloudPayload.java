package de.aivot.GoverBackend.ozgCloud.models;

import jakarta.annotation.Nonnull;

import java.util.List;

public record OZGCloudPayload(
        @Nonnull
        OZGCloudControlData control,
        @Nonnull
        List<OZGCloudFormDataItem> formData
) {
}
