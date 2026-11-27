import {DialogProps} from '@mui/material/Dialog';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

export interface HelpDialogProps extends DialogProps {
    form?: FormLayoutElement;
    version?: Pick<ProcessVersionEntity, 'publicTitle' | 'technicalSupportDepartmentId' | 'legalSupportDepartmentId'>;
    technicalSupportDepartmentId?: number | null;
    legalSupportDepartmentId?: number | null;
    onHide: () => void;
}
