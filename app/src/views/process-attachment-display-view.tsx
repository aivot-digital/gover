import {BaseViewProps} from './base-view';
import {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';
import {useMemo} from 'react';
import {useOptionalProcessTaskViewAttachmentContext} from '../modules/process/pages/details/process-task-view-attachment-context';
import {Box, Chip, Stack, Typography} from '@mui/material';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';

export function ProcessAttachmentDisplayView(props: BaseViewProps<ProcessAttachmentDisplayElement, void>) {
    const {
        element,
    } = props;

    const attachmentContext = useOptionalProcessTaskViewAttachmentContext();

    const matchingAttachments = useMemo(() => {
        if (attachmentContext == null || element.fileName == null || element.fileName.trim().length === 0) {
            return [];
        }

        return attachmentContext.attachments.filter((attachment) => attachment.fileName === element.fileName);
    }, [attachmentContext, element.fileName]);

    if (attachmentContext == null) {
        return (
            <Typography color="text.secondary">
                {
                    element.fileName == null || element.fileName.trim().length === 0 ?
                        'Dateiname konfigurieren, um passende Anlagen anzuzeigen.' :
                        `Anlagen mit dem Dateinamen "${element.fileName}" werden hier angezeigt.`
                }
            </Typography>
        );
    }

    if (attachmentContext.isLoadingAttachments) {
        return (
            <Typography color="text.secondary">
                Lade Anlagen...
            </Typography>
        );
    }

    if (element.fileName == null || element.fileName.trim().length === 0) {
        return (
            <Typography color="text.secondary">
                Es ist kein Dateiname konfiguriert.
            </Typography>
        );
    }

    if (matchingAttachments.length === 0) {
        return (
            <Typography color="text.secondary">
                Keine passenden Anlagen vorhanden.
            </Typography>
        );
    }

    return (
        <Box>
            <Stack
                direction="row"
                spacing={1}
                useFlexGap
                flexWrap="wrap"
            >
                {
                    matchingAttachments.map((attachment) => (
                        <Chip
                            key={attachment.key}
                            variant="outlined"
                            label={attachment.fileName}
                            onDelete={() => {
                                void attachmentContext.downloadAttachment(attachment);
                            }}
                            deleteIcon={<Download color="primary" />}
                            sx={{
                                maxWidth: 320,
                                '& .MuiChip-label': {
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                },
                            }}
                        />
                    ))
                }
            </Stack>
        </Box>
    );
}
