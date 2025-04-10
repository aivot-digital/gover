package de.aivot.GoverBackend.department.entities;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class DepartmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "departments_id_seq")
    @SequenceGenerator(name = "departments_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Column(length = 96)
    @Length(min = 3, max = 96)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull
    @Column(length = 255)
    @NotNull(message = "Address cannot be blank")
    private String address;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String imprint;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String privacy;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String accessibility;

    @NotNull
    @Column(length = 255)
    @NotBlank(message = "TechnicalSupportAddress cannot be blank")
    private String technicalSupportAddress;

    @NotNull
    @Column(length = 255)
    @NotBlank(message = "SpecialSupportAddress cannot be blank")
    private String specialSupportAddress;

    @Column(length = 255)
    private String departmentMail;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getDepartmentMail() {
        return departmentMail;
    }

    public void setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
    }


    // endregion
}
