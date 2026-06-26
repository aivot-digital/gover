import React, {useEffect, useMemo, useState} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import List from '@mui/material/List';
import {format} from 'date-fns/format';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import {useConfirm} from '../../../providers/confirm-provider';
import {withDelay} from '../../../utils/with-delay';
import {ApiError, isApiError} from '../../../models/api-error';
import {Link} from 'react-router-dom';
import {Actions} from '../../../components/actions/actions';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {LoadingOverlay} from '../../../components/loading-overlay/loading-overlay';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessStatus, ProcessStatusIcons} from '../enums/process-status';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';
import {ProcessStatusChip} from '../components/process-status/process-status-chip';
import {useProcessExport} from '../../../hooks/use-process-export';
import {ProcessVersionsDialogRowMenu} from './process-versions-dialog-row-menu';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';
import {ProcessExport} from '../entities/process-export';
import {NewProcessDialog} from './new-process-dialog';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';

interface ProcessVersionsDialogProps {
    open: boolean;
    process: ProcessEntity;
    currentOpenVersion?: number;
    onClose: () => void;
    onNewDraft: (basis: {
        process: ProcessEntity;
        version: ProcessVersionEntity;
    }) => void | Promise<ProcessVersionEntity | void>;
    onDeleteVersion: (process: number, version: number) => void;
    onShouldReload?: (process: ProcessEntity) => void;
}

