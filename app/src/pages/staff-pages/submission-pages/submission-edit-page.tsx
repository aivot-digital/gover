import React, { type FormEvent, useEffect, useState } from 'react';
import {
    Box,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import { Link, useParams } from 'react-router-dom';
import { type Application } from '../../../models/entities/application';
import { type SubmissionDetailsDto } from '../../../models/entities/submission-details-dto';
import { ApplicationService } from '../../../services/application-service';
import { SubmissionService } from '../../../services/submission-service';
import { format, parseISO } from 'date-fns';
import { faArrowsRotate, faFileDownload, faFilePdf, faFileZipper } from '@fortawesome/pro-light-svg-icons';
import { TextFieldComponent } from '../../../components/text-field/text-field-component';
import { SelectFieldComponent } from '../../../components/select-field/select-field-component';
import { UsersService } from '../../../services/users-service';
import { type SelectFieldComponentOption } from '../../../components/select-field/select-field-component-option';
import { type User } from '../../../models/entities/user';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { type SubmissionAttachmentListDto } from '../../../models/entities/submission-attachment-list-dto';
import { downloadBlobFile } from '../../../utils/download-utils';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { showErrorSnackbar, showSuccessSnackbar } from '../../../slices/snackbar-slice';
import { AlertComponent } from '../../../components/alert/alert-component';
import { type Destination } from '../../../models/entities/destination';
import { DestinationsService } from '../../../services/destinations-service';
import { PageWrapper } from '../../../components/page-wrapper/page-wrapper';
import { ConfirmDialog } from '../../../dialogs/confirm-dialog/confirm-dialog';
import { useChangeBlocker } from '../../../hooks/use-change-blocker';
import { delayPromise } from '../../../utils/with-delay';
import { isStringNotNullOrEmpty } from '../../../utils/string-utils';

async function fetchData(formId: string, submissionId: string): Promise<{
    form: Application;
    submission: SubmissionDetailsDto;
    attachments: SubmissionAttachmentListDto[];
    destination?: Destination;
    users: SelectFieldComponentOption[];
}> {
    const form = await ApplicationService.retrieve(parseInt(formId));

    const submission = await SubmissionService.retrieve(form.id, submissionId);

    const attachments = await SubmissionService.retrieveAttachments(form.id, submissionId);

    let destination: Destination | undefined;
    if (submission.destination != null) {
        destination = await DestinationsService.retrieve(submission.destination);
    }

    const fetchUserPromises: Array<Promise<User[]>> = [
        UsersService
            .list({
                admin: 'true',
            }),
    ];

    if (form.responsibleDepartment != null) {
        fetchUserPromises.push(
            UsersService
                .list({
                    department: form.responsibleDepartment,
                }),
        );
    }

    if (form.managingDepartment != null) {
        fetchUserPromises.push(
            UsersService
                .list({
                    department: form.managingDepartment,
                }),
        );
    }

    if (submission.assignee != null) {
        fetchUserPromises.push(
            UsersService
                .retrieve(submission.assignee)
                .then((res) => [res]),
        );
    }

    const users = await Promise
        .all(fetchUserPromises)
        .then((res) => res
            .reduce((acc, currentValue) => {
                for (const val of currentValue) {
                    const skipVal = acc.some((o) => o.id === val.id);
                    if (!skipVal) {
                        acc.push(val);
                    }
                }
                return acc;
            }, []))
        .then((users) => users.map((user) => ({
            value: user.id.toString(),
            label: user.name + (user.admin ? ' (Globaler Administrator)' : ''),
        })));

    return {
        form,
        submission,
        attachments,
        destination,
        users,
    };
}

export function SubmissionEditPage(): JSX.Element {
    const dispatch = useAppDispatch();

    const {
        applicationId,
        id,
    } = useParams();

    const [form, setForm] = useState<Application>();
    const [destination, setDestination] = useState<Destination>();
    const [attachments, setAttachments] = useState<SubmissionAttachmentListDto[]>();
    const [userOptions, setUserOptions] = useState<SelectFieldComponentOption[]>();

    const [originalSubmission, setOriginalSubmission] = useState<SubmissionDetailsDto>();
    const [editedSubmission, setEditedSubmission] = useState<SubmissionDetailsDto>();

    const [confirmArchive, setConfirmArchive] = useState<() => void>();

    const [isLoading, setIsLoading] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);

    const hasChanged = useChangeBlocker(originalSubmission, editedSubmission);

    useEffect(() => {
        if (applicationId != null && id != null) {
            setIsLoading(true);
            setIsNotFound(false);

            delayPromise(fetchData(applicationId, id))
                .then((res) => {
                    setForm(res.form);
                    setOriginalSubmission(res.submission);
                    setEditedSubmission(res.submission);
                    setAttachments(res.attachments);
                    setDestination(res.destination);
                    setUserOptions(res.users);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    }, [applicationId, id]);

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (form != null && editedSubmission != null) {
            setIsLoading(true);

            SubmissionService
                .update(form.id, editedSubmission.id, editedSubmission)
                .then((savedSubmission) => {
                    dispatch(showSuccessSnackbar('Antrag erfolgreich gespeichert'));
                    setOriginalSubmission(savedSubmission);
                    setEditedSubmission(savedSubmission);
                })
                .catch((err) => {
                    if (err.response?.status === 409) {
                        dispatch(showErrorSnackbar('Der Antrag wurde bereits archiviert'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Antrag konnte nicht gespeichert werden'));
                    }
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const handleArchive = (): void => {
        if (form != null && editedSubmission != null) {
            setConfirmArchive(() => () => {
                const archivedSubmission = {
                    ...editedSubmission,
                    archived: new Date().toISOString(),
                };

                setIsLoading(true);
                SubmissionService
                    .update(form.id, editedSubmission.id, archivedSubmission)
                    .then(() => {
                        setOriginalSubmission(archivedSubmission);
                        setEditedSubmission(archivedSubmission);
                        setConfirmArchive(undefined);
                        dispatch(showSuccessSnackbar('Antrag erfolgreich archiviert'));
                    })
                    .catch((err) => {
                        if (err.response?.status === 409) {
                            dispatch(showErrorSnackbar('Der Antrag wurde bereits archiviert'));
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Antrag konnte nicht archiviert werden'));
                        }
                    })
                    .finally(() => {
                        setIsLoading(false);
                    });
            });
        }
    };

    const handleResendDestination = (): void => {
        if (form != null && editedSubmission != null) {
            setIsLoading(true);

            SubmissionService
                .resentDestination(form.id, editedSubmission.id)
                .then((resentSubmission) => {
                    setOriginalSubmission(resentSubmission);
                    setEditedSubmission(resentSubmission);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Erneutes Übertragen des Antrags fehlgeschlagen'));
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const handleDownloadPrint = (): void => {
        if (form != null && editedSubmission != null) {
            setIsLoading(true);

            SubmissionService
                .downloadPdf(form.id, editedSubmission.id)
                .then((res) => {
                    downloadBlobFile(`${ form.title }.pdf`, res);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('PDF konnte nicht heruntergeladen werden'));
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const handleDownloadAttachment = (att: SubmissionAttachmentListDto): void => {
        if (form != null && editedSubmission != null) {
            setIsLoading(true);

            SubmissionService
                .downloadAttachment(form.id, editedSubmission.id, att.id)
                .then((res) => {
                    downloadBlobFile(att.filename, res);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Anlage konnte nicht heruntergeladen werden'));
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    };

    const created = editedSubmission != null ? parseISO(editedSubmission.created) : undefined;
    const archived = editedSubmission?.archived != null ? parseISO(editedSubmission.archived) : undefined;

    let title = 'Antrag';
    if (form != null) {
        title += ` - ${ form.title } - ${ form.version }`;
        if (isStringNotNullOrEmpty(editedSubmission?.fileNumber)) {
            title += ` - ${ editedSubmission?.fileNumber ?? '' }`;
        }
    }

    return (
        <PageWrapper
            title={ isLoading ? 'Lade...' : (isNotFound ? 'Nicht gefunden' : title) }
            isLoading={ isLoading }
            is404={ isNotFound }
            toolbarActions={ archived == null && editedSubmission?.destination == null ?
                [
                    {
                        icon: faFileZipper,
                        tooltip: 'Antrag abschließen',
                        onClick: handleArchive,
                    },
                ] :
                (editedSubmission?.destination != null && !(editedSubmission?.destinationSuccess ?? true) ?
                    [
                        {
                            icon: faArrowsRotate,
                            tooltip: 'Erneut übertragen',
                            onClick: handleResendDestination,
                        },
                    ] :
                    undefined) }
        >
            {
                (editedSubmission?.isTestSubmission ?? false) &&
                <Box
                    sx={ {mb: 4} }
                >
                    <AlertComponent
                        title="Test-Antrag"
                        text="Bei diesem Antrag handelt es sich im einen Test-Antrag. Der Antrag wurde für ein Formular abgesendet, dass noch nicht veröffentlicht wurde."
                        color="info"
                    />
                </Box>
            }

            {
                editedSubmission?.destination == null &&
                editedSubmission?.assignee == null &&
                <Box
                    sx={ {mb: 4} }
                >
                    <AlertComponent
                        title="Noch nicht in Bearbeitung"
                        text="Dieser Antrag befindet sich noch nicht in Bearbeitung. Wählen Sie eine Bearbeiter:in aus, um den Vorgang zu starten."
                        color="warning"
                    />
                </Box>
            }

            {
                editedSubmission?.destination == null &&
                archived != null &&
                <Box
                    sx={ {mb: 4} }
                >
                    <AlertComponent
                        title="Abgeschlossener Vorgang"
                        text={ `Dieser Antrag wurde am ${ format(archived, 'dd.MM.yyyy') } um ${ format(archived, 'HH:mm') } Uhr abgeschlossen.` }
                        color="warning"
                    />
                </Box>
            }

            {
                editedSubmission?.destination != null &&
                (editedSubmission?.destinationSuccess ?? false) &&
                archived != null &&
                <Box
                    sx={ {mb: 4} }
                >
                    <AlertComponent
                        title="An Schnittstelle übertragen"
                        color="success"
                    >
                        Dieser Antrag wurde am { format(archived, 'dd.MM.yyyy') } um { format(archived, 'HH:mm') } Uhr
                        erfolgreich an die
                        Schnittstelle <Link to={ `/destinations/${ destination?.id ?? 0 }` }>{ destination?.name }</Link> übertragen.
                    </AlertComponent>
                </Box>
            }

            {
                editedSubmission?.destination != null &&
                !(editedSubmission?.destinationSuccess ?? false) &&
                <Box
                    sx={ {mb: 4} }
                >
                    <AlertComponent
                        title="Übertragung fehlgeschlagen"
                        color="error"
                    >
                        Dieser Antrag konnte nicht an die
                        Schnittstelle <Link to={ `/destinations/${ destination?.id ?? 0 }` }>{ destination?.name }</Link> übertragen
                        werden.
                        Bitte überprüfen Sie die Schnittstelle und probieren Sie es erneut.
                    </AlertComponent>
                </Box>
            }

            <Typography
                variant="h6"
                sx={ {mb: 2} }
            >
                Antragsinformationen
            </Typography>

            <TableContainer>
                <Table>
                    <TableBody>
                        <TableRow>
                            <TableCell scope="th">
                                Eingangsdatum
                            </TableCell>
                            <TableCell>
                                { (created != null) && format(created, 'dd.MM.yyyy') }
                            </TableCell>
                        </TableRow>

                        <TableRow>
                            <TableCell scope="th">
                                Eingangsuhrzeit
                            </TableCell>
                            <TableCell>
                                { (created != null) && format(created, 'HH:mm') } Uhr
                            </TableCell>
                        </TableRow>
                    </TableBody>
                </Table>
            </TableContainer>

            {
                editedSubmission?.destination == null &&
                <form onSubmit={ handleSubmit }>
                    <Typography
                        variant="h6"
                        sx={ {mt: 4, mb: 2} }
                    >
                        Bearbeitungsinformationen
                    </Typography>

                    <TextFieldComponent
                        label="Aktenzeichen"
                        value={ editedSubmission?.fileNumber ?? undefined }
                        onChange={ (val) => {
                            if (editedSubmission == null) {
                                return;
                            }
                            setEditedSubmission({
                                ...editedSubmission,
                                fileNumber: val ?? null,
                            });
                        } }
                        disabled={ archived != null }
                    />

                    <SelectFieldComponent
                        label="Zuständige Mitarbeiter:in"
                        value={ editedSubmission?.assignee?.toString() ?? undefined }
                        onChange={ (val) => {
                            if (editedSubmission == null) {
                                return;
                            }
                            setEditedSubmission({
                                ...editedSubmission,
                                assignee: val != null ? parseInt(val) : null,
                            });
                        } }
                        options={ userOptions ?? [] }
                        disabled={ archived != null }
                    />

                    {
                        editedSubmission?.archived == null &&
                        <Box sx={ {mt: 2} }>
                            <Button
                                type="submit"
                                disabled={ !hasChanged }
                            >
                                Speichern
                            </Button>

                            <Button
                                type="reset"
                                color="error"
                                disabled={ !hasChanged }
                                onClick={ () => {
                                    setEditedSubmission(JSON.parse(JSON.stringify(originalSubmission)));
                                } }
                                sx={ {ml: 2} }
                            >
                                Zurücksetzen
                            </Button>
                        </Box>
                    }
                </form>
            }

            <Typography
                variant="h6"
                sx={ {mt: 4, mb: 2} }
            >
                Antrag
            </Typography>

            <Button
                onClick={ handleDownloadPrint }
                endIcon={
                    <FontAwesomeIcon
                        icon={ faFilePdf }
                    />
                }
            >
                Antrags-PDF herunterladen
            </Button>

            {
                attachments != null &&
                attachments.length > 0 &&
                <>
                    <Typography
                        variant="h6"
                        sx={ {mt: 4, mb: 2} }
                    >
                        Anlagen
                    </Typography>

                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow>
                                    <TableCell>
                                        Dateiname
                                    </TableCell>
                                    <TableCell/>
                                </TableRow>
                            </TableHead>

                            <TableBody>
                                {
                                    attachments.map((att) => (
                                        <TableRow key={ att.id }>
                                            <TableCell>
                                                { att.filename }
                                            </TableCell>
                                            <TableCell>
                                                <Button
                                                    onClick={ () => {
                                                        handleDownloadAttachment(att);
                                                    } }
                                                    endIcon={
                                                        <FontAwesomeIcon icon={ faFileDownload }/>
                                                    }
                                                >
                                                    Herunterladen
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </TableContainer>
                </>
            }

            <ConfirmDialog
                title="Vorgang abschließen"
                onConfirm={ confirmArchive }
                onCancel={ () => {
                    setConfirmArchive(undefined);
                } }
            >
                Sind Sie sicher, dass Sie den Vorgang abschließen wollen? Der Vorgang kann danach nicht wieder in
                Bearbeitung genommen werden.
            </ConfirmDialog>
        </PageWrapper>
    );
}
