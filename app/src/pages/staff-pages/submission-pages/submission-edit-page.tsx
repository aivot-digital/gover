import React, {FormEvent, useEffect, useState} from "react";
import {Box, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from "@mui/material";
import {Link, useParams} from "react-router-dom";
import {Application} from "../../../models/entities/application";
import {SubmissionDetailsDto} from "../../../models/entities/submission-details-dto";
import {ApplicationService} from "../../../services/application-service";
import {SubmissionService} from "../../../services/submission-service";
import {format, parseISO} from "date-fns";
import {faFileDownload, faFilePdf} from "@fortawesome/pro-light-svg-icons";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";
import {SelectFieldComponent} from "../../../components/select-field/select-field-component";
import {UsersService} from "../../../services/users-service";
import {SelectFieldComponentOption} from "../../../components/select-field/select-field-component-option";
import {User} from "../../../models/entities/user";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {SubmissionAttachmentListDto} from "../../../models/entities/submission-attachment-list-dto";
import {downloadBlobFile} from "../../../utils/download-utils";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showErrorSnackbar, showSuccessSnackbar} from "../../../slices/snackbar-slice";
import {AlertComponent} from "../../../components/alert/alert-component";
import {Destination} from "../../../models/entities/destination";
import {DestinationsService} from "../../../services/destinations-service";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {ConfirmDialog} from "../../../dialogs/confirm-dialog/confirm-dialog";
import {useChangeBlocker} from "../../../hooks/use-change-blocker";
import FolderZipOutlinedIcon from '@mui/icons-material/FolderZipOutlined';
import CachedOutlinedIcon from '@mui/icons-material/CachedOutlined';

