import {Box, Button, Grid, Typography, useTheme} from '@mui/material';
import {FileUploadElement, FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {humanizeFileSize} from '../../utils/humanization-utils';
import {BaseSummaryProps} from '../../summaries/base-summary';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import {BaseApiService} from '../../services/base-api-service';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../slices/snackbar-slice';

const PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX = 'process-instance-attachment:';

export function FileUploadSummary({
                                      model,
                                      value,
                                  }: BaseSummaryProps<FileUploadElement, FileUploadElementItem[]>) {
    const theme = useTheme();
    const dispatch = useAppDispatch();

    const handleDownload = async (file: FileUploadElementItem) => {
        const attachmentKey = resolveProcessInstanceAttachmentKey(file);
        if (attachmentKey == null) {
            return;
        }

        try {
            const blob = await new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachmentKey)}/file/?download=true`);
            const objectUrl = URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = file.name;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(objectUrl);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht heruntergeladen werden.'));
        }
    };

    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4
                }}>
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    md: 8
                }}>
                {
                    value != null && value.length > 0 ?
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 0.5,
                            }}
                        >
                            {
                                value.map((item) => {
                                    const isPersistedAttachment = isProcessInstanceAttachment(item);

                                    return (
                                        <Box
                                            key={item.uri}
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 1,
                                                flexWrap: 'wrap',
                                            }}
                                        >
                                            <Typography variant="body2">
                                                {item.name} ({humanizeFileSize(item.size)})
                                            </Typography>

                                            {
                                                isPersistedAttachment &&
                                                <Button
                                                    size="small"
                                                    startIcon={<Download fontSize="small" />}
                                                    onClick={() => {
                                                        void handleDownload(item);
                                                    }}
                                                >
                                                    Herunterladen
                                                </Button>
                                            }
                                        </Box>
                                    );
                                })
                            }
                        </Box> :
                        <Typography variant="body2">
                            Keine Anlagen hinzugefügt
                        </Typography>
                }
            </Grid>
        </Grid>
    );
}

function isProcessInstanceAttachment(file: FileUploadElementItem): boolean {
    return file.uri.startsWith(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX);
}

function resolveProcessInstanceAttachmentKey(file: FileUploadElementItem): string | null {
    if (!file.uri.startsWith(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX)) {
        return null;
    }

    const attachmentKey = file.uri.slice(PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX.length).trim();
    return attachmentKey.length === 0 ? null : attachmentKey;
}
