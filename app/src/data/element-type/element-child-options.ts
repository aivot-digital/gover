import {ElementType} from './element-type';
import {ElementTypesMap} from './element-types-map';

const BaseComponents = [
    ElementType.Image,
    ElementType.Container,

    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Headline,
    ElementType.MultiCheckbox,
    ElementType.Text,
    ElementType.Number,
    ElementType.Richtext,
    ElementType.Radio,
    ElementType.Select,
    ElementType.Spacer,
    ElementType.Table,
    ElementType.Time,
    ElementType.Alert,

    ElementType.ReplicatingContainer,
];

export const ElementChildOptions: ElementTypesMap<ElementType[] | null> = {
    [ElementType.Alert]: null,
    [ElementType.Image]: null,
    [ElementType.Container]: BaseComponents,
    [ElementType.Step]: BaseComponents,
    [ElementType.Root]: [
        ElementType.Step,
    ],
    [ElementType.Checkbox]: null,
    [ElementType.Date]: null,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: null,
    [ElementType.Number]: null,
    [ElementType.ReplicatingContainer]: BaseComponents,
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
