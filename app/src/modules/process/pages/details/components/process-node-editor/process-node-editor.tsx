import {validateDataKeyPath, validateNodeDataKey} from '../../../../../../utils/data-key-input';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import React, {type ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {Alert, Box, Button, IconButton, Tab, Tabs, useTheme} from '@mui/material';
import {alpha, keyframes} from '@mui/material/styles';
import {Link, Outlet, useLocation, useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useProcessDetailsPageContext} from '../../process-details-page-context';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
} from '../../../../services/process-node-provider-api-service';
import {ProcessNodeEditorProvider} from './process-node-editor-context';
import Typography from '@mui/material/Typography';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useChangeBlocker} from '../../../../../../hooks/use-change-blocker-2';
import {ProcessNodeEditorMenu} from './components/process-node-editor-menu';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {getNodeName} from '../process-flow-editor/utils/node-utils';
import {isApiError} from '../../../../../../models/api-error';
import {useEditorDraft} from '../../../../../../hooks/use-editor-draft';
import {useEditorDraftVisit} from '../../../../../../hooks/use-editor-draft-visit';
import {useAppSelector} from '../../../../../../hooks/use-app-selector';
import {selectUser} from '../../../../../../slices/user-slice';
import {useMergeConflictResolution} from '../../../../../../hooks/use-merge-conflict-resolution';
import {createProcessNodeMergeSpec, getExternallyEditedFields} from '../../../../utils/process-node-merge-spec';
import {
    type EditorSaveCheckpoint,
    canPendingSaveStillCommit,
    pendingSaveCanChangeStored,
    prepareEditorDraftRecovery,
    persistEditorSave,
    signInToSaveEditorMessage,
    rebaseAfterSave,
} from '../../../../../../utils/editor-save-rebase';
import {editorValuesEqual} from '../../../../../../utils/variable-reference-merge';
import {useCurrentState} from '../../../../../../hooks/use-current-state';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../../../hooks/use-app-dispatch';
import {ProcessNodeEditorSkeleton} from './process-node-editor-skeleton';
import {clearLoadingMessage, setLoadingMessage} from '../../../../../../slices/shell-slice';
import {useDelayedVisibility} from '../../../../../../hooks/use-delayed-visibility';
import {downloadObjectFile} from '../../../../../../utils/download-utils';
import {ProcessNodeProblems} from '../../../../entities/process-node-problems';
import {isDerivedRuntimeElementData} from '../../../../../../models/element-data';
import {isProcessNodeEditorTabNavigation, shouldSkipProcessNodeEditorChangeBlocker} from './process-node-editor-change-blocker';
import {type ProcessNodeDefinitionMetadata} from '../../../../entities/process-node-definition-metadata';
import {
    getProcessNodeProviderIcon,
    ProcessNodeProviderDetailsDialog,
} from '../../../../components/process-node-provider-details';
import {ProcessNodeProviderBadges} from '../../../../components/process-node-provider-badges';

type IncomingMetadataState =
    | {status: 'loading'}
    | {status: 'loaded'; metadata: ProcessNodeDefinitionMetadata}
    | {status: 'failed'};

const PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY = 150;
const MERGED_SAVE_MESSAGE = 'Ihre Änderungen wurden mit dem zwischenzeitlich gespeicherten Stand zusammengeführt und gespeichert.';
const PROCESS_NODE_EDITOR_LOADED_FEEDBACK_DURATION = 1200;
const processNodeEditorLoadedSidebarFlashLight = keyframes`
    0% {
        background-color: ${'#f6f6f6'};
    }
    100% {
        background-color: transparent;
    }
`;
const processNodeEditorLoadedSidebarFlashDark = keyframes`
    0% {
        background-color: ${'rgba(0, 0, 0, 0.34)'};
    }
    100% {
        background-color: transparent;
    }
`;

