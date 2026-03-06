import {ElementType} from './element-type';
import {type AnyElement} from '../../models/elements/any-element';

const ElementNames: Record<ElementType, string> = {
    [ElementType.Alert]: 'Hinweis',
    [ElementType.Checkbox]: 'Bestätigung (Ja/Nein)',
    [ElementType.Image]: 'Bild',
    [ElementType.GroupLayout]: 'Gruppierung',
    [ElementType.Date]: 'Datum',
    [ElementType.Step]: 'Abschnitt',
    [ElementType.FormLayout]: 'Formular',
    [ElementType.Headline]: 'Überschrift',
    [ElementType.MultiCheckbox]: 'Mehrfachauswahl',
    [ElementType.Number]: 'Zahl',
    [ElementType.ReplicatingContainer]: 'Strukturierte Listeneingabe',
    [ElementType.RichText]: 'Fließtext',
    [ElementType.Radio]: 'Einzelauswahl (Optionsfelder)',
    [ElementType.Select]: 'Einzelauswahl (Auswahlmenü)',
    [ElementType.Spacer]: 'Abstand',
    [ElementType.Table]: 'Tabelle',
    [ElementType.Text]: 'Text',
    [ElementType.Time]: 'Uhrzeit',
    [ElementType.IntroductionStep]: 'Allgemeine Informationen',
    [ElementType.SummaryStep]: 'Zusammenfassung',
    [ElementType.SubmitStep]: 'Absenden des Antrages',
    [ElementType.SubmittedStep]: 'Antrag abgesendet',
    [ElementType.FileUpload]: 'Anlage(n)',
    [ElementType.DialogLayout]: 'Dialog',
    [ElementType.StepperLayout]: 'Abschnitte',
    [ElementType.ConfigLayout]: 'Konfigurationsbereich',
    [ElementType.FunctionInput]: 'Funktionseingabe',
    [ElementType.CodeInput]: 'Codeeingabe',
    [ElementType.RichTextInput]: 'Markdown-Eingabe',
    [ElementType.UiDefinitionInput]: 'UI-Definition Eingabe',
    [ElementType.IdentityInput]: 'Identitätseingabe',
    [ElementType.TabLayout]: 'Tabs',
    [ElementType.ChipInput]: 'Tag-Liste (Schlagwörter)',
    [ElementType.DateTime]: 'Datum und Uhrzeit',
    [ElementType.DateRange]: 'Datumsspanne',
    [ElementType.TimeRange]: 'Zeitspanne',
    [ElementType.DateTimeRange]: 'Datum- und Zeitspanne',
    [ElementType.MapPoint]: 'Kartenpunkt (Technische Preview)',
    [ElementType.DomainAndUserSelect]: 'Domänen- und Mitarbeitendenauswahl',
    [ElementType.AssignmentContext]: 'Verantwortlicher Personenkreis',
    [ElementType.DataModelSelect]: 'Datenmodell-Auswahl',
    [ElementType.DataObjectSelect]: 'Datenobjekt-Auswahl',
    [ElementType.NoCodeInput]: 'No-Code-Eingabe',
};

export function getElementName(element: AnyElement): string {
    if (element.type === ElementType.GroupLayout && element.storeLink != null) {
        return 'Store-Baustein';
    }

    return getElementNameForType(element.type);
}

export function getElementNameForType(type: ElementType): string {
    return ElementNames[type];
}
