import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField} from '@mui/material';
import React, {useCallback, useState} from 'react';

interface AddPresetDialogProps extends DialogProps {
    onSavePreset: (newTitle: string) => void;
    onClose: () => void;
}

export function AddPresetDialog(props: AddPresetDialogProps) {
    const {
        onSavePreset,
        onClose,
        ...passTroughProps
    } = props;

    const [newPresetTitle, setNewPresetTitle] = useState('');

    const handleClosePreset = useCallback(() => {
        setNewPresetTitle('');
        onClose();
    }, [onClose]);

    const handleSavePreset = useCallback(() => {
        onSavePreset(newPresetTitle);
        setNewPresetTitle('');
    }, [newPresetTitle, onSavePreset]);

    return (
        <Dialog
            {...passTroughProps}
            onClose={handleClosePreset}
        >
            <DialogTitle>
                Neue Vorlage anlegen
            </DialogTitle>
            <DialogContent>
                <TextField
                    label="Titel der Vorlage"
                    value={newPresetTitle}
                    onChange={event => setNewPresetTitle(event.target.value ?? '')}
                    onBlur={() => setNewPresetTitle(newPresetTitle.trim())}
                />
            </DialogContent>
            <DialogActions>
                {/*TODO: adjust buttons to fit our left-aligned layout*/}
                <Button onClick={handleClosePreset}>
                    Abbrechen
                </Button>
                <Button onClick={handleSavePreset}>
                    Speichern
                </Button>
            </DialogActions>
        </Dialog>
    );
}
