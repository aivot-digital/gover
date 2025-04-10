import {ElementType} from '../data/element-type/element-type';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../models/elements/steps/submit-step-element';
import {type AnyElement} from '../models/elements/any-element';
import {generateElementIdForType} from './id-utils';
import ProjectPackage from '../../package.json';
import {DateFieldComponentModelMode} from '../models/elements/form/input/date-field-element';

export function generateElementWithDefaultValues<T extends ElementType>(type: T): AnyElement | undefined {
    const id = generateElementIdForType(type);
    const appVersion = ProjectPackage.version;

    switch (type) {
        case ElementType.Root:
            return {
                id,
                type,
                appVersion,
                headline: 'Ihr Neues\nOnline-Formular',
                children: [],
                introductionStep: generateElementWithDefaultValues(ElementType.IntroductionStep) as IntroductionStepElement,
                summaryStep: generateElementWithDefaultValues(ElementType.SummaryStep) as SummaryStepElement,
                submitStep: generateElementWithDefaultValues(ElementType.SubmitStep) as SubmitStepElement,
                privacyText: 'Bitte beachten Sie die {privacy}Hinweise zum Datenschutz{/privacy}.',
            };
        case ElementType.Step:
            return {
                id,
                type,
                appVersion,
                children: [],
            };
        case ElementType.Alert:
            return {
                id,
                type,
                appVersion,
                title: 'Hinweis',
                text: '<p class="MuiTypography-root MuiTypography-body2">Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen.</p>',
            };
        case ElementType.Container:
            return {
                id,
                type,
                appVersion,
                children: [],
                storeLink: null,
            };
        case ElementType.Checkbox:
            return {
                id,
                type,
                appVersion,
                label: 'Bestätigung (Ja/Nein)',
            };
        case ElementType.Date:
            return {
                id,
                type,
                appVersion,
                label: 'Datum',
                mode: DateFieldComponentModelMode.Day,
            };
        case ElementType.Headline:
            return {
                id,
                type,
                appVersion,
                content: 'Überschrift',
            };
        case ElementType.MultiCheckbox:
            return {
                id,
                type,
                appVersion,
                label: 'Mehrfachauswahl',
                options: [
                    {
                        value: 'option_1',
                        label: 'Option 1',
                    },
                    {
                        value: 'option_2',
                        label: 'Option 2',
                    },
                    {
                        value: 'option_3',
                        label: 'Option 3',
                    },
                ],
            };
        case ElementType.Number:
            return {
                id,
                type,
                appVersion,
                label: 'Zahl',
            };
        case ElementType.ReplicatingContainer:
            return {
                id,
                type,
                appVersion,
                label: 'Strukturierte Listeneingabe',
                headlineTemplate: 'Datensatz Nr. #',
                children: [],
            };
        case ElementType.Richtext:
            return {
                id,
                type,
                appVersion,
                content: '<p class="MuiTypography-root MuiTypography-body2">Fließtext</p>',
            };
        case ElementType.Radio:
            return {
                id,
                type,
                appVersion,
                label: 'Einzelauswahl (Optionsfelder)',
                options: [
                    {
                        value: 'option_1',
                        label: 'Option 1',
                    },
                    {
                        value: 'option_2',
                        label: 'Option 2',
                    },
                    {
                        value: 'option_3',
                        label: 'Option 3',
                    },
                ],
            };
        case ElementType.Select:
            return {
                id,
                type,
                appVersion,
                label: 'Einzelauswahl (Auswahlmenü)',
                options: [
                    {
                        value: 'option_1',
                        label: 'Option 1',
                    },
                    {
                        value: 'option_2',
                        label: 'Option 2',
                    },
                    {
                        value: 'option_3',
                        label: 'Option 3',
                    },
                ],
            };
        case ElementType.Spacer:
            return {
                id,
                type,
                appVersion,
                height: '30',
            };
        case ElementType.Table:
            return {
                id,
                type,
                appVersion,
                label: 'Tabelle',
                fields: [
                    {
                        'label': 'Spalte 1',
                        'datatype': 'string',
                        'placeholder': 'Inhalt für Spalte 1',
                    },
                    {
                        'label': 'Spalte 2',
                        'datatype': 'string',
                        'placeholder': 'Inhalt für Spalte 2',
                    },
                ],
            };
        case ElementType.Text:
            return {
                id,
                type,
                appVersion,
                label: 'Text',
            };
        case ElementType.Time:
            return {
                id,
                type,
                appVersion,
                label: 'Uhrzeit',
            };
        case ElementType.IntroductionStep:
            return {
                id,
                type,
                appVersion, // TODO: Mit Default-Daten befüllen
            };
        case ElementType.SubmitStep:
            return {
                id,
                type,
                appVersion,
                textPreSubmit: 'Sie können Ihren Antrag nun verbindlich bei der zuständigen/bewirtschaftenden Stelle einreichen. Nach der Einreichung können Sie sich den Antrag für Ihre Unterlagen herunterladen oder zusenden lassen.',
                textPostSubmit: 'Sie können Ihren Antrag herunterladen oder sich per E-Mail zuschicken lassen. Wir empfehlen Ihnen, den Antrag anschließend zu Ihren Unterlagen zu nehmen.',
            };
        case ElementType.SummaryStep:
            return {
                id,
                type,
                appVersion, // TODO: Mit Default-Daten befüllen
            };
        case ElementType.Image:
            return {
                id,
                type,
                appVersion,
                alt: 'Beispiel-Grafik mit weißem Gover Logo auf blauem Hintergrund (bitte ersetzen)',
                src: `${window.location.protocol}//${window.location.host}${process.env.PUBLIC_URL}/assets/images/gover-beispiel-grafik.svg`,
            };
        case ElementType.FileUpload:
            return {
                id,
                type,
                appVersion,
                label: 'Anlage(n)',
                extensions: ['pdf'],
            };
    }
}
