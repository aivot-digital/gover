package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "process_instance_attachment_sets")
public class ProcessInstanceAttachmentSetEntity {
    private static final String ID_SEQUENCE_NAME = "process_instance_attachment_sets_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Name darf nicht null sein.")
    @NotBlank(message = "Der Name darf nicht leer sein.")
    @Size(max = 255, message = "Der Name darf maximal 255 Zeichen lang sein.")
    private String name;

    @Nonnull
    @NotNull(message = "Der Datenschlüssel darf nicht null sein.")
    @NotBlank(message = "Der Datenschlüssel darf nicht leer sein.")
    @Size(max = 255, message = "Der Datenschlüssel darf maximal 255 Zeichen lang sein.")
    private String dataKey;

    @Nonnull
    @NotNull(message = "Die ID der Prozessinstanz darf nicht null sein.")
    private Long processInstanceId;

    @Nullable
    private Long processInstanceTaskId;

    public ProcessInstanceAttachmentSetEntity() {
    }

    public ProcessInstanceAttachmentSetEntity(@Nonnull Integer id,
                                              @Nonnull String name,
                                              @Nonnull String dataKey,
                                              @Nonnull Long processInstanceId,
                                              @Nullable Long processInstanceTaskId) {
        this.id = id;
        this.name = name;
        this.dataKey = dataKey;
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessInstanceAttachmentSetEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public ProcessInstanceAttachmentSetEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDataKey() {
        return dataKey;
    }

    public ProcessInstanceAttachmentSetEntity setDataKey(@Nonnull String dataKey) {
        this.dataKey = dataKey;
        return this;
    }

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceAttachmentSetEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nullable
    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceAttachmentSetEntity setProcessInstanceTaskId(@Nullable Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }
}
