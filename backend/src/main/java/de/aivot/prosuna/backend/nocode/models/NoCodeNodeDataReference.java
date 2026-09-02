package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public class NoCodeNodeDataReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeNodeDataReference";

    private static final String NODE_KEY_REGEX = "[a-zA-Z0-9_]+";
    private static final Pattern NODE_KEY_PATTERN = Pattern.compile(NODE_KEY_REGEX);

    private static final String PROCESS_DATA_KEY_REGEX = "[a-zA-Z0-9\\.\\[\\]_]+";
    private static final Pattern PROCESS_DATA_KEY_PATTERN = Pattern.compile(PROCESS_DATA_KEY_REGEX);

    @Nullable
    private String nodeDataKey;

    @Nullable
    private String path;

    public NoCodeNodeDataReference() {
        super(TYPE_ID);
    }

    public NoCodeNodeDataReference(@Nullable String nodeDataKey, @Nullable String path) {
        super(TYPE_ID);
        this.nodeDataKey = nodeDataKey;
        this.path = path;
    }

    @Override
    @Nonnull
    public NoCodeOperandError validate() {
        if (StringUtils.isNullOrEmpty(nodeDataKey)) {
            return new NoCodeOperandError(this, "Der Knotenschlüssel darf nicht leer sein.", null);
        }

        if (!NODE_KEY_PATTERN.matcher(nodeDataKey).matches()) {
            return new NoCodeOperandError(this, "Der Knotenschlüssel darf nur Buchstaben (A-Z), Zahlen und Unterstriche enthalten", null);
        }

        if (StringUtils.isNullOrEmpty(path)) {
            return new NoCodeOperandError(this, "Der Pfad darf nicht leer sein.", null);
        }

        if  (!PROCESS_DATA_KEY_PATTERN.matcher(path).matches()) {
            return new NoCodeOperandError(this, "Der Pfad darf nur Buchstaben (A-Z), Zahlen, Punkte, Unterstriche und eckigen Klammern enthalten.", null);
        }

        return NoCodeOperandError.NO_ERROR(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeNodeDataReference that = (NoCodeNodeDataReference) o;
        return Objects.equals(nodeDataKey, that.nodeDataKey)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeDataKey, path);
    }

    @Nullable
    public String getNodeDataKey() {
        return nodeDataKey;
    }

    public NoCodeNodeDataReference setNodeDataKey(@Nullable String nodeDataKey) {
        this.nodeDataKey = nodeDataKey;
        return this;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public NoCodeNodeDataReference setPath(@Nullable String path) {
        this.path = path;
        return this;
    }
}
