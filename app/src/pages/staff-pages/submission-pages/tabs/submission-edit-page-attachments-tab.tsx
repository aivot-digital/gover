import React, {useEffect, useState} from 'react';
import {Button, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from '@mui/material';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {Form} from '../../../../models/entities/form';
import {Submission} from '../../../../models/entities/submission';
import {Api} from '../../../../hooks/use-api';
import {useSubmissionsApi} from '../../../../hooks/use-submissions-api';
import {downloadBlobFile, downloadObjectFile} from '../../../../utils/download-utils';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {SubmissionAttachment} from '../../../../models/entities/submission-attachment';
import {stringOrDefault} from '../../../../utils/string-utils';
import WebhookOutlinedIcon from '@mui/icons-material/WebhookOutlined';
import CloudSyncOutlinedIcon from '@mui/icons-material/CloudSyncOutlined';

interface SubmissionEditPageAttachmentsTabProps {
    api: Api;
    form: Form;
    submission: Submission;
}

export function SubmissionEditPageAttachmentsTab(props: SubmissionEditPageAttachmentsTabProps): JSX.Element {
    const dispatch = useAppDispatch();
    const [attachments, setAttachments] = useState<SubmissionAttachment[]>();
    const [isLoading, setIsLoading] = useState(false);

    const defaultFileName = 'Antragsdaten-' + stringOrDefault(props.submission.fileNumber, props.submission.id);

    useEffect(() => {
        setIsLoading(true);

        useSubmissionsApi(props.api)
            .listAttachments(props.submission.id)
            .then(setAttachments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Anlagen konnten nicht geladen werden'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);


    const handleDownloadPrint = (): void => {
        setIsLoading(true);

        useSubmissionsApi(props.api)
            .downloadPdf(props.submission.id)
            .then((res) => {
                downloadBlobFile(defaultFileName + '.pdf', res);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('PDF konnte nicht heruntergeladen werden'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    const handleDownloadAttachment = (att: SubmissionAttachment): void => {
        setIsLoading(true);

        useSubmissionsApi(props.api)
            .downloadAttachment(props.submission.id, att.id)
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
    };

    const handleDownloadJson = (): void => {
        downloadObjectFile(defaultFileName + '.json', props.submission.customerInput);
    };

    const handleDownloadDestinationData = (): void => {
        setIsLoading(true);

        useSubmissionsApi(props.api)
            .downloadDestinationData(props.submission.id)
            .then((res) => {
                downloadObjectFile(defaultFileName + '-including-metainfo', res);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Schnittstellen-Daten konnten nicht heruntergeladen werden'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    const files = [
        {
            id: 'pdf',
            type: 'Antragsdaten als PDF-Dokument',
            filename: defaultFileName + '.pdf',
            icon: <PictureAsPdfOutlinedIcon />,
            onClick: handleDownloadPrint,
        },
        {
            id: 'json',
            type: 'Antragsdaten im JSON-Format',
            filename: defaultFileName + '.json',
            icon: <DataObjectOutlinedIcon />,
            onClick: handleDownloadJson,
        },
        {
            id: 'http-json',
            type: 'Antragsdaten inklusive Metainformationen im JSON-Format',
            filename: defaultFileName + '-including-metainfo.json',
            icon: <WebhookOutlinedIcon />,
            onClick: handleDownloadDestinationData,
        },
        ...(attachments ?? []).map((att) => ({
            id: att.id,
            type:  att.type === 'destination' ? 'Von der Schnittstelle übermittelte Anlage' : 'Durch die antragstellende Person hochgeladene Anlage',
            filename: att.filename,
            icon: att.type === 'destination' ? <CloudSyncOutlinedIcon/> : <FileDownloadOutlinedIcon />,
            onClick: () => {
                handleDownloadAttachment(att);
            },
        })),
    ];

    if (isLoading) {
        return (
            <Skeleton
                width="100%"
                height="12em"
            />
        );
    }

    return (
        <>
            <TableContainer
                sx={{
                    mt: 2,
                }}
            >
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell />
                            <TableCell>
                                Dateiname
                            </TableCell>
                            <TableCell />
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {
                            files.map((att) => (
                                <TableRow key={att.id}>
                                    <TableCell>
                                        {att.icon}
                                    </TableCell>
                                    <TableCell>
                                        <Typography
                                            variant="caption"
                                        >
                                            {att.type}
                                        </Typography>
                                        <Typography>
                                            {att.filename}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Button
                                            onClick={att.onClick}
                                            endIcon={
                                                <FileDownloadOutlinedIcon />
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
    );
}
