import {ElementType} from './element-type';
import {ElementTypesMap} from './element-types-map';

export const ElementNames: ElementTypesMap<string> = {
    [ElementType.Alert]: 'Hinweis',
    [ElementType.Checkbox]: 'Checkbox (Ja/Nein)',
    [ElementType.Image]: 'Bild',
    [ElementType.Container]: 'Gruppierung',
    [ElementType.Date]: 'Datum',
    [ElementType.Step]: 'Abschnitt',
    [ElementType.Root]: 'Formular',
    [ElementType.Headline]: 'Überschrift',
    [ElementType.MultiCheckbox]: 'Checkbox (Mehrfachauswahl)',
    [ElementType.Number]: 'Zahl',
    [ElementType.ReplicatingContainer]: 'Strukturierte Listeneingabe',
    [ElementType.Richtext]: 'Fließtext',
    [ElementType.Radio]: 'Radio-Button (Einfachauswahl)',
    [ElementType.Select]: 'Dropdown (Einfachauswahl)',
    [ElementType.Spacer]: 'Abstand',
    [ElementType.Table]: 'Tabelle',
    [ElementType.Text]: 'Text',
    [ElementType.Time]: 'Zeit',
    [ElementType.IntroductionStep]: 'Allgemeine Informationen',
    [ElementType.SummaryStep]: 'Zusammenfassung',
    [ElementType.SubmitStep]: 'Absenden des Antrages',
    [ElementType.SubmittedStep]: 'Antrag abgesendet',
    [ElementType.FileUpload]: 'Anlage(n)',
};
