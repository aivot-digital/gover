import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import React, {type ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {Box, Button, IconButton, Tab, Tabs} from '@mui/material';
import {alpha, keyframes} from '@mui/material/styles';
import {Link, Outlet, useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useProcessDetailsPageContext} from '../../process-details-page-context';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
} from '../../../../services/process-node-provider-api-service';
import {ProcessNodeEditorProvider} from './process-node-editor-context';
import {useLocation} from 'react-router';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import {useChangeBlocker} from '../../../../../../hooks/use-change-blocker';
import {ProcessNodeEditorMenu} from './components/process-node-editor-menu';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {getNodeName} from '../process-flow-editor/utils/node-utils';
import {isApiError} from '../../../../../../models/api-error';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../../../hooks/use-app-dispatch';
import {type ProcessTestClaimEntity} from '../../../../entities/process-test-claim-entity';
import {ProcessTestClaimApiService} from '../../../../services/process-test-claim-api-service';
import {isElementData} from '../../../../../../models/element-data';
import {ProcessNodeEditorSkeleton} from './process-node-editor-skeleton';
import {clearLoadingMessage, setLoadingMessage} from '../../../../../../slices/shell-slice';
import {useDelayedVisibility} from '../../../../../../hooks/use-delayed-visibility';

const PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY = 150;
const PROCESS_NODE_EDITOR_LOADED_FEEDBACK_DURATION = 1200;
const processNodeEditorLoadedSidebarFlash = keyframes`
    0% {
        background-color: ${'#f6f6f6'};
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

    const [originalNode, setOriginalNode] = useState<ProcessNodeEntity | null>(null);

    const {
        editable,
        onSave,
        onDelete,
        onStartReplaceNode,
        nodeRefreshSignal,
    } = useProcessDetailsPageContext();

    const [provider, setProvider] = useState<ProcessNodeProvider | null>(null);
    const [editedNode, setEditedNode] = useState<ProcessNodeEntity | null>(null);
    const [testClaim, setTestClaim] = useState<ProcessTestClaimEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [isNodeLoading, setIsNodeLoading] = useState(false);
    const [showNodeLoadedFeedback, setShowNodeLoadedFeedback] = useState(false);
    const hasEditorContent = originalNode != null && layout != null && provider != null;
    const showNodeLoadingOverlay = useDelayedVisibility(isNodeLoading, PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY);
    const showInitialNodeEditorSkeleton = useDelayedVisibility(!hasEditorContent, PROCESS_NODE_EDITOR_LOADING_INDICATOR_DELAY);
    const hasShownShellLoadingRef = useRef(false);

    const nodeId = (() => {
        const rawNodeId = params.nodeId;
        if (rawNodeId == null) {
            throw new Error('nodeId is required');
        }
        return parseInt(rawNodeId, 10);
    })();
    const nodeRefreshVersion = nodeRefreshSignal.nodeId === nodeId ? nodeRefreshSignal.version : 0;

    useEffect(() => {
        if (originalNode == null) {
            setTestClaim(null);
            return;
        }

        new ProcessTestClaimApiService()
            .listAll({
                processId: originalNode.processId,
                processVersion: originalNode.processVersion,
            })
            .then((claims) => {
                if (claims.content.length > 0) {
                    setTestClaim(claims.content[0]);
                } else {
                    setTestClaim(null);
                }
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Testansprüche für das Prozesselement konnten nicht geladen werden.'));
            });
    }, [originalNode?.processId, originalNode?.processVersion]);

    const {
        hasChanged,
        dialog: changeBlockerDialog,
    } = useChangeBlocker(originalNode, editedNode);

    useEffect(() => {
        let isCancelled = false;
        const hasEditorContent = originalNode != null && layout != null && provider != null;

        if (!hasEditorContent) {
            setOriginalNode(null);
            setEditedNode(null);
            setLayout(null);
            setProvider(null);
            setTestClaim(null);
        }

        setIsNodeLoading(true);
        setShowNodeLoadedFeedback(false);

        (async () => {
            const [node, configurationLayout] = await Promise.all([
                new ProcessNodeApiService().retrieve(nodeId),
                new ProcessNodeApiService().getConfigurationLayout(nodeId),
            ]);
            const nodeProvider = await new ProcessNodeProviderApiService()
                .getNodeProvider(node.processNodeDefinitionKey, node.processNodeDefinitionVersion);

            return {
                node,
                configurationLayout,
                nodeProvider,
            };
        })()
            .then(({node, configurationLayout, nodeProvider}) => {
                if (isCancelled) {
                    return;
                }
                setOriginalNode(node);
                setEditedNode(node);
                setLayout(configurationLayout);
                setProvider(nodeProvider);
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
    }, [nodeId, nodeRefreshVersion]);

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
        Icon: TypeIcon,
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = useMemo(() => {
        return provider == null ?
            {
                Icon: () => null,
                label: '',
                bgColor: '#ffffff',
                textColor: '#000000',
            } :
            ProviderTypeStyles[provider.type];
    }, [provider]);

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

    const handleSaveSelected = (): void => {
        if (!hasChanged || editedNode == null) {
            return;
        }

        onSave(editedNode)
            .then(() => {
                setOriginalNode(editedNode);
                dispatch(showSuccessSnackbar('Der Knoten wurde erfolgreich gespeichert.'));
            })
            .catch((err: any) => {
                if (isApiError(err) && err.status === 400 && isElementData(err.details)) {
                    setEditedNode({
                        ...editedNode,
                        configuration: err.details,
                    });
                    dispatch(showErrorSnackbar('Der Knoten konnte nicht gespeichert werden, da die Konfiguration ungültig ist.'));
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Der Knoten konnte nicht gespeichert werden.'));
                }
            });
    };

    const handleDeleteSelected = (): void => {
        if (originalNode == null || provider == null) {
            return;
        }

        confirm({
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
                if (confirm) {
                    onDelete(originalNode);
                }
            })
            .catch((err) => {
                dispatch(showErrorSnackbar('Das Prozesselement konnte nicht gelöscht werden.'));
                console.error(err);
            });
    };

    if (!hasEditorContent) {
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
                    animation: showNodeLoadedFeedback ? `${processNodeEditorLoadedSidebarFlash} ${PROCESS_NODE_EDITOR_LOADED_FEEDBACK_DURATION}ms ease-out` : 'none',
                }}
            >
                {
                    showNodeLoadingOverlay &&
                    <Box
                        sx={{
                            position: 'absolute',
                            inset: 0,
                            zIndex: 2,
                            bgcolor: 'rgba(255, 255, 255, 0.42)',
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
                        pointerEvents: isNodeLoading ? 'none' : 'auto',
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
                            <TypeIcon/>
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
                                    fontWeight="bold"
                                    component="div"
                                    title={provider.name}
                                    sx={{
                                        flex: 1,
                                        minWidth: 0,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                    }}
                                >
                                    {provider.name}
                                </Typography>
                                <Chip
                                    label={`Version ${provider.majorVersion}`}
                                    size="small"
                                    sx={{
                                        fontWeight: 'normal',
                                        flexShrink: 0,
                                    }}
                                />
                            </Box>
                        </Box>

                        <IconButton
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
                            navigate(`/processes/${params.processId}/versions/${params.processVersion}/nodes/${originalNode.id}/tabs/${value}?${searchParams.toString()}`);
                        }}
                        sx={{
                            mt: 0.25,
                            borderBottom: '1px solid #ddd',
                        }}
                    >
                        <Tab
                            label="Eigenschaften"
                            value="configuration"
                        />

                        {
                            provider.outputs.length > 0 &&
                            <Tab
                                label="Ausgangsdaten"
                                value="outputs"
                            />
                        }

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
                        sx={{
                            px: 2,
                            py: 1,
                            flex: 1,
                            overflowY: 'auto',
                        }}
                    >
                        <ProcessNodeEditorProvider
                            key={nodeId}
                            value={{
                                provider,
                                layout,
                                testClaim,
                                node: editedNode ?? originalNode,
                                setNode: (node, updateOriginal) => {
                                    if (updateOriginal) {
                                        setOriginalNode(node);
                                    }
                                    setEditedNode(node);
                                },
                                isEditable: true,
                            }}
                        >
                            <Outlet/>
                        </ProcessNodeEditorProvider>
                    </Box>
                </Box>

                <Box
                    sx={{
                        borderTop: '1px solid #ddd',
                        mt: 'auto',
                        px: 2,
                        pt: 2,
                        pb: 2.5,
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                >
                    <Button
                        onClick={handleSaveSelected}
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!hasChanged || isNodeLoading}
                    >
                        Konfiguration speichern
                    </Button>

                    <Button
                        component={Link}
                        to={`/processes/${params.processId}/versions/${params.processVersion}?${searchParams.toString()}`}
                        color="error"
                    >
                        Abbrechen
                    </Button>
                </Box>
            </Box>

            {changeBlockerDialog}

            <ProcessNodeEditorMenu
                anchorEl={menuAnchorEl}
                onClose={() => {
                    setMenuAnchorEl(null);
                }}
                onReplaceNode={() => {
                    if (!editable || originalNode == null) {
                        return;
                    }

                    onStartReplaceNode(originalNode);
                }}
                onDeleteNode={handleDeleteSelected}
            />
        </>
    );
}
