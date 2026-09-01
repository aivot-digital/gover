import {DialogProps} from '@mui/material/Dialog';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

export interface ImprintDialogProps extends DialogProps {
    form: FormLayoutElement;
    version?: ProcessVersionEntity;
    onHide: () => void;
    isListingPage?: boolean;
}
