package de.aivot.GoverBackend.form.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @Column(columnDefinition = "text")
    private String publicTitle;

    @Nonnull
    private Integer developingDepartmentId;

    @Nullable
    private Integer managingDepartmentId;

    @Nullable
    private Integer responsibleDepartmentId;

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

    // region constructors

    // Empty constructor for JPA
    public FormEntity() {
    }

    // Full constructor
    public FormEntity(@Nonnull Integer id,
                      @Nonnull String slug,
                      @Nonnull String internalTitle,
                      @Nonnull String publicTitle,
                      @Nonnull Integer developingDepartmentId,
                      @Nullable Integer managingDepartmentId,
                      @Nullable Integer responsibleDepartmentId,
                      @Nonnull LocalDateTime created,
                      @Nonnull LocalDateTime updated,
                      @Nullable Integer publishedVersion,
                      @Nullable Integer draftedVersion) {
        this.id = id;
        this.slug = slug;
        this.internalTitle = internalTitle;
        this.publicTitle = publicTitle;
        this.developingDepartmentId = developingDepartmentId;
        this.managingDepartmentId = managingDepartmentId;
        this.responsibleDepartmentId = responsibleDepartmentId;
        this.created = created;
        this.updated = updated;
        this.publishedVersion = publishedVersion;
        this.draftedVersion = draftedVersion;
    }

    public static FormEntity from(FormVersionWithDetailsEntity formVersionWithDetailsEntity) {
        return new FormEntity(
                formVersionWithDetailsEntity.getId(),
                formVersionWithDetailsEntity.getSlug(),
                formVersionWithDetailsEntity.getInternalTitle(),
                formVersionWithDetailsEntity.getPublicTitle(),
                formVersionWithDetailsEntity.getDevelopingDepartmentId(),
                formVersionWithDetailsEntity.getManagingDepartmentId(),
                formVersionWithDetailsEntity.getResponsibleDepartmentId(),
                formVersionWithDetailsEntity.getCreated(),
                formVersionWithDetailsEntity.getUpdated(),
                formVersionWithDetailsEntity.getPublishedVersion(),
                formVersionWithDetailsEntity.getDraftedVersion()
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
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormEntity that = (FormEntity) object;
        return id.equals(that.id) && slug.equals(that.slug) && internalTitle.equals(that.internalTitle) && publicTitle.equals(that.publicTitle) && developingDepartmentId.equals(that.developingDepartmentId) && Objects.equals(managingDepartmentId, that.managingDepartmentId) && Objects.equals(responsibleDepartmentId, that.responsibleDepartmentId) && created.equals(that.created) && updated.equals(that.updated) && Objects.equals(publishedVersion, that.publishedVersion) && Objects.equals(draftedVersion, that.draftedVersion);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + slug.hashCode();
        result = 31 * result + internalTitle.hashCode();
        result = 31 * result + publicTitle.hashCode();
        result = 31 * result + developingDepartmentId.hashCode();
        result = 31 * result + Objects.hashCode(managingDepartmentId);
        result = 31 * result + Objects.hashCode(responsibleDepartmentId);
        result = 31 * result + created.hashCode();
        result = 31 * result + updated.hashCode();
        result = 31 * result + Objects.hashCode(publishedVersion);
        result = 31 * result + Objects.hashCode(draftedVersion);
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
    public String getPublicTitle() {
        return publicTitle;
    }

    public FormEntity setPublicTitle(@Nonnull String publicTitle) {
        this.publicTitle = publicTitle;
        return this;
    }

    @Nonnull
    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public FormEntity setDevelopingDepartmentId(@Nonnull Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    @Nullable
    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public FormEntity setManagingDepartmentId(@Nullable Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    @Nullable
    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public FormEntity setResponsibleDepartmentId(@Nullable Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
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

    @JsonIgnore
    public Integer getRelevantDepartmentId() {
        if (managingDepartmentId != null) {
            return managingDepartmentId;
        } else if (responsibleDepartmentId != null) {
            return responsibleDepartmentId;
        } else {
            return developingDepartmentId;
        }
    }

    // endregion
}
