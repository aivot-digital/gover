package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.process.models.ProcessNodeProvider;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Entity
@Table(name = "process_definition_nodes")
public class ProcessDefinitionNodeEntity {
    private static final String ID_SEQUENCE_NAME = "process_definition_nodes_id_seq";
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
    @NotBlank(message = "Der Code-Key darf nicht leer sein.")
    @NotNull(message = "Der Code-Key darf nicht null sein.")
    @Size(min = 1, max = 32, message = "Der Code-Key muss zwischen 1 und 32 Zeichen lang sein.")
    private String codeKey;

    @Nonnull
    @NotNull(message = "Die Konfiguration darf nicht null sein.")
    @Convert(converter = ElementDataConverter.class)
    @Column(columnDefinition = "jsonb")
    private ElementData configuration;

    // region Utils

    public String resolveName(ProcessNodeProvider provider) {
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

    public ProcessDefinitionNodeEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionNodeEntity setProcessDefinitionId(@Nonnull Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionNodeEntity setProcessDefinitionVersion(@Nonnull Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public ProcessDefinitionNodeEntity setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public ProcessDefinitionNodeEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public String getDataKey() {
        return dataKey;
    }

    public ProcessDefinitionNodeEntity setDataKey(@Nonnull String dataKey) {
        this.dataKey = dataKey;
        return this;
    }

    @Nonnull
    public String getCodeKey() {
        return codeKey;
    }

    public ProcessDefinitionNodeEntity setCodeKey(@Nonnull String codeKey) {
        this.codeKey = codeKey;
        return this;
    }

    @Nonnull
    public ElementData getConfiguration() {
        return configuration;
    }

    public ProcessDefinitionNodeEntity setConfiguration(@Nonnull ElementData configuration) {
        this.configuration = configuration;
        return this;
    }

    // endregion
}