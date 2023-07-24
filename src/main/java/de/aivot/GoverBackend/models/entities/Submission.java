package de.aivot.GoverBackend.models.entities;

import de.aivot.GoverBackend.converters.JsonObjectConverter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @Column(length = 36)
    private String id;

    @NotNull
    @ManyToOne
    private Application application;

    @NotNull
    private LocalDateTime created;

    @ManyToOne
    private User assignee;

    private String fileNumber;

    private LocalDateTime archived;

    @NotNull
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> customerInput;

    @ManyToOne
    private Destination destination;

    private Boolean destinationSuccess;

    @NotNull
    private Boolean isTestSubmission;

    @NotNull
    @ColumnDefault("FALSE")
    private Boolean copySent;

    @NotNull
    @ColumnDefault("0")
    private Integer copyTries;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
    }

    // region Getter & Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public void setArchived(LocalDateTime archived) {
        this.archived = archived;
    }

    public Map<String, Object> getCustomerInput() {
        return customerInput;
    }

    public void setCustomerInput(Map<String, Object> customerInput) {
        this.customerInput = customerInput;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Boolean getDestinationSuccess() {
        return destinationSuccess;
    }

    public void setDestinationSuccess(Boolean destinationSuccess) {
        this.destinationSuccess = destinationSuccess;
    }

    public Boolean getIsTestSubmission() {
        return isTestSubmission;
    }

    public void setIsTestSubmission(Boolean testSubmission) {
        isTestSubmission = testSubmission;
    }

    public Boolean getCopySent() {
        return copySent;
    }

    public void setCopySent(Boolean copySent) {
        this.copySent = copySent;
    }

    public Integer getCopyTries() {
        return copyTries;
    }

    public void setCopyTries(Integer copyTries) {
        this.copyTries = copyTries;
    }

    // endregion
}
