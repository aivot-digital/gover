import {useCallback, useEffect, useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent, List, ListItem, ListItemIcon, ListItemText, Skeleton, Typography} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import LinkIcon from '@aivot/mui-material-symbols-400-outlined/dist/link/Link';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessSlugHistoryEntity} from '../../entities/process-slug-history-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useConfirm} from '../../../../providers/confirm-provider';

interface ProcessSettingsDialogSlugHistoryDialogProps {
    open: boolean;
    process: ProcessEntity;
    onClose: () => void;
}

export function ProcessSettingsDialogSlugHistoryDialog(props: ProcessSettingsDialogSlugHistoryDialogProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        open,
        process,
        onClose,
    } = props;

    const [slugHistory, setSlugHistory] = useState<ProcessSlugHistoryEntity[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isClearing, setIsClearing] = useState(false);

    const loadSlugHistory = useCallback(() => {
        setIsLoading(true);
        new ProcessDefinitionApiService()
            .listSlugHistory(process.id)
            .then(setSlugHistory)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die URL-Namespace-Historie konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [dispatch, process.id]);

    useEffect(() => {
        if (open) {
            loadSlugHistory();
        }
    }, [loadSlugHistory, open]);

    const handleClear = async () => {
        if (slugHistory.length === 0 || isClearing) {
            return;
        }

        const confirmed = await confirm({
            title: 'URL-Namespace-Historie leeren',
            confirmButtonText: 'Historie leeren',
            isDestructive: true,
            children: (
                <Typography>
                    Möchten Sie alle früheren URL-Namespaces dieses Prozesses wirklich löschen?
                    Diese Namespaces können danach von anderen Prozessen verwendet werden.
                </Typography>
            ),
        });

        if (!confirmed) {
            return;
        }

        setIsClearing(true);
        // Clearing history releases old namespaces; the current process slug is never deleted here.
        new ProcessDefinitionApiService()
            .clearSlugHistory(process.id)
            .then(() => {
                setSlugHistory([]);
                dispatch(showSuccessSnackbar('Die URL-Namespace-Historie wurde geleert.'));
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die URL-Namespace-Historie konnte nicht geleert werden.'));
            })
            .finally(() => {
                setIsClearing(false);
            });
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="sm"
        >
            <DialogTitleWithClose onClose={onClose}>
                URL-Namespace-Historie
            </DialogTitleWithClose>
            <DialogContent>
                <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
                    Die Historie enthält frühere URL-Namespaces des Prozesses. Alte Namespaces werden automatisch auf den aktuellen
                    Namespace umgeleitet, bis Sie die Historie leeren.
                </Typography>

                {
                    isLoading &&
                    <Skeleton height={96}/>
                }

                {
                    !isLoading && slugHistory.length === 0 &&
                    <Alert severity="info">
                        Für diesen Prozess sind keine früheren URL-Namespaces gespeichert.
                    </Alert>
                }

                {
                    !isLoading && slugHistory.length > 0 &&
                    <>
                        <Alert severity="warning" sx={{mb: 2}}>
                            Beim Leeren werden diese Namespaces freigegeben und können anschließend von anderen Prozessen verwendet werden.
                        </Alert>
                        <List
                            dense
                            sx={{'& .MuiListItem-root:last-of-type': {borderBottom: 'none'}}}
                        >
                            {
                                slugHistory.map((history) => (
                                    <ListItem
                                        key={history.slug}
                                        sx={{
                                            borderBottom: '1px solid #eee',
                                            px: 0.25,
                                        }}
                                    >
                                        <ListItemIcon sx={{color: 'primary.dark', minWidth: '2.5rem', textAlign: 'center'}}>
                                            <LinkIcon/>
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={
                                                <Typography component="code" variant="body2">
                                                    /{history.slug}
                                                </Typography>
                                            }
                                            secondary={`Leitet automatisch auf /${process.slug} um`}
                                            slotProps={{
                                                primary: {
                                                    sx: {
                                                        whiteSpace: 'nowrap',
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                    },
                                                },
                                            }}
                                        />
                                    </ListItem>
                                ))
                            }
                        </List>
                    </>
                }
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    color="error"
                    startIcon={<Delete/>}
                    disabled={slugHistory.length === 0 || isLoading || isClearing}
                    onClick={() => {
                        void handleClear();
                    }}
                >
                    Historie leeren
                </Button>
                <div style={{flexGrow: 1}}/>
                <Button
                    onClick={onClose}
                    disabled={isClearing}
                >
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
