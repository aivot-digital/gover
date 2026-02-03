package de.aivot.GoverBackend.storage.models;

import jakarta.annotation.Nonnull;

import java.util.List;

public class KnownFileExtension {
    @Nonnull
    private String name;

    @Nonnull
    private String mime;

    @Nonnull
    private List<String> extensions;

    public KnownFileExtension() {
        this.name = "";
        this.mime = "";
        this.extensions = List.of();
    }

    public KnownFileExtension(@Nonnull String name,
                              @Nonnull String mime,
                              @Nonnull List<String> extensions) {
        this.name = name;
        this.mime = mime;
        this.extensions = extensions;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public KnownFileExtension setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getMime() {
        return mime;
    }

    public KnownFileExtension setMime(@Nonnull String mime) {
        this.mime = mime;
        return this;
    }

    @Nonnull
    public List<String> getExtensions() {
        return extensions;
    }

    public KnownFileExtension setExtensions(@Nonnull List<String> extensions) {
        this.extensions = extensions;
        return this;
    }
}
