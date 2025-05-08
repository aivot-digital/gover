package de.aivot.GoverBackend.captcha.dtos;

/** request body for POST /api/captcha/verify */
public record CaptchaVerificationRequestDTO(
        String payload,
        Long expiresAt
) {}