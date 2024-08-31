package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.enums.DestinationType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "destinations")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "destinations_id_seq")
    @SequenceGenerator(name = "destinations_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Column(length = 96)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull
    @Column(length = 96)
    private DestinationType type;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    private String mailTo;
    private String mailCC;
    private String mailBCC;

    private String apiAddress;
    private String authorizationHeader;

    private Integer maxAttachmentMegaBytes;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    //region Getters & Setters

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

    public DestinationType getType() {
        return type;
    }

    public void setType(DestinationType type) {
        this.type = type;
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

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public String getMailCC() {
        return mailCC;
    }

    public void setMailCC(String mailCC) {
        this.mailCC = mailCC;
    }

    public String getMailBCC() {
        return mailBCC;
    }

    public void setMailBCC(String mailBCC) {
        this.mailBCC = mailBCC;
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public void setApiAddress(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public Integer getMaxAttachmentMegaBytes() {
        return maxAttachmentMegaBytes;
    }

    public void setMaxAttachmentMegaBytes(Integer maxAttachmentMegaBytes) {
        this.maxAttachmentMegaBytes = maxAttachmentMegaBytes;
    }

    //endregion
}
