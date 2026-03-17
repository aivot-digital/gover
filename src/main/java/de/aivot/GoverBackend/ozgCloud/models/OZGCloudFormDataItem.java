package de.aivot.GoverBackend.ozgCloud.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public record OZGCloudFormDataItem(
        @Nonnull
        String name,
        @Nonnull
        String label,
        @Nullable
        String stringValue,
        @Nullable
        Number numberValue,
        @Nullable
        String dateValue,
        @Nullable
        Boolean booleanValue,
        @Nullable
        List<OZGCloudFormDataItem> formItems
) {
}
