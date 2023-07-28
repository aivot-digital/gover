import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {type Application} from '../../../models/entities/application';
import {type ListApplication} from '../../../models/entities/list-application';

export interface AddApplicationDialogProps extends DialogProps {
    applicationToBaseOn?: Application;
    mode: 'new' | 'new-version' | 'clone' | 'import';
    existingApplications: ListApplication[];
    onClose: () => void;
    onSave: (application: Application, navigateToEditAfterwards: boolean) => void;
}
