import {DialogProps} from '@mui/material/Dialog';

export interface AccessibilityDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
