import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {type Form as Application, FormListProjection} from '../../../models/entities/form';

export interface AddApplicationDialogProps extends DialogProps {
    applicationToBaseOn?: Application;
    mode: 'new' | 'new-version' | 'clone' | 'import';
    existingApplications: FormListProjection[];
    onClose: () => void;
    onSave: (application: Application, navigateToEditAfterwards: boolean) => void;
}
