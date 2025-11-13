package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nullable;
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

    @Nullable
    private Integer themeId;

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

    public DepartmentEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public DepartmentEntity setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getImprint() {
        return imprint;
    }

    public DepartmentEntity setImprint(String imprint) {
        this.imprint = imprint;
        return this;
    }

    public String getPrivacy() {
        return privacy;
    }

    public DepartmentEntity setPrivacy(String privacy) {
        this.privacy = privacy;
        return this;
    }

    public String getAccessibility() {
        return accessibility;
    }

    public DepartmentEntity setAccessibility(String accessibility) {
        this.accessibility = accessibility;
        return this;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public DepartmentEntity setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public DepartmentEntity setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public DepartmentEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    @Nullable
    public Integer getThemeId() {
        return themeId;
    }

    public DepartmentEntity setThemeId(@Nullable Integer themeId) {
        this.themeId = themeId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public DepartmentEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public DepartmentEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
