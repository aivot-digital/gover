import {DialogProps} from '@mui/material/Dialog';
import {FormLayoutElement} from '../../models/elements/form-layout-element';

export interface HelpDialogProps extends DialogProps {
    form: FormLayoutElement;
    onHide: () => void;
}
