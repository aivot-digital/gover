package de.aivot.GoverBackend.storage.exceptions;

// TODO: Transform to checked exception?
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String format, Object... args) {
        super(String.format(format, args));
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }
}