export function SubmissionEditPage() {
    const dispatch = useAppDispatch();

    const {applicationId, id} = useParams();

    const [application, setApplication] = useState<Application>();
    const [destination, setDestination] = useState<Destination>();
    const [attachments, setAttachments] = useState<SubmissionAttachmentListDto[]>();
    const [userOptions, setUserOptions] = useState<SelectFieldComponentOption[]>();

    const [originalSubmission, setOriginalSubmission] = useState<SubmissionDetailsDto>();
    const [editedSubmission, setEditedSubmission] = useState<SubmissionDetailsDto>();

    const [confirmArchive, setConfirmArchive] = useState<() => void>();

    const hasChanged = useChangeBlocker(originalSubmission, editedSubmission);

    useEffect(() => {
        if (applicationId != null) {
            ApplicationService
                .retrieve(parseInt(applicationId))
                .then(setApplication);
        }
    }, [applicationId]);

    useEffect(() => {
        if (applicationId != null && id != null) {
            SubmissionService
                .retrieve(parseInt(applicationId), id)
                .then(sub => {
                    setEditedSubmission(sub);
                    setOriginalSubmission(sub);
                });

            SubmissionService
                .retrieveAttachments(parseInt(applicationId), id)
                .then(setAttachments);
        }
    }, [applicationId, id]);

    useEffect(() => {
        if (application != null) {
            const fetchPromises: Promise<User[]>[] = [
                UsersService.list({admin: 'true'}),
            ];

            if (application.responsibleDepartment != null) {
                fetchPromises.push(
                    UsersService
                        .list({department: application.responsibleDepartment})
                );
            }

            if (application.managingDepartment != null) {
                fetchPromises.push(
                    UsersService
                        .list({department: application.managingDepartment})
                );
            }

            Promise
                .all(fetchPromises)
                .then(res => res
                    .reduce((acc, currentValue) => {
                        for (const val of currentValue) {
                            const skipVal = acc.some(o => o.id === val.id);
                            if (!skipVal) {
                                acc.push(val);
                            }
                        }
                        return acc;
                    }, []))
                .then(users => users.map(user => ({
                    value: user.id.toString(),
                    label: user.name + (user.admin ? ' (Globaler Administrator)' : ''),
                })))
                .then(setUserOptions);
        }
    }, [application]);

    useEffect(() => {
        if (editedSubmission != null && editedSubmission.destination != null && (destination == null || destination.id !== editedSubmission.destination)) {
            DestinationsService
                .retrieve(editedSubmission.destination)
                .then(setDestination);
        }
    }, [editedSubmission, destination]);

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (application != null && editedSubmission != null) {
            SubmissionService
                .update(application.id, editedSubmission.id, editedSubmission)
                .then(savedSubmission => {
                    dispatch(showSuccessSnackbar('Antrag erfolgreich gespeichert'));
                    setOriginalSubmission(savedSubmission);
                    setEditedSubmission(savedSubmission);
                });
        }
    };

    const handleArchive = () => {
        if (application != null && editedSubmission != null) {
            setConfirmArchive(() => () => {
                const archivedSubmission = {
                    ...editedSubmission,
                    archived: new Date().toISOString(),
                }
                SubmissionService
                    .update(application.id, editedSubmission.id, archivedSubmission)
                    .then(() => {
                        dispatch(showSuccessSnackbar('Antrag erfolgreich archiviert'));
                    });
                setOriginalSubmission(archivedSubmission);
                setEditedSubmission(archivedSubmission);
                setConfirmArchive(undefined);
            });
        }
    };

    const handleResendDestination = () => {
        if (application != null && editedSubmission != null) {
            SubmissionService
                .resentDestination(application.id, editedSubmission.id)
                .then(resentSubmission => {
                    setOriginalSubmission(resentSubmission);
                    setEditedSubmission(resentSubmission);
                })
                .catch(() => {
                    dispatch(showErrorSnackbar('Erneutes Übertragen des Antrags fehlgeschlagen'));
                });
        }
    };

    const handleDownloadPrint = () => {
        if (application != null && editedSubmission != null) {
            SubmissionService
                .downloadPdf(application.id, editedSubmission.id)
                .then(res => downloadBlobFile(`${application.title}.pdf`, res));
        }
    };

    const handleDownloadAttachment = (att: SubmissionAttachmentListDto) => {
        if (application != null && editedSubmission != null) {
            SubmissionService
                .downloadAttachment(application.id, editedSubmission.id, att.id)
                .then(res => downloadBlobFile(att.filename, res));
        }
    };

    const created = editedSubmission != null ? parseISO(editedSubmission.created) : undefined;
    const archived = editedSubmission?.archived != null ? parseISO(editedSubmission.archived) : undefined;
    const title = `Antrage - ${application?.title} - ${editedSubmission?.fileNumber ?? format(created ?? new Date(), 'dd.MM.yyyy - HH:mm')}`;

    return (
        <PageWrapper
            title={title}
            isLoading={application == null || editedSubmission == null || userOptions == null || attachments == null}
            toolbarActions={archived == null && editedSubmission?.destination == null ? [
                {
                    icon: <FolderZipOutlinedIcon/>,
                    tooltip: 'Antrag abschließen',
                    onClick: handleArchive,
                },
            ] : (editedSubmission?.destination != null && !editedSubmission?.destinationSuccess ? [
                {
                    icon: <CachedOutlinedIcon/>,
                    tooltip: 'Erneut übertragen',
                    onClick: handleResendDestination,
                },
            ] : undefined)}
        >

            {
                editedSubmission?.destination == null &&
                editedSubmission?.assignee == null &&
                <Box
                    sx={{mb: 4}}
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
                    sx={{mb: 4}}
                >
                    <AlertComponent
                        title="Abgeschlossener Vorgang"
                        text={`Dieser Antrag wurde am ${format(archived, 'dd.MM.yyyy')} um ${format(archived, 'HH:mm')} Uhr abgeschlossen.`}
                        color="warning"
                    />
                </Box>
            }

            {
                editedSubmission?.destination != null &&
                editedSubmission?.destinationSuccess &&
                archived != null &&
                <Box
                    sx={{mb: 4}}
                >
                    <AlertComponent
                        title="An Schnittstelle übertragen"
                        color="success"
                    >
                        Dieser Antrag wurde am {format(archived, 'dd.MM.yyyy')} um {format(archived, 'HH:mm')} Uhr
                        erfolgreich an die
                        Schnittstelle <Link to={`/destinations/${destination?.id}`}>{destination?.name}</Link> übertragen.
                    </AlertComponent>
                </Box>
            }

            {
                editedSubmission?.destination != null &&
                !editedSubmission?.destinationSuccess &&
                <Box
                    sx={{mb: 4}}
                >
                    <AlertComponent
                        title="Übertragung fehlgeschlagen"
                        color="error"
                    >
                        Dieser Antrag konnte nicht an die
                        Schnittstelle <Link to={`/destinations/${destination?.id}`}>{destination?.name}</Link> übertragen
                        werden.
                        Bitte überprüfen Sie die Schnittstelle und probieren Sie es erneut.
                    </AlertComponent>
                </Box>
            }

            <Typography
                variant="h6"
                sx={{mb: 2}}
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
                                {created && format(created, 'dd.MM.yyyy')}
                            </TableCell>
                        </TableRow>

                        <TableRow>
                            <TableCell scope="th">
                                Eingangsuhrzeit
                            </TableCell>
                            <TableCell>
                                {created && format(created, 'HH:mm')} Uhr
                            </TableCell>
                        </TableRow>
                    </TableBody>
                </Table>
            </TableContainer>

            {
                editedSubmission?.destination == null &&
                <form onSubmit={handleSubmit}>
                    <Typography
                        variant="h6"
                        sx={{mt: 4, mb: 2}}
                    >
                        Bearbeitungsinformationen
                    </Typography>

                    <TextFieldComponent
                        label="Aktenzeichen"
                        value={editedSubmission?.fileNumber ?? undefined}
                        onChange={val => setEditedSubmission({
                            ...editedSubmission!,
                            fileNumber: val ?? null,
                        })}
                        required
                        disabled={archived != null}
                    />

                    <SelectFieldComponent
                        label="Zuständige Mitarbeiter:in"
                        value={editedSubmission?.assignee?.toString() ?? undefined}
                        onChange={val => setEditedSubmission({
                            ...editedSubmission!,
                            assignee: val != null ? parseInt(val) : null,
                        })}
                        options={userOptions ?? []}
                        required
                        disabled={archived != null}
                    />

                    {
                        editedSubmission?.archived == null &&
                        <Box sx={{mt: 2}}>
                            <Button
                                type="submit"
                                disabled={!hasChanged}
                            >
                                Speichern
                            </Button>

                            <Button
                                type="reset"
                                color="error"
                                disabled={!hasChanged}
                                onClick={() => setEditedSubmission(JSON.parse(JSON.stringify(originalSubmission)))}
                                sx={{ml: 2}}
                            >
                                Zurücksetzen
                            </Button>
                        </Box>
                    }
                </form>
            }

            <Typography
                variant="h6"
                sx={{mt: 4, mb: 2}}
            >
                Antrag
            </Typography>

            <Button
                onClick={handleDownloadPrint}
                endIcon={
                    <FontAwesomeIcon
                        icon={faFilePdf}
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
                        sx={{mt: 4, mb: 2}}
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
                                    attachments.map(att => (
                                        <TableRow key={att.id}>
                                            <TableCell>
                                                {att.filename}
                                            </TableCell>
                                            <TableCell>
                                                <Button
                                                    onClick={() => handleDownloadAttachment(att)}
                                                    endIcon={
                                                        <FontAwesomeIcon icon={faFileDownload}/>
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
                title="Antrag archivieren"
                onConfirm={confirmArchive}
                onCancel={() => setConfirmArchive(undefined)}
            >
                Sind Sie sicher, dass Sie den Antrag archivieren wollen? Dies kann nicht rückgängig gemacht werden.
            </ConfirmDialog>
        </PageWrapper>
    );
}