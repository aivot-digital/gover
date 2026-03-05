import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {Box, Button, IconButton, Skeleton, Tab, Tabs} from '@mui/material';
import {Link, Outlet, useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {useProcessDetailsPageContext} from '../../process-details-page-context';
import {withDelay} from '../../../../../../utils/with-delay';
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

export function ProcessNodeEditor(): ReactNode {
    const params = useParams();
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();
    const location = useLocation();
    const confirm = useConfirm();
    const dispatch = useAppDispatch();

    const [originalNode, setOriginalNode] = useState<ProcessNodeEntity | null>(null);

    const {
        onSave,
        onDelete,
    } = useProcessDetailsPageContext();

    const [provider, setProvider] = useState<ProcessNodeProvider | null>(null);
    const [editedNode, setEditedNode] = useState<ProcessNodeEntity | null>(null);
    const [testClaim, setTestClaim] = useState<ProcessTestClaimEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);

    const nodeId = useMemo(() => {
        const nodeId = params.nodeId;
        if (nodeId == null) {
            throw new Error('nodeId is required');
        }
        return parseInt(nodeId, 10);
    }, [params]);

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
    }, [originalNode]);

    const {
        dialog: changeBlockerDialog,
    } = useChangeBlocker(originalNode, editedNode);

    useEffect(() => {
        // Reset state
        setOriginalNode(null);
        setEditedNode(null);

        // Fetch node details
        new ProcessNodeApiService()
            .retrieve(nodeId)
            .then((node) => {
                setOriginalNode(node);
                setEditedNode(null);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Details für das Prozesselement konnten nicht geladen werden.'));
            });

        // Fetch the configuration layout
        new ProcessNodeApiService()
            .getConfigurationLayout(nodeId)
            .then(setLayout)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Konfigurationsoberfläche für das Prozesselement konnte nicht geladen werden.'));
            });
    }, [nodeId]);

    useEffect(() => {
        if (originalNode == null) {
            setLayout(null);
            setEditedNode(null);
            return;
        }

        setEditedNode(originalNode);

        withDelay(new ProcessNodeApiService()
            .getConfigurationLayout(originalNode.id), 500)
            .then(setLayout)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Konfigurationsoberfläche für das Prozesselement konnte nicht geladen werden.'));
            });

        new ProcessNodeProviderApiService()
            .getNodeProvider(originalNode.processNodeDefinitionKey, originalNode.processNodeDefinitionVersion)
            .then(setProvider)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Anbietertyp für das Prozesselement konnte nicht geladen werden.'));
            });
    }, [originalNode]);

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
        if (editedNode != null) {
            onSave(editedNode)
                .then(() => {
                    dispatch(showSuccessSnackbar('Der Knoten wurde erfolgreich gespeichert.'));
                })
                .catch((err: any) => {
                    if (isApiError(err) && err.status === 400 && err.displayableToUser) {
                        dispatch(showErrorSnackbar('Der Knoten konnte nicht gespeichert werden, da die Konfiguration ungültig ist.'));
                    } else {
                        dispatch(showApiErrorSnackbar(err, 'Der Knoten konnte nicht gespeichert werden.'));
                    }
                });
        }
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

    if (originalNode == null || layout == null || provider == null) {
        return (
            <Box
                sx={{
                    px: 2,
                }}
            >
                <Skeleton height={96}/>
                <Skeleton height={96}/>
                <Skeleton height={256}/>
            </Box>
        );
    }

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    height: '100vh',
                }}
            >
                <Box
                    sx={{
                        flex: 1,
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 2,
                            px: 2,
                            pt: 1,
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: 32,
                                height: 32,
                                borderRadius: '50%',
                                backgroundColor: typeBgColor,
                                color: typeTextColor,
                            }}
                        >
                            <TypeIcon/>
                        </Box>

                        <Box>
                            <Typography variant="caption">
                                {typeLabel}
                            </Typography>

                            <Typography
                                fontWeight="bold"
                                component="div"
                            >
                                {provider.name} <Chip
                                    label={`Version ${provider.majorVersion}`}
                                    size="small"
                                    sx={{
                                        ml: 1,
                                        fontWeight: 'normal',
                                    }}
                                />
                            </Typography>
                        </Box>

                        <IconButton
                            sx={{
                                marginLeft: 'auto',
                            }}
                            onClick={(event) => {
                                setMenuAnchorEl(event.target as HTMLElement);
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
                            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
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
                        sx={{
                            px: 2,
                            py: 1,
                            flex: 1,
                            overflowY: 'auto',
                        }}
                    >
                        <ProcessNodeEditorProvider
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
                        pb: 4,
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                >
                    <Button
                        onClick={handleSaveSelected}
                        variant="contained"
                        startIcon={<Save/>}
                    >
                        Knoten speichern
                    </Button>

                    <Button
                        component={Link}
                        to={`/processes/${params.processId}/versions/${params.processVersion}`}
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

                onDeleteNode={handleDeleteSelected}
            />
        </>
    );
}
