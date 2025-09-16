import {type DeleteApplicationDialogProps} from './delete-application-dialog-props';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import React, { useEffect, useState } from 'react';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    List, ListItem, ListItemIcon, ListItemText, Typography,
} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useApi} from '../../../hooks/use-api';
import {FormsApiService} from '../../../modules/forms/forms-api-service';
import {SubmissionsApiService} from '../../../modules/submissions/submissions-api-service';
import {SubmissionListResponseDTO} from '../../../modules/submissions/dtos/submission-list-response-dto';
import FolderSharedOutlinedIcon from '@mui/icons-material/FolderSharedOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import {SubmissionWithMembershipResponseDTO} from '../../../modules/submissions/dtos/submission-with-membership-response-dto';

export function DeleteApplicationDialog(props: DeleteApplicationDialogProps) {
    const api = useApi();

    const dispatch = useAppDispatch();

    const [submissions, setSubmissions] = useState<SubmissionWithMembershipResponseDTO[]>();
    const [formTitle, setFormTitle] = useState<string>();
    const [isBusy, setIsBusy] = useState(false);

    useEffect(() => {
        if (props.form == null) {
            return;
        }

        setIsBusy(true);
        new SubmissionsApiService(api)
            .list(0, 999, undefined, undefined, {
                notTestSubmission: true,
                notArchived: true,
                formId: props.form.id,
            })
            .then((page) => {
                setSubmissions(page.content);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Offene Anträge konnten nicht geprüft werden.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    }, [props.form]);

    const handleDelete = (): void => {
        props.onDelete();
    };

    return (
        <Dialog
            open={props.form != null}
        >
            <DialogTitle>
                Formular löschen
            </DialogTitle>
            <DialogContent tabIndex={0}>
                {
                    ((submissions?.length ?? 0) > 0 || props.form?.publishedVersion != null) &&
                    <>
                        <DialogContentText>
                            Bitte klären Sie die folgenden Punkte, bevor Sie das Formular <strong>{props.form?.internalTitle}</strong> löschen können:
                        </DialogContentText>
                        <List dense>
                            {
                                (submissions?.length ?? 0) > 0 &&
                                <ListItem sx={{alignItems: 'start', color: 'text.secondary'}}>
                                    <ListItemIcon sx={{paddingTop: .4, minWidth: 40}}>
                                        <FolderSharedOutlinedIcon />
                                    </ListItemIcon>
                                    <ListItemText>
                                        <strong>Offene Anträge:</strong> <br/>Es sind noch offene Anträge ({submissions?.length ?? 0}) für dieses Formular vorhanden.
                                        Bitte schließen Sie diese, bevor Sie das Formular löschen.
                                    </ListItemText>
                                </ListItem>
                            }
                            {
                                props.form?.publishedVersion != null &&
                                <ListItem sx={{alignItems: 'start', color: 'text.secondary'}}>
                                    <ListItemIcon sx={{paddingTop: .4, minWidth: 40}}>
                                        <DescriptionOutlinedIcon />
                                    </ListItemIcon>
                                    <ListItemText>
                                        <strong>Veröffentlichungsstatus:</strong> <br/>Dieses Formular ist noch veröffentlicht. Bitte ziehen Sie es zuerst zurück und
                                        entfernen Sie mögliche Links auf das Formular von dritten Webseiten, bevor Sie es löschen.
                                    </ListItemText>
                                </ListItem>
                            }
                        </List>
                    </>
                }

                {
                    submissions?.length === 0 && props.form?.publishedVersion == null &&
                    <>
                        <DialogContentText>
                            Sind Sie sicher, dass Sie das
                            Formular <strong>{props.form?.internalTitle}</strong> wirklich
                            löschen wollen? Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
                            Es werden alle Anträge, die für diese Formularversion eingegangen sind, gelöscht.
                        </DialogContentText>


                        <DialogContentText
                            sx={{
                                mt: 2,
                            }}
                        >
                            Bitte geben Sie den folgenden Text ein, um die Aktion zu bestätigen:
                            <Typography component="pre" variant="body2" sx={{ fontFamily: "monospace", fontSize: 14, fontWeight: "bold", backgroundColor: "#f0f0f0", py: .5, px: 1, borderRadius: 2, mt: 1 }}>
                                {props.form?.internalTitle}
                            </Typography>
                        </DialogContentText>

                        <TextFieldComponent
                            sx={{mt: 3}}
                            label="Eingabe zur Bestätigung"
                            value={formTitle}
                            onChange={setFormTitle}
                        />
                    </>
                }
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={handleDelete}
                    disabled={isBusy || formTitle !== props.form?.internalTitle}
                    color="error"
                    variant="contained"
                    startIcon={<DeleteOutlinedIcon />}
                >
                    Ja, endgültig löschen
                </Button>
                <Button
                    onClick={() => {
                        setFormTitle(undefined);
                        props.onCancel();
                    }}
                    disabled={isBusy}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
