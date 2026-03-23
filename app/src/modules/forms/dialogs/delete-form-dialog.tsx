import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, List, ListItem, ListItemIcon, ListItemText, Typography} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useApi} from '../../../hooks/use-api';
import {SubmissionsApiService} from '../../submissions/submissions-api-service';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {SubmissionWithMembershipResponseDTO} from '../../submissions/dtos/submission-with-membership-response-dto';
import Description from '@aivot/mui-material-symbols-400-outlined/dist/description/Description';
import FolderShared from '@aivot/mui-material-symbols-400-outlined/dist/folder-shared/FolderShared';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {FormEntity} from '../entities/form-entity';

interface DeleteFormDialogProps {
    form?: FormEntity;
    onDelete: (form: FormEntity) => void;
    onCancel: () => void;
}

export function DeleteFormDialog(props: DeleteFormDialogProps) {
    const api = useApi();

    const dispatch = useAppDispatch();

    const [formTitle, setFormTitle] = useState<string>();
    const [isBusy, setIsBusy] = useState(false);

    const handleDelete = (): void => {
        if (props.form == null) {
            return;
        }
        setFormTitle(undefined);
        props.onDelete(props.form);
    };

    return (
        <Dialog
            open={props.form != null}
        >
            <DialogTitleWithClose
                onClose={() => {
                    setFormTitle(undefined);
                    props.onCancel();
                }}
            >
                Formular löschen
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                <Typography>
                    Sind Sie sicher, dass Sie das
                    Formular <strong>{props.form?.internalTitle}</strong> wirklich
                    löschen wollen? Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
                    Es werden alle Anträge, die für diese Formularversion eingegangen sind, gelöscht.
                </Typography>


                <Typography
                    sx={{
                        mt: 2,
                    }}
                >
                    Bitte geben Sie den folgenden Text ein, um die Aktion zu bestätigen:
                    <Typography
                        component="pre"
                        variant="body2"
                        sx={{fontFamily: 'monospace', fontSize: 14, fontWeight: 'bold', backgroundColor: '#f0f0f0', py: .5, px: 1, borderRadius: 2, mt: 1}}
                    >
                        {props.form?.internalTitle}
                    </Typography>
                </Typography>

                <TextFieldComponent
                    sx={{mt: 3}}
                    label="Eingabe zur Bestätigung"
                    value={formTitle}
                    onChange={setFormTitle}
                />
            </DialogContent>
            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    onClick={handleDelete}
                    disabled={isBusy || formTitle !== props.form?.internalTitle}
                    color="error"
                    variant="contained"
                    startIcon={<DeleteOutlinedIcon />}
                >
                    Formular endgültig löschen
                </Button>
                <Button
                    onClick={() => {
                        setFormTitle(undefined);
                        props.onCancel();
                    }}
                    disabled={isBusy}
                    variant="outlined"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
