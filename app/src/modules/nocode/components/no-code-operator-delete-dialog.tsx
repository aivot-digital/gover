import {useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';

interface NoCodeOperatorDeleteDialogProps {
    open: boolean;
    name: string;
    hasNotes: boolean;
    childrenOptions: { value: string; label: string; warning?: string | null }[];
    onClose: () => void;
    onExited: () => void;
    onDelete: () => void;
    onKeep: (id: string) => void;
}

export function NoCodeOperatorDeleteDialog(props: NoCodeOperatorDeleteDialogProps) {
    const hasChildren = props.childrenOptions.length > 0;
    const [selected, setSelected] = useState(props.childrenOptions.length === 1 ? props.childrenOptions[0].value : '');
    const warning = props.childrenOptions.find((child) => child.value === selected)?.warning;
    if (!hasChildren) {
        return <ConfirmDialog title="Ausdruck löschen?"
                              onConfirm={props.open ? props.onDelete : undefined}
                              onCancel={props.onClose}
                              onExited={props.onExited}
                              disableRestoreFocus
                              isDestructive
                              confirmButtonText="Ausdruck löschen">
            <Typography sx={{mb: props.hasNotes ? 2 : 0}}>Möchten Sie diesen Ausdruck wirklich löschen?</Typography>
            {props.hasNotes && <Alert severity="warning">Mit dem Ausdruck werden auch seine Notizen gelöscht.</Alert>}
        </ConfirmDialog>;
    }

    // Keeping a child and deleting the subtree are separate actions; ConfirmDialog offers only one confirmation.
    return <Dialog open={props.open}
                   onClose={props.onClose}
                   fullWidth
                   maxWidth="sm"
                   disableRestoreFocus
                   slotProps={{transition: {onExited: props.onExited}}}>
        <DialogTitleWithClose onClose={props.onClose}>Funktion entfernen?</DialogTitleWithClose>
        <DialogContent sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
        }}>
            <Typography>Sie können „{props.name}“ mit allen enthaltenen Werten löschen oder durch einen direkten
                        Teilausdruck ersetzen. Beim Ersetzen werden die übrigen Teilausdrücke gelöscht.</Typography>
            <SelectFieldComponent label="Teilausdruck erhalten"
                                  value={selected}
                                  onChange={(value) => setSelected(value ?? '')}
                                  options={props.childrenOptions}
                                  emptyOptionLabel="Teilausdruck auswählen"/>
            {props.hasNotes && <Alert severity="warning">Bei „Alles löschen“ werden auch die Notizen zum Ausdruck gelöscht. Beim Ersetzen durch einen Teilausdruck bleiben sie erhalten.</Alert>}
            {warning && <Alert severity="warning">{warning}</Alert>}
        </DialogContent>
        <DialogActions sx={{flexWrap: 'wrap'}}>
            <Button variant="contained"
                    disabled={!selected}
                    onClick={() => props.onKeep(selected)}>Durch Teilausdruck ersetzen</Button>
            <Button color="error"
                    onClick={props.onDelete}>Alles löschen</Button>
            <Button onClick={props.onClose}>Abbrechen</Button>
        </DialogActions>
    </Dialog>;
}
