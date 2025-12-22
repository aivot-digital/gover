import {ElementType} from '../data/element-type/element-type';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../models/elements/steps/submit-step-element';
import {type AnyElement, AnyElementType} from '../models/elements/any-element';
import {generateElementIdForType} from './id-utils';
import {DateFieldComponentModelMode, DateFieldElement} from '../models/elements/form/input/date-field-element';
import {RootElement} from '../models/elements/root-element';
import {BaseElement} from '../models/elements/base-element';
import {StepElement} from '../models/elements/steps/step-element';
import {AlertElement} from '../models/elements/form/content/alert-element';
import {BaseFormElement} from '../models/elements/form/base-form-element';
import {GroupLayout} from '../models/elements/form/layout/group-layout';
import {CheckboxFieldElement} from '../models/elements/form/input/checkbox-field-element';
import {BaseInputElement} from '../models/elements/form/base-input-element';
import {HeadlineElement} from '../models/elements/form/content/headline-element';
import {MultiCheckboxFieldElement} from '../models/elements/form/input/multi-checkbox-field-element';
import {NumberFieldElement} from '../models/elements/form/input/number-field-element';
import {ReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';
import {RichtextElement} from '../models/elements/form/content/richtext-element';
import {RadioFieldElement} from '../models/elements/form/input/radio-field-element';
import {SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {SpacerElement} from '../models/elements/form/content/spacer-element';
import {TableFieldElement} from '../models/elements/form/input/table-field-element';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {TimeFieldElement} from '../models/elements/form/input/time-field-element';
import {ImageElement} from '../models/elements/form/content/image-element';
import {SubmittedStepElement} from '../models/elements/steps/submitted-step-element';
import {FileUploadElement} from '../models/elements/form/input/file-upload-element';
import {AppInfo} from '../app-info';

function makeBase<T extends ElementType>(t: T, id: string): BaseElement<T> {
    return {
        id: id,
        type: t,
        name: undefined,
        metadata: undefined,
        override: undefined,
        visibility: undefined,
        testProtocolSet: undefined,
    };
}

function makeFormBase<T extends ElementType>(t: T, id: string): BaseFormElement<T> {
    return {
        ...makeBase(t, id),
        weight: 12,
    };
}

function makeInputBase<T extends ElementType>(t: T, id: string): Omit<BaseInputElement<T>, 'label'> {
    return {
        ...makeFormBase(t, id),
        destinationKey: undefined,
        disabled: undefined,
        hint: undefined,
        required: undefined,
        technical: undefined,
        validation: undefined,
        value: undefined,
    };
}

const elementConstructors: {
    [ElementType.FormLayout]: (id: string) => RootElement;
    [ElementType.Step]: (id: string) => StepElement;
    [ElementType.Alert]: (id: string) => AlertElement;
    [ElementType.GroupLayout]: (id: string) => GroupLayout;
    [ElementType.Checkbox]: (id: string) => CheckboxFieldElement;
    [ElementType.Date]: (id: string) => DateFieldElement;
    [ElementType.Headline]: (id: string) => HeadlineElement;
    [ElementType.MultiCheckbox]: (id: string) => MultiCheckboxFieldElement;
    [ElementType.Number]: (id: string) => NumberFieldElement;
    [ElementType.ReplicatingContainer]: (id: string) => ReplicatingContainerLayout;
    [ElementType.RichText]: (id: string) => RichtextElement;
    [ElementType.Radio]: (id: string) => RadioFieldElement;
    [ElementType.Select]: (id: string) => SelectFieldElement;
    [ElementType.Spacer]: (id: string) => SpacerElement;
    [ElementType.Table]: (id: string) => TableFieldElement;
    [ElementType.Text]: (id: string) => TextFieldElement;
    [ElementType.Time]: (id: string) => TimeFieldElement;
    [ElementType.IntroductionStep]: (id: string) => IntroductionStepElement;
    [ElementType.SubmitStep]: (id: string) => SubmitStepElement;
    [ElementType.SummaryStep]: (id: string) => SummaryStepElement;
    [ElementType.Image]: (id: string) => ImageElement;
    [ElementType.SubmittedStep]: (id: string) => SubmittedStepElement;
    [ElementType.FileUpload]: (id: string) => FileUploadElement;
    [ElementType.DialogLayout]: (id: string) => void;
    [ElementType.StepperLayout]: (id: string) => void;
    [ElementType.ConfigLayout]: (id: string) => void;
    [ElementType.FunctionInput]: (id: string) => void;
    [ElementType.CodeInput]: (id: string) => void;
    [ElementType.RichTextInput]: (id: string) => void;
    [ElementType.UiDefinitionInput]: (id: string) => void;
    [ElementType.IdentityInput]: (id: string) => void;
    [ElementType.TabLayout]: (id: string) => void;
} = {
    [ElementType.FormLayout]: (id) => ({
        ...makeBase(ElementType.FormLayout, id),
        headline: 'Ihr Neues\nOnline-Formular',
        tabTitle: undefined,
        children: [],
        expiring: undefined,
        privacyText: 'Bitte beachten Sie die {privacy}Hinweise zum Datenschutz{/privacy}.',
        offlineSubmissionText: undefined,
        offlineSignatureNeeded: undefined,
        introductionStep: generateElementWithDefaultValues(ElementType.IntroductionStep) as IntroductionStepElement,
        summaryStep: generateElementWithDefaultValues(ElementType.SummaryStep) as SummaryStepElement,
        submitStep: generateElementWithDefaultValues(ElementType.SubmitStep) as SubmitStepElement,
    }),
    [ElementType.Step]: (id) => ({
        ...makeBase(ElementType.Step, id),
        children: [],
        icon: undefined,
        title: undefined,
    }),
    [ElementType.Alert]: (id) => ({
        ...makeFormBase(ElementType.Alert, id),
        title: 'Hinweis',
        text: '<p class="MuiTypography-root MuiTypography-body2">Nutzen Sie diesen Hinweis, um Antragsstellenden zusätzliche Informationen hervorgehoben bereitzustellen.</p>',
        alertType: 'info',
    }),
    [ElementType.GroupLayout]: (id) => ({
        ...makeFormBase(ElementType.GroupLayout, id),
        children: [],
        storeLink: null,
    }),
    [ElementType.Checkbox]: (id) => ({
        ...makeInputBase(ElementType.Checkbox, id),
        label: 'Bestätigung (Ja/Nein)',
    }),
    [ElementType.Date]: (id) => ({
        ...makeInputBase(ElementType.Date, id),
        label: 'Datum',
        mode: DateFieldComponentModelMode.Day,
        autocomplete: undefined,
        placeholder: undefined,
    }),
    [ElementType.Headline]: (id) => ({
        ...makeFormBase(ElementType.Headline, id),
        content: 'Überschrift',
        small: false,
        uppercase: false,
    }),
    [ElementType.MultiCheckbox]: (id) => ({
        ...makeInputBase(ElementType.MultiCheckbox, id),
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
        displayInline: false,
        minimumRequiredOptions: undefined,
    }),
    [ElementType.Number]: (id) => ({
        ...makeInputBase(ElementType.Number, id),
        label: 'Zahl',
        placeholder: undefined,
        decimalPlaces: undefined,
        suffix: undefined,
    }),
    [ElementType.ReplicatingContainer]: (id) => ({
        ...makeInputBase(ElementType.ReplicatingContainer, id),
        label: 'Strukturierte Listeneingabe',
        headlineTemplate: 'Datensatz Nr. #',
        children: [],
        minimumRequiredSets: undefined,
        maximumSets: undefined,
        addLabel: undefined,
        removeLabel: undefined,
    }),
    [ElementType.RichText]: (id) => ({
        ...makeFormBase(ElementType.RichText, id),
        content: '<p class="MuiTypography-root MuiTypography-body2">Fließtext</p>',
    }),
    [ElementType.Radio]: (id) => ({
        ...makeInputBase(ElementType.Radio, id),
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
        displayInline: false,
    }),
    [ElementType.Select]: (id) => ({
        ...makeInputBase(ElementType.Select, id),
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
        autocomplete: undefined,
        placeholder: undefined,
    }),
    [ElementType.Spacer]: (id) => ({
        ...makeFormBase(ElementType.Spacer, id),
        height: '30',
    }),
    [ElementType.Table]: (id) => ({
        ...makeInputBase(ElementType.Table, id),
        label: 'Tabelle',
        fields: [
            {
                key: 'sp_1',
                label: 'Spalte 1',
                datatype: 'string',
                placeholder: 'Inhalt für Spalte 1',
                decimalPlaces: undefined,
                disabled: false,
                optional: false,
            },
            {
                key: 'sp_2',
                label: 'Spalte 2',
                datatype: 'string',
                placeholder: 'Inhalt für Spalte 2',
                decimalPlaces: undefined,
                disabled: false,
                optional: false,
            },
        ],
        maximumRows: undefined,
        minimumRequiredRows: undefined,
    }),
    [ElementType.Text]: (id) => ({
        ...makeInputBase(ElementType.Text, id),
        label: 'Text',
        autocomplete: undefined,
        placeholder: undefined,
        isMultiline: false,
        maxCharacters: undefined,
        minCharacters: undefined,
        pattern: undefined,
        suggestions: undefined,
    }),
    [ElementType.Time]: (id) => ({
        ...makeInputBase(ElementType.Time, id),
        label: 'Uhrzeit',
    }),
    [ElementType.IntroductionStep]: (id) => ({
        ...makeFormBase(ElementType.IntroductionStep, id),
        initiativeName: undefined,
        initiativeLogoLink: undefined,
        initiativeLink: undefined,
        teaserText: undefined,
        organization: undefined,
        eligiblePersons: undefined,
        supportingDocuments: undefined,
        documentsToAttach: undefined,
        expectedCosts: undefined,
    }),
    [ElementType.SubmitStep]: (id) => ({
        ...makeFormBase(ElementType.SubmitStep, id),
        textPreSubmit: 'Sie können Ihren Antrag nun verbindlich bei der zuständigen/bewirtschaftenden Stelle einreichen. Nach der Einreichung können Sie sich den Antrag für Ihre Unterlagen herunterladen oder zusenden lassen.',
        textPostSubmit: 'Sie können Ihren Antrag herunterladen oder sich per E-Mail zuschicken lassen. Wir empfehlen Ihnen, den Antrag anschließend zu Ihren Unterlagen zu nehmen.',
        textProcessingTime: undefined,
        documentsToReceive: undefined,
    }),
    [ElementType.SummaryStep]: (id) => ({
        ...makeFormBase(ElementType.SummaryStep, id),
    }),
    [ElementType.Image]: (id) => ({
        ...makeFormBase(ElementType.Image, id),
        alt: 'Beispiel-Grafik mit weißem Gover Logo auf blauem Hintergrund (bitte ersetzen)',
        src: `${AppInfo.mode == 'staff' ? '/staff' : ''}/assets/images/gover-beispiel-grafik.svg`,
        height: undefined,
        width: undefined,
        caption: undefined,
    }),
    [ElementType.SubmittedStep]: (id) => ({
        ...makeFormBase(ElementType.SubmittedStep, id),
    }),
    [ElementType.FileUpload]: (id) => ({
        ...makeInputBase(ElementType.FileUpload, id),
        label: 'Anlage(n)',
        extensions: ['pdf'],
        isMultifile: undefined,
        maxFiles: undefined,
        minFiles: undefined,
    }),
    [ElementType.DialogLayout]: (id) => ({}),
    [ElementType.StepperLayout]: (id) => ({}),
    [ElementType.ConfigLayout]: (id) => ({}),
    [ElementType.FunctionInput]: (id) => ({}),
    [ElementType.CodeInput]: (id) => ({}),
    [ElementType.RichTextInput]: (id) => ({}),
    [ElementType.UiDefinitionInput]: (id) => ({}),
    [ElementType.IdentityInput]: (id) => ({}),
    [ElementType.TabLayout]: (id) => ({}),
};

export function generateElementWithDefaultValues<T extends ElementType>(type: T): AnyElementType<T> {
    const id = generateElementIdForType(type);
    return elementConstructors[type](id) as AnyElementType<T>;
}
