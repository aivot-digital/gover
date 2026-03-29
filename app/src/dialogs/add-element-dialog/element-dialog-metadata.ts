import {type ElementTypesMap} from '../../data/element-type/element-types-map';
import {ElementType} from '../../data/element-type/element-type';

export enum ElementTypeGroups {
    Display,
    Information,
    Input,
    DateTime,
    Select,
    Group,
    Other,
}

export const elementTypeGroupLabels: Record<ElementTypeGroups, string> = {
    [ElementTypeGroups.Display]: 'Darstellung',
    [ElementTypeGroups.Information]: 'Informationen',
    [ElementTypeGroups.Select]: 'Auswahl',
    [ElementTypeGroups.Input]: 'Eingabe',
    [ElementTypeGroups.DateTime]: 'Datum und Zeit',
    [ElementTypeGroups.Group]: 'Gruppierung',
    [ElementTypeGroups.Other]: 'Sonstige',
};

export const elementGroupMap: ElementTypesMap<ElementTypeGroups | null> = {
    [ElementType.Alert]: ElementTypeGroups.Information,
    [ElementType.Image]: ElementTypeGroups.Information,
    [ElementType.GroupLayout]: ElementTypeGroups.Display,
    [ElementType.Step]: null,
    [ElementType.FormLayout]: null,
    [ElementType.Checkbox]: ElementTypeGroups.Select,
    [ElementType.Date]: ElementTypeGroups.DateTime,
    [ElementType.Headline]: ElementTypeGroups.Information,
    [ElementType.MultiCheckbox]: ElementTypeGroups.Select,
    [ElementType.Number]: ElementTypeGroups.Input,
    [ElementType.ReplicatingContainer]: ElementTypeGroups.Input,
    [ElementType.RichText]: ElementTypeGroups.Information,
    [ElementType.Radio]: ElementTypeGroups.Select,
    [ElementType.Select]: ElementTypeGroups.Select,
    [ElementType.Spacer]: ElementTypeGroups.Display,
    [ElementType.Table]: ElementTypeGroups.Input,
    [ElementType.Text]: ElementTypeGroups.Input,
    [ElementType.ChipInput]: ElementTypeGroups.Input,
    [ElementType.Time]: ElementTypeGroups.DateTime,
    [ElementType.DateTime]: ElementTypeGroups.DateTime,
    [ElementType.DateRange]: ElementTypeGroups.DateTime,
    [ElementType.TimeRange]: ElementTypeGroups.DateTime,
    [ElementType.DateTimeRange]: ElementTypeGroups.DateTime,
    [ElementType.MapPoint]: ElementTypeGroups.Input,
    [ElementType.DomainAndUserSelect]: ElementTypeGroups.Input,
    [ElementType.AssignmentContext]: ElementTypeGroups.Input,
    [ElementType.DataModelSelect]: ElementTypeGroups.Input,
    [ElementType.DataObjectSelect]: ElementTypeGroups.Input,
    [ElementType.NoCodeInput]: ElementTypeGroups.Input,
    [ElementType.FileUpload]: ElementTypeGroups.Input,
    [ElementType.RichTextInput]: ElementTypeGroups.Input,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: null,
    [ElementType.CodeInput]: ElementTypeGroups.Input,
    [ElementType.UiDefinitionInput]: ElementTypeGroups.Input,
    [ElementType.IdentityInput]: null,
    [ElementType.TabLayout]: null,
};

export const elementTypeDescriptions: Partial<Record<ElementType, string>> = {
    [ElementType.Alert]: 'Zeigt hervorgehobene Hinweise im Formular an.',
    [ElementType.Image]: 'Bindet ein Bild in den Formularfluss ein.',
    [ElementType.GroupLayout]: 'Fasst mehrere Elemente zu einem Abschnitt zusammen.',
    [ElementType.Checkbox]: 'Erfasst eine einzelne Ja-/Nein-Angabe.',
    [ElementType.Date]: 'Erfasst ein einzelnes Datum.',
    [ElementType.Headline]: 'Gliedert Inhalte mit einer Überschrift.',
    [ElementType.MultiCheckbox]: 'Ermöglicht die Auswahl mehrerer Optionen.',
    [ElementType.Number]: 'Erfasst Zahlenwerte und Mengenangaben.',
    [ElementType.ReplicatingContainer]: 'Wiederholt einen Feldblock mehrfach.',
    [ElementType.RichText]: 'Zeigt formatierten Fließtext an.',
    [ElementType.Radio]: 'Ermöglicht genau eine Auswahl per Optionsfeld.',
    [ElementType.Select]: 'Ermöglicht genau eine Auswahl im Dropdown.',
    [ElementType.Spacer]: 'Erzeugt gezielten Abstand zwischen Inhalten.',
    [ElementType.Table]: 'Erfasst strukturierte Daten in Tabellenform.',
    [ElementType.Text]: 'Erfasst freie Texteingaben.',
    [ElementType.Time]: 'Erfasst eine einzelne Uhrzeit.',
    [ElementType.FileUpload]: 'Ermöglicht das Hochladen von Dateien.',
    [ElementType.ChipInput]: 'Erfasst mehrere Stichworte als Liste.',
    [ElementType.DateTime]: 'Erfasst Datum und Uhrzeit gemeinsam.',
    [ElementType.DateRange]: 'Erfasst einen Datumsbereich.',
    [ElementType.TimeRange]: 'Erfasst einen Uhrzeitbereich.',
    [ElementType.DateTimeRange]: 'Erfasst einen kombinierten Zeitbereich.',
    [ElementType.MapPoint]: 'Ermöglicht die Auswahl eines Punkts auf der Karte.',
    [ElementType.DomainAndUserSelect]: 'Wählt Organisationseinheiten oder Mitarbeitende aus.',
    [ElementType.AssignmentContext]: 'Definiert zuständige Personen oder Gruppen.',
    [ElementType.DataModelSelect]: 'Wählt ein Datenmodell aus.',
    [ElementType.DataObjectSelect]: 'Wählt ein konkretes Datenobjekt aus.',
    [ElementType.NoCodeInput]: 'Erfasst Logik über einen No-Code-Ausdruck.',
    [ElementType.CodeInput]: 'Erfasst technischen oder ausführbaren Code.',
    [ElementType.RichTextInput]: 'Erfasst formatierte Texte in Markdown.',
    [ElementType.UiDefinitionInput]: 'Definiert eine Benutzeroberfläche (UI) für z. B. Formulare oder Aufgaben.',
};

export function getElementGroupForType(type: ElementType): ElementTypeGroups | null {
    return elementGroupMap[type] ?? ElementTypeGroups.Other;
}

export function getElementGroupLabelForType(type: ElementType): string {
    const group = getElementGroupForType(type);
    return elementTypeGroupLabels[group ?? ElementTypeGroups.Other];
}

export function getElementDescriptionForType(type: ElementType): string {
    return elementTypeDescriptions[type] ?? 'Dieses Element steht für den aktuellen Bereich zur Verfügung.';
}
