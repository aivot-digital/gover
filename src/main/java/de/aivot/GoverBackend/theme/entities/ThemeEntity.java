package de.aivot.GoverBackend.theme.entities;

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
    @Column(length = 7)
    private String main;

    @Nonnull
    @Column(length = 7)
    private String mainDark;

    @Nonnull
    @Column(length = 7)
    private String accent;

    @Nonnull
    @Column(length = 7)
    private String error;

    @Nonnull
    @Column(length = 7)
    private String warning;

    @Nonnull
    @Column(length = 7)
    private String info;

    @Nonnull
    @Column(length = 7)
    private String success;

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
                       @Nonnull String main,
                       @Nonnull String mainDark,
                       @Nonnull String accent,
                       @Nonnull String error,
                       @Nonnull String warning,
                       @Nonnull String info,
                       @Nonnull String success,
                       @Nullable UUID logoKey,
                       @Nullable UUID faviconKey) {
        this.id = id;
        this.name = name;
        this.main = main;
        this.mainDark = mainDark;
        this.accent = accent;
        this.error = error;
        this.warning = warning;
        this.info = info;
        this.success = success;
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
    public String getMain() {
        return main;
    }

    public ThemeEntity setMain(@Nonnull String main) {
        this.main = main;
        return this;
    }

    @Nonnull
    public String getMainDark() {
        return mainDark;
    }

    public ThemeEntity setMainDark(@Nonnull String mainDark) {
        this.mainDark = mainDark;
        return this;
    }

    @Nonnull
    public String getAccent() {
        return accent;
    }

    public ThemeEntity setAccent(@Nonnull String accent) {
        this.accent = accent;
        return this;
    }

    @Nonnull
    public String getError() {
        return error;
    }

    public ThemeEntity setError(@Nonnull String error) {
        this.error = error;
        return this;
    }

    @Nonnull
    public String getWarning() {
        return warning;
    }

    public ThemeEntity setWarning(@Nonnull String warning) {
        this.warning = warning;
        return this;
    }

    @Nonnull
    public String getInfo() {
        return info;
    }

    public ThemeEntity setInfo(@Nonnull String info) {
        this.info = info;
        return this;
    }

    @Nonnull
    public String getSuccess() {
        return success;
    }

    public ThemeEntity setSuccess(@Nonnull String success) {
        this.success = success;
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
