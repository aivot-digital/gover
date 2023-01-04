package de.aivot.GoverBackend.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ElementType {
    Root(0),
    Step(1),
    Alert(2),
    Container(3),
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
    SubmittedStep(21);

    private final int id;

    ElementType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean matches(Object id) {
        return id.equals(this.id);
    }

    public static Optional<ElementType> findElement(Object id) {
        return Arrays
                .stream(ElementType.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
