import {AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';
import {generateElementIdForType} from './generate-element-id';
import {IntroductionStepElement} from '../models/elements/step-elements/introduction-step-element';
import {SummaryStepElement} from '../models/elements/step-elements/summary-step-element';
import {SubmitStepElement} from '../models/elements/step-elements/submit-step-element';

export function generateElementWithDefaultValues<T extends ElementType>(type: T): AnyElement | undefined {
    const id = generateElementIdForType(type);
    switch (type) {
        case ElementType.Root:
            return {
                id, type,
                children: [],
                introductionStep: generateElementWithDefaultValues(ElementType.IntroductionStep) as IntroductionStepElement,
                summaryStep: generateElementWithDefaultValues(ElementType.SummaryStep) as SummaryStepElement,
                submitStep: generateElementWithDefaultValues(ElementType.SubmitStep) as SubmitStepElement,
            };
        case ElementType.Step:
            return {
                id, type,
                children: [],
            };
        case ElementType.Alert:
            return {
                id, type,
                title: 'Hinweis',
                text: 'Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen.'
            };
        case ElementType.Container:
            return {
                id, type,
                children: [],
            };
        case ElementType.Checkbox:
            return {
                id, type,
                label: 'Checkbox (Ja/Nein)',
            };
        case ElementType.Date:
            return {
                id, type,
                label: 'Datum',
            };
        case ElementType.Headline:
            return {
                id, type,
                content: 'Überschrift',
            };
        case ElementType.MultiCheckbox:
            return {
                id, type,
                label: 'Checkbox (Mehrfachauswahl)',
                options: [
                    'Option 1',
                    'Option 2',
                    'Option 3'
                ],
            };
        case ElementType.Number:
            return {
                id, type,
                label: 'Zahl',
            };
        case ElementType.ReplicatingContainer:
            return {
                id, type,
                label: 'Strukturierte Listeneingabe',
                headlineTemplate: 'Datensatz Nr. #',
                children: [],
            };
        case ElementType.Richtext:
            return {
                id, type,
                content: '<p class="MuiTypography-root MuiTypography-body2">Fließtext</p>',
            };
        case ElementType.Radio:
            return {
                id, type,
                label: 'Radio-Button (Einfachauswahl)',
                options: [
                    'Option 1',
                    'Option 2',
                    'Option 3'
                ],
            };
        case ElementType.Select:
            return {
                id, type,
                label: 'Dropdown (Einfachauswahl)',
                options: [
                    'Option 1',
                    'Option 2',
                    'Option 3'
                ],
            };
        case ElementType.Spacer:
            return {
                id, type,
                height: '30',
            };
        case ElementType.Table:
            return {
                id, type,
                label: 'Tabelle',
                fields: [
                    {
                        'label': 'Spalte 1',
                        'datatype': 'string',
                        'placeholder': 'Inhalt für Spalte 1'
                    },
                    {
                        'label': 'Spalte 2',
                        'datatype': 'string',
                        'placeholder': 'Inhalt für Spalte 2'
                    }
                ],
            };
        case ElementType.Text:
            return {
                id, type,
                label: 'Text',
            };
        case ElementType.Time:
            return {
                id, type,
                label: 'Zeit',
            };
        case ElementType.IntroductionStep:
            return {
                id, type, // TODO: Mit Default-Daten befüllen
            };
        case ElementType.SubmitStep:
            return {
                id, type,
                textPreSubmit: 'Sie können Ihren Antrag nun verbindlich bei der zuständigen/bewirtschaftenden Stelle einreichen. Nach der Einreichung können Sie sich den Antrag für Ihre Unterlagen herunterladen oder zusenden lassen.',
                textPostSubmit: 'Sie können Ihren Antrag herunterladen oder sich per E-Mail zuschicken lassen. Wir empfehlen Ihnen, den Antrag anschließend zu Ihren Unterlagen zu nehmen.',
            };
        case ElementType.SummaryStep:
            return {
                id, type, // TODO: Mit Default-Daten befüllen
            };
        case ElementType.Image:
            return {
                id,
                type: ElementType.Image,
                alt: 'Aivot Logo',
                src: 'https://aivot.de/img/aivot-logo.svg',
            };
        case ElementType.FileUpload:
            return {
                id,
                type: ElementType.FileUpload,
                label: 'Anlage(n)',
            };
    }
}
