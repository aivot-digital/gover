package de.aivot.GoverBackend.lib.exceptions;

import org.springframework.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResponseException extends Exception {
    @Nonnull
    private final HttpStatus status;
    @Nonnull
    private final String title;
    @Nullable
    private final String details;

    // region Constructors

    public ResponseException() {
        super();
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.title = "Unbekannter Fehler";
        this.details = "Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.";
    }

    public ResponseException(
            @Nonnull HttpStatus status,
            @Nonnull String title,
            @Nullable String details,
            @Nonnull Throwable cause
    ) {
        super(title, cause);
        this.status = status;
        this.title = title;
        this.details = details;
    }

    public ResponseException(
            @Nonnull HttpStatus status,
            @Nonnull String title,
            @Nullable String details
    ) {
        super(title);
        this.status = status;
        this.title = title;
        this.details = details;
    }

    public ResponseException(
            @Nonnull HttpStatus status,
            @Nonnull String title,
            @Nonnull Throwable cause
    ) {
        super(title, cause);
        this.status = status;
        this.title = title;
        this.details = null;
    }

    public ResponseException(
            @Nonnull HttpStatus status,
            @Nonnull String title
    ) {
        super(title);
        this.status = status;
        this.title = title;
        this.details = null;
    }

    // endregion

    // region Builders

    public static ResponseException badRequest() {
        return ResponseException.badRequest("Die Anfrage ist fehlerhaft.");
    }

    public static ResponseException badRequest(String message) {
        return new ResponseException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseException badRequest(String message, String details) {
        return new ResponseException(HttpStatus.BAD_REQUEST, message, details);
    }

    public static ResponseException unauthorized() {
        return ResponseException.unauthorized("Sie sind nicht angemeldet. Bitte melden Sie sich an.");
    }

    public static ResponseException unauthorized(String message) {
        return new ResponseException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ResponseException forbidden() {
        return ResponseException.forbidden("Sie haben keine Berechtigung für diese Aktion.");
    }

    public static ResponseException forbidden(String message) {
        return new ResponseException(HttpStatus.FORBIDDEN, message);
    }

    public static ResponseException notFound() {
        return ResponseException.notFound("Die angeforderte Ressource wurde nicht gefunden.");
    }

    public static ResponseException notFound(String message, Object ... args) {
        return new ResponseException(HttpStatus.NOT_FOUND, String.format(message, args));
    }

    public static ResponseException notFound(String message) {
        return new ResponseException(HttpStatus.NOT_FOUND, message);
    }

    public static ResponseException conflict(String message, Object ... args) {
        return ResponseException.conflict(String.format(message, args));
    }

    public static ResponseException conflict(String message) {
        return new ResponseException(HttpStatus.CONFLICT, message);
    }

    public static ResponseException locked() {
        return ResponseException.locked("Die angeforderte Ressource wird gerade bearbeitet. Bitte versuchen Sie es später erneut.");
    }

    public static ResponseException locked(String message) {
        return new ResponseException(HttpStatus.LOCKED, message);
    }

    public static ResponseException internalServerError() {
        return ResponseException.locked("Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.");
    }

    public static ResponseException internalServerError(String message) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ResponseException internalServerError(String message, String details) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, message, details);
    }

    public static ResponseException internalServerError(String message, Object ... args) {
        return ResponseException.internalServerError(String.format(message, args));
    }

    public static ResponseException internalServerError(Throwable cause, String message, Object ... args) {
        return ResponseException.internalServerError(String.format(message, args), cause);
    }

    public static ResponseException internalServerError(String message, Throwable cause) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public static ResponseException internalServerError(Throwable cause) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.", cause);
    }

    // endregion

    // region Getter

    @Nonnull
    public HttpStatus getStatus() {
        return status;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDetails() {
        return details;
    }

    // endregion
}
