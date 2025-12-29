package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "process_nodes")
public class ProcessNodeEntity {
    private static final String ID_SEQUENCE_NAME = "process_nodes_id_seq";
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processVersion;

    @Nullable
    @Size(max = 96, message = "Der Name darf maximal 96 Zeichen lang sein.")
    private String name;

    @Nullable
    @Size(max = 512, message = "Die Beschreibung darf maximal 512 Zeichen lang sein.")
    private String description;

    @Nonnull
    @NotBlank(message = "Der Data-Key darf nicht leer sein.")
    @NotNull(message = "Der Data-Key darf nicht null sein.")
    @Size(min = 1, max = 32, message = "Der Data-Key muss zwischen 1 und 32 Zeichen lang sein.")
    private String dataKey;

    @Nonnull
    @NotBlank(message = "Der Schlüssel der Prozessknoten-Definition darf nicht leer sein.")
    @NotNull(message = "Der Schlüssel der Prozessknoten-Definition darf nicht null sein.")
    @Size(min = 1, max = 32, message = "Der Schlüssel der Prozessknoten-Definition muss zwischen 1 und 32 Zeichen lang sein.")
    private String processNodeDefinitionKey;

    @Nonnull
    @NotBlank(message = "Die Version der Prozessknoten-Definition darf nicht leer sein.")
    @NotNull(message = "Die Version  der Prozessknoten-Definition darf nicht null sein.")
    private Integer processNodeDefinitionVersion;

    @Nonnull
    @NotNull(message = "Die Konfiguration darf nicht null sein.")
    @Convert(converter = ElementDataConverter.class)
    @Column(columnDefinition = "jsonb")
    private ElementData configuration;

    // region Utils

    public String resolveName(ProcessNodeDefinition provider) {
        if (StringUtils.isNotNullOrEmpty(name)) {
            return name;
        }
        return provider.getName();
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessNodeEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessNodeEntity setProcessId(@Nonnull Integer processDefinitionId) {
        this.processId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessNodeEntity setProcessVersion(@Nonnull Integer processDefinitionVersion) {
        this.processVersion = processDefinitionVersion;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public ProcessNodeEntity setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public ProcessNodeEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public String getDataKey() {
        return dataKey;
    }

    public ProcessNodeEntity setDataKey(@Nonnull String dataKey) {
        this.dataKey = dataKey;
        return this;
    }

    @Nonnull
    public String getProcessNodeDefinitionKey() {
        return processNodeDefinitionKey;
    }

    public ProcessNodeEntity setProcessNodeDefinitionKey(@Nonnull String processNodeDefinitionKey) {
        this.processNodeDefinitionKey = processNodeDefinitionKey;
        return this;
    }

    @Nonnull
    public Integer getProcessNodeDefinitionVersion() {
        return processNodeDefinitionVersion;
    }

    public ProcessNodeEntity setProcessNodeDefinitionVersion(@Nonnull Integer processNodeDefinitionVersion) {
        this.processNodeDefinitionVersion = processNodeDefinitionVersion;
        return this;
    }

    @Nonnull
    public ElementData getConfiguration() {
        return configuration;
    }

    public ProcessNodeEntity setConfiguration(@Nonnull ElementData configuration) {
        this.configuration = configuration;
        return this;
    }


    // endregion
}