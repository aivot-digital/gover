import {DialogProps} from '@mui/material/Dialog';
import {FormLayoutElement} from '../../models/elements/form-layout-element';

export interface PrivacyDialogProps extends DialogProps {
    form: FormLayoutElement;
    onHide: () => void;
    isListingPage?: boolean;
}
