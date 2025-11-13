import {type DeleteApplicationDialogProps} from './delete-application-dialog-props';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, List, ListItem, ListItemIcon, ListItemText, Typography} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useApi} from '../../../hooks/use-api';
import {SubmissionsApiService} from '../../../modules/submissions/submissions-api-service';
import FolderSharedOutlinedIcon from '@mui/icons-material/FolderSharedOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {SubmissionWithMembershipResponseDTO} from '../../../modules/submissions/dtos/submission-with-membership-response-dto';
import Description from '@aivot/mui-material-symbols-400-outlined/dist/description/Description';
import FolderShared from '@aivot/mui-material-symbols-400-outlined/dist/folder-shared/FolderShared';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';

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
            .listAll({
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
                {
                    ((submissions?.length ?? 0) > 0 || props.form?.publishedVersion != null) &&
                    <>
                        <Typography>
                            Bitte klären Sie die folgenden Punkte, bevor Sie das Formular <strong>{props.form?.internalTitle}</strong> löschen können:
                        </Typography>
                        <List dense>
                            {
                                (submissions?.length ?? 0) > 0 &&
                                <ListItem sx={{alignItems: 'start'}}>
                                    <ListItemIcon sx={{paddingTop: .4, minWidth: 40}}>
                                        <FolderShared />
                                    </ListItemIcon>
                                    <ListItemText>
                                        <strong>Offene Anträge:</strong> <br/>Es sind noch offene Anträge ({submissions?.length ?? 0}) für dieses Formular vorhanden.
                                        Bitte schließen Sie diese, bevor Sie das Formular löschen.
                                    </ListItemText>
                                </ListItem>
                            }
                            {
                                props.form?.publishedVersion != null &&
                                <ListItem sx={{alignItems: 'start'}}>
                                    <ListItemIcon sx={{paddingTop: .4, minWidth: 40}}>
                                        <Description />
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
                            <Typography component="pre" variant="body2" sx={{ fontFamily: "monospace", fontSize: 14, fontWeight: "bold", backgroundColor: "#f0f0f0", py: .5, px: 1, borderRadius: 2, mt: 1 }}>
                                {props.form?.internalTitle}
                            </Typography>
                        </Typography>

                        <TextFieldComponent
                            sx={{mt: 3}}
                            label="Eingabe zur Bestätigung"
                            value={formTitle}
                            onChange={setFormTitle}
                        />
                    </>
                }
            </DialogContent>
            <DialogActions sx={{ justifyContent: 'flex-start' }}>
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
