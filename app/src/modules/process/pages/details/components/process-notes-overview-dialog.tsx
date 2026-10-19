import React, {type ReactNode, useMemo} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Stack, Typography} from '@mui/material';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import Assignment from '@aivot/mui-material-symbols-400-n25-outlined/Assignment';
import {DialogTitleWithClose} from '../../../../../components/dialog-title-with-close/dialog-title-with-close';
import {MarkdownContent} from '../../../../../components/markdown-content/markdown-content';
import {isStringNotNullOrEmpty} from '../../../../../utils/string-utils';
import {type ProcessNodeEntity} from '../../../entities/process-node-entity';
import {type ProcessNodeProvider} from '../../../services/process-node-provider-api-service';
import {ProviderTypeStyles} from '../../../data/provider-type-styles';
import {KnownProviderIcons} from '../../../data/known-provider-icons';
import {getProcessNodeProviderKey} from './process-flow-editor/utils/process-flow-graph-utils';
import {getNodeName} from './process-flow-editor/utils/node-utils';
import {type SvgIconComponent} from '../../../../../types/svg-icon-component';

interface ProcessNotesOverviewDialogProps {
    open: boolean;
    nodes: ProcessNodeEntity[];
    providerCache: Record<string, ProcessNodeProvider>;
    onClose: () => void;
    onSelectNode: (node: ProcessNodeEntity) => void;
}

interface ProcessNoteEntry {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider | null;
}

export function ProcessNotesOverviewDialog(props: ProcessNotesOverviewDialogProps): ReactNode {
    const {
        open,
        nodes,
        providerCache,
        onClose,
        onSelectNode,
    } = props;

    const entries = useMemo<ProcessNoteEntry[]>(() => (
        nodes
            .filter((node) => isStringNotNullOrEmpty(node.notes))
            .sort((left, right) => left.id - right.id)
            .map((node) => ({
                node,
                provider: providerCache[getProcessNodeProviderKey(
                    node.processNodeDefinitionKey,
                    node.processNodeDefinitionVersion,
                )] ?? null,
            }))
    ), [nodes, providerCache]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="sm"
            fullWidth
        >
            <DialogTitleWithClose onClose={onClose}>
                Übersicht der Notizen
            </DialogTitleWithClose>

            <DialogContent>
                <Typography
                    sx={{
                        color: "text.secondary",
                        mb: 2.5,
                        maxWidth: 680
                    }}>
                    Diese Übersicht zeigt alle Notizen, die an Prozesselementen der aktuellen Prozessversion hinterlegt wurden.
                    Nutzen Sie sie, um offene Punkte, fachliche Annahmen oder Hinweise zur Modellierung zentral nachzuvollziehen.
                    Prozesselemente ohne Notiz werden nicht aufgeführt.
                </Typography>

                {
                    entries.length === 0 ?
                        <Stack
                            spacing={1}
                            sx={{
                                alignItems: 'center',
                                border: '1px dashed',
                                borderColor: 'divider',
                                borderRadius: 1,
                                color: 'text.secondary',
                                py: 5,
                                px: 3,
                                textAlign: 'center',
                            }}
                        >
                            <Typography variant="h5" sx={{
                                color: "text.primary"
                            }}>
                                Keine Notizen hinterlegt
                            </Typography>
                            <Typography>
                                Für diese Prozessversion sind aktuell keine Notizen an Prozesselementen hinterlegt.
                            </Typography>
                        </Stack> :
                        <Stack spacing={1.5}>
                            {
                                entries.map((entry) => (
                                    <ProcessNotesOverviewDialogEntry
                                        key={entry.node.id}
                                        entry={entry}
                                        onSelectNode={onSelectNode}
                                    />
                                ))
                            }
                        </Stack>
                }
            </DialogContent>

            <DialogActions
                sx={{
                    justifyContent: 'flex-end',
                }}
            >
                <Button onClick={onClose}>
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

interface ProcessNotesOverviewDialogEntryProps {
    entry: ProcessNoteEntry;
    onSelectNode: (node: ProcessNodeEntity) => void;
}

function ProcessNotesOverviewDialogEntry(props: ProcessNotesOverviewDialogEntryProps): ReactNode {
    const {
        entry,
        onSelectNode,
    } = props;
    const {
        node,
        provider,
    } = entry;

    const providerTypeStyle = provider == null ? null : ProviderTypeStyles[provider.type];
    const typeLabel = providerTypeStyle?.label ?? 'Prozesselement';
    const ProviderIcon: SvgIconComponent = provider == null ?
        Assignment :
        KnownProviderIcons[provider.componentKey] ?? KnownProviderIcons[provider.key] ?? providerTypeStyle?.Icon ?? Assignment;
    const nodeName = provider == null ? (node.name ?? node.processNodeDefinitionKey) : getNodeName(node, provider);

    return (
        <Box
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                p: 2,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: {
                        xs: 'flex-start',
                        sm: 'center',
                    },
                    flexDirection: {
                        xs: 'column',
                        sm: 'row',
                    },
                    gap: 2,
                    minWidth: 0,
                }}
            >
                <Box
                    sx={{
                        aspectRatio: '1 / 1',
                        alignItems: 'center',
                        bgcolor: providerTypeStyle?.bgColor ?? 'action.hover',
                        borderRadius: '50%',
                        color: providerTypeStyle?.textColor ?? 'text.secondary',
                        display: 'flex',
                        flexShrink: 0,
                        height: 38,
                        justifyContent: 'center',
                        minHeight: 38,
                        minWidth: 38,
                        width: 38,
                    }}
                >
                    <ProviderIcon/>
                </Box>

                <Box
                    sx={{
                        flex: 1,
                        minWidth: 0,
                        width: {
                            xs: '100%',
                            sm: 'auto',
                        },
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
                            title={nodeName}
                            sx={{
                                fontWeight: "bold",
                                flex: 1,
                                minWidth: 0,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap'
                            }}>
                            {nodeName}
                        </Typography>
                    </Box>

                    <Typography
                        variant="body2"
                        sx={{
                            color: "text.secondary",
                            mt: 0.25
                        }}>
                        Datenschlüssel: {node.dataKey}
                    </Typography>
                </Box>

                <Button
                    variant="outlined"
                    startIcon={<ArrowForward/>}
                    onClick={() => {
                        onSelectNode(node);
                    }}
                    sx={{
                        alignSelf: {
                            xs: 'flex-start',
                            sm: 'center',
                        },
                        flexShrink: 0,
                    }}
                >
                    Zum Element
                </Button>
            </Box>

            <MarkdownContent
                markdown={node.notes}
                className="content-without-margin-on-childs"
                sx={{
                    bgcolor: 'action.hover',
                    borderRadius: 1,
                    mt: 1.5,
                    p: 1.5,
                }}
            />
        </Box>
    );
}
