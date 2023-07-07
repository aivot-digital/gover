package de.aivot.GoverBackend.models.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.converters.JacksonRootElementDeserializer;
import de.aivot.GoverBackend.converters.JacksonRootElementSerializer;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.entities.Application;


public class ApplicationDetailsFullDto extends ApplicationDetailsMinimalDto {

    @JsonSerialize(converter = JacksonRootElementSerializer.class)
    @JsonDeserialize(converter = JacksonRootElementDeserializer.class)
    private RootElement root;

    public ApplicationDetailsFullDto() {
    }

    public ApplicationDetailsFullDto(Application app) {
        super(app);
        root = app.getRoot();
    }

    // region Getters & Setters

    public RootElement getRoot() {
        return root;
    }

    public void setRoot(RootElement root) {
        this.root = root;
    }

    // endregion
}
