import {useEffect, useId, useRef, useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';

interface PersonAssignmentDialogProps {
    title: string;
    description: string;
    assignedUserId: string | null;
    loadOptions: () => Promise<SelectFieldComponentOption<string>[]>;
    onSave: (userId: string | null) => Promise<void>;
    onClose: () => void;
}

// Mount when opening so every session starts with fresh candidates and the current assignment.
export function PersonAssignmentDialog({
    title,
    description,
    assignedUserId,
    loadOptions,
    onSave,
    onClose,
}: PersonAssignmentDialogProps) {
    const titleId = useId();
    const descriptionId = useId();
    const [selectedId, setSelectedId] = useState(assignedUserId);
    const [options, setOptions] = useState<SelectFieldComponentOption<string>[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadFailed, setLoadFailed] = useState(false);
    const [saveError, setSaveError] = useState<string>();
    const [saving, setSaving] = useState(false);
    const [reload, setReload] = useState(0);
    const submitting = useRef(false);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setLoadFailed(false);
        loadOptions()
            .then((result) => {
                if (!cancelled) setOptions(result);
            })
            .catch(() => {
                if (!cancelled) setLoadFailed(true);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [loadOptions, reload]);

    const canSaveSelection =
        !loading &&
        !loadFailed &&
        selectedId != null &&
        selectedId !== assignedUserId &&
        options.some((option) => option.value === selectedId);

    const save = async (userId: string | null) => {
        if (submitting.current || (userId == null ? assignedUserId == null : !canSaveSelection)) return;
        submitting.current = true;
        setSaving(true);
        setSaveError(undefined);
        try {
            await onSave(userId);
            onClose();
        } catch {
            setSaveError(
                userId == null
                    ? 'Die Zuweisung konnte nicht aufgehoben werden.'
                    : 'Die Zuweisung konnte nicht gespeichert werden. Prüfen Sie, ob die ausgewählte Person weiterhin verfügbar ist.',
            );
        } finally {
            submitting.current = false;
            setSaving(false);
        }
    };

    return (
        <Dialog
            open
            fullWidth
            maxWidth="sm"
            onClose={() => {
                if (!submitting.current) onClose();
            }}
            aria-labelledby={titleId}
            aria-describedby={descriptionId}
        >
            <DialogTitle id={titleId}>{title}</DialogTitle>
            <DialogContent>
                <Typography
                    id={descriptionId}
                    sx={{mb: 3}}
                >
                    {description}
                </Typography>
                {loadFailed ? (
                    <Alert
                        severity="error"
                        action={<Button onClick={() => setReload((value) => value + 1)}>Erneut laden</Button>}
                    >
                        Die verfügbaren Personen konnten nicht geladen werden.
                    </Alert>
                ) : (
                    <SelectFieldComponent<string>
                        label="Zugewiesen an"
                        required
                        value={selectedId}
                        onChange={(value) => {
                            setSelectedId(value);
                            setSaveError(undefined);
                        }}
                        options={options}
                        busy={loading || saving}
                        disabled={loading || saving}
                        presentation={SelectFieldPresentation.Combobox}
                        emptyStatePlaceholder="Keine passenden Personen verfügbar"
                    />
                )}
                {!loading && !loadFailed && options.length === 0 && (
                    <Alert
                        severity="info"
                        sx={{mt: 2}}
                    >
                        Derzeit erfüllt keine Person die Voraussetzungen für eine Zuweisung.
                    </Alert>
                )}
                {!loading &&
                    !loadFailed &&
                    assignedUserId != null &&
                    !options.some((option) => option.value === assignedUserId) && (
                        <Alert
                            severity="info"
                            sx={{mt: 2}}
                        >
                            Die bisher zugewiesene Person steht nicht mehr zur Auswahl. Sie können eine andere Person
                            auswählen oder die Zuweisung aufheben.
                        </Alert>
                    )}
                {saveError && (
                    <Alert
                        severity="error"
                        sx={{mt: 2}}
                    >
                        {saveError}
                    </Alert>
                )}
            </DialogContent>
            <DialogActions
                disableSpacing
                sx={{
                    justifyContent: 'flex-start',
                    gap: 1,
                }}
            >
                <Button
                    variant="contained"
                    onClick={() => void save(selectedId)}
                    disabled={saving || !canSaveSelection}
                >
                    Zuweisung speichern
                </Button>
                <Button
                    onClick={onClose}
                    disabled={saving}
                >
                    Abbrechen
                </Button>
                <Button
                    sx={{ml: 'auto'}}
                    color="error"
                    onClick={() => void save(null)}
                    disabled={saving || assignedUserId == null}
                >
                    Zuweisung aufheben
                </Button>
            </DialogActions>
        </Dialog>
    );
}
