package de.aivot.prosuna.backend.theme.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "themes")
public class ThemeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "themes_id_seq")
    @SequenceGenerator(name = "themes_id_seq", allocationSize = 1)
    private Integer id;

    @Nonnull
    @Column(length = 96)
    private String name;

    @Nonnull
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Nonnull
    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Nullable
    @Column(name = "primary_color_dark", length = 7)
    private String primaryColorDark;

    @Nullable
    @Column(name = "secondary_color_dark", length = 7)
    private String secondaryColorDark;

    @Nullable
    @Column(columnDefinition = "uuid")
    private UUID logoKey;

    @Nullable
    @Column(columnDefinition = "uuid")
    private UUID faviconKey;

    // region Constructors

    // Empty constructor for JPA
    public ThemeEntity() {
    }

    // Full constructor

    public ThemeEntity(Integer id,
                       @Nonnull String name,
                       @Nonnull String primaryColor,
                       @Nonnull String secondaryColor,
                       @Nullable String primaryColorDark,
                       @Nullable String secondaryColorDark,
                       @Nullable UUID logoKey,
                       @Nullable UUID faviconKey) {
        this.id = id;
        this.name = name;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.primaryColorDark = primaryColorDark;
        this.secondaryColorDark = secondaryColorDark;
        this.logoKey = logoKey;
        this.faviconKey = faviconKey;
    }

    // endregion

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public ThemeEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public ThemeEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getPrimaryColor() {
        return primaryColor;
    }

    public ThemeEntity setPrimaryColor(@Nonnull String primaryColor) {
        this.primaryColor = primaryColor;
        return this;
    }

    @Nonnull
    public String getSecondaryColor() {
        return secondaryColor;
    }

    public ThemeEntity setSecondaryColor(@Nonnull String secondaryColor) {
        this.secondaryColor = secondaryColor;
        return this;
    }

    @Nullable
    public String getPrimaryColorDark() {
        return primaryColorDark;
    }

    public ThemeEntity setPrimaryColorDark(@Nullable String primaryColorDark) {
        this.primaryColorDark = primaryColorDark;
        return this;
    }

    @Nullable
    public String getSecondaryColorDark() {
        return secondaryColorDark;
    }

    public ThemeEntity setSecondaryColorDark(@Nullable String secondaryColorDark) {
        this.secondaryColorDark = secondaryColorDark;
        return this;
    }

    @Nullable
    public UUID getLogoKey() {
        return logoKey;
    }

    public ThemeEntity setLogoKey(@Nullable UUID logoKey) {
        this.logoKey = logoKey;
        return this;
    }

    @Nullable
    public UUID getFaviconKey() {
        return faviconKey;
    }

    public ThemeEntity setFaviconKey(@Nullable UUID faviconKey) {
        this.faviconKey = faviconKey;
        return this;
    }

    // endregion
}
