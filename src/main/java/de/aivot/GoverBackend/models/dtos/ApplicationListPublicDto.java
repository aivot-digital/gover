package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Application;

public class ApplicationListPublicDto {
    private Integer id;
    private String slug;
    private String version;
    private String headline;
    private String theme;

    public static ApplicationListPublicDto valueOf(Application app) {
        ApplicationListPublicDto la = new ApplicationListPublicDto();

        la.id = app.getId();
        la.slug = app.getSlug();
        la.version = app.getVersion();
        la.headline = app.getRoot().getHeadline();
        la.theme = app.getRoot().getTheme();

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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }


    // endregion
}