export function ProcessVersionsDialog(props: ProcessVersionsDialogProps) {
    const {
        open,
        process,
        currentOpenVersion,
        onClose,
        onNewDraft,
        onDeleteVersion,
        onShouldReload,
    } = props;

    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();
    const showExport = useProcessExport();

    const [isLoading, setIsLoading] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    const [versions, setVersions] = useState<ProcessVersionEntity[]>([]);

    const hasDraft = useMemo(() => {
        return versions.some(v => v.status === ProcessStatus.Drafted);
    }, [versions]);

    const publishedVersion = useMemo(() => {
        return versions.find(v => v.status === ProcessStatus.Published)?.processVersion ?? null;
    }, [versions]);

    const draftedVersion = useMemo(() => {
        if (versions.length > 0) {
            return versions.find(v => v.status === ProcessStatus.Drafted)?.processVersion ?? null;
        }

        return process.draftedVersion;
    }, [process.draftedVersion, versions]);

    // The parent process prop can be stale after dialog-local version changes, so derive version metadata from the loaded versions.
    const effectiveProcess = useMemo(() => {
        return {
            ...process,
            draftedVersion,
            publishedVersion: versions.length > 0 ? publishedVersion : process.publishedVersion,
            versionCount: versions.length > 0 ? versions.length : process.versionCount,
        };
    }, [draftedVersion, process, publishedVersion, versions.length]);

    const latestVersion = useMemo(() => {
        return versions.length > 0 ? versions[0].processVersion : null;
    }, [versions]);

    const [moreMenuAnchorEl, setMoreMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [moreMenuProcess, setMoreMenuProcess] = useState<ProcessVersionEntity | null>(null);

    const handleCloseMoreMenu = () => {
        setMoreMenuAnchorEl(null);
        setMoreMenuProcess(null);
    };

    const [versionToImport, setVersionToImport] = useState<ProcessExport | null>(null);

    const loadVersions = (): Promise<ProcessVersionEntity[]> => {
        return new ProcessDefinitionVersionApiService()
            .listAllOrdered('processVersion', 'DESC', {
                processId: process.id,
            })
            .then(({content}) => {
                setVersions(content);
                return content;
            })
            .catch(error => {
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Prozessversionen'));
                return versions;
            });
    };

    async function loadVersion(version: number): Promise<ProcessVersionEntity> {
        setIsBusy(true);
        try {
            const loadedVersions = await withDelay(loadVersions(), 500);

            const item = loadedVersions
                .find(v => v.processVersion === version);

            if (!item) {
                const error: ApiError = {
                    status: 404,
                    message: 'Die ausgewählte Prozessversion wurde nicht gefunden.',
                    details: null,
                    displayableToUser: true,
                };
                throw error;
            }

            return item;
        } finally {
            setIsBusy(false);
        }
    }

    useEffect(() => {
        setIsLoading(true);
        withDelay(loadVersions(), 600)
            .finally(() => {
                setIsLoading(false);
            });
    }, [process]);

    async function createDraftFromVersion(version: ProcessVersionEntity): Promise<void> {
        setIsBusy(true);
        try {
            await onNewDraft({
                process: effectiveProcess,
                version,
            });
            await withDelay(loadVersions(), 500);
        } finally {
            setIsBusy(false);
        }
    }

    async function handleUseAsNewDraft(version: number): Promise<void> {
        let item: ProcessVersionEntity;
        try {
            item = await loadVersion(version);
        } catch (error) {
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(error.message));
            } else {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Laden der Prozessversion'));
            }
            return;
        }

        if (effectiveProcess.draftedVersion == null) {
            await createDraftFromVersion(item);
            return;
        }

        const confirmed = await showConfirm({
            title: 'Bestehenden Entwurf überschreiben?',
            children: (
                <Typography>
                    Für diesen Prozess existiert bereits eine Arbeitsversion (Version {effectiveProcess.draftedVersion}).
                    Möchten Sie dennoch einen neuen Entwurf auf Basis dieser Version erstellen?
                    Die bestehende Arbeitsversion wird dabei überschrieben.
                </Typography>
            ),
            isDestructive: false,
        });

        if (confirmed) {
            await createDraftFromVersion(item);
        }
    }

    const handleNewDraft = (version: number) => {
        showConfirm({
            title: 'Neuen Entwurf anlegen?',
            confirmButtonText: 'Ja, Entwurf anlegen',
            children: (
                <Box>
                    Für diesen Prozess existiert derzeit kein aktiver Entwurf.
                    Möchten Sie einen neuen Entwurf (Arbeitsversion) für diesen Prozess anlegen um diesen zu
                    bearbeiten?
                </Box>
            ),
        }).then((confirmed) => {
            if (confirmed) {
                handleUseAsNewDraft(version);
            }
        });
    };

    async function handleUseAsNewProcess(version: number): Promise<void> {
        dispatch(setLoadingMessage({
            message: 'Die Prozessversion wird für den export vorbereitet',
            blocking: true,
            estimatedTime: 2000,
        }));

        new ProcessDefinitionApiService()
            .export(process.id, version)
            .then((exp) => {
                setVersionToImport({
                    ...exp,
                    process: {
                        ...exp.process,
                        internalTitle: process.internalTitle,
                    },
                });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Vorbereiten der Prozessdefinition'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });

        handleCloseMoreMenu();
    }

    const handleExportProcessVersion = (version: number) => {
        showExport(process.id, version);
        handleCloseMoreMenu();
    };

    async function handleDeleteProcessVersion(version: number): Promise<void> {
        let item: ProcessVersionEntity;
        try {
            item = await loadVersion(version);
        } catch (error) {
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(error.message));
            } else {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Laden der Prozessversion'));
            }
            return;
        }

        if (effectiveProcess.publishedVersion === item.processVersion) {
            dispatch(showErrorSnackbar('Die veröffentlichte Version kann nicht gelöscht werden.'));
            return;
        }

        if (item.status === ProcessStatus.Drafted && effectiveProcess.versionCount < 2) {
            showConfirm({
                title: 'Prozessversion löschen',
                hideCancelButton: true,
                children: (
                    <Typography>
                        Die letzte Version eines Prozesses kann nicht gelöscht werden.
                        <br/>
                        Bitte löschen Sie stattdessen den gesamten Prozess.
                    </Typography>
                ),
            });
            return;
        }

        showConfirm({
            title: 'Prozessversion löschen',
            children: (
                <Typography>
                    Soll die Prozessversion {item.processVersion} wirklich gelöscht werden?
                </Typography>
            ),
            isDestructive: true,
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                const processVersionsApi = new ProcessDefinitionVersionApiService();

                processVersionsApi
                    .destroy({
                        processDefinitionId: process.id,
                        processDefinitionVersion: item.processVersion,
                    })
                    .then(() => {
                        loadVersions();
                        handleCloseMoreMenu();
                        if (onShouldReload != null) {
                            onShouldReload(effectiveProcess);
                        }
                        onDeleteVersion(process.id, item.processVersion);
                    })
                    .catch(error => {
                        if (isApiError(error) && error.displayableToUser) {
                            dispatch(showErrorSnackbar(error.message));
                        } else {
                            dispatch(showErrorSnackbar('Fehler beim Löschen der Prozessversion'));
                        }
                        console.error(error);
                    });
            });
    }

    return (
        <>
            <Dialog
                open={open}
                onClose={onClose}
                fullWidth={true}
            >
                <DialogTitleWithClose
                    onClose={onClose}
                    actions={[
                        {
                            label: 'Entwurf anlegen',
                            icon: <NewWindow/>,
                            onClick: () => latestVersion != null && handleNewDraft(publishedVersion ?? latestVersion),
                            variant: 'text',
                            visible: !hasDraft && !isLoading,
                        },
                    ]}
                >
                    Versionen dieses Prozesses
                </DialogTitleWithClose>
                <DialogContent>
                    <LoadingOverlay
                        isLoading={isBusy}
                        message="Liste der Versionen wird aktualisiert"
                    />

                    {
                        isLoading && (
                            <List
                                role="status"
                                aria-live="polite"
                                aria-busy="true"
                                sx={{'& .MuiDivider-root:last-of-type': {display: 'none'}}}
                            >
                                {Array.from({length: 4}).map((_, i) => (
                                    <React.Fragment key={i}>
                                        <VersionListItemSkeleton/>
                                        <Divider sx={{my: 1.5}}/>
                                    </React.Fragment>
                                ))}
                            </List>
                        )
                    }
                    {
                        !isLoading &&
                        <List sx={{'& .MuiDivider-root:last-of-type': {display: 'none'}}}>
                            {
                                versions.map(ver => (
                                    <React.Fragment key={ver.processVersion}>
                                        <VersionListItem
                                            item={ver}
                                            currentOpenVersion={currentOpenVersion}
                                            onMoreClick={(target, item) => {
                                                setMoreMenuAnchorEl(target);
                                                setMoreMenuProcess(item);
                                            }}
                                        />
                                        <Divider sx={{my: 1.5}}/>
                                    </React.Fragment>
                                ))
                            }
                        </List>
                    }
                </DialogContent>
            </Dialog>

            {
                moreMenuAnchorEl != null &&
                moreMenuProcess != null &&
                <ProcessVersionsDialogRowMenu
                    anchorEl={moreMenuAnchorEl}
                    process={effectiveProcess}
                    processVersion={moreMenuProcess}
                    onClose={handleCloseMoreMenu}
                    onReuseVersionAsDraft={handleUseAsNewDraft}
                    onReuseVersionAsNewProcess={handleUseAsNewProcess}
                    onExportVersion={handleExportProcessVersion}
                    onDeleteVersion={handleDeleteProcessVersion}
                />
            }

            {
                versionToImport != null &&
                <NewProcessDialog
                    open={true}
                    onCancel={() => {
                        setVersionToImport(null);
                    }}
                    preselectedTemplate={versionToImport}
                />
            }
        </>
    );
}

