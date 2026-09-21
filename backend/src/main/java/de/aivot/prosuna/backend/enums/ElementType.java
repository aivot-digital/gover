package de.aivot.prosuna.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.*;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.*;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.IntroductionStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SummaryStepElement;
import de.aivot.prosuna.backend.lib.models.Identifiable;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Optional;

public enum ElementType implements Identifiable<Integer> {
    FormLayout(0, "Formular", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    Step(
            1,
            "Abschnitt",
            "Ein generischer Abschnitt für mehrstufige Formulare, der flexibel mit verschiedenen Elementen befüllt werden kann."
    ),
    Alert(2, "Hinweis", "Zeigt hervorgehobene Hinweise im Formular an."),
    GroupLayout(3, "Gruppierung", "Fasst inhaltlich zusammengehörige Elemente zusammen."),
    Checkbox(4, "Bestätigung (Ja/Nein)", "Erfasst eine einzelne Ja-/Nein-Angabe."),
    Date(5, "Datum", "Erfasst ein einzelnes Datum."),
    Headline(6, "Überschrift", "Gliedert Inhalte mit einer Überschrift."),
    MultiCheckbox(7, "Mehrfachauswahl", "Ermöglicht die Auswahl mehrerer Optionen."),
    Number(8, "Zahl", "Erfasst Zahlenwerte und Mengenangaben."),
    ReplicatingContainerLayout(9, "Strukturierte Listeneingabe", "Wiederholt eine Elementgruppe mehrfach."),
    RichText(10, "Fließtext", "Zeigt formatierten Fließtext an."),
    Radio(11, "Einzelauswahl (Optionsfelder)", "Ermöglicht genau eine Auswahl per Optionsfeld."),
    Select(12, "Einzelauswahl", "Ermöglicht genau eine Auswahl aus vorgegebenen Optionen."),
    Spacer(13, "Abstand", "Erzeugt gezielten Abstand zwischen Inhalten."),
    Table(14, "Tabelle", "Erfasst strukturierte Daten in Tabellenform."),
    Text(15, "Text", "Erfasst freie Texteingaben."),
    Time(16, "Uhrzeit", "Erfasst eine einzelne Uhrzeit."),
    IntroductionStep(
            17,
            "Allgemeine Informationen",
            "Optionaler Abschnitt am Anfang eines mehrstufigen Formulars, der zur Einführung oder zur Erklärung des weiteren Ablaufs genutzt werden kann."
    ),
    SubmitStep(
            18,
            "Abschluss und Einreichung",
            "Optionaler Abschnitt am Ende eines Formulars. Zeigt Hinweise vor der Einreichung an und schützt die Übermittlung mit einer Sicherheitsprüfung vor automatisierten Einreichungen (Captcha)."
    ),
    SummaryStep(
            19,
            "Zusammenfassung",
            "Optionaler Abschnitt mit der Zusammenfassung aller eingegebenen Informationen und einer Bestätigung, dass die eingegebenen Daten korrekt sind."
    ),
    Image(20, "Bild", "Bindet ein Bild in den Formularfluss ein."),
    SubmittedStep(
            21,
            "Einreichung abgeschlossen",
            "Dieses Element steht für den aktuellen Bereich zur Verfügung."
    ), // This step does not exist anymore, but is kept for compatibility
    FileUpload(22, "Anlage(n)", "Ermöglicht das Hochladen von Dateien."),
    DialogLayout(23, "Dialog", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    StepperLayout(24, "Abschnitte", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    ConfigLayout(25, "Konfigurationsbereich", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    FunctionInput(26, "Funktionseingabe", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    CodeInput(27, "Codeeingabe", "Erfasst technischen oder ausführbaren Code."),
    RichTextInput(28, "Markdown-Eingabe", "Erfasst formatierte Texte in Markdown."),
    UiDefinitionInput(
            29,
            "UI-Definition-Editor",
            "Definiert eine Benutzeroberfläche (UI) für z. B. Formulare oder Aufgaben."
    ),
    IdentityConfig(
            30,
            "Identitätseingabe",
            "Ermöglicht eine Identifizierung über Servicekonten oder alternativ die Eingabe einer E-Mail-Adresse."
    ),
    TabLayout(31, "Tabs", "Dieses Element steht für den aktuellen Bereich zur Verfügung."),
    ChipInput(32, "Tag-Liste (Schlagwörter)", "Erfasst mehrere Stichworte als Liste."),
    DateTime(33, "Datum und Uhrzeit", "Erfasst Datum und Uhrzeit gemeinsam."),
    DateRange(34, "Datumsspanne", "Erfasst einen Datumsbereich."),
    TimeRange(35, "Zeitspanne", "Erfasst einen Uhrzeitbereich."),
    DateTimeRange(36, "Datum- und Zeitspanne", "Erfasst einen kombinierten Zeitbereich."),
    MapPoint(37, "Kartenpunkt (Technische Preview)", "Ermöglicht die Auswahl eines Punkts auf der Karte."),
    DomainAndUserSelect(
            38,
            "Domänen- und Mitarbeitendenauswahl",
            "Wählt Organisationseinheiten oder Mitarbeitende aus."
    ),
    AssignmentContext(
            39,
            "Verantwortlicher Personenkreis",
            "Definiert zuständige Personen oder Gruppen."
    ),
    DataModelSelect(40, "Datenmodell-Auswahl", "Wählt ein Datenmodell aus."),
    DataObjectSelect(41, "Datenobjekt-Auswahl", "Wählt ein konkretes Datenobjekt aus."),
    NoCodeInput(42, "No-Code-Eingabe", "Erfasst Logik über einen No-Code-Ausdruck."),
    SummaryLayout(43, "Zusammenfassung", "Fasst mehrere Elemente zu einer Übersicht zusammen."),
    ProcessDataKeyInput(
            44,
            "Prozessdaten-Schlüssel",
            "Erfasst einen Prozessdaten-Schlüssel und schlägt vorhandene Pfade vor."
    ),
    ProcessAttachmentDisplay(
            45,
            "Anhang zum Vorgang",
            "Zeigt Vorgangsanhänge zur Ansicht und zum Download an."
    ),
    ProcessInstanceAttachmentSetSelect(
            46,
            "Anlagensatz-Auswahl",
            "Wählt einen oder mehrere Anlagensätze der Prozessinstanz aus."
    ),
    ProcessIdentityIdInput(
            47,
            "Prozessidentitäts-Auswahl",
            "Wählt eine verfügbare Prozessidentität aus."
    ),
    HtmlTemplateInput(
            48,
            "HTML-Vorlage",
            "Befüllt Slots einer HTML-Vorlage mit Text, Rich-Text oder Bildern."
    ),
    StoragePathSelector(
            49,
            "Speicherpfad-Auswahl",
            "Wählt einen Speicheranbieter und einen Ordner oder Zielpfad aus."
    ),
    PaymentConfig(
            50,
            "Zahlungskonfiguration",
            "Konfiguriert den Zahlungsdienstleister und die Zahlungsposten eines Formulars."
    ),
    LinkButton(
            51,
            "Link-Button",
            "Zeigt einen Button an, der einen Link öffnet oder in Aufgabenansichten ein Ereignis auslöst."
    ),
    SecretSelectInput(
            52,
            "Geheimnis-Auswahl",
            "Wählt ein sicher hinterlegtes Geheimnis aus und speichert dessen Schlüssel."
    ),
    AssetSelectInput(
            53,
            "Asset-Auswahl",
            "Wählt eine Datei aus den Assets aus und speichert deren stabilen Schlüssel."
    ),
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
    public static final String ID_SummaryLayout = "43";
    public static final String ID_ProcessDataKeyInput = "44";
    public static final String ID_ProcessAttachmentDisplay = "45";
    public static final String ID_ProcessInstanceAttachmentSetSelect = "46";
    public static final String ID_ProcessIdentityIdInput = "47";
    public static final String ID_HtmlTemplateInput = "48";
    public static final String ID_StoragePathSelector = "49";
    public static final String ID_PaymentConfig = "50";
    public static final String ID_LinkButton = "51";
    public static final String ID_SecretSelectInput = "52";
    public static final String ID_AssetSelectInput = "53";

    private final Integer key;
    private final String displayName;
    private final String description;

    ElementType(
            Integer id,
            String displayName,
            String description
    ) {
        this.key = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
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
            case Step -> new GenericStepElement();
            case Alert -> new AlertContentElement();
            case GroupLayout -> new GroupLayoutElement();
            case Checkbox -> new CheckboxInputElement();
            case Date -> new DateInputElement();
            case Headline -> new HeadlineContentElement();
            case MultiCheckbox -> new MultiCheckboxInputElement();
            case Number -> new NumberInputElement();
            case ReplicatingContainerLayout -> new ReplicatingContainerLayoutElement();
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
            case IdentityConfig -> new IdentityConfigElement();
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
            case SummaryLayout -> new SummaryLayoutElement();
            case ProcessDataKeyInput -> new ProcessDataKeyInputElement();
            case ProcessAttachmentDisplay -> new ProcessAttachmentDisplayContentElement();
            case ProcessInstanceAttachmentSetSelect -> new ProcessInstanceAttachmentSetSelectElement();
            case ProcessIdentityIdInput -> new ProcessIdentityIdInputElement();
            case HtmlTemplateInput -> new HtmlTemplateInputElement();
            case StoragePathSelector -> new StoragePathSelectorInputElement();
            case PaymentConfig -> new PaymentConfigElement();
            case LinkButton -> new LinkButtonContentElement();
            case SecretSelectInput -> new SecretSelectInputElement();
            case AssetSelectInput -> new AssetSelectInputElement();
            default -> throw new ElementDataConversionException("Unsupported element type: %s", type.name());
        };
    }
}
