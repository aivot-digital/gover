import {ElementType} from './element-type';
import {GridColType} from '@mui/x-data-grid/models/colDef/gridColType';


export const ElementToMuiDataGridType: Record<ElementType, GridColType | null> = {
    [ElementType.Alert]: null,
    [ElementType.Checkbox]: 'boolean',
    [ElementType.Image]: null,
    [ElementType.Container]: null,
    [ElementType.Date]: 'string',
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: 'string',
    [ElementType.Number]: 'number',
    [ElementType.ReplicatingContainer]: null,
    [ElementType.Richtext]: null,
    [ElementType.Radio]: 'string',
    [ElementType.Select]: 'string',
    [ElementType.Spacer]: null,
    [ElementType.Table]: null,
    [ElementType.Text]: 'string',
    [ElementType.Time]: 'string',
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: null,
};
