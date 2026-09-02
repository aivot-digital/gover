package de.aivot.prosuna.backend.customLink.entities;

import de.aivot.prosuna.backend.customLink.converters.CustomLinkTypeConverter;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "custom_links")
public class CustomLink {
    private static final String ID_SEQUENCE_NAME = "custom_links_id_seq";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @NotNull
    @NotBlank
    @Size(max = 128)
    @Column(length = 128)
    private String label;

    @Nullable
    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @NotNull
    @NotBlank
    @Size(max = 500)
    @Column(length = 500)
    private String url;

    @Nullable
    @Size(max = 64)
    @Column(length = 64)
    private String icon;

    @NotNull
    @Column(columnDefinition = "int2")
    @Convert(converter = CustomLinkTypeConverter.class)
    private CustomLinkType type;

    @NotNull
    private Integer position;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Instant created;

    @NotNull
    private Instant updated;

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public CustomLink setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public CustomLink setLabel(String label) {
        this.label = label;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public CustomLink setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public CustomLink setUrl(String url) {
        this.url = url;
        return this;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public CustomLink setIcon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    public CustomLinkType getType() {
        return type;
    }

    public CustomLink setType(CustomLinkType type) {
        this.type = type;
        return this;
    }

    public Integer getPosition() {
        return position;
    }

    public CustomLink setPosition(Integer position) {
        this.position = position;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public CustomLink setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getUpdated() {
        return updated;
    }
}
