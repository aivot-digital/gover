import {ProcessNodeEntity} from "../../../../entities/process-node-entity";
import {useContext, useEffect, useMemo, useState} from "react";
import {GroupLayout} from "../../../../../../models/elements/form/layout/group-layout";
import {ProcessNodeApiService} from "../../../../services/process-node-api-service";
import {Box, Button, Divider, IconButton, Skeleton, Tab, Tabs} from "@mui/material";
import {TextFieldComponent} from "../../../../../../components/text-field/text-field-component";
import {ElementDerivationContext} from "../../../../../elements/components/element-derivation-context";
import {Outlet, useNavigate, useParams} from "react-router-dom";
import {ProcessDetailsPageContext} from "../../process-details-page-context";
import {withDelay} from "../../../../../../utils/with-delay";
import {ProviderTypeStyles} from "../../../../data/provider-type-styles";
import {
    ProcessNodeProvider,
    ProcessNodeProviderApiService
} from "../../../../services/process-node-provider-api-service";
import {ProcessNodeEditorProvider} from "./process-node-editor-context";
import {useLocation} from "react-router";
import Typography from "@mui/material/Typography";
import Chip from "@mui/material/Chip";
import More from "@aivot/mui-material-symbols-400-outlined/dist/more/More";
import MoreVert from "@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert";
import Save from "@aivot/mui-material-symbols-400-outlined/dist/save/Save";

export function ProcessNodeEditor() {
    const params = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    const [selectedNode, setSelectedNode] = useState<ProcessNodeEntity | null>(null);

    const {
        onSave,
        onDelete,
    } = useContext(ProcessDetailsPageContext);

    const [provider, setProvider] = useState<ProcessNodeProvider | null>(null);

    const [localNode, setLocalNode] = useState<ProcessNodeEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);

    const nodeId = useMemo(() => {
        return parseInt(params.nodeId!);
    }, [params]);

    useEffect(() => {
        // Reset state
        setSelectedNode(null);
        setLocalNode(null);

        // Fetch node details
        new ProcessNodeApiService()
            .retrieve(nodeId)
            .then((node) => {
                setSelectedNode(node);
                setLocalNode(null);
            });

        new ProcessNodeApiService()
            .getConfigurationLayout(nodeId)
            .then(setLayout);
    }, [nodeId]);

    useEffect(() => {
        if (selectedNode == null) {
            setLayout(null);
            setLocalNode(null);
            return;
        }

        setLocalNode(selectedNode);
        withDelay(
            new ProcessNodeApiService()
                .getConfigurationLayout(selectedNode.id), 500)
            .then(setLayout);

        new ProcessNodeProviderApiService()
            .getNodeProvider(selectedNode.processNodeDefinitionKey, selectedNode.processNodeDefinitionVersion)
            .then(setProvider);
    }, [selectedNode]);

    const {
        Icon: TypeIcon,
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = useMemo(() => {
        return provider == null ? {
            Icon: () => null,
            label: '',
            bgColor: '#ffffff',
            textColor: '#000000',
        } : ProviderTypeStyles[provider.type];
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
        if (localNode != null) {
            onSave(localNode);
        }
    };

    const handleDeleteSelected = () => {
        if (selectedNode == null) {
            return;
        }

        onDelete(selectedNode);
        // TODO: show error because there are edits
    };

    if (selectedNode == null || layout == null || provider == null) {
        return (
            <Box>
                <Skeleton height={96}/>
                <Skeleton height={96}/>
                <Skeleton height={256}/>
            </Box>
        );
    }

    return (
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

                        <Typography fontWeight="bold">
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
                        onClick={() => {
                            alert('Weitere Optionen demnächst verfügbar');
                        }}
                    >
                        <MoreVert/>
                    </IconButton>
                </Box>

                <Tabs
                    value={currentTab}
                    onChange={(_, value) => {
                        navigate(`/processes/${params.processId}/versions/${params.processVersion}/nodes/${selectedNode.id}/tabs/${value}`);
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
                            provider: provider,
                            layout: layout,
                            node: localNode ?? selectedNode,
                            setNode: setLocalNode,
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
                    justifyContent: 'space-between'
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
                    onClick={handleDeleteSelected}
                    color="error"
                >
                    Knoten entfernen
                </Button>
            </Box>

        </Box>
    );
}