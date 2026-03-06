package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.*;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.*;
import de.aivot.GoverBackend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.GoverBackend.lib.models.Identifiable;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Optional;

public enum ElementType implements Identifiable<Integer> {
    FormLayout(0),
    Step(1),
    Alert(2),
    Group(3),
    Checkbox(4),
    Date(5),
    Headline(6),
    MultiCheckbox(7),
    Number(8),
    ReplicatingContainer(9),
    RichText(10),
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
    SubmittedStep(21), // This step does not exist anymore, but is kept for compatibility
    FileUpload(22),
    DialogLayout(23),
    StepperLayout(24),
    ConfigLayout(25),
    FunctionInput(26),
    CodeInput(27),
    RichTextInput(28),
    UiDefinitionInput(29),
    IdentityInput(30),
    TabLayout(31),
    ChipInput(32),
    DateTime(33),
    DateRange(34),
    TimeRange(35),
    DateTimeRange(36),
    MapPoint(37),
    DomainAndUserSelect(38),
    AssignmentContext(39),
    DataModelSelect(40),
    DataObjectSelect(41),
    NoCodeInput(42),
    ;

    public static final String ID_FormLayout = "0";
    public static final String ID_Step = "1";
    public static final String ID_Alert = "2";
    public static final String ID_Group = "3";
    public static final String ID_Checkbox = "4";
    public static final String ID_Date = "5";
    public static final String ID_Headline = "6";
    public static final String ID_MultiCheckbox = "7";
    public static final String ID_Number = "8";
    public static final String ID_ReplicatingContainer = "9";
    public static final String ID_RichText = "10";
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
    public static final String ID_SubmittedStep = "21"; // This step does not exist anymore, but is kept for compatibility
    public static final String ID_FileUpload = "22";
    public static final String ID_DialogLayout = "23";
    public static final String ID_StepperLayout = "24";
    public static final String ID_ConfigLayout = "25";
    public static final String ID_FunctionInput = "26";
    public static final String ID_CodeInput = "27";
    public static final String ID_RichTextInput = "28";
    public static final String ID_UiDefinitionInput = "29";
    public static final String ID_IdentityInput = "30";
    public static final String ID_TabLayout = "31";
    public static final String ID_ChipInput = "32";
    public static final String ID_DateTime = "33";
    public static final String ID_DateRange = "34";
    public static final String ID_TimeRange = "35";
    public static final String ID_DateTimeRange = "36";
    public static final String ID_MapPoint = "37";
    public static final String ID_DomainAndUserSelect = "38";
    public static final String ID_AssignmentContext = "39";
    public static final String ID_DataModelSelect = "40";
    public static final String ID_DataObjectSelect = "41";
    public static final String ID_NoCodeInput = "42";

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

    @Nonnull
    public static BaseElement getElementClass(ElementType type) throws ElementDataConversionException {
        return switch (type) {
            case FormLayout -> new FormLayoutElement();
            case Step -> new StepElement();
            case Alert -> new AlertContentElement();
            case Group -> new GroupLayoutElement();
            case Checkbox -> new CheckboxInputElement();
            case Date -> new DateInputElement();
            case Headline -> new HeadlineContentElement();
            case MultiCheckbox -> new MultiCheckboxInputElement();
            case Number -> new NumberInputElement();
            case ReplicatingContainer -> new ReplicatingContainerLayoutElement();
            case RichText -> new RichTextContentElement();
            case Radio -> new RadioInputElement();
            case Select -> new SelectInputElement();
            case Spacer -> new SpacerContentElement();
            case Table -> new TableInputElement();
            case Text -> new TextInputElement();
            case Time -> new TimeInputElement();
            case IntroductionStep -> new IntroductionStepElement();
            case SubmitStep -> new SubmitStepElement();
            case SummaryStep -> new SummaryStepElement();
            case Image -> new ImageContentElement();
            case SubmittedStep ->
                    throw new ElementDataConversionException("Element type SubmittedStep is no longer supported.");
            case FileUpload -> new FileUploadInputElement();
            case DialogLayout -> new DialogLayoutElement();
            case StepperLayout -> new StepperLayoutElement();
            case ConfigLayout -> new ConfigLayoutElement();
            case FunctionInput -> new FunctionInputElement();
            case CodeInput -> new CodeInputElement();
            case RichTextInput -> new RichTextInputElement();
            case UiDefinitionInput -> new UiDefinitionInputElement();
            case IdentityInput -> new IdentityInputElement();
            case TabLayout -> new TabLayoutElement();
            case ChipInput -> new ChipInputElement();
            case DateTime -> new DateTimeInputElement();
            case DateRange -> new DateRangeInputElement();
            case TimeRange -> new TimeRangeInputElement();
            case DateTimeRange -> new DateTimeRangeInputElement();
            case MapPoint -> new MapPointInputElement();
            case DomainAndUserSelect -> new DomainAndUserSelectInputElement();
            case AssignmentContext -> new AssignmentContextInputElement();
            case DataModelSelect -> new DataModelSelectInputElement();
            case DataObjectSelect -> new DataObjectSelectInputElement();
            case NoCodeInput -> new NoCodeInputElement();
            default -> throw new ElementDataConversionException("Unsupported element type: %s", type.name());
        };
    }
}
