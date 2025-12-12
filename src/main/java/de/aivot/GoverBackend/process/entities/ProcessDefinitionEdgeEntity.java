package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "process_definition_edges")
public class ProcessDefinitionEdgeEntity {
    private static final String ID_SEQUENCE_NAME = "process_definition_edges_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processDefinitionId;

    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processDefinitionVersion;

    @Nonnull
    @NotNull(message = "Die Ausgehende-Knoten-ID darf nicht null sein.")
    private Integer fromNodeId;

    @Nonnull
    @NotNull(message = "Die Eingehende-Knoten-ID darf nicht null sein.")
    private Integer toNodeId;

    @Nonnull
    @NotNull(message = "Der Via-Port darf nicht null sein.")
    @NotBlank(message = "Der Via-Port darf nicht leer sein.")
    @Length(min = 1, max = 32, message = "Der Via-Port muss zwischen 1 und 32 Zeichen lang sein.")
    private String viaPort;

    // region Constructors

    // Empty constructor for JPA
    public ProcessDefinitionEdgeEntity() {
    }

    // Full constructor
    public ProcessDefinitionEdgeEntity(@Nonnull Integer id,
                                       @Nonnull Integer processDefinitionId,
                                       @Nonnull Integer processDefinitionVersion,
                                       @Nonnull Integer fromNodeId,
                                       @Nonnull Integer toNodeId,
                                       @Nonnull String viaPort) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionVersion = processDefinitionVersion;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.viaPort = viaPort;
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessDefinitionEdgeEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionEdgeEntity setProcessDefinitionId(@Nonnull Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionEdgeEntity setProcessDefinitionVersion(@Nonnull Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Nonnull
    public Integer getFromNodeId() {
        return fromNodeId;
    }

    public ProcessDefinitionEdgeEntity setFromNodeId(@Nonnull Integer fromNodeId) {
        this.fromNodeId = fromNodeId;
        return this;
    }

    @Nonnull
    public Integer getToNodeId() {
        return toNodeId;
    }

    public ProcessDefinitionEdgeEntity setToNodeId(@Nonnull Integer toNodeId) {
        this.toNodeId = toNodeId;
        return this;
    }

    @Nonnull
    public String getViaPort() {
        return viaPort;
    }

    public ProcessDefinitionEdgeEntity setViaPort(@Nonnull String viaPort) {
        this.viaPort = viaPort;
        return this;
    }


    // endregion
}