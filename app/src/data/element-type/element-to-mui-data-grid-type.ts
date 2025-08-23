import {ElementType} from './element-type';
import {GridColType} from '@mui/x-data-grid/models/colDef/gridColType';


export const ElementToMuiDataGridType: Record<ElementType, GridColType | null> = {
    [ElementType.Alert]: null,
    [ElementType.Checkbox]: 'boolean',
    [ElementType.Image]: null,
    [ElementType.Container]: null,
    [ElementType.Date]: 'date',
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: null,
    [ElementType.Number]: 'number',
    [ElementType.ReplicatingContainer]: null,
    [ElementType.Richtext]: null,
    [ElementType.Radio]: null,
    [ElementType.Select]: null,
    [ElementType.Spacer]: null,
    [ElementType.Table]: null,
    [ElementType.Text]: 'string',
    [ElementType.Time]: null,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: null,
};
