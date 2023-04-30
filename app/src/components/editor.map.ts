import {ElementType} from '../data/element-type/element-type';
import {ElementTypesMap} from '../data/element-type/element-types-map';
import {RootComponentEditor} from './elements/root/root.component.editor';
import {StepComponentEditor} from './step/step.component.editor';
import {AlertComponentEditor} from './alert/alert.component.editor';
import {ContainerComponentEditor} from './container/container.component.editor';
import {HeadlineComponentEditor} from './headline/headline.component.editor';
import {RichtextComponentEditor} from './richtext/richtext.component.editor';
import {RadioFieldComponentEditor} from './radio-field/radio-field.component.editor';
import {SpacerComponentEditor} from './spacer/spacer.component.editor';
import {TextFieldComponentEditor} from './text-field/text-field.component.editor';
import {TimeFieldComponentEditor} from './time-field/time-field.component.editor';
import {TableFieldComponentEditor} from './table-field/table-field.component.editor';
import {SelectFieldComponentEditor} from './select-field/select-field.component.editor';
import {NumberFieldComponentEditor} from './number-field/number-field.component.editor';
import {DateFieldComponentEditor} from './date-field/date-field.component.editor';
import {CheckboxFieldComponentEditor} from './checkbox-field/checkbox-field.component.editor';
import {
    MultiCheckboxFieldComponentEditor
} from './multi-checkbox-field/multi-checkbox-field.component.editor';
import {ReplicatingContainerEditor} from './replicating-container/replicating-container.editor';
import {SummaryComponentEditor} from './summary/summary.component.editor';
import {
    GeneralInformationComponentEditor
} from './general-information/general-information.component.editor';
import {SubmitComponentEditor} from './submit/submit.component.editor';
import {BaseEditorProps} from './_lib/base-editor-props';
import {RootComponentEditorTabSchnittstellen} from './elements/root/root.component.editor-tab.schnittstellen';
import {RootComponentEditorTabLegal} from './elements/root/root.component.editor-tab.legal';
import {FunctionComponent} from 'react';
import {ImageEditor} from './image/image-editor';
import {RootComponentEditorTabPublish} from './elements/root/root.component.editor-tab.publish';
import {FileUploadEditor} from "./file-upload-field/file-upload.editor";

export type EditorType = FunctionComponent<BaseEditorProps<any>>;

export type EditorTypeSet = {
    root: EditorType;
    additionalTabs: {
        label: string;
        editor: EditorType;
    }[];
};

export function isEditorTypeSet(obj: any): obj is EditorTypeSet {
    return obj.root != null && obj.additionalTabs != null;
}

export const EditorMap: ElementTypesMap<EditorType | EditorTypeSet> = {
    [ElementType.Root]: {
        root: RootComponentEditor,
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
                label: 'Veröffentlichen',
                editor: RootComponentEditorTabPublish,
            },
        ],
    },
    [ElementType.Step]: StepComponentEditor,
    [ElementType.Alert]: AlertComponentEditor,
    [ElementType.Image]: ImageEditor,
    [ElementType.Container]: ContainerComponentEditor,
    [ElementType.Checkbox]: CheckboxFieldComponentEditor,
    [ElementType.Date]: DateFieldComponentEditor,
    [ElementType.Headline]: HeadlineComponentEditor,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentEditor,
    [ElementType.Number]: NumberFieldComponentEditor,
    [ElementType.ReplicatingContainer]: ReplicatingContainerEditor,
    [ElementType.Richtext]: RichtextComponentEditor,
    [ElementType.Radio]: RadioFieldComponentEditor,
    [ElementType.Select]: SelectFieldComponentEditor,
    [ElementType.Spacer]: SpacerComponentEditor,
    [ElementType.Table]: TableFieldComponentEditor,
    [ElementType.Text]: TextFieldComponentEditor,
    [ElementType.Time]: TimeFieldComponentEditor,
    [ElementType.IntroductionStep]: GeneralInformationComponentEditor,
    [ElementType.SummaryStep]: SummaryComponentEditor,
    [ElementType.SubmitStep]: SubmitComponentEditor,
    [ElementType.SubmittedStep]: SubmitComponentEditor,
    [ElementType.FileUpload]: FileUploadEditor,
}