interface VersionListItemProps {
    item: ProcessVersionEntity;
    currentOpenVersion?: number;
    onMoreClick: (target: HTMLButtonElement, item: ProcessVersionEntity) => void;
}


function VersionListItem(props: VersionListItemProps) {
    const {
        item,
        currentOpenVersion,
        onMoreClick,
    } = props;

    const {
        processVersion: version,
        status,
        updated,
        published,
        revoked,
        processId,
    } = item;

    const editorFullName = undefined; // TODO: find out, who edited this version the last time

    const subtext = useMemo(() => {
        const _format = (val: string | null | undefined) => {
            const fallback = updated != null ? new Date(updated) : new Date();
            return format(val ?? fallback, 'dd.MM.yyyy – HH:mm') + ' Uhr';
        };

        switch (status) {
            case ProcessStatus.Drafted:
                return `Zuletzt bearbeitet: ${_format(updated)}\nBearbeitet von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            case ProcessStatus.Published:
                return `Veröffentlicht am: ${_format(published)}\nVeröffentlicht von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            case ProcessStatus.Revoked:
                return `Zurückgezogen am: ${_format(revoked)}\nZurückgezogen von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
            default:
                return '';
        }
    }, [status, updated, revoked, published, editorFullName]);

    const Icon = useMemo(() => ProcessStatusIcons[status], [status]);

    return (
        <ListItem
            sx={{
                px: 0,
                display: 'flex',
                alignItems: 'start',
            }}
        >
            <Box sx={{width: 20, textAlign: 'center', mr: 2.5}}>
                <Icon/>
            </Box>
            <Box sx={{flexGrow: '1'}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap'}}>
                    <Typography
                        variant="h5"
                    >
                        <Link
                            style={{color: 'inherit', textDecoration: 'none'}}
                            to={`/processes/${processId}/versions/${version}`}
                            title={'Prozess bearbeiten'}
                        >
                            Version {version}
                        </Link>
                    </Typography>
                    {(status === ProcessStatus.Drafted || status === ProcessStatus.Published) && (
                        <Box sx={{ml: 'auto', mr: 0.5}}>
                            <ProcessStatusChip
                                status={status}
                                size="small"
                                variant="soft"
                            />
                        </Box>
                    )}
                </Box>
                <Typography
                    color="text.secondary"
                    sx={{mt: 0.5}}
                >
                    {subtext}
                </Typography>
            </Box>

            <Box
                sx={{
                    ml: 2,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    transform: 'translateY(-4px)',
                }}
            >
                <Actions
                    actions={[
                        {
                            icon: <Edit/>,
                            to: `/processes/${processId}/versions/${version}`,
                            tooltip: 'Version bearbeiten',
                            visible: !item.published && !revoked && item.processVersion !== currentOpenVersion,
                        },
                        {
                            icon: <Visibility/>,
                            to: `/processes/${processId}/versions/${version}`,
                            tooltip: 'Version ansehen',
                            visible: !!(published || revoked) && item.processVersion !== currentOpenVersion,
                        },
                        {
                            icon: <MoreVertOutlinedIcon/>,
                            onClick: (event) => {
                                onMoreClick(event.currentTarget as HTMLButtonElement, item);
                            },
                            tooltip: 'Optionen',
                        },
                    ]}
                    sx={{
                        justifyContent: 'end',
                    }}
                    dense
                />
            </Box>

        </ListItem>
    );
}

function VersionListItemSkeleton() {
    return (
        <ListItem
            sx={{
                px: 0,
                display: 'flex',
                alignItems: 'start',
            }}
        >
            <Box sx={{width: 24, textAlign: 'center', mr: 2}}>
                <Skeleton
                    variant="circular"
                    width={24}
                    height={24}
                    sx={{mt: 0.5}}
                />
            </Box>

            <Box sx={{flex: '1 1 auto', minWidth: 0}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap'}}>
                    <Skeleton
                        variant="text"
                        width={'40%'}
                        height={28}
                    />
                    <Box sx={{ml: 'auto'}}>
                        <Skeleton
                            variant="rectangular"
                            width={92}
                            height={24}
                            sx={{borderRadius: 999}}
                        />
                    </Box>
                </Box>

                <Skeleton
                    variant="text"
                    width={'35%'}
                    height={20}
                    sx={{mt: 0.5, mr: 0.5}}
                />
            </Box>

            <Box sx={{ml: 2, display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0}}>
                <Skeleton
                    variant="circular"
                    width={28}
                    height={28}
                />
                <Skeleton
                    variant="circular"
                    width={28}
                    height={28}
                />
            </Box>
        </ListItem>
    );
}
