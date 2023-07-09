import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Application} from '../../../models/entities/application';
import {ListApplication} from "../../../models/entities/list-application";

export interface AddApplicationDialogProps extends DialogProps {
    applicationToBaseOn?: Application;
    mode: 'new' | 'new-version' | 'clone' | 'import';
    existingApplications: ListApplication[];
    onClose: () => void;
    onSave: (application: Application, navigateToEditAfterwards: boolean) => void;
}
