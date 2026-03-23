package de.aivot.GoverBackend.form.entities;

import de.aivot.GoverBackend.models.lib.DiffItem;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "form_revisions")
public class FormRevisionEntity {
    @Id
    @Nonnull
    @Column(columnDefinition = "bigserial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "form_revisions_id_seq")
    @SequenceGenerator(name = "form_revisions_id_seq", allocationSize = 1)
    private BigInteger id;

    @Nonnull
    private Integer formId;

    @Nonnull
    @Column(columnDefinition = "int2")
    private Integer formVersion;

    @Nonnull
    private String userId;

    @Nonnull
    private LocalDateTime timestamp;

    @Nonnull
    @JdbcTypeCode(SqlTypes.JSON)
    private Collection<DiffItem> diff;

    // region constructors

    public FormRevisionEntity() {
    }

    public FormRevisionEntity(@Nonnull BigInteger id,
                              @Nonnull Integer formId,
                              @Nonnull Integer formVersion,
                              @Nonnull String userId,
                              @Nonnull LocalDateTime timestamp,
                              @Nonnull Collection<DiffItem> diff) {
        this.id = id;
        this.formId = formId;
        this.formVersion = formVersion;
        this.userId = userId;
        this.timestamp = timestamp;
        this.diff = diff;
    }

    // endregion

    // region Equals and HashCode

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        FormRevisionEntity that = (FormRevisionEntity) object;
        return Objects.equals(id, that.id) && Objects.equals(formId, that.formId) && formVersion.equals(that.formVersion) && Objects.equals(userId, that.userId) && Objects.equals(timestamp, that.timestamp) && Objects.equals(diff, that.diff);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(formId);
        result = 31 * result + formVersion.hashCode();
        result = 31 * result + Objects.hashCode(userId);
        result = 31 * result + Objects.hashCode(timestamp);
        result = 31 * result + Objects.hashCode(diff);
        return result;
    }

    // endregion

    // start region Getters and Setters

    @Nonnull
    public BigInteger getId() {
        return id;
    }

    public FormRevisionEntity setId(@Nonnull BigInteger id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getFormId() {
        return formId;
    }

    public FormRevisionEntity setFormId(@Nonnull Integer formId) {
        this.formId = formId;
        return this;
    }

    @Nonnull
    public Integer getFormVersion() {
        return formVersion;
    }

    public FormRevisionEntity setFormVersion(@Nonnull Integer formVersion) {
        this.formVersion = formVersion;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public FormRevisionEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public FormRevisionEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    public Collection<DiffItem> getDiff() {
        return diff;
    }

    public FormRevisionEntity setDiff(@Nonnull Collection<DiffItem> diff) {
        this.diff = diff;
        return this;
    }

    // endregion
}
