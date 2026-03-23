import {DialogProps} from '@mui/material/Dialog';

export interface PrivacyDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
