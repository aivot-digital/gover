import {ElementType} from './element-type';
import {type AnyElement} from '../../models/elements/any-element';

const ElementNames: Record<ElementType, string> = {
    [ElementType.Alert]: 'Hinweis',
    [ElementType.Checkbox]: 'Bestätigung (Ja/Nein)',
    [ElementType.Image]: 'Bild',
    [ElementType.Container]: 'Gruppierung',
    [ElementType.Date]: 'Datum',
    [ElementType.Step]: 'Abschnitt',
    [ElementType.Root]: 'Formular',
    [ElementType.Headline]: 'Überschrift',
    [ElementType.MultiCheckbox]: 'Mehrfachauswahl',
    [ElementType.Number]: 'Zahl',
    [ElementType.ReplicatingContainer]: 'Strukturierte Listeneingabe',
    [ElementType.Richtext]: 'Fließtext',
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
};

export function getElementName(element: AnyElement): string {
    if (element.type === ElementType.Container && element.storeLink != null) {
        return 'Store-Baustein';
    }

    return getElementNameForType(element.type);
}

export function getElementNameForType(type: ElementType): string {
    return ElementNames[type];
}
