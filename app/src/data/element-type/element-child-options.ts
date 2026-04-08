import {ElementType} from './element-type';

export enum ElementDisplayContext {
    CitizenFacing = 'CitizenFacing',
    StaffFacing = 'StaffFacing',
}

const CitizenFacingBaseElements: ElementType[] = [
    // Content
    ElementType.Image,
    ElementType.Headline,
    ElementType.RichText,
    ElementType.Alert,
    ElementType.Spacer,

    // Inputs
    ElementType.Text,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
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
    ElementType.SummaryLayout,
];

const StaffFacingBaseElements: ElementType[] = [
    // Content
    ElementType.Image,
    ElementType.Headline,
    ElementType.RichText,
    ElementType.Alert,
    ElementType.Spacer,

    // Inputs
    ElementType.Text,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
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
    ElementType.SummaryLayout,
];

// Summaries can only contain inputs
const CitizenFacingSummaryChildElements: ElementType[] = [
    ElementType.Text,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
    ElementType.Table,
    ElementType.Time,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.MapPoint,
    ElementType.FileUpload,
    ElementType.ChipInput,
];

const StaffFacingSummaryChildElements: ElementType[] = [
    ElementType.Text,
    ElementType.Checkbox,
    ElementType.Date,
    ElementType.Number,
    ElementType.Radio,
    ElementType.Select,
    ElementType.MultiCheckbox,
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
        [ElementType.SummaryLayout]: CitizenFacingSummaryChildElements,
    },
    [ElementDisplayContext.StaffFacing]: {
        [ElementType.GroupLayout]: StaffFacingBaseElements,
        [ElementType.Step]: StaffFacingBaseElements,
        [ElementType.ReplicatingContainer]: StaffFacingBaseElements,
        [ElementType.SummaryLayout]: StaffFacingSummaryChildElements,
    },
};
