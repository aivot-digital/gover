package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

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
    SubmittedStep(21), // This step does not eixist anymore, but is kept for compatibility
    FileUpload(22);

    public static final String ID_Root = "0";
    public static final String ID_Step = "1";
    public static final String ID_Alert = "2";
    public static final String ID_Group = "3";
    public static final String ID_Checkbox = "4";
    public static final String ID_Date = "5";
    public static final String ID_Headline = "6";
    public static final String ID_MultiCheckbox = "7";
    public static final String ID_Number = "8";
    public static final String ID_ReplicatingContainer = "9";
    public static final String ID_Richtext = "10";
    public static final String ID_Radio = "11";
    public static final String ID_Select = "12";
    public static final String ID_Spacer = "13";
    public static final String ID_Table = "14";
    public static final String ID_Text = "15";
    public static final String ID_Time = "16";
    public static final String ID_IntroductionStep = "17";
    public static final String ID_SubmitStep = "18";
    public static final String ID_SummaryStep = "19";
    public static final String ID_Image = "20";
    public static final String ID_SubmittedStep = "21";  // This step does not eixist anymore, but is kept for compatibility
    public static final String ID_FileUpload = "22";

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
