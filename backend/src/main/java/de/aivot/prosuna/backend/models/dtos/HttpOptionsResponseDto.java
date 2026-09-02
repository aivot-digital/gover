package de.aivot.prosuna.backend.models.dtos;

import jakarta.annotation.Nullable;

import java.util.List;

public record HttpOptionsResponseDto(
        String method,
        List<String> allow,
        @Nullable String allowedMethod,
        @Nullable String acceptedContentType,
        @Nullable String returnedContentType
) {
}
