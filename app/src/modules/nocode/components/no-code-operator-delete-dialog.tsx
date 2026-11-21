import {useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';

interface NoCodeOperatorDeleteDialogProps {
    open: boolean;
    name: string;
    childrenOptions: { value: string; label: string; warning?: string | null }[];
    onClose: () => void;
    onExited: () => void;
    onDelete: () => void;
    onKeep: (id: string) => void;
}

export function NoCodeOperatorDeleteDialog(props: NoCodeOperatorDeleteDialogProps) {
    const [selected, setSelected] = useState(props.childrenOptions.length === 1 ? props.childrenOptions[0].value : '');
    const warning = props.childrenOptions.find((child) => child.value === selected)?.warning;
    return <Dialog open={props.open}
                   onClose={props.onClose}
                   fullWidth
                   maxWidth="sm"
                   disableRestoreFocus
                   slotProps={{transition: {onExited: props.onExited}}}>
        <DialogTitleWithClose onClose={props.onClose}>Operator entfernen?</DialogTitleWithClose>
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
