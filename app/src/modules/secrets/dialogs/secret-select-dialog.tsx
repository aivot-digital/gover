import {SearchBaseDialog} from '../../../dialogs/search-base-dialog/search-base-dialog';

export interface SecretSelectDialogProps {
    open: boolean;
    onClose: () => void;
}
// TODO: Implement SecretSelectDialog
export function SecretSelectDialog(props: SecretSelectDialogProps) {
    return (
        <SearchBaseDialog
            open={props.open}
            onClose={props.onClose}
            title="Geheimnis auswählen"
            tabs={[]}
        />
    );
}