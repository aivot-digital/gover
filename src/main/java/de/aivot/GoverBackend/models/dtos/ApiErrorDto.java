package de.aivot.GoverBackend.models.dtos;

import org.springframework.http.HttpStatus;

public class ApiErrorDto {
    private HttpStatus status;
    private String message;
    private Object details;

    public ApiErrorDto(HttpStatus status, String message, Object details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
