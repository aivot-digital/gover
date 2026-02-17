import {ElementType} from '../data/element-type/element-type';
import {ElementTypesMap} from '../data/element-type/element-types-map';
import {BaseSummary} from './base-summary';
import {TextFieldSummary} from './text-field-summary';
import {CheckboxFieldSummary} from './checkbox-field-summary';
import {DateFieldSummary} from './date-field-summary';
import {
    MultiCheckboxFieldComponentSummary
} from '../components/multi-checkbox-field/multi-checkbox-field.component.summary';
import {NumberSummary} from './number-summary';
import {SelectFieldSummary} from './select-field-summary';
import {ReplicationContainerSummary} from '../components/replicating-container/replication-container.summary';
import {RadioFieldComponentSummary} from '../components/radio-field/radio-field.component.summary';
import {TableFieldComponentSummary} from '../components/table-field/table-field.component.summary';
import {TimeFieldComponentSummary} from '../components/time-field/time-field.component.summary';
import {FileUploadSummary} from '../components/file-upload-field/file-upload.summary';
import {StepComponentSummary} from '../components/step/step.component.summary';
import {GroupSummary} from './group-summary';
import {RootSummary} from './root-summary';

export const summaries: ElementTypesMap<BaseSummary<any, any> | null> = {
    [ElementType.FormLayout]: RootSummary,
    [ElementType.Step]: StepComponentSummary,
    [ElementType.Alert]: null,
    [ElementType.Image]: null,
    [ElementType.GroupLayout]: GroupSummary,
    [ElementType.Checkbox]: CheckboxFieldSummary,
    [ElementType.Date]: DateFieldSummary,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentSummary,
    [ElementType.Number]: NumberSummary,
    [ElementType.ReplicatingContainer]: ReplicationContainerSummary,
    [ElementType.RichText]: null,
    [ElementType.Radio]: RadioFieldComponentSummary,
    [ElementType.Select]: SelectFieldSummary,
    [ElementType.Spacer]: null,
    [ElementType.Table]: TableFieldComponentSummary,
    [ElementType.Text]: TextFieldSummary,
    [ElementType.Time]: TimeFieldComponentSummary,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: FileUploadSummary,
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: null,
    [ElementType.CodeInput]: null,
    [ElementType.RichTextInput]: null,
    [ElementType.UiDefinitionInput]: null,
    [ElementType.IdentityInput]: null,
    [ElementType.TabLayout]: null,
};

