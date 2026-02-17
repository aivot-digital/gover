package de.aivot.GoverBackend.user.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_user_deputy_with_details")
public class VUserDeputyWithDetailsEntity {
    @Id
    private Integer id;
    private LocalDateTime fromTimestamp;
    private LocalDateTime untilTimestamp;
    private Boolean active;

    private String originalUserId;
    private String originalUserEmail;
    private String originalUserFirstName;
    private String originalUserLastName;
    private Boolean originalUserEnabled;
    private Boolean originalUserVerified;
    private Boolean originalUserDeletedInIdp;
    private Integer originalUserSystemRoleId;
    private String originalUserFullName;

    private String deputyUserId;
    private String deputyUserEmail;
    private String deputyUserFirstName;
    private String deputyUserLastName;
    private Boolean deputyUserEnabled;
    private Boolean deputyUserVerified;
    private Boolean deputyUserDeletedInIdp;
    private Integer deputyUserSystemRoleId;
    private String deputyUserFullName;

    public Integer getId() {
        return id;
    }

    public VUserDeputyWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getFromTimestamp() {
        return fromTimestamp;
    }

    public VUserDeputyWithDetailsEntity setFromTimestamp(LocalDateTime fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
        return this;
    }

    public LocalDateTime getUntilTimestamp() {
        return untilTimestamp;
    }

    public VUserDeputyWithDetailsEntity setUntilTimestamp(LocalDateTime untilTimestamp) {
        this.untilTimestamp = untilTimestamp;
        return this;
    }

    public String getOriginalUserEmail() {
        return originalUserEmail;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserEmail(String originalUserEmail) {
        this.originalUserEmail = originalUserEmail;
        return this;
    }

    public String getOriginalUserFirstName() {
        return originalUserFirstName;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserFirstName(String originalUserFirstName) {
        this.originalUserFirstName = originalUserFirstName;
        return this;
    }

    public String getOriginalUserLastName() {
        return originalUserLastName;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserLastName(String originalUserLastName) {
        this.originalUserLastName = originalUserLastName;
        return this;
    }

    public Boolean getOriginalUserEnabled() {
        return originalUserEnabled;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserEnabled(Boolean originalUserEnabled) {
        this.originalUserEnabled = originalUserEnabled;
        return this;
    }

    public Boolean getOriginalUserVerified() {
        return originalUserVerified;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserVerified(Boolean originalUserVerified) {
        this.originalUserVerified = originalUserVerified;
        return this;
    }

    public Boolean getOriginalUserDeletedInIdp() {
        return originalUserDeletedInIdp;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserDeletedInIdp(Boolean originalUserDeletedInIdp) {
        this.originalUserDeletedInIdp = originalUserDeletedInIdp;
        return this;
    }

    public Integer getOriginalUserSystemRoleId() {
        return originalUserSystemRoleId;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserSystemRoleId(Integer originalUserSystemRoleId) {
        this.originalUserSystemRoleId = originalUserSystemRoleId;
        return this;
    }

    public String getOriginalUserFullName() {
        return originalUserFullName;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserFullName(String originalUserFullName) {
        this.originalUserFullName = originalUserFullName;
        return this;
    }

    public String getDeputyUserEmail() {
        return deputyUserEmail;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserEmail(String deputyUserEmail) {
        this.deputyUserEmail = deputyUserEmail;
        return this;
    }

    public String getDeputyUserFirstName() {
        return deputyUserFirstName;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserFirstName(String deputyUserFirstName) {
        this.deputyUserFirstName = deputyUserFirstName;
        return this;
    }

    public String getDeputyUserLastName() {
        return deputyUserLastName;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserLastName(String deputyUserLastName) {
        this.deputyUserLastName = deputyUserLastName;
        return this;
    }

    public Boolean getDeputyUserEnabled() {
        return deputyUserEnabled;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserEnabled(Boolean deputyUserEnabled) {
        this.deputyUserEnabled = deputyUserEnabled;
        return this;
    }

    public Boolean getDeputyUserVerified() {
        return deputyUserVerified;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserVerified(Boolean deputyUserVerified) {
        this.deputyUserVerified = deputyUserVerified;
        return this;
    }

    public Boolean getDeputyUserDeletedInIdp() {
        return deputyUserDeletedInIdp;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserDeletedInIdp(Boolean deputyUserDeletedInIdp) {
        this.deputyUserDeletedInIdp = deputyUserDeletedInIdp;
        return this;
    }

    public Integer getDeputyUserSystemRoleId() {
        return deputyUserSystemRoleId;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserSystemRoleId(Integer deputyUserSystemRoleId) {
        this.deputyUserSystemRoleId = deputyUserSystemRoleId;
        return this;
    }

    public String getDeputyUserFullName() {
        return deputyUserFullName;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserFullName(String deputyUserFullName) {
        this.deputyUserFullName = deputyUserFullName;
        return this;
    }

    public String getDeputyUserId() {
        return deputyUserId;
    }

    public VUserDeputyWithDetailsEntity setDeputyUserId(String deputyUserId) {
        this.deputyUserId = deputyUserId;
        return this;
    }

    public String getOriginalUserId() {
        return originalUserId;
    }

    public VUserDeputyWithDetailsEntity setOriginalUserId(String originalUserId) {
        this.originalUserId = originalUserId;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public VUserDeputyWithDetailsEntity setActive(Boolean active) {
        this.active = active;
        return this;
    }
}
