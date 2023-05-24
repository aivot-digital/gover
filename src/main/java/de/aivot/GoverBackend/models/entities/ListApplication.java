package de.aivot.GoverBackend.models.entities;

public class ListApplication {
    private final Long id;
    private final String slug;
    private final String version;
    private final String headline;
    private final String theme;

    public ListApplication(Application app) {
        this.id = app.getId();
        this.slug = app.getSlug();
        this.version = app.getVersion();
        this.headline = app.getRoot().getHeadline();
        this.theme = app.getRoot().getTheme();
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getVersion() {
        return version;
    }

    public String getHeadline() {
        return headline;
    }

    public String getTheme() {
        return theme;
    }
}
