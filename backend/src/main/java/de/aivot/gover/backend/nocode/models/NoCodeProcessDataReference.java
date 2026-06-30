package de.aivot.gover.backend.nocode.models;

import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public class NoCodeProcessDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeProcessDataReference";

    private static final String PROCESS_DATA_KEY_REGEX = "[a-zA-Z0-9\\.\\*_\\[\\]]+";
    private static final Pattern PROCESS_DATA_KEY_PATTERN = Pattern.compile(PROCESS_DATA_KEY_REGEX);

    @Nullable
    private String path;

    public NoCodeProcessDataReference() {
        super(TYPE_ID);
    }

    public NoCodeProcessDataReference(@Nullable String path) {
        super(TYPE_ID);
        this.path = path;
    }

    @Override
    @Nonnull
    public NoCodeOperandError validate() {
        if (StringUtils.isNullOrEmpty(path)) {
            return new NoCodeOperandError(this, "Der Prozessdaten-Schlüssel darf nicht leer sein.", null);
        }

        if (!PROCESS_DATA_KEY_PATTERN.matcher(path).matches()) {
            return new NoCodeOperandError(this, "Der Prozessdaten-Schlüssel darf nur Buchstaben (A-Z), Zahlen, Punkte, Unterstriche, Sterne und eckigen Klammern enthalten.", null);
        }

        return new NoCodeOperandError(this, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeProcessDataReference that = (NoCodeProcessDataReference) o;
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

    public NoCodeProcessDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
