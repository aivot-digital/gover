import {Dialog} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';

export interface SecretCreateDialogProps {
    open: boolean;
    onClose: () => void;
    onCreate: () => void;
}
// TODO: Implement SecretCreateDialog
export function SecretCreateDialog(props: SecretCreateDialogProps) {
    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Geheimnis anlegen
            </DialogTitleWithClose>
        </Dialog>
    );
}