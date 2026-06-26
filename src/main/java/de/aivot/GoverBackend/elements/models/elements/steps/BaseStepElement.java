package de.aivot.GoverBackend.elements.models.elements.steps;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nonnull;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GenericStepElement.class, name = ElementType.ID_Step),
        @JsonSubTypes.Type(value = IntroductionStepElement.class, name = ElementType.ID_IntroductionStep),
        @JsonSubTypes.Type(value = SubmitStepElement.class, name = ElementType.ID_SubmitStep),
        @JsonSubTypes.Type(value = SummaryStepElement.class, name = ElementType.ID_SummaryStep),
})
public abstract class BaseStepElement extends BaseElement {
    public BaseStepElement(@Nonnull ElementType type) {
        super(type);
    }
}
