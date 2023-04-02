import {ElementType} from '../data/element-type/element-type';
import {ElementTypesMap} from '../data/element-type/element-types-map';
import {BaseValidator} from '../validators/base-validator';
import {NumberFieldComponentValidator} from './number-field/number-field.component.validator';
import {RadioFieldComponentValidator} from './radio-field/radio-field.component.validator';
import {SelectFieldComponentValidator} from './select-field/select-field.component.validator';
import {TableFieldComponentValidator} from './table-field/table-field.component.validator';
import {TextFieldComponentValidator} from './text-field/text-field.component.validator';
import {TimeFieldComponentValidator} from './time-field/time-field.component.validator';
import {CheckboxFieldComponentValidator} from './checkbox-field/checkbox-field.component.validator';
import {DateFieldComponentValidator} from './date-field/date-field.component.validator';
import {MultiCheckboxFieldComponentValidator} from './multi-checkbox-field/multi-checkbox-field.component.validator';
import {ReplicatingContainerValidator} from './replicating-container/replicating-container.validator';
import {FileUploadValidator} from "./file-upload-field/file-upload.validator";

export const ValidatorMap: ElementTypesMap<BaseValidator<any> | null> = {
    [ElementType.Root]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.Image]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: new CheckboxFieldComponentValidator(),
    [ElementType.Date]: new DateFieldComponentValidator(),
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: new MultiCheckboxFieldComponentValidator(),
    [ElementType.Number]: new NumberFieldComponentValidator(),
    [ElementType.ReplicatingContainer]: new ReplicatingContainerValidator(),
    [ElementType.Richtext]: null,
    [ElementType.Radio]: new RadioFieldComponentValidator(),
    [ElementType.Select]: new SelectFieldComponentValidator(),
    [ElementType.Spacer]: null,
    [ElementType.Table]: new TableFieldComponentValidator(),
    [ElementType.Text]: new TextFieldComponentValidator(),
    [ElementType.Time]: new TimeFieldComponentValidator(),
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: new FileUploadValidator(),
}
