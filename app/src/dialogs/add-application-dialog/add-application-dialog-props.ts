import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Application} from '../../models/application';

export interface AddApplicationDialogProps extends DialogProps {
    applications: Application[];
    onHide: () => void;
    onSave: (application: Application, navigateToEditAfterwards: boolean) => void;
}
