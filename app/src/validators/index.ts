import {ElementType} from '../data/element-type/element-type';
import {TextFieldValidator} from './text-field-validator';
import {type BaseValidator} from './base-validator';
import {CheckboxFieldValidator} from './checkbox-field-validator';
import {DateFieldValidator} from './date-field-validator';
import {NumberFieldValidator} from './number-field-validator';
import {SelectFieldValidator} from './select-field-validator';
import {
    MultiCheckboxFieldComponentValidator
} from '../components/multi-checkbox-field/multi-checkbox-field.component.validator';
import {ReplicatingContainerValidator} from '../components/replicating-container/replicating-container.validator';
import {TableFieldComponentValidator} from '../components/table-field/table-field.component.validator';
import {TimeFieldComponentValidator} from '../components/time-field/time-field.component.validator';
import {FileUploadValidator} from '../components/file-upload-field/file-upload.validator';
import {RadioFieldValidator} from './radio-field-validator';

const validators: Record<ElementType, BaseValidator<any> | null> = {
    [ElementType.Root]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.Image]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: new CheckboxFieldValidator(),
    [ElementType.Date]: new DateFieldValidator(),
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: new MultiCheckboxFieldComponentValidator(),
    [ElementType.Number]: new NumberFieldValidator(),
    [ElementType.ReplicatingContainer]: new ReplicatingContainerValidator(),
    [ElementType.Richtext]: null,
    [ElementType.Radio]: new RadioFieldValidator(),
    [ElementType.Select]: new SelectFieldValidator(),
    [ElementType.Spacer]: null,
    [ElementType.Table]: new TableFieldComponentValidator(),
    [ElementType.Text]: new TextFieldValidator(),
    [ElementType.Time]: new TimeFieldComponentValidator(),
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: new FileUploadValidator(),
};

export default validators;
