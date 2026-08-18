package de.aivot.gover.backend.user.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "user_deputies")
public class UserDeputyEntity {
    private static final String ID_SEQUENCE_NAME = "user_deputies_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    @NotNull(message = "Die ID der vertretenen Mitarbeiter:in darf nicht null sein.")
    @Size(min = 36, max = 36, message = "Die ID der vertretenen Mitarbeiter:in muss 36 Zeichen lang sein.")
    private String originalUserId;

    @Nonnull
    @NotNull(message = "Die ID der stellvertretenden Mitarbeiter:in darf nicht null sein.")
    @Size(min = 36, max = 36, message = "Die ID der stellvertretenden Mitarbeiter:in muss 36 Zeichen lang sein.")
    private String deputyUserId;

    @Nonnull
    @NotNull(message = "Das Startdatum der Vertretung darf nicht null sein.")
    private LocalDate fromDate;

    @Nullable
    private LocalDate untilDate;

    @Nonnull
    public Integer getId() {
        return id;
    }

    public UserDeputyEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getOriginalUserId() {
        return originalUserId;
    }

    public UserDeputyEntity setOriginalUserId(@Nonnull String originalUserId) {
        this.originalUserId = originalUserId;
        return this;
    }

    @Nonnull
    public String getDeputyUserId() {
        return deputyUserId;
    }

    public UserDeputyEntity setDeputyUserId(@Nonnull String deputyUserId) {
        this.deputyUserId = deputyUserId;
        return this;
    }

    @Nonnull
    public LocalDate getFromDate() {
        return fromDate;
    }

    public UserDeputyEntity setFromDate(@Nonnull LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    @Nullable
    public LocalDate getUntilDate() {
        return untilDate;
    }

    public UserDeputyEntity setUntilDate(@Nullable LocalDate untilDate) {
        this.untilDate = untilDate;
        return this;
    }
}
