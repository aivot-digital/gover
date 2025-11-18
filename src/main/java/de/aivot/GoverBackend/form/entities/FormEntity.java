package de.aivot.GoverBackend.form.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "forms")
public class FormEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    @Column(length = 255)
    private String slug;

    @Nonnull
    @Column(length = 96)
    private String internalTitle;

    @Nonnull
    private Integer developingOrganizationalUnitId;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    @Nullable
    @Column(columnDefinition = "int2")
    private Integer publishedVersion;

    @Nullable
    @Column(columnDefinition = "int2")
    private Integer draftedVersion;

    @Nonnull
    private Integer versionCount;

    // region constructors

    // Empty constructor for JPA
    public FormEntity() {
    }

    // Full constructor
    public FormEntity(@Nonnull Integer id,
                      @Nonnull String slug,
                      @Nonnull String internalTitle,
                      @Nonnull Integer developingOrganizationalUnitId,
                      @Nonnull LocalDateTime created,
                      @Nonnull LocalDateTime updated,
                      @Nullable Integer publishedVersion,
                      @Nullable Integer draftedVersion,
                      @Nonnull Integer versionCount) {
        this.id = id;
        this.slug = slug;
        this.internalTitle = internalTitle;
        this.developingOrganizationalUnitId = developingOrganizationalUnitId;
        this.created = created;
        this.updated = updated;
        this.publishedVersion = publishedVersion;
        this.draftedVersion = draftedVersion;
        this.versionCount = versionCount;
    }

    public static FormEntity from(FormVersionWithDetailsEntity formVersionWithDetailsEntity) {
        return new FormEntity(
                formVersionWithDetailsEntity.getId(),
                formVersionWithDetailsEntity.getSlug(),
                formVersionWithDetailsEntity.getInternalTitle(),
                formVersionWithDetailsEntity.getDevelopingDepartmentId(),
                formVersionWithDetailsEntity.getCreated(),
                formVersionWithDetailsEntity.getUpdated(),
                formVersionWithDetailsEntity.getPublishedVersion(),
                formVersionWithDetailsEntity.getDraftedVersion(),
                formVersionWithDetailsEntity.getVersionCount()
        );
    }

    // endregion

    // region Signales

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // endregion

    // region Equals & HashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        FormEntity that = (FormEntity) o;
        return id.equals(that.id) && slug.equals(that.slug) && internalTitle.equals(that.internalTitle) && developingOrganizationalUnitId.equals(that.developingOrganizationalUnitId) && created.equals(that.created) && updated.equals(that.updated) && Objects.equals(publishedVersion, that.publishedVersion) && Objects.equals(draftedVersion, that.draftedVersion) && versionCount.equals(that.versionCount);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + slug.hashCode();
        result = 31 * result + internalTitle.hashCode();
        result = 31 * result + developingOrganizationalUnitId.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + updated.hashCode();
        result = 31 * result + Objects.hashCode(publishedVersion);
        result = 31 * result + Objects.hashCode(draftedVersion);
        result = 31 * result + versionCount.hashCode();
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public FormEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getSlug() {
        return slug;
    }

    public FormEntity setSlug(@Nonnull String slug) {
        this.slug = slug;
        return this;
    }

    @Nonnull
    public String getInternalTitle() {
        return internalTitle;
    }

    public FormEntity setInternalTitle(@Nonnull String title) {
        this.internalTitle = title;
        return this;
    }

    @Nonnull
    public Integer getDevelopingOrganizationalUnitId() {
        return developingOrganizationalUnitId;
    }

    public FormEntity setDevelopingOrganizationalUnitId(@Nonnull Integer developingDepartmentId) {
        this.developingOrganizationalUnitId = developingDepartmentId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public FormEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public FormEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Integer getPublishedVersion() {
        return publishedVersion;
    }

    public FormEntity setPublishedVersion(@Nullable Integer publishedVersion) {
        this.publishedVersion = publishedVersion;
        return this;
    }

    @Nullable
    public Integer getDraftedVersion() {
        return draftedVersion;
    }

    public FormEntity setDraftedVersion(@Nullable Integer draftedVersion) {
        this.draftedVersion = draftedVersion;
        return this;
    }

    @Nonnull
    public Integer getVersionCount() {
        return versionCount;
    }

    public FormEntity setVersionCount(@Nonnull Integer versionCount) {
        this.versionCount = versionCount;
        return this;
    }

    // endregion
}
