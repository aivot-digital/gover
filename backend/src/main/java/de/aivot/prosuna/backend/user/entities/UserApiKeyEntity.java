package de.aivot.prosuna.backend.user.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "user_api_keys")
public class UserApiKeyEntity {
    private static final String ID_SEQUENCE_NAME = "user_api_keys_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der Mitarbeiter:in darf nicht null sein.")
    @Size(min = 36, max = 36, message = "Die ID der Mitarbeiter:in muss 36 Zeichen lang sein.")
    private String userId;

    @Nonnull
    @NotNull(message = "Der Name des API-Schlüssels darf nicht null sein.")
    @NotBlank(message = "Der Name des API-Schlüssels darf nicht leer sein.")
    @Size(max = 255, message = "Der Name des API-Schlüssels darf höchstens 255 Zeichen lang sein.")
    private String name;

    @Nonnull
    @Column(nullable = false, updatable = false)
    private Instant created;

    @Nonnull
    @NotNull(message = "Das Ablaufdatum des API-Schlüssels darf nicht null sein.")
    private Instant expiration;

    /**
     * Lowercase hex SHA-256 digest of the complete API key including its prefix. The key itself is never stored.
     */
    @Nonnull
    @NotNull(message = "Der Hash des API-Schlüssels darf nicht null sein.")
    @Size(min = 64, max = 64, message = "Der Hash des API-Schlüssels muss 64 Zeichen lang sein.")
    private String keyHash;

    @PrePersist
    public void prePersist() {
        created = Instant.now();
    }

    @Nonnull
    public Integer getId() {
        return id;
    }

    public UserApiKeyEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public UserApiKeyEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public UserApiKeyEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public UserApiKeyEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getExpiration() {
        return expiration;
    }

    public UserApiKeyEntity setExpiration(@Nonnull Instant expiration) {
        this.expiration = expiration;
        return this;
    }

    @Nonnull
    public String getKeyHash() {
        return keyHash;
    }

    public UserApiKeyEntity setKeyHash(@Nonnull String keyHash) {
        this.keyHash = keyHash;
        return this;
    }
}
