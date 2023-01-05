import {ElementType} from './element-type';
import {ElementTypesMap} from './element-types-map';

export const ElementDropzones: ElementTypesMap<string | null> = {
    [ElementType.Alert]: null,
    [ElementType.Checkbox]: null,
    [ElementType.Image]: null,
    [ElementType.Container]: 'base-dropzone',
    [ElementType.Date]: null,
    [ElementType.Step]: 'base-dropzone',
    [ElementType.Root]: null,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: null,
    [ElementType.Number]: null,
    [ElementType.ReplicatingContainer]: 'base-dropzone',
    [ElementType.Richtext]: null,
    [ElementType.Radio]: null,
    [ElementType.Select]: null,
    [ElementType.Spacer]: null,
    [ElementType.Table]: null,
    [ElementType.Text]: null,
    [ElementType.Time]: null,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
};
