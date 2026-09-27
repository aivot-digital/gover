package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeProcessDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeProcessDataReference";

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

        if (!ProcessDataValueUtils.isValidDestinationKey(path, false, true)) {
            return new NoCodeOperandError(this, "Verwenden Sie einen Vorgangsdatenpfad wie person.name, items[0].name oder items[*].name.", null);
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
