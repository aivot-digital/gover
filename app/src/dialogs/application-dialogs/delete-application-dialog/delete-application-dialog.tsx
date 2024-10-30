import {type DeleteApplicationDialogProps} from './delete-application-dialog-props';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useApi} from '../../../hooks/use-api';
import {useFormsApi} from '../../../hooks/use-forms-api';
import {useSubmissionsApi} from '../../../hooks/use-submissions-api';
import {SubmissionListProjection} from '../../../models/entities/submission';

export function DeleteApplicationDialog(props: DeleteApplicationDialogProps): JSX.Element {
    const api = useApi();

    const dispatch = useAppDispatch();

    const [submissions, setSubmissions] = useState<SubmissionListProjection[]>();
    const [formTitle, setFormTitle] = useState<string>();
    const [isBusy, setIsBusy] = useState(false);

    useEffect(() => {
        if (props.application == null) {
            return;
        }

        setIsBusy(true);
        useSubmissionsApi(api)
            .list({
                includeArchived: false,
                includeTest: false,
                assigneeId: undefined,
                formId: props.application.id,
            })
            .then((submissions) => {
                setSubmissions(submissions);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Offene Anträge konnten nicht geprüft werden.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    }, [props.application]);

    const handleDelete = (): void => {
        if (props.application == null) {
            return;
        }

        setIsBusy(true);

        useFormsApi(api)
            .destroy(props.application.id)
            .then(() => {
                setFormTitle(undefined);
                setSubmissions(undefined);
                props.onDelete();
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Formular konnte nicht gelöscht werden.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    return (
        <Dialog
            open={props.application != null}
        >
            <DialogTitle>
                Formular löschen
            </DialogTitle>
            <DialogContent tabIndex={0}>
                {
                    (submissions?.length ?? 0) > 0 &&
                    <AlertComponent
                        title="Offene Anträge gefunden"
                        color="error"
                    >
                        Für dieses Formular existieren aktuell noch offene Anträge.
                        Bitte schließen Sie diese, bevor Sie das Formular löschen.
                    </AlertComponent>
                }

                {
                    props.application?.status === 2 &&
                    <AlertComponent
                        title="Formular ist veröffentlicht"
                        color="error"
                    >
                        Dieses Formular ist derzeit noch veröffentlicht. Bitte ziehen Sie es zuerst zurück und
                        entfernen Sie mögliche Links auf das Formular von dritten Webseiten, bevor Sie es löschen.
                    </AlertComponent>
                }

                {
                    submissions?.length === 0 && props.application?.status !== 2 &&
                    <>
                        <DialogContentText>
                            Sind Sie sicher, dass Sie das
                            Formular <strong>{props.application?.title}</strong> wirklich
                            löschen wollen? Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
                            Es werden alle Anträge, die für diese Formularversion eingegangen sind, gelöscht.
                        </DialogContentText>


                        <DialogContentText
                            sx={{
                                mt: 2,
                            }}
                        >
                            Bitte geben Sie den Titel des Formulars ein, um die Löschung zu bestätigen.
                        </DialogContentText>

                        <TextFieldComponent
                            label="Titel bestätigen"
                            value={formTitle}
                            onChange={setFormTitle}
                        />
                    </>
                }
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={handleDelete}
                    disabled={isBusy || formTitle !== props.application?.title}
                    color="error"
                    variant="contained"
                >
                    Unwiderruflich löschen
                </Button>
                <Button
                    onClick={props.onCancel}
                    disabled={isBusy}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
