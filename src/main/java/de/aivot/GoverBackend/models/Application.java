package de.aivot.GoverBackend.models;

import de.aivot.GoverBackend.converters.ElementConverter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "applications", uniqueConstraints={
        @UniqueConstraint(columnNames = {"slug", "version"})
})
public class Application {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank(message = "Slug cannot be blank")
    private String slug;
    @NotBlank(message = "Version cannot be blank")
    @ColumnDefault("'1.0'")
    private String version;
    @NotBlank(message = "Status cannot be blank")
    @ColumnDefault("0")
    private ApplicationStatus status;
    @Lob
    @Convert(converter = ElementConverter.class)
    private RootElement root;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public RootElement getRoot() {
        return root;
    }

    public void setRoot(RootElement root) {
        this.root = root;
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

    public String getApplicationTitle() {
        return root.getHeadline() != null ? root.getHeadline() : (root.getTitle() != null ? root.getTitle() : getSlug());
    }

    public ApplicationPdfDto toPdfDto(Map<String, Object> customerData) {
        return new ApplicationPdfDto(this, customerData);
    }
}
