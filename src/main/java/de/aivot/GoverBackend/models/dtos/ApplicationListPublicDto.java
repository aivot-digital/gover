package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Application;

import java.time.LocalDateTime;

public class ApplicationListPublicDto {
    private Integer id;
    private String slug;
    private String version;
    private String headline;
    private Integer theme;
    private LocalDateTime updated;

    public static ApplicationListPublicDto valueOf(Application app) {
        ApplicationListPublicDto la = new ApplicationListPublicDto();

        la.id = app.getId();
        la.slug = app.getSlug();
        la.version = app.getVersion();
        la.headline = app.getRoot().getHeadline();
        if (app.getTheme() != null) {
            la.theme = app.getTheme().getId();
        }
        la.updated = app.getUpdated();

        return la;
    }

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Integer getTheme() {
        return theme;
    }

    public void setTheme(Integer theme) {
        this.theme = theme;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    // endregion
}
