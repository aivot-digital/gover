import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Application} from '../../models/application';

export interface CloneApplicationDialogProps extends DialogProps {
    applications: Application[];
    onHide: () => void;
    onSave: (application: Application, navigateToEditAfterwards: boolean) => void;
    source: Application;
}
