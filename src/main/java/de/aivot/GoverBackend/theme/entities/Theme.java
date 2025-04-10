package de.aivot.GoverBackend.theme.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "themes")
public class Theme {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "themes_id_seq")
    @SequenceGenerator(name = "themes_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Column(length = 96)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Main cannot be blank")
    private String main;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "MainDark cannot be blank")
    private String mainDark;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Accent cannot be blank")
    private String accent;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Error cannot be blank")
    private String error;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Warning cannot be blank")
    private String warning;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Info cannot be blank")
    private String info;

    @NotNull
    @Column(length = 7)
    @NotBlank(message = "Success cannot be blank")
    private String success;

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getMainDark() {
        return mainDark;
    }

    public void setMainDark(String mainDark) {
        this.mainDark = mainDark;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    // endregion
}
