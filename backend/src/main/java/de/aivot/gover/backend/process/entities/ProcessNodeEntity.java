package de.aivot.gover.backend.process.entities;

import de.aivot.gover.backend.core.converters.AuthoredElementValuesConverter;
import de.aivot.gover.backend.core.converters.JsonObjectConverter;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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

    @Nonnull
    private Boolean savedWithErrors = false;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    // region Properties

    public String getDisplayName() {
        if (StringUtils.isNotNullOrEmpty(name)) {
            return name;
        }

        // TODO: Get the name of the Process Definition Node based on the getName of the corresponding ProcessNodeDefinition

        return "";
    }

    // endregion

    // region Constructors

    public ProcessNodeEntity() {
    }

    public ProcessNodeEntity(@Nonnull Integer id,
                             @Nonnull Integer processId,
                             @Nonnull Integer processVersion,
                             @Nullable String name,
                             @Nullable String description,
                             @Nonnull String dataKey,
                             @Nonnull String processNodeDefinitionKey,
                             @Nonnull Integer processNodeDefinitionVersion,
                             @Nonnull AuthoredElementValues configuration,
                             @Nonnull Map<String, String> outputMappings,
                             @Nullable Integer timeLimitDays,
                             @Nullable String requirements,
                             @Nullable String notes,
                             @Nonnull Boolean savedWithErrors) {
        this.id = id;
        this.processId = processId;
        this.processVersion = processVersion;
        this.name = name;
        this.description = description;
        this.dataKey = dataKey;
        this.processNodeDefinitionKey = processNodeDefinitionKey;
        this.processNodeDefinitionVersion = processNodeDefinitionVersion;
        this.configuration = configuration;
        this.outputMappings = outputMappings;
        this.timeLimitDays = timeLimitDays;
        this.requirements = requirements;
        this.notes = notes;
        this.savedWithErrors = savedWithErrors;
    }

    // endregion

    // region Signale

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessNodeEntity that = (ProcessNodeEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion) &&
                Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(dataKey, that.dataKey) &&
                Objects.equals(processNodeDefinitionKey, that.processNodeDefinitionKey) &&
                Objects.equals(processNodeDefinitionVersion, that.processNodeDefinitionVersion) && Objects.equals(configuration, that.configuration) &&
                Objects.equals(outputMappings, that.outputMappings) && Objects.equals(timeLimitDays, that.timeLimitDays) &&
                Objects.equals(requirements, that.requirements) && Objects.equals(notes, that.notes) &&
                Objects.equals(savedWithErrors, that.savedWithErrors) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, processId, processVersion, name, description, dataKey, processNodeDefinitionKey, processNodeDefinitionVersion, configuration, outputMappings, timeLimitDays, requirements, notes, savedWithErrors, created, updated);
    }


    // endregion

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

    public ProcessNodeEntity setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessNodeEntity setProcessVersion(@Nonnull Integer processVersion) {
        this.processVersion = processVersion;
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

    @Nonnull
    public Boolean getSavedWithErrors() {
        return savedWithErrors;
    }

    public ProcessNodeEntity setSavedWithErrors(@Nonnull Boolean savedWithErrors) {
        this.savedWithErrors = savedWithErrors;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public ProcessNodeEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessNodeEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}