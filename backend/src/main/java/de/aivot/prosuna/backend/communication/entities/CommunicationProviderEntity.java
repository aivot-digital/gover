package de.aivot.prosuna.backend.communication.entities;

import de.aivot.prosuna.backend.core.converters.AuthoredElementValuesConverter;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;


@Entity
@Table(name = "communication_providers")
public class CommunicationProviderEntity {
    private static final String ID_SEQUENCE_NAME = "communication_providers_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    @NotNull(message = "Die ID des Kommunikationsanbieters darf nicht null sein.")
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Definition-Schlüssel des Kommunikationsanbieters darf nicht null sein.")
    @NotBlank(message = "Der Definition-Schlüssel des Kommunikationsanbieters darf nicht leer sein.")
    @Size(max = 255, message = "Der Definition-Schlüssel des Kommunikationsanbieters darf maximal 255 Zeichen lang sein.")
    private String communicationProviderDefinitionKey;

    @Nonnull
    @NotNull(message = "Die Definitionsversion des Kommunikationsanbieters darf nicht null sein.")
    private Integer communicationProviderDefinitionVersion;

    @Nonnull
    @Column(length = 64)
    @NotNull(message = "Der Name des Kommunikationsanbieters darf nicht null sein.")
    private String name;

    @Nonnull
    @Column(length = 255)
    @NotNull(message = "Die Beschreibung des Kommunikationsanbieters darf nicht null sein.")
    private String description;

    @Nonnull
    @NotNull(message = "Die Konfiguration des Kommunikationsanbieters darf nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = AuthoredElementValuesConverter.class)
    private AuthoredElementValues configuration;

    @Nonnull
    @NotNull(message = "Der Aktivierungsstatus des Kommunikationsanbieters darf nicht null sein.")
    @ColumnDefault("FALSE")
    private Boolean isEnabled;

    @Nonnull
    @NotNull(message = "Der Teststatus des Kommunikationsanbieters darf nicht null sein.")
    @ColumnDefault("FALSE")
    private Boolean isTestProvider;

    public CommunicationProviderEntity() {
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CommunicationProviderEntity that = (CommunicationProviderEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(communicationProviderDefinitionKey, that.communicationProviderDefinitionKey) && Objects.equals(communicationProviderDefinitionVersion, that.communicationProviderDefinitionVersion) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(configuration, that.configuration) && Objects.equals(isEnabled, that.isEnabled) && Objects.equals(isTestProvider, that.isTestProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, communicationProviderDefinitionKey, communicationProviderDefinitionVersion, name, description, configuration, isEnabled, isTestProvider);
    }

    // endregion

    // Getters & Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public void setId(@Nonnull Integer id) {
        this.id = id;
    }

    @Nonnull
    public String getCommunicationProviderDefinitionKey() {
        return communicationProviderDefinitionKey;
    }

    public void setCommunicationProviderDefinitionKey(@Nonnull String communicationProviderDefinitionKey) {
        this.communicationProviderDefinitionKey = communicationProviderDefinitionKey;
    }

    @Nonnull
    public Integer getCommunicationProviderDefinitionVersion() {
        return communicationProviderDefinitionVersion;
    }

    public void setCommunicationProviderDefinitionVersion(@Nonnull Integer communicationProviderDefinitionVersion) {
        this.communicationProviderDefinitionVersion = communicationProviderDefinitionVersion;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nonnull String description) {
        this.description = description;
    }

    @Nonnull
    public AuthoredElementValues getConfiguration() {
        return configuration;
    }

    public void setConfiguration(@Nonnull AuthoredElementValues configuration) {
        this.configuration = configuration;
    }

    @Nonnull
    public Boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(@Nonnull Boolean enabled) {
        isEnabled = enabled;
    }

    @Nonnull
    public Boolean getTestProvider() {
        return isTestProvider;
    }

    public void setTestProvider(@Nonnull Boolean testProvider) {
        isTestProvider = testProvider;
    }

    // endregion
}
