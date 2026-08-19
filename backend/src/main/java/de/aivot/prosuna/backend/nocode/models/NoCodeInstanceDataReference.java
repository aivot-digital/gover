package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public class NoCodeInstanceDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeInstanceDataReference";

    private static final String PROCESS_DATA_KEY_REGEX = "[a-zA-Z0-9\\.\\[\\]_]+";
    private static final Pattern PROCESS_DATA_KEY_PATTERN = Pattern.compile(PROCESS_DATA_KEY_REGEX);

    @Nullable
    private String path;

    public NoCodeInstanceDataReference() {
        super(TYPE_ID);
    }

    public NoCodeInstanceDataReference(@Nullable String path) {
        super(TYPE_ID);
        this.path = path;
    }

    @Nonnull
    @Override
    public NoCodeOperandError validate() {
        if (StringUtils.isNullOrEmpty(path)) {
            return new NoCodeOperandError(this, "Der Instanzdaten-Schlüssel darf nicht leer sein.", null);
        }

        if (!PROCESS_DATA_KEY_PATTERN.matcher(path).matches()) {
            return new NoCodeOperandError(this, "Der Instanzdaten-Schlüssel darf nur Buchstaben (A-Z), Zahlen, Punkte, Unterstriche und eckigen Klammern enthalten.", null);
        }

        return NoCodeOperandError.NO_ERROR(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeInstanceDataReference that = (NoCodeInstanceDataReference) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public NoCodeInstanceDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
