package de.aivot.gover.backend.process.entities;

import de.aivot.gover.backend.core.converters.JsonObjectConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "process_test_configs")
public class ProcessTestConfigEntity {
    private static final String ID_SEQUENCE_NAME = "process_test_configs_id_seq";
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Name der Testkonfiguration darf nicht null sein.")
    @NotBlank(message = "Der Name der Testkonfiguration darf nicht leer sein.")
    @Length(min = 3, max = 128, message = "Der Name der Testkonfiguration muss zwischen 3 und 128 Zeichen lang sein.")
    private String name;

    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processVersion;

    @Nonnull
    @NotNull(message = "Die Konfigurationen dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> configs;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    // region Constructors

    // Empty constructor for JPA

    public ProcessTestConfigEntity() {

    }

    // Full Constructor

    public ProcessTestConfigEntity(@Nonnull Integer id,
                                   @Nonnull String name,
                                   @Nonnull Integer processId,
                                   @Nonnull Integer processVersion,
                                   @Nonnull Map<String, Object> configs,
                                   @Nonnull Instant created,
                                   @Nonnull Instant updated) {
        this.id = id;
        this.name = name;
        this.processId = processId;
        this.processVersion = processVersion;
        this.configs = configs;
        this.created = created;
        this.updated = updated;
    }

    // endregion

    // region Signals
    @PrePersist
    public void onCreate() {
        this.created = Instant.now();
        this.updated = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated = Instant.now();
    }
    // endregion

    // region HashCode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessTestConfigEntity that = (ProcessTestConfigEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion) && Objects.equals(configs, that.configs) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, processId, processVersion, configs, created, updated);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessTestConfigEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public ProcessTestConfigEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessTestConfigEntity setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessTestConfigEntity setProcessVersion(@Nonnull Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    @Nonnull
    public Map<String, Object> getConfigs() {
        return configs;
    }

    public ProcessTestConfigEntity setConfigs(@Nonnull Map<String, Object> configs) {
        this.configs = configs;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public ProcessTestConfigEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessTestConfigEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
