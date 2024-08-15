package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Arrays;
import java.util.Optional;

public enum ElementType implements Identifiable<Integer> {
    Root(0),
    Step(1),
    Alert(2),
    Group(3),
    Checkbox(4),
    Date(5),
    Headline(6),
    MultiCheckbox(7),
    Number(8),
    ReplicatingContainer(9),
    Richtext(10),
    Radio(11),
    Select(12),
    Spacer(13),
    Table(14),
    Text(15),
    Time(16),
    IntroductionStep(17),
    SubmitStep(18),
    SummaryStep(19),
    Image(20),
    SubmittedStep(21),
    FileUpload(22);

    private final Integer key;

    ElementType(Integer id) {
        this.key = id;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    public static Optional<ElementType> findElement(Object id) {
        return Arrays
                .stream(ElementType.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