export function ProcessNodeEditor(): ReactNode {
    const params = useParams();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const location = useLocation();
    const confirm = useConfirm();
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const userId = useAppSelector(selectUser)?.id;

    const [originalNode, setOriginalNode, currentOriginal] = useCurrentState<ProcessNodeEntity | null>(null);
    // Replaced together with the node so the suggestions always belong to the configuration on screen.
    const [incomingMetadataState, setIncomingMetadataState] = useState<IncomingMetadataState>({status: 'loading'});

    const {
        editable,
        structureEditable,
        onSave,
        onStoredNode,
        onDelete,
        onStartReplaceNode,
        nodeRefreshSignal,
        testClaim,
    } = useProcessDetailsPageContext();

    const [dataKeyUnlocked, setDataKeyUnlocked] = useState(false);
    const [dataKeyValidationRequested, setDataKeyValidationRequested] = useState(false);
    const [outputMappingErrors, setOutputMappingErrors] = useState<Record<string, string>>({});
    const [provider, setProvider] = useState<ProcessNodeProvider | null>(null);
    const [editedNode, setEditedNode, currentEdit] = useCurrentState<ProcessNodeEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [isProviderDetailsDialogOpen, setIsProviderDetailsDialogOpen] = useState(false);
    const [isNodeLoading, setIsNodeLoading] = useState(false);
    const [showNodeLoadedFeedback, setShowNodeLoadedFeedback] = useState(false);
    const [problems, setProblems] = useState<ProcessNodeProblems | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const activeSave = useRef<AbortController | null>(null);
    const [saveCheckpoint, setSaveCheckpoint, currentCheckpoint] = useCurrentState<EditorSaveCheckpoint<ProcessNodeEntity> | undefined>(undefined);
    const validatingNode = useRef<ProcessNodeEntity | null>(null);
    const keyLockRevision = useRef(0);
    const {
        resolvePrepared,
        dialog: mergeConflictDialog,
    } = useMergeConflictResolution();

    const hasEditorContent = originalNode != null && layout != null && provider != null;
    const showNodeLoadingOverlay = useDelayedVisibility(isNodeLoading, PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY);
    const showInitialNodeEditorSkeleton = useDelayedVisibility(!hasEditorContent, PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY);
    const hasShownShellLoadingRef = useRef(false);
    const nodeEditorLoadedSidebarFlash = theme.palette.mode === 'dark'
        ? processNodeEditorLoadedSidebarFlashDark
        : processNodeEditorLoadedSidebarFlashLight;

    const nodeId = (() => {
        const rawNodeId = params.nodeId;
        if (rawNodeId == null) {
            throw new Error('nodeId is required');
        }
        return parseInt(rawNodeId, 10);
    })();
    const nodeRefreshVersion = nodeRefreshSignal.nodeId === nodeId ? nodeRefreshSignal.version : 0;
    // A change of editor tab keeps the existing visit. Revisiting the same node starts a new one.
    const request = useMemo(() => ({
        userId, nodeId, processId: params.processId, processVersion: params.processVersion, nodeRefreshVersion, editable,
    }), [userId, nodeId, params.processId, params.processVersion, nodeRefreshVersion, editable]);
    const [loaded, setLoaded] = useState<{request: typeof request; generation: number} | null>(null);
    const matchesRequest = loaded?.request === request && originalNode?.id === nodeId &&
        originalNode.processId === Number(params.processId) && originalNode.processVersion === Number(params.processVersion);
    const visitKey = matchesRequest && editable && userId != null ? JSON.stringify([userId, nodeId, loaded.generation]) : null;
    const {checked, isCurrent, finish, canEdit} = useEditorDraftVisit(visitKey);
    const previewLocked = !matchesRequest || (editable && !checked);

    const cancelActiveSave = useCallback(() => {
        validatingNode.current = null;
        if (activeSave.current != null) {
            activeSave.current.abort();
            activeSave.current = null;
            setIsSaving(false);
        }
    }, []);

    // A new load owns its editor even when revisiting the same node. Its internal tabs share the current save.
    useLayoutEffect(() => cancelActiveSave, [cancelActiveSave, request]);

    const hasNodeChanges = useMemo(() => !editorValuesEqual(originalNode, editedNode), [originalNode, editedNode]);
    const {
        hasChanged,
        dialog: changeBlockerDialog,
    } = useChangeBlocker({
        visitKey,
        original: false,
        // Even a return to the old stored value needs protecting until the PUT's result is known.
        edited: checked && (saveCheckpoint?.phase === 'pending' || hasNodeChanges),
        onConfirmNavigation: async () => {
            if (!canEdit()) return;
            cancelActiveSave();
            await discardForNavigation();
            if (canEdit() && currentEdit.current === editedNode) {
                setEditedNode(originalNode);
                setSaveCheckpoint(undefined);
            }
        },
        // Tab changes keep the same editing session. Do not persist a bypass in history state: revisiting that
        // entry from a different node would otherwise discard its unsaved changes without confirmation.
        shouldAllowNavigation: ({currentLocation, nextLocation}) =>
            isProcessNodeEditorTabNavigation(currentLocation.pathname, nextLocation.pathname) ||
            shouldSkipProcessNodeEditorChangeBlocker(nextLocation.state),
    });

    useLayoutEffect(() => {
        let isCancelled = false;
        const hasEditorContent = originalNode != null && layout != null && provider != null;

        if (!hasEditorContent) {
            setOriginalNode(null);
            setEditedNode(null);
            setLayout(null);
            setProvider(null);
            setIncomingMetadataState({status: 'loading'});
        }

        setIsNodeLoading(true);
        setShowNodeLoadedFeedback(false);
        setMenuAnchorEl(null);
        setIsProviderDetailsDialogOpen(false);

        (async () => {
            const [node, configurationLayout, problems, incomingMetadataState] = await Promise.all([
                new ProcessNodeApiService().retrieve(nodeId),
                new ProcessNodeApiService().getConfigurationLayout(nodeId),
                new ProcessNodeApiService().validate(nodeId),
                // Suggestions are optional, so their failure must not keep the configuration from opening.
                new ProcessNodeApiService().getIncomingMetadata(nodeId)
                    .then((metadata): IncomingMetadataState => ({status: 'loaded', metadata}))
                    .catch((error): IncomingMetadataState => {
                        console.error(error);
                        return {status: 'failed'};
                    }),
            ]);
            const nodeProvider = await new ProcessNodeProviderApiService()
                .getNodeProvider(node.processNodeDefinitionKey, node.processNodeDefinitionVersion);

            return {
                node,
                configurationLayout,
                nodeProvider,
                problems,
                incomingMetadataState,
            };
        })()
            .then(({node, configurationLayout, nodeProvider, problems, incomingMetadataState}) => {
                if (isCancelled) {
                    return;
                }
                setDataKeyUnlocked(false);
                setDataKeyValidationRequested(false);
                setOutputMappingErrors({});
                setOriginalNode(node);
                setEditedNode(node);
                setSaveCheckpoint(undefined);
                setLoaded((previous) => ({request, generation: (previous?.generation ?? 0) + 1}));
                setLayout(configurationLayout);
                setProvider(nodeProvider);
                setProblems(problems);
                setIncomingMetadataState(incomingMetadataState);
                if (hasEditorContent) {
                    setShowNodeLoadedFeedback(true);
                }
            })
            .catch((error) => {
                if (isCancelled) {
                    return;
                }
                dispatch(showApiErrorSnackbar(error, 'Die Details für das Prozesselement konnten nicht geladen werden.'));
            })
            .finally(() => {
                if (isCancelled) {
                    return;
                }

                setIsNodeLoading(false);
            });

        return () => {
            isCancelled = true;
        };
    }, [request]);

    useEffect(() => {
        if (!showNodeLoadingOverlay) {
            if (hasShownShellLoadingRef.current) {
                dispatch(clearLoadingMessage());
                hasShownShellLoadingRef.current = false;
            }
            return;
        }

        hasShownShellLoadingRef.current = true;
        dispatch(setLoadingMessage({
            message: 'Prozesselement wird geladen',
            blocking: false,
            estimatedTime: 500,
        }));

        return () => {
            if (hasShownShellLoadingRef.current) {
                dispatch(clearLoadingMessage());
                hasShownShellLoadingRef.current = false;
            }
        };
    }, [dispatch, showNodeLoadingOverlay]);

    useEffect(() => {
        if (!showNodeLoadedFeedback) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            setShowNodeLoadedFeedback(false);
        }, PROCESS_NODE_EDITOR_LOADED_FEEDBACK_DURATION);

        return () => {
            window.clearTimeout(timeoutId);
        };
    }, [showNodeLoadedFeedback]);

    const {
        loadDraft,
        clearDraft,
        discardForNavigation,
        draftLogoutSignal, isDraftRecoveryCurrent, beginManualSave, confirmManualSave,
        isReady: isDraftStorageReady,
    } = useEditorDraft<ProcessNodeEntity>({
        draftKey: visitKey != null ? `process-node/${nodeId}/configuration` : null,
        base: originalNode,
        baseLockVersion: originalNode?.lockVersion,
        edited: editedNode,
        saveCheckpoint,
        hasChanged,
        enabled: checked,
    });

    // Offers the unsaved changes of an earlier visit after each load, e.g. after the session expired.
    useLayoutEffect(() => {
        if (originalNode == null || !isCurrent() || !isDraftStorageReady) {
            return;
        }
        const controller = new AbortController();
        const signal = draftLogoutSignal ? AbortSignal.any([controller.signal, draftLogoutSignal]) : controller.signal;
        if (signal.aborted) {
            finish();
            return;
        }
        draftLogoutSignal?.addEventListener('abort', finish);
        (async () => {
            const draft = await loadDraft();
            if (draft == null || signal.aborted || !isCurrent()) {
                return;
            }

            const spec = createProcessNodeMergeSpec(layout, originalNode);
            const pending = draft.saveCheckpoint;
            const putCanStillChangeNode = canPendingSaveStillCommit(pending, draft.baseLockVersion, originalNode.lockVersion) &&
                pendingSaveCanChangeStored(pending, originalNode, spec);
            const outcome = await resolvePrepared({
                merge: (choices) => {
                    const merged = prepareEditorDraftRecovery(draft, originalNode, spec, choices);
                    return putCanStillChangeNode ? {...merged, changed: true} : merged;
                },
                mode: 'restore',
                signal,
                storedStateChanged: draft.baseLockVersion !== (originalNode.lockVersion ?? null),
            });
            if (!await isDraftRecoveryCurrent() || signal.aborted || !isCurrent()) {
                return;
            }
            if (outcome.status === 'apply') {
                setEditedNode(outcome.value);
                if (putCanStillChangeNode) setSaveCheckpoint(pending);
            } else {
                // Discarded, or nothing left to restore because the changes were saved elsewhere.
                await clearDraft().catch(() => {
                    if (!signal.aborted && isCurrent()) {
                        dispatch(showErrorSnackbar('Die zwischengespeicherten Änderungen konnten nicht gelöscht werden.'));
                    }
                });
            }
        })()
            .catch(console.error)
            .finally(() => {
                if (!controller.signal.aborted) {
                    finish();
                }
            });
        return () => {
            controller.abort();
            draftLogoutSignal?.removeEventListener('abort', finish);
        };
    }, [isCurrent, finish, isDraftStorageReady, loadDraft, draftLogoutSignal, isDraftRecoveryCurrent]);

    const {
        Icon: TypeIcon,
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = useMemo(() => {
        return provider == null ?
            {
                Icon: () => null,
                label: '',
                bgColor: theme.palette.background.paper,
                textColor: theme.palette.text.primary,
            } :
            ProviderTypeStyles[provider.type];
    }, [provider, theme.palette.background.paper, theme.palette.text.primary]);

    const ProviderIcon = useMemo(() => {
        if (provider == null) {
            return TypeIcon;
        }

        return getProcessNodeProviderIcon(provider);
    }, [provider, TypeIcon]);

    const currentTab = useMemo(() => {
        if (location.pathname.endsWith('/tabs/outputs')) {
            return 'outputs';
        } else if (location.pathname.endsWith('/tabs/more')) {
            return 'more';
        } else if (location.pathname.endsWith('/tabs/testing')) {
            return 'testing';
        }
        return 'configuration';
    }, [location]);
    const tabUsesIncomingMetadata = currentTab === 'configuration' || currentTab === 'outputs';

    const editorTabContentContainerRef = useRef<HTMLDivElement | null>(null);
    useEffect(() => {
        if (editorTabContentContainerRef.current == null) {
            return;
        }
        editorTabContentContainerRef.current.scrollTo({
            top: 0,
            behavior: 'smooth',
        });
    }, [location]);

    const canSave = checked && hasChanged && !isNodeLoading && editedNode?.id === nodeId;
    const handleSaveSelected = async (): Promise<void> => {
        // Guards against a second click while the first save is still running, which would end in a needless conflict.
        const submitted = currentEdit.current;
        if (!canSave || !canEdit() || originalNode == null || submitted?.id !== nodeId || activeSave.current != null) {
            return;
        }

        setDataKeyValidationRequested(true);
        const invalidMappings = Object.fromEntries(Object.entries(submitted.outputMappings ?? {})
            .filter(([key]) => provider?.outputs.some((output) => output.key === key))
            .map(([key, path]) => [key, validateDataKeyPath(path)])
            .filter((entry): entry is [string, string] => entry[1] != null));
        setOutputMappingErrors(invalidMappings);
        if (validateNodeDataKey(submitted.dataKey) != null || Object.keys(invalidMappings).length > 0) {
            navigate(`/processes/${params.processId}/versions/${params.processVersion}/nodes/${submitted.id}/tabs/outputs?${searchParams.toString()}`);
            return;
        }

        // Once sent, a PUT can still commit. Its response updates the graph even if this editor has moved on.
        const controller = new AbortController();
        activeSave.current = controller;
        validatingNode.current = null;
        const lockRevisionAtClick = keyLockRevision.current;
        const applyConfirmed = (stored: ProcessNodeEntity) => {
            const working = currentEdit.current!;
            const rebased = rebaseAfterSave(submitted, working, stored, createProcessNodeMergeSpec(layout, stored));
            setOriginalNode(stored);
            setEditedNode(rebased.value);
            setSaveCheckpoint(rebased.checkpoint);
            setProblems(null);
            if (rebased.checkpoint || !editorValuesEqual(rebased.value, stored)) return;
            if (keyLockRevision.current === lockRevisionAtClick) setDataKeyUnlocked(false);
            setDataKeyValidationRequested(false);
            setOutputMappingErrors({});

            // Validation describes the confirmed node. A subsequent save may start while it runs.
            const checked = rebased.value;
            validatingNode.current = checked;
            void new ProcessNodeApiService().validate(stored.id).then((updatedProblems) => {
                if (validatingNode.current === checked && currentEdit.current === checked) setProblems(updatedProblems);
            }).catch((error) => {
                if (validatingNode.current === checked && currentEdit.current === checked) console.error(error);
            });
        };

        // Only show backend field errors for the PUT that produced them.
        let attemptedNode: ProcessNodeEntity | undefined;
        let signal = controller.signal;
        setIsSaving(true);
        try {
            const token = await beginManualSave();
            if (signal.aborted) return;
            if (!token) {
                dispatch(showErrorSnackbar(signInToSaveEditorMessage));
                return;
            }
            signal = AbortSignal.any([signal, token.signal]);
            const fieldsToOmit = getExternallyEditedFields(layout);
            const result = await persistEditorSave<ProcessNodeEntity, ProcessNodeEntity>({
                original: originalNode,
                submitted,
                checkpoint: saveCheckpoint,
                read: () => {
                    attemptedNode = undefined;
                    return new ProcessNodeApiService().retrieve(nodeId);
                },
                write: (base, value, reserveVersion) => {
                    attemptedNode = reserveVersion ? base : value;
                    return onSave(attemptedNode, {query: {omitConfigSave: fieldsToOmit}, abortBeforeSend: signal});
                },
                valueOf: (stored) => stored,
                specFor: (stored) => createProcessNodeMergeSpec(layout, stored),
                setPending: setSaveCheckpoint,
                resolvePrepared,
                onRead: onStoredNode,
                signal,
                requireServerConfirmation: token.resumesDrafts,
            });
            signal.throwIfAborted();
            if (result.status === 'cancel') {
                setSaveCheckpoint(saveCheckpoint);
                return;
            }
            if (result.status === 'unknown') {
                dispatch(showApiErrorSnackbar(result.error, 'Das Prozesselement konnte nicht gespeichert werden.'));
                return;
            }
            applyConfirmed(result.stored);
            if (result.status === 'discarded') return;
            await confirmManualSave(token);
            if (signal.aborted) return;
            dispatch(showSuccessSnackbar(result.merged ? MERGED_SAVE_MESSAGE : 'Das Element wurde erfolgreich gespeichert.'));
        } catch (err) {
            if (signal.aborted) {
                return;
            }
            setSaveCheckpoint(saveCheckpoint);
            if (attemptedNode && isApiError(err) && err.status === 400) {
                if (currentEdit.current === submitted && editorValuesEqual(attemptedNode.configuration, submitted.configuration) &&
                    isDerivedRuntimeElementData(err.details)) {
                    setProblems({
                        node: attemptedNode, derivedRuntimeElementData: err.details, commonErrors: {}, problems: [],
                    });
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Das Prozesselement konnte nicht gespeichert werden, da die Konfiguration ungültig ist.'));
                }
            } else {
                dispatch(showApiErrorSnackbar(err, 'Das Prozesselement konnte nicht gespeichert werden.'));
            }
        } finally {
            if (activeSave.current === controller) {
                activeSave.current = null;
                setIsSaving(false);
            }
        }
    };

    const handleDeleteSelected = (): void => {
        if (!structureEditable || !canEdit() || originalNode == null || provider == null) {
            return;
        }

        confirm({
            isDestructive: true,
            title: 'Prozesselement löschen',
            children: (
                <>
                    <Typography>
                        Möchten Sie das Prozesselement <strong>{getNodeName(originalNode, provider)}</strong> wirklich
                        löschen?
                    </Typography>
                </>
            ),
        })
            .then((confirm) => {
                if (confirm && canEdit()) {
                    setEditedNode(originalNode);
                    onDelete(originalNode);
                }
            })
            .catch((err) => {
                dispatch(showErrorSnackbar('Das Prozesselement konnte nicht gelöscht werden.'));
                console.error(err);
            });
    };

    const handleExportSelected = (): void => {
        if (!matchesRequest || originalNode == null || provider == null) {
            return;
        }

        new ProcessNodeApiService()
            .export(originalNode.id)
            .then((exp) => {
                downloadObjectFile(`${getNodeName(originalNode, provider)}.node.prosuna.json`, exp);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Das Prozesselement konnte nicht exportiert werden.'));
            });
    };

    if (!hasEditorContent || loaded?.request.userId !== userId) {
        return showInitialNodeEditorSkeleton ?
            <ProcessNodeEditorSkeleton/> :
            <Box sx={{height: '100vh'}}/>;
    }

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    height: '100vh',
                    position: 'relative',
                    animation: showNodeLoadedFeedback ? `${nodeEditorLoadedSidebarFlash} ${PROCESS_NODE_EDITOR_LOADED_FEEDBACK_DURATION}ms ease-out` : 'none',
                }}
            >
                {
                    showNodeLoadingOverlay &&
                    <Box
                        sx={{
                            position: 'absolute',
                            inset: 0,
                            zIndex: 2,
                            pointerEvents: 'none',
                            bgcolor: theme.palette.mode === 'dark'
                                ? alpha(theme.palette.common.black, 0.42)
                                : alpha(theme.palette.common.white, 0.42),
                            backdropFilter: 'blur(1.5px)',
                        }}
                    />
                }
                <Box
                    sx={{
                        flex: 1,
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                        opacity: showNodeLoadingOverlay ? 0.78 : 1,
                        transition: 'opacity 140ms ease',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 2,
                            px: 2,
                            pt: 1.5,
                            pb: 0.5,
                            minWidth: 0,
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: 38,
                                height: 38,
                                minWidth: 38,
                                minHeight: 38,
                                aspectRatio: '1 / 1',
                                flexShrink: 0,
                                borderRadius: '50%',
                                backgroundColor: typeBgColor,
                                color: typeTextColor,
                            }}
                        >
                            <ProviderIcon/>
                        </Box>

                        <Box
                            sx={{
                                flex: 1,
                                minWidth: 0,
                            }}
                        >
                            <Typography
                                variant="caption"
                                sx={{
                                    display: 'block',
                                    lineHeight: 1.2,
                                    mt: 0.5,
                                }}
                            >
                                {typeLabel}
                            </Typography>

                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 1,
                                    minWidth: 0,
                                }}
                            >
                                <Typography
                                    component="div"
                                    title={provider.name}
                                    sx={{
                                        fontWeight: "bold",
                                        flex: 1,
                                        minWidth: 0,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap'
                                    }}>
                                    {provider.name}
                                </Typography>
                                <ProcessNodeProviderBadges
                                    provider={provider}
                                    onShowDeprecationDetails={() => {
                                        setIsProviderDetailsDialogOpen(true);
                                    }}
                                />
                            </Box>
                        </Box>

                        <IconButton
                            disabled={!matchesRequest}
                            sx={{
                                marginLeft: 'auto',
                                flexShrink: 0,
                            }}
                            onClick={(event) => {
                                setMenuAnchorEl(event.currentTarget);
                            }}
                        >
                            <MoreVert/>
                        </IconButton>
                    </Box>

                    <Tabs
                        value={currentTab}
                        onChange={(_, value) => {
                            navigate(`/processes/${params.processId}/versions/${params.processVersion}/nodes/${nodeId}/tabs/${value}?${searchParams.toString()}`);
                        }}
                        sx={{
                            mt: 0.25,
                            borderBottom: '1px solid',
                            borderBottomColor: 'divider',
                        }}
                    >
                        <Tab
                            label="Eigenschaften"
                            value="configuration"
                        />

                        <Tab
                            label="Ausgangsdaten"
                            value="outputs"
                        />

                        <Tab
                            label="Weiteres"
                            value="more"
                        />

                        {
                            testClaim != null &&
                            <Tab
                                label="Testen"
                                value="testing"
                            />
                        }
                    </Tabs>

                    <Box
                        ref={editorTabContentContainerRef}
                        inert={previewLocked}
                        aria-busy={isNodeLoading || (matchesRequest && previewLocked)}
                        sx={{
                            px: 2,
                            py: 1,
                            flex: 1,
                            overflowY: 'auto',
                        }}
                    >
                        {incomingMetadataState.status === 'failed' && tabUsesIncomingMetadata && (
                            <Alert severity="warning" sx={{mb: 2}}>
                                Auswahlvorschläge und wiederverwendbare Inhalte konnten nicht geladen werden.
                                {checked && ' Sie können die Konfiguration weiterhin bearbeiten.'}
                            </Alert>
                        )}
                        {/* Nested dialogs can live outside the inert subtree. Each working copy starts with fresh controls. */}
                        <ProcessNodeEditorProvider
                            key={JSON.stringify([nodeId, loaded?.generation, matchesRequest, checked])}
                            value={{
                                provider,
                                layout,
                                testClaim,
                                node: checked ? editedNode ?? originalNode : originalNode,
                                originalNode,
                                dataKeyUnlocked,
                                onUnlockDataKey: () => {
                                    if (!canEdit()) return;
                                    keyLockRevision.current++;
                                    setDataKeyUnlocked(true);
                                },
                                onLockDataKey: () => {
                                    if (!canEdit()) return;
                                    keyLockRevision.current++;
                                    setDataKeyUnlocked(false);
                                },
                                dataKeyValidationRequested,
                                outputMappingErrors,
                                setNode: (node, updateOriginal) => {
                                    if (!canEdit()) return;
                                    if (updateOriginal) {
                                        setOriginalNode(node);
                                    }
                                    if (node.dataKey !== (editedNode ?? originalNode).dataKey) {
                                        setProblems((previous) => previous == null ? previous : {
                                            ...previous,
                                            commonErrors: {...previous.commonErrors, dataKey: []},
                                        });
                                    }
                                    setOutputMappingErrors({});
                                    if (currentCheckpoint.current?.phase === 'confirmed' && editorValuesEqual(node, currentOriginal.current)) {
                                        setSaveCheckpoint(undefined);
                                    }
                                    setEditedNode(node);
                                },
                                isEditable: checked,
                                problems: problems,
                                incomingMetadata: incomingMetadataState.status === 'loaded'
                                    ? incomingMetadataState.metadata
                                    : null,
                                incomingMetadataLoadFailed: incomingMetadataState.status === 'failed',
                            }}
                        >
                            <Outlet/>
                        </ProcessNodeEditorProvider>
                    </Box>
                </Box>

                <Box
                    sx={{
                        borderTop: '1px solid',
                        borderTopColor: 'divider',
                        mt: 'auto',
                        px: 2,
                        pt: 2,
                        pb: 2.5,
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                >
                    <Button
                        onClick={() => {
                            void handleSaveSelected();
                        }}
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!canSave || isSaving}
                    >
                        Konfiguration speichern
                    </Button>

                    <Button
                        component={Link}
                        to={`/processes/${params.processId}/versions/${params.processVersion}?${searchParams.toString()}`}
                        color={hasChanged ? 'error' : 'primary'}
                    >
                        {hasChanged ? 'Abbrechen' : 'Schließen'}
                    </Button>
                </Box>
            </Box>

            {changeBlockerDialog}
            {mergeConflictDialog}

            <ProcessNodeEditorMenu
                anchorEl={menuAnchorEl}
                onClose={() => {
                    setMenuAnchorEl(null);
                }}
                editable={structureEditable && checked}
                onShowNodeProviderDetails={() => {
                    setIsProviderDetailsDialogOpen(true);
                }}
                onExportNode={handleExportSelected}
                onReplaceNode={() => {
                    if (!structureEditable || !canEdit() || originalNode == null) {
                        return;
                    }

                    onStartReplaceNode(originalNode);
                }}
                onDeleteNode={handleDeleteSelected}
            />

            <ProcessNodeProviderDetailsDialog
                open={isProviderDetailsDialogOpen}
                provider={provider}
                ports={originalNode?.ports}
                onClose={() => {
                    setIsProviderDetailsDialogOpen(false);
                }}
            />
        </>
    );
}
