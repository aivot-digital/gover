import React, {type ReactNode, useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, Stack, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {RadioFieldComponent} from '../../components/radio-field/radio-field-component';
import {type MergeChoice, type MergeChoices} from '../../utils/three-way-merge';
import {getLiteralAuthoredValue} from '../../models/element-data';
import {type AuthoredInputValue} from '../../models/input-mode';

export interface MergeConflictDialogConflict {
    id: string;
    label: string;
    mine: unknown;
    theirs: unknown;
}

/**
 * `restore` offers unsaved changes from an earlier visit of the editor, `save` resolves a save that a newer stored
 * state rejected.
 */
export type MergeConflictDialogMode = 'restore' | 'save';

interface MergeConflictDialogProps {
    open: boolean;
    mode: MergeConflictDialogMode;
    /** Whether a newer state was stored since the changes were started. */
    storedStateChanged: boolean;
    conflicts: MergeConflictDialogConflict[];
    onApply: (choices: MergeChoices) => void;
    onDiscard: () => void;
    /** Only in the `save` mode: keeps editing without saving. */
    onCancel?: () => void;
}

const PREVIEW_MAX_LENGTH = 80;

export function MergeConflictDialog(props: MergeConflictDialogProps): ReactNode {
    const {open, mode, storedStateChanged, conflicts, onApply, onDiscard, onCancel} = props;
    const [choices, setChoices] = useState<MergeChoices>({});

    useEffect(() => {
        if (open) {
            setChoices(Object.fromEntries(conflicts.map((conflict) => [conflict.id, 'mine' as MergeChoice])));
        }
    }, [open, conflicts]);

    const hasConflicts = conflicts.length > 0;
    const title = mode === 'restore' ? 'Ungespeicherte Änderungen wiederherstellen?' : 'Neuerer Stand gespeichert';

    return (
        <Dialog
            open={open}
            // Restoring needs a decision, otherwise the draft would be overwritten by the next edit unnoticed.
            onClose={mode === 'save' ? onCancel : undefined}
            fullWidth={true}
            maxWidth={hasConflicts ? 'md' : 'sm'}
        >
            <DialogTitleWithClose onClose={mode === 'save' ? onCancel : undefined}>
                {title}
            </DialogTitleWithClose>

            <DialogContent tabIndex={0}>
                <Typography>
                    {
                        mode === 'restore' ?
                            'Aus Ihrer letzten Bearbeitung liegen ungespeicherte Änderungen vor. Sie können diese wiederherstellen und die Bearbeitung fortsetzen.' :
                            'Während Sie Ihre Änderungen vorgenommen haben, wurde an anderer Stelle ein neuerer Stand gespeichert.'
                    }
                </Typography>

                {
                    mode === 'restore' && storedStateChanged && !hasConflicts &&
                    <Typography sx={{mt: 2}}>
                        Zwischenzeitlich wurde ein neuerer Stand gespeichert. Ihre Änderungen können mit diesem Stand
                        zusammengeführt werden.
                    </Typography>
                }

                {
                    hasConflicts &&
                    <>
                        <Typography sx={{mt: 2}}>
                            {
                                mode === 'restore' ?
                                    'Zwischenzeitlich wurde ein neuerer Stand gespeichert. Einige Angaben wurden in beiden Fassungen unterschiedlich geändert. Wählen Sie für jede Angabe aus, welchen Stand Sie übernehmen möchten. Alle übrigen Änderungen werden zusammengeführt.' :
                                    'Ihre übrigen Änderungen werden mit dem gespeicherten Stand zusammengeführt. Die folgenden Angaben wurden jedoch an beiden Stellen unterschiedlich geändert. Wählen Sie jeweils aus, welcher Stand gelten soll.'
                            }
                        </Typography>

                        <Stack sx={{mt: 2}}>
                            {conflicts.map((conflict) => (
                                <RadioFieldComponent
                                    key={conflict.id}
                                    label={conflict.label}
                                    required={true}
                                    value={choices[conflict.id] ?? 'mine'}
                                    onChange={(value) => {
                                        setChoices((previous) => ({
                                            ...previous,
                                            [conflict.id]: value === 'theirs' ? 'theirs' : 'mine',
                                        }));
                                    }}
                                    options={[
                                        {value: 'mine', label: 'Meine Änderung', subLabel: formatPreview(conflict.mine)},
                                        {value: 'theirs', label: 'Gespeicherter Stand', subLabel: formatPreview(conflict.theirs)},
                                    ]}
                                />
                            ))}
                        </Stack>
                    </>
                }

                {
                    mode === 'restore' &&
                    <Typography sx={{mt: 2}}>
                        Speichern Sie Ihre Änderungen nach der Wiederherstellung. Beim Verwerfen wird die
                        zwischengespeicherte Fassung gelöscht.
                    </Typography>
                }
            </DialogContent>

            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    variant="contained"
                    onClick={() => onApply(choices)}
                >
                    {mode === 'save' ? 'Übernehmen und speichern' : 'Änderungen wiederherstellen'}
                </Button>
                <Button
                    variant="outlined"
                    color="error"
                    onClick={onDiscard}
                >
                    Änderungen verwerfen
                </Button>
                {
                    mode === 'save' && onCancel != null &&
                    <Button variant="outlined" onClick={onCancel}>
                        Abbrechen
                    </Button>
                }
            </DialogActions>
        </Dialog>
    );
}

/** Shows short values inline; larger structures such as whole elements are only named by the conflict label. */
function formatPreview(value: unknown): string | undefined {
    if (value === undefined) {
        return 'Entfernt';
    }
    if (value === null || value === '') {
        return 'Leer';
    }
    if (typeof value === 'boolean') {
        return value ? 'Ja' : 'Nein';
    }
    if (typeof value === 'string' || typeof value === 'number') {
        const text = String(value);
        return text.length > PREVIEW_MAX_LENGTH ? `${text.slice(0, PREVIEW_MAX_LENGTH)} …` : text;
    }
    // Configuration values are wrapped as authored values, whose literal content is the meaningful part.
    const literal = getLiteralAuthoredValue(value as AuthoredInputValue<unknown>);
    return literal === undefined ? undefined : formatPreview(literal);
}
