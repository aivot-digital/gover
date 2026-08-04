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
import MoreVertOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {useConfirm} from '../../../providers/confirm-provider';
import {withDelay} from '../../../utils/with-delay';
import {ApiError, isApiError} from '../../../models/api-error';
import {Link} from 'react-router-dom';
import {Actions} from '../../../components/actions/actions';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {LoadingOverlay} from '../../../components/loading-overlay/loading-overlay';
import NewWindow from '@aivot/mui-material-symbols-400-n25-outlined/NewWindow';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
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
import {ProcessPublishDialog} from './process-publish-dialog';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
} from '../services/process-node-provider-api-service';
import {useRevokeProcessVersion} from '../hooks/use-revoke-process-version';
import {ProcessTestClaimApiService} from '../services/process-test-claim-api-service';
import {type ProcessTestClaimEntity} from '../entities/process-test-claim-entity';
import {type User} from '../../users/models/user';
import {UsersApiService} from '../../users/users-api-service';
import {resolveUserName} from '../../users/utils/resolve-user-name';

function deriveProcessFromVersions(
    process: ProcessEntity,
    versions: ProcessVersionEntity[],
): ProcessEntity {
    if (versions.length === 0) {
        return process;
    }

    return {
        ...process,
        draftedVersion: versions.find(v => v.status === ProcessStatus.Drafted)?.processVersion ?? null,
        publishedVersion: versions.find(v => v.status === ProcessStatus.Published)?.processVersion ?? null,
        versionCount: versions.length,
    };
}

interface ProcessVersionTestClaim {
    claim: ProcessTestClaimEntity;
    user: User | null;
}

interface ProcessVersionsDialogProps {
    open: boolean;
    process: ProcessEntity;
    currentOpenVersion?: number;
    currentTestClaim?: ProcessVersionTestClaim | null;
    onClose: () => void;
    onNewDraft: (basis: {
        process: ProcessEntity;
        version: ProcessVersionEntity;
    }) => void | Promise<ProcessVersionEntity | void>;
    onDeleteVersion: (process: number, version: number) => void;
    onShouldReload?: (process: ProcessEntity, currentOpenVersion?: ProcessVersionEntity) => void;
}

