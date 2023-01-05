import {ElementType} from './element-type';
import {ElementTypesMap} from './element-types-map';

export const ElementIsInput: ElementTypesMap<boolean> = {
    [ElementType.Alert]: false,
    [ElementType.Checkbox]: true,
    [ElementType.Image]: false,
    [ElementType.Container]: false,
    [ElementType.Date]: true,
    [ElementType.Step]: false,
    [ElementType.Root]: false,
    [ElementType.Headline]: false,
    [ElementType.MultiCheckbox]: true,
    [ElementType.Number]: true,
    [ElementType.ReplicatingContainer]: true,
    [ElementType.Richtext]: false,
    [ElementType.Radio]: true,
    [ElementType.Select]: true,
    [ElementType.Spacer]: false,
    [ElementType.Table]: true,
    [ElementType.Text]: true,
    [ElementType.Time]: true,
    [ElementType.IntroductionStep]: false,
    [ElementType.SummaryStep]: false,
    [ElementType.SubmitStep]: false,
    [ElementType.SubmittedStep]: false,
};
