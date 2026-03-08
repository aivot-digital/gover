import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    TextField,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../../../../components/dialog-title-with-close/dialog-title-with-close';
import {type ProcessFlow} from '../process-details-page';
import {
    type ProcessNodePort,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../../../services/process-node-provider-api-service';
import {type ProcessNodeEntity} from '../../../entities/process-node-entity';
import {getProcessNodeProviderKey} from './process-flow-editor/utils/process-flow-graph-utils';
import {getNodeName} from './process-flow-editor/utils/node-utils';

interface ConnectExistingNodeDialogProps {
    open: boolean;
    processFlow: ProcessFlow;
    nodeProviders: ProcessNodeProvider[];
    sourceNode: ProcessNodeEntity | null;
    onClose: () => void;
    onConnect: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
}

interface ConnectTargetOption {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
    label: string;
}

export function ProcessConnectExistingNodeDialog(props: ConnectExistingNodeDialogProps): ReactNode {
    const {
        open,
        processFlow,
        nodeProviders,
        sourceNode,
        onClose,
        onConnect,
    } = props;

    const [retainedSourceNode, setRetainedSourceNode] = useState<ProcessNodeEntity | null>(sourceNode);
    const [selectedTarget, setSelectedTarget] = useState<ConnectTargetOption | null>(null);
    const [selectedPortKey, setSelectedPortKey] = useState<string>('');
    const renderSourceNode = sourceNode ?? retainedSourceNode;

    const providerMap = useMemo(() => new Map(
        nodeProviders.map((provider) => [
            getProcessNodeProviderKey(provider.key, provider.majorVersion),
            provider,
        ]),
    ), [nodeProviders]);

    const sourceProvider = useMemo(() => {
        if (renderSourceNode == null) {
            return null;
        }

        return providerMap.get(getProcessNodeProviderKey(
            renderSourceNode.processNodeDefinitionKey,
            renderSourceNode.processNodeDefinitionVersion,
        )) ?? null;
    }, [providerMap, renderSourceNode]);

    const availablePorts = useMemo<ProcessNodePort[]>(() => {
        if (renderSourceNode == null || sourceProvider == null) {
            return [];
        }

        const usedPortKeys = new Set(
            processFlow.edges
                .filter((edge) => edge.fromNodeId === renderSourceNode.id)
                .map((edge) => edge.viaPort),
        );

        return sourceProvider.ports.filter((port) => !usedPortKeys.has(port.key));
    }, [processFlow.edges, renderSourceNode, sourceProvider]);

    const targetOptions = useMemo<ConnectTargetOption[]>(() => (
        processFlow.nodes
            .flatMap((node) => {
                if (renderSourceNode != null && node.id === renderSourceNode.id) {
                    return [];
                }

                const provider = providerMap.get(getProcessNodeProviderKey(
                    node.processNodeDefinitionKey,
                    node.processNodeDefinitionVersion,
                ));
                if (provider == null || provider.type === ProcessNodeType.Trigger) {
                    return [];
                }

                return [{
                    node,
                    provider,
                    label: getNodeName(node, provider),
                }];
            })
            .sort((a, b) => a.label.localeCompare(b.label, 'de'))
    ), [processFlow.nodes, providerMap, renderSourceNode]);

    useEffect(() => {
        if (sourceNode != null) {
            setRetainedSourceNode(sourceNode);
        }
    }, [sourceNode]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setSelectedPortKey(availablePorts[0]?.key ?? '');
        setSelectedTarget(targetOptions.length === 1 ? targetOptions[0] : null);
    }, [availablePorts, open, targetOptions]);

    const handleConnect = (): void => {
        if (renderSourceNode == null || selectedTarget == null || selectedPortKey.length === 0) {
            return;
        }

        onConnect(renderSourceNode.id, selectedTarget.node.id, selectedPortKey);
        onClose();
    };

    const sourceNodeName = renderSourceNode != null && sourceProvider != null ? getNodeName(renderSourceNode, sourceProvider) : null;

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="sm"
            TransitionProps={{
                onExited: () => {
                    setRetainedSourceNode(null);
                    setSelectedTarget(null);
                    setSelectedPortKey('');
                },
            }}
        >
            <DialogTitleWithClose onClose={onClose}>
                Mit bestehendem Knoten verbinden
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2,
                }}
            >
                <Typography color="text.secondary">
                    Verbinden Sie <strong>{sourceNodeName ?? 'dieses Prozesselement'}</strong> mit einem bereits vorhandenen Zielknoten.
                </Typography>

                {
                    availablePorts.length === 0 &&
                    <Alert severity="info">
                        Dieses Prozesselement hat keine freien Ausgangsports mehr.
                    </Alert>
                }

                {
                    targetOptions.length === 0 &&
                    <Alert severity="info">
                        Es steht aktuell kein geeigneter Zielknoten zur Auswahl.
                    </Alert>
                }

                {
                    availablePorts.length > 1 &&
                    <TextField
                        select
                        fullWidth
                        label="Ausgang"
                        value={selectedPortKey}
                        onChange={(event) => {
                            setSelectedPortKey(event.target.value);
                        }}
                        SelectProps={{
                            native: true,
                        }}
                    >
                        <option value="" disabled>
                            Ausgang auswählen
                        </option>
                        {
                            availablePorts.map((port) => (
                                <option key={port.key} value={port.key}>
                                    {port.label}
                                </option>
                            ))
                        }
                    </TextField>
                }

                {
                    availablePorts.length === 1 &&
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Ausgang
                        </Typography>
                        <Typography fontWeight={600}>
                            {availablePorts[0].label}
                        </Typography>
                    </Box>
                }

                <Autocomplete
                    options={targetOptions}
                    value={selectedTarget}
                    onChange={(_, value) => {
                        setSelectedTarget(value);
                    }}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, value) => option.node.id === value.node.id}
                    renderOption={(optionProps, option) => (
                        <Box component="li" {...optionProps}>
                            <Box sx={{minWidth: 0}}>
                                <Typography fontWeight={600}>
                                    {option.label}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {option.provider.name}
                                </Typography>
                            </Box>
                        </Box>
                    )}
                    renderInput={(params) => (
                        <TextField
                            {...params}
                            label="Zielknoten"
                            placeholder="Knoten suchen"
                        />
                    )}
                    disabled={targetOptions.length === 0}
                />
            </DialogContent>

            <DialogActions sx={{px: 3, pb: 3}}>
                <Button
                    variant="contained"
                    onClick={handleConnect}
                    disabled={renderSourceNode == null || selectedTarget == null || selectedPortKey.length === 0}
                >
                    Verbinden
                </Button>
                <Button onClick={onClose}>
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
