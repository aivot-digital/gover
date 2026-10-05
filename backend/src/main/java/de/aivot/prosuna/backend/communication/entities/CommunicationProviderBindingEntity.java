package de.aivot.prosuna.backend.communication.entities;

import de.aivot.prosuna.backend.core.converters.AuthoredElementValuesConverter;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;
import java.util.UUID;

/**
 * Configures one concrete use of a communication provider by an identity provider.
 *
 * <p>The binding is deliberately addressable by its own ID. Runtime identities retain this
 * ID so that provider-specific mapping and customer input are always interpreted in the
 * context in which they were collected.</p>
 */
@Entity
@Table(name = "communication_provider_bindings")
public class CommunicationProviderBindingEntity {
    private static final String ID_SEQUENCE_NAME = "communication_provider_bindings_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull
    @Column(columnDefinition = "uuid")
    private UUID identityProviderKey;

    @Nonnull
    @NotNull
    private Integer communicationProviderId;

    @Nonnull
    @NotBlank
    @Size(max = 64)
    @Column(length = 64)
    private String name;

    @Nonnull
    @NotNull
    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @Nonnull
    @NotNull
    @ColumnDefault("FALSE")
    private Boolean isEnabled;

    @Nonnull
    @NotNull
    @ColumnDefault("0")
    private Integer position;

    @Nonnull
    @NotNull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = AuthoredElementValuesConverter.class)
    private AuthoredElementValues configuration;

    public CommunicationProviderBindingEntity() {
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        CommunicationProviderBindingEntity that = (CommunicationProviderBindingEntity) object;
        return Objects.equals(id, that.id)
                && Objects.equals(identityProviderKey, that.identityProviderKey)
                && Objects.equals(communicationProviderId, that.communicationProviderId)
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(isEnabled, that.isEnabled)
                && Objects.equals(position, that.position)
                && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identityProviderKey, communicationProviderId, name, description, isEnabled, position, configuration);
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public CommunicationProviderBindingEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public UUID getIdentityProviderKey() {
        return identityProviderKey;
    }

    public CommunicationProviderBindingEntity setIdentityProviderKey(@Nonnull UUID identityProviderKey) {
        this.identityProviderKey = identityProviderKey;
        return this;
    }

    @Nonnull
    public Integer getCommunicationProviderId() {
        return communicationProviderId;
    }

    public CommunicationProviderBindingEntity setCommunicationProviderId(@Nonnull Integer communicationProviderId) {
        this.communicationProviderId = communicationProviderId;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public CommunicationProviderBindingEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public CommunicationProviderBindingEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public Boolean getEnabled() {
        return isEnabled;
    }

    public CommunicationProviderBindingEntity setEnabled(@Nonnull Boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    @Nonnull
    public Integer getPosition() {
        return position;
    }

    public CommunicationProviderBindingEntity setPosition(@Nonnull Integer position) {
        this.position = position;
        return this;
    }

    @Nonnull
    public AuthoredElementValues getConfiguration() {
        return configuration;
    }

    public CommunicationProviderBindingEntity setConfiguration(@Nonnull AuthoredElementValues configuration) {
        this.configuration = configuration;
        return this;
    }
}