export function ProcessVersionsDialog(props: ProcessVersionsDialogProps) {
    const {
        open,
        process,
        currentOpenVersion,
        currentTestClaim,
        onClose,
        onNewDraft,
        onDeleteVersion,
        onShouldReload,
    } = props;

    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();
    const showExport = useProcessExport();
    const revokeProcessVersion = useRevokeProcessVersion();

    const [isLoading, setIsLoading] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    const [versions, setVersions] = useState<ProcessVersionEntity[]>([]);
    const [testClaims, setTestClaims] = useState<ProcessVersionTestClaim[]>([]);
    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);
    const [hasLoadedNodeProviders, setHasLoadedNodeProviders] = useState(false);
    const [versionToPublish, setVersionToPublish] = useState<ProcessVersionEntity | null>(null);

    const hasDraft = useMemo(() => {
        return versions.some(v => v.status === ProcessStatus.Drafted);
    }, [versions]);

    const publishedVersion = useMemo(() => {
        return versions.find(v => v.status === ProcessStatus.Published)?.processVersion ?? null;
    }, [versions]);

    // The parent process prop can be stale after dialog-local version changes, so derive version metadata from the loaded versions.
    const effectiveProcess = useMemo(() => {
        return deriveProcessFromVersions(process, versions);
    }, [process, versions]);

    const latestVersion = useMemo(() => {
        return versions.length > 0 ? versions[0].processVersion : null;
    }, [versions]);

    const testClaimByVersion = useMemo(() => {
        const claimMap = new Map<number, ProcessVersionTestClaim>();

        for (const testClaim of testClaims) {
            claimMap.set(testClaim.claim.processVersion, testClaim);
        }

        // Overlay the detail page state so a newly started test is visible before the dialog reloads its own claim list.
        if (currentTestClaim != null && currentTestClaim.claim.processId === process.id) {
            claimMap.set(currentTestClaim.claim.processVersion, currentTestClaim);
        }

        return claimMap;
    }, [currentTestClaim, process.id, testClaims]);

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

    const loadTestClaims = (): Promise<ProcessVersionTestClaim[]> => {
        return new ProcessTestClaimApiService()
            .listAll({
                processId: process.id,
            })
            .then(async ({content}) => {
                const usersById = new Map<string, User | null>();
                const userIds = Array.from(new Set(content.map((claim) => claim.owningUserId)));

                await Promise.all(userIds.map(async (userId) => {
                    try {
                        usersById.set(userId, await new UsersApiService().retrieve(userId));
                    } catch (error) {
                        usersById.set(userId, null);
                    }
                }));

                const testClaimContexts = content.map((claim) => ({
                    claim,
                    user: usersById.get(claim.owningUserId) ?? null,
                }));

                setTestClaims(testClaimContexts);
                return testClaimContexts;
            })
            .catch(error => {
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Testansprüche'));
                return testClaims;
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

    function notifyVersionsChanged(loadedVersions: ProcessVersionEntity[]): void {
        if (onShouldReload == null) {
            return;
        }

        const currentVersion = currentOpenVersion == null
            ? undefined
            : loadedVersions.find(v => v.processVersion === currentOpenVersion);

        onShouldReload(deriveProcessFromVersions(process, loadedVersions), currentVersion);
    }

    async function ensureAvailableNodeProviders(): Promise<void> {
        if (hasLoadedNodeProviders) {
            return;
        }

        const nodeProviders = await new ProcessNodeProviderApiService()
            .getNodeProviders();

        setAvailableNodeProviders(nodeProviders);
        setHasLoadedNodeProviders(true);
    }

    useEffect(() => {
        if (!open) {
            return;
        }

        setIsLoading(true);
        withDelay(Promise.all([
            loadVersions(),
            loadTestClaims(),
        ]), 600)
            .finally(() => {
                setIsLoading(false);
            });
    }, [open, process.id]);

    useEffect(() => {
        if (currentTestClaim !== null || currentOpenVersion == null) {
            return;
        }

        // A null detail-page claim only proves that the opened version is no longer in test mode.
        // Keep claims for other versions that were loaded by the dialog itself.
        setTestClaims((previousTestClaims) => previousTestClaims.filter((testClaim) => (
            testClaim.claim.processId !== process.id ||
            testClaim.claim.processVersion !== currentOpenVersion
        )));
    }, [currentOpenVersion, currentTestClaim, process.id]);

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

    async function handlePublishProcessVersion(version: number): Promise<void> {
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

        setIsBusy(true);
        try {
            await ensureAvailableNodeProviders();
            setVersionToPublish(item);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die verfügbaren Prozesselemente konnten nicht geladen werden.'));
        } finally {
            setIsBusy(false);
        }
    }

    async function handlePublishedProcessVersion(_publishedVersion: ProcessVersionEntity): Promise<void> {
        setVersionToPublish(null);
        setIsBusy(true);
        try {
            const loadedVersions = await withDelay(loadVersions(), 500);
            notifyVersionsChanged(loadedVersions);
        } finally {
            setIsBusy(false);
        }
    }

    async function handleRevokeProcessVersion(version: number): Promise<void> {
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

        const revokedVersion = await revokeProcessVersion(effectiveProcess, item);

        if (revokedVersion == null) {
            return;
        }

        setIsBusy(true);
        try {
            const loadedVersions = await withDelay(loadVersions(), 500);
            notifyVersionsChanged(loadedVersions);
        } finally {
            setIsBusy(false);
        }
    }

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
                    .then(async () => {
                        const loadedVersions = await loadVersions();
                        handleCloseMoreMenu();
                        notifyVersionsChanged(loadedVersions);
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
                                            testClaim={testClaimByVersion.get(ver.processVersion) ?? null}
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
                    lifecycleActionsDisabled={testClaimByVersion.has(moreMenuProcess.processVersion)}
                    lifecycleActionsDisabledReason="Diese Prozessversion befindet sich aktuell im Test."
                    onClose={handleCloseMoreMenu}
                    onPublishVersion={handlePublishProcessVersion}
                    onRevokeVersion={handleRevokeProcessVersion}
                    onReuseVersionAsDraft={handleUseAsNewDraft}
                    onReuseVersionAsNewProcess={handleUseAsNewProcess}
                    onExportVersion={handleExportProcessVersion}
                    onDeleteVersion={handleDeleteProcessVersion}
                />
            }

            {
                versionToPublish != null &&
                <ProcessPublishDialog
                    open={true}
                    process={effectiveProcess}
                    version={versionToPublish}
                    availableNodeProviders={availableNodeProviders}
                    onClose={() => {
                        setVersionToPublish(null);
                    }}
                    onPublish={handlePublishedProcessVersion}
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
    testClaim: ProcessVersionTestClaim | null;
    onMoreClick: (target: HTMLButtonElement, item: ProcessVersionEntity) => void;
}


function VersionListItem(props: VersionListItemProps) {
    const {
        item,
        currentOpenVersion,
        testClaim,
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
    const testClaimOwnerName = useMemo(() => {
        if (testClaim == null) {
            return null;
        }

        return testClaim.user != null ? resolveUserName(testClaim.user) : 'Unbekannte Mitarbeiter:in';
    }, [testClaim]);

    const subtext = useMemo(() => {
        const _format = (val: string | null | undefined) => {
            const fallback = updated != null ? new Date(updated) : new Date();
            return format(val ?? fallback, 'dd.MM.yyyy – HH:mm') + ' Uhr';
        };

        let statusText = '';

        switch (status) {
            case ProcessStatus.Drafted:
                statusText = `Zuletzt bearbeitet: ${_format(updated)}\nBearbeitet von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
                break;
            case ProcessStatus.Published:
                statusText = `Veröffentlicht am: ${_format(published)}\nVeröffentlicht von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
                break;
            case ProcessStatus.Revoked:
                statusText = `Zurückgezogen am: ${_format(revoked)}\nZurückgezogen von: ${editorFullName ?? 'Unbekannte Nutzer:in'}`;
                break;
        }

        return statusText;
    }, [status, updated, revoked, published, editorFullName]);

    const Icon = useMemo(() => ProcessStatusIcons[status], [status]);
    const isCurrentOpenVersion = item.processVersion === currentOpenVersion;
    const showStatusChip = status === ProcessStatus.Drafted || status === ProcessStatus.Published;

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
                        sx={{
                            display: 'flex',
                            alignItems: 'baseline',
                            gap: 1,
                            flexWrap: 'wrap',
                        }}
                    >
                        <Link
                            style={{color: 'inherit', textDecoration: 'none'}}
                            to={`/processes/${processId}/versions/${version}`}
                            title={'Prozess bearbeiten'}
                        >
                            Version {version}
                        </Link>
                        {
                            isCurrentOpenVersion &&
                            <Typography
                                component="span"
                                color="text.secondary"
                                sx={{
                                    fontSize: '0.875rem',
                                    fontWeight: 400,
                                }}
                            >
                                (aktuell geöffnet)
                            </Typography>
                        }
                    </Typography>
                    {
                        showStatusChip &&
                        <Box
                            sx={{
                                ml: 'auto',
                                mr: 0.5,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 0.75,
                                flexShrink: 0,
                            }}
                        >
                            <ProcessStatusChip
                                status={status}
                                size="small"
                                variant="soft"
                            />
                        </Box>
                    }
                </Box>
                <Typography
                    color="text.secondary"
                    sx={{
                        mt: 0.5,
                        whiteSpace: 'pre-line',
                    }}
                >
                    {subtext}
                </Typography>
                {
                    testClaimOwnerName != null &&
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 0.5,
                        }}
                    >
                        <Typography
                            component="span"
                            sx={{
                                color: 'text.secondary',
                            }}
                        >
                            Im Test durch:
                        </Typography>
                        <Typography
                            component="span"
                            sx={{
                                color: 'text.secondary',
                            }}
                        >
                            {testClaimOwnerName}
                        </Typography>
                        <Science
                            sx={{
                                color: 'warning.main',
                                fontSize: 16,
                                flexShrink: 0,
                            }}
                        />
                    </Box>
                }
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
