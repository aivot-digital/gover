import React from 'react';
import {Typography} from '@mui/material';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {FormEntity} from '../entities/form-entity';

interface DeleteFormDialogProps {
    form?: FormEntity;
    onDelete: (form: FormEntity) => void;
    onCancel: () => void;
}

export function DeleteFormDialog(props: DeleteFormDialogProps) {
    const form = props.form;

    return (
        <ConfirmDialog
            key={form?.id ?? 'closed'}
            title="Formular löschen"
            onCancel={props.onCancel}
            onConfirm={form != null ? () => props.onDelete(form) : undefined}
            confirmationText={form?.internalTitle}
            inputLabel="Eingabe zur Bestätigung"
            confirmButtonText="Formular endgültig löschen"
            isDestructive
        >
            <Typography>
                Sind Sie sicher, dass Sie das Formular <strong>{form?.internalTitle}</strong> wirklich löschen
                wollen? Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
            </Typography>
        </ConfirmDialog>
    );
}
