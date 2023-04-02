import {ElementType} from '../data/element-type/element-type';
import {ElementTypesMap} from '../data/element-type/element-types-map';
import {TextFieldComponentSummary} from './text-field/text-field.component.summary';
import {HeadlineComponentSummary} from './headline/headline.component.summary';
import {TimeFieldComponentSummary} from './time-field/time-field.component.summary';
import {CheckboxFieldComponentSummary} from './checkbox-field/checkbox-field.component.summary';
import {StepComponentSummary} from './step/step.component.summary';
import {RadioFieldComponentSummary} from './radio-field/radio-field.component.summary';
import {SelectFieldComponentSummary} from './select-field/select-field.component.summary';
import {DateFieldComponentSummary} from './date-field/date-field.component.summary';
import {
    MultiCheckboxFieldComponentSummary
} from './multi-checkbox-field/multi-checkbox-field.component.summary';
import {TableFieldComponentSummary} from './table-field/table-field.component.summary';
import {NumberComponentSummary} from './number-field/number.component.summary';
import {ReplicationContainerSummary} from './replicating-container/replication-container.summary';
import {FileUploadSummary} from "./file-upload-field/file-upload.summary";

export const SummaryMap: ElementTypesMap<any | null> = {
    [ElementType.Root]: null,
    [ElementType.Step]: StepComponentSummary,
    [ElementType.Alert]: null,
    [ElementType.Image]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: CheckboxFieldComponentSummary,
    [ElementType.Date]: DateFieldComponentSummary,
    [ElementType.Headline]: HeadlineComponentSummary,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentSummary,
    [ElementType.Number]: NumberComponentSummary,
    [ElementType.ReplicatingContainer]: ReplicationContainerSummary,
    [ElementType.Richtext]: null,
    [ElementType.Radio]: RadioFieldComponentSummary,
    [ElementType.Select]: SelectFieldComponentSummary,
    [ElementType.Spacer]: null,
    [ElementType.Table]: TableFieldComponentSummary,
    [ElementType.Text]: TextFieldComponentSummary,
    [ElementType.Time]: TimeFieldComponentSummary,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: FileUploadSummary,
}
