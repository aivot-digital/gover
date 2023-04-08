package de.aivot.GoverBackend.models.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank(message = "Name cannot be blank")
    private String name;
    @NotNull(message = "Address cannot be blank")
    private String address;
    @Column(columnDefinition = "TEXT")
    @NotNull(message = "Imprint cannot be blank")
    private String imprint;
    @Column(columnDefinition = "TEXT")
    @NotNull(message = "Privacy cannot be blank")
    private String privacy;
    @Column(columnDefinition = "TEXT")
    @NotNull(message = "Accessibility cannot be blank")
    private String accessibility;
    @NotNull(message = "TechnicalSupportAddress cannot be blank")
    private String technicalSupportAddress;
    @NotNull(message = "SpecialSupportAddress cannot be blank")
    private String specialSupportAddress;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImprint() {
        return imprint;
    }

    public void setImprint(String imprint) {
        this.imprint = imprint;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(String accessibility) {
        this.accessibility = accessibility;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public void setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public void setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
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
}
