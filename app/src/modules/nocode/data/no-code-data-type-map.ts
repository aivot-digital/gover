import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeDataType} from '../../../data/no-code-data-type';

export const NoCodeDataTypeMap: Record<ElementType, NoCodeDataType> = {
    [ElementType.Alert]: NoCodeDataType.Runtime,
    [ElementType.Checkbox]: NoCodeDataType.Runtime,
    [ElementType.Image]: NoCodeDataType.Runtime,
    [ElementType.Container]: NoCodeDataType.Runtime,
    [ElementType.Date]: NoCodeDataType.Date,
    [ElementType.Step]: NoCodeDataType.Runtime,
    [ElementType.Root]: NoCodeDataType.Runtime,
    [ElementType.Headline]: NoCodeDataType.Runtime,
    [ElementType.MultiCheckbox]: NoCodeDataType.List,
    [ElementType.Number]: NoCodeDataType.Number,
    [ElementType.ReplicatingContainer]: NoCodeDataType.List,
    [ElementType.Richtext]: NoCodeDataType.Runtime,
    [ElementType.Radio]: NoCodeDataType.String,
    [ElementType.Select]: NoCodeDataType.String,
    [ElementType.Spacer]: NoCodeDataType.Runtime,
    [ElementType.Table]: NoCodeDataType.List,
    [ElementType.Text]: NoCodeDataType.String,
    [ElementType.Time]: NoCodeDataType.String,
    [ElementType.IntroductionStep]: NoCodeDataType.Runtime,
    [ElementType.SummaryStep]: NoCodeDataType.Runtime,
    [ElementType.SubmitStep]: NoCodeDataType.Runtime,
    [ElementType.SubmittedStep]: NoCodeDataType.Runtime,
    [ElementType.FileUpload]: NoCodeDataType.List,
}