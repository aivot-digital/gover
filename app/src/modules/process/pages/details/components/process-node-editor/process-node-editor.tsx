import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {useContext, useEffect, useMemo, useState} from 'react';
import {type GroupLayout} from '../../../../../../models/elements/form/layout/group-layout';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {Box, Button, IconButton, Skeleton, Tab, Tabs} from '@mui/material';
import {Link, Outlet, useNavigate, useParams} from 'react-router-dom';
import {ProcessDetailsPageContext} from '../../process-details-page-context';
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

export function ProcessNodeEditor() {
    const params = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const confirm = useConfirm();

    const [originalNode, setOriginalNode] = useState<ProcessNodeEntity | null>(null);

    const {
        onSave,
        onDelete,
    } = useContext(ProcessDetailsPageContext);

    const [provider, setProvider] = useState<ProcessNodeProvider | null>(null);

    const [editedNode, setEditedNode] = useState<ProcessNodeEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);

    const nodeId = useMemo(() => {
        return parseInt(params.nodeId!);
    }, [params]);

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
            });

        new ProcessNodeApiService()
            .getConfigurationLayout(nodeId)
            .then(setLayout);
    }, [nodeId]);

    useEffect(() => {
        if (originalNode == null) {
            setLayout(null);
            setEditedNode(null);
            return;
        }

        setEditedNode(originalNode);
        withDelay(
            new ProcessNodeApiService()
                .getConfigurationLayout(originalNode.id), 500)
            .then(setLayout);

        new ProcessNodeProviderApiService()
            .getNodeProvider(originalNode.processNodeDefinitionKey, originalNode.processNodeDefinitionVersion)
            .then(setProvider);
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
        }
        return 'configuration';
    }, [location]);

    const handleSaveSelected = () => {
        if (editedNode != null) {
            onSave(editedNode);
        }
    };

    const handleDeleteSelected = () => {
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

                            <Typography fontWeight="bold" component="div">
                                {provider.name} <Chip
                                    label={`Version ${provider.version}`}
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
                            navigate(`/processes/${params.processId}/versions/${params.processVersion}/nodes/${originalNode.id}/tabs/${value}`);
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
                                node: editedNode ?? originalNode,
                                setNode: setEditedNode,
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
