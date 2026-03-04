import {ElementType} from '../data/element-type/element-type';
import {type ElementTypesMap} from '../data/element-type/element-types-map';
import {TextFieldEditor} from './text-field-editor';
import {type BaseEditor} from './base-editor';
import {RootComponentEditor} from '../components/root/root.component.editor';
import {RootComponentEditorTabSchnittstellen} from '../components/root/root.component.editor-tab.schnittstellen';
import {StepComponentEditor} from '../components/step/step.component.editor';
import {NumberFieldEditor} from './number-field-editor';
import {AlertEditor} from './alert-editor';
import {SelectFieldEditor} from './select-field-editor';
import {DateFieldEditor} from './date-field-editor';
import {RootComponentEditorTabLegal} from '../components/root/root.component.editor-tab.legal';
import {ImageEditor} from '../components/image/image-editor';
import {
    MultiCheckboxFieldComponentEditor
} from '../components/multi-checkbox-field/multi-checkbox-field.component.editor';
import {HeadlineComponentEditor} from '../components/headline/headline.component.editor';
import {ReplicatingContainerEditor} from '../components/replicating-container/replicating-container.editor';
import {RichtextComponentEditor} from '../components/richtext/richtext.component.editor';
import {RadioFieldComponentEditor} from '../components/radio-field/radio-field.component.editor';
import {SpacerComponentEditor} from '../components/spacer/spacer.component.editor';
import {TableFieldComponentEditor} from '../components/table-field/table-field.component.editor';
import {
    GeneralInformationComponentEditor
} from '../components/general-information/general-information.component.editor';
import {SubmitComponentEditor} from '../components/submit/submit.component.editor';
import {FileUploadEditor} from '../components/file-upload-field/file-upload.editor';
import {ContainerEditor} from './container-editor';
import {RootComponentEditorTabPayment} from '../components/root/root.component.editor-tab.payment';
import {ChipInputFieldEditor} from './chip-input-field-editor';
import {DateTimeFieldEditor} from './date-time-field-editor';
import {DateRangeFieldEditor} from './date-range-field-editor';
import {TimeRangeFieldEditor} from './time-range-field-editor';
import {DateTimeRangeFieldEditor} from './date-time-range-field-editor';
import {TimeFieldEditor} from './time-field-editor';
import {MapPointFieldEditor} from './map-point-field-editor';
import {DomainUserSelectFieldEditor} from './domain-user-select-field-editor';
import {AssignmentContextFieldEditor} from './assignment-context-field-editor';
import {DataModelSelectFieldEditor} from './data-model-select-field-editor';
import {DataObjectSelectFieldEditor} from './data-object-select-field-editor';
import {RichTextInputFieldEditor} from './rich-text-input-field-editor';

export interface EditorTab {
    label: string;
    editor: BaseEditor<any, any>;
}

export interface EditorSet {
    default: BaseEditor<any, any>;
    additionalTabs?: EditorTab[];
}

export const editors: ElementTypesMap<EditorSet | null> = {
    [ElementType.FormLayout]: {
        default: RootComponentEditor,
        additionalTabs: [
            {
                label: 'Schnittstellen',
                editor: RootComponentEditorTabSchnittstellen,
            },
            {
                label: 'Rechtliches',
                editor: RootComponentEditorTabLegal,
            },
            {
                label: 'E-Payment',
                editor: RootComponentEditorTabPayment,
            },
        ],
    },
    [ElementType.Step]: {
        default: StepComponentEditor,
    },
    [ElementType.Alert]: {
        default: AlertEditor,
    },
    [ElementType.Image]: {
        default: ImageEditor,
    },
    [ElementType.GroupLayout]: {
        default: ContainerEditor,
    },
    [ElementType.Checkbox]: null,
    [ElementType.Date]: {
        default: DateFieldEditor,
    },
    [ElementType.Headline]: {
        default: HeadlineComponentEditor,
    },
    [ElementType.MultiCheckbox]: {
        default: MultiCheckboxFieldComponentEditor,
    },
    [ElementType.Number]: {
        default: NumberFieldEditor,
    },
    [ElementType.ReplicatingContainer]: {
        default: ReplicatingContainerEditor,
    },
    [ElementType.RichText]: {
        default: RichtextComponentEditor,
    },
    [ElementType.Radio]: {
        default: RadioFieldComponentEditor,
    },
    [ElementType.Select]: {
        default: SelectFieldEditor,
    },
    [ElementType.Spacer]: {
        default: SpacerComponentEditor,
    },
    [ElementType.Table]: {
        default: TableFieldComponentEditor,
    },
    [ElementType.Text]: {
        default: TextFieldEditor,
    },
    [ElementType.Time]: {
        default: TimeFieldEditor,
    },
    [ElementType.IntroductionStep]: {
        default: GeneralInformationComponentEditor,
    },
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: {
        default: SubmitComponentEditor,
    },
    [ElementType.SubmittedStep]: {
        default: SubmitComponentEditor,
    },
    [ElementType.FileUpload]: {
        default: FileUploadEditor,
    },
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: null,
    [ElementType.CodeInput]: null,
    [ElementType.RichTextInput]: {
        default: RichTextInputFieldEditor,
    },
    [ElementType.UiDefinitionInput]: null,
    [ElementType.IdentityInput]: null,
    [ElementType.TabLayout]: null,
    [ElementType.ChipInput]: {
        default: ChipInputFieldEditor,
    },
    [ElementType.DateTime]: {
        default: DateTimeFieldEditor,
    },
    [ElementType.DateRange]: {
        default: DateRangeFieldEditor,
    },
    [ElementType.TimeRange]: {
        default: TimeRangeFieldEditor,
    },
    [ElementType.DateTimeRange]: {
        default: DateTimeRangeFieldEditor,
    },
    [ElementType.MapPoint]: {
        default: MapPointFieldEditor,
    },
    [ElementType.DomainAndUserSelect]: {
        default: DomainUserSelectFieldEditor,
    },
    [ElementType.AssignmentContext]: {
        default: AssignmentContextFieldEditor,
    },
    [ElementType.DataModelSelect]: {
        default: DataModelSelectFieldEditor,
    },
    [ElementType.DataObjectSelect]: {
        default: DataObjectSelectFieldEditor,
    },
};
