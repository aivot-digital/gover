package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.entities.Application;

import java.time.LocalDateTime;

public class ApplicationListDto {
    private Integer id;
    private String slug;
    private String version;
    private String headline;
    private Integer theme;
    private String title;
    private ApplicationStatus status;
    private Integer developingDepartment;
    private Integer managingDepartment;
    private Integer responsibleDepartment;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Integer openSubmissions;
    private Integer inProgressSubmissions;
    private Integer totalSubmissions;

    public ApplicationListDto() {
    }

    public ApplicationListDto(Application app) {
        id = app.getId();
        slug = app.getSlug();
        version = app.getVersion();
        headline = app.getRoot().getHeadline();
        if (app.getTheme() != null) {
            theme = app.getTheme().getId();
        }
        title = app.getTitle();
        status = app.getStatus();
        developingDepartment = app.getDevelopingDepartment().getId();
        if (app.getManagingDepartment() != null) {
            managingDepartment = app.getManagingDepartment().getId();
        }
        if (app.getResponsibleDepartment() != null) {
            responsibleDepartment = app.getResponsibleDepartment().getId();
        }
        created = app.getCreated();
        updated = app.getUpdated();
        openSubmissions = app.getOpenSubmissions();
        inProgressSubmissions = app.getInProgressSubmissions();
        totalSubmissions = app.getTotalSubmissions();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Integer getDevelopingDepartment() {
        return developingDepartment;
    }

    public void setDevelopingDepartment(Integer developingDepartment) {
        this.developingDepartment = developingDepartment;
    }

    public Integer getManagingDepartment() {
        return managingDepartment;
    }

    public void setManagingDepartment(Integer managingDepartment) {
        this.managingDepartment = managingDepartment;
    }

    public Integer getResponsibleDepartment() {
        return responsibleDepartment;
    }

    public void setResponsibleDepartment(Integer responsibleDepartment) {
        this.responsibleDepartment = responsibleDepartment;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Integer getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Integer totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Integer getOpenSubmissions() {
        return openSubmissions;
    }

    public void setOpenSubmissions(Integer openSubmissions) {
        this.openSubmissions = openSubmissions;
    }

    public Integer getInProgressSubmissions() {
        return inProgressSubmissions;
    }

    public void setInProgressSubmissions(Integer inProgressSubmissions) {
        this.inProgressSubmissions = inProgressSubmissions;
    }


    // endregion
}
