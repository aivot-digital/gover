import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Application} from '../../models/application';

export interface ImportApplicationDialogProps extends DialogProps {
    applications: Application[];
    onHide: () => void;
    onImport: (application: Application, navigateToEditAfterwards: boolean) => void;
}
