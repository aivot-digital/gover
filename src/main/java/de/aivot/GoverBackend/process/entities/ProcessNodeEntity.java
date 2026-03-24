package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.AuthoredElementValuesConverter;
import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Map;

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
    @Size(min = 1, max = 128, message = "Der Schlüssel der Prozessknoten-Definition muss zwischen 1 und 128 Zeichen lang sein.")
    private String processNodeDefinitionKey;

    @Nonnull
    @NotNull(message = "Die Version  der Prozessknoten-Definition darf nicht null sein.")
    private Integer processNodeDefinitionVersion;

    @Nonnull
    @NotNull(message = "Die Konfiguration darf nicht null sein.")
    @Convert(converter = AuthoredElementValuesConverter.class)
    @Column(columnDefinition = "jsonb")
    private AuthoredElementValues configuration;

    @Nonnull
    @NotNull(message = "Die Input-Mappings dürfen nicht null sein.")
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> outputMappings;

    @Nullable
    @Min(value = 1, message = "Das Zeitlimit in Tagen muss mindestens 1 sein.")
    @Max(value = 3652, message = "Das Zeitlimit in Tagen darf maximal 3652 sein.")
    private Integer timeLimitDays;

    @Nullable
    @Size(max = 1024, message = "Die Anforderungen dürfen maximal 1024 Zeichen lang sein.")
    private String requirements;

    @Nullable
    @Size(max = 2048, message = "Die Notizen dürfen maximal 2048 Zeichen lang sein.")
    private String notes;

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
    public AuthoredElementValues getConfiguration() {
        return configuration;
    }

    public ProcessNodeEntity setConfiguration(@Nonnull AuthoredElementValues configuration) {
        this.configuration = configuration;
        return this;
    }

    @Nonnull
    public Map<String, String> getOutputMappings() {
        return outputMappings;
    }

    public ProcessNodeEntity setOutputMappings(@Nonnull Map<String, String> outputMappings) {
        this.outputMappings = outputMappings;
        return this;
    }

    @Nullable
    public Integer getTimeLimitDays() {
        return timeLimitDays;
    }

    public ProcessNodeEntity setTimeLimitDays(@Nullable Integer timeLimitDays) {
        this.timeLimitDays = timeLimitDays;
        return this;
    }

    @Nullable
    public String getRequirements() {
        return requirements;
    }

    public ProcessNodeEntity setRequirements(@Nullable String requirements) {
        this.requirements = requirements;
        return this;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    public ProcessNodeEntity setNotes(@Nullable String notes) {
        this.notes = notes;
        return this;
    }


    // endregion
}