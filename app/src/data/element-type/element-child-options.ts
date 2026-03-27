import {ElementType} from './element-type';

export enum ElementDisplayContext {
    CitizenFacing,
    StaffFacing,
}

const CitizenFacingBaseElements: ElementType[] = [
    // Content
    ElementType.Image,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Headline,
    ElementType.RichText,
    ElementType.Alert,

    // Inputs
    ElementType.Text,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
    ElementType.Spacer,
    ElementType.Table,
    ElementType.Time,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.MapPoint,
    ElementType.FileUpload,
    ElementType.ChipInput,

    // Layouts
    ElementType.ReplicatingContainer,
    ElementType.GroupLayout,
];


const StaffFacingBaseElements: ElementType[] = [
    // Content
    ElementType.Image,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Headline,
    ElementType.RichText,
    ElementType.Alert,

    // Inputs
    ElementType.Text,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
    ElementType.Spacer,
    ElementType.Table,
    ElementType.Time,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.MapPoint,
    ElementType.FileUpload,
    ElementType.ChipInput,
    ElementType.DataModelSelect,
    ElementType.DataObjectSelect,
    ElementType.RichTextInput,

    // Layouts
    ElementType.ReplicatingContainer,
    ElementType.GroupLayout,
];

export const ElementChildOptions: Record<ElementDisplayContext, Partial<Record<ElementType, ElementType[]>>> = {
    [ElementDisplayContext.CitizenFacing]: {
        [ElementType.GroupLayout]: CitizenFacingBaseElements,
        [ElementType.Step]: CitizenFacingBaseElements,
        [ElementType.FormLayout]: [
            ElementType.Step,
            ElementType.IntroductionStep,
            ElementType.SummaryStep,
            ElementType.SubmitStep,
        ],
        [ElementType.ReplicatingContainer]: CitizenFacingBaseElements,
    },
    [ElementDisplayContext.StaffFacing]: {
        [ElementType.GroupLayout]: StaffFacingBaseElements,
        [ElementType.Step]: StaffFacingBaseElements,
        [ElementType.ReplicatingContainer]: StaffFacingBaseElements,
    },
};
