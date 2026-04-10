package de.aivot.GoverBackend.lib.exceptions;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import org.springframework.http.HttpStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ResponseException extends Exception {
    @Nonnull
    private final HttpStatus status;
    @Nonnull
    private final String title;
    @Nullable
    private final Object details;

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
            @Nonnull DerivedRuntimeElementData elementData
    ) {
        super(title);
        this.status = status;
        this.title = title;
        this.details = elementData;
    }

    public ResponseException(
            @Nonnull HttpStatus status,
            @Nonnull String title,
            @Nonnull Throwable cause
    ) {
        super(title, cause);
        this.status = status;
        this.title = title;
        this.details = cause.getMessage();
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

    public static ResponseException badRequest(DerivedRuntimeElementData elementData) {
        return new ResponseException(
                HttpStatus.BAD_REQUEST,
                "Bei der Auswertung der Eingabedaten wurden Fehler gefunden.",
                elementData
        );
    }

    public static ResponseException badRequest(String message) {
        return new ResponseException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseException badRequest(String format, Object... args) {
        String message = String.format(format, args);
        return ResponseException.badRequest(message);
    }

    public static ResponseException badRequest(String message, Throwable cause) {
        return new ResponseException(HttpStatus.BAD_REQUEST, message, cause);
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

    public static ResponseException forbidden(String format, Object... args) {
        return ResponseException.forbidden(String.format(format, args));
    }

    public static ResponseException noSuperAdminPermission() {
        return ResponseException.forbidden("Sie müssen die Systemrolle „Superadministrator:in“ besitzen, um diese Aktion durchzuführen.");
    }

    public static ResponseException noSystemAdminPermission() {
        return ResponseException.forbidden("Sie müssen die Systemrolle „Systemadministrator:in“ besitzen, um diese Aktion durchzuführen.");
    }

    public static ResponseException noPermission(String permissionName) {
        return ResponseException.forbidden(String.format(
                "Sie müssen die Systemrolle „Superadministrator:in“ besitzen, oder benötigen eine Domänenrolle mit der Berechtigung „%s“, um diese Aktion durchzuführen.", permissionName));
    }

    public static ResponseException notFound() {
        return ResponseException.notFound("Die angeforderte Ressource wurde nicht gefunden.");
    }

    public static ResponseException notFound(String message, Object... args) {
        return new ResponseException(HttpStatus.NOT_FOUND, String.format(message, args));
    }

    public static ResponseException notFound(String message) {
        return new ResponseException(HttpStatus.NOT_FOUND, message);
    }

    public static ResponseException conflict(String message, Object... args) {
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

    public static ResponseException internalServerError(String format, Object... args) {
        return ResponseException.internalServerError(String.format(format, args));
    }

    public static ResponseException internalServerError(Throwable cause, String format, Object... args) {
        return ResponseException.internalServerError(String.format(format, args), cause);
    }

    public static ResponseException internalServerError(String message, Throwable cause) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public static ResponseException internalServerError(Throwable cause) {
        return new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.", cause);
    }

    public static ResponseException notAcceptable() {
        return ResponseException.conflict("Die angeforderte Ressource ist nicht in dem gewünschten Format verfügbar.");
    }

    public static ResponseException notAcceptable(String format, Object... args) {
        return ResponseException.notAcceptable(String.format(format, args));
    }

    public static ResponseException notAcceptable(String message) {
        return new ResponseException(HttpStatus.NOT_ACCEPTABLE, "Die angeforderte Ressource ist nicht in dem gewünschten Format verfügbar.");
    }

    public static ResponseException methodNotAllowed(String message) {
        return new ResponseException(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    public static ResponseException methodNotAllowed(String format, Object... args) {
        return new ResponseException(HttpStatus.METHOD_NOT_ALLOWED, String.format(format, args));
    }

    public static ResponseException unsupportedMediaType(String message) {
        return new ResponseException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    public static ResponseException unsupportedMediaType(String format, Object... args) {
        return ResponseException.unsupportedMediaType(String.format(format, args));
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
    public Object getDetails() {
        return details;
    }

    // endregion
}
