package de.aivot.GoverBackend.models.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

public class ApiErrorDto {
    @Nonnull
    private Integer status;
    @Nonnull
    private HttpStatus httpStatus;
    @Nonnull
    private String message;
    @Nullable
    private Object details;
    @Nonnull
    private Boolean displayableToUser;

    public ApiErrorDto(@Nonnull HttpStatus status,
                       @Nonnull String message,
                       @Nullable Object details,
                       @Nonnull Boolean displayableToUser) {
        this.status = status.value();
        this.httpStatus = status;
        this.message = message;
        this.details = details;
        this.displayableToUser = displayableToUser;
    }

    @Nonnull
    public Integer getStatus() {
        return status;
    }

    public ApiErrorDto setStatus(@Nonnull Integer status) {
        this.status = status;
        return this;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public ApiErrorDto setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }

    @Nullable
    public Object getDetails() {
        return details;
    }

    public ApiErrorDto setDetails(@Nullable Object details) {
        this.details = details;
        return this;
    }

    @Nonnull
    public Boolean getDisplayableToUser() {
        return displayableToUser;
    }

    public ApiErrorDto setDisplayableToUser(@Nonnull Boolean displayableToUser) {
        this.displayableToUser = displayableToUser;
        return this;
    }

    @Nonnull
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ApiErrorDto setHttpStatus(@Nonnull HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }
}
