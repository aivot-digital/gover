package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.models.lib.DiffItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;

@Entity
@Table(name = "form_revisions")
public class FormRevision {
    @Id
    @Column(columnDefinition = "bigserial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "form_revisions_id_seq")
    @SequenceGenerator(name = "form_revisions_id_seq", allocationSize = 1)
    private BigInteger id;

    @NotNull
    private Integer formId;

    @NotNull
    private String userId;

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private Collection<DiffItem> diff;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public @NotNull Integer getFormId() {
        return formId;
    }

    public void setFormId(@NotNull Integer formId) {
        this.formId = formId;
    }

    public @NotNull String getUserId() {
        return userId;
    }

    public void setUserId(@NotNull String userId) {
        this.userId = userId;
    }

    public @NotNull LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NotNull LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public @NotNull Collection<DiffItem> getDiff() {
        return diff;
    }

    public void setDiff(@NotNull Collection<DiffItem> diff) {
        this.diff = diff;
    }
}
